package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Chains the given filters - if ALL of them accept, the result is also accepted
public record ChainedFilters(List<DirectoryStream.Filter<Path>> filters) implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChainedFilters.class);

    @Override
    public boolean accept(Path entry) throws IOException {
        return filters.stream().allMatch(filter -> {
            try {
                return filter.accept(entry);
            } catch (IOException e) {
                LOGGER.error("Could not apply filter", e);
                return true;
            }
        });
    }
}
