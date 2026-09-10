package org.jabref.logic.ai.ingestion.logic.parsing;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;

@NullMarked
public interface FileContentParser {
    List<String> parse(Path path);

    default Optional<String> parseAsString(Path path) {
        List<String> pages = parse(path);
        if (pages.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(String.join("\n\n", pages));
    }
}
