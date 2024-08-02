package org.jabref.gui.bibtexextractor;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public class BibtexExtractorViewModelOffline {

    private final ImportHandler importHandler;
    private final StringProperty inputTextProperty = new SimpleStringProperty("");

    public BibtexExtractorViewModelOffline(BibDatabaseContext bibdatabaseContext,
                                           DialogService dialogService,
                                           PreferencesService preferencesService,
                                           FileUpdateMonitor fileUpdateMonitor,
                                           TaskExecutor taskExecutor,
                                           UndoManager undoManager,
                                           StateManager stateManager) {
        this.importHandler = new ImportHandler(
                bibdatabaseContext,
                preferencesService,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);
    }

    public void startParsing() {
        BibEntry parsedEntry = new BibtexExtractor().extract(inputTextProperty.getValue());
        importHandler.importEntries(List.of(parsedEntry));
    }

    public StringProperty inputTextProperty() {
        return this.inputTextProperty;
    }
}
