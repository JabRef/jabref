package net.sf.jabref.autosave;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.sf.jabref.autosave.event.AutosaveEvent;
import net.sf.jabref.event.BibDatabaseContextChangedEvent;
import net.sf.jabref.model.database.BibDatabaseContext;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Saves the given {@link BibDatabaseContext} on every {@link BibDatabaseContextChangedEvent} by posting a new {@link AutosaveEvent}.
 * An intelligent {@link ExecutorService} with a {@link BlockingQueue} prevents a high load while saving and rejects all redundant save tasks.
 */
public class AutosaveManager {

    private static final Log LOGGER = LogFactory.getLog(AutosaveManager.class);

    private static Set<AutosaveManager> runningInstances = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;
    private final BlockingQueue<Runnable> workerQueue;
    private final ExecutorService executor;
    private final EventBus eventBus;


    public AutosaveManager(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.workerQueue = new ArrayBlockingQueue<>(1);
        this.executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, workerQueue);
        this.eventBus = new EventBus();

        bibDatabaseContext.getDatabase().registerListener(this);
        bibDatabaseContext.getMetaData().registerListener(this);
        runningInstances.add(this);
    }

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        try {
            executor.submit(() -> {
                eventBus.post(new AutosaveEvent());
            });
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Rejecting autosave while another save process is already running.");
        }
    }

    private void shutdown() {
        bibDatabaseContext.getDatabase().unregisterListener(this);
        bibDatabaseContext.getMetaData().unregisterListener(this);
        executor.shutdown();
    }

    /**
     * Shuts down the Autosaver which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext) {
        for (AutosaveManager autosaver : runningInstances) {
            if (autosaver.bibDatabaseContext == bibDatabaseContext) {
                autosaver.shutdown();
            }
        }
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        eventBus.unregister(listener);
    }
}
