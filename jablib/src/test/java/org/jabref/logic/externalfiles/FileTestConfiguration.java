package org.jabref.logic.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.FilePreferences;

import org.jilt.Builder;
import org.jilt.BuilderStyle;

import static org.mockito.Mockito.when;

public class FileTestConfiguration {

    public final Path sourceDir;
    public final Path targetDir;

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

        this.targetDir = tempDir.resolve(targetDir);
        Files.createDirectories(this.targetDir);

        when(filePreferences.shouldStoreFilesRelativeToBibFile()).thenReturn(shouldStoreFilesRelativeToBibFile);
        when(filePreferences.shouldAdjustOrCopyLinkedFilesOnTransfer()).thenReturn(shouldAdjustOrCopyLinkedFilesOnTransfer);
    }
}
