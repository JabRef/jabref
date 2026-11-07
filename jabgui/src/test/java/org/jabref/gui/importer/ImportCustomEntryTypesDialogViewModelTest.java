package org.jabref.gui.importer;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.importer.util.MetaDataParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.BibEntryTypeBuilder;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.BiblatexNonStandardEntryType;
import org.jabref.model.entry.types.UnknownEntryType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/// Tests for the dialog offering to store the custom entry types of a just opened library.
///
/// The entry types are the ones of the library attached to <https://github.com/JabRef/jabref/issues/9930>:
/// `manuscript` is unknown to JabRef, whereas `audio` is a customization of a type JabRef ships.
///
/// [utest->req~import.entry-types.offered-once~1]
@NullMarked
class ImportCustomEntryTypesDialogViewModelTest {

    private static final BibDatabaseMode MODE = BibDatabaseMode.BIBLATEX;

    private static final BibEntryType MANUSCRIPT_FROM_FILE = parse(
            "jabref-entrytype: manuscript: req[library;location;shelfmark] opt[origin;scribe]");
    private static final BibEntryType AUDIO_FROM_FILE = parse(
            "jabref-entrytype: audio: req[author;date;publisher;title] opt[url;urldate]");

    private BibEntryTypesManager entryTypesManager;
    private CliPreferences preferences;
    private Set<String> declinedDecisions;

    private static BibEntryType parse(String comment) {
        return MetaDataParser.parseCustomEntryType(comment).orElseThrow();
    }

    @BeforeEach
    void setUp() {
        entryTypesManager = new BibEntryTypesManager();
        preferences = mock(CliPreferences.class);
        declinedDecisions = new HashSet<>();
        when(preferences.getDeclinedCustomEntryTypes()).thenAnswer(_ -> Set.copyOf(declinedDecisions));
        doAnswer(invocation -> declinedDecisions.addAll(invocation.getArgument(0)))
                .when(preferences).addDeclinedCustomEntryTypes(any());
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
    void uncheckedTypesAreNotOfferedAgainAfterOk() {
        List<BibEntryType> typesInFile = List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE);

        ImportCustomEntryTypesDialogViewModel firstStart = viewModelFor(typesInFile);
        firstStart.importBibEntryTypes(List.of(MANUSCRIPT_FROM_FILE), List.of());

        ImportCustomEntryTypesDialogViewModel secondStart = viewModelFor(typesInFile);

        assertEquals(List.of(), List.copyOf(secondStart.newTypes()));
        assertEquals(List.of(), List.copyOf(secondStart.differentCustomizations()));
        assertEquals(Optional.of(MANUSCRIPT_FROM_FILE), entryTypesManager.enrich(new UnknownEntryType("manuscript"), MODE));
        assertEquals(List.of(MANUSCRIPT_FROM_FILE), List.copyOf(entryTypesManager.getAllCustomizedTypes(MODE)));
    }

    @Test
    void okWithoutSelectionDeclinesEverythingWithoutStoringTypes() {
        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE));

        viewModel.importBibEntryTypes(List.of(), List.of());

        assertEquals(List.of(), List.copyOf(entryTypesManager.getAllCustomizedTypes(MODE)));
        verify(preferences, never()).storeCustomEntryTypesRepository(entryTypesManager);
        assertEquals(List.of(), List.copyOf(viewModelFor(List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE)).newTypes()));
        assertEquals(List.of(), List.copyOf(viewModelFor(List.of(AUDIO_FROM_FILE, MANUSCRIPT_FROM_FILE)).differentCustomizations()));
    }

    @Test
    void declinedTypeIsOfferedAgainWhenTheLibraryChangesItsDefinition() {
        viewModelFor(List.of(MANUSCRIPT_FROM_FILE)).importBibEntryTypes(List.of(), List.of());
        BibEntryType changedManuscript = parse("jabref-entrytype: manuscript: req[library;shelfmark] opt[origin]");

        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(changedManuscript));

        assertEquals(List.of(changedManuscript), List.copyOf(viewModel.newTypes()));
    }

    @Test
    void declinedTypeIsOfferedAgainWhenTheStoredDefinitionChanges() {
        viewModelFor(List.of(AUDIO_FROM_FILE)).importBibEntryTypes(List.of(), List.of());
        entryTypesManager.addCustomOrModifiedType(parse("jabref-entrytype: audio: req[title] opt[url]"), MODE);

        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE));

        assertEquals(List.of(AUDIO_FROM_FILE), viewModel.differentCustomizations().stream()
                                                        .map(BibEntryTypePrefsAndFileViewModel::customTypeFromFile)
                                                        .toList());
    }

    @Test
    void declinedTypeIsOfferedAgainWhenOnlyStoredFieldPrioritiesChange() {
        entryTypesManager.addCustomOrModifiedType(new BibEntryTypeBuilder()
                .withType(BiblatexNonStandardEntryType.Audio)
                .withRequiredFields(StandardField.AUTHOR)
                .withImportantFields(StandardField.URL)
                .withDetailFields(StandardField.URLDATE)
                .build(), MODE);
        viewModelFor(List.of(AUDIO_FROM_FILE)).importBibEntryTypes(List.of(), List.of());
        entryTypesManager.addCustomOrModifiedType(new BibEntryTypeBuilder()
                .withType(BiblatexNonStandardEntryType.Audio)
                .withRequiredFields(StandardField.AUTHOR)
                .withImportantFields(StandardField.URL, StandardField.URLDATE)
                .build(), MODE);

        ImportCustomEntryTypesDialogViewModel viewModel = viewModelFor(List.of(AUDIO_FROM_FILE));

        assertEquals(List.of(AUDIO_FROM_FILE), viewModel.differentCustomizations().stream()
                                                        .map(BibEntryTypePrefsAndFileViewModel::customTypeFromFile)
                                                        .toList());
    }
}
