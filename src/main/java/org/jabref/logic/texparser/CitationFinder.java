package org.jabref.logic.texparser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.jabref.model.texparser.Citation;
import org.jabref.model.texparser.LatexParserResults;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CitationFinder {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationFinder.class);
    private static final String TEX_EXT = ".tex";
    private Path directory;

    private LatexParserResults latexParserResults;

    public CitationFinder(Path directory) {
        this.directory = directory;
    }

    public void setDirectory(Path directory) {
        latexParserResults = null;
        this.directory = directory;
    }

    public Collection<Citation> searchAndParse(String citeKey) throws IOException {
        if (latexParserResults == null) {
            if (!directory.toFile().exists()) {
                throw new IOException("Current search directory does not exist: %s".formatted(directory));
            }

            List<Path> texFiles = searchDirectory(directory);
            LOGGER.debug("Found tex files: {}", texFiles);
            latexParserResults = new DefaultLatexParser().parse(texFiles);
        }

        return latexParserResults.getCitationsByKey(citeKey);
    }

    /**
     * @param directory the directory to search for. It is recursively searched.
     */
    private List<Path> searchDirectory(Path directory) {
        LOGGER.debug("Searching directory {}", directory);
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths.filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(TEX_EXT))
                        .toList();
        } catch (IOException e) {
            LOGGER.error("Error while searching files", e);
            return List.of();
        }
    }
}
