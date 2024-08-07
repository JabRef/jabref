package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.util.Optional;

import org.jabref.gui.StateManager;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.entry.field.InternalField;

import com.airhacks.afterburner.injection.Injector;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummariesStorage {
    private final static Logger LOGGER = LoggerFactory.getLogger(SummariesStorage.class);

    private final MVStore mvStore;

    private final EventBus eventBus = new EventBus();

    @Inject private StateManager stateManager = Injector.instantiateModelOrService(StateManager.class);

    public SummariesStorage(MVStore mvStore) {
        this.mvStore = mvStore;
    }

    public void registerListener(Object object) {
        eventBus.register(object);
    }

    public static class SummarySetEvent { }

    private MVMap<String, String> getMap(Path bibDatabasePath) {
        return mvStore.openMap("summarizationRecords-" + bibDatabasePath.toString());
    }

    public void set(Path bibDatabasePath, String citationKey, String contents) {
        getMap(bibDatabasePath).put(citationKey, contents);
        eventBus.post(new SummarySetEvent());
    }

    public Optional<String> get(Path bibDatabasePath, String citationKey) {
        return Optional.ofNullable(getMap(bibDatabasePath).get(citationKey));
    }

    public void clear(Path bibDatabasePath, String citationKey) {
        getMap(bibDatabasePath).remove(citationKey);
    }

    @Subscribe
    private void fieldChangedEventListener(FieldChangedEvent event) {
        // TODO: This methods doesn't take into account if the new citation key is valid.

        if (event.getField() != InternalField.KEY_FIELD) {
            return;
        }

        Optional<BibDatabaseContext> bibDatabaseContext = stateManager.getOpenDatabases().stream().filter(dbContext -> dbContext.getDatabase().getEntries().contains(event.getBibEntry())).findFirst();

        if (bibDatabaseContext.isEmpty()) {
            LOGGER.error("Could not listen to field change event because no database context was found. BibEntry: {}", event.getBibEntry());
            return;
        }

        Optional<Path> bibDatabasePath = bibDatabaseContext.get().getDatabasePath();

        if (bibDatabasePath.isEmpty()) {
            LOGGER.error("Could not listen to field change event because no database path was found. BibEntry: {}", event.getBibEntry());
            return;
        }

        Optional<String> oldSummary = get(bibDatabasePath.get(), event.getOldValue());

        if (oldSummary.isEmpty()) {
            LOGGER.info("Old summary not found for {}", event.getNewValue());
            return;
        }

        set(bibDatabasePath.get(), event.getNewValue(), oldSummary.get());
        clear(bibDatabasePath.get(), event.getOldValue());
    }
}
