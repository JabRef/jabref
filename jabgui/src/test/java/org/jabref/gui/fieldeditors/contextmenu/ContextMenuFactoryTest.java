package org.jabref.gui.fieldeditors.contextmenu;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

import org.jabref.gui.DialogService;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(ApplicationExtension.class)
class ContextMenuFactoryTest {

    @Test
    void createMenuForSelection_returnsEmptyMenu_whenSelectionIsNull() {
        DialogService dialogService = mock(DialogService.class);
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
        ObservableOptionalValue<BibEntry> bibEntryOptional = mockEmptyBibEntryOptional();

        ContextMenuFactory contextMenuFactory = new ContextMenuFactory(
                dialogService, guiPreferences, bibDatabaseContext, bibEntryOptional, mockEditorViewModel()
        );

        ContextMenu contextMenu = contextMenuFactory.createMenuForSelection(null);

        assertNotNull(contextMenu);
        assertTrue(contextMenu.getItems().isEmpty(), "Menu should be empty for null selection");
    }

    @Test
    void createMenuForSelection_returnsEmptyMenu_whenSelectionIsEmpty() {
        DialogService dialogService = mock(DialogService.class);
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
        ObservableOptionalValue<BibEntry> bibEntryOptional = mockEmptyBibEntryOptional();

        ContextMenuFactory contextMenuFactory = new ContextMenuFactory(
                dialogService, guiPreferences, bibDatabaseContext, bibEntryOptional, mockEditorViewModel()
        );

        ObservableList<LinkedFileViewModel> emptySelection = FXCollections.observableArrayList();
        ContextMenu contextMenu = contextMenuFactory.createMenuForSelection(emptySelection);

        assertNotNull(contextMenu);
        assertTrue(contextMenu.getItems().isEmpty(), "Menu should be empty for empty selection");
    }

    @Test
    void createMenuForSelection_returnsNonEmptyMenu_forSingleSelection_offlineExistingFile() {
        DialogService dialogService = mock(DialogService.class);
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

        BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
        ObservableOptionalValue<BibEntry> bibEntryOptional = mockEmptyBibEntryOptional();

        LinkedFileViewModel offlineExistingFileViewModel = mockOfflineExistingFileViewModel(
                bibDatabaseContext, filePreferences, ""
        );

        ObservableList<LinkedFileViewModel> singleSelection = FXCollections.observableArrayList(offlineExistingFileViewModel);

        ContextMenuFactory contextMenuFactory = new ContextMenuFactory(
                dialogService, guiPreferences, bibDatabaseContext, bibEntryOptional, mockEditorViewModel()
        );

        ContextMenu contextMenu = contextMenuFactory.createMenuForSelection(singleSelection);

        assertNotNull(contextMenu);
        assertFalse(contextMenu.getItems().isEmpty(), "Single-selection menu should not be empty");
    }

    @Test
    void createMenuForSelection_returnsNonEmptyMenu_andContainsExpectedItems_forMultiSelection_mixed() {
        DialogService dialogService = mock(DialogService.class);
        GuiPreferences guiPreferences = mock(GuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(guiPreferences.getFilePreferences()).thenReturn(filePreferences);

        BibDatabaseContext bibDatabaseContext = mock(BibDatabaseContext.class);
        ObservableOptionalValue<BibEntry> bibEntryOptional = mockEmptyBibEntryOptional();

        LinkedFileViewModel offlineExistingFileViewModel = mockOfflineExistingFileViewModel(
                bibDatabaseContext, filePreferences, "https://example.com/file.pdf"
        );
        LinkedFileViewModel onlineFileViewModel = mockOnlineFileViewModel(
        );

        ObservableList<LinkedFileViewModel> multiSelection = FXCollections.observableArrayList(
                List.of(offlineExistingFileViewModel, onlineFileViewModel)
        );

        ContextMenuFactory contextMenuFactory = new ContextMenuFactory(
                dialogService, guiPreferences, bibDatabaseContext, bibEntryOptional, mockEditorViewModel()
        );

        ContextMenu contextMenu = contextMenuFactory.createMenuForSelection(multiSelection);

        assertNotNull(contextMenu);
        assertFalse(contextMenu.getItems().isEmpty(), "Multi-selection menu should not be empty");
        assertTrue(containsMenuItemWithText(contextMenu, "Remove link"),
                "Menu should contain 'Remove link' in multi-selection");
        assertTrue(containsMenuItemWithText(contextMenu, "Copy linked file"),
                "Menu should contain 'Copy linked file' item");
    }

    // ---------- helpers ----------

    private static boolean containsMenuItemWithText(ContextMenu contextMenu, String expectedFragment) {
        return contextMenu.getItems().stream()
                          .map(MenuItem::getText)
                          .filter(Objects::nonNull)
                          .anyMatch(text -> text.contains(expectedFragment));
    }

    private static org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel mockEditorViewModel() {
        return mock(org.jabref.gui.fieldeditors.LinkedFilesEditorViewModel.class);
    }

    private static ObservableOptionalValue<BibEntry> mockEmptyBibEntryOptional() {
        @SuppressWarnings("unchecked")
        ObservableOptionalValue<BibEntry> optional =
                (ObservableOptionalValue<BibEntry>) mock(ObservableOptionalValue.class);

        when(optional.getValue()).thenReturn(Optional.empty());
        return optional;
    }

    private static LinkedFileViewModel mockOfflineExistingFileViewModel(BibDatabaseContext bibDatabaseContext,
                                                                        FilePreferences filePreferences,
                                                                        String sourceUrl) {
        LinkedFile modelLinkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(modelLinkedFile.isOnlineLink()).thenReturn(false);
        when(modelLinkedFile.findIn(bibDatabaseContext, filePreferences)).thenReturn(Optional.of(Path.of("dummy.pdf")));
        when(modelLinkedFile.linkProperty()).thenReturn(new SimpleStringProperty("dummy.pdf"));
        when(modelLinkedFile.getSourceUrl()).thenReturn(sourceUrl == null ? "" : sourceUrl);
        when(modelLinkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(sourceUrl == null ? "" : sourceUrl));

        LinkedFileViewModel fileViewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(fileViewModel.getFile()).thenReturn(modelLinkedFile);
        when(fileViewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(false);
        return fileViewModel;
    }

    private static LinkedFileViewModel mockOnlineFileViewModel() {
        LinkedFile modelLinkedFile = mock(LinkedFile.class, Answers.RETURNS_DEEP_STUBS);
        when(modelLinkedFile.isOnlineLink()).thenReturn(true);
        when(modelLinkedFile.findIn(any(), any())).thenReturn(Optional.empty());
        when(modelLinkedFile.linkProperty()).thenReturn(new SimpleStringProperty("https://host/file2.pdf"));
        when(modelLinkedFile.getSourceUrl()).thenReturn("https://host/file2.pdf");
        when(modelLinkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty("https://host/file2.pdf"));

        LinkedFileViewModel fileViewModel = mock(LinkedFileViewModel.class, Answers.RETURNS_DEEP_STUBS);
        when(fileViewModel.getFile()).thenReturn(modelLinkedFile);
        when(fileViewModel.isGeneratedPathSameAsOriginal()).thenReturn(false);
        when(fileViewModel.isGeneratedNameSameAsOriginal()).thenReturn(false);
        return fileViewModel;
    }
}
