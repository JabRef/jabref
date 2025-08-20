package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.FilePreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContextMenuFactoryTest {

    private DialogService dialogService;
    private GuiPreferences guiPreferences;
    private FilePreferences filePreferences;
    private BibDatabaseContext databaseContext;
    private ObservableOptionalValue<BibEntry> bibEntry;
    private LinkedFilesEditorViewModel viewModel;
    private ContextMenuFactory factory;

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException alreadyStarted) {
            CountDownLatch latch = new CountDownLatch(1);
            assertDoesNotThrow(() -> Platform.runLater(latch::countDown));
            assertTrue(latch.await(1, TimeUnit.SECONDS), "JavaFX Platform should be running");
        }
    }

    @BeforeEach
    void setUp() {
        dialogService = mock(DialogService.class);
        guiPreferences = mock(GuiPreferences.class);
        filePreferences = mock(FilePreferences.class);
        databaseContext = mock(BibDatabaseContext.class);
        viewModel = mock(LinkedFilesEditorViewModel.class);

        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

        SimpleObjectProperty<BibEntry> bibEntryProperty = new SimpleObjectProperty<>(new BibEntry());
        bibEntry = EasyBind.wrapNullable(bibEntryProperty);

        ContextMenuFactory.SingleContextCommandFactory singleCommandFactory = (action, file) ->
                new ContextAction(action, file, databaseContext, bibEntry, guiPreferences, viewModel);

        ContextMenuFactory.MultiContextCommandFactory multiCommandFactory = (action, files) ->
                new MultiContextAction(action, files, databaseContext, bibEntry, guiPreferences, viewModel);

        factory = new ContextMenuFactory(
                dialogService,
                guiPreferences,
                databaseContext,
                bibEntry,
                viewModel,
                singleCommandFactory,
                multiCommandFactory
        );
    }

    private LinkedFileViewModel mockLocalExistingFile(String linkName) {
        LinkedFile lf = mock(LinkedFile.class);
        when(lf.isOnlineLink()).thenReturn(false);
        when(lf.linkProperty()).thenReturn(new SimpleStringProperty(linkName));
        when(lf.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));
        when(lf.getSourceUrl()).thenReturn("");
        when(lf.findIn(eq(databaseContext), eq(filePreferences))).thenReturn(Optional.of(Path.of(linkName)));

        LinkedFileViewModel vm = mock(LinkedFileViewModel.class);
        when(vm.getFile()).thenReturn(lf);
        when(vm.isGeneratedNameSameAsOriginal()).thenReturn(false);
        when(vm.isGeneratedPathSameAsOriginal()).thenReturn(false);
        return vm;
    }

    private boolean hasItemLike(ContextMenu menu, String needleLower) {
        return menu.getItems().stream().anyMatch(i -> {
            String t = i.getText();
            return t != null && t.toLowerCase().contains(needleLower);
        });
    }

    @Test
    void multiMenuHasExpectedItemCount() {
        LinkedFileViewModel file1 = mockLocalExistingFile("file1.pdf");
        LinkedFileViewModel file2 = mockLocalExistingFile("file2.pdf");
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(file1, file2);

        ContextMenu menu = factory.createForSelection(sel);
        assertNotNull(menu);
        assertEquals(11, menu.getItems().size());
    }

    @Test
    void multiMenuContainsExpectedCommandsByText() {
        LinkedFileViewModel file1 = mockLocalExistingFile("file1.pdf");
        LinkedFileViewModel file2 = mockLocalExistingFile("file2.pdf");
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(file1, file2);

        ContextMenu menu = factory.createForSelection(sel);

        assertTrue(hasItemLike(menu, "open file"));
        assertTrue(hasItemLike(menu, "open folder"));
        assertTrue(hasItemLike(menu, "download"));
        assertTrue(hasItemLike(menu, "redownload"));
        assertTrue(hasItemLike(menu, "move file"));
        assertTrue(hasItemLike(menu, "copy linked file"));
        assertTrue(hasItemLike(menu, "remove link"));
        assertTrue(hasItemLike(menu, "delete"));
    }

    @Test
    void removeLinksInvokesViewModelOnAllSelected() {
        LinkedFileViewModel file1 = mockLocalExistingFile("file1.pdf");
        LinkedFileViewModel file2 = mockLocalExistingFile("file2.pdf");
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(file1, file2);

        ContextMenu menu = factory.createForSelection(sel);

        menu.getItems().stream()
            .filter(item -> item.getText() != null && item.getText().toLowerCase().contains("remove links"))
            .findFirst()
            .ifPresent(item -> item.getOnAction().handle(null));

        verify(viewModel).removeFileLink(file1);
        verify(viewModel).removeFileLink(file2);
    }

    @Test
    void copyFilesToFolderShowsDirectoryDialog() {
        LinkedFileViewModel file1 = mockLocalExistingFile("file1.pdf");
        LinkedFileViewModel file2 = mockLocalExistingFile("file2.pdf");
        ObservableList<LinkedFileViewModel> sel = FXCollections.observableArrayList(file1, file2);

        when(dialogService.showDirectorySelectionDialog(any())).thenReturn(Optional.empty());

        ContextMenu menu = factory.createForSelection(sel);

        menu.getItems().stream()
            .filter(item -> item.getText() != null && item.getText().toLowerCase().contains("copy linked file"))
            .findFirst()
            .ifPresent(item -> item.getOnAction().handle(null));

        verify(dialogService).showDirectorySelectionDialog(any());
    }
}
