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

import static org.mockito.Mockito.when;

public class FileTestConfiguration {

    public final Path sourceBibDir;
    public final Path sourceFile;
    public final BibDatabaseContext sourceContext;
    public final BibEntry sourceEntry;

    public final Path targetBibDir;
    public final BibDatabaseContext targetContext;
    public final BibEntry targetEntry;

    /// @param tempDir the temporary directory to use
    /// @param filePreferences the file preferences to modify
    /// @param sourceFileDir relative to tempDir
    /// @param storeSourceFileRelative should the path to the test file stored relative (or absolute)
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
            boolean storeSourceFileRelative,

            String targetBibDir,
            @Opt String targetLibrarySpecificFileDirectory,
            @Opt String targetUserSpecificFileDirectory
    ) throws IOException {
        this.sourceBibDir = tempDir.resolve(sourceBibDir);
        Files.createDirectories(this.sourceBibDir);

        Path sourceFileDirPath = tempDir.resolve(sourceFileDir);
        Files.createDirectories(sourceFileDirPath);

        this.sourceFile = sourceFileDirPath.resolve("test.pdf");
        Files.createFile(this.sourceFile);

        String fileLink;
        if (storeSourceFileRelative) {
            fileLink = Path.of(sourceFileDir).resolve("test.pdf").toString();
        } else {
            fileLink = sourceFile.toString();
        }
        LinkedFile linkedFile = new LinkedFile("", fileLink, "PDF");
        sourceEntry = new BibEntry()
                .withFiles(List.of(linkedFile));
        sourceContext = new BibDatabaseContext(new BibDatabase(List.of(sourceEntry)));
        sourceContext.setDatabasePath(this.sourceBibDir.resolve("source.bib"));
        if (sourceLibrarySpecificFileDirectory != null) {
            sourceContext.getMetaData().setLibrarySpecificFileDirectory(sourceLibrarySpecificFileDirectory);
        }
        if (sourceUserSpecificFileDirectory != null) {
            sourceContext.getMetaData().setUserFileDirectory("testuser", sourceUserSpecificFileDirectory);
        }

        if (mainFileDirectory == null) {
            when(filePreferences.getMainFileDirectory()).thenReturn(Optional.empty());
        } else {
            Path mainFileDirectoryPath = tempDir.resolve(mainFileDirectory);
            when(filePreferences.getMainFileDirectory()).thenReturn(Optional.of(mainFileDirectoryPath));
        }

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
}
