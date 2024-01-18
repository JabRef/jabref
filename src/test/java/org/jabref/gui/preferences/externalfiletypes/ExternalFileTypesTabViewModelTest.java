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
    private ExternalFileTypeItemViewModel externalFileTypeItemViewModel = new ExternalFileTypeItemViewModel();

    @Spy
    private ExternalFileTypesTabViewModel externalFileTypesTabViewModel = spy(new ExternalFileTypesTabViewModel(filePreferences, dialogService));

    @BeforeEach
    void setUp() {
        externalFileTypeItemViewModel.nameProperty().set("Excel 2007");
        externalFileTypeItemViewModel.extensionProperty().set("xlsx");
        externalFileTypeItemViewModel.mimetypeProperty().set("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        externalFileTypeItemViewModel.applicationProperty().set("oocalc");
    }

    public void setupViewModelWithoutName() {
        externalFileTypeItemViewModel.nameProperty().set("");
    }

    public void viewModelClone(ExternalFileTypeItemViewModel updatedModel) {
        updatedModel.nameProperty().set(externalFileTypeItemViewModel.getName());
        updatedModel.extensionProperty().set(externalFileTypeItemViewModel.extensionProperty().get());
        updatedModel.mimetypeProperty().set(externalFileTypeItemViewModel.mimetypeProperty().get());
        updatedModel.applicationProperty().set(externalFileTypeItemViewModel.applicationProperty().get());
    }

    public boolean viewModelIsSameValue(ExternalFileTypeItemViewModel item) {
        return !(!item.getName().equals(externalFileTypeItemViewModel.getName())
                || !item.extensionProperty().get().equals(externalFileTypeItemViewModel.extensionProperty().get())
                || !item.mimetypeProperty().get().equals(externalFileTypeItemViewModel.mimetypeProperty().get())
                || !item.applicationProperty().get().equals(externalFileTypeItemViewModel.applicationProperty().get()));
    }

    @Test
    public void whenExternalFileTypeItemViewModelWithNonEmptyStringValueThenisValidExternalFileTypeReturnTrue() {
        assertTrue(externalFileTypesTabViewModel.isValidExternalFileType(externalFileTypeItemViewModel));
    }

    @Test
    public void whenExternalFileTypeItemViewModelWithEmptyNameThenisValidExternalFileTypeReturnFalse() {
        this.setupViewModelWithoutName();
        assertFalse(externalFileTypesTabViewModel.isValidExternalFileType(externalFileTypeItemViewModel));
    }

    @Test
    public void WhenExternalFileTypeItemViewModelIsValidThenAddNewTypeIsSuccessful() {
        ArgumentCaptor<ExternalFileTypeItemViewModel> itemCaptor = ArgumentCaptor.forClass(ExternalFileTypeItemViewModel.class);
        doAnswer(mocked -> {
            ExternalFileTypeItemViewModel capturedItem = itemCaptor.getValue();
            this.viewModelClone(capturedItem);
            return null;
        }).when(externalFileTypesTabViewModel).showEditDialog(itemCaptor.capture(), any());

        externalFileTypesTabViewModel.addNewType();

        ObservableList<ExternalFileTypeItemViewModel> actualFileTypes = externalFileTypesTabViewModel.getFileTypes();
        assertEquals(1, actualFileTypes.size());
        assertTrue(viewModelIsSameValue(actualFileTypes.getFirst()));
    }

    @Test
    public void WhenExternalFileTypeItemViewModelMissNameThenAddNewTypeIsFailed() {
        setupViewModelWithoutName();
        ArgumentCaptor<ExternalFileTypeItemViewModel> itemCaptor = ArgumentCaptor.forClass(ExternalFileTypeItemViewModel.class);
        doAnswer(mocked -> {
            ExternalFileTypeItemViewModel capturedItem = itemCaptor.getValue();
            viewModelClone(capturedItem);
            return null;
        }).when(externalFileTypesTabViewModel).showEditDialog(itemCaptor.capture(), any());

        externalFileTypesTabViewModel.addNewType();

        ObservableList<ExternalFileTypeItemViewModel> emptyFileTypes = externalFileTypesTabViewModel.getFileTypes();
        assertEquals(0, emptyFileTypes.size());
    }
}
