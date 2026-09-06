package org.jabref.gui.autosaveandbackup;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jabref.logic.util.CoarseChangeFilter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.event.AutosaveEvent;
import org.jabref.model.database.event.BibDatabaseContextChangedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Saves the given [BibDatabaseContext] on every [BibDatabaseContextChangedEvent] by posting a new [AutosaveEvent].
/// An intelligent [ScheduledThreadPoolExecutor] prevents a high load while saving and rejects all redundant save tasks.
/// The scheduled action is stored and canceled if a newer save action is proposed.
public class AutosaveManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AutosaveManager.class);

    private static final int DELAY_BETWEEN_AUTOSAVE_ATTEMPTS_IN_SECONDS = 31;

    private static Set<AutosaveManager> runningInstances = new HashSet<>();

    private final BibDatabaseContext bibDatabaseContext;

    private final EventBus eventBus;
    private final ScheduledThreadPoolExecutor executor;
    private final CoarseChangeFilter coarseChangeFilter;
    private final AtomicBoolean needsSave = new AtomicBoolean(false);

    private AutosaveManager(BibDatabaseContext bibDatabaseContext, CoarseChangeFilter coarseChangeFilter) {
        this(bibDatabaseContext, coarseChangeFilter, new ScheduledThreadPoolExecutor(2));
    }

    AutosaveManager(BibDatabaseContext bibDatabaseContext, CoarseChangeFilter coarseChangeFilter, ScheduledThreadPoolExecutor executor) {
        this.bibDatabaseContext = bibDatabaseContext;
        this.coarseChangeFilter = coarseChangeFilter;
        this.eventBus = new EventBus();

        this.executor = executor;
        this.executor.scheduleAtFixedRate(
                () -> {
                    synchronized (this) {
                        // Serialize posting with shutdown so a callback cannot post after disposal.
                        // Clear before posting, so a change arriving while the save runs is not lost
                        if (!executor.isShutdown() && needsSave.getAndSet(false)) {
                            eventBus.post(new AutosaveEvent());
                        }
                    }
                },
                DELAY_BETWEEN_AUTOSAVE_ATTEMPTS_IN_SECONDS,
                DELAY_BETWEEN_AUTOSAVE_ATTEMPTS_IN_SECONDS,
                TimeUnit.SECONDS);
    }

    /// Every change counts, including the keystrokes the filter marks as minor: the filter exists to spare listeners
    /// expensive work per keystroke, but a flag is cheap and the timer throttles the saves anyway. Ignoring minor
    /// changes would leave a field the user only types in unsaved until the user moves to another field.
    @Subscribe
    public void listen(@SuppressWarnings("unused") BibDatabaseContextChangedEvent event) {
        needsSave.set(true);
    }

    boolean isSavePending() {
        return needsSave.get();
    }

    synchronized void shutdown() {
        executor.shutdownNow();
        needsSave.set(false);
        try {
            coarseChangeFilter.unregisterListener(this);
        } catch (IllegalArgumentException e) {
            // ignore exception if the listener was not registered before
        }
        runningInstances.remove(this);
    }

    /// Starts the Autosaver which is associated with the given [BibDatabaseContext].
    /// [impl->req~jabgui.autosaveandbackup.autosave-listens~1]
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    public static AutosaveManager start(BibDatabaseContext bibDatabaseContext, CoarseChangeFilter coarseChangeFilter) {
        AutosaveManager autosaveManager = new AutosaveManager(bibDatabaseContext, coarseChangeFilter);
        coarseChangeFilter.registerListener(autosaveManager);
        runningInstances.add(autosaveManager);
        return autosaveManager;
    }

    /// Shuts down the Autosaver which is associated with the given [BibDatabaseContext].
    ///
    /// @param bibDatabaseContext Associated [BibDatabaseContext]
    public static void shutdown(BibDatabaseContext bibDatabaseContext) {
        runningInstances.stream().filter(instance -> instance.bibDatabaseContext == bibDatabaseContext).findAny()
                        .ifPresent(AutosaveManager::shutdown);
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void unregisterListener(Object listener) {
        try {
            eventBus.unregister(listener);
        } catch (IllegalArgumentException e) {
            // occurs if the event source has not been registered, should not prevent shutdown
            LOGGER.error("Problem unregistering", e);
        }
    }
}
