package org.jabref.gui.edit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.clipboard.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefDialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.push.PushToApplicationPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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

        PushToApplicationPreferences pushToApplicationPreferences = mock(PushToApplicationPreferences.class);
        when(pushToApplicationPreferences.getCiteCommand()).thenReturn(new CitationCommandString("\\cite{", ",", "}"));
        when(preferences.getPushToApplicationPreferences()).thenReturn(pushToApplicationPreferences);
    }

    @Test
    void executeOnFail() {
        when(stateManager.getActiveDatabase()).thenReturn(Optional.empty());
        when(stateManager.getSelectedEntries()).thenReturn(FXCollections.emptyObservableList());
        copyMoreAction = new CopyMoreAction(StandardActions.COPY_FIELD_TITLE, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(0)).notify(any(String.class));
    }

    static Stream<Arguments> getTestParams() {
        // Given is a list with 2 entries with 1 with an undefined field
        return Stream.of(
                Arguments.of(StandardActions.COPY_FIELD_TITLE,
                        (Consumer<BibEntry>) entry -> entry.clearField(StandardField.TITLE),
                        Localization.lang("None of the selected entries have %0.", "Title"),
                        Localization.lang("Warning: %0 out of %1 entries have undefined %2.", "1", "2", "Title")),

                Arguments.of(StandardActions.COPY_CITATION_KEY,
                        (Consumer<BibEntry>) BibEntry::clearCiteKey,
                        Localization.lang("None of the selected entries have citation keys."),
                        Localization.lang("Warning: %0 out of %1 entries have undefined citation key.", "1", "2")),

                Arguments.of(StandardActions.COPY_DOI,
                        (Consumer<BibEntry>) entry -> entry.clearField(StandardField.DOI),
                        Localization.lang("None of the selected entries have DOIs."),
                        Localization.lang("Warning: %0 out of %1 entries have undefined DOIs.", "1", "2")),

                Arguments.of(StandardActions.COPY_FIELD_AUTHOR,
                        (Consumer<BibEntry>) entry -> entry.clearField(StandardField.AUTHOR),
                        Localization.lang("None of the selected entries have %0.", "Author"),
                        Localization.lang("Warning: %0 out of %1 entries have undefined %2.", "1", "2", "Author")),

                Arguments.of(StandardActions.COPY_FIELD_JOURNAL,
                        (Consumer<BibEntry>) entry -> {
                            entry.clearField(StandardField.JOURNAL);
                            entry.clearField(StandardField.JOURNALTITLE);
                        },
                        Localization.lang("None of the selected entries have %0.", "Journal"),
                        Localization.lang("Warning: %0 out of %1 entries have undefined %2.", "1", "2", "Journal")),

                Arguments.of(StandardActions.COPY_FIELD_DATE,
                        (Consumer<BibEntry>) entry -> {
                            entry.clearField(StandardField.DATE);
                            entry.clearField(StandardField.YEAR);
                        },
                        Localization.lang("None of the selected entries have %0.", "Date"),
                        Localization.lang("Warning: %0 out of %1 entries have undefined %2.", "1", "2", "Date")),

                Arguments.of(StandardActions.COPY_FIELD_KEYWORDS,
                        (Consumer<BibEntry>) entry -> entry.clearField(StandardField.KEYWORDS),
                        Localization.lang("None of the selected entries have %0.", "Keywords"),
                        Localization.lang("Warning: %0 out of %1 entries have undefined %2.", "1", "2", "Keywords")),

                Arguments.of(StandardActions.COPY_FIELD_ABSTRACT,
                        (Consumer<BibEntry>) entry -> entry.clearField(StandardField.ABSTRACT),
                        Localization.lang("None of the selected entries have %0.", "Abstract"),
                        Localization.lang("Warning: %0 out of %1 entries have undefined %2.", "1", "2", "Abstract"))
        );
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void executeWithNoValue(StandardActions action, Consumer<BibEntry> remover, String expectedNoneMessage, String ignoredWarning) {
        BibEntry modified = new BibEntry(entry);
        remover.accept(modified);
        ObservableList<BibEntry> entries = FXCollections.observableArrayList(modified);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entries);
        copyMoreAction = new CopyMoreAction(action, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        verify(clipBoardManager, times(0)).setContent(any(String.class));
        verify(dialogService, times(1)).notify(expectedNoneMessage);
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void executeOnPartialSuccess(StandardActions action, Consumer<BibEntry> remover, String ignoredNone, String expectedWarning) {
        BibEntry modified = new BibEntry(entry);
        remover.accept(modified);
        ObservableList<BibEntry> mixedEntries = FXCollections.observableArrayList(modified, entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(mixedEntries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(mixedEntries);
        copyMoreAction = new CopyMoreAction(action, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String expectedClipboard = expectedClipboardString(action);
        verify(clipBoardManager, times(1)).setContent(expectedClipboard);
        verify(dialogService, times(1)).notify(expectedWarning);
    }

    @ParameterizedTest
    @MethodSource("getTestParams")
    void executeOnSuccess(StandardActions action, Consumer<BibEntry> remover, String ignoredNone, String ignoredWarning) {
        ObservableList<BibEntry> entries = FXCollections.observableArrayList(entry);
        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase(entries));

        when(stateManager.getActiveDatabase()).thenReturn(Optional.of(databaseContext));
        when(stateManager.getSelectedEntries()).thenReturn(entries);
        copyMoreAction = new CopyMoreAction(action, dialogService, stateManager, clipBoardManager, preferences, abbreviationRepository);
        copyMoreAction.execute();

        String expectedClipboard = expectedClipboardString(action);
        verify(clipBoardManager, times(1)).setContent(expectedClipboard);
        verify(dialogService, times(1)).notify(Localization.lang("Copied '%0' to clipboard.",
                JabRefDialogService.shortenDialogMessage(expectedClipboard)));
    }

    private String expectedClipboardString(StandardActions action) {
        return String.join("\n", switch (action) {
            case COPY_FIELD_TITLE ->
                    titles;
            case COPY_CITATION_KEY ->
                    keys;
            case COPY_DOI ->
                    dois;
            case COPY_FIELD_AUTHOR ->
                    authors;
            case COPY_FIELD_JOURNAL ->
                    journals;
            case COPY_FIELD_DATE ->
                    dates;
            case COPY_FIELD_KEYWORDS ->
                    keywords;
            case COPY_FIELD_ABSTRACT ->
                    abstracts;
            default ->
                    throw new IllegalArgumentException("Unhandled action: " + action);
        });
    }
}
