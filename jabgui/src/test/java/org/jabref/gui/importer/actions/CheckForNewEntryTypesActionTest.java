package org.jabref.gui.importer.actions;

import java.io.Reader;
import java.util.List;

import org.jabref.gui.DialogService;
import org.jabref.gui.importer.BibEntryTypePrefsAndFileViewModel;
import org.jabref.gui.importer.ImportCustomEntryTypesDialogViewModel;
import org.jabref.logic.LibraryPreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;

import com.airhacks.afterburner.injection.Injector;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// The library is a reduced version of the one attached to <https://github.com/JabRef/jabref/issues/9930>:
/// `audio` and `review` customize entry types JabRef ships, `manuscript` is unknown to JabRef.
@NullMarked
class CheckForNewEntryTypesActionTest {

    private static final String LIBRARY = """
            @Manuscript{NAK5-333,
              library   = {National Archives, Kathmandu},
              shelfmark = {5-333},
            }

            @Comment{jabref-meta: databaseType:biblatex;}

            @Comment{jabref-entrytype: audio: req[author;date;publisher;title] opt[url;urldate]}

            @Comment{jabref-entrytype: manuscript: req[library;location;shelfmark] opt[origin;scribe]}

            @Comment{jabref-entrytype: review: req[author;date;journaltitle;pages] opt[doi;url]}
            """;

    private final CheckForNewEntryTypesAction action = new CheckForNewEntryTypesAction();
    private final DialogService dialogService = mock(DialogService.class);

    private BibEntryTypesManager entryTypesManager;
    private CliPreferences preferences;
    private ParserResult parserResult;

    @BeforeEach
    void setUp() throws Exception {
        entryTypesManager = new BibEntryTypesManager();
        Injector.setModelOrService(BibEntryTypesManager.class, entryTypesManager);

        preferences = mock(CliPreferences.class);
        when(preferences.getLibraryPreferences()).thenReturn(LibraryPreferences.getDefault());

        parserResult = new BibtexParser(mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS))
                .parse(Reader.of(LIBRARY));
    }

    @AfterAll
    static void tearDown() {
        Injector.forgetAll();
    }

    @Test
    void dialogIsOfferedForACustomizingLibrary() {
        assertEquals(3, parserResult.getEntryTypes().size());
        assertTrue(action.isActionNecessary(parserResult, dialogService, preferences));
    }

    /// Reproduces <https://github.com/JabRef/jabref/issues/9930>: after the user accepted all offered entry types,
    /// opening the very same library must not ask again - not even for the customized types JabRef ships.
    @Test
    void dialogIsNotOfferedAgainAfterTheUserAcceptedEverything() {
        importAllOfferedEntryTypes();

        assertFalse(action.isActionNecessary(parserResult, dialogService, preferences));
    }

    /// What the dialog does when the user checks every box, without going through JavaFX.
    private void importAllOfferedEntryTypes() {
        List<BibEntryType> typesInFile = List.copyOf(parserResult.getEntryTypes());
        ImportCustomEntryTypesDialogViewModel viewModel = new ImportCustomEntryTypesDialogViewModel(
                BibDatabaseMode.BIBLATEX, typesInFile, preferences, entryTypesManager);

        viewModel.importBibEntryTypes(
                List.copyOf(viewModel.newTypes()),
                viewModel.differentCustomizations().stream()
                         .map(BibEntryTypePrefsAndFileViewModel::customTypeFromFile)
                         .toList());
    }
}
