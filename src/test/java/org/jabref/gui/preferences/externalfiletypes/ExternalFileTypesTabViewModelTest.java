package org.jabref.gui.preferences.externalfiletypes;

import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class ExternalFileTypesTabViewModelTest {

    private FilePreferences filePreferences = mock(FilePreferences.class);
    private DialogService dialogService = mock(DialogService.class);

    @Spy
    private ExternalFileTypesTabViewModel externalFileTypesTabViewModel = spy(new ExternalFileTypesTabViewModel(filePreferences, dialogService));
    private ExternalFileTypeItemViewModelTestData externalFileTypeItemViewModel = new ExternalFileTypeItemViewModelTestData();

    @BeforeEach
    void setUp() {
        externalFileTypeItemViewModel.setup();
    }

    @Test
    public void whenExternalFileTypeItemViewModelWithNonEmptyStringValueThenisValidExternalFileTypeReturnTrue() {
        assertTrue(externalFileTypesTabViewModel.isValidExternalFileType(externalFileTypeItemViewModel.get()));
    }

    @Test
    public void whenExternalFileTypeItemViewModelWithEmptyNameThenisValidExternalFileTypeReturnFalse() {
        externalFileTypeItemViewModel.setupWithoutName();
        assertFalse(externalFileTypesTabViewModel.isValidExternalFileType(externalFileTypeItemViewModel.get()));
    }

    @Test
    public void WhenExternalFileTypeItemViewModelIsValidThenAddNewTypeIsSuccessful() {
        ArgumentCaptor<ExternalFileTypeItemViewModel> itemCaptor = ArgumentCaptor.forClass(ExternalFileTypeItemViewModel.class);
        doAnswer(mocked -> {
            ExternalFileTypeItemViewModel capturedItem = itemCaptor.getValue();
            externalFileTypeItemViewModel.clone(capturedItem);
            return null;
        }).when(externalFileTypesTabViewModel).showEditDialog(itemCaptor.capture(), any());

        externalFileTypesTabViewModel.addNewType();

        ObservableList<ExternalFileTypeItemViewModel> actualFileTypes = externalFileTypesTabViewModel.getFileTypes();
        assertEquals(actualFileTypes.size(), 1);
        assertTrue(externalFileTypeItemViewModel.isSameValue(actualFileTypes.getFirst()));
    }

    @Test
    public void WhenExternalFileTypeItemViewModelMissNameThenAddNewTypeIsFailed() {
        externalFileTypeItemViewModel.setupWithoutName();
        ArgumentCaptor<ExternalFileTypeItemViewModel> itemCaptor = ArgumentCaptor.forClass(ExternalFileTypeItemViewModel.class);
        doAnswer(mocked -> {
            ExternalFileTypeItemViewModel capturedItem = itemCaptor.getValue();
            externalFileTypeItemViewModel.clone(capturedItem);
            return null;
        }).when(externalFileTypesTabViewModel).showEditDialog(itemCaptor.capture(), any());

        externalFileTypesTabViewModel.addNewType();

        ObservableList<ExternalFileTypeItemViewModel> emptyFileTypes = externalFileTypesTabViewModel.getFileTypes();
        assertEquals(emptyFileTypes.size(), 0);
    }
}
