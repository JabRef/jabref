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
import org.jabref.logic.JabRefException;
import org.jabref.logic.biblog.BibLogPathResolver;
import org.jabref.logic.biblog.BibWarningToIntegrityMessageConverter;
import org.jabref.logic.biblog.BibtexLogParser;
import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.biblog.BibWarning;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

/// 1. Connects MetaData with the view.
/// 2. Wraps .blg warnings as IntegrityMessages.
/// 3. Supports file browsing and reset actions.
public class BibLogSettingsViewModel extends AbstractViewModel {
    private final ObservableList<IntegrityMessage> blgWarnings = FXCollections.observableArrayList();
    private final StringProperty path = new SimpleStringProperty("");
    private final MetaData metaData;
    private final Optional<Path> bibPath;
    private final String user;
    private Optional<Path> lastResolvedBlgPath = Optional.empty();
    private boolean userManuallySelectedBlgFile = false;

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
     * @return An Optional containing the list of integrity messages if the file exists and can be parsed,
     * or an empty Optional if the file does not exist.
     * @throws JabRefException if the .blg file cannot be parsed or read
     */
    public Optional<List<IntegrityMessage>> getBlgWarnings(BibDatabaseContext databaseContext) throws JabRefException {
        Optional<Path> resolved = getResolvedBlgPath();
        if (resolved.isEmpty()) {
            blgWarnings.clear();
            return Optional.empty();
        }

        Path path = resolved.get();

        this.lastResolvedBlgPath = Optional.of(path);
        try {
            BibtexLogParser parser = new BibtexLogParser();
            List<BibWarning> warnings = parser.parseBiblog(path);
            List<IntegrityMessage> newWarnings = BibWarningToIntegrityMessageConverter.convert(warnings, databaseContext);
            blgWarnings.setAll(newWarnings);
            return Optional.of(newWarnings);
        } catch (IOException e) {
            blgWarnings.clear();
            throw new JabRefException(
                    "Failed to parse .blg file",
                    Localization.lang("Could not read BibTeX log file. Please check the file path and try again."),
                    e
            );
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
        this.userManuallySelectedBlgFile = true;
    }

    public void resetBlgFilePath() {
        metaData.clearBlgFilePath(user);
        userManuallySelectedBlgFile = false;
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

    public boolean wasBlgFileManuallySelected() {
        return userManuallySelectedBlgFile;
    }
}
