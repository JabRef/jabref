package org.jabref.gui.bibtexextractor;

import java.util.List;

import javax.swing.undo.UndoManager;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class BibtexExtractorViewModelOffline extends BibtexExtractorViewModel {

    public BibtexExtractorViewModelOffline(BibDatabaseContext bibdatabaseContext,
                                           DialogService dialogService,
                                           PreferencesService preferencesService,
                                           FileUpdateMonitor fileUpdateMonitor,
                                           TaskExecutor taskExecutor,
                                           UndoManager undoManager,
                                           StateManager stateManager) {
        super(bibdatabaseContext, dialogService, preferencesService, fileUpdateMonitor, taskExecutor, undoManager, stateManager);
    }

    @Override
    public void startParsing() {
        BibEntry parsedEntry = new BibtexExtractor().extract(inputTextProperty.getValue());
        importHandler.importEntries(List.of(parsedEntry));
    }
}
