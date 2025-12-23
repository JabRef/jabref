package org.jabref.gui.cleanup;

import java.util.List;
import java.util.function.Supplier;

import javax.swing.undo.UndoManager;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.logic.FilePreferences;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.airhacks.afterburner.views.ViewLoader;

public class CleanupDialog extends BaseDialog<Void> {

    @FXML private TabPane tabPane;
    @FXML private ButtonType cleanUpButton;

    private final CleanupDialogViewModel viewModel;

    // Constructor for multiple-entry cleanup
    public CleanupDialog(BibDatabaseContext databaseContext,
                         CliPreferences preferences,
                         DialogService dialogService,
                         StateManager stateManager,
                         UndoManager undoManager,
                         Supplier<LibraryTab> tabSupplier,
                         TaskExecutor taskExecutor) {
        super();
        this.viewModel = new CleanupDialogViewModel(
                databaseContext, preferences, dialogService,
                stateManager, undoManager, tabSupplier, taskExecutor
        );


        init(databaseContext, preferences);
    }

    // Constructor for single-entry cleanup
    public CleanupDialog(BibEntry targetEntry,
                         BibDatabaseContext databaseContext,
                         CliPreferences preferences,
                         DialogService dialogService,
                         StateManager stateManager,
                         UndoManager undoManager) {

        this.viewModel = new CleanupDialogViewModel(
                databaseContext, preferences, dialogService,
                stateManager, undoManager, null, null
        );

        viewModel.setTargetEntries(List.of(targetEntry));

        init(databaseContext, preferences);
    }

    private void init(BibDatabaseContext databaseContext, CliPreferences preferences) {
        setTitle(Localization.lang("Clean up entries"));

        ViewLoader.view(this)
                  .load()
                  .setAsDialogPane(this);

        CleanupPreferences initialPreset = preferences.getCleanupPreferences();
        FilePreferences filePreferences = preferences.getFilePreferences();

        CleanupSingleFieldPanel singleFieldPanel = new CleanupSingleFieldPanel(initialPreset, viewModel);
        CleanupFileRelatedPanel fileRelatedPanel = new CleanupFileRelatedPanel(databaseContext, initialPreset, filePreferences, viewModel);
        CleanupMultiFieldPanel multiFieldPanel = new CleanupMultiFieldPanel(initialPreset, viewModel);

        tabPane.getTabs().setAll(
                new Tab(Localization.lang("Single field"), singleFieldPanel),
                new Tab(Localization.lang("File-related"), fileRelatedPanel),
                new Tab(Localization.lang("Multi-field"), multiFieldPanel)
        );

        Button btn = (Button) getDialogPane().lookupButton(cleanUpButton);

        btn.addEventFilter(ActionEvent.ACTION, event -> {
            Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
            if (selectedTab != null && selectedTab.getContent() instanceof CleanupPanel panel) {
                viewModel.apply(panel.getSelectedTab());
            }
            event.consume();
        });

        setResultConverter(button -> {
            if (button == ButtonType.CANCEL) {
                return null;
            }
            return null;
        });
    }
}
