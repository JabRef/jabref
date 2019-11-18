package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.undo.CountingUndoManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.bibtex.FieldContentParserPreferences;
import org.jabref.logic.bibtex.LatexFieldFormatterPreferences;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.model.bibtexkeypattern.GlobalBibtexKeyPattern;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
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
    private BasePanel basePanel = mock(BasePanel.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private BibDatabaseContext dbContext = spy(BibDatabaseContext.class);
    private SaveDatabaseAction saveDatabaseAction;

    @BeforeEach
    public void setUp() {
        when(basePanel.frame()).thenReturn(jabRefFrame);
        when(basePanel.getBibDatabaseContext()).thenReturn(dbContext);
        when(jabRefFrame.getDialogService()).thenReturn(dialogService);

        saveDatabaseAction = spy(new SaveDatabaseAction(basePanel, preferences, mock(BibEntryTypesManager.class)));
    }

    @Test
    public void saveAsShouldSetWorkingDirectory() {
        when(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn(TEST_BIBTEX_LIBRARY_LOCATION);
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.of(file));
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(1)).setWorkingDir(file.getParent());
    }

    @Test
    public void saveAsShouldNotSetWorkingDirectoryIfNotSelected() {
        when(preferences.get(JabRefPreferences.WORKING_DIRECTORY)).thenReturn(TEST_BIBTEX_LIBRARY_LOCATION);
        when(dialogService.showFileSaveDialog(any(FileDialogConfiguration.class))).thenReturn(Optional.empty());
        doNothing().when(saveDatabaseAction).saveAs(any());

        saveDatabaseAction.saveAs();

        verify(preferences, times(0)).setWorkingDir(file.getParent());
    }

    @Test
    public void saveAsShouldSetNewDatabasePathIntoContext() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);

        saveDatabaseAction.saveAs(file);

        verify(dbContext, times(1)).setDatabaseFile(file);
    }

    @Test
    public void saveShouldShowSaveAsIfDatabaseNotSelected() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(file));
        doNothing().when(saveDatabaseAction).saveAs(file);

        saveDatabaseAction.save();

        verify(saveDatabaseAction, times(1)).saveAs(file);
    }

    private SaveDatabaseAction createSaveDatabaseActionForBibDatabase(BibDatabase database) throws IOException  {
        file = Files.createTempFile("JabRef", ".bib");
        file.toFile().deleteOnExit();

        LatexFieldFormatterPreferences latexFieldFormatterPreferences = mock(LatexFieldFormatterPreferences.class);
        when(latexFieldFormatterPreferences.getFieldContentParserPreferences()).thenReturn(mock(FieldContentParserPreferences.class));
        SavePreferences savePreferences = mock(SavePreferences.class);
        // In case a "thenReturn" is modified, the whole mock has to be recreated
        dbContext = mock(BibDatabaseContext.class);
        basePanel = mock(BasePanel.class);
        MetaData metaData = mock(MetaData.class);
        when(savePreferences.withEncoding(any(Charset.class))).thenReturn(savePreferences);
        when(savePreferences.withSaveType(any(SavePreferences.DatabaseSaveType.class))).thenReturn(savePreferences);
        when(savePreferences.getEncoding()).thenReturn(Charset.forName("UTF-8"));
        when(savePreferences.getLatexFieldFormatterPreferences()).thenReturn(latexFieldFormatterPreferences);
        GlobalBibtexKeyPattern emptyGlobalBibtexKeyPattern = GlobalBibtexKeyPattern.fromPattern("");
        when(savePreferences.getGlobalCiteKeyPattern()).thenReturn(emptyGlobalBibtexKeyPattern);
        when(metaData.getCiteKeyPattern(any(GlobalBibtexKeyPattern.class))).thenReturn(emptyGlobalBibtexKeyPattern);
        when(dbContext.getDatabasePath()).thenReturn(Optional.of(file));
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(dbContext.getDatabase()).thenReturn(database);
        when(dbContext.getMetaData()).thenReturn(metaData);
        when(dbContext.getEntries()).thenReturn(database.getEntries());
        when(preferences.getBoolean(JabRefPreferences.LOCAL_AUTO_SAVE)).thenReturn(false);
        when(preferences.getDefaultEncoding()).thenReturn(Charset.forName("UTF-8"));
        when(preferences.getFieldContentParserPreferences()).thenReturn(mock(FieldContentParserPreferences.class));
        when(preferences.loadForSaveFromPreferences()).thenReturn(savePreferences);
        when(basePanel.frame()).thenReturn(jabRefFrame);
        when(basePanel.getBibDatabaseContext()).thenReturn(dbContext);
        when(basePanel.getUndoManager()).thenReturn(mock(CountingUndoManager.class));
        when(basePanel.getBibDatabaseContext()).thenReturn(dbContext);
        saveDatabaseAction = new SaveDatabaseAction(basePanel, preferences, mock(BibEntryTypesManager.class));
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

        assertThat(database.getEntries()).extracting(BibEntry::hasChanged).doesNotContain(false);
    }

    @Test
    public void saveShouldNotSaveDatabaseIfPathNotSet() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());

        boolean result = saveDatabaseAction.save();

        assertFalse(result);
    }
}
