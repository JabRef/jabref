package org.jabref.gui.linkedfile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.TreeSet;

import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.LinkedFile;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkedFileEditDialogViewModelTest {
    private final GuiPreferences preferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
    private final BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
    private final DialogService dialogService = mock(DialogService.class);
    private final ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);

    @BeforeEach
    void setup() {
        when(externalApplicationsPreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(new TreeSet<>(ExternalFileTypes.getDefaultExternalFileTypes())));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
    }

    @Test
    @DisabledOnOs(value = OS.WINDOWS, disabledReason = "? is an invalid filename character in Windows")
    void badFilenameCharWillBeReplacedByUnderscore(@TempDir Path tempDir) throws IOException {
        Path invalidFile = tempDir.resolve("?invalid.pdf");
        Files.createFile(invalidFile);
        when(dialogService.showConfirmationDialogAndWait(any(), any(), any())).thenReturn(true);

        LinkedFileEditDialogViewModel viewModel = new LinkedFileEditDialogViewModel(LinkedFile.of("", "", ""), bibDatabaseContext, dialogService, externalApplicationsPreferences, filePreferences);

        viewModel.checkForBadFileNameAndAdd(invalidFile);

        LinkedFile expectedFile = LinkedFile.of("", tempDir.resolve("_invalid.pdf").toString(), "PDF");
        assertEquals(expectedFile, viewModel.getNewLinkedFile());
    }
}
