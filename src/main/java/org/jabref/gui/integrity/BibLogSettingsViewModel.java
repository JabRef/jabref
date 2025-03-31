package org.jabref.gui.integrity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

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
public class BibLogSettingsViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BibLogSettingsViewModel.class);
    private final MetaData metaData;
    private final Optional<Path> bibPath;
    private final StringProperty path = new SimpleStringProperty("");
    private Optional<Path> lastResolvedBlgPath = Optional.empty();

    public BibLogSettingsViewModel(MetaData metaData, Optional<Path> bibPath) {
        this.metaData = metaData;
        this.bibPath = bibPath;

        Optional<Path> resolved = BibLogPathResolver.resolve(metaData, bibPath);
        LOGGER.info("Resolved .blg path = {}", resolved);

        resolved.ifPresent(resolvedPath -> {
            this.path.set(resolvedPath.toString());
            // If user hasn't set a path, use resolved path as default for display
            if (metaData.getBlgFilePath().isEmpty()) {
                metaData.setBlgFilePath(resolvedPath);
            }
        });
    }

    public StringProperty pathProperty() {
        return path;
    }

    public void setBlgFilePath(Path path) {
        metaData.setBlgFilePath(path);
        this.path.set(path.toString());
    }

    public void resetBlgFilePath() {
        metaData.clearBlgFilePath();
        BibLogPathResolver.resolve(metaData, bibPath)
                          .map(Path::toString)
                          .ifPresentOrElse(
                                  this.path::set,
                                  () -> this.path.set(""));
    }

    public Optional<Path> getResolvedBlgPath() {
        return BibLogPathResolver.resolve(metaData, bibPath)
                                 .filter(Files::exists);
    }

    public Optional<Path> getLastResolvedBlgPath() {
        return lastResolvedBlgPath;
    }

    /**
     * Parses .blg file into IntegrityMessages (if path exists).
     */
    public List<IntegrityMessage> getBlgWarnings(BibDatabaseContext databaseContext) {
        Optional<Path> resolved = BibLogPathResolver.resolve(metaData, bibPath)
                                                    .filter(Files::exists);
        if (resolved.isPresent()) {
            this.lastResolvedBlgPath = resolved;
            try {
                BibtexLogParser parser = new BibtexLogParser();
                List<BibWarning> warnings = parser.parseBiblog(resolved.get());
                return BibWarningToIntegrityMessageConverter.convert(warnings, databaseContext);
            } catch (IOException e) {
                LOGGER.warn("Failed to parse .blg file", e);
            }
        }
        return List.of();
    }
}
