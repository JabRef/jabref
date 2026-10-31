package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.conferences.ConferenceAbbreviationLoader;
import org.jabref.logic.conferences.ConferenceAbbreviationPreferences;
import org.jabref.logic.conferences.ConferenceAbbreviationRepository;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationLoader;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.CurrentThreadTaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ResourceLock("Localization.lang")
class ConferenceAbbreviationsTabViewModelTest {
    private static final String FULL_NAME = "International Conference on Business Process Management";

    @TempDir
    Path directory;

    private final DialogService dialogService = mock(DialogService.class);
    private final ConferenceAbbreviationPreferences preferences = ConferenceAbbreviationPreferences.getDefault();
    private JournalAbbreviationsTabViewModel viewModel;

    @BeforeEach
    void setUp() {
        viewModel = new JournalAbbreviationsTabViewModel(preferences, dialogService, new CurrentThreadTaskExecutor(), new ConferenceAbbreviationRepository());
    }

    // [utest->req~abbreviations.conference-preferences~1]
    @Test
    void importsConferenceListAndPersistsOnlyOnSave() throws IOException {
        Path csv = createExample();
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(csv));
        viewModel.setValues();
        viewModel.openFile();

        assertEquals(List.of(), preferences.getExternalLists());
        assertEquals(List.of(new Abbreviation(FULL_NAME, "BPM")), viewModel.abbreviationsProperty().stream().map(AbbreviationViewModel::getAbbreviationObject).toList());
        assertEquals(1, viewModel.journalFilesProperty().size());
        viewModel.storeSettings();
        assertEquals(List.of(csv.toAbsolutePath().toString()), preferences.getExternalLists());
        assertEquals(Optional.of("BPM"), ConferenceAbbreviationLoader.loadRepository(preferences).getDefaultAbbreviation(FULL_NAME));
    }

    @Test
    void editsExistingConferenceCsvAndReloadsSavedRecords() throws IOException {
        Path csv = createExample();
        preferences.setExternalLists(List.of(csv.toString()));
        viewModel.setValues();
        AbbreviationViewModel row = viewModel.abbreviationsProperty().getFirst();
        viewModel.editAbbreviation(row, new Abbreviation(FULL_NAME, "BPM edited", "BPM"));
        viewModel.storeSettings();

        assertEquals(List.of(new Abbreviation(FULL_NAME, "BPM edited", "BPM")), List.copyOf(AbbreviationLoader.readAbbreviationsFromCsvFile(csv)));
        assertEquals(Optional.of("BPM edited"), ConferenceAbbreviationLoader.loadRepository(preferences).getDefaultAbbreviation(FULL_NAME));
    }

    @Test
    void resettingUnsavedChangesPreservesCsvAndPreferences() throws IOException {
        Path csv = createExample();
        preferences.setExternalLists(List.of(csv.toString()));
        viewModel.setValues();
        viewModel.editAbbreviation(viewModel.abbreviationsProperty().getFirst(), new Abbreviation(FULL_NAME, "Unsaved"));
        viewModel.removeCurrentFile();
        viewModel.setValues();

        assertEquals(List.of(csv.toString()), preferences.getExternalLists());
        assertEquals(List.of(new Abbreviation(FULL_NAME, "BPM")), List.copyOf(AbbreviationLoader.readAbbreviationsFromCsvFile(csv)));
        assertEquals("BPM", viewModel.abbreviationsProperty().getFirst().getAbbreviation());
    }

    @Test
    void cancelingImportedListRestoresEmptyPage() throws IOException {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(createExample()));
        viewModel.openFile();
        viewModel.setValues();

        assertEquals(List.of(), preferences.getExternalLists());
        assertEquals(List.of(), List.copyOf(viewModel.journalFilesProperty()));
        assertEquals(List.of(), List.copyOf(viewModel.abbreviationsProperty()));
        assertEquals(Optional.empty(), Optional.ofNullable(viewModel.currentFileProperty().get()));
        assertFalse(viewModel.isEditableAndRemovableProperty().get());
    }

    @Test
    void emptyConferenceListCanBeCreatedAndPopulated() throws IOException {
        Path csv = directory.resolve("new.csv");
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(csv));
        viewModel.setValues();
        viewModel.addNewFile();

        assertTrue(viewModel.isEditableAndRemovableProperty().get());
        viewModel.addAbbreviation(new Abbreviation(FULL_NAME, "BPM"));
        viewModel.storeSettings();
        assertEquals(List.of(new Abbreviation(FULL_NAME, "BPM")), List.copyOf(AbbreviationLoader.readAbbreviationsFromCsvFile(csv)));
    }

    @Test
    void removingListUpdatesSelectionWithoutDeletingFile() throws IOException {
        Path first = createExample();
        Path second = directory.resolve("second.csv");
        Files.copy(first, second);
        preferences.setExternalLists(List.of(first.toString(), second.toString()));
        viewModel.setValues();
        viewModel.removeCurrentFile();
        viewModel.storeSettings();

        assertEquals(first, viewModel.currentFileProperty().get().getAbsolutePath().orElseThrow());
        assertEquals(List.of(first.toString()), preferences.getExternalLists());
        assertEquals(List.of(new Abbreviation(FULL_NAME, "BPM")), List.copyOf(AbbreviationLoader.readAbbreviationsFromCsvFile(second)));
    }

    @Test
    void duplicateFileUsesConferenceMessage() throws IOException {
        Path csv = createExample();
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(csv));
        viewModel.openFile();
        viewModel.openFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        verify(dialogService).showErrorDialogAndWait(Localization.lang("Duplicate conference file"), Localization.lang("Conference file %0 already added", csv.toString()));
    }

    private Path createExample() throws IOException {
        Path csv = directory.resolve("conferences.csv");
        Files.writeString(csv, "\"" + FULL_NAME + "\",\"BPM\"\n");
        return csv;
    }
}
