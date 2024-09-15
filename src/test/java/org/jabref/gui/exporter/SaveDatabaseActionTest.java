package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveDatabaseActionTest {

    private static final String TEST_BIBTEX_LIBRARY_LOCATION = "C:\\Users\\John_Doe\\Jabref\\literature.bib";
    private Path file = Path.of(TEST_BIBTEX_LIBRARY_LOCATION);
    private final DialogService dialogService = mock(DialogService.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private BibDatabaseContext dbContext = spy(BibDatabaseContext.class);
    private SaveDatabaseAction saveDatabaseAction;

    @BeforeEach
    void setUp() {
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        when(filePreferences.getWorkingDirectory()).thenReturn(Path.of(TEST_BIBTEX_LIBRARY_LOCATION));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExportPreferences()).thenReturn(mock(ExportPreferences.class));
        saveDatabaseAction = spy(new SaveDatabaseAction(libraryTab, dialogService, preferences, mock(BibEntryTypesManager.class)));
    }

    @Test
    void saveAsShouldSetWorkingDirectory() {
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.of(file));
        doReturn(true).when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(filePreferences, times(1)).setWorkingDirectory(file.getParent());
    }

    @Test
    void saveAsShouldNotSetWorkingDirectoryIfNotSelected() {
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.empty());
        doReturn(false).when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(filePreferences, times(0)).setWorkingDirectory(any());
    }

    @Test
    void saveShouldShowSaveAsIfDatabaseNotSelected() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(file));
        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);
        when(libraryPreferences.autoSaveProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(file));
        doReturn(true).when(saveDatabaseAction).saveAs(any(), any());

        saveDatabaseAction.save();

        verify(saveDatabaseAction, times(1)).saveAs(file, SaveDatabaseAction.SaveDatabaseMode.NORMAL);
    }

    private SaveDatabaseAction createSaveDatabaseActionForBibDatabase(BibDatabase database) throws IOException {
        file = Files.createTempFile("JabRef", ".bib");
        file.toFile().deleteOnExit();

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        SaveConfiguration saveConfiguration = mock(SaveConfiguration.class);
        // In case a "thenReturn" is modified, the whole mock has to be recreated
        dbContext = mock(BibDatabaseContext.class);
        libraryTab = mock(LibraryTab.class);
        MetaData metaData = mock(MetaData.class);
        when(saveConfiguration.withSaveType(any(BibDatabaseWriter.SaveType.class))).thenReturn(saveConfiguration);
        when(saveConfiguration.getSaveOrder()).thenReturn(SaveOrder.getDefaultSaveOrder());
        GlobalCitationKeyPatterns emptyGlobalCitationKeyPatterns = GlobalCitationKeyPatterns.fromPattern("");
        when(metaData.getCiteKeyPatterns(any(GlobalCitationKeyPatterns.class))).thenReturn(emptyGlobalCitationKeyPatterns);
        when(dbContext.getDatabasePath()).thenReturn(Optional.of(file));
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(dbContext.getDatabase()).thenReturn(database);
        when(dbContext.getMetaData()).thenReturn(metaData);
        when(dbContext.getEntries()).thenReturn(database.getEntries());
        LibraryPreferences libraryPreferences = mock(LibraryPreferences.class);
        when(preferences.getLibraryPreferences()).thenReturn(libraryPreferences);
        when(libraryPreferences.autoSaveProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(preferences.getFieldPreferences()).thenReturn(fieldPreferences);
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class));
        when(preferences.getCitationKeyPatternPreferences().getKeyPatterns()).thenReturn(emptyGlobalCitationKeyPatterns);
        when(preferences.getFieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(preferences.getLibraryPreferences()).thenReturn(mock(LibraryPreferences.class));
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        when(libraryTab.getUndoManager()).thenReturn(mock(CountingUndoManager.class));
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        saveDatabaseAction = new SaveDatabaseAction(libraryTab, dialogService, preferences, mock(BibEntryTypesManager.class));
        return saveDatabaseAction;
    }

    @Test
    void saveKeepsChangedFlag() throws Exception {
        BibEntry firstEntry = new BibEntry().withField(StandardField.AUTHOR, "first");
        firstEntry.setChanged(true);
        BibEntry secondEntry = new BibEntry().withField(StandardField.AUTHOR, "second");
        secondEntry.setChanged(true);
        BibDatabase database = new BibDatabase(List.of(firstEntry, secondEntry));

        saveDatabaseAction = createSaveDatabaseActionForBibDatabase(database);
        saveDatabaseAction.save();

        assertEquals(database
                        .getEntries().stream()
                        .map(BibEntry::hasChanged).filter(changed -> false).collect(Collectors.toList()),
                Collections.emptyList());
    }

    @Test
    void saveShouldNotSaveDatabaseIfPathNotSet() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        boolean result = saveDatabaseAction.save();
        assertFalse(result);
    }
}
