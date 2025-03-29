package org.jabref.gui.fieldeditors.contextmenu;

import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.optional.ObservableOptionalValue;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class ContextMenuFactoryTest {

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
        // Initialize JavaFX platform once
        Platform.startup(() -> {});
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
    public void testCreateContextMenuForSingleFile() {
        LinkedFileViewModel file = mockFileWithLink("file1.pdf");
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList(file);

        ContextMenu menu = factory.createForSelection(files);
        assertNotNull(menu);
        assertFalse(menu.getItems().isEmpty());
    }

    @Test
    public void testCreateContextMenuForMultipleFiles() {
        LinkedFileViewModel file1 = mockFileWithLink("file1.pdf");
        LinkedFileViewModel file2 = mockFileWithLink("file2.pdf");
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList(file1, file2);

        ContextMenu menu = factory.createForSelection(files);
        assertNotNull(menu);
        assertEquals(1, menu.getItems().size());
    }

    @Test
    public void testCreateContextMenuForEmptySelection() {
        ObservableList<LinkedFileViewModel> files = FXCollections.observableArrayList();
        ContextMenu menu = factory.createForSelection(files);

        assertNotNull(menu);
        assertTrue(menu.getItems().isEmpty());
    }

    @Test
    public void testRemoveLinkActionCallsViewModelForSingleFile() {
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
    public void testRemoveLinksActionCallsViewModelForAllSelectedFiles() {
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
