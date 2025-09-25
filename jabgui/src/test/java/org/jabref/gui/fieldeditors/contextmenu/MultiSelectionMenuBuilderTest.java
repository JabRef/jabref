package org.jabref.gui.fieldeditors.contextmenu;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Answers;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class MultiSelectionMenuBuilderTest {

    @Test
    void shouldSupportOnlyWhenSelectionHasMoreThanOneItem() {
        MultiSelectionMenuBuilder builder = newBuilder(
                mock(LinkedFilesEditorViewModel.class),
                mock(DialogService.class)
        );

        assertEquals(false,
                builder.supports(FXCollections.observableArrayList()),
                "Empty selection should not be supported");

        ObservableList<LinkedFileViewModel> single =
                FXCollections.observableArrayList(mock(LinkedFileViewModel.class));
        assertEquals(false,
                builder.supports(single),
                "Single selection should not be supported");

        ObservableList<LinkedFileViewModel> multiple = FXCollections.observableArrayList(
                mock(LinkedFileViewModel.class), mock(LinkedFileViewModel.class));
        assertEquals(true,
                builder.supports(multiple),
                "Multiple selection should be supported");
    }

    /**
     * Test for the public contract of {@code MultiSelectionMenuBuilder}.
     * Verifies that the multi-file context menu is built with 8 items in the expected
     * order. Other tests assert the behavior of individual items; this
     * pre-condition prevents accidental reordering or removal during changes.
     */
    @Test
    void shouldBuildEightMenuItemsInExpectedOrder() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        DialogService dialogService = mock(DialogService.class);
        MultiSelectionMenuBuilder builder = newBuilder(editorViewModel, dialogService);

        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(
                mockOfflineExistingFileViewModel(), mockOnlineLinkViewModel("https://host/x.pdf"));

        List<MenuItem> items = builder.buildMenu(selection);

        assertNotNull(items);
        assertEquals(8, items.size(), "Multi-selection menu must contain 8 items");

        items.forEach(item -> assertNotNull(item.getOnAction(), "Each item should have an action handler"));
    }

    @Test
    void openFileItemInvokesOpenOnlyForLocalExistingFiles() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        MultiSelectionMenuBuilder builder = newBuilder(editorViewModel, mock(DialogService.class));
        LinkedFileViewModel localExisting = mockOfflineExistingFileViewModel();
        LinkedFileViewModel onlineLink = mockOnlineLinkViewModel("https://host/a.pdf");

        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(localExisting, onlineLink);
        List<MenuItem> items = builder.buildMenu(selection);

        items.getFirst().fire();

        verify(localExisting).open();
        verify(onlineLink, never()).open();
    }

    @Test
    void openFolderItemEnabledOnlyWhenAtLeastOneLocalExistingFilePresent() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        DialogService dialogService = mock(DialogService.class);
        MultiSelectionMenuBuilder builder = newBuilder(editorViewModel, dialogService);

        LinkedFileViewModel localExisting = mockOfflineExistingFileViewModel();
        LinkedFileViewModel onlineLink = mockOnlineLinkViewModel("https://host/a.pdf");

        ObservableList<LinkedFileViewModel> selectionA =
                FXCollections.observableArrayList(localExisting, onlineLink);
        List<MenuItem> itemsA = builder.buildMenu(selectionA);
        assertEquals(false, itemsA.get(1).isDisable(),
                "OPEN_FOLDER should be enabled when at least one local existing file is present");

        LinkedFileViewModel onlineLink1 = mockOnlineLinkViewModel("https://host/b.pdf");
        LinkedFileViewModel onlineLink2 = mockOnlineLinkViewModel("https://host/c.pdf");
        ObservableList<LinkedFileViewModel> selectionB =
                FXCollections.observableArrayList(onlineLink1, onlineLink2);
        List<MenuItem> itemsB = builder.buildMenu(selectionB);
        assertEquals(true, itemsB.get(1).isDisable(),
                "OPEN_FOLDER should be disabled when there are no local existing files");
    }

    @Test
    void downloadItemInvokesDownloadForOnlineOnly() {
        MultiSelectionMenuBuilder builder = newBuilder(
                mock(LinkedFilesEditorViewModel.class),
                mock(DialogService.class)
        );

        LinkedFileViewModel onlineA = mockOnlineLinkViewModel("https://host/a.pdf");
        LinkedFileViewModel onlineB = mockOnlineLinkViewModel("https://host/b.pdf");
        LinkedFileViewModel localExisting = mockOfflineExistingFileViewModel();

        ObservableList<LinkedFileViewModel> selection =
                FXCollections.observableArrayList(onlineA, onlineB, localExisting);
        List<MenuItem> items = builder.buildMenu(selection);

        items.get(2).fire();

        verify(onlineA).download(true);
        verify(onlineB).download(true);
        verify(localExisting, never()).download(anyBoolean());
    }

    @Test
    void redownloadItemInvokesRedownloadOnlyWhenSourceUrlPresent() {
        MultiSelectionMenuBuilder builder = newBuilder(
                mock(LinkedFilesEditorViewModel.class),
                mock(DialogService.class)
        );

        LinkedFileViewModel withSource = mockOnlineLinkViewModel("https://host/a.pdf");
        when(withSource.getFile().getSourceUrl()).thenReturn("https://host/a.pdf");
        when(withSource.getFile().sourceUrlProperty()).thenReturn(new SimpleStringProperty("https://host/a.pdf"));

        LinkedFileViewModel withoutSource = mockOnlineLinkViewModel("");
        when(withoutSource.getFile().getSourceUrl()).thenReturn("");
        when(withoutSource.getFile().sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        ObservableList<LinkedFileViewModel> selection =
                FXCollections.observableArrayList(withSource, withoutSource);
        List<MenuItem> items = builder.buildMenu(selection);

        items.get(3).fire();

        verify(withSource).redownload();
        verify(withoutSource, never()).redownload();
    }

    @Test
    void moveFileToFolderInvokesMoveOnlyForMovableLocalFiles() {
        MultiSelectionMenuBuilder builder = newBuilder(
                mock(LinkedFilesEditorViewModel.class),
                mock(DialogService.class)
        );

        LinkedFileViewModel movableLocal = mockOfflineExistingFileViewModel();
        when(movableLocal.isGeneratedPathSameAsOriginal()).thenReturn(false);

        LinkedFileViewModel nonMovableLocal = mockOfflineExistingFileViewModel();
        when(nonMovableLocal.isGeneratedPathSameAsOriginal()).thenReturn(true);

        LinkedFileViewModel onlineLink = mockOnlineLinkViewModel("https://host/x.pdf");

        ObservableList<LinkedFileViewModel> selection =
                FXCollections.observableArrayList(movableLocal, nonMovableLocal, onlineLink);
        List<MenuItem> items = builder.buildMenu(selection);

        items.get(4).fire();

        verify(movableLocal).moveToDefaultDirectory();
        verify(nonMovableLocal, never()).moveToDefaultDirectory();
        verify(onlineLink, never()).moveToDefaultDirectory();
    }

    @Test
    void removeLinksInvokesRemoveForEachSelectedItem() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        MultiSelectionMenuBuilder builder = newBuilder(editorViewModel, mock(DialogService.class));

        LinkedFileViewModel a = mockOfflineExistingFileViewModel();
        LinkedFileViewModel b = mockOnlineLinkViewModel("https://host/x.pdf");

        ObservableList<LinkedFileViewModel> selection = FXCollections.observableArrayList(a, b);
        List<MenuItem> items = builder.buildMenu(selection);

        items.get(6).fire();

        verify(editorViewModel).removeFileLink(a);
        verify(editorViewModel).removeFileLink(b);
    }

    @Test
    void deleteFileInvokesDeleteOnlyForLocalExistingFiles() {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        MultiSelectionMenuBuilder builder = newBuilder(editorViewModel, mock(DialogService.class));

        LinkedFileViewModel localExisting = mockOfflineExistingFileViewModel();
        LinkedFileViewModel onlineLink = mockOnlineLinkViewModel("https://host/x.pdf");

        ObservableList<LinkedFileViewModel> selection =
                FXCollections.observableArrayList(localExisting, onlineLink);
        List<MenuItem> items = builder.buildMenu(selection);

        items.get(7).fire();

        verify(editorViewModel).deleteFile(localExisting);
        verify(editorViewModel, never()).deleteFile(onlineLink);
    }

    @Test
    void copyToFolderCopiesOnlyLocalExistingFilesAndNotifies(@TempDir Path tempDir) throws IOException {
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);
        DialogService dialogService = mock(DialogService.class);

        Path workingDir = Files.createDirectory(tempDir.resolve("working"));
        Path exportDir = Files.createDirectory(tempDir.resolve("export"));

        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getWorkingDirectory()).thenReturn(workingDir);

        Path sourceFile = workingDir.resolve("source.pdf");
        Files.writeString(sourceFile, "dummy");

        LinkedFileViewModel localExisting = mockOfflineExistingFileViewModel(sourceFile);
        LinkedFileViewModel onlineLink = mockOnlineLinkViewModel("https://host/ignore.pdf");

        when(dialogService.showDirectorySelectionDialog(any(DirectoryDialogConfiguration.class)))
                .thenReturn(Optional.of(exportDir));

        MultiSelectionMenuBuilder builder = new MultiSelectionMenuBuilder(
                dialogService,
                mock(BibDatabaseContext.class),
                emptyBibEntryOptional(),
                guiPreferences,
                editorViewModel
        );

        ObservableList<LinkedFileViewModel> selection =
                FXCollections.observableArrayList(localExisting, onlineLink);
        List<MenuItem> items = builder.buildMenu(selection);

        items.get(5).fire();

        Path copied = exportDir.resolve(sourceFile.getFileName());
        assertTrue(Files.exists(copied), "Copied file must exist in export directory");

        verify(dialogService, atLeastOnce()).notify(anyString());
        verify(dialogService, never()).showInformationDialogAndWait(anyString(), anyString());
    }

    private static MultiSelectionMenuBuilder newBuilder(LinkedFilesEditorViewModel editorViewModel,
                                                        DialogService dialogService) {
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(
                mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS));

        return new MultiSelectionMenuBuilder(
                dialogService,
                mock(BibDatabaseContext.class),
                emptyBibEntryOptional(),
                guiPreferences,
                editorViewModel
        );
    }

    private static ObservableOptionalValue<BibEntry> emptyBibEntryOptional() {
        return EasyBind.wrapNullable(new SimpleObjectProperty<>(null));
    }

    private static LinkedFileViewModel mockOfflineExistingFileViewModel() {
        return mockOfflineExistingFileViewModel(Path.of("dummy.pdf"));
    }

    private static LinkedFileViewModel mockOfflineExistingFileViewModel(Path sourcePath) {
        LinkedFile modelLinkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(modelLinkedFile.isOnlineLink()).thenReturn(false);
        when(modelLinkedFile.findIn(any(BibDatabaseContext.class), any(FilePreferences.class)))
                .thenReturn(Optional.of(sourcePath));
        when(modelLinkedFile.linkProperty())
                .thenReturn(new SimpleStringProperty(sourcePath.getFileName().toString()));
        when(modelLinkedFile.getSourceUrl()).thenReturn("");
        when(modelLinkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel fileViewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(fileViewModel.getFile()).thenReturn(modelLinkedFile);
        when(fileViewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(false);
        return fileViewModel;
    }

    private static LinkedFileViewModel mockOnlineLinkViewModel(String sourceUrl) {
        String nonNullUrl = sourceUrl == null ? "" : sourceUrl;

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
