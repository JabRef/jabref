package org.jabref.gui.bibtexextractor;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseContext;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * GUI Dialog for the feature "Extract BibTeX from plain text".
 */
public class ExtractBibtexDialogOffline extends ExtractBibtexDialog {

    public ExtractBibtexDialogOffline() {
        ViewLoader.view(ExtractBibtexDialog.class)
                  .controller(this)
                  .load()
                  .setAsDialogPane(this);
        this.setTitle(Localization.lang("Plain References Parser (offline)"));
    }

    @Override
    protected BibtexExtractorViewModel getViewModel(BibDatabaseContext database) {
        return new BibtexExtractorViewModelOffline(
                database,
                dialogService,
                preferencesService,
                fileUpdateMonitor,
                taskExecutor,
                undoManager,
                stateManager);
    }
}
