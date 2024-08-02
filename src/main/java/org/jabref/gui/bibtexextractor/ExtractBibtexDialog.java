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
 * Handles both online and offline case.
 *
 * @implNote Instead of using inherticance, we do if/else checks
 *
 */
public class ExtractBibtexDialog extends BaseDialog<Void> {

    @Inject protected StateManager stateManager;
    @Inject protected DialogService dialogService;
    @Inject protected FileUpdateMonitor fileUpdateMonitor;
    @Inject protected TaskExecutor taskExecutor;
    @Inject protected UndoManager undoManager;
    @Inject protected PreferencesService preferencesService;

    @FXML protected TextArea input;

    @FXML private ButtonType parseButtonType;

    public ExtractBibtexDialog() {
        ViewLoader.view(ExtractBibtexDialogHelper.class)
                  .controller(this)
                  .load()
                  .setAsDialogPane(this);
        this.setTitle(Localization.lang("Plain References Parser (online)"));
    }

    @FXML
    private void initialize() {
        BibDatabaseContext database = stateManager.getActiveDatabase().orElseThrow(() -> new NullPointerException("Database null"));
        BibtexExtractorViewModelGrobid bibtexExtractorViewModel = new BibtexExtractorViewModelGrobid(
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
