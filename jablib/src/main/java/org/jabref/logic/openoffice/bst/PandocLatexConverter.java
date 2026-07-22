package org.jabref.logic.openoffice.bst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Converts LaTeX fragments to HTML by shelling out to pandoc.
///
/// The pandoc path defaults to `"pandoc"` (system PATH) for testing.
@NullMarked
public class PandocLatexConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(PandocLatexConverter.class);

    // TEST ONLY — replace with OpenOfficePreferences.getPandocPath() before PR
    private static final String DEFAULT_PANDOC_PATH = "pandoc";

    private final String pandocPath;

    public PandocLatexConverter() {
        this(DEFAULT_PANDOC_PATH);
    }

    public PandocLatexConverter(String pandocPath) {
        this.pandocPath = pandocPath;
    }

    public boolean isAvailable() {
        try {
            Process p = new ProcessBuilder(pandocPath, "--version")
                    .redirectErrorStream(true).start();
            return p.waitFor(5, TimeUnit.SECONDS) && p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /// Converts a LaTeX fragment to HTML via pandoc stdin/stdout (no temp files).
    /// Both stdout and stderr are drained to prevent pipe-buffer deadlock.
    /// Throws [IOException] if pandoc exits non-zero, surfacing the real error message.
    public String latexToHtml(String latex) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(pandocPath, "-f", "latex", "-t", "html", "--wrap=none").start();

        try (var out = p.getOutputStream()) {
            out.write(latex.getBytes(StandardCharsets.UTF_8));
        }

        // Drain BOTH streams before waitFor() to avoid pipe-buffer deadlock
        String html = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String err  = new String(p.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("pandoc timed out");
        }

        int exit = p.exitValue();
        if (exit != 0) {
            throw new IOException("pandoc failed (exit " + exit + "): " + err);
        }
        if (!err.isBlank()) {
            LOGGER.warn("pandoc stderr: {}", err);
        }
        return html;
    }
}
