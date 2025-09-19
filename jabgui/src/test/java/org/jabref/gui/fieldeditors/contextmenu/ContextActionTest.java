package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.actions.StandardActions;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class ContextActionTest {

    @Test
    void shouldBeExecutableForOpenFileWhenOfflineFileExists() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.OPEN_FILE, fileViewModel);

        assertTrue(action.isExecutable(), "OPEN_FILE should be executable for an existing offline file");
    }

    @Test
    void shouldNotBeExecutableForRenameToPatternWhenNameAlreadyMatchesSuggestion() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(true);

        ContextAction action = newAction(StandardActions.RENAME_FILE_TO_PATTERN, fileViewModel);

        assertFalse(action.isExecutable(),
                "RENAME_FILE_TO_PATTERN should be disabled when suggested name equals original");
    }

    @Test
    void shouldBeExecutableForDownloadWhenFileIsOnlineLink() {
        LinkedFileViewModel fileViewModel = mockOnlineLinkViewModel("https://host/resource.pdf");
        ContextAction action = newAction(StandardActions.DOWNLOAD_FILE, fileViewModel);

        assertTrue(action.isExecutable(), "DOWNLOAD_FILE should be executable for online link");
    }

    @Test
    void shouldNotBeExecutableForMoveToFolderWhenGeneratedPathMatchesOriginal() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        when(fileViewModel.isGeneratedPathSameAsOriginal()).thenReturn(true);

        ContextAction action = newAction(StandardActions.MOVE_FILE_TO_FOLDER, fileViewModel);

        assertFalse(action.isExecutable(),
                "MOVE_FILE_TO_FOLDER should be disabled when generated path equals original");
    }

    @Test
    void shouldExecuteEditFileLink() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.EDIT_FILE_LINK, fileViewModel);

        action.execute();

        verify(fileViewModel).edit();
    }

    @Test
    void shouldExecuteOpenFile() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.OPEN_FILE, fileViewModel);

        action.execute();

        verify(fileViewModel).open();
    }

    @Test
    void shouldExecuteOpenFolder() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.OPEN_FOLDER, fileViewModel);

        action.execute();

        verify(fileViewModel).openFolder();
    }

    @Test
    void shouldExecuteDownloadOnlyWhenOnlineLink() {
        LinkedFileViewModel onlineViewModel = mockOnlineLinkViewModel("https://host/file.pdf");
        ContextAction onlineAction = newAction(StandardActions.DOWNLOAD_FILE, onlineViewModel);
        onlineAction.execute();
        verify(onlineViewModel).download(true);

        LinkedFileViewModel offlineViewModel = mockOfflineExistingFileViewModel();
        ContextAction offlineAction = newAction(StandardActions.DOWNLOAD_FILE, offlineViewModel);
        offlineAction.execute();
        verify(offlineViewModel, never()).download(anyBoolean());
    }

    @Test
    void shouldExecuteRedownloadOnlyWhenSourceUrlPresent() {
        LinkedFileViewModel viewModelWithSource = mockOnlineLinkViewModel("https://host/file.pdf");
        when(viewModelWithSource.getFile().getSourceUrl()).thenReturn("https://host/file.pdf");
        ContextAction actionWithSource = newAction(StandardActions.REDOWNLOAD_FILE, viewModelWithSource);
        actionWithSource.execute();
        verify(viewModelWithSource).redownload();

        LinkedFileViewModel viewModelWithoutSource = mockOnlineLinkViewModel("");
        when(viewModelWithoutSource.getFile().getSourceUrl()).thenReturn("");
        ContextAction actionWithoutSource = newAction(StandardActions.REDOWNLOAD_FILE, viewModelWithoutSource);
        actionWithoutSource.execute();
        verify(viewModelWithoutSource, never()).redownload();
    }

    @Test
    void shouldExecuteRenameToSuggestion() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(false);

        ContextAction action = newAction(StandardActions.RENAME_FILE_TO_PATTERN, fileViewModel);

        action.execute();

        verify(fileViewModel).renameToSuggestion();
    }

    @Test
    void shouldExecuteAskForNameAndRename() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.RENAME_FILE_TO_NAME, fileViewModel);

        action.execute();

        verify(fileViewModel).askForNameAndRename();
    }

    @Test
    void shouldExecuteMoveToDefaultDirectory() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.MOVE_FILE_TO_FOLDER, fileViewModel);

        action.execute();

        verify(fileViewModel).moveToDefaultDirectory();
    }

    @Test
    void shouldExecuteMoveToDefaultDirectoryAndRename() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        ContextAction action = newAction(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, fileViewModel);

        action.execute();

        verify(fileViewModel).moveToDefaultDirectoryAndRename();
    }

    @Test
    void shouldExecuteDeleteFile() {
        LinkedFileViewModel fileViewModel = mockOfflineExistingFileViewModel();
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);

        ContextAction action = newAction(StandardActions.DELETE_FILE, fileViewModel, editorViewModel);

        action.execute();

        verify(editorViewModel).deleteFile(fileViewModel);
    }

    @Test
    void shouldExecuteRemoveFileLinkForRemoveLinkAndRemoveLinks() {
        LinkedFileViewModel fileViewModel = mockOnlineLinkViewModel("https://host/x.pdf");
        LinkedFilesEditorViewModel editorViewModel = mock(LinkedFilesEditorViewModel.class);

        ContextAction removeLinkAction = newAction(StandardActions.REMOVE_LINK, fileViewModel, editorViewModel);
        ContextAction removeLinksAction = newAction(StandardActions.REMOVE_LINKS, fileViewModel, editorViewModel);

        removeLinkAction.execute();
        removeLinksAction.execute();

        verify(editorViewModel, times(2)).removeFileLink(fileViewModel);
    }

    private static ContextAction newAction(StandardActions actionType, LinkedFileViewModel fileViewModel) {
        return newAction(actionType, fileViewModel, mock(LinkedFilesEditorViewModel.class));
    }

    private static ContextAction newAction(StandardActions actionType,
                                           LinkedFileViewModel fileViewModel,
                                           LinkedFilesEditorViewModel editorViewModel) {
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

        BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);
        ObservableOptionalValue<BibEntry> bibEntryOptional = mockEmptyBibEntryOptional();

        return new ContextAction(
                actionType,
                fileViewModel,
                databaseContext,
                bibEntryOptional,
                guiPreferences,
                editorViewModel
        );
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
