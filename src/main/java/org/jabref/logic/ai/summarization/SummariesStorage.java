package org.jabref.logic.ai.summarization;

import java.nio.file.Path;
import java.util.Optional;

public interface SummariesStorage {
    void set(Path bibDatabasePath, String citationKey, Summary summary);

    Optional<Summary> get(Path bibDatabasePath, String citationKey);

    void clear(Path bibDatabasePath, String citationKey);
}
