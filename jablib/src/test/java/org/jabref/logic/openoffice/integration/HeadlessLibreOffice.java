package org.jabref.logic.openoffice.integration;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.sun.star.beans.PropertyValue;
import com.sun.star.bridge.XUnoUrlResolver;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.connection.NoConnectException;
import com.sun.star.document.MacroExecMode;
import com.sun.star.document.UpdateDocMode;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XStorable;
import com.sun.star.lang.XComponent;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/// Owns an isolated Writer process, its disposable profile, and every document opened by a test.
@NullMarked
final class HeadlessLibreOffice implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeadlessLibreOffice.class);
    private static final Duration STARTUP_TIMEOUT = Duration.ofMinutes(1);

    private final Path artifacts;
    private final List<XTextDocument> documents = new ArrayList<>();
    private Optional<XComponentContext> context = Optional.empty();
    private Optional<Process> process = Optional.empty();

    HeadlessLibreOffice(Path artifacts) {
        this.artifacts = artifacts;
    }

    void start(Path profile) throws Exception {
        Files.createDirectories(artifacts);
        int port;
        try (ServerSocket socket = new ServerSocket(0, 0, InetAddress.getLoopbackAddress())) {
            port = socket.getLocalPort();
        }
        String executable = Optional.ofNullable(System.getenv("LIBREOFFICE_EXECUTABLE"))
                                    .filter(value -> !value.isBlank())
                                    .orElse("soffice");
        Path log = artifacts.resolve("libreoffice.log");
        Process writer = new ProcessBuilder(
                executable,
                "-env:UserInstallation=" + profile.toUri().toASCIIString(),
                "--headless", "--nologo", "--nodefault", "--nofirststartwizard", "--norestore",
                "--accept=socket,host=127.0.0.1,port=" + port + ",tcpNoDelay=1;urp;StarOffice.ServiceManager")
                .redirectErrorStream(true)
                .redirectOutput(log.toFile())
                .start();
        process = Optional.of(writer);

        // UNO bootstrap adds its own entries to this map.
        XComponentContext localContext = Bootstrap.createInitialComponentContext(new HashMap<>());
        XUnoUrlResolver resolver = query(XUnoUrlResolver.class, localContext.getServiceManager()
                                                                            .createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", localContext));
        long deadline = System.nanoTime() + STARTUP_TIMEOUT.toNanos();
        while (System.nanoTime() < deadline) {
            assertTrue(writer.isAlive(), () -> "LibreOffice exited during startup; see " + log);
            try {
                Object remote = resolver.resolve("uno:socket,host=127.0.0.1,port=" + port + ";urp;StarOffice.ComponentContext");
                context = Optional.of(query(XComponentContext.class, remote));
                return;
            } catch (NoConnectException exception) {
                LOGGER.debug("Waiting for the isolated LibreOffice UNO listener", exception);
                TimeUnit.MILLISECONDS.sleep(200);
            }
        }
        fail("LibreOffice UNO listener did not start within " + STARTUP_TIMEOUT + "; see " + log);
    }

    XComponentContext context() {
        return context.orElseThrow();
    }

    XTextDocument load(Path path) throws com.sun.star.uno.Exception {
        return loadUrl(path.toUri().toASCIIString());
    }

    XTextDocument newDocument() throws com.sun.star.uno.Exception {
        return loadUrl("private:factory/swriter");
    }

    private XTextDocument loadUrl(String url) throws com.sun.star.uno.Exception {
        XComponentContext remote = context();
        XComponentLoader loader = query(XComponentLoader.class, remote.getServiceManager()
                                                                      .createInstanceWithContext("com.sun.star.frame.Desktop", remote));
        XTextDocument document = query(XTextDocument.class, loader.loadComponentFromURL(url, "_blank", 0,
                new PropertyValue[] {property("Hidden", true), property("UpdateDocMode", UpdateDocMode.NO_UPDATE), property("MacroExecutionMode", MacroExecMode.NEVER_EXECUTE)}));
        documents.add(document);
        return document;
    }

    XTextDocument saveAndReload(XTextDocument document, Path target) throws com.sun.star.uno.Exception {
        save(document, target);
        query(XComponent.class, document).dispose();
        documents.remove(document);
        return load(target);
    }

    private void save(XTextDocument document, Path target) throws com.sun.star.io.IOException {
        query(XStorable.class, document).storeToURL(target.toUri().toASCIIString(),
                new PropertyValue[] {property("FilterName", "writer8"), property("Overwrite", true)});
    }

    /// Preserve even a failing test's documents for inspection in the CI artifact.
    void saveAndCloseDocuments(String testName) throws com.sun.star.io.IOException {
        try {
            int index = 0;
            for (XTextDocument document : documents) {
                save(document, artifacts.resolve(testName + "-" + index++ + ".odt"));
            }
        } finally {
            for (XTextDocument document : documents) {
                query(XComponent.class, document).dispose();
            }
            documents.clear();
        }
    }

    @Override
    public void close() throws Exception {
        if (process.isEmpty()) {
            return;
        }
        Process writer = process.orElseThrow();
        List<ProcessHandle> descendants = writer.descendants().toList();
        try {
            if (context.isPresent() && writer.isAlive()) {
                XComponentContext remote = context.orElseThrow();
                XDesktop desktop = query(XDesktop.class, remote.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", remote));
                assertTrue(desktop.terminate(), "Isolated Writer refused to terminate after closing its documents");
            }
        } finally {
            if (!writer.waitFor(10, TimeUnit.SECONDS)) {
                descendants.forEach(ProcessHandle::destroyForcibly);
                writer.destroyForcibly();
                assertTrue(writer.waitFor(10, TimeUnit.SECONDS), "LibreOffice did not exit after forced termination");
            }
            // The launcher can exit before soffice.bin releases the Windows profile locks.
            for (ProcessHandle descendant : descendants) {
                if (descendant.isAlive()) {
                    descendant.destroyForcibly();
                }
                descendant.onExit().get(10, TimeUnit.SECONDS);
            }
        }
    }

    static PropertyValue property(String name, Object value) {
        PropertyValue property = new PropertyValue();
        property.Name = name;
        property.Value = value;
        return property;
    }

    static <T> T query(Class<T> type, Object object) {
        return assertInstanceOf(type, UnoRuntime.queryInterface(type, object), () -> "Missing UNO interface " + type.getName());
    }
}
