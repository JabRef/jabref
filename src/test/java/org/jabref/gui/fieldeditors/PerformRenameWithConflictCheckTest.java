package test.java.org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.externalfiles.LinkedFileHandler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.logic.util.io.FileNameUniqueness;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockitoAnnotations;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
public class PerformRenameWithConflictCheckTest {

    @TempDir
    Path tempDir;  // JUnit 5 annotation for creating temporary files/folders

    private LinkedFile linkedFile;
    private BibEntry entry;
    private BibDatabaseContext databaseContext;
    private TaskExecutor taskExecutor;
    private DialogService dialogService;
    private LinkedFileHandler linkedFileHandler;
    private LinkedFileViewModel viewModel;

    private final DialogService dialogServiceMock = mock(DialogService.class);
    private final LinkedFileHandler linkedFileHandlerMock = mock(LinkedFileHandler.class);

    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Mock dependencies
        // linkedFile = mock(LinkedFile.class);
        entry = mock(BibEntry.class);
        databaseContext = mock(BibDatabaseContext.class);
        taskExecutor = mock(TaskExecutor.class);
        dialogService = mock(DialogService.class);
        linkedFileHandler = mock(LinkedFileHandler.class);

        // Mock preferences
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(filePreferences.confirmDeleteLinkedFile()).thenReturn(true);
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getXmpPreferences()).thenReturn(mock(XmpPreferences.class));

        // Mock database context behavior (use a temporary path)
        when(databaseContext.getFileDirectories(any())).thenReturn(List.of(tempDir));

        // Create temporary file
        Path existingFile = tempDir.resolve("existingFile.pdf");
        Assertions.assertDoesNotThrow(() -> {
            Files.createFile(existingFile);  // Create the real file
        }, "Failed to create temporary file: ");

        Path fileToRename = tempDir.resolve("fileToRename.pdf");
        Assertions.assertDoesNotThrow(() -> {
            Files.createFile(fileToRename);  // Create the real file
        }, "Failed to create temporary file: ");

        // Create a real LinkedFile object
        LinkedFile linkedFile = new LinkedFile("desc", fileToRename, "pdf");

        if (linkedFile.getLink() == null) {
            throw new IllegalStateException("linkedFile.getLink() returned null");
        }
    
        viewModel = new LinkedFileViewModel(linkedFile, entry, databaseContext, taskExecutor, dialogServiceMock, preferences);
    }

    // Test 1: No conflict, renames the file
    @Test
    void performRenameWithConflictCheckWhenNoConflictRenamesFile() throws IOException {
        Path newFileName = tempDir.resolve("newFileName.pdf");

        // Simulate no conflict, check that the file doesn't exist before renaming
        assertFalse(Files.exists(newFileName), "File should not exist before renaming");

        // Call the method to rename the file
        viewModel.performRenameWithConflictCheck(newFileName.getFileName().toString());

        // Verify the new file has been renamed
        assertTrue(Files.exists(newFileName), "File should be renamed successfully");
        assertFalse(Files.exists(tempDir.resolve("fileToRename.pdf")), "Original file should be renamed");
    }

    // Test 2: File exists, user chooses to override
    @Test
    void performRenameWithConflictCheckWhenOverrideChosenOverwritesFile() throws IOException {
        
        Path existingFilePath = tempDir.resolve("existingFile.pdf");
        Path fileToRename = tempDir.resolve("fileToRename.pdf");

        // Simulate user choosing "Keep both"
        when(dialogServiceMock.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class)))
            .thenAnswer(invocation -> Optional.of(invocation.getArgument(2))); // override is argument index 2

        // Call the method
        viewModel.performRenameWithConflictCheck(existingFilePath.getFileName().toString());

        // Verify the file is overwritten
        assertTrue(Files.exists(existingFilePath), "The file should be overwritten");
    }

    // Test 3: File exists, user chooses "Keep both"
    @Test
    void performRenameWithConflictCheckWhenKeepBothChosenKeepsBothFiles() throws IOException {

        Path existingFilePath = tempDir.resolve("existingFile.pdf");
        Path fileToRename = tempDir.resolve("fileToRename.pdf");

        // Simulate user choosing "Keep both"
        when(dialogServiceMock.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class)))
            .thenAnswer(invocation -> Optional.of(invocation.getArgument(3))); // keepBothButton is argument index 3

        // Expected new file name (generated using `getNonOverWritingFileName`)
        String newFileName = FileNameUniqueness.getNonOverWritingFileName(tempDir, existingFilePath.getFileName().toString());
        Path expectedNewFilePath = tempDir.resolve(newFileName);

        // Call the method
        viewModel.performRenameWithConflictCheck(existingFilePath.getFileName().toString());

        // Verify that both files exist and the new file is renamed correctly
        assertTrue(Files.exists(existingFilePath), "The existing file should still exist");
        assertTrue(Files.exists(expectedNewFilePath), "The renamed file should exist with a unique name");
        // Verify that the original fileToRename no longer exists
        assertFalse(Files.exists(fileToRename), "The original fileToRename should not exist anymore");
    }


    // Test 4: File exists, user chooses to provide an alternative file name
    @Test
    void performRenameWithConflictCheckWhenProvideAlternativeFileNameChosenUsesUserInput() {
        Path existingFilePath = tempDir.resolve("existingFile.pdf");
        Path fileToRename = tempDir.resolve("fileToRename.pdf");

        // Simulate the user selecting "Provide alternative file name"
        when(dialogServiceMock.showCustomDialogAndWait(
                anyString(),
                any(DialogPane.class),
                any(ButtonType.class),
                any(ButtonType.class),
                any(ButtonType.class)))
            .thenAnswer(invocation -> Optional.of(invocation.getArgument(4))); // provideAltFileNameButton is index 4

        // Simulate the user entering a new file name
        String userProvidedFileName = "newFileName.pdf";
        when(dialogServiceMock.showInputDialogWithDefaultAndWait(anyString(), anyString(), anyString()))
            .thenReturn(Optional.of(userProvidedFileName));

        // Call the method
        viewModel.performRenameWithConflictCheck(existingFilePath.getFileName().toString());

        // Expected new path based on user input
        Path expectedNewFilePath = tempDir.resolve(userProvidedFileName);

        // Check that the renamed file exists
        assertTrue(Files.exists(expectedNewFilePath), "The file should be renamed to the user-provided name");

        // Verify that the original fileToRename no longer exists
        assertFalse(Files.exists(fileToRename), "The original file should not exist anymore");

        // Ensure the conflicting file was not overwritten
        assertTrue(Files.exists(existingFilePath), "The existing file should still be there");
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up any real files after each test
        Files.walk(tempDir)
             .map(Path::toFile)
             .forEach(file -> {
                 if (!file.isDirectory()) {
                     file.delete();
                 }
             });
    }
}
