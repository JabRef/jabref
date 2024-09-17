package org.jabref.gui.bibtexextractor;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * View model for the feature "Extract BibTeX from plain text".
 * Handles both online and offline case.
 *
 * @implNote Instead of using inheritance, we do if/else checks.
 */
public class BibtexExtractorViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexExtractorViewModel.class);

    private final boolean onlineMode;
    private final DialogService dialogService;
    private final CliPreferences preferences;
    private final TaskExecutor taskExecutor;

    private final ImportHandler importHandler;
    private final StringProperty inputTextProperty = new SimpleStringProperty("");

    public BibtexExtractorViewModel(boolean onlineMode,
                                    BibDatabaseContext bibdatabaseContext,
                                    DialogService dialogService,
                                    GuiPreferences preferences,
                                    FileUpdateMonitor fileUpdateMonitor,
                                    TaskExecutor taskExecutor,
                                    UndoManager undoManager,
                                    StateManager stateManager) {
        this.onlineMode = onlineMode;
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.importHandler = new ImportHandler(
                bibdatabaseContext,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);
    }

    public void startParsing() {
        if (onlineMode) {
            startParsingOnline();
        } else {
            startParsingOffline();
        }
    }

    private void startParsingOffline() {
        BibEntry parsedEntry = new BibtexExtractor().extract(inputTextProperty.getValue());
        importHandler.importEntries(List.of(parsedEntry));
    }

    private void startParsingOnline() {
        GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(preferences.getGrobidPreferences(), preferences.getImportFormatPreferences());
        BackgroundTask.wrap(() -> grobidCitationFetcher.performSearch(inputTextProperty.getValue()))
                      .onRunning(() -> dialogService.notify(Localization.lang("Your text is being parsed...")))
                      .onFailure(e -> {
                          if (e instanceof FetcherException) {
                              String msg = Localization.lang("There are connection issues with a JabRef server. Detailed information: %0",
                                      e.getMessage());
                              dialogService.notify(msg);
                          } else {
                              LOGGER.warn("Missing exception handling.", e);
                          }
                      })
                      .onSuccess(parsedEntries -> {
                          dialogService.notify(Localization.lang("%0 entries were parsed from your query.", String.valueOf(parsedEntries.size())));
                          importHandler.importEntries(parsedEntries);
                      }).executeWith(taskExecutor);
    }

    public StringProperty inputTextProperty() {
        return this.inputTextProperty;
    }
}
