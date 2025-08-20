package org.jabref.gui.copyfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CopyMultipleFilesActionTest {

    @TempDir
    Path tmpDir;

    private DialogService dialogService;
    private BibDatabaseContext databaseContext;
    private FilePreferences filePreferences;

    private Path srcA;
    private Path srcB;

    @BeforeEach
    void setUp() throws IOException {
        dialogService = mock(DialogService.class);
        databaseContext = mock(BibDatabaseContext.class);
        filePreferences = mock(FilePreferences.class);

        srcA = Files.createFile(tmpDir.resolve("a.pdf"));
        srcB = Files.createFile(tmpDir.resolve("b.pdf"));
        Files.writeString(srcA, "AAA");
        Files.writeString(srcB, "BBB");
    }

    private LinkedFileViewModel vmLocal(Path p) {
        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile().isOnlineLink()).thenReturn(false);
        when(vm.getFile().findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.of(p));
        return vm;
    }

    private LinkedFileViewModel vmOnline() {
        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile().isOnlineLink()).thenReturn(true);
        when(vm.getFile().findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.empty());
        return vm;
    }

    private LinkedFileViewModel vmMissing() {
        LinkedFileViewModel vm = mock(LinkedFileViewModel.class, RETURNS_DEEP_STUBS);
        when(vm.getFile().isOnlineLink()).thenReturn(false);
        when(vm.getFile().findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.empty());
        return vm;
    }

    @Test
    void executableIsFalseWhenSelectionEmpty() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList();
        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);

        assertFalse(action.executableProperty().get());
    }

    @Test
    void executableIsFalseWhenAnyIsOnline() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocal(srcA),
                vmOnline()
        );
        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);

        assertFalse(action.executableProperty().get());
    }

    @Test
    void executableIsFalseWhenAnyLocalButMissing() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocal(srcA),
                vmMissing()
        );
        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);

        assertFalse(action.executableProperty().get());
    }

    @Test
    void executableIsTrueWhenAllLocalAndPresent() {
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocal(srcA),
                vmLocal(srcB)
        );
        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);

        assertTrue(action.executableProperty().get());
    }

    @Test
    void copiesAllSelectedFilesToChosenDirectory() {
        Path target = tmpDir.resolve("dest");
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocal(srcA),
                vmLocal(srcB)
        );

        when(dialogService.showDirectorySelectionDialog(any(DirectoryDialogConfiguration.class)))
                .thenReturn(Optional.of(target));

        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);
        assertTrue(action.executableProperty().get());

        action.execute();

        assertTrue(Files.exists(target.resolve(srcA.getFileName())));
        assertTrue(Files.exists(target.resolve(srcB.getFileName())));

        verify(dialogService, atLeastOnce()).notify(anyString());
        verify(dialogService, never()).showErrorDialogAndWait(anyString());
        verify(dialogService, never()).showErrorDialogAndWait(anyString(), any(Throwable.class));
    }

    @Test
    void createsDestinationDirectoryIfMissing() {
        Path target = tmpDir.resolve("newDir/inner");
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(vmLocal(srcA));

        when(dialogService.showDirectorySelectionDialog(any(DirectoryDialogConfiguration.class)))
                .thenReturn(Optional.of(target));

        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);
        action.execute();

        assertTrue(Files.isDirectory(target));
        assertTrue(Files.exists(target.resolve(srcA.getFileName())));
    }

    @Test
    void showsErrorWhenTargetIsAFileNotDirectory() throws IOException {
        Path notADir = Files.createFile(tmpDir.resolve("not-a-dir.txt"));

        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(vmLocal(srcA));
        when(dialogService.showDirectorySelectionDialog(any(DirectoryDialogConfiguration.class)))
                .thenReturn(Optional.of(notADir));

        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);
        action.execute();

        verify(dialogService).showErrorDialogAndWait(anyString());
        assertFalse(Files.exists(notADir.resolve(srcA.getFileName())));
    }

    @Test
    void showsErrorForSingleFileCopyFailureButProceedsForOthers() throws IOException {
        Path target = tmpDir.resolve("dest2");
        Files.createDirectories(target);

        Files.createDirectory(target.resolve(srcA.getFileName().toString()));

        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(
                vmLocal(srcA),
                vmLocal(srcB)
        );

        when(dialogService.showDirectorySelectionDialog(any(DirectoryDialogConfiguration.class)))
                .thenReturn(Optional.of(target));

        CopyMultipleFilesAction action = new CopyMultipleFilesAction(sel, dialogService, databaseContext, filePreferences);
        action.execute();

        assertTrue(Files.exists(target.resolve(srcB.getFileName())));
        verify(dialogService, atLeastOnce()).showErrorDialogAndWait(anyString(), any(Throwable.class));
    }

}
