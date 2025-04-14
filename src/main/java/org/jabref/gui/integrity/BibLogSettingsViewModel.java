package org.jabref.gui.integrity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.AbstractViewModel;
import org.jabref.logic.biblog.BibLogPathResolver;
import org.jabref.logic.biblog.BibWarningToIntegrityMessageConverter;
import org.jabref.logic.biblog.BibtexLogParser;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.biblog.BibWarning;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 1. Connects MetaData with the view.
 * 2. Wraps .blg warnings as IntegrityMessages.
 * 3. Supports file browsing and reset actions.
 */
public class BibLogSettingsViewModel extends AbstractViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibLogSettingsViewModel.class);
    private final ObservableList<IntegrityMessage> blgWarnings = FXCollections.observableArrayList();
    private final StringProperty path = new SimpleStringProperty("");
    private final MetaData metaData;
    private final Optional<Path> bibPath;
    private final String user;
    private Optional<Path> lastResolvedBlgPath = Optional.empty();

    public BibLogSettingsViewModel(MetaData metaData, Optional<Path> bibPath) {
        this.metaData = metaData;
        this.bibPath = bibPath;
        this.user = System.getProperty("user.name");

        BibLogPathResolver.resolve(metaData, bibPath, user)
                          .ifPresent(resolvedPath -> {
            this.path.set(resolvedPath.toString());
            if (metaData.getBlgFilePath(user).isEmpty()) {
                metaData.setBlgFilePath(user, resolvedPath);
                this.lastResolvedBlgPath = Optional.of(resolvedPath);
            }
        });
    }

    /**
     * Parses the .blg file (if it exists) into the observable list.
     *
     * @param databaseContext the current database context used to resolve citation keys in warnings.
     */
    public void getBlgWarnings(BibDatabaseContext databaseContext) {
        Optional<Path> resolved = getResolvedBlgPath();
        if (resolved.isEmpty()) {
            blgWarnings.clear();
            return;
        }

        Path path = resolved.get();

        this.lastResolvedBlgPath = Optional.of(path);
        try {
            BibtexLogParser parser = new BibtexLogParser();
            List<BibWarning> warnings = parser.parseBiblog(path);
            List<IntegrityMessage> newWarnings = BibWarningToIntegrityMessageConverter.convert(warnings, databaseContext);
            if (newWarnings.isEmpty()) {
                LOGGER.debug("No blg warning matched the current database.");
            }
            blgWarnings.setAll(newWarnings);
        } catch (IOException e) {
            LOGGER.warn("Failed to parse .blg file", e);
            blgWarnings.clear();
            throw new RuntimeException("Failed to parse .blg file", e);
        }
    }

    public ObservableList<IntegrityMessage> getBlgWarningsObservable() {
        return blgWarnings;
    }

    public StringProperty pathProperty() {
        return path;
    }

    public void setBlgFilePath(Path path) {
        metaData.setBlgFilePath(user, path);
        this.path.set(path.toString());
        this.lastResolvedBlgPath = Optional.of(path);
    }

    public void resetBlgFilePath() {
        metaData.clearBlgFilePath(user);
        Optional<Path> resolved = BibLogPathResolver.resolve(metaData, bibPath, user);
        if (resolved.isEmpty()) {
            path.set("");
            lastResolvedBlgPath = Optional.empty();
            return;
        }

        Path resolvedPath = resolved.get();
        path.set(resolvedPath.toString());
        lastResolvedBlgPath = Optional.of(resolvedPath);
    }

    public Optional<Path> getResolvedBlgPath() {
        return BibLogPathResolver.resolve(metaData, bibPath, user)
                                 .filter(Files::exists);
    }

    public Path getInitialDirectory() {
        return bibPath.flatMap(path -> Optional.ofNullable(path.getParent()))
                      .orElse(Path.of(System.getProperty("user.home")));
    }
}
