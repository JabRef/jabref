package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.preferences.PreferencesService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CopyMoreActionTest {

    private CopyMoreAction copyMoreAction;
    private DialogService dialogService = spy(DialogService.class);
    private ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private PreferencesService preferencesService = mock(PreferencesService.class);
    private StateManager stateManager = mock(StateManager.class);
    private BibEntry entry;
    private List<String> titles = new ArrayList<String>();
    private List<String> keys = new ArrayList<String>();
    private List<String> dois = new ArrayList<String>();

    @BeforeEach
    public void setUp() {
        String title = "A tale from the trenches";
        entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma")
                .withField(StandardField.TITLE, title)
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development")
                .withCitationKey("abc");
        titles.add(title);
        keys.add("abc");
        dois.add("10.1145/3377811.3380330");
    }

    @Test
    public void testExecuteOnFail() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(0)).notify(any(String.class));
    }

    @Test
    public void testExecuteCopyTitleWithNoTitle() {
        BibEntry entryWithNoTitle = (BibEntry) entry.clone();
        entryWithNoTitle.clearField(StandardField.TITLE);
        ObservableList<BibEntry> entriesWithNoTitles = FXCollections.observableArrayList(entryWithNoTitle);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoTitles));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoTitles);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have titles."));
    }

    @Test
    public void testExecuteCopyTitleOnPartialSuccess() {
        BibEntry entryWithNoTitle = (BibEntry) entry.clone();
        entryWithNoTitle.clearField(StandardField.TITLE);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoTitle, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        String copiedTitles = String.join("\n", titles);
        verify(clipBoardManager, times(1)).setContent(copiedTitles);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined title.",
                Integer.toString(mixedEntries.size() - titles.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    public void testExecuteCopyTitleOnSuccess() {
        ObservableList<BibEntry> entriesWithTitles = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithTitles));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithTitles);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        String copiedTitles = String.join("\n", titles);
        verify(clipBoardManager, times(1)).setContent(copiedTitles);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedTitles)));
    }

    @Test
    public void testExecuteCopyKeyWithNoKey() {
        BibEntry entryWithNoKey = (BibEntry) entry.clone();
        entryWithNoKey.clearCiteKey();
        ObservableList<BibEntry> entriesWithNoKeys = FXCollections.observableArrayList(entryWithNoKey);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoKeys));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoKeys);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have citation keys."));
    }

    @Test
    public void testExecuteCopyKeyOnPartialSuccess() {
        BibEntry entryWithNoKey = (BibEntry) entry.clone();
        entryWithNoKey.clearCiteKey();
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoKey, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        String copiedKeys = String.join("\n", keys);
        verify(clipBoardManager, times(1)).setContent(copiedKeys);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                Integer.toString(mixedEntries.size() - titles.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    public void testExecuteCopyKeyOnSuccess() {
        ObservableList<BibEntry> entriesWithKeys = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithKeys));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithKeys);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        String copiedKeys = String.join("\n", keys);
        verify(clipBoardManager, times(1)).setContent(copiedKeys);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedKeys)));
    }

    @Test
    public void testExecuteCopyDoiWithNoDoi() {
        BibEntry entryWithNoDoi = (BibEntry) entry.clone();
        entryWithNoDoi.clearField(StandardField.DOI);
        ObservableList<BibEntry> entriesWithNoDois = FXCollections.observableArrayList(entryWithNoDoi);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoDois));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoDois);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have DOIs."));
    }

    @Test
    public void testExecuteCopyDoiOnPartialSuccess() {
        BibEntry entryWithNoDoi = (BibEntry) entry.clone();
        entryWithNoDoi.clearField(StandardField.DOI);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoDoi, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        String copiedDois = String.join("\n", dois);
        verify(clipBoardManager, times(1)).setContent(copiedDois);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined DOIs.",
                Integer.toString(mixedEntries.size() - titles.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    public void testExecuteCopyDoiOnSuccess() {
        ObservableList<BibEntry> entriesWithDois = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithDois));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithDois);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferencesService);
        copyMoreAction.execute();

        String copiedDois = String.join("\n", dois);
        verify(clipBoardManager, times(1)).setContent(copiedDois);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedDois)));
    }
}
