package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

import static org.mockito.Mockito.when;

public class FileTestConfiguration {

    public final Path sourceDir;
    public final Path sourceFile;
    public final BibDatabaseContext sourceContext;
    public final BibEntry sourceEntry;

    public final Path targetDir;
    public final BibDatabaseContext targetContext;
    public final BibEntry targetEntry;

    /// @param tempDir the temporary directory to use
    /// @param filePreferences the file preferences to modify
    @Builder(style = BuilderStyle.STAGED_PRESERVING_ORDER)
    public FileTestConfiguration(
            Path tempDir,
            FilePreferences filePreferences,
            String sourceDir,
            String sourceFile,
            String targetDir,
            Boolean shouldStoreFilesRelativeToBibFile,
            Boolean shouldAdjustOrCopyLinkedFilesOnTransfer
    ) throws IOException {
        this.sourceDir = tempDir.resolve(sourceDir);
        Files.createDirectories(this.sourceDir);
        this.sourceFile = this.sourceDir.resolve(sourceFile);
        Files.createDirectories(this.sourceFile.getParent());
        Files.createFile(this.sourceFile);
        LinkedFile linkedFile = new LinkedFile("", sourceFile, "PDF");
        sourceEntry = new BibEntry()
                .withFiles(List.of(linkedFile));
        sourceContext = new BibDatabaseContext(new BibDatabase(List.of(sourceEntry)));
        sourceContext.setDatabasePath(this.sourceDir.resolve("source.bib"));

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(shouldStoreFilesRelativeToBibFile);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(shouldAdjustOrCopyLinkedFilesOnTransfer);

        this.targetDir = tempDir.resolve(targetDir);
        Files.createDirectories(this.targetDir);
        targetEntry = new BibEntry(sourceEntry);
        targetContext = new BibDatabaseContext(new BibDatabase(List.of(targetEntry)));
        targetContext.setDatabasePath(this.targetDir.resolve("target.bib"));
    }
}
