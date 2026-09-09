package org.jabref.gui.importer;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/// Tests for the dialog offering to store the custom entry types of a just opened library.
///
/// The entry types are the ones of the library attached to <https://github.com/JabRef/jabref/issues/9930>:
/// `manuscript` is unknown to JabRef, whereas `audio` is a customization of a type JabRef ships.
///
/// [utest->req~import.entry-types.offered-once~1]
class ImportCustomEntryTypesDialogViewModelTest {

    private static final BibDatabaseMode MODE = BibDatabaseMode.BIBLATEX;

    private static final BibEntryType MANUSCRIPT_FROM_FILE = parse(
            "jabref-entrytype: manuscript: req[library;location;shelfmark] opt[origin;scribe]");
    private static final BibEntryType AUDIO_FROM_FILE = parse(
            "jabref-entrytype: audio: req[author;date;publisher;title] opt[url;urldate]");

    private BibEntryTypesManager entryTypesManager;
    private CliPreferences preferences;

    private static BibEntryType parse(String comment) {
        return MetaDataParser.parseCustomEntryType(comment).orElseThrow();
    }

    @BeforeEach
    void setUp() {
        entryTypesManager = new BibEntryTypesManager();
        preferences = mock(CliPreferences.class);
    }

    private ImportCustomEntryTypesDialogViewModel viewModelFor(List<BibEntryType> typesInFile) {
        return new ImportCustomEntryTypesDialogViewModel(MODE, typesInFile, preferences, entryTypesManager);
    }

    @Test
    void unknownTypeIsOfferedAsNewType() {
        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(MANUSCRIPT_FROM_FILE));

        assertEquals(List.of(MANUSCRIPT_FROM_FILE), List.copyOf(viewModel.newTypes()));
        assertEquals(List.of(), List.copyOf(viewModel.differentCustomizations()));
    }

    @Test
    void modifiedShippedTypeIsOfferedAsDifferentCustomization() {
        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE));

        assertEquals(List.of(), List.copyOf(viewModel.newTypes()));
        assertEquals(List.of(AUDIO_FROM_FILE), viewModel.differentCustomizations().stream()
                                                        .map(BibEntryTypePrefsAndFileViewModel::customTypeFromFile)
                                                        .toList());
    }

    @Test
    void typeEqualToTheStoredOneIsNotOffered() {
        entryTypesManager.addCustomOrModifiedType(AUDIO_FROM_FILE, MODE);

        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE));

        assertEquals(List.of(), List.copyOf(viewModel.newTypes()));
        assertEquals(List.of(), List.copyOf(viewModel.differentCustomizations()));
    }

    @Test
    void importingNewTypeStoresIt() {
        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(MANUSCRIPT_FROM_FILE));

        viewModel.importBibEntryTypes(List.of(MANUSCRIPT_FROM_FILE), List.of());

        assertEquals(Optional.of(MANUSCRIPT_FROM_FILE), entryTypesManager.enrich(new UnknownEntryType("manuscript"), MODE));
        verify(preferences).storeCustomEntryTypesRepository(entryTypesManager);
    }

    /// Regression test for <https://github.com/JabRef/jabref/issues/9930>: a checked different customization
    /// has to overwrite the currently stored type - it was silently dropped before.
    @Test
    void importingDifferentCustomizationStoresTheTypeFromTheFile() {
        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE));

        viewModel.importBibEntryTypes(List.of(), List.of(AUDIO_FROM_FILE));

        assertEquals(Optional.of(AUDIO_FROM_FILE), entryTypesManager.enrich(BiblatexNonStandardEntryType.Audio, MODE));
        verify(preferences).storeCustomEntryTypesRepository(entryTypesManager);
    }

    /// The behaviour reported at <https://github.com/JabRef/jabref/issues/9930>: after the import,
    /// opening the very same library must not offer anything again.
    @Test
    void importedTypesAreNotOfferedAgain() {
        List<BibEntryType> typesInFile = List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE);

        ImportCustomEntryTypesDialogViewModel firstStart = viewModelFor(typesInFile);
        firstStart.importBibEntryTypes(List.of(MANUSCRIPT_FROM_FILE), List.of(AUDIO_FROM_FILE));

        ImportCustomEntryTypesDialogViewModel secondStart = viewModelFor(typesInFile);

        assertEquals(List.of(), List.copyOf(secondStart.newTypes()));
        assertEquals(List.of(), List.copyOf(secondStart.differentCustomizations()));
        assertFalse(typesInFile.stream().anyMatch(type -> entryTypesManager.isDifferentCustomOrModifiedType(type, MODE)));
    }

    @Test
    void unselectedTypesAreStillOfferedAtTheNextStart() {
        List<BibEntryType> typesInFile = List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE);

        ImportCustomEntryTypesDialogViewModel firstStart = viewModelFor(typesInFile);
        firstStart.importBibEntryTypes(List.of(MANUSCRIPT_FROM_FILE), List.of());

        ImportCustomEntryTypesDialogViewModel secondStart = viewModelFor(typesInFile);

        assertEquals(List.of(), List.copyOf(secondStart.newTypes()));
        assertEquals(List.of(AUDIO_FROM_FILE), secondStart.differentCustomizations().stream()
                                                          .map(BibEntryTypePrefsAndFileViewModel::customTypeFromFile)
                                                          .toList());
    }

    @Test
    void nothingIsStoredWhenNothingIsChecked() {
        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE));

        viewModel.importBibEntryTypes(List.of(), List.of());

        assertTrue(entryTypesManager.getAllCustomizedTypes(MODE).isEmpty());
        verify(preferences, never()).storeCustomEntryTypesRepository(entryTypesManager);
    }
}
