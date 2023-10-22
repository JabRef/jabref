package org.jabref.gui.preferences.externalfiletypes;

import java.util.Set;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.StandardExternalFileType;
import org.jabref.preferences.FilePreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExternalFileTypesTabViewModelTest {
    private static final Set<ExternalFileType> TEST_LIST = Set.of(
            StandardExternalFileType.MARKDOWN,
            StandardExternalFileType.PDF,
            StandardExternalFileType.URL,
            StandardExternalFileType.JPG,
            StandardExternalFileType.TXT);

    private ExternalFileTypesTabViewModel externalFileTypesTabViewModel = mock(ExternalFileTypesTabViewModel.class);
    private FilePreferences filePreferences = mock(FilePreferences.class);
    private ObservableList<ExternalFileTypeItemViewModel> fileTypes = FXCollections.observableArrayList();

    @BeforeEach
    void setUp() {
        // Arrange
        when(filePreferences.getExternalFileTypes()).thenReturn(FXCollections.observableSet(TEST_LIST));
        fileTypes.addAll(filePreferences.getExternalFileTypes().stream().map(ExternalFileTypeItemViewModel::new).toList());
    }

    @Test
    public void addNewTypeSuccess() {
        // Arrange
        doAnswer(invocation -> {
            ExternalFileTypeItemViewModel item = new ExternalFileTypeItemViewModel();
            fileTypes.add(item);
            return null;
        }).when(externalFileTypesTabViewModel).addNewType();

        //Action
        externalFileTypesTabViewModel.addNewType();

        //Assert
        assertEquals(fileTypes.size(), 6);
    }

    @Test
    public void addNewTypeFailed() {
        // Arrange
        doNothing().when(externalFileTypesTabViewModel).addNewType();

        //Action
        externalFileTypesTabViewModel.addNewType();

        //Assert
        assertEquals(fileTypes.size(), 5);
    }
}
