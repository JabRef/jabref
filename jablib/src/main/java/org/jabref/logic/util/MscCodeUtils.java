package org.jabref.logic.util;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;

import org.jabref.logic.msc.MscCodeLoader;
import org.jabref.logic.msc.MscCodeRepository;
import org.jabref.logic.shared.exception.MscCodeLoadingException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MscCodeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MscCodeUtils.class);

    private static final ReentrantLock MSC_CODES_LOCK = new ReentrantLock();
    private static @Nullable volatile MscCodeRepository mscCodes;

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

    @NonNull
    public static Optional<MscCodeRepository> loadMscCodeRepositoryFromMvStore(Path mvStoreFile) {
        try {
            if (MscCodeLoader.isMvStoreAvailableWithData(mvStoreFile)) {
                return Optional.of(new MscCodeRepository(mvStoreFile));
            }
        } catch (IOException e) {
            LOGGER.error("Error loading MSC codes from MVStore", e);
        }
        return Optional.empty();
    }

    @NonNull
    public static Optional<MscCodeRepository> getMscCodeRepository() {
        if (mscCodes == null) {
            MSC_CODES_LOCK.lock();
            try {
                if (mscCodes == null) {
                    Path mscMvFile = Directories.getMscDirectory().resolve(MscCodeLoader.MSC_FILE_NAME);
                    mscCodes = loadMscCodeRepositoryFromMvStore(mscMvFile).orElseGet(MscCodeRepository::new);
                }
            } finally {
                MSC_CODES_LOCK.unlock();
            }
        }
        return Optional.of(mscCodes);
    }

    public static void setMscCodeRepository(MscCodeRepository repository) {
        MSC_CODES_LOCK.lock();
        try {
            mscCodes = repository;
        } finally {
            MSC_CODES_LOCK.unlock();
        }
    }
}
