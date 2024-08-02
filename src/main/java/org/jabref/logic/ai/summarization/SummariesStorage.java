package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.util.Optional;

import com.google.common.eventbus.EventBus;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;

public class SummariesStorage {

    private final MVStore mvStore;

    private final EventBus eventBus = new EventBus();

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
}
