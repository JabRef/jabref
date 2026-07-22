package org.jabref.logic.openoffice.bst;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;

/// Converts LaTeX fragments to HTML by shelling out to pandoc.
///
/// The pandoc path is hardcoded for testing — replace with a preference lookup before any PR.
@NullMarked
public class PandocLatexConverter {

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
    public String latexToHtml(String latex) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(pandocPath, "-f", "latex", "-t", "html", "--wrap=none").start();
        try (var out = p.getOutputStream()) {
            out.write(latex.getBytes(StandardCharsets.UTF_8));
        }
        String html = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        if (!p.waitFor(30, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            throw new IOException("pandoc timed out");
        }
        return html;
    }
}
