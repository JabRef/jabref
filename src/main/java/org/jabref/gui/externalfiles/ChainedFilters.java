package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Chains the given filters - if ALL of them accept, the result is also accepted
 */
public class ChainedFilters implements DirectoryStream.Filter<Path> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChainedFilters.class);

    private DirectoryStream.Filter<Path>[] filters;

    public ChainedFilters(DirectoryStream.Filter<Path>... filters) {
        this.filters = filters;
    }

    @Override
    public boolean accept(Path entry) throws IOException {
        return Arrays.stream(filters).allMatch(filter -> {
            try {
                return filter.accept(entry);
            } catch (IOException e) {
                LOGGER.error("Could not apply filter", e);
                return true;
            }
        });
    }
}
