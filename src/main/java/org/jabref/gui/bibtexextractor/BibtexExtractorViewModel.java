package org.jabref.gui.bibtexextractor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibtexExtractorViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexExtractorViewModel.class);

    private final StringProperty inputTextProperty = new SimpleStringProperty("");
    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;
    private final ImportHandler importHandler;

    public BibtexExtractorViewModel(BibDatabaseContext bibdatabaseContext,
                                    DialogService dialogService,
                                    PreferencesService preferencesService,
                                    FileUpdateMonitor fileUpdateMonitor,
                                    TaskExecutor taskExecutor,
                                    UndoManager undoManager,
                                    StateManager stateManager) {

        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
        this.importHandler = new ImportHandler(
                bibdatabaseContext,
                ExternalFileTypes.getInstance(),
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager);
    }

    public StringProperty inputTextProperty() {
        return this.inputTextProperty;
    }

    public void startParsing() {
        if (preferencesService.getImporterPreferences().isGrobidEnabled()) {
            parseUsingGrobid();
        } else {
            parseUsingBibtexExtractor();
        }
    }

    private void parseUsingBibtexExtractor() {
        BibEntry parsedEntry = new BibtexExtractor().extract(inputTextProperty.getValue());
        importHandler.importEntries(List.of(parsedEntry));
        trackNewEntry(parsedEntry, "ParseWithBibTeXExtractor");
    }

    private void parseUsingGrobid() {
        GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(preferencesService.getImporterPreferences(), preferencesService.getImportFormatPreferences());
        BackgroundTask.wrap(() -> grobidCitationFetcher.performSearch(inputTextProperty.getValue()))
                      .onRunning(() -> dialogService.notify(Localization.lang("Your text is being parsed...")))
                      .onFailure((e) -> {
                          if (e instanceof FetcherException) {
                              String msg = Localization.lang("There are connection issues with a JabRef server. Detailed information: %0.",
                                      e.getMessage());
                              dialogService.notify(msg);
                          } else {
                              LOGGER.warn("Missing exception handling.", e);
                          }
                      })
                      .onSuccess(parsedEntries -> {
                          dialogService.notify(Localization.lang("%0 entries were parsed from your query.", String.valueOf(parsedEntries.size())));
                          importHandler.importEntries(parsedEntries);
                          for (BibEntry bibEntry : parsedEntries) {
                              trackNewEntry(bibEntry, "ParseWithGrobid");
                          }
                      }).executeWith(taskExecutor);
    }

    private void trackNewEntry(BibEntry bibEntry, String eventMessage) {
        Map<String, String> properties = new HashMap<>();
        properties.put("EntryType", bibEntry.typeProperty().getValue().getName());
        Globals.getTelemetryClient().ifPresent(client -> client.trackEvent(eventMessage, properties, new HashMap<>()));
    }
}
