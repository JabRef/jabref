///usr/bin/env jbang "$0" "$@" ; exit $?

//JAVA 25
//NATIVE_IMAGE_OPTIONS --no-fallback -O2 -H:+ReportExceptionStackTraces
//SOURCES Json.java
//DEPS org.hisp.dhis:json-tree:1.5
//DEPS org.tinylog:tinylog-api:2.7.0
//DEPS org.tinylog:tinylog-impl:2.7.0
//FILES META-INF/native-image/jabext-bridge/reflect-config.json=reflect-config.json

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.tinylog.Logger;

/// Loopback HTTP bridge that lets JabRef's `BrowserExtensionFulltextFetcher`
/// reach the JabRef Browser Extension (experimental).
///
/// Two halves running in one process:
///
/// 1. **HTTP server** on `127.0.0.1:<ephemeral>` exposing `/v1/health` and
///    `/v1/fulltext`. Implements the protocol defined in
///    `docs/requirements/browser-extension-fulltext.md` (the canonical
///    `req~bxf.*~1` spec).
/// 2. **Native-messaging client** on stdio. The browser launches the
///    bridge process when the extension calls `runtime.connectNative`.
///    Inbound HTTP requests are forwarded to the extension as NM
///    messages; correlated replies complete the pending HTTP requests.
///
/// Lifecycle is tied to the browser: stdin EOF (extension disconnects /
/// browser exits) terminates the loop, the shutdown hook deletes the
/// discovery file, and the process exits.
public final class JabExtBridge {

    private static final String PROVIDER_NAME = "jabext-bridge";
    private static final String PROVIDER_DISPLAY_NAME = "JabRef Browser Extension (experimental)";
    private static final int PROTOCOL_VERSION = 1;
    private static final Duration FETCH_TIMEOUT = Duration.ofMinutes(5);
    private static final Duration MATHSCINET_OPEN_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_NM_MESSAGE = 1 << 20;

    private final ConcurrentHashMap<String, CompletableFuture<Json.NmReply>> pending = new ConcurrentHashMap<>();
    private final Object stdoutLock = new Object();
    private final OutputStream rawStdout;
    private final AtomicInteger requestIdSeq = new AtomicInteger();
    private final String bearer;
    private final Path tokenFile;
    private final Path discoveryFile;

    private HttpServer httpServer;
    private volatile boolean shutdownDone;

    static {
        // Configure tinylog before any Logger call. Inline config avoids the
        // classpath-resource lookup that does not survive native-image.
        System.setProperty("tinylog.writer", "console");
        System.setProperty("tinylog.writer.stream", "err");
        System.setProperty("tinylog.writer.format",
                "{date: HH:mm:ss.SSS} [bridge] {level|min-size=5}: {message}");
        System.setProperty("tinylog.writer.level", "info");
    }

    public static void main(String[] args) {
        JabExtBridge bridge = null;
        int exitCode = 0;
        try {
            bridge = new JabExtBridge();
            bridge.run();
        } catch (Exception e) {
            Logger.error(e, "fatal");
            exitCode = 1;
        } finally {
            if (bridge != null) {
                bridge.shutdown();
            }
        }
        System.exit(exitCode);
    }

    private JabExtBridge() throws IOException {
        this.rawStdout = System.out;
        // Detach System.out from stdout to keep the native-messaging frame
        // stream pristine; tinylog writes through System.err per properties.
        System.setOut(System.err);

        this.tokenFile = ensureTokenFile();
        this.bearer = Files.readString(tokenFile, StandardCharsets.UTF_8).strip();
        this.discoveryFile = JabRefPaths.discoveryDirectory().resolve(PROVIDER_NAME + ".json");
    }

    private void run() throws Exception {
        int port = startHttpServer();
        writeDiscoveryFile(port);
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown, "jbe-shutdown"));
        runNativeMessagingLoop();
    }

    /// Idempotent teardown. Removes the discovery file and stops the HTTP
    /// server so the JVM can exit cleanly even after an unexpected error
    /// on the native-messaging thread.
    private synchronized void shutdown() {
        if (shutdownDone) {
            return;
        }
        shutdownDone = true;
        try {
            Files.deleteIfExists(discoveryFile);
        } catch (IOException e) {
            Logger.warn(e, "failed to remove discovery file");
        }
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private int startHttpServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/health", this::handleHealth);
        server.createContext("/v1/fulltext", this::handleFulltext);
        server.createContext("/v1/mathscinet/open", this::handleMathSciNetOpen);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        this.httpServer = server;
        int port = server.getAddress().getPort();
        Logger.info("http listening on 127.0.0.1:{}", port);
        return port;
    }

    private void writeDiscoveryFile(int port) throws IOException {
        Path dir = discoveryFile.getParent();
        Files.createDirectories(dir);
        String json = Json.writeDiscovery(
                PROVIDER_NAME,
                PROVIDER_DISPLAY_NAME,
                port,
                tokenFile.toAbsolutePath().toString(),
                PROTOCOL_VERSION);
        Files.writeString(discoveryFile, json, StandardCharsets.UTF_8);
        Logger.info("wrote discovery file {}", discoveryFile);
    }

    // region HTTP handlers

    private void handleHealth(HttpExchange ex) throws IOException {
        try (ex) {
            if (rejectByOrigin(ex)) {
                return;
            }
            byte[] body = Json.writeHealth(PROVIDER_NAME, PROTOCOL_VERSION).getBytes(StandardCharsets.UTF_8);
            ex.getResponseHeaders().set("Content-Type", "application/json");
            ex.sendResponseHeaders(200, body.length);
            ex.getResponseBody().write(body);
        }
    }

    private void handleFulltext(HttpExchange ex) throws IOException {
        try (ex) {
            if (rejectByOrigin(ex) || rejectByBearer(ex)) {
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                writeError(ex, 405, "bad-request", "Only POST is supported");
                return;
            }

            Json.FulltextRequest req;
            try (InputStream in = ex.getRequestBody()) {
                req = Json.readFulltextRequest(in);
            } catch (RuntimeException e) {
                writeError(ex, 400, "bad-request", "Malformed request body");
                return;
            }
            if (blank(req.doi()) && blank(req.url())) {
                writeError(ex, 400, "bad-request", "At least one of doi or url is required");
                return;
            }

            String requestId = "r" + requestIdSeq.incrementAndGet();
            CompletableFuture<Json.NmReply> future = new CompletableFuture<>();
            pending.put(requestId, future);
            try {
                sendNmFetch(requestId, req.doi(), req.url());
                Json.NmReply reply = future.get(FETCH_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

                if (reply.error() != null) {
                    int status = httpStatusForError(reply.error());
                    writeError(ex, status, reply.error(), Optional.ofNullable(reply.message()).orElse(reply.error()));
                    return;
                }
                if (blank(reply.path()) || !Files.isReadable(Path.of(reply.path()))) {
                    writeError(ex, 404, "no-pdf-found", "Provider returned no readable PDF path");
                    return;
                }
                byte[] body = Json.writeFulltextResponse(reply.id(), reply.path(), reply.sourceUrl())
                                  .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
            } catch (TimeoutException e) {
                writeError(ex, 504, "timeout", "Provider fetch exceeded internal timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                writeError(ex, 500, "internal-error", "Interrupted");
            } catch (ExecutionException e) {
                writeError(ex, 500, "internal-error", "Native-messaging dispatch failed");
            } finally {
                pending.remove(requestId);
            }
        }
    }

    private void handleMathSciNetOpen(HttpExchange ex) throws IOException {
        try (ex) {
            if (rejectByOrigin(ex) || rejectByBearer(ex)) {
                return;
            }
            if (!"POST".equals(ex.getRequestMethod())) {
                writeError(ex, 405, "bad-request", "Only POST is supported");
                return;
            }

            Json.MathSciNetOpenRequest req;
            try (InputStream in = ex.getRequestBody()) {
                req = Json.readMathSciNetOpenRequest(in);
            } catch (RuntimeException e) {
                writeError(ex, 400, "bad-request", "Malformed request body");
                return;
            }
            if (blank(req.mrNumber())) {
                writeError(ex, 400, "bad-request", "mrNumber is required");
                return;
            }

            String requestId = "r" + requestIdSeq.incrementAndGet();
            CompletableFuture<Json.NmReply> future = new CompletableFuture<>();
            pending.put(requestId, future);
            try {
                sendNmMathSciNetOpen(requestId, req.mrNumber());
                Json.NmReply reply = future.get(MATHSCINET_OPEN_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);

                if (reply.error() != null) {
                    int status = httpStatusForError(reply.error());
                    writeError(ex, status, reply.error(), Optional.ofNullable(reply.message()).orElse(reply.error()));
                    return;
                }
                if (blank(reply.action()) || reply.tabId() == null) {
                    writeError(ex, 500, "internal-error", "Provider returned no tab action");
                    return;
                }
                byte[] body = Json.writeMathSciNetOpenResponse(reply.action(), reply.tabId())
                                  .getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json");
                ex.sendResponseHeaders(200, body.length);
                ex.getResponseBody().write(body);
            } catch (TimeoutException e) {
                writeError(ex, 504, "timeout", "Provider tab action exceeded internal timeout");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                writeError(ex, 500, "internal-error", "Interrupted");
            } catch (ExecutionException e) {
                writeError(ex, 500, "internal-error", "Native-messaging dispatch failed");
            } finally {
                pending.remove(requestId);
            }
        }
    }

    private static int httpStatusForError(String code) {
        return switch (code) {
            case "no-pdf-found", "no-adapter", "auth-required", "not-reachable" -> 404;
            case "timeout" -> 504;
            case "busy" -> 503;
            case "bad-request" -> 400;
            default -> 500;
        };
    }

    private boolean rejectByOrigin(HttpExchange ex) throws IOException {
        String origin = ex.getRequestHeaders().getFirst("Origin");
        if (origin == null || origin.isBlank() || "null".equals(origin)) {
            return false;
        }
        writeError(ex, 403, "bad-request", "Origin header rejected");
        return true;
    }

    private boolean rejectByBearer(HttpExchange ex) throws IOException {
        String auth = ex.getRequestHeaders().getFirst("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            writeError(ex, 401, "bad-request", "Missing bearer token");
            return true;
        }
        String token = auth.substring(7).strip();
        if (!constantTimeEquals(token, bearer)) {
            writeError(ex, 401, "bad-request", "Invalid bearer token");
            return true;
        }
        return false;
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    private void writeError(HttpExchange ex, int status, String code, String message) throws IOException {
        byte[] body = Json.writeError(code, message).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        ex.getResponseBody().write(body);
    }

    // endregion HTTP handlers

    // region Native-messaging dispatch

    private void sendNmFetch(String requestId, String doi, String url) throws IOException {
        writeNmFrame(Json.writeNmFetchRequest(requestId, doi, url));
    }

    private void sendNmMathSciNetOpen(String requestId, String mrNumber) throws IOException {
        writeNmFrame(Json.writeNmMathSciNetOpenRequest(requestId, mrNumber));
    }

    private void writeNmFrame(String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        if (payload.length > MAX_NM_MESSAGE) {
            throw new IOException("NM payload exceeds 1 MiB");
        }
        synchronized (stdoutLock) {
            ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(payload.length);
            rawStdout.write(header.array());
            rawStdout.write(payload);
            rawStdout.flush();
        }
    }

    private void runNativeMessagingLoop() throws IOException {
        InputStream in = System.in;
        byte[] lenBuf = new byte[4];
        while (true) {
            if (!readFully(in, lenBuf)) {
                Logger.info("stdin EOF; shutting down");
                break;
            }
            int len = ByteBuffer.wrap(lenBuf).order(ByteOrder.LITTLE_ENDIAN).getInt();
            if (len <= 0 || len > MAX_NM_MESSAGE) {
                Logger.warn("invalid NM frame length: {}", len);
                break;
            }
            byte[] body = new byte[len];
            if (!readFully(in, body)) {
                break;
            }
            try {
                Json.NmReply reply = Json.readNmReply(body);
                if (reply.requestId() == null) {
                    Logger.debug("NM message without requestId, ignored");
                    continue;
                }
                CompletableFuture<Json.NmReply> fut = pending.get(reply.requestId());
                if (fut != null) {
                    fut.complete(reply);
                } else {
                    Logger.debug("reply for unknown requestId {}", reply.requestId());
                }
            } catch (RuntimeException e) {
                Logger.warn(e, "malformed NM message");
            }
        }
    }

    private static boolean readFully(InputStream in, byte[] buf) throws IOException {
        int off = 0;
        while (off < buf.length) {
            int n = in.read(buf, off, buf.length - off);
            if (n < 0) {
                return false;
            }
            off += n;
        }
        return true;
    }

    // endregion Native-messaging dispatch

    // region Token bootstrap

    private static Path ensureTokenFile() throws IOException {
        Path dir = JabRefPaths.tokenDirectory();
        Files.createDirectories(dir);
        Path file = dir.resolve(PROVIDER_NAME + ".token");
        if (Files.exists(file) && Files.size(file) > 0) {
            return file;
        }
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String token = Base64.getEncoder().withoutPadding().encodeToString(raw);
        Files.writeString(file, token + System.lineSeparator(), StandardCharsets.UTF_8);
        applyOwnerOnlyPermissions(file);
        return file;
    }

    private static void applyOwnerOnlyPermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows path: directory under %APPDATA%\Roaming\JabRef inherits user-only ACL by default.
        }
    }

    // endregion Token bootstrap

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    /// Resolves the platform paths the bridge writes into. The discovery
    /// directory mirrors JabRef's `BrowserExtensionProviderDiscovery`; the
    /// token directory is a sibling so the bridge owns its own state.
    static final class JabRefPaths {
        private JabRefPaths() {
        }

        static Path discoveryDirectory() {
            return jabrefConfigBase().resolve("fulltext-providers");
        }

        static Path tokenDirectory() {
            return jabrefConfigBase().resolve("fulltext-providers-state");
        }

        private static Path jabrefConfigBase() {
            String os = System.getProperty("os.name", "").toLowerCase();
            if (os.contains("win")) {
                String appData = System.getenv("APPDATA");
                Path base = (appData == null || appData.isBlank())
                        ? Path.of(System.getProperty("user.home"), "AppData", "Roaming")
                        : Path.of(appData);
                return base.resolve("JabRef");
            }
            if (os.contains("mac") || os.contains("darwin")) {
                return Path.of(System.getProperty("user.home"),
                        "Library", "Application Support", "JabRef");
            }
            String xdg = System.getenv("XDG_CONFIG_HOME");
            Path base = (xdg == null || xdg.isBlank())
                    ? Path.of(System.getProperty("user.home"), ".config")
                    : Path.of(xdg);
            return base.resolve("jabref");
        }
    }
}
