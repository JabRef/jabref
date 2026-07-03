package org.jabref.logic.cleanup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenamePdfCleanup implements CleanupJob {
    private static final Logger LOGGER = LoggerFactory.getLogger(RenamePdfCleanup.class);

    private static final String PDF_EXTENSION = "pdf";

    private final Supplier<BibDatabaseContext> databaseContext;
    private final boolean onlyRelativePaths;
    private final boolean onlyPdfFiles;
    private final boolean preserveCustomSuffix;
    private final FilePreferences filePreferences;

    public RenamePdfCleanup(boolean onlyRelativePaths,
                            @NonNull Supplier<BibDatabaseContext> databaseContext,
                            FilePreferences filePreferences) {
        this(onlyRelativePaths, false, databaseContext, filePreferences);
    }

    public RenamePdfCleanup(boolean onlyRelativePaths,
                            boolean onlyPdfFiles,
                            @NonNull Supplier<BibDatabaseContext> databaseContext,
                            FilePreferences filePreferences) {
        this(onlyRelativePaths, onlyPdfFiles, false, databaseContext, filePreferences);
    }

    public RenamePdfCleanup(boolean onlyRelativePaths,
                            boolean onlyPdfFiles,
                            boolean preserveCustomSuffix,
                            @NonNull Supplier<BibDatabaseContext> databaseContext,
                            FilePreferences filePreferences) {
        this.databaseContext = databaseContext;
        this.onlyRelativePaths = onlyRelativePaths;
        this.onlyPdfFiles = onlyPdfFiles;
        this.preserveCustomSuffix = preserveCustomSuffix;
        this.filePreferences = filePreferences;
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry, Consumer<Runnable> mutationScheduler) {
        List<LinkedFile> files = entry.getFiles();

        Optional<String> detectedOriginalPattern = preserveCustomSuffix
                                                   ? detectOriginalPattern(files)
                                                   : Optional.empty();

        boolean changed = false;
        for (LinkedFile file : files) {
            if (onlyRelativePaths && Path.of(file.getLink()).isAbsolute()) {
                continue;
            }

            if (onlyPdfFiles && !isPdf(file)) {
                continue;
            }

            LinkedFileHandler fileHandler = new LinkedFileHandler(file, entry, databaseContext.get(), filePreferences, preserveCustomSuffix, detectedOriginalPattern);
            try {
                boolean changedFile = fileHandler.renameToSuggestedName();
                if (changedFile) {
                    changed = true;
                }
            } catch (IOException exception) {
                // There is no exception logged here, because the stack trace can get very large (and is not helpful)
                // The only "real" information lost is i) the absolute path of the source file and ii) the absolute path of the target file.
                LOGGER.error("Error while renaming file {}", file.getLink());
            }
        }

        if (changed) {
            return FileFieldCleanupUpdater.updateFileField(entry, files, mutationScheduler);
        }

        return List.of();
    }

    @Override
    public List<FieldChange> cleanup(BibEntry entry) {
        return cleanup(entry, Runnable::run);
    }

    /// Determines whether the linked file is a PDF, based on its file extension.
    private static boolean isPdf(LinkedFile file) {
        return FileUtil.getFileExtension(file.getLink())
                       .map(extension -> extension.equalsIgnoreCase(PDF_EXTENSION))
                       .orElse(false);
    }

    /// Detects the base name pattern that was originally applied to the entry's linked files, so a user-added suffix
    /// can be preserved even after the configured pattern changed (e.g. the citation key was edited).
    ///
    /// The detected pattern is the longest run of leading tokens (delimited by `-`, `_` or space) shared by the base
    /// names of *all* files. For example, `article-suffix1.jpg` and `article-suffix2.pdf` yield `ogart`. When the files
    /// share no leading token, an empty string is returned, which disables suffix preservation. With fewer than two
    /// files there is nothing to compare against, so detection is skipped (empty `Optional`) and the caller falls back
    /// to comparing against the freshly generated pattern.
    static Optional<String> detectOriginalPattern(List<LinkedFile> files) {
        if (files.size() < 2) {
            return Optional.empty();
        }

        List<String> baseNames = files.stream()
                                      .map(file -> FileUtil.getBaseName(file.getLink()))
                                      .toList();
        return Optional.of(commonLeadingTokenPrefix(baseNames));
    }

    private static boolean isSuffixSeparator(char character) {
        return character == '-' || character == '_' || character == ' ';
    }

    /// Returns the longest leading substring shared by all base names that ends on a token boundary (the end of a
    /// name or a separator character) in every name, or an empty string when the names share no leading token.
    static String commonLeadingTokenPrefix(List<String> baseNames) {
        String first = baseNames.getFirst();

        // Longest common (character-level) prefix across all names.
        int commonLength = first.length();
        for (String name : baseNames) {
            commonLength = Math.min(commonLength, name.length());
            int index = 0;
            while (index < commonLength && name.charAt(index) == first.charAt(index)) {
                index++;
            }
            commonLength = index;
        }

        // Trim the common prefix back to the largest position that is a token boundary in every name.
        for (int cut = commonLength; cut > 0; cut--) {
            if (cut < commonLength) {
                // Inside the common prefix all names share the same character, so checking 'first' is enough.
                if (isSuffixSeparator(first.charAt(cut))) {
                    return first.substring(0, cut);
                }
            } else if (isTokenBoundaryInAll(baseNames, cut)) {
                return first.substring(0, cut);
            }
        }
        return "";
    }

    private static boolean isTokenBoundaryInAll(List<String> baseNames, int position) {
        for (String name : baseNames) {
            if (position != name.length() && !isSuffixSeparator(name.charAt(position))) {
                return false;
            }
        }
        return true;
    }
}
