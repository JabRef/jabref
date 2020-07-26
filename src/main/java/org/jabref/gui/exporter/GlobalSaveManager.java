package org.jabref.gui.exporter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.jabref.gui.exporter.SaveAction.SaveMethod;
import org.jabref.logic.util.DelayTaskThrottler;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.AutosaveEvent;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;
import org.jabref.model.database.event.CoarseChangeFilter;
import org.jabref.model.database.event.SaveEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Saves the given {@link BibDatabaseContext} on every {@link BibDatabaseContextChangedEvent} by posting a new {@link AutosaveEvent}.
 * An intelligent {@link ScheduledThreadPoolExecutor} prevents a high load while saving and rejects all redundant save tasks.
 * The scheduled action is stored and canceled if a newer save action is proposed.
 */
public class GlobalSaveManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalSaveManager.class);

    private static Map<BibDatabaseContext, GlobalSaveManager> runningInstances = new HashMap<>();

    private final BibDatabaseContext bibDatabaseContext;

    private final EventBus eventBus;
    private final CoarseChangeFilter changeFilter;
    private final DelayTaskThrottler throttler;

    private GlobalSaveManager(BibDatabaseContext bibDatabaseContext) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.throttler = new DelayTaskThrottler(2000);
        this.eventBus = new EventBus();
        this.changeFilter = new CoarseChangeFilter(bibDatabaseContext);
        changeFilter.registerListener(this);
    }

    @Subscribe
    public synchronized void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        throttler.schedule(() -> {
            eventBus.post(new SaveEvent());
        });
    }

    public static void addSaveAction(SaveDatabaseAction action, SaveMethod saveMethod, BibDatabaseContext bibDatabaseContext) {

        var instance = runningInstances.get(bibDatabaseContext);

                // Never happens

        if(instance != null) {
            instance.throttler.schedule(() -> {
                execsaveAction(saveMethod, action);
            });


        }
    }
    private static void execsaveAction(SaveMethod saveMethod, SaveDatabaseAction action) {
        switch (saveMethod) {
            case SAVE:
                action.save();
                break;
            case SAVE_AS:
                action.saveAs();
                break;
            case SAVE_SELECTED:
                action.saveSelectedAsPlain();
                break;
            default:
        }
    }

    private void shutdown() {
        changeFilter.unregisterListener(this);
        changeFilter.shutdown();
        throttler.shutdown();
    }

    /**
     * Starts the Autosaver which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static GlobalSaveManager start(BibDatabaseContext bibDatabaseContext) {
        GlobalSaveManager autosaver = new GlobalSaveManager(bibDatabaseContext);
        runningInstances.put(bibDatabaseContext, autosaver);
        return autosaver;
    }

    /**
     * Shuts down the Autosaver which is associated with the given {@link BibDatabaseContext}.
     *
     * @param bibDatabaseContext Associated {@link BibDatabaseContext}
     */
    public static void shutdown(BibDatabaseContext bibDatabaseContext) {
        var instance = runningInstances.get(bibDatabaseContext);
        if(instance != null) {
            instance.shutdown();
            runningInstances.remove(bibDatabaseContext);
        }
    }



    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        try {
            eventBus.unregister(listener);
        } catch (IllegalArgumentException e) {
            // occurs if the event source has not been registered, should not prevent shutdown
            LOGGER.debug("Proble, unregistering", e);
        }
    }


}
