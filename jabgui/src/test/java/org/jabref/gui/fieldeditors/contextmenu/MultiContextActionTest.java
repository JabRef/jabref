package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.Optional;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MultiContextActionTest {

    private GuiPreferences preferences;
    private FilePreferences filePreferences;
    private BibDatabaseContext databaseContext;
    private LinkedFilesEditorViewModel viewModel;
    private ObservableOptionalValue<BibEntry> bibEntry;

    @BeforeEach
    void setUp() {
        preferences = mock(GuiPreferences.class);
        filePreferences = mock(FilePreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);

        databaseContext = mock(BibDatabaseContext.class);
        viewModel = mock(LinkedFilesEditorViewModel.class);

        SimpleObjectProperty<BibEntry> entryProp = new SimpleObjectProperty<>();
        bibEntry = EasyBind.wrapNullable(entryProp);
        entryProp.set(new BibEntry());
    }

    private LinkedFileViewModel vmLocalExisting(String nameOrPath) {
        LinkedFile lf = mock(LinkedFile.class);
        when(lf.isOnlineLink()).thenReturn(false);
        when(lf.findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.of(Path.of(nameOrPath)));
        when(lf.getSourceUrl()).thenReturn("");
        when(lf.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile()).thenReturn(lf);
        when(vm.isGeneratedPathSameAsOriginal()).thenReturn(false);
        return vm;
    }

    private LinkedFileViewModel vmLocalMissing() {
        LinkedFile lf = mock(LinkedFile.class);
        when(lf.isOnlineLink()).thenReturn(false);
        when(lf.findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.empty());
        when(lf.getSourceUrl()).thenReturn("");
        when(lf.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile()).thenReturn(lf);
        when(vm.isGeneratedPathSameAsOriginal()).thenReturn(false);
        return vm;
    }

    private LinkedFileViewModel vmOnline() {
        LinkedFile lf = mock(LinkedFile.class);
        when(lf.isOnlineLink()).thenReturn(true);
        when(lf.findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.empty());
        when(lf.getSourceUrl()).thenReturn("");
        when(lf.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile()).thenReturn(lf);
        return vm;
    }

    private LinkedFileViewModel vmWithSourceUrl() {
        LinkedFile lf = mock(LinkedFile.class);
        when(lf.isOnlineLink()).thenReturn(false);
        when(lf.findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.of(Path.of("x.pdf")));
        when(lf.getSourceUrl()).thenReturn("http://x");
        when(lf.sourceUrlProperty()).thenReturn(new SimpleStringProperty("http://x"));

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile()).thenReturn(lf);
        return vm;
    }

    private MultiContextAction make(StandardActions action, ObservableList<LinkedFileViewModel> sel) {
        return new MultiContextAction(action, sel, databaseContext, bibEntry, preferences, viewModel);
    }

    @Test
    void executableFalseWhenSelectionEmpty() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList();
        MultiContextAction action = make(StandardActions.OPEN_FILES, sel);
        assertFalse(action.executableProperty().get());
    }

    @Test
    void openFilesExecutableTrueIfAnyLocalExisting() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmOnline(), vmLocalExisting("a.pdf")
        );
        MultiContextAction action = make(StandardActions.OPEN_FILES, sel);
        assertTrue(action.executableProperty().get());
    }

    @Test
    void openFilesExecutableFalseIfNoLocalExisting() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmOnline(), vmLocalMissing()
        );
        MultiContextAction action = make(StandardActions.OPEN_FILES, sel);
        assertFalse(action.executableProperty().get());
    }

    @Test
    void downloadFilesExecutableTrueIfAnyOnline() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocalExisting("a.pdf"), vmOnline()
        );
        MultiContextAction action = make(StandardActions.DOWNLOAD_FILES, sel);
        assertTrue(action.executableProperty().get());
    }

    @Test
    void downloadFilesExecutableFalseIfNoneOnline() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocalExisting("a.pdf"), vmLocalExisting("b.pdf")
        );
        MultiContextAction action = make(StandardActions.DOWNLOAD_FILES, sel);
        assertFalse(action.executableProperty().get());
    }

    @Test
    void redownloadFilesExecutableTrueIfAnyHasSourceUrl() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmWithSourceUrl(), vmLocalExisting("b.pdf")
        );
        MultiContextAction action = make(StandardActions.REDOWNLOAD_FILES, sel);
        assertTrue(action.executableProperty().get());
    }

    @Test
    void redownloadFilesExecutableFalseIfNoSourceUrl() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocalExisting("a.pdf"), vmLocalExisting("b.pdf")
        );
        MultiContextAction action = make(StandardActions.REDOWNLOAD_FILES, sel);
        assertFalse(action.executableProperty().get());
    }

    @Test
    void moveFilesToFolderExecutableTrueOnlyIfAllLocalExistingAndNeedMove() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocalExisting("a.pdf"), vmLocalExisting("b.pdf")
        );
        MultiContextAction action = make(StandardActions.MOVE_FILES_TO_FOLDER, sel);
        assertTrue(action.executableProperty().get());
    }

    @Test
    void moveFilesToFolderExecutableFalseIfAnyMissing() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocalExisting("a.pdf"), vmLocalMissing()
        );
        MultiContextAction action = make(StandardActions.MOVE_FILES_TO_FOLDER, sel);
        assertFalse(action.executableProperty().get());
    }

    @Test
    void moveFilesToFolderExecutableFalseIfAnyHasSameGeneratedPath() {
        LinkedFileViewModel a = vmLocalExisting("a.pdf");
        LinkedFileViewModel b = vmLocalExisting("b.pdf");
        when(b.isGeneratedPathSameAsOriginal()).thenReturn(true);

        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(a, b);
        MultiContextAction action = make(StandardActions.MOVE_FILES_TO_FOLDER, sel);
        assertFalse(action.executableProperty().get());
    }

    @Test
    void executeOpenFoldersWithMultipleFilesDoesNotThrow() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocalExisting("dir1/a.pdf"),
                vmLocalExisting("dir1/b.pdf"),
                vmLocalExisting("dir2/c.pdf")
        );

        MultiContextAction action = make(StandardActions.OPEN_FOLDERS, sel);
        assertTrue(action.executableProperty().get());
        assertDoesNotThrow(action::execute);
    }

    @Test
    void executeDownloadFilesDoesNotThrowWithMixedSelection() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmOnline(), vmLocalExisting("a.pdf")
        );

        MultiContextAction action = make(StandardActions.DOWNLOAD_FILES, sel);
        assertTrue(action.executableProperty().get());
        assertDoesNotThrow(action::execute);
    }
}
