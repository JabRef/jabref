package org.jabref.gui.fieldeditors;

import java.io.IOException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import org.jabref.gui.AbstractViewModel;
import org.jabref.gui.DialogService;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

public class DoiEditorViewModel extends AbstractViewModel {
    private BooleanProperty doiIsNotPresent = new SimpleBooleanProperty(true);
    private BooleanProperty doiLookupInProgress = new SimpleBooleanProperty(false);
    private TaskExecutor taskExecutor;
    private DialogService dialogService;

    public DoiEditorViewModel(TaskExecutor taskExecutor, DialogService dialogService) {
        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;
    }

    public boolean isDoiIsNotPresent() {
        return doiIsNotPresent.get();
    }

    public BooleanProperty doiIsNotPresentProperty() {
        return doiIsNotPresent;
    }

    public void openDoi(String doi) {
        try {
            JabRefDesktop.openDoi(doi);
        } catch (IOException ex) {
            dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), ex);
        }
    }

    public boolean isDoiLookupInProgress() {
        return doiLookupInProgress.get();
    }

    public BooleanProperty doiLookupInProgressProperty() {
        return doiLookupInProgress;
    }

    public FetchAndMergeEntry fetchByDoi(BibEntry entry) {
        return new FetchAndMergeEntry(entry, FieldName.DOI);
    }

    public void lookupDoi(BibEntry entry) {
        BackgroundTask
                .wrap(() -> WebFetchers.getIdFetcherForIdentifier(DOI.class).findIdentifier(entry))
                .onRunning(() -> doiLookupInProgress.setValue(true))
                .onFinished(() -> doiLookupInProgress.setValue(false))
                .onSuccess(doi -> {
                    if (doi.isPresent()) {
                        entry.setField(FieldName.DOI, doi.get().getDOI());
                    } else {
                        dialogService.notify(Localization.lang("No %0 found", FieldName.getDisplayName(FieldName.DOI)));
                    }
                })
                .onFailure(dialogService::showErrorDialogAndWait)
                .executeWith(taskExecutor);
    }
}
