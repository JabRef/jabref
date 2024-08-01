package org.jabref.gui.bibtexextractor;

import javax.swing.undo.UndoManager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

public abstract class BibtexExtractorViewModel {

    protected final ImportHandler importHandler;
    protected final StringProperty inputTextProperty = new SimpleStringProperty("");

    public BibtexExtractorViewModel(BibDatabaseContext bibdatabaseContext,
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

    public StringProperty inputTextProperty() {
        return this.inputTextProperty;
    }

    public abstract void startParsing();
}
