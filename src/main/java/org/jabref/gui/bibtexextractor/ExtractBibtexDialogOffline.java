package org.jabref.gui.bibtexextractor;

import javax.swing.undo.UndoManager;

import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BaseDialog;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import com.airhacks.afterburner.views.ViewLoader;
import jakarta.inject.Inject;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 *
 * Code is mostly the same as ExtractBibtexDialogGrobid, but with the title changed to "offline".
 */
public class ExtractBibtexDialogOffline extends BaseDialog<Void> {

    @Inject protected StateManager stateManager;
    @Inject protected DialogService dialogService;
    @Inject protected FileUpdateMonitor fileUpdateMonitor;
    @Inject protected TaskExecutor taskExecutor;
    @Inject protected UndoManager undoManager;
    @Inject protected PreferencesService preferencesService;

    @FXML protected TextArea input;

    @FXML private ButtonType parseButtonType;

    public ExtractBibtexDialogOffline() {
        ViewLoader.view(ExtractBibtexDialogHelper.class)
                  .controller(this)
                  .load()
                  .setAsDialogPane(this);
        this.setTitle(Localization.lang("Plain References Parser (offline)"));
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        BibtexExtractorViewModelOffline bibtexExtractorViewModel = new BibtexExtractorViewModelOffline(
                database,
                dialogService,
                preferencesService,
                fileUpdateMonitor,
                taskExecutor,
                undoManager,
                stateManager);
        Runnable parsingRunnable = () -> bibtexExtractorViewModel.startParsing();
        ExtractBibtexDialogHelper.initialize(input, parseButtonType, bibtexExtractorViewModel.inputTextProperty(), getDialogPane(), parsingRunnable);
    }
}
