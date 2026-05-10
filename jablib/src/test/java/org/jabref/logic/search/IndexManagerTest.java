package org.jabref.logic.search;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.logic.FilePreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.CurrentThreadTaskExecutor;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryPreferences;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.StandardField;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("embeddedPostgres")
class IndexManagerTest {

    private static final TaskExecutor TASK_EXECUTOR = new CurrentThreadTaskExecutor();

    private final CliPreferences preferences = mock(CliPreferences.class);
    private final FilePreferences filePreferences = mock(FilePreferences.class);
    private final BibEntryPreferences bibEntryPreferences = mock(BibEntryPreferences.class);
    private BibDatabaseContext databaseContext;
    private PostgreServer postgreServer;

    @TempDir
    private Path indexDir;

    @BeforeEach
    void setUp() {
        when(preferences.getBibEntryPreferences()).thenReturn(bibEntryPreferences);
        when(preferences.getFilePreferences()).thenReturn(filePreferences);
        when(filePreferences.fulltextIndexLinkedFilesProperty()).thenReturn(new SimpleBooleanProperty(false));
        when(bibEntryPreferences.getKeywordSeparator()).thenReturn(',');

        databaseContext = spy(new BibDatabaseContext());
        when(databaseContext.getFulltextIndexPath()).thenReturn(indexDir);

        postgreServer = new PostgreServer();
    }

    @AfterEach
    void tearDown() {
        postgreServer.close();
    }

    @Test
    void closeAndWaitCancelsScheduledThrottledUpdateAndShutsDownThrottler() throws Exception {
        IndexManager indexManager = new IndexManager(databaseContext, TASK_EXECUTOR, preferences, postgreServer);

        BibEntry entry = new BibEntry().withField(StandardField.TITLE, "old");
        databaseContext.getDatabase().insertEntry(entry);
        indexManager.updateEntry(new FieldChangedEvent(entry, StandardField.TITLE, "new", "old"));

        DelayTaskThrottler throttler = getPrivateField(indexManager, "indexUpdateThrottler", DelayTaskThrottler.class);
        ScheduledFuture<?> scheduledTaskBeforeClose = getPrivateField(throttler, "scheduledTask", ScheduledFuture.class);
        assertNotNull(scheduledTaskBeforeClose);
        assertFalse(scheduledTaskBeforeClose.isCancelled());

        indexManager.closeAndWait();

        ScheduledThreadPoolExecutor executor = getPrivateField(throttler, "executor", ScheduledThreadPoolExecutor.class);
        ScheduledFuture<?> scheduledTaskAfterClose = getPrivateField(throttler, "scheduledTask", ScheduledFuture.class);
        assertTrue(executor.isShutdown());
        assertTrue(scheduledTaskAfterClose == null || scheduledTaskAfterClose.isCancelled());
    }

    @SuppressWarnings("unchecked")
    private static <T> T getPrivateField(Object object, String fieldName, Class<T> expectedType) throws Exception {
        Field field = object.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) expectedType.cast(field.get(object));
    }
}
