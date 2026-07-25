package org.jabref.logic.ai.ingestion.logic.parsing;

import java.nio.file.Path;
import java.util.Optional;

public interface FileContentParser {
    Optional<String> parse(Path path);
}
