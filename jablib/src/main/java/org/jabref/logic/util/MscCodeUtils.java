package org.jabref.logic.util;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.jabref.logic.msc.MscCodeLoader;
import org.jabref.logic.msc.MscCodeRepository;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MscCodeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MscCodeUtils.class);

    /// Load MSC codes and descriptions from a CSV resource URL into a repository.
    ///
    /// @param resourceUrl URL to the CSV resource containing MSC codes
    /// @return repository with MSC codes as keys and descriptions as values
    /// @throws MscCodeLoadingException if there is an issue loading or parsing the CSV
    @NonNull
    public static Optional<MscCodeRepository> loadMscCodeRepositoryFromCsv(URL resourceUrl) throws MscCodeLoadingException {
        try {
            MscCodeRepository repository = new MscCodeRepository(MscCodeLoader.readMscCodesFromCsvUrl(resourceUrl));
            if (repository.getAllLoaded().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(repository);
        } catch (IOException e) {
            LOGGER.error("Error loading MSC codes from CSV URL", e);
            throw new MscCodeLoadingException("Failed to load MSC codes from CSV URL", e);
        }
    }
}
