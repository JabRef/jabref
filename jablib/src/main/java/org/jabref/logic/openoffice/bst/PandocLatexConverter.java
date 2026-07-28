package org.jabref.logic.openoffice.bst;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.jabref.logic.os.OS;
import org.jabref.logic.util.HeadlessExecutorService;
import org.jabref.logic.util.StreamGobbler;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Converts LaTeX fragments to HTML by shelling out to pandoc.
///
/// The path to pandoc is read from [OpenOfficePreferences] and can be changed by the user
/// via the OpenOffice panel settings menu. Use [autoDetect] to find pandoc automatically.
@NullMarked
public class PandocLatexConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PandocLatexConverter.class);

    /// Candidate paths tried by [autoDetect], in priority order, per OS.
    private static final List<String> WINDOWS_CANDIDATES = List.of(
            "pandoc"
            // MSI installer adds to PATH; AppData path varies per user so we leave PATH as the only candidate
    );
    private static final List<String> MACOS_CANDIDATES = List.of(
            "pandoc",
            "/usr/local/bin/pandoc",
            "/opt/homebrew/bin/pandoc"
    );
    private static final List<String> LINUX_CANDIDATES = List.of(
            "pandoc",
            "/usr/bin/pandoc",
            "/usr/local/bin/pandoc"
    );

    private final String pandocPath;

    public PandocLatexConverter(String pandocPath) {
        this.pandocPath = pandocPath;
    }

    /// Returns the first pandoc executable found in the OS-specific candidate list,
    /// or [Optional.empty] if none responds to `--version` within 5 seconds.
    public static Optional<String> autoDetect() {
        List<String> candidates = OS.WINDOWS ? WINDOWS_CANDIDATES
                                             : OS.OS_X ? MACOS_CANDIDATES
                                                       : LINUX_CANDIDATES;
        return candidates.stream()
                         .filter(PandocLatexConverter::probeCandidate)
                         .findFirst();
    }

    private static boolean probeCandidate(String candidate) {
        try {
            Process p = new ProcessBuilder(candidate, "--version")
                    .redirectErrorStream(true)
                    .start();
            boolean finished = p.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.debug("pandoc candidate '{}' not usable: {}", candidate, e.getMessage(), e);
            return false;
        }
    }

    public boolean isAvailable() {
        return probeCandidate(pandocPath);
    }

    /// Converts a LaTeX fragment to HTML via pandoc stdin/stdout (no temp files).
    /// Both stdout and stderr are drained to prevent pipe-buffer deadlock.
    /// Throws [IOException] if pandoc exits non-zero, surfacing the real error message.
    public String latexToHtml(String latex) throws IOException, InterruptedException {
        Process pandocProcess = new ProcessBuilder(pandocPath, "-f", "latex", "-t", "html", "--wrap=none").start();

        // Start gobblers to prevent blocking on full buffers
        StringBuilder stdoutBuf = new StringBuilder();
        StringBuilder stderrBuf = new StringBuilder();
        StreamGobbler outGobbler = new StreamGobbler(pandocProcess.getInputStream(), line -> {
            stdoutBuf.append(line).append('\n');
        });
        StreamGobbler errGobbler = new StreamGobbler(pandocProcess.getErrorStream(), line -> {
            stderrBuf.append(line).append('\n');
        });
        HeadlessExecutorService.INSTANCE.execute(outGobbler);
        HeadlessExecutorService.INSTANCE.execute(errGobbler);

        // Write LaTeX to stdin and close to signal EOF
        try (OutputStream out = pandocProcess.getOutputStream()) {
            out.write(latex.getBytes(StandardCharsets.UTF_8));
        }

        if (!pandocProcess.waitFor(30, TimeUnit.SECONDS)) {
            pandocProcess.destroyForcibly();
            throw new IOException("pandoc timed out");
        }

        String html = stdoutBuf.toString();
        String err = stderrBuf.toString();

        int exit = pandocProcess.exitValue();
        if (exit != 0) {
            throw new IOException("pandoc failed (exit " + exit + "): " + err);
        }
        if (!err.isBlank()) {
            LOGGER.warn("pandoc stderr: {}", err);
        }
        return html;
    }
}
