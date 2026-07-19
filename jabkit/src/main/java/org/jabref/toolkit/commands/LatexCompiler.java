package org.jabref.toolkit.commands;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.l10n.Localization;
import org.jabref.toolkit.exception.CliException;

import picocli.CommandLine;

/// Compiles a LaTeX document with `latexmk` (which drives the pdflatex/bibtex passes itself, and
/// re-runs bibtex when the `.bib` changes) and reports the resulting page count. The count is read
/// from the pdflatex `.log` (`Output written on X.pdf (N pages, ...)`) rather than by parsing the
/// PDF, so jabkit needs no PDF library. Prefers a local `latexmk`; falls back to the Island of TeX
/// `texlive/texlive` Docker image when no local TeX distribution is present.
class LatexCompiler {

    /// pdflatex writes this line once per run; the last occurrence reflects the final pass.
    private static final Pattern OUTPUT_PAGES = Pattern.compile("Output written on \\S+\\.pdf \\((\\d+) pages?");
    private static final String DOCKER_IMAGE = "texlive/texlive";
    private static final int TIMEOUT_SECONDS = 180;

    private final Path workingDir;
    private final String texFileName;
    private final boolean useDocker;

    LatexCompiler(Path texFile) {
        this.workingDir = texFile.toAbsolutePath().getParent();
        this.texFileName = texFile.getFileName().toString();
        this.useDocker = !isLocalLatexmkAvailable();
    }

    boolean usesDocker() {
        return useDocker;
    }

    /// Compiles the document and returns the page count of the produced PDF.
    int compileAndCountPages() throws CliException {
        int exitCode = runLatexmk();
        return pageCountOrFail(exitCode, readLog(), texFileName);
    }

    /// Turns a compile run into a page count, or fails. A non-zero latexmk exit is a hard failure
    /// even when the log still carries a page count: pdflatex writes a PDF in nonstopmode despite
    /// errors, and shortening must not proceed to rewrite the `.bib` on top of a broken compile.
    /// A zero exit must still yield a page count (a missing one means no PDF was produced).
    static int pageCountOrFail(int exitCode, String log, String texFileName) throws CliException {
        if (exitCode != 0) {
            throw new CliException(
                    "LaTeX compilation of '%s' failed (latexmk exit %d)".formatted(texFileName, exitCode),
                    Localization.lang("LaTeX compilation of '%0' failed. Run latexmk manually to see the error.", texFileName),
                    CommandLine.ExitCode.SOFTWARE);
        }
        OptionalInt pages = parsePageCount(log);
        if (pages.isEmpty()) {
            throw new CliException(
                    "latexmk reported success but produced no page count for '%s'".formatted(texFileName),
                    Localization.lang("LaTeX compilation of '%0' failed. Run latexmk manually to see the error.", texFileName),
                    CommandLine.ExitCode.SOFTWARE);
        }
        return pages.getAsInt();
    }

    private int runLatexmk() throws CliException {
        try {
            Process process = new ProcessBuilder(buildCommand())
                    .directory(workingDir.toFile())
                    .redirectErrorStream(true)
                    .start();
            // Drain the output so the process cannot block on a full pipe buffer.
            process.getInputStream().readAllBytes();
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new CliException(
                        "latexmk timed out compiling '%s'".formatted(texFileName),
                        Localization.lang("Compiling '%0' timed out.", texFileName),
                        CommandLine.ExitCode.SOFTWARE);
            }
            return process.exitValue();
        } catch (IOException e) {
            throw new CliException(
                    "Could not run latexmk: " + e.getMessage(),
                    Localization.lang("Could not run latexmk. Install a TeX distribution or Docker."),
                    e, CommandLine.ExitCode.SOFTWARE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CliException(
                    "Interrupted while compiling '%s'".formatted(texFileName),
                    Localization.lang("Compiling '%0' was interrupted.", texFileName),
                    e, CommandLine.ExitCode.SOFTWARE);
        }
    }

    private List<String> buildCommand() {
        if (useDocker) {
            // ponytail: Docker fallback is untested in this TeX-less path; local latexmk is used when present.
            List<String> command = new ArrayList<>(List.of("docker", "run", "--rm"));
            // Without --user, the bind-mounted artifacts (pdf/log/aux) are created as the image's
            // default user (often root), which the invoking user then cannot read back or clean up.
            unixUserFlag().ifPresent(user -> {
                command.add("--user");
                command.add(user);
            });
            command.addAll(List.of(
                    "-v", workingDir + ":/work", "-w", "/work",
                    DOCKER_IMAGE,
                    "latexmk", "-pdf", "-interaction=nonstopmode", texFileName));
            return command;
        }
        return List.of("latexmk", "-pdf", "-interaction=nonstopmode", texFileName);
    }

    /// The invoking user's `uid:gid` for Docker's `--user`, read from the (self-created) working
    /// directory. Empty on non-Unix filesystems, where Docker manages bind-mount ownership itself.
    private Optional<String> unixUserFlag() {
        try {
            Object uid = Files.getAttribute(workingDir, "unix:uid");
            Object gid = Files.getAttribute(workingDir, "unix:gid");
            return Optional.of(uid + ":" + gid);
        } catch (IOException | UnsupportedOperationException e) {
            return Optional.empty();
        }
    }

    /// Read with Latin-1: TeX logs are not guaranteed UTF-8, and Latin-1 maps every byte without
    /// throwing while leaving the ASCII "Output written on" line intact.
    private String readLog() {
        Path log = workingDir.resolve(texFileName.replaceFirst("\\.tex$", "") + ".log");
        try {
            return Files.exists(log) ? Files.readString(log, StandardCharsets.ISO_8859_1) : "";
        } catch (IOException e) {
            return "";
        }
    }

    static OptionalInt parsePageCount(String log) {
        Matcher matcher = OUTPUT_PAGES.matcher(log);
        int pages = -1;
        while (matcher.find()) {
            pages = Integer.parseInt(matcher.group(1));
        }
        return pages < 0 ? OptionalInt.empty() : OptionalInt.of(pages);
    }

    private static boolean isLocalLatexmkAvailable() {
        String path = System.getenv("PATH");
        if (path == null) {
            return false;
        }
        for (String dir : path.split(File.pathSeparator)) {
            if (!dir.isBlank()
                    && (Files.isExecutable(Path.of(dir, "latexmk")) || Files.isExecutable(Path.of(dir, "latexmk.exe")))) {
                return true;
            }
        }
        return false;
    }
}
