package org.jabref.logic.ai.util;

import java.nio.file.Files;
import java.nio.file.Path;

import org.jabref.gui.DialogService;

import jakarta.annotation.Nullable;
import org.h2.mvstore.MVStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class MVStoreBase implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MVStoreBase.class);

    protected final MVStore mvStore;

    public MVStoreBase(Path path, DialogService dialogService) {
        @Nullable Path mvStorePath = path;

        try {
            Files.createDirectories(path.getParent());
        } catch (Exception e) {
            LOGGER.error(errorMessageForOpening(), e);
            dialogService.notify(errorMessageForOpeningLocalized());
            mvStorePath = null;
        }

        this.mvStore = new MVStore.Builder()
                .autoCommitDisabled()
                .fileName(mvStorePath == null ? null : mvStorePath.toString())
                .open();
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
