package org.jabref.logic.ai.summarization;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import com.google.common.eventbus.EventBus;
import org.h2.mvstore.MVStore;

public class SummariesStorage {
    private record SummarizationRecord(String library, String citationKey, String contents) implements Serializable { }

    private final Map<Integer, SummarizationRecord> summarizationRecords;

    private final EventBus eventBus = new EventBus();

    public SummariesStorage(MVStore mvStore) {
        this.summarizationRecords = mvStore.openMap("summarizationRecords");
    }

    public void registerListener(Object object) {
        eventBus.register(object);
    }

    public static class SummarySetEvent { }

    public void set(Path bibDatabasePath, String citationKey, String contents) {
        int id = summarizationRecords.keySet().size() + 1;
        SummarizationRecord summarizationRecord = new SummarizationRecord(bibDatabasePath.toString(), citationKey, contents);
        summarizationRecords.put(id, summarizationRecord);

        eventBus.post(new SummarySetEvent());
    }

    public Optional<String> get(Path bibDatabasePath, String citationKey) {
        return summarizationRecords
                .values()
                .stream()
                .filter(v -> v.library.equals(bibDatabasePath.toString()) && v.citationKey.equals(citationKey))
                .map(v -> v.contents)
                .findFirst();
    }

    public void clear(Path bibDatabasePath, String citationKey) {
        summarizationRecords
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue().library.equals(bibDatabasePath.toString()) && entry.getValue().citationKey.equals(citationKey))
                .map(Map.Entry::getKey)
                .forEach(summarizationRecords::remove);
    }
}
