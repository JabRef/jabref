package org.jabref.gui.bibtexextractor;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.fetcher.GrobidCitationFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BibtexExtractorViewModelGrobid extends BibtexExtractorViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(BibtexExtractorViewModelGrobid.class);

    private final DialogService dialogService;
    private final PreferencesService preferencesService;
    private final TaskExecutor taskExecutor;

    public BibtexExtractorViewModelGrobid(BibDatabaseContext bibdatabaseContext,
                                          DialogService dialogService,
                                          PreferencesService preferencesService,
                                          FileUpdateMonitor fileUpdateMonitor,
                                          TaskExecutor taskExecutor,
                                          UndoManager undoManager,
                                          StateManager stateManager) {
        super(bibdatabaseContext, dialogService, preferencesService, fileUpdateMonitor, taskExecutor, undoManager, stateManager);
        this.dialogService = dialogService;
        this.preferencesService = preferencesService;
        this.taskExecutor = taskExecutor;
    }

    @Override
    public void startParsing() {
        GrobidCitationFetcher grobidCitationFetcher = new GrobidCitationFetcher(preferencesService.getGrobidPreferences(), preferencesService.getImportFormatPreferences());
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
}
