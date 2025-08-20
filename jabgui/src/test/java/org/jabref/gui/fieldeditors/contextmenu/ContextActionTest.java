package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextActionTest {

    private GuiPreferences preferences;
    private FilePreferences filePreferences;
    private BibDatabaseContext databaseContext;
    private LinkedFilesEditorViewModel editorViewModel;
    private ObservableOptionalValue<BibEntry> bibEntry;

    @BeforeEach
    void setUp() {
        preferences = mock(GuiPreferences.class);
        filePreferences = mock(FilePreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        databaseContext = mock(BibDatabaseContext.class);
        editorViewModel = mock(LinkedFilesEditorViewModel.class);

        var entryProperty = new SimpleObjectProperty<>(new BibEntry());
        bibEntry = EasyBind.wrapNullable(entryProperty);
    }

    private LinkedFileViewModel vmLocal(boolean exists,
                                        boolean genNameSame,
                                        boolean genPathSame,
                                        String sourceUrl) {
        LinkedFile linkedfile = mock(LinkedFile.class);
        when(linkedfile.isOnlineLink()).thenReturn(false);
        when(linkedfile.linkProperty()).thenReturn(new SimpleStringProperty("x.pdf"));
        when(linkedfile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(sourceUrl == null ? "" : sourceUrl));
        when(linkedfile.getSourceUrl()).thenReturn(sourceUrl == null ? "" : sourceUrl);
        when(linkedfile.findIn(eq(databaseContext), eq(filePreferences)))
                .thenReturn(exists ? Optional.of(Path.of("x.pdf")) : Optional.empty());

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile()).thenReturn(linkedfile);
        when(vm.isGeneratedNameSameAsOriginal()).thenReturn(genNameSame);
        when(vm.isGeneratedPathSameAsOriginal()).thenReturn(genPathSame);
        return vm;
    }

    private LinkedFileViewModel vmOnline() {
        LinkedFile linkedfile = mock(LinkedFile.class);
        when(linkedfile.isOnlineLink()).thenReturn(true);
        when(linkedfile.linkProperty()).thenReturn(new SimpleStringProperty("http://a"));
        when(linkedfile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));
        when(linkedfile.getSourceUrl()).thenReturn("");

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile()).thenReturn(linkedfile);
        return vm;
    }

    private ContextAction make(StandardActions action, LinkedFileViewModel vm) {
        return new ContextAction(action, vm, databaseContext, bibEntry, preferences, editorViewModel);
    }

    @Test
    void openFileEnabledOnlyForLocalExisting() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "");
        assertTrue(make(StandardActions.OPEN_FILE, vm).executableProperty().get());

        LinkedFileViewModel missing = vmLocal(false, false, false, "");
        assertFalse(make(StandardActions.OPEN_FILE, missing).executableProperty().get());

        LinkedFileViewModel online = vmOnline();
        assertFalse(make(StandardActions.OPEN_FILE, online).executableProperty().get());
    }

    @Test
    void downloadEnabledOnlyForOnline() {
        assertTrue(make(StandardActions.DOWNLOAD_FILE, vmOnline()).executableProperty().get());
        assertFalse(make(StandardActions.DOWNLOAD_FILE, vmLocal(true, false, false, "")).executableProperty().get());
    }

    @Test
    void redownloadEnabledIfSourceUrlPresent() {
        assertTrue(make(StandardActions.REDOWNLOAD_FILE, vmLocal(true, false, false, "http://src")).executableProperty().get());
        assertFalse(make(StandardActions.REDOWNLOAD_FILE, vmLocal(true, false, false, "")).executableProperty().get());
    }

    @Test
    void renameToPatternEnabledWhenLocalExistingAndNameDiffers() {
        assertTrue(make(StandardActions.RENAME_FILE_TO_PATTERN, vmLocal(true, false, false, "")).executableProperty().get());
        assertFalse(make(StandardActions.RENAME_FILE_TO_PATTERN, vmLocal(true, true, false, "")).executableProperty().get());
        assertFalse(make(StandardActions.RENAME_FILE_TO_PATTERN, vmLocal(false, false, false, "")).executableProperty().get());
    }

    @Test
    void moveToFolderEnabledWhenLocalExistingAndPathDiffers() {
        assertTrue(make(StandardActions.MOVE_FILE_TO_FOLDER, vmLocal(true, false, false, "")).executableProperty().get());
        assertFalse(make(StandardActions.MOVE_FILE_TO_FOLDER, vmLocal(true, false, true, "")).executableProperty().get());
        assertFalse(make(StandardActions.MOVE_FILE_TO_FOLDER, vmLocal(false, false, false, "")).executableProperty().get());
    }

    @Test
    void executeOpenFileCallsViewModelOpen() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "");
        make(StandardActions.OPEN_FILE, vm).execute();
        verify(vm).open();
    }

    @Test
    void executeOpenFolderCallsViewModelOpenFolder() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "");
        make(StandardActions.OPEN_FOLDER, vm).execute();
        verify(vm).openFolder();
    }

    @Test
    void executeDownloadCallsDownloadOnlyForOnline() {
        LinkedFileViewModel online = vmOnline();
        make(StandardActions.DOWNLOAD_FILE, online).execute();
        verify(online).download(true);

        LinkedFileViewModel local = vmLocal(true, false, false, "");
        make(StandardActions.DOWNLOAD_FILE, local).execute();
        verify(local, never()).download(anyBoolean());
    }

    @Test
    void executeRedownloadCallsOnlyWhenSourceUrlPresent() {
        LinkedFileViewModel withSrc = vmLocal(true, false, false, "http://src");
        make(StandardActions.REDOWNLOAD_FILE, withSrc).execute();
        verify(withSrc).redownload();

        LinkedFileViewModel noSrc = vmLocal(true, false, false, "");
        make(StandardActions.REDOWNLOAD_FILE, noSrc).execute();
        verify(noSrc, never()).redownload();
    }

    @Test
    void executeRenameVariants() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "");
        make(StandardActions.RENAME_FILE_TO_PATTERN, vm).execute();
        verify(vm).renameToSuggestion();

        make(StandardActions.RENAME_FILE_TO_NAME, vm).execute();
        verify(vm).askForNameAndRename();
    }

    @Test
    void executeMoveVariants() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "");
        make(StandardActions.MOVE_FILE_TO_FOLDER, vm).execute();
        verify(vm).moveToDefaultDirectory();

        make(StandardActions.MOVE_FILE_TO_FOLDER_AND_RENAME, vm).execute();
        verify(vm).moveToDefaultDirectoryAndRename();
    }

    @Test
    void executeDeleteAndRemoveDelegateToEditorViewModel() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "");

        make(StandardActions.DELETE_FILE, vm).execute();
        verify(editorViewModel).deleteFile(vm);

        make(StandardActions.REMOVE_LINK, vm).execute();
        verify(editorViewModel).removeFileLink(vm);
    }

    @Test
    void pluralFormsAreDispatchedToo() {
        LinkedFileViewModel vm = vmLocal(true, false, false, "http://src");
        make(StandardActions.OPEN_FILES, vm).execute();
        verify(vm).open();

        make(StandardActions.OPEN_FOLDERS, vm).execute();
        verify(vm).openFolder();

        make(StandardActions.DOWNLOAD_FILES, vmOnline()).execute();
        verify(vm, never()).download(anyBoolean());

        make(StandardActions.REDOWNLOAD_FILES, vm).execute();
        verify(vm).redownload();

        make(StandardActions.MOVE_FILES_TO_FOLDER, vm).execute();
        verify(vm).moveToDefaultDirectory();

        make(StandardActions.DELETE_FILES, vm).execute();
        verify(editorViewModel).deleteFile(vm);

        make(StandardActions.REMOVE_LINKS, vm).execute();
    }
}
