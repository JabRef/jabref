package org.jabref.gui.fieldeditors.contextmenu;

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
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ContextMenuFactoryTest {

    private static boolean toolkitInitialized = false;

    private DialogService dialogService;
    private GuiPreferences guiPreferences;
    private BibDatabaseContext databaseContext;
    private ObservableOptionalValue<BibEntry> bibEntry;
    private LinkedFilesEditorViewModel viewModel;
    private ContextMenuFactory factory;
    private ContextMenuFactory.SingleContextCommandFactory singleCommandFactory;
    private ContextMenuFactory.MultiContextCommandFactory multiCommandFactory;

    @BeforeAll
    public static void initToolkit() {
        if (!toolkitInitialized) {
            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException e) {
                // Toolkit already initialized by another thread/test
            }
            toolkitInitialized = true;
        }
    }

    @BeforeEach
    public void setUp() {
        dialogService = mock(DialogService.class);
        guiPreferences = mock(GuiPreferences.class);
        databaseContext = mock(BibDatabaseContext.class);
        viewModel = mock(LinkedFilesEditorViewModel.class);

        SimpleObjectProperty<BibEntry> bibEntryProperty = new SimpleObjectProperty<>();
        bibEntry = EasyBind.wrapNullable(bibEntryProperty);
        bibEntryProperty.set(new BibEntry());

        singleCommandFactory = (action, file) ->
                new ContextAction(action, file, databaseContext, bibEntry, guiPreferences, viewModel);

        multiCommandFactory = (action, files) ->
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

    private LinkedFileViewModel mockFileWithLink(String link) {
        LinkedFile linkedFile = mock(LinkedFile.class);
        when(linkedFile.isOnlineLink()).thenReturn(false);
        when(linkedFile.linkProperty()).thenReturn(new SimpleStringProperty(link));
        when(linkedFile.sourceUrlProperty()).thenReturn(new SimpleStringProperty(""));

        LinkedFileViewModel file = mock(LinkedFileViewModel.class);
        when(file.getFile()).thenReturn(linkedFile);
        when(file.isGeneratedNameSameAsOriginal()).thenReturn(false);
        when(file.isGeneratedPathSameAsOriginal()).thenReturn(false);

        return file;
    }

    @Test
    public void createContextMenuForSingleFile() {
        LinkedFileViewModel file = mockFileWithLink("file1.pdf");
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList(file);

        ContextMenu menu = factory.createForSelection(files);
        assertNotNull(menu);
        assertFalse(menu.getItems().isEmpty());
    }

    @Test
    public void createContextMenuForMultipleFiles() {
        LinkedFileViewModel file1 = mockFileWithLink("file1.pdf");
        LinkedFileViewModel file2 = mockFileWithLink("file2.pdf");
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList(file1, file2);

        ContextMenu menu = factory.createForSelection(files);
        assertNotNull(menu);
        assertEquals(1, menu.getItems().size());
    }

    @Test
    public void createContextMenuForEmptySelection() {
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList();
        ContextMenu menu = factory.createForSelection(files);

        assertNotNull(menu);
        assertTrue(menu.getItems().isEmpty());
    }

    @Test
    public void removeLinkActionCallsViewModelForSingleFile() {
        LinkedFileViewModel file = mockFileWithLink("file1.pdf");
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList(file);
        ContextMenu menu = factory.createForSelection(files);

        menu.getItems().stream()
            .filter(item -> {
                String text = item.getText();
                return text != null && text.toLowerCase().contains("remove link");
            })
            .findFirst()
            .ifPresent(item -> item.getOnAction().handle(null));

        verify(viewModel).removeFileLink(file);
    }

    @Test
    public void removeLinksActionCallsViewModelForAllSelectedFiles() {
        LinkedFileViewModel file1 = mockFileWithLink("file1.pdf");
        LinkedFileViewModel file2 = mockFileWithLink("file2.pdf");

        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList(file1, file2);
        ContextMenu menu = factory.createForSelection(files);

        menu.getItems().stream()
            .filter(item -> {
                String text = item.getText();
                return text != null && text.toLowerCase().contains("remove links");
            })
            .findFirst()
            .ifPresent(item -> item.getOnAction().handle(null));

        verify(viewModel).removeFileLink(file1);
        verify(viewModel).removeFileLink(file2);
    }
}
