package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.bibtex.FieldContentFormatterPreferences;
import org.jabref.logic.bibtex.FieldWriterPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.GeneralPreferences;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaveDatabaseActionTest {

    private static final String TEST_BIBTEX_LIBRARY_LOCATION = "C:\\Users\\John_Doe\\Jabref\\literature.bib";
    private Path file = Path.of(TEST_BIBTEX_LIBRARY_LOCATION);
    private DialogService dialogService = mock(DialogService.class);
    private JabRefPreferences preferences = mock(JabRefPreferences.class);
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private BibDatabaseContext dbContext = spy(BibDatabaseContext.class);
    private SaveDatabaseAction saveDatabaseAction;

    @BeforeEach
    public void setUp() {
        when(libraryTab.frame()).thenReturn(jabRefFrame);
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveDatabaseAction = spy(new SaveDatabaseAction(libraryTab, preferences, mock(BibEntryTypesManager.class)));
    }

    @Test
    public void saveAsShouldSetWorkingDirectory() {
        when(preferences.getWorkingDir()).thenReturn(Path.of(TEST_BIBTEX_LIBRARY_LOCATION));
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.of(file));
        doReturn(true).when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(1)).setWorkingDirectory(file.getParent());
    }

    @Test
    public void saveAsShouldNotSetWorkingDirectoryIfNotSelected() {
        when(preferences.getWorkingDir()).thenReturn(Path.of(TEST_BIBTEX_LIBRARY_LOCATION));
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.empty());
        doReturn(false).when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(0)).setWorkingDirectory(file.getParent());
    }

    @Test
    public void saveShouldShowSaveAsIfDatabaseNotSelected() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(file));
        doReturn(true).when(saveDatabaseAction).saveAs(any(), any());

        saveDatabaseAction.save();

        verify(saveDatabaseAction, times(1)).saveAs(file, SaveDatabaseAction.SaveDatabaseMode.NORMAL);
    }

    private SaveDatabaseAction createSaveDatabaseActionForBibDatabase(BibDatabase database) throws IOException {
        file = Files.createTempFile("JabRef", ".bib");
        file.toFile().deleteOnExit();

        FieldWriterPreferences fieldWriterPreferences = mock(FieldWriterPreferences.class);
        when(fieldWriterPreferences.getFieldContentFormatterPreferences()).thenReturn(mock(FieldContentFormatterPreferences.class));
        GeneralPreferences generalPreferences = mock(GeneralPreferences.class);
        SavePreferences savePreferences = mock(SavePreferences.class);
        // In case a "thenReturn" is modified, the whole mock has to be recreated
        dbContext = mock(BibDatabaseContext.class);
        libraryTab = mock(LibraryTab.class);
        MetaData metaData = mock(MetaData.class);
        when(savePreferences.withSaveType(any(SavePreferences.DatabaseSaveType.class))).thenReturn(savePreferences);
        when(generalPreferences.getDefaultEncoding()).thenReturn(StandardCharsets.UTF_8);
        when(savePreferences.getFieldWriterPreferences()).thenReturn(fieldWriterPreferences);
        GlobalCitationKeyPattern emptyGlobalCitationKeyPattern = GlobalCitationKeyPattern.fromPattern("");
        when(metaData.getCiteKeyPattern(any(GlobalCitationKeyPattern.class))).thenReturn(emptyGlobalCitationKeyPattern);
        when(savePreferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class));
        when(savePreferences.getCitationKeyPatternPreferences().getKeyPattern()).thenReturn(emptyGlobalCitationKeyPattern);
        when(dbContext.getDatabasePath()).thenReturn(Optional.of(file));
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(dbContext.getDatabase()).thenReturn(database);
        when(dbContext.getMetaData()).thenReturn(metaData);
        when(dbContext.getEntries()).thenReturn(database.getEntries());
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);
        when(preferences.getGeneralPreferences()).thenReturn(generalPreferences);
        when(preferences.getFieldContentParserPreferences()).thenReturn(mock(FieldContentFormatterPreferences.class));
        when(preferences.getSavePreferences()).thenReturn(savePreferences);
        when(libraryTab.frame()).thenReturn(jabRefFrame);
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        when(libraryTab.getUndoManager()).thenReturn(mock(CountingUndoManager.class));
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        saveDatabaseAction = new SaveDatabaseAction(libraryTab, preferences, mock(BibEntryTypesManager.class));
        return saveDatabaseAction;
    }

    @Test
    public void saveKeepsChangedFlag() throws Exception {
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
    public void saveShouldNotSaveDatabaseIfPathNotSet() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        boolean result = saveDatabaseAction.save();
        assertFalse(result);
    }
}
