package org.jabref.logic.ai.impl.embeddings;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.jabref.logic.ai.events.FileIngestedEvent;
import org.jabref.model.entry.LinkedFile;

import com.google.common.eventbus.EventBus;
import org.h2.mvstore.MVStore;

/**
 * This class is responsible for recording the information about which files have been fully ingested.
 * It does not do this automatically, it encapsulates how the information is stored.
 * <p>
 * It will also post an {@link FileIngestedEvent} to its event bus when a file is fully ingested.
 * <p>
 * The class also records the file modification time.
 */
public class IngestedFilesTracker {
    // This map stores the ingested files. The key is LinkedFile.getLink(), and the value is the modification time in seconds.
    // If an entry is present, then it means the file was ingested. Otherwise, file was not ingested.
    // The reason why we need to track ingested files is because we cannot use AiEmbeddingsManager and see if there are
    // any embeddings because when we ingest a file embeddings are generated in series, so if 1 embedding is present
    // it doesn't mean the file is fully ingested.
    private final Map<String, Long> ingestedMap;

    private final EventBus eventBus = new EventBus();

    public IngestedFilesTracker(MVStore mvStore) {
        this.ingestedMap = mvStore.openMap("ingestedMap");
    }

    public boolean hasIngestedFile(String link) {
        return ingestedMap.containsKey(link);
    }

    public boolean hasIngestedFiles(Stream<String> links) {
        return links.allMatch(this::hasIngestedFile);
    }

    public boolean haveIngestedLinkedFiles(Collection<LinkedFile> linkedFiles) {
        return hasIngestedFiles(linkedFiles.stream().map(LinkedFile::getLink));
    }

    public void endIngestingFile(String link, long modificationTimeInSeconds) {
        ingestedMap.put(link, modificationTimeInSeconds);
        eventBus.post(new FileIngestedEvent(link));
    }

    public Optional<Long> getIngestedFileModificationTime(String link) {
        return Optional.ofNullable(ingestedMap.get(link));
    }

    public void registerListener(Object listener) {
        eventBus.register(listener);
    }

    public void removeIngestedFileTrack(String link) {
        ingestedMap.remove(link);
    }

    public Set<String> getIngestedLinkedFiles() {
        return new HashSet<>(ingestedMap.keySet());
    }
}
