package org.jabref.logic.ai.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.logic.util.NotificationService;

import org.h2.mvstore.MVStore;
import org.h2.mvstore.MVStoreException;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MVStoreBase implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreBase.class);

    protected MVStore mvStore;

    public MVStoreBase(@NonNull Path path, NotificationService dialogService) {
        Path mvStorePath = path;

        try {
            Files.createDirectories(path.getParent());
        } catch (IOException e) {
            LOGGER.error(errorMessageForOpening(), e);
            dialogService.notify(errorMessageForOpeningLocalized());
            mvStorePath = null;
        }

        try {
            this.mvStore = new MVStore.Builder()
                    .autoCommitDisabled()
                    .fileName(mvStorePath == null ? null : mvStorePath.toString())
                    .open();
        } catch (MVStoreException e) {
            this.mvStore = new MVStore.Builder()
                    .autoCommitDisabled()
                    .fileName(null) // creates an in memory store
                    .open();
            LOGGER.error(errorMessageForOpening(), e);
        }
    }

    public void commit() {
        mvStore.commit();
    }

    public void close() {
        mvStore.close();
    }

    protected abstract String errorMessageForOpening();

    protected abstract String errorMessageForOpeningLocalized();
}
