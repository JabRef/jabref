/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
package org.jabref.logic.journals;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public abstract class AbbreviationLoader<R extends AbbreviationRepository> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbbreviationLoader.class);

    private final String resourcePath;

    protected AbbreviationLoader(String directory, String mvName) {
        resourcePath = "/" + directory + "/" + mvName;
    }

    public static Collection<Abbreviation> readAbbreviationsFromCsvFile(Path file) throws IOException {
        LOGGER.debug("Reading abbreviation list from file {}", file);
        AbbreviationParser parser = new AbbreviationParser();
        parser.readJournalListFromFile(file);
        return parser.getAbbreviations();
    }

    protected R loadRepositorySynchronously(AbbreviationPreferences preferences) {
        R repository;
        Optional<Path> builtInList;
        try {
            builtInList = extractResource(resourcePath);
            repository = builtInList.isPresent()
                         ? createRepository(builtInList.orElseThrow())
                         : createFallbackRepository();
        } catch (IOException e) {
            LOGGER.error("Error while loading abbreviation repository {}", resourcePath, e);
            return createFallbackRepository();
        }

        List<String> externalLists = Optional.ofNullable(preferences.getExternalLists())
                                             .map(List::copyOf)
                                             .orElseGet(List::of);
        for (String filename : externalLists.reversed()) {
            try {
                repository.addCustomAbbreviations(readAbbreviationsFromCsvFile(Path.of(filename)));
            } catch (IOException | InvalidPathException e) {
                LOGGER.error("Cannot read external abbreviation list file {}", filename, e);
            }
        }
        return repository;
    }

    protected static Optional<Path> extractResource(String resourcePath) throws IOException {
        Optional<InputStream> resource = Optional.ofNullable(AbbreviationLoader.class.getResourceAsStream(resourcePath));
        if (resource.isEmpty()) {
            LOGGER.warn("Abbreviation resource {} is not available", resourcePath);
            return Optional.empty();
        }
        try (InputStream input = resource.orElseThrow()) {
            Path directory = Files.createTempDirectory("jabref-abbreviation-loading");
            directory.toFile().deleteOnExit();
            Path file = directory.resolve(Path.of(resourcePath).getFileName());
            file.toFile().deleteOnExit();
            Files.copy(input, file);
            return Optional.of(file);
        }
    }

    protected abstract R createRepository(Path file) throws IOException;

    protected abstract R createFallbackRepository();
}
