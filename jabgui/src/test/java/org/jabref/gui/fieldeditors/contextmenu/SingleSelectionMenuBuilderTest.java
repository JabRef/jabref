package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class SingleSelectionMenuBuilderTest {

    private static final List<Integer> SEPARATOR_INDICES =
            List.of(0, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13);

    @Test
    void supportsReturnsTrueOnlyForSingleSelection() {
        SingleSelectionMenuBuilder builder = newBuilder(mock(LinkedFilesEditorViewModel.class));

        ObservableList<LinkedFileViewModel> empty = FXCollections.observableArrayList();
        assertFalse(builder.supports(empty), "Empty selection should not be supported");

        ObservableList<LinkedFileViewModel> one = FXCollections.observableArrayList(mock(LinkedFileViewModel.class));
        assertTrue(builder.supports(one), "Single selection should be supported");

        ObservableList<LinkedFileViewModel> two = FXCollections.observableArrayList(
                mock(LinkedFileViewModel.class),
                mock(LinkedFileViewModel.class)
        );
        assertFalse(builder.supports(two), "Multi selection should not be supported");
    }

    @Test
    void buildMenuReturnsItemsInExpectedOrderForOfflineExistingFile() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        SingleSelectionMenuBuilder builder = newBuilder(editorViewModel);

        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(fileViewModel);

        List<MenuItem> items = builder.buildMenu(selection);

        assertNotNull(items);
        assertEquals(14, items.size(), "Menu should contain 14 items including separators");

        assertInstanceOf(SeparatorMenuItem.class, items.get(1), "Second item should be a separator");
        assertInstanceOf(SeparatorMenuItem.class, items.get(4), "Fifth item should be a separator");

        SEPARATOR_INDICES.forEach(i ->
                assertInstanceOf(SeparatorMenuItem.class, items.get(i)));
    }

    @Test
    void menuActionsInvokeCorrespondingMethodsForOfflineExistingFile() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        SingleSelectionMenuBuilder builder = newBuilder(editorViewModel);

        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(fileViewModel);

        List<MenuItem> items = builder.buildMenu(selection);

        items.getFirst().fire();
        verify(fileViewModel).edit();

        items.get(2).fire();
        verify(fileViewModel).open();

        items.get(3).fire();
        verify(fileViewModel).openFolder();

        items.get(6).fire();
        verify(fileViewModel).renameToSuggestion();

        items.get(7).fire();
        verify(fileViewModel).askForNameAndRename();

        items.get(8).fire();
        verify(fileViewModel).moveToDefaultDirectory();

        items.get(9).fire();
        verify(fileViewModel).moveToDefaultDirectoryAndRename();

        items.get(12).fire();
        verify(editorViewModel).removeFileLink(fileViewModel);

        items.get(13).fire();
        verify(editorViewModel).deleteFile(fileViewModel);
    }

    @Test
    void downloadItemInvokesDownloadForOnlineLink() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        SingleSelectionMenuBuilder builder = newBuilder(editorViewModel);

        LinkedFileViewModel onlineViewModel = mockOnlineLinkViewModel();
        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(onlineViewModel);

        List<MenuItem> items = builder.buildMenu(selection);

        items.get(5).fire();
        verify(onlineViewModel).download(true);
    }

    @Test
    void redownloadItemInvokesRedownloadWhenSourceUrlPresent() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        SingleSelectionMenuBuilder builder = newBuilder(editorViewModel);

        LinkedFileViewModel viewModelWithSource = mockOfflineExistingFileViewModel();
        when(viewModelWithSource.getFile().getSourceUrl()).thenReturn("https://host/file.pdf");
        when(viewModelWithSource.getFile().sourceUrlProperty()).thenReturn(new SimpleStringProperty("https://host/file.pdf"));

        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(viewModelWithSource);

        List<MenuItem> items = builder.buildMenu(selection);

        items.get(11).fire();
        verify(viewModelWithSource).redownload();
    }

    private static SingleSelectionMenuBuilder newBuilder(LinkedFilesEditorViewModel editorViewModel) {
        DialogService dialogService = mock(DialogService.class);
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        ObservableOptionalValue<BibEntry> bibEntryOptional = mockEmptyBibEntryOptional();

        return new SingleSelectionMenuBuilder(dialogService, databaseContext, bibEntryOptional, guiPreferences, editorViewModel);
    }

    private static ObservableOptionalValue<BibEntry> mockEmptyBibEntryOptional() {
        @SuppressWarnings("unchecked")
        ObservableOptionalValue<BibEntry> optional =
                (ObservableOptionalValue<BibEntry>) mock(ObservableOptionalValue.class);
        when(optional.getValue()).thenReturn(Optional.empty());
        return optional;
    }

    private static LinkedFileViewModel mockOfflineExistingFileViewModel() {
        LinkedFile modelLinkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(modelLinkedFile.isOnlineLink()).thenReturn(false);
        when(modelLinkedFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.of(Path.of("dummy.pdf")));
        when(modelLinkedFile.linkProperty()).thenReturn(new SimpleStringProperty("dummy.pdf"));
        when(modelLinkedFile.getSourceUrl()).thenReturn("");
        when(modelLinkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel fileViewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(fileViewModel.getFile()).thenReturn(modelLinkedFile);
        when(fileViewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(false);
        return fileViewModel;
    }

    private static LinkedFileViewModel mockOnlineLinkViewModel() {
        String nonNullUrl = "https://host/resource.pdf";

        LinkedFile modelLinkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(modelLinkedFile.isOnlineLink()).thenReturn(true);
        when(modelLinkedFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.empty());
        when(modelLinkedFile.linkProperty()).thenReturn(new SimpleStringProperty("https://host/file.pdf"));
        when(modelLinkedFile.getSourceUrl()).thenReturn(nonNullUrl);
        when(modelLinkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(nonNullUrl));

        LinkedFileViewModel fileViewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(fileViewModel.getFile()).thenReturn(modelLinkedFile);
        when(fileViewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(false);
        return fileViewModel;
    }
}
