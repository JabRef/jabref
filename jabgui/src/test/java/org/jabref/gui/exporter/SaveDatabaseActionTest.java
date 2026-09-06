package org.jabref.gui.exporter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.undo.GuiUndoManager;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.bibtex.FieldPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SaveConfiguration;
import org.jabref.logic.git.preferences.GitPreferences;
import org.jabref.logic.git.util.GitHandlerRegistry;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.SaveOrder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
    private final StateManager stateManager = mock(StateManager.class);
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private BibDatabaseContext dbContext = spy(BibDatabaseContext.class);
    private SaveDatabaseAction saveDatabaseAction;
    private MetaData metaData;

    @BeforeEach
    void setUp() {
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        when(filePreferences.getWorkingDirectory()).thenReturn(Path.of(TEST_BIBTEX_LIBRARY_LOCATION));
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(preferences.getExportPreferences()).thenReturn(mock(ExportPreferences.class));
        saveDatabaseAction = spy(newSaveDatabaseAction(libraryTab, mock(BibEntryTypesManager.class)));
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
        doReturn(true).when(saveDatabaseAction).saveAs(any(), any(), anyBoolean());

        saveDatabaseAction.save();

        verify(saveDatabaseAction, times(1)).saveAs(file, SaveDatabaseAction.SaveDatabaseMode.NORMAL, true);
    }

    @Test
    void saveReportsRunningSaveInsteadOfSuccess() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.of(file));
        when(libraryTab.isSaving()).thenReturn(true);

        assertEquals(SaveDatabaseAction.SaveResult.ALREADY_SAVING, saveDatabaseAction.save(SaveDatabaseAction.SaveDatabaseMode.SILENT));
    }

    @Test
    void saveAsDoesNotAdoptTheNewPathWhileASaveIsRunning() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.of(file));
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(libraryTab.isSaving()).thenReturn(true);

        assertFalse(saveDatabaseAction.saveAs(Path.of("other.bib"), SaveDatabaseAction.SaveDatabaseMode.SILENT));
        verify(dbContext, never()).setDatabasePath(any());
    }

    @Test
    void unsuccessfulSaveAsBringsTheManagersOfTheOriginalLibraryBack() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.of(file));
        when(dbContext.getLocation()).thenReturn(DatabaseLocation.LOCAL);
        when(libraryTab.isSaving()).thenReturn(true);

        saveDatabaseAction.saveAs(Path.of("other.bib"), SaveDatabaseAction.SaveDatabaseMode.SILENT);

        verify(libraryTab, times(1)).installAutosaveManagerAndBackupManager();
        verify(libraryTab, times(1)).createSearchContext();
    }

    private SaveDatabaseAction createSaveDatabaseActionForBibDatabase(BibDatabase database) throws IOException {
        file = Files.createTempFile("JabRef", ".bib");
        file.toFile().deleteOnExit();

        FieldPreferences fieldPreferences = mock(FieldPreferences.class);
        SaveConfiguration saveConfiguration = mock(SaveConfiguration.class);
        // In case a "thenReturn" is modified, the whole mock has to be recreated
        dbContext = mock(BibDatabaseContext.class);
        libraryTab = mock(LibraryTab.class);
        metaData = mock(MetaData.class);
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
        when(preferences.getAbbreviationPreferences()).thenReturn(mock(AbbreviationPreferences.class));
        when(preferences.getCitationKeyPatternPreferences()).thenReturn(mock(CitationKeyPatternPreferences.class));
        when(preferences.getCitationKeyPatternPreferences().getKeyPatterns()).thenReturn(emptyGlobalCitationKeyPatterns);
        when(preferences.getFieldPreferences().getNonWrappableFields()).thenReturn(FXCollections.emptyObservableList());
        when(preferences.getLibraryPreferences()).thenReturn(mock(LibraryPreferences.class));
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        when(libraryTab.getUndoManager()).thenReturn(mock(GuiUndoManager.class));
        when(libraryTab.getBibDatabaseContext()).thenReturn(dbContext);
        saveDatabaseAction = newSaveDatabaseAction(libraryTab, mock(BibEntryTypesManager.class));
        return saveDatabaseAction;
    }

    private SaveDatabaseAction newSaveDatabaseAction(LibraryTab libraryTab, BibEntryTypesManager entryTypesManager) {
        return new SaveDatabaseAction(
                libraryTab,
                dialogService,
                preferences,
                entryTypesManager,
                stateManager,
                mock(JournalAbbreviationRepository.class),
                new GitHandlerRegistry(mock(GitPreferences.class)),
                new CurrentThreadTaskExecutor());
    }

    @Test
    void saveKeepsChangedFlag() throws IOException {
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
                List.of());
    }

    @Test
    void saveShouldNotSaveDatabaseIfPathNotSet() {
        when(dbContext.getDatabasePath()).thenReturn(Optional.empty());
        assertEquals(SaveDatabaseAction.SaveResult.FAILURE, saveDatabaseAction.save());
    }

    @Test
    @ExtendWith(ApplicationExtension.class)
    void encodingRetryAbortsWhenFileWasSavedExternallyWhileDialogWasOpen() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Café");
        entry.setChanged(true);
        BibDatabase database = new BibDatabase(List.of(entry));
        saveDatabaseAction = createSaveDatabaseActionForBibDatabase(database);
        when(dbContext.getMetaData().getEncoding()).thenReturn(Optional.of(StandardCharsets.US_ASCII));
        // "Café" is not encodable in US-ASCII, so the encoding-problems dialog opens. While it is open, another
        // program saves the file; the user then chooses to retry with a different encoding.
        when(dialogService.showCustomDialogAndWait(any(String.class), any(DialogPane.class), any(ButtonType.class), any(ButtonType.class)))
                .thenAnswer(invocation -> {
                    Files.writeString(file, "external content");
                    ButtonType tryDifferentEncoding = invocation.getArgument(3);
                    return Optional.of(tryDifferentEncoding);
                });
        when(dialogService.showChoiceDialogAndWait(any(), any(), any(), any(), any())).thenReturn(Optional.of(StandardCharsets.UTF_8));

        SaveDatabaseAction.SaveResult result = saveDatabaseAction.save();

        assertEquals("external content", Files.readString(file));
        assertEquals(SaveDatabaseAction.SaveResult.FAILURE, result);
    }

    @Test
    @ExtendWith(ApplicationExtension.class)
    void ignoredEncodingProblemsReportFailureWhenFileWasSavedExternallyWhileDialogWasOpen() throws Exception {
        BibEntry entry = new BibEntry().withField(StandardField.AUTHOR, "Café");
        entry.setChanged(true);
        BibDatabase database = new BibDatabase(List.of(entry));
        saveDatabaseAction = createSaveDatabaseActionForBibDatabase(database);
        when(dbContext.getMetaData().getEncoding()).thenReturn(Optional.of(StandardCharsets.US_ASCII));
        // While the encoding-problems dialog is open, another program saves the file; the user then clicks "Ignore".
        // The committed first write no longer is the file's content, so the save must not be reported as successful.
        when(dialogService.showCustomDialogAndWait(any(String.class), any(DialogPane.class), any(ButtonType.class), any(ButtonType.class)))
                .thenAnswer(invocation -> {
                    Files.writeString(file, "external content");
                    ButtonType ignore = invocation.getArgument(2);
                    return Optional.of(ignore);
                });

        SaveDatabaseAction.SaveResult result = saveDatabaseAction.save();

        assertEquals("external content", Files.readString(file));
        assertEquals(SaveDatabaseAction.SaveResult.FAILURE, result);
        verify(libraryTab, never()).resetChangedProperties();
    }

    @Test
    void saveSuspendsAndResumesChangeMonitorAroundSuccessfulSave() throws Exception {
        BibDatabase database = new BibDatabase(List.of(new BibEntry().withField(StandardField.AUTHOR, "first")));
        saveDatabaseAction = createSaveDatabaseActionForBibDatabase(database);
        when(libraryTab.isSaving()).thenReturn(false);

        saveDatabaseAction.save();

        verify(libraryTab).suspendChangeMonitor();
        verify(libraryTab).resumeChangeMonitor();
        var inOrder = inOrder(libraryTab);
        inOrder.verify(libraryTab).suspendChangeMonitor();
        inOrder.verify(libraryTab).resumeChangeMonitor();
    }
}
