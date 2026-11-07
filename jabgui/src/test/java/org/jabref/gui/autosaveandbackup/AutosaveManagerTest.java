package org.jabref.gui.autosaveandbackup;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jabref.gui.dialogs.AutosaveUiManager;
import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.AutosaveEvent;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@NullMarked
class AutosaveManagerTest {

    private final BibDatabaseContext databaseContext = new BibDatabaseContext();

    @AfterEach
    void tearDown() {
        AutosaveManager.shutdown(databaseContext);
    }

    // [utest->req~jabgui.autosaveandbackup.autosave-listens~1]
    @Test
    void startListensToChangesAndShutdownStops() {
        CoarseChangeFilter coarseChangeFilter = mock(CoarseChangeFilter.class);

        AutosaveManager autosaveManager = AutosaveManager.start(databaseContext, coarseChangeFilter);
        verify(coarseChangeFilter).registerListener(autosaveManager);

        AutosaveManager.shutdown(databaseContext);
        verify(coarseChangeFilter).unregisterListener(autosaveManager);
    }

    @Test
    void minorChangeMarksSavePending() {
        AutosaveManager autosaveManager = AutosaveManager.start(databaseContext, mock(CoarseChangeFilter.class));
        assertFalse(autosaveManager.isSavePending());

        // A single typed character, which the filter marks as minor
        FieldChangedEvent keystroke = new FieldChangedEvent(new BibEntry(), StandardField.TITLE, "T", "");
        keystroke.setFiltered(true);
        autosaveManager.listen(keystroke);

        assertTrue(autosaveManager.isSavePending());
        AutosaveManager.shutdown(databaseContext);
    }

    @Test
    void shutdownTerminatesExecutorAndCancelsPeriodicTask() throws InterruptedException {
        try (ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(2)) {
            AutosaveManager manager = new AutosaveManager(databaseContext, mock(CoarseChangeFilter.class), executor);
            try {
                manager.listen(new FieldChangedEvent(new BibEntry(), StandardField.TITLE, "T", ""));

                manager.shutdown();

                assertTrue(executor.isShutdown());
                assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
                assertEquals(List.of(), List.copyOf(executor.getQueue()));
                assertFalse(manager.isSavePending());
            } finally {
                manager.shutdown();
            }
        }
    }

    @Test
    void queuedCallbackDoesNotPostAfterShutdown() {
        ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
        AutosaveManager manager = new AutosaveManager(databaseContext, mock(CoarseChangeFilter.class), executor);
        AutosaveUiManager listener = mock(AutosaveUiManager.class);
        manager.registerListener(listener);
        ArgumentCaptor<Runnable> callback = ArgumentCaptor.forClass(Runnable.class);
        verify(executor).scheduleAtFixedRate(callback.capture(), anyLong(), anyLong(), eq(TimeUnit.SECONDS));

        try {
            manager.listen(new FieldChangedEvent(new BibEntry(), StandardField.TITLE, "T", ""));
            callback.getValue().run();
            verify(listener).listen(any(AutosaveEvent.class));
            clearInvocations(listener);

            manager.listen(new FieldChangedEvent(new BibEntry(), StandardField.TITLE, "Ti", "T"));
            manager.shutdown();
            verify(executor).shutdownNow();
            when(executor.isShutdown()).thenReturn(true);

            // A change notification already in flight can still reach the unregistered listener.
            manager.listen(new FieldChangedEvent(new BibEntry(), StandardField.TITLE, "Tit", "Ti"));
            callback.getValue().run();

            verifyNoInteractions(listener);
        } finally {
            manager.shutdown();
        }
    }
}
