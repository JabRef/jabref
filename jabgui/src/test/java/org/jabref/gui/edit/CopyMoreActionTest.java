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
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CopyMoreActionTest {

    private final DialogService dialogService = spy(DialogService.class);
    private final ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private final GuiPreferences preferences = mock(GuiPreferences.class);
    private final JournalAbbreviationRepository abbreviationRepository = mock(JournalAbbreviationRepository.class);
    private final StateManager stateManager = mock(StateManager.class);
    private final List<String> titles = new ArrayList<>();
    private final List<String> keys = new ArrayList<>();
    private final List<String> dois = new ArrayList<>();
    private final List<String> authors = new ArrayList<>();
    private final List<String> journals = new ArrayList<>();
    private final List<String> dates = new ArrayList<>();
    private final List<String> keywords = new ArrayList<>();
    private final List<String> abstracts = new ArrayList<>();

    private CopyMoreAction copyMoreAction;
    private BibEntry entry;

    @BeforeEach
    void setUp() {
        String title = "A tale from the trenches";
        String author = "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma";
        String journal = "Journal of the American College of Nutrition";
        String date = "2001-10";
        String keyword = "software engineering, cognitive bias, empirical study";
        String abstractText = "This paper presents a study on cognitive biases in software development.";

        entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, author)
                .withField(StandardField.TITLE, title)
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.JOURNAL, journal)
                .withField(StandardField.DATE, date)
                .withField(StandardField.KEYWORDS, keyword)
                .withField(StandardField.ABSTRACT, abstractText)
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development")
                .withCitationKey("abc");
        titles.add(title);
        keys.add("abc");
        dois.add("10.1145/3377811.3380330");
        authors.add(author);
        journals.add(journal);
        dates.add(date);
        keywords.add(keyword);
        abstracts.add(abstractText);

        ExternalApplicationsPreferences externalApplicationsPreferences = mock(ExternalApplicationsPreferences.class);
        when(externalApplicationsPreferences.getCiteCommand()).thenReturn(new CitationCommandString("\\cite{", ",", "}"));
        when(preferences.getExternalApplicationsPreferences()).thenReturn(externalApplicationsPreferences);
    }

    @Test
    void executeOnFail() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(0)).notify(any(String.class));
    }

    @Test
    void executeCopyTitleWithNoTitle() {
        BibEntry entryWithNoTitle = (BibEntry) entry.clone();
        entryWithNoTitle.clearField(StandardField.TITLE);
        ObservableList<BibEntry> entriesWithNoTitles = FXCollections.observableArrayList(entryWithNoTitle);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoTitles));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoTitles);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have titles."));
    }

    @Test
    void executeCopyTitleOnPartialSuccess() {
        BibEntry entryWithNoTitle = (BibEntry) entry.clone();
        entryWithNoTitle.clearField(StandardField.TITLE);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoTitle, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedTitles = String.join("\n", titles);
        verify(clipBoardManager, times(1)).setContent(copiedTitles);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined title.",
                Integer.toString(mixedEntries.size() - titles.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    void executeCopyTitleOnSuccess() {
        ObservableList<BibEntry> entriesWithTitles = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithTitles));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithTitles);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedTitles = String.join("\n", titles);
        verify(clipBoardManager, times(1)).setContent(copiedTitles);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedTitles)));
    }

    @Test
    void executeCopyKeyWithNoKey() {
        BibEntry entryWithNoKey = (BibEntry) entry.clone();
        entryWithNoKey.clearCiteKey();
        ObservableList<BibEntry> entriesWithNoKeys = FXCollections.observableArrayList(entryWithNoKey);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoKeys));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoKeys);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have citation keys."));
    }

    @Test
    void executeCopyKeyOnPartialSuccess() {
        BibEntry entryWithNoKey = (BibEntry) entry.clone();
        entryWithNoKey.clearCiteKey();
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoKey, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedKeys = String.join("\n", keys);
        verify(clipBoardManager, times(1)).setContent(copiedKeys);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined citation key.",
                Integer.toString(mixedEntries.size() - titles.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    void executeCopyKeyOnSuccess() {
        ObservableList<BibEntry> entriesWithKeys = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithKeys));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithKeys);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_KEY, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedKeys = String.join("\n", keys);
        verify(clipBoardManager, times(1)).setContent(copiedKeys);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedKeys)));
    }

    @Test
    void executeCopyDoiWithNoDoi() {
        BibEntry entryWithNoDoi = (BibEntry) entry.clone();
        entryWithNoDoi.clearField(StandardField.DOI);
        ObservableList<BibEntry> entriesWithNoDois = FXCollections.observableArrayList(entryWithNoDoi);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoDois));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoDois);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have DOIs."));
    }

    @Test
    void executeCopyDoiOnPartialSuccess() {
        BibEntry entryWithNoDoi = (BibEntry) entry.clone();
        entryWithNoDoi.clearField(StandardField.DOI);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoDoi, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedDois = String.join("\n", dois);
        verify(clipBoardManager, times(1)).setContent(copiedDois);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined DOIs.",
                Integer.toString(mixedEntries.size() - titles.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    void executeCopyDoiOnSuccess() {
        ObservableList<BibEntry> entriesWithDois = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithDois));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithDois);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_DOI, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedDois = String.join("\n", dois);
        verify(clipBoardManager, times(1)).setContent(copiedDois);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedDois)));
    }

    @Test
    void executeCopyAuthorWithNoAuthor() {
        BibEntry entryWithNoAuthor = (BibEntry) entry.clone();
        entryWithNoAuthor.clearField(StandardField.AUTHOR);
        ObservableList<BibEntry> entriesWithNoAuthors = FXCollections.observableArrayList(entryWithNoAuthor);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoAuthors));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoAuthors);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_AUTHOR, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have %0.", "authors"));
    }

    @Test
    void executeCopyAuthorOnPartialSuccess() {
        BibEntry entryWithNoAuthor = (BibEntry) entry.clone();
        entryWithNoAuthor.clearField(StandardField.AUTHOR);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoAuthor, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_AUTHOR, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedAuthors = String.join("\n", authors);
        verify(clipBoardManager, times(1)).setContent(copiedAuthors);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined %2.",
                Integer.toString(mixedEntries.size() - authors.size()), Integer.toString(mixedEntries.size()), "authors"));
    }

    @Test
    void executeCopyAuthorOnSuccess() {
        ObservableList<BibEntry> entriesWithAuthors = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithAuthors));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithAuthors);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_AUTHOR, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedAuthors = String.join("\n", authors);
        verify(clipBoardManager, times(1)).setContent(copiedAuthors);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedAuthors)));
    }

    @Test
    void executeCopyJournalWithNoJournal() {
        BibEntry entryWithNoJournal = (BibEntry) entry.clone();
        entryWithNoJournal.clearField(StandardField.JOURNAL);
        entryWithNoJournal.clearField(StandardField.JOURNALTITLE);
        ObservableList<BibEntry> entriesWithNoJournals = FXCollections.observableArrayList(entryWithNoJournal);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoJournals));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoJournals);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_JOURNAL, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have journal names."));
    }

    @Test
    void executeCopyJournalOnPartialSuccess() {
        BibEntry entryWithNoJournal = (BibEntry) entry.clone();
        entryWithNoJournal.clearField(StandardField.JOURNAL);
        entryWithNoJournal.clearField(StandardField.JOURNALTITLE);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoJournal, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_JOURNAL, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedJournals = String.join("\n", journals);
        verify(clipBoardManager, times(1)).setContent(copiedJournals);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined journal names.",
                Integer.toString(mixedEntries.size() - journals.size()), Integer.toString(mixedEntries.size())));
    }

    @Test
    void executeCopyJournalOnSuccess() {
        ObservableList<BibEntry> entriesWithJournals = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithJournals));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithJournals);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_JOURNAL, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedJournals = String.join("\n", journals);
        verify(clipBoardManager, times(1)).setContent(copiedJournals);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedJournals)));
    }

    @Test
    void executeCopyDateWithNoDate() {
        BibEntry entryWithNoDate = (BibEntry) entry.clone();
        entryWithNoDate.clearField(StandardField.DATE);
        ObservableList<BibEntry> entriesWithNoDates = FXCollections.observableArrayList(entryWithNoDate);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoDates));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoDates);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_DATE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have %0.", "dates"));
    }

    @Test
    void executeCopyDateOnPartialSuccess() {
        BibEntry entryWithNoDate = (BibEntry) entry.clone();
        entryWithNoDate.clearField(StandardField.DATE);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoDate, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_DATE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedDates = String.join("\n", dates);
        verify(clipBoardManager, times(1)).setContent(copiedDates);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined %2.",
                Integer.toString(mixedEntries.size() - dates.size()), Integer.toString(mixedEntries.size()), "dates"));
    }

    @Test
    void executeCopyDateOnSuccess() {
        ObservableList<BibEntry> entriesWithDates = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithDates));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithDates);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_DATE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedDates = String.join("\n", dates);
        verify(clipBoardManager, times(1)).setContent(copiedDates);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedDates)));
    }

    @Test
    void executeCopyKeywordsWithNoKeywords() {
        BibEntry entryWithNoKeywords = (BibEntry) entry.clone();
        entryWithNoKeywords.clearField(StandardField.KEYWORDS);
        ObservableList<BibEntry> entriesWithNoKeywords = FXCollections.observableArrayList(entryWithNoKeywords);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoKeywords));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoKeywords);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_KEYWORDS, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have %0.", "keywords"));
    }

    @Test
    void executeCopyKeywordsOnPartialSuccess() {
        BibEntry entryWithNoKeywords = (BibEntry) entry.clone();
        entryWithNoKeywords.clearField(StandardField.KEYWORDS);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoKeywords, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_KEYWORDS, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedKeywords = String.join("\n", keywords);
        verify(clipBoardManager, times(1)).setContent(copiedKeywords);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined %2.",
                Integer.toString(mixedEntries.size() - keywords.size()), Integer.toString(mixedEntries.size()), "keywords"));
    }

    @Test
    void executeCopyKeywordsOnSuccess() {
        ObservableList<BibEntry> entriesWithKeywords = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithKeywords));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithKeywords);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_KEYWORDS, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedKeywords = String.join("\n", keywords);
        verify(clipBoardManager, times(1)).setContent(copiedKeywords);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedKeywords)));
    }

    @Test
    void executeCopyAbstractWithNoAbstract() {
        BibEntry entryWithNoAbstract = (BibEntry) entry.clone();
        entryWithNoAbstract.clearField(StandardField.ABSTRACT);
        ObservableList<BibEntry> entriesWithNoAbstracts = FXCollections.observableArrayList(entryWithNoAbstract);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithNoAbstracts));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithNoAbstracts);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_ABSTRACT, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(Localization.lang("None of the selected entries have %0.", "abstracts"));
    }

    @Test
    void executeCopyAbstractOnPartialSuccess() {
        BibEntry entryWithNoAbstract = (BibEntry) entry.clone();
        entryWithNoAbstract.clearField(StandardField.ABSTRACT);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(entryWithNoAbstract, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_ABSTRACT, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedAbstracts = String.join("\n", abstracts);
        verify(clipBoardManager, times(1)).setContent(copiedAbstracts);
        verify(dialogService, times(1)).notify(Localization.lang("Warning: %0 out of %1 entries have undefined %2.",
                Integer.toString(mixedEntries.size() - abstracts.size()), Integer.toString(mixedEntries.size()), "abstracts"));
    }

    @Test
    void executeCopyAbstractOnSuccess() {
        ObservableList<BibEntry> entriesWithAbstracts = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entriesWithAbstracts));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.ofNullable(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entriesWithAbstracts);
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_ABSTRACT, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String copiedAbstracts = String.join("\n", abstracts);
        verify(clipBoardManager, times(1)).setContent(copiedAbstracts);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(copiedAbstracts)));
    }
}
