package org.jabref.gui.preferences.journals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.logic.journals.Abbreviation;
import org.jabref.logic.journals.AbbreviationPreferences;
import org.jabref.logic.journals.JournalAbbreviationLoader;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.TaskExecutor;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JournalAbbreviationsViewModelTabTest {

    private final static TestAbbreviation ABBREVIATION_0 = new TestAbbreviation("Full0", "Abb0");
    private final static TestAbbreviation ABBREVIATION_0_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full0", "Abb0", "A0");

    private final static TestAbbreviation ABBREVIATION_1 = new TestAbbreviation("Full1", "Abb1");
    private final static TestAbbreviation ABBREVIATION_1_SHOW = new TestAbbreviation("Full1", "Abb1", true);
    private final static TestAbbreviation ABBREVIATION_1_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full1", "Abb1", "A1");

    private final static TestAbbreviation ABBREVIATION_2 = new TestAbbreviation("Full2", "Abb2");
    private final static TestAbbreviation ABBREVIATION_2_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full2", "Abb2", "A2");

    private final static TestAbbreviation ABBREVIATION_3 = new TestAbbreviation("Full3", "Abb3");
    private final static TestAbbreviation ABBREVIATION_3_OTHER_SHORT_UNIQUE = new TestAbbreviation("Full3", "Abb3", "A3");

    private final static TestAbbreviation ABBREVIATION_4 = new TestAbbreviation("Full4", "Abb4");

    private final static TestAbbreviation ABBREVIATION_5 = new TestAbbreviation("Full5", "Abb5");

    private final static TestAbbreviation ABBREVIATION_6 = new TestAbbreviation("Full6", "Abb6");

    private JournalAbbreviationsTabViewModel viewModel;
    private Path emptyTestFile;
    private Path tempFolder;
    private final JournalAbbreviationRepository repository = JournalAbbreviationLoader.loadBuiltInRepository();
    private DialogService dialogService;

    /// Keeps queued repository tasks outside Mockito's invocation history so completed fixtures can be collected.
    @NullMarked
    private static class QueuedTaskExecutor extends CurrentThreadTaskExecutor {
        private final List<Runnable> queued;

        private QueuedTaskExecutor(List<Runnable> queued) {
            this.queued = queued;
        }

        @Override
        public <V> Future<V> execute(BackgroundTask<V> task) {
            queued.add(() -> super.execute(task));
            return CompletableFuture.completedFuture(null);
        }
    }

    static class TestAbbreviation extends Abbreviation {

        private final boolean showShortestUniqueAbbreviation;

        public TestAbbreviation(String name, String abbreviation) {
            this(name, abbreviation, false);
        }

        public TestAbbreviation(String name, String abbreviation, boolean showShortestUniqueAbbreviation) {
            super(name, abbreviation);
            this.showShortestUniqueAbbreviation = showShortestUniqueAbbreviation;
        }

        public TestAbbreviation(String name, String abbreviation, String shortestUniqueAbbreviation) {
            super(name, abbreviation, shortestUniqueAbbreviation);
            this.showShortestUniqueAbbreviation = true;
        }

        @Override
        public String toString() {
            if (showShortestUniqueAbbreviation) {
                return this.getName() + "," + this.getAbbreviation() + "," + this.getShortestUniqueAbbreviation();
            }
            return this.getName() + "," + this.getAbbreviation();
        }
    }

    private static String csvListOfAbbreviations(List<TestAbbreviation> testAbbreviations) {
        return testAbbreviations.stream()
                                .map(TestAbbreviation::toString)
                                .collect(Collectors.joining("\n"));
    }

    private static String csvListOfAbbreviations(TestAbbreviation... testAbbreviations) {
        return csvListOfAbbreviations(Arrays.stream(testAbbreviations).toList());
    }

    private record CsvFileNameAndContent(String fileName, String content) {
        CsvFileNameAndContent(String fileName, TestAbbreviation... testAbbreviations) {
            this(fileName, csvListOfAbbreviations(testAbbreviations));
        }
    }

    private record TestData(
            List<CsvFileNameAndContent> csvFiles,
            TestAbbreviation abbreviationToCheck,
            List<String> finalContentsOfFile2,
            List<String> finalContentsOfFile3
    ) {
        /// Note that we have a **different** ordering at the constructor, because Java generics have "type erasure"
        public TestData(
                List<CsvFileNameAndContent> csvFiles,
                List<TestAbbreviation> finalContentsOfFile2,
                List<TestAbbreviation> finalContentsOfFile3,
                TestAbbreviation abbreviationToCheck
        ) {
            this(csvFiles,
                    abbreviationToCheck,
                    finalContentsOfFile2.stream().map(TestAbbreviation::toString).toList(),
                    finalContentsOfFile3.stream().map(TestAbbreviation::toString).toList());
        }
    }

    public static Stream<TestData> provideTestFiles() {
        // filenameing: testfileXY, where X is the number of test (count starts at 1), and Y is the number of the file in the test (count starts at 0)
        // testfile_3 has 5 entries after de-duplication
        return Stream.of(
                // No shortest unique abbreviations in files
                // Shortest unique abbreviations in entries (being the same then the abbreviation)
                new TestData(
                        List.of(
                                new CsvFileNameAndContent("testFile10.csv", ABBREVIATION_1),
                                new CsvFileNameAndContent("testFile11.csv", ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2),
                                new CsvFileNameAndContent("testFile12.csv", ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_3),
                                new CsvFileNameAndContent("testFile13.csv", ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_3, ABBREVIATION_4)),
                        List.of(ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_5),
                        List.of(ABBREVIATION_0, ABBREVIATION_1, ABBREVIATION_2, ABBREVIATION_3, ABBREVIATION_6),
                        ABBREVIATION_1_SHOW),

                // Shortest unique abbreviations
                new TestData(
                        List.of(
                                new CsvFileNameAndContent("testFile20.csv", ABBREVIATION_1_OTHER_SHORT_UNIQUE),
                                new CsvFileNameAndContent("testFile21.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE),
                                new CsvFileNameAndContent("testFile22.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3_OTHER_SHORT_UNIQUE),
                                // contains duplicate entry ABBREVIATION_1_OTHER_SHORT_UNIQUE, therefore 6 entries
                                new CsvFileNameAndContent("testFile23.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3, ABBREVIATION_4)),
                        List.of(ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_5),
                        // without duplicates
                        List.of(ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3, ABBREVIATION_6),
                        ABBREVIATION_1_OTHER_SHORT_UNIQUE),

                // Mixed abbreviations (some have shortest unique, some have not)
                new TestData(
                        List.of(
                                new CsvFileNameAndContent("testFile30.csv", ABBREVIATION_1),
                                new CsvFileNameAndContent("testFile31.csv", ABBREVIATION_0_OTHER_SHORT_UNIQUE, ABBREVIATION_1, ABBREVIATION_2_OTHER_SHORT_UNIQUE),
                                new CsvFileNameAndContent("testFile32.csv", ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_3),
                                // contains ABBREVIATION_1 in two variants, therefore 5 in total
                                new CsvFileNameAndContent("testFile33.csv", ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_1, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_4)),
                        // Entries of testFile2 plus ABBREVIATION_5_SHOW minus ABBREVIATION_3
                        List.of(ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_5),
                        // Entries of testFile3 plus ABBREVIATION_6_SHOW minus ABBREVIATION_4
                        List.of(ABBREVIATION_0, ABBREVIATION_1_OTHER_SHORT_UNIQUE, ABBREVIATION_1, ABBREVIATION_2_OTHER_SHORT_UNIQUE, ABBREVIATION_6),
                        ABBREVIATION_1_OTHER_SHORT_UNIQUE)
        );
    }

    @BeforeEach
    void setUpViewModel(@TempDir Path tempFolder) throws IOException {
        AbbreviationPreferences abbreviationPreferences = org.mockito.Mockito.spy(AbbreviationPreferences.getDefault());

        dialogService = mock(DialogService.class);
        this.tempFolder = tempFolder;

        TaskExecutor taskExecutor = new CurrentThreadTaskExecutor();
        viewModel = new JournalAbbreviationsTabViewModel(abbreviationPreferences, dialogService, taskExecutor, repository);

        emptyTestFile = createTestFile(new CsvFileNameAndContent("emptyTestFile.csv", ""));
    }

    @Test
    void builtInChoiceIsLocalUntilSavedAndRefreshesRetainedRepository() {
        AbbreviationPreferences preferences = AbbreviationPreferences.getDefault();
        JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepository(preferences);
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(
                preferences, dialogService, new CurrentThreadTaskExecutor(), retained);
        model.setValues();
        model.journalFilesProperty().getLast().enabledProperty().set(false);
        assertTrue(preferences.shouldUseBuiltInJournalList());
        assertEquals(Optional.of("2D Mater."), retained.getDefaultAbbreviation("2D Materials"));

        model.setValues();
        assertTrue(model.useBuiltInJournalListProperty().get());
        model.journalFilesProperty().getLast().enabledProperty().set(false);
        model.storeSettings();
        assertFalse(preferences.shouldUseBuiltInJournalList());
        assertEquals(Optional.empty(), retained.getDefaultAbbreviation("2D Materials"));
        model.setValues();
        assertFalse(model.useBuiltInJournalListProperty().get());
        assertEquals(List.of("2D Materials"), model.abbreviationsProperty().stream()
                                                   .map(AbbreviationViewModel::getName)
                                                   .filter("2D Materials"::equals)
                                                   .toList());
    }

    @Test
    void externalListChoiceIsLocalUntilSavedAndRemainsEditableWhenDisabled() throws IOException {
        // [utest->req~journals.disable-external~1]
        Files.writeString(emptyTestFile, "External Journal;Ext. J.;Ext. J.");
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(emptyTestFile.toString()), true, false);
        JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepository(preferences);
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(
                preferences, dialogService, new CurrentThreadTaskExecutor(), retained);
        model.setValues();
        model.journalFilesProperty().getFirst().enabledProperty().set(false);
        assertEquals(List.of(), preferences.getDisabledExternalJournalLists());
        assertEquals(Optional.of("Ext. J."), retained.getDefaultAbbreviation("External Journal"));

        model.setValues();
        assertTrue(model.journalFilesProperty().getFirst().enabledProperty().get());
        model.journalFilesProperty().getFirst().enabledProperty().set(false);
        model.storeSettings();
        assertEquals(List.of(emptyTestFile.toString()), preferences.getDisabledExternalJournalLists());
        assertEquals(List.of(emptyTestFile.toString()), preferences.getExternalJournalLists());
        assertEquals(List.of(), retained.getFullNames().stream().filter("External Journal"::equals).toList());

        model.setValues();
        AbbreviationsFileViewModel disabled = model.journalFilesProperty().getFirst();
        assertFalse(disabled.enabledProperty().get());
        model.currentFileProperty().set(disabled);
        assertTrue(model.isEditableAndRemovableProperty().get());
        model.addAbbreviation(ABBREVIATION_0);
        model.storeSettings();
        assertEquals(List.of("External Journal", "Full0"), JournalAbbreviationLoader.readAbbreviationsFromCsvFile(emptyTestFile).stream()
                                                                                    .map(Abbreviation::getName).toList());
        assertEquals(List.of(), retained.getFullNames().stream().filter("Full0"::equals).toList());

        disabled.enabledProperty().set(true);
        model.storeSettings();
        assertEquals(Optional.of("Ext. J."), retained.getDefaultAbbreviation("External Journal"));
        assertEquals(Optional.of("Abb0"), retained.getDefaultAbbreviation("Full0"));
    }

    @Test
    void removingDisabledListClearsItsSavedStateWithoutDeletingFile() {
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(emptyTestFile.toString()), true, false);
        preferences.setDisabledExternalJournalLists(List.of(emptyTestFile.toString()));
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(
                preferences, dialogService, new CurrentThreadTaskExecutor(), repository);
        model.setValues();
        model.currentFileProperty().set(model.journalFilesProperty().getFirst());
        model.removeCurrentFile();
        model.storeSettings();

        assertEquals(List.of(), preferences.getExternalJournalLists());
        assertEquals(List.of(), preferences.getDisabledExternalJournalLists());
        assertTrue(Files.exists(emptyTestFile));
        assertTrue(model.currentFileProperty().get().isBuiltInListProperty().get());
        assertFalse(model.isFileRemovableProperty().get());

        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(emptyTestFile));
        model.openFile();
        assertTrue(model.currentFileProperty().get().enabledProperty().get());
    }

    @Test
    void overlappingSavesKeepLatestExternalEditsAndToggle() throws IOException {
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(emptyTestFile.toString()), true, false);
        List<Runnable> queued = new ArrayList<>();
        TaskExecutor executor = new QueuedTaskExecutor(queued);
        JournalAbbreviationRepository retained = JournalAbbreviationLoader.loadRepository(preferences);
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(preferences, dialogService, executor, retained);
        model.setValues();
        queued.getFirst().run();
        model.currentFileProperty().set(model.journalFilesProperty().getFirst());
        model.addAbbreviation(ABBREVIATION_0);
        model.useBuiltInJournalListProperty().set(false);
        model.storeSettings();
        model.addAbbreviation(ABBREVIATION_1);
        model.useBuiltInJournalListProperty().set(true);
        model.storeSettings();

        queued.get(2).run();
        queued.get(1).run();
        queued.getLast().run();
        assertTrue(preferences.shouldUseBuiltInJournalList());
        assertEquals(Optional.of("2D Mater."), retained.getDefaultAbbreviation("2D Materials"));
        assertEquals(Optional.of("Abb0"), retained.getDefaultAbbreviation("Full0"));
        assertEquals(Optional.of("Abb1"), retained.getDefaultAbbreviation("Full1"));
        assertEquals(2, JournalAbbreviationLoader.readAbbreviationsFromCsvFile(emptyTestFile).size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void newerSaveFromReopenedPreferencesIsNotOverwrittenByOlderSave(boolean editReopenedList) throws IOException {
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(emptyTestFile.toString()), true, false);
        List<Runnable> queued = new ArrayList<>();
        TaskExecutor executor = new QueuedTaskExecutor(queued);
        JournalAbbreviationsTabViewModel first = new JournalAbbreviationsTabViewModel(preferences, dialogService, executor, repository);
        first.setValues();
        queued.removeFirst().run();
        first.currentFileProperty().set(first.journalFilesProperty().getFirst());
        first.addAbbreviation(ABBREVIATION_0);
        first.storeSettings();
        Runnable olderSave = queued.removeFirst();

        JournalAbbreviationsTabViewModel reopened = new JournalAbbreviationsTabViewModel(preferences, dialogService, executor, repository);
        reopened.setValues();
        queued.removeFirst().run();
        reopened.currentFileProperty().set(reopened.journalFilesProperty().getFirst());
        if (editReopenedList) {
            reopened.addAbbreviation(ABBREVIATION_1);
        }
        reopened.storeSettings();
        queued.removeFirst().run();
        olderSave.run();

        assertEquals(editReopenedList ? List.of("Full0", "Full1") : List.of("Full0"), JournalAbbreviationLoader.readAbbreviationsFromCsvFile(emptyTestFile).stream()
                                                                                                               .map(Abbreviation::getName)
                                                                                                               .toList());
    }

    @Test
    void editAfterSaveSnapshotIsWrittenByNextSave() throws IOException {
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(emptyTestFile.toString()), true, false);
        List<Runnable> queued = new ArrayList<>();
        TaskExecutor executor = new QueuedTaskExecutor(queued);
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(preferences, dialogService, executor, repository);
        model.setValues();
        queued.removeFirst().run();
        model.currentFileProperty().set(model.journalFilesProperty().getFirst());
        model.addAbbreviation(ABBREVIATION_0);
        model.storeSettings();
        model.addAbbreviation(ABBREVIATION_1);
        queued.removeFirst().run();
        model.storeSettings();
        queued.removeLast().run();
        assertEquals(List.of("Full0", "Full1"), JournalAbbreviationLoader.readAbbreviationsFromCsvFile(emptyTestFile).stream()
                                                                         .map(Abbreviation::getName)
                                                                         .toList());
    }

    @Test
    void failedWritesShowPathsAndRemainRetryable() throws IOException {
        // [utest->req~journals.report-write-failures~1]
        Path failedLast = tempFolder.resolve("missing/z.csv");
        Path failedFirst = tempFolder.resolve("missing/a.csv");
        AbbreviationPreferences preferences = new AbbreviationPreferences(
                List.of(failedLast.toString(), failedFirst.toString(), emptyTestFile.toString()), true, false);
        preferences.setUseBuiltInJournalList(false);
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(
                preferences, dialogService, new CurrentThreadTaskExecutor(), repository);
        model.createFileObjects();
        model.currentFileProperty().set(model.journalFilesProperty().getFirst());
        model.addAbbreviation(ABBREVIATION_0);

        model.storeSettings();

        verify(dialogService).showErrorDialogAndWait(Localization.lang("Journal abbreviations"),
                Localization.lang("Could not save the following journal abbreviation lists:\n%0\n\nUnsaved changes will be lost when JabRef closes.",
                        failedFirst + "\n" + failedLast));
        assertEquals(Optional.of(List.of(new Abbreviation("Full0", "Abb0"))), repository.getPendingJournalList(failedLast));
        Files.createDirectory(failedLast.getParent());
        model.storeSettings();

        assertEquals(List.of("Full0"), JournalAbbreviationLoader.readAbbreviationsFromCsvFile(failedLast).stream().map(Abbreviation::getName).toList());
        assertEquals(Optional.empty(), repository.getPendingJournalList(failedLast));
        verify(dialogService, times(1)).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void initialHasNoFilesAndNoAbbreviations() {
        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void initialWithSavedFilesIncrementsFilesCounter(TestData testData) throws IOException {
        addFourTestFileToViewModelAndPreferences(testData);
        assertEquals(4, viewModel.journalFilesProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void removeDuplicatesWhenReadingFiles(TestData testData) throws IOException {
        addFourTestFileToViewModelAndPreferences(testData);
        viewModel.selectLastJournalFile();

        assertEquals(4, viewModel.journalFilesProperty().size());
        assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @Test
    void addFileIncreasesCounterOfOpenFilesAndHasNoAbbreviations() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void addDuplicatedFileResultsInErrorDialog(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.getFirst())));
        viewModel.addNewFile();
        viewModel.addNewFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void openDuplicatedFileResultsInAnException(TestData testData) throws IOException {
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.getFirst())));
        viewModel.openFile();
        viewModel.openFile();
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void openingEquivalentPathKeepsOneList(boolean useRelativePath) {
        Path equivalentPath = useRelativePath
                              ? Path.of("").toAbsolutePath().relativize(emptyTestFile)
                              : emptyTestFile.resolveSibling("unused").resolve("..").resolve(emptyTestFile.getFileName());
        when(dialogService.showFileOpenDialog(any())).thenReturn(Optional.of(emptyTestFile), Optional.of(equivalentPath));

        viewModel.openFile();
        viewModel.openFile();

        assertEquals(List.of(Optional.of(emptyTestFile)), viewModel.journalFilesProperty().stream()
                                                                   .map(AbbreviationsFileViewModel::getAbsolutePath).toList());
        verify(dialogService, times(1)).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void duplicateSavedPathsOpenOnceAndRetainDisabledState() {
        Path equivalentPath = emptyTestFile.resolveSibling(".").resolve(emptyTestFile.getFileName());
        AbbreviationPreferences preferences = new AbbreviationPreferences(
                List.of(emptyTestFile.toString(), emptyTestFile.toString(), equivalentPath.toString()), true, false);
        preferences.setDisabledExternalJournalLists(List.of(equivalentPath.toString()));
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(
                preferences, dialogService, new CurrentThreadTaskExecutor(), repository);

        model.createFileObjects();

        assertEquals(1, model.journalFilesProperty().size());
        assertFalse(model.journalFilesProperty().getFirst().enabledProperty().get());
        verifyNoInteractions(dialogService);
    }

    @Test
    void savingDuplicateRowsUsesFirstListContentsAndEnabledState() throws IOException {
        AbbreviationPreferences preferences = new AbbreviationPreferences(List.of(emptyTestFile.toString()), true, false);
        preferences.setUseBuiltInJournalList(false);
        JournalAbbreviationsTabViewModel model = new JournalAbbreviationsTabViewModel(
                preferences, dialogService, new CurrentThreadTaskExecutor(), repository);
        model.createFileObjects();
        model.addAbbreviation(ABBREVIATION_0);
        model.journalFilesProperty().getFirst().enabledProperty().set(false);
        Path equivalentPath = emptyTestFile.resolveSibling(".").resolve(emptyTestFile.getFileName());
        model.journalFilesProperty().add(new AbbreviationsFileViewModel(equivalentPath));

        model.storeSettings();

        assertEquals(List.of(emptyTestFile.toString()), preferences.getExternalJournalLists());
        assertEquals(List.of(emptyTestFile.toString()), preferences.getDisabledExternalJournalLists());
        assertEquals(List.of("Full0"), JournalAbbreviationLoader.readAbbreviationsFromCsvFile(emptyTestFile).stream()
                                                                .map(Abbreviation::getName).toList());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void selectLastJournalFileSwitchesFilesAndTheirAbbreviations(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(0, viewModel.abbreviationsCountProperty().get());

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.getFirst())));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        assertEquals(1, viewModel.abbreviationsCountProperty().get());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void openValidFileContainsTheSpecificEntryAndEnoughAbbreviations(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();

        assertEquals(1, viewModel.journalFilesProperty().size());
        assertEquals(4, viewModel.abbreviationsProperty().size());

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testData.abbreviationToCheck)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void removeLastListSetsCurrentFileAndCurrentAbbreviationToNull(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.getFirst())));
        viewModel.addNewFile();
        viewModel.removeCurrentFile();

        assertEquals(0, viewModel.journalFilesProperty().size());
        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertNull(viewModel.currentFileProperty().get());
        assertNull(viewModel.currentAbbreviationProperty().get());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void mixedFileUsage(TestData testData) throws IOException {
        // simulate open file button twice
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(1));

        // size of the list of journal files should be incremented by two
        assertEquals(2, viewModel.journalFilesProperty().size());

        // our third test file has 4 abbreviations
        assertEquals(4, viewModel.abbreviationsProperty().size());

        // check "arbitrary" abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testData.abbreviationToCheck)));

        // simulate add new file button
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(2));

        // size of the list of journal files should be incremented by one
        assertEquals(3, viewModel.journalFilesProperty().size());

        // a new file has zero abbreviations
        assertEquals(0, viewModel.abbreviationsProperty().size());

        // simulate opening of testFile_3
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(3))));
        viewModel.addNewFile();
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().get(3));

        // size of the list of journal files should have been incremented by one
        assertEquals(4, viewModel.journalFilesProperty().size());

        // after de-duplication
        assertEquals(5, viewModel.abbreviationsProperty().size());

        // check "arbitrary" abbreviation
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testData.abbreviationToCheck)));
    }

    @Test
    void builtInListsIncludeAllBuiltInAbbreviations() {
        viewModel.addBuiltInList();
        assertEquals(1, viewModel.journalFilesProperty().getSize());
        viewModel.currentFileProperty().set(viewModel.journalFilesProperty().getFirst());
        ObservableList<Abbreviation> expected = FXCollections.observableArrayList(repository.getAllLoaded());
        ObservableList<Abbreviation> actualAbbreviations = FXCollections
                .observableArrayList(viewModel.abbreviationsProperty().stream()
                                              .map(AbbreviationViewModel::getAbbreviationObject)
                                              .collect(Collectors.toList()));

        assertEquals(expected, actualAbbreviations);
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void currentFilePropertyChangeActiveFile(TestData testData) throws IOException {
        for (CsvFileNameAndContent testFile : testData.csvFiles) {
            when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testFile)));
            viewModel.addNewFile();
        }
        viewModel.selectLastJournalFile();

        AbbreviationsFileViewModel test1 = viewModel.journalFilesProperty().getFirst();
        AbbreviationsFileViewModel test3 = viewModel.journalFilesProperty().get(1);
        AbbreviationsFileViewModel test4 = viewModel.journalFilesProperty().get(2);
        AbbreviationsFileViewModel test5 = viewModel.journalFilesProperty().get(3);

        // test if the last opened file is active, but duplicated entry has been removed
        assertEquals(5, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test1);

        // test if the current abbreviations matches with the ones in testFile1Entries
        assertEquals(1, viewModel.abbreviationsProperty().size());

        viewModel.currentFileProperty().set(test3);
        assertEquals(3, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test1);
        assertEquals(1, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test4);
        assertEquals(4, viewModel.abbreviationsProperty().size());
        viewModel.currentFileProperty().set(test5);
        assertEquals(5, viewModel.abbreviationsProperty().size());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void addAbbreviationIncludesAbbreviationsInAbbreviationList(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(3))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation(testAbbreviation);

        assertEquals(6, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void addDuplicatedAbbreviationResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        viewModel.addAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        viewModel.addAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @Test
    void editSameAbbreviationWithNoChangeDoesNotResultInException() {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation(testAbbreviation);
        viewModel.editAbbreviation(testAbbreviation);

        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void editAbbreviationIncludesNewAbbreviationInAbbreviationsList(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(2))));
        viewModel.addNewFile();
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(3))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        Abbreviation testAbbreviation = new Abbreviation("YetAnotherEntry", "YAE");
        viewModel.addAbbreviation(testAbbreviation);

        assertEquals(6, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));

        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(emptyTestFile));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        // addAbbreviation(testAbbreviation);

        assertEquals(0, viewModel.abbreviationsProperty().size());
        assertFalse(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(testAbbreviation)));
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void editAbbreviationToExistingOneResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        viewModel.currentAbbreviationProperty().set(viewModel.abbreviationsProperty().get(1));
        viewModel.editAbbreviation(new Abbreviation("YetAnotherEntry", "YAE"));
        verify(dialogService).showErrorDialogAndWait(anyString(), anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void editAbbreviationToEmptyNameResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation(new Abbreviation("", "YAE"));
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void editAbbreviationToEmptyAbbreviationResultsInException(TestData testData) throws IOException {
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(testData.csvFiles.get(1))));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();

        assertEquals(3, viewModel.abbreviationsProperty().size());

        viewModel.editAbbreviation(new Abbreviation("YetAnotherEntry", ""));
        verify(dialogService).showErrorDialogAndWait(anyString());
    }

    @ParameterizedTest
    @MethodSource("provideTestFiles")
    void saveAbbreviationsToFilesCreatesNewFilesWithWrittenAbbreviations(TestData testData) throws IOException {
        Path testFile2 = createTestFile(testData.csvFiles.get(2));
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile2));
        viewModel.addNewFile();
        viewModel.selectLastJournalFile();
        selectLastAbbreviation();
        viewModel.editAbbreviation(ABBREVIATION_5);
        assertEquals(4, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(ABBREVIATION_5)));

        Path testFile3 = createTestFile(testData.csvFiles.get(3));
        when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(testFile3));
        viewModel.addNewFile();
        assertEquals(5, viewModel.abbreviationsProperty().size());

        viewModel.selectLastJournalFile();
        assertTrue(viewModel.currentFileProperty().get().getAbsolutePath().get().getFileName().toString().endsWith("3.csv"));
        selectLastAbbreviation();
        viewModel.deleteAbbreviation();
        viewModel.addAbbreviation(ABBREVIATION_6);

        // deletion and addition of an entry leads to same size
        assertEquals(5, viewModel.abbreviationsProperty().size());
        assertTrue(viewModel.abbreviationsProperty().contains(new AbbreviationViewModel(ABBREVIATION_6)));

        // overwrite all files
        viewModel.saveJournalAbbreviationFiles();

        List<String> actual = Files.readAllLines(testFile2, StandardCharsets.UTF_8);
        assertEquals(testData.finalContentsOfFile2, actual);

        actual = Files.readAllLines(testFile3, StandardCharsets.UTF_8);
        assertEquals(testData.finalContentsOfFile3, actual);
    }

    /// Select the last abbreviation in the list of abbreviations
    private void selectLastAbbreviation() {
        viewModel.currentAbbreviationProperty()
                 .set(viewModel.abbreviationsProperty().get(viewModel.abbreviationsCountProperty().get() - 1));
    }

    private void addFourTestFileToViewModelAndPreferences(TestData testData) throws IOException {
        for (CsvFileNameAndContent csvFile : testData.csvFiles) {
            when(dialogService.showFileSaveDialog(any())).thenReturn(Optional.of(createTestFile(csvFile)));
            viewModel.addNewFile();
        }
        viewModel.storeSettings();
    }

    private Path createTestFile(CsvFileNameAndContent testFile) throws IOException {
        Path file = this.tempFolder.resolve(testFile.fileName);
        Files.writeString(file, testFile.content);
        return file;
    }
}
