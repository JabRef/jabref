package org.jabref.gui.taskprogressmanager;

import com.airhacks.afterburner.views.ViewLoader;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import org.controlsfx.control.TaskProgressView;
import org.fxmisc.easybind.EasyBind;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.*;

import javax.inject.Inject;

public class TaskProgressDialog extends BaseDialog<Boolean> {

    public TaskProgressView<Task<?>> taskProgressView;
    private TaskViewModel viewModel;
    @Inject private DialogService dialogService;
    @Inject private StateManager stateManager;

    private ObservableList<Task<?>> tasks;

    public TaskProgressDialog() {
        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);
    }

    @FXML
    private void initialize() {
        viewModel = new TaskViewModel(dialogService, stateManager);
        taskProgressView.setRetainTasks(true);

        tasks = EasyBind.map(viewModel.getBackgroundTasks(), ObjectProperty<Task<?>>::<Task<?>>get);
        EasyBind.listBind(taskProgressView.getTasks(), tasks);
    }
}
