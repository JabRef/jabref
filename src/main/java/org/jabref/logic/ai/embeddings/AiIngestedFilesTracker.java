package org.jabref.logic.ai.embeddings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.ai.embeddings.events.FileIngestedEvent;
import org.jabref.model.entry.LinkedFile;

import com.google.common.eventbus.EventBus;
import jakarta.validation.constraints.Null;
import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.jspecify.annotations.Nullable;

public class AiIngestedFilesTracker {
    private final MVMap<String, Long> ingestedMap;

    private final EventBus eventBus = new EventBus();

    public AiIngestedFilesTracker(MVStore mvStore) {
        this.ingestedMap = mvStore.openMap("ingestedMap");
    }

    public void endIngestingFile(String link, long modificationTimeInSeconds) {
        ingestedMap.put(link, modificationTimeInSeconds);
        eventBus.post(new FileIngestedEvent(link));
    }

    public boolean haveIngestedFile(String link) {
        return ingestedMap.get(link) != null;
    }

    public @Nullable Long getIngestedFileModificationTime(String link) {
        return ingestedMap.get(link);
    }

    public boolean haveIngestedFiles(Stream<String> links) {
        return links.allMatch(this::haveIngestedFile);
    }

    public boolean haveIngestedLinkedFiles(Collection<LinkedFile> linkedFiles) {
        return haveIngestedFiles(linkedFiles.stream().map(LinkedFile::getLink));
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void removeIngestedFileTrack(String link) {
        ingestedMap.remove(link);
    }

    public Set<String> getListOfIngestedFilesLinks() {
        return new HashSet<>(ingestedMap.keySet());
    }
}
