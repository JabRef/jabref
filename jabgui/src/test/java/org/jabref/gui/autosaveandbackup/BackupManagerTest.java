package org.jabref.gui.autosaveandbackup;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jabref.gui.LibraryTab;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackupFileType;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.logic.util.Directories;
import org.jabref.logic.util.io.BackupFileUtil;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.event.GroupUpdatedEvent;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.metadata.event.MetaDataChangeSource;
import org.jabref.model.metadata.event.MetaDataChangedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupManagerTest {

    Path backupDir;

    @BeforeEach
    void setup(@TempDir Path tempDir) {
        backupDir = tempDir.resolve("backup");
    }

    @Test
    void backupFileNameIsCorrectlyGeneratedInAppDataDirectory() {
        Path bibPath = Path.of("tmp", "test.bib");
        backupDir = Directories.getBackupDirectory();
        Path backupPath = BackupManager.getBackupPathForNewBackup(bibPath, backupDir);

        // Pattern is "27182d3c--test.bib--", but the hashing is implemented differently on Linux than on Windows
        assertNotEquals("", backupPath);
    }

    @Test
    void backupFileIsEqualForNonExistingBackup() throws URISyntaxException {
        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-autosave.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile, backupDir));
    }

    @Test
    void backupFileIsEqual() throws URISyntaxException, IOException {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerTest.class.getResource("no-changes-backup.bib").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI()), BackupFileType.BACKUP, backupDir);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        assertFalse(BackupManager.backupFileDiffers(originalFile, backupDir));
    }

    @Test
    void backupFileDiffers() throws URISyntaxException, IOException {
        // Prepare test: Create backup file on "right" path
        Path source = Path.of(BackupManagerTest.class.getResource("changes-backup.bib").toURI());
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(Path.of(BackupManagerTest.class.getResource("changes.bib").toURI()), BackupFileType.BACKUP, backupDir);
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);

        Path originalFile = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        assertTrue(BackupManager.backupFileDiffers(originalFile, backupDir));
    }

    @Test
    void correctBackupFileDeterminedForMultipleBackupFiles() throws URISyntaxException, IOException {
        Path noChangesBib = Path.of(BackupManagerTest.class.getResource("no-changes.bib").toURI());
        Path noChangesBibBackup = Path.of(BackupManagerTest.class.getResource("no-changes-backup.bib").toURI());

        // Prepare test: Create backup files on "right" path
        // most recent file does not have any changes
        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(noChangesBib, BackupFileType.BACKUP, backupDir);
        Files.copy(noChangesBibBackup, target, StandardCopyOption.REPLACE_EXISTING);

        // create "older" backup files containing changes
        for (int i = 0; i < 10; i++) {
            Path changesBib = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
            String timeSuffix = "2020-02-03--00.00.0" + i;
            String fileName = BackupFileUtil.getUniqueFilePrefix(noChangesBib) + "--no-changes.bib--" + timeSuffix + ".bib";
            target = backupDir.resolve(fileName);
            Files.copy(changesBib, target, StandardCopyOption.REPLACE_EXISTING);
        }

        Path originalFile = noChangesBib;
        assertFalse(BackupManager.backupFileDiffers(originalFile, backupDir));
    }

    @Test
    void backupFileWithNewerTimeStampLeadsToDiff() throws URISyntaxException, IOException {
        Path changesBib = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        Path changesBibBackup = Path.of(BackupManagerTest.class.getResource("changes-backup.bib").toURI());

        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(changesBib, BackupFileType.BACKUP, backupDir);
        Files.copy(changesBibBackup, target, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(BackupManager.backupFileDiffers(changesBib, backupDir));
    }

    @Test
    void backupFileWithOlderTimeStampDoesNotLeadToDiff() throws URISyntaxException, IOException {
        Path changesBib = Path.of(BackupManagerTest.class.getResource("changes.bib").toURI());
        Path changesBibBackup = Path.of(BackupManagerTest.class.getResource("changes-backup.bib").toURI());

        Path target = BackupFileUtil.getPathForNewBackupFileAndCreateDirectory(changesBib, BackupFileType.BACKUP, backupDir);
        Files.copy(changesBibBackup, target, StandardCopyOption.REPLACE_EXISTING);

        // Make backup file very old
        Files.setLastModifiedTime(target, FileTime.fromMillis(0));

        assertFalse(BackupManager.backupFileDiffers(changesBib, backupDir));
    }

    @Test
    void latestBackupCanBeEmpty(@TempDir Path tempDir) throws IOException {
        Path originalFile = tempDir.resolve("library.bib");
        Files.writeString(originalFile, "@article{existing}");
        Path emptyBackup = BackupManager.getBackupPathForNewBackup(originalFile, backupDir);
        Files.writeString(emptyBackup, "");

        assertEquals(emptyBackup, BackupManager.getLatestBackupPath(originalFile, backupDir).orElseThrow());
    }

    @Test
        // [utest->req~jabgui.autosaveandbackup.complete-backup~1]
    void restoringAnEmptyBackupLeavesTheOriginalFileUnchanged(@TempDir Path tempDir) throws IOException {
        Path originalFile = tempDir.resolve("library.bib");
        Files.writeString(originalFile, "@article{existing}");
        Path emptyBackup = BackupManager.getBackupPathForNewBackup(originalFile, backupDir);
        Files.writeString(emptyBackup, "");

        assertEquals(new BackupManager.RestoreResult.Empty(emptyBackup), BackupManager.restoreBackup(originalFile, backupDir));

        assertEquals("@article{existing}", Files.readString(originalFile));
    }

    @Test
    void missingBackupIncludesTheOriginalFilePath(@TempDir Path tempDir) {
        Path originalFile = tempDir.resolve("library.bib");

        assertEquals(new BackupManager.RestoreResult.NotFound(originalFile), BackupManager.restoreBackup(originalFile, backupDir));
    }

    @Test
    void failedRestoreIncludesTheCause(@TempDir Path tempDir) throws IOException {
        Path originalFile = tempDir.resolve("library.bib");
        Files.createDirectory(originalFile);
        Files.writeString(originalFile.resolve("existing-file"), "existing content");
        Path backup = BackupManager.getBackupPathForNewBackup(originalFile, backupDir);
        Files.writeString(backup, "@article{backup}");

        BackupManager.RestoreResult result = BackupManager.restoreBackup(originalFile, backupDir);

        BackupManager.RestoreResult.Failed failure = assertInstanceOf(BackupManager.RestoreResult.Failed.class, result);
        assertEquals(backup, failure.backupPath());
        assertInstanceOf(DirectoryNotEmptyException.class, failure.exception());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "A"})
        // [utest->req~jabgui.autosaveandbackup.backup-every-edit~1]
    void typingIntoNewOrExistingFieldIsBackedUp(String initialValue, @TempDir Path tempDir) throws IOException, ParseException {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("entry").withField(StandardField.EDITOR, initialValue);
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        Path originalFile = tempDir.resolve("library.bib");
        context.setDatabasePath(originalFile);
        Files.writeString(originalFile, """
                @Article{entry,
                  editor = {%s},
                }
                """.formatted(initialValue));
        Files.setLastModifiedTime(originalFile, FileTime.fromMillis(0));

        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        CoarseChangeFilter filter = new CoarseChangeFilter(context);
        BackupManager manager = BackupManager.start(mock(LibraryTab.class), context, filter, new BibEntryTypesManager(), preferences);
        Path backupPath = manager.determineBackupPathForNewBackup(backupDir).orElseThrow();
        String editor = initialValue + "Editor";
        try {
            for (int length = initialValue.length() + 1; length <= editor.length(); length++) {
                String value = editor.substring(0, length);
                entry.setField(StandardField.EDITOR, value);
                manager.performBackup(backupPath);

                assertEquals(Optional.of(value), BibtexParser.singleFromString(Files.readString(backupPath), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS))
                                                             .orElseThrow().getField(StandardField.EDITOR));
            }
            assertTrue(BackupManager.backupFileDiffers(originalFile, backupDir));
            assertEquals(new BackupManager.RestoreResult.Restored(), BackupManager.restoreBackup(originalFile, backupDir));
            assertEquals(Files.readString(backupPath), Files.readString(originalFile));
        } finally {
            BackupManager.shutdown(context, backupDir, false);
            filter.shutdown();
        }
    }

    @Test
        // [utest->req~jabgui.autosaveandbackup.preserve-pending-edits~1]
    void editAfterSnapshotIsIncludedInNextBackup(@TempDir Path tempDir) throws IOException, ParseException {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("entry");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        context.setDatabasePath(tempDir.resolve("library.bib"));
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        var fieldPreferences = preferences.getFieldPreferences();
        // Writer preferences are read after cloning, so this edit cannot be in the first snapshot.
        when(preferences.getFieldPreferences()).thenAnswer(_ -> {
            entry.setField(StandardField.EDITOR, "Editor!");
            return fieldPreferences;
        }).thenReturn(fieldPreferences);
        CoarseChangeFilter filter = new CoarseChangeFilter(context);
        BackupManager manager = BackupManager.start(mock(LibraryTab.class), context, filter, new BibEntryTypesManager(), preferences);
        ImportFormatPreferences importPreferences = mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS);
        Path firstBackup = tempDir.resolve("first.bib");
        Path nextBackup = tempDir.resolve("next.bib");
        Path unnecessaryBackup = tempDir.resolve("unnecessary.bib");
        try {
            entry.setField(StandardField.EDITOR, "Editor");
            manager.performBackup(firstBackup);
            assertEquals(Optional.of("Editor"), BibtexParser.singleFromString(Files.readString(firstBackup), importPreferences)
                                                            .orElseThrow().getField(StandardField.EDITOR));

            manager.performBackup(nextBackup);
            assertEquals(Optional.of("Editor!"), BibtexParser.singleFromString(Files.readString(nextBackup), importPreferences)
                                                             .orElseThrow().getField(StandardField.EDITOR));

            manager.performBackup(unnecessaryBackup);
            assertFalse(Files.exists(unnecessaryBackup));
        } finally {
            BackupManager.shutdown(context, backupDir, false);
            filter.shutdown();
        }
    }

    @Test
        // [utest->req~jabgui.autosaveandbackup.preserve-pending-edits~1]
    void failedBackupCommitLeavesEditsPending(@TempDir Path tempDir) throws IOException, ParseException {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("entry");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        context.setDatabasePath(tempDir.resolve("library.bib"));
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        CoarseChangeFilter filter = new CoarseChangeFilter(context);
        BackupManager manager = BackupManager.start(mock(LibraryTab.class), context, filter, new BibEntryTypesManager(), preferences);
        Path failedBackup = Files.createDirectory(tempDir.resolve("failed.bib"));
        Files.writeString(failedBackup.resolve("existing"), "Keep this file");
        Path nextBackup = tempDir.resolve("next.bib");
        try {
            entry.setField(StandardField.EDITOR, "Editor");
            manager.performBackup(failedBackup);
            manager.performBackup(nextBackup);

            assertEquals(Optional.of("Editor"), BibtexParser.singleFromString(Files.readString(nextBackup), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS))
                                                            .orElseThrow().getField(StandardField.EDITOR));
        } finally {
            BackupManager.shutdown(context, backupDir, false);
            filter.shutdown();
        }
    }

    @Test
        // [utest->req~jabgui.autosaveandbackup.serialized-writes~1]
    void shutdownWaitsForScheduledBackupBeforeWritingNewestEdit(@TempDir Path tempDir) throws Exception {
        BibEntry entry = new BibEntry(StandardEntryType.Article).withCitationKey("entry");
        BibDatabaseContext context = new BibDatabaseContext(new BibDatabase(List.of(entry)));
        Path libraryPath = tempDir.resolve("library.bib");
        context.setDatabasePath(libraryPath);
        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        when(preferences.getFilePreferences().getBackupDirectory()).thenReturn(backupDir);
        var fieldPreferences = preferences.getFieldPreferences();
        CountDownLatch snapshotCaptured = new CountDownLatch(1);
        CountDownLatch releaseSnapshot = new CountDownLatch(1);
        AtomicInteger writes = new AtomicInteger();
        AtomicBoolean overlappingWrites = new AtomicBoolean();
        when(preferences.getFieldPreferences()).thenAnswer(_ -> {
            if (writes.incrementAndGet() == 1) {
                snapshotCaptured.countDown();
                assertTrue(releaseSnapshot.await(5, TimeUnit.SECONDS));
            } else {
                overlappingWrites.set(releaseSnapshot.getCount() > 0);
            }
            return fieldPreferences;
        });
        CoarseChangeFilter filter = new CoarseChangeFilter(context);
        BackupManager manager = BackupManager.start(mock(LibraryTab.class), context, filter, new BibEntryTypesManager(), preferences);
        try (var scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
             var shutdownExecutor = Executors.newSingleThreadExecutor()) {
            try {
                entry.setField(StandardField.EDITOR, "Editor");
                Path backupPath = manager.determineBackupPathForNewBackup(backupDir).orElseThrow();
                Future<?> scheduledBackup = scheduledExecutor.schedule(() -> manager.performBackup(backupPath), 0, TimeUnit.SECONDS);
                assertTrue(snapshotCaptured.await(5, TimeUnit.SECONDS));
                entry.setField(StandardField.EDITOR, "Editor!");
                CompletableFuture<Thread> shutdownThread = new CompletableFuture<>();
                Future<?> shutdownBackup = shutdownExecutor.submit(() -> {
                    shutdownThread.complete(Thread.currentThread());
                    BackupManager.shutdown(context, backupDir, true);
                });
                awaitShutdownBackupAttempt(shutdownBackup, shutdownThread.get(5, TimeUnit.SECONDS));
                releaseSnapshot.countDown();
                scheduledBackup.get(5, TimeUnit.SECONDS);
                shutdownBackup.get(5, TimeUnit.SECONDS);

                Path latestBackup = BackupManager.getLatestBackupPath(libraryPath, backupDir).orElseThrow();
                assertEquals(Optional.of("Editor!"), BibtexParser.singleFromString(Files.readString(latestBackup), mock(ImportFormatPreferences.class, Answers.RETURNS_DEEP_STUBS))
                                                                 .orElseThrow().getField(StandardField.EDITOR));
                assertFalse(overlappingWrites.get());
                assertEquals(2, writes.get());
            } finally {
                releaseSnapshot.countDown();
            }
        } finally {
            BackupManager.shutdown(context, backupDir, false);
            filter.shutdown();
        }
    }

    private void awaitShutdownBackupAttempt(Future<?> shutdownBackup, Thread shutdownThread) {
        long startedAt = System.nanoTime();
        // Release the older writer only once shutdown has finished or is waiting at the backup monitor.
        while (!shutdownBackup.isDone()) {
            if (shutdownThread.getState() == Thread.State.BLOCKED
                    && Arrays.stream(shutdownThread.getStackTrace()).findFirst()
                             .filter(frame -> frame.getClassName().equals(BackupManager.class.getName()) && "performBackup".equals(frame.getMethodName()))
                             .isPresent()) {
                return;
            }
            if (System.nanoTime() - startedAt >= TimeUnit.SECONDS.toNanos(5)) {
                fail("Shutdown did not finish or wait for the in-flight backup");
            }
            Thread.yield();
        }
    }

    @Test
    void shouldNotCreateABackup(@TempDir Path customDir) throws IOException {
        Path backupDir = customDir.resolve("subBackupDir");
        Files.createDirectories(backupDir);

        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        databaseContext.setDatabasePath(customDir.resolve("Bibfile.bib"));

        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getBackupDirectory()).thenReturn(backupDir);
        when(filePreferences.shouldCreateBackup()).thenReturn(false);

        BackupManager manager = BackupManager.start(
                mock(LibraryTab.class),
                databaseContext,
                mock(CoarseChangeFilter.class),
                mock(BibEntryTypesManager.class, Answers.RETURNS_DEEP_STUBS),
                preferences);
        manager.listen(new MetaDataChangedEvent(new MetaData(), MetaDataChangeSource.LOCAL));

        BackupManager.shutdown(databaseContext, filePreferences.getBackupDirectory(), filePreferences.shouldCreateBackup());

        List<Path> files = Files.list(backupDir).toList();
        assertEquals(List.of(), files);
    }

    @Test
    void shouldCreateABackup(@TempDir Path customDir) throws IOException {
        Path backupDir = customDir.resolve("subBackupDir");
        Files.createDirectories(backupDir);

        BibDatabaseContext databaseContext = new BibDatabaseContext(new BibDatabase());
        databaseContext.setDatabasePath(customDir.resolve("Bibfile.bib"));

        CliPreferences preferences = mock(CliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        FilePreferences filePreferences = mock(FilePreferences.class);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.getBackupDirectory()).thenReturn(backupDir);
        when(filePreferences.shouldCreateBackup()).thenReturn(true);

        BackupManager manager = BackupManager.start(
                mock(LibraryTab.class),
                databaseContext,
                mock(CoarseChangeFilter.class),
                mock(BibEntryTypesManager.class, Answers.RETURNS_DEEP_STUBS),
                preferences);
        manager.listen(new MetaDataChangedEvent(new MetaData(), MetaDataChangeSource.LOCAL));

        Optional<Path> fullBackupPath = manager.determineBackupPathForNewBackup(backupDir);
        fullBackupPath.ifPresent(manager::performBackup);
        manager.listen(new GroupUpdatedEvent(new MetaData()));

        BackupManager.shutdown(databaseContext, backupDir, true);

        List<Path> files = Files.list(backupDir).sorted().toList();
        // we only know the first backup path because the second one is created on shutdown
        // due to timing issues we cannot test that reliable
        assertEquals(fullBackupPath.get(), files.getFirst());
    }
}
