package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// All paths are relative to tmpDir
public class BibTestConfiguration {

    @NonNull final Path tempDir;
    @NonNull final Path bibDir;
    @NonNull final Path pdfFile;
    @Nullable final Path librarySpecificFileDir;
    @Nullable final Path userSpecificFileDir;
    final FileTestConfiguration.TestFileLinkMode fileLinkMode;

    @Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)
    BibTestConfiguration(
            Path tempDir,
            String bibDir,
            @Opt String librarySpecificFileDir,
            @Opt String userSpecificFileDir,
            String pdfFileDir,
            FileTestConfiguration.TestFileLinkMode fileLinkMode
    ) throws IOException {
        this.tempDir = tempDir;

        this.fileLinkMode = fileLinkMode;
        this.bibDir = tempDir.resolve(bibDir);
        Files.createDirectories(this.bibDir);

        this.pdfFile = tempDir.resolve(pdfFileDir).resolve("test.pdf");
        Files.createDirectories(this.pdfFile.getParent());
        if (!Files.exists(this.pdfFile)) {
            Files.createFile(this.pdfFile);
        }

        if (librarySpecificFileDir == null) {
            this.librarySpecificFileDir = null;
        } else {
            this.librarySpecificFileDir = tempDir.resolve(librarySpecificFileDir);
        }
        if (userSpecificFileDir == null) {
            this.userSpecificFileDir = null;
        } else {
            this.userSpecificFileDir = tempDir.resolve(userSpecificFileDir);
        }
    }

    void updateContext(BibDatabaseContext context, @Nullable Path mainFileDir) {
        context.setDatabasePath(this.bibDir.resolve("test.bib"));

        if (this.librarySpecificFileDir != null) {
            context.getMetaData().setLibrarySpecificFileDirectory(this.librarySpecificFileDir.toString());
        }
        if (this.userSpecificFileDir != null) {
            context.getMetaData().setUserFileDirectory("testuser", this.userSpecificFileDir.toString());
        }

        Path fileLinkPath = convertLink(mainFileDir);
        String fileLink = fileLinkPath.toString();
        LinkedFile linkedFile = new LinkedFile("", fileLink, "PDF");
        BibEntry entry = new BibEntry().withFiles(List.of(linkedFile));
        context.getDatabase().insertEntry(entry);
    }

    private Path convertLink(@Nullable Path mainFileDir) {
        return switch (fileLinkMode) {
            case ABSOLUTE ->
                    pdfFile;
            case RELATIVE_TO_BIB ->
                    this.bibDir.relativize(pdfFile);
            case RELATIVE_TO_MAIN_FILE_DIR ->
                    tempDir.resolve(mainFileDir).relativize(pdfFile);
            case RELATIVE_TO_LIBRARY_SPECIFIC_DIR ->
                    tempDir.resolve(this.librarySpecificFileDir).relativize(pdfFile);
            case RELATIVE_TO_USER_SPECIFIC_DIR ->
                    tempDir.resolve(this.userSpecificFileDir).relativize(pdfFile);
        };
    }
}
