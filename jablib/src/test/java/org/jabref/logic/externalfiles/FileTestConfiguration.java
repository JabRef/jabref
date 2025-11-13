package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jilt.Builder;
import org.jilt.BuilderStyle;
import org.jilt.Opt;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import static org.mockito.Mockito.when;

public class FileTestConfiguration {

    public final Path sourceBibDir;
    public final Path sourceFile;
    public final BibDatabaseContext sourceContext;
    public final BibEntry sourceEntry;

    public final Path targetBibDir;
    public final BibDatabaseContext targetContext;
    public final BibEntry targetEntry;

    @NonNull private final Path tempDir;
    @Nullable private final Path mainFileDir;
    @Nullable private final Path sourceLibrarySpecificFileDir;
    @Nullable private final Path sourceUserSpecificFileDir;

    /// @param tempDir the temporary directory to use
    /// @param filePreferences the file preferences to modify
    /// @param sourceFileDir relative to tempDir
    @Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)
    public FileTestConfiguration(
            Path tempDir,

            FilePreferences filePreferences,
            @Opt String mainFileDirectory,
            Boolean shouldStoreFilesRelativeToBibFile,
            Boolean shouldAdjustOrCopyLinkedFilesOnTransfer,

            String sourceBibDir,
            @Opt String sourceLibrarySpecificFileDirectory,
            @Opt String sourceUserSpecificFileDirectory,
            String sourceFileDir,
            TestFileLinkMode testFileLinkMode,

            String targetBibDir,
            @Opt String targetLibrarySpecificFileDirectory,
            @Opt String targetUserSpecificFileDirectory
    ) throws IOException {
        this.tempDir = tempDir;
        this.sourceBibDir = tempDir.resolve(sourceBibDir);
        Files.createDirectories(this.sourceBibDir);

        Path sourceFileDirPath = tempDir.resolve(sourceFileDir);
        Files.createDirectories(sourceFileDirPath);

        sourceContext = new BibDatabaseContext(new BibDatabase());
        sourceContext.setDatabasePath(this.sourceBibDir.resolve("source.bib"));

        this.sourceFile = sourceFileDirPath.resolve("test.pdf");
        Files.createFile(this.sourceFile);

        if (mainFileDirectory == null) {
            when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
            this.mainFileDir = null;
        } else {
            this.mainFileDir = tempDir.resolve(mainFileDirectory);
            when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(this.mainFileDir));
        }
        if (sourceLibrarySpecificFileDirectory == null) {
            this.sourceLibrarySpecificFileDir = null;
        } else {
            this.sourceLibrarySpecificFileDir = tempDir.resolve(sourceLibrarySpecificFileDirectory);
            sourceContext.getMetaData().setLibrarySpecificFileDirectory(sourceLibrarySpecificFileDir.toString());
        }
        if (sourceUserSpecificFileDirectory == null) {
            this.sourceUserSpecificFileDir = null;
        } else {
            this.sourceUserSpecificFileDir = tempDir.resolve(sourceUserSpecificFileDirectory);
            sourceContext.getMetaData().setUserFileDirectory("testuser", sourceUserSpecificFileDir.toString());
        }

        Path resolvedSourceFile = sourceFileDirPath.resolve("test.pdf");
        Path fileLinkPath = convertLink(resolvedSourceFile, testFileLinkMode);
        String fileLink = fileLinkPath.toString();
        LinkedFile linkedFile = new LinkedFile("", fileLink, "PDF");
        sourceEntry = new BibEntry().withFiles(List.of(linkedFile));
        sourceContext.getDatabase().insertEntry(sourceEntry);

        when(filePreferences.getUserAndHost()).thenReturn("testuser@testhost");
        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(shouldStoreFilesRelativeToBibFile);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(shouldAdjustOrCopyLinkedFilesOnTransfer);

        this.targetBibDir = tempDir.resolve(targetBibDir);
        Files.createDirectories(this.targetBibDir);
        targetEntry = new BibEntry(sourceEntry);
        targetContext = new BibDatabaseContext(new BibDatabase(List.of(targetEntry)));
        targetContext.setDatabasePath(this.targetBibDir.resolve("target.bib"));
        if (targetLibrarySpecificFileDirectory != null) {
            targetContext.getMetaData().setLibrarySpecificFileDirectory(targetLibrarySpecificFileDirectory);
        }
        if (targetUserSpecificFileDirectory != null) {
            targetContext.getMetaData().setUserFileDirectory("testuser", targetUserSpecificFileDirectory);
        }
    }

    Path convertLink(Path path, TestFileLinkMode testFileLinkMode) {
        Path resolvedSourceFile = tempDir.resolve(path);
        return switch (testFileLinkMode) {
            case ABSOLUTE ->
                    resolvedSourceFile;
            case RELATIVE_TO_BIB ->
                    this.sourceBibDir.relativize(resolvedSourceFile);
            case RELATIVE_TO_MAIN_FILE_DIR ->
                    tempDir.resolve(this.mainFileDir).relativize(resolvedSourceFile);
            case RELATIVE_TO_LIBRARY_SPECIFIC_DIR ->
                    tempDir.resolve(this.sourceLibrarySpecificFileDir).relativize(resolvedSourceFile);
            case RELATIVE_TO_USER_SPECIFIC_DIR ->
                    tempDir.resolve(this.sourceUserSpecificFileDir).relativize(resolvedSourceFile);
        };
    }

    enum TestFileLinkMode {
        ABSOLUTE,
        RELATIVE_TO_MAIN_FILE_DIR,
        RELATIVE_TO_BIB,
        RELATIVE_TO_LIBRARY_SPECIFIC_DIR,
        RELATIVE_TO_USER_SPECIFIC_DIR
    }
}
