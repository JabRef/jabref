package org.jabref.gui;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.application.Platform;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.shared.SharedDatabaseCleanup;
import org.jabref.gui.testutils.JavaFxExtension;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.DBMSConnectionPropertiesBuilder;
import org.jabref.logic.shared.DBMSType;
import org.jabref.logic.shared.DatabaseLocation;
import org.jabref.logic.shared.DatabaseSynchronizer;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseMode;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
@ExtendWith(JavaFxExtension.class)
class LibraryTabTest {

    // [utest->req~ux.tabs.library-kind-icon~1]
    @ParameterizedTest
    @CsvSource({
            "LOCAL, BIBTEX, BIBTEX_LIBRARY",
            "LOCAL, BIBLATEX, BIBLATEX_LIBRARY",
            "SHARED, BIBTEX, SHARED_DATABASE_LIBRARY",
            "SHARED, BIBLATEX, SHARED_DATABASE_LIBRARY"
    })
    void tabIconTellsLibraryKind(DatabaseLocation location, BibDatabaseMode mode, IconTheme.JabRefIcons expected) {
        assertEquals(expected, LibraryTab.tabIcon(location, mode));
    }

    @Test
    void closesSharedDatabaseContextWhenConnectionCompletesAfterCancellation() throws Exception {
        BibDatabaseContext connectedContext = mock(BibDatabaseContext.class);
        DatabaseSynchronizer synchronizer = mock(DatabaseSynchronizer.class);
        when(connectedContext.getDBMSSynchronizer()).thenReturn(synchronizer);

        LibraryTab.SharedDatabaseLoadingCallbacks callbacks = new LibraryTab.SharedDatabaseLoadingCallbacks(
                mock(LibraryTab.class),
                mock(TaskExecutor.class),
                (_, _) -> {
                },
                _ -> {
                });
        LibraryTab.SharedDatabaseLoadingTask task = new LibraryTab.SharedDatabaseLoadingTask(() -> connectedContext, callbacks);

        task.cancel();
        task.call();

        verify(connectedContext).convertToLocalDatabase();
        verify(synchronizer).closeSharedDatabase();
        verify(connectedContext).clearDBMSSynchronizer();
    }

    @Test
    void failureAfterCancellationDoesNotReachTheFailureHandler() {
        List<Exception> handledFailures = new ArrayList<>();
        LibraryTab.SharedDatabaseLoadingCallbacks callbacks = new LibraryTab.SharedDatabaseLoadingCallbacks(
                mock(LibraryTab.class),
                mock(TaskExecutor.class),
                (_, _) -> {
                },
                handledFailures::add);
        LibraryTab.SharedDatabaseLoadingTask task = new LibraryTab.SharedDatabaseLoadingTask(() -> mock(BibDatabaseContext.class), callbacks);

        task.cancel();
        callbacks.onDatabaseLoadingFailed(new SQLException("Connection refused"));

        assertEquals(List.of(), handledFailures);
    }

    @Test
    void cancellationSchedulesCleanupWhenConnectionAlreadyCompleted() throws Exception {
        BibDatabaseContext connectedContext = mock(BibDatabaseContext.class);
        DatabaseSynchronizer synchronizer = mock(DatabaseSynchronizer.class);
        when(connectedContext.getDBMSSynchronizer()).thenReturn(synchronizer);
        TaskExecutor executor = mock(TaskExecutor.class);
        LibraryTab.SharedDatabaseLoadingCallbacks callbacks = new LibraryTab.SharedDatabaseLoadingCallbacks(
                mock(LibraryTab.class), executor, (_, _) -> {
        }, _ -> {
        });
        LibraryTab.SharedDatabaseLoadingTask task = new LibraryTab.SharedDatabaseLoadingTask(() -> connectedContext, callbacks);

        task.call();
        task.cancel();

        verify(synchronizer, never()).closeSharedDatabase();
        ArgumentCaptor<BackgroundTask<Void>> cleanup = ArgumentCaptor.captor();
        verify(executor).execute(cleanup.capture());
        cleanup.getValue().call();
        verify(synchronizer).closeSharedDatabase();
    }

    // [utest->req~shared-database.background-close~1]
    @Test
    void closePreparesOnFxThreadAndReconnectWaitsForBackgroundCleanup() throws Exception {
        DBMSConnectionProperties properties = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("cleanup-test")
                .setPort(5432)
                .setDatabase("literature")
                .setUser("user")
                .createDBMSConnectionProperties();
        DBMSConnectionProperties otherDatabase = new DBMSConnectionPropertiesBuilder()
                .setType(DBMSType.POSTGRESQL)
                .setHost("cleanup-test")
                .setPort(5432)
                .setDatabase("other")
                .setUser("user")
                .createDBMSConnectionProperties();
        BibDatabaseContext context = mock(BibDatabaseContext.class);
        DatabaseSynchronizer synchronizer = mock(DatabaseSynchronizer.class);
        when(context.getDBMSSynchronizer()).thenReturn(synchronizer);
        when(synchronizer.getConnectionProperties()).thenReturn(properties);
        AtomicBoolean preparedOnFxThread = new AtomicBoolean();
        AtomicBoolean finishedOnFxThread = new AtomicBoolean(true);
        CountDownLatch cleanupStarted = new CountDownLatch(1);
        CountDownLatch releaseCleanup = new CountDownLatch(1);
        CountDownLatch waitersStarted = new CountDownLatch(2);
        doAnswer(_ -> {
            preparedOnFxThread.set(Platform.isFxApplicationThread());
            return null;
        }).when(synchronizer).prepareForClose();
        doAnswer(_ -> {
            finishedOnFxThread.set(Platform.isFxApplicationThread());
            cleanupStarted.countDown();
            releaseCleanup.await();
            return null;
        }).when(synchronizer).finishClose();

        try (var workers = Executors.newFixedThreadPool(3)) {
            TaskExecutor executor = mock(TaskExecutor.class);
            when(executor.execute(any())).thenAnswer(invocation -> {
                BackgroundTask<?> task = invocation.getArgument(0);
                return workers.submit(task::call);
            });
            try {
                JavaFxExtension.invokeAndWait(() -> SharedDatabaseCleanup.close(context, executor));
                assertTrue(cleanupStarted.await(5, TimeUnit.SECONDS));
                JavaFxExtension.invokeAndWait(() -> assertTrue(Platform.isFxApplicationThread()));
                SharedDatabaseCleanup.awaitDatabase(otherDatabase);
                Future<?> reopening = workers.submit(() -> {
                    waitersStarted.countDown();
                    SharedDatabaseCleanup.awaitDatabase(properties);
                    return null;
                });
                Future<?> shutdown = workers.submit(() -> {
                    waitersStarted.countDown();
                    SharedDatabaseCleanup.awaitAll();
                });
                assertTrue(waitersStarted.await(5, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> reopening.get(100, TimeUnit.MILLISECONDS));
                assertThrows(TimeoutException.class, () -> shutdown.get(100, TimeUnit.MILLISECONDS));
                releaseCleanup.countDown();
                reopening.get(5, TimeUnit.SECONDS);
                shutdown.get(5, TimeUnit.SECONDS);
                assertTrue(preparedOnFxThread.get());
                assertFalse(finishedOnFxThread.get());
            } finally {
                releaseCleanup.countDown();
            }
        }
    }
}
