package org.jabref.gui.taskprogressmanager;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.fieldeditors.LinkedFileViewModel;
import org.jabref.gui.groups.GroupTreeNodeViewModel;
import org.jabref.gui.groups.UndoableAddOrRemoveGroup;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableInsertEntries;
import org.jabref.gui.undo.UndoableInsertString;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.MetaData;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.undo.UndoManager;
import java.util.List;
import java.util.Optional;

public class TaskViewModel extends AbstractViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(TaskViewModel.class);

    private final StringProperty message;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private ObservableList<Task> tasks;

    public TaskViewModel(DialogService dialogService, StateManager stateManager) {
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.tasks = stateManager.getBackgroundTasks();
        this.message = new SimpleStringProperty();
    }

    public String getMessage() {
        return message.get();
    }

    public ObservableList<Task> getBackgroundTasks() {return this.tasks;};

    public StringProperty messageProperty() {
        return message;
    }
}
