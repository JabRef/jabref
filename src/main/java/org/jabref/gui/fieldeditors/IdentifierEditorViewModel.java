package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.AutoCompleteSuggestionProvider;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.util.IdentifierParser;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.Identifier;

import org.fxmisc.easybind.EasyBind;

public class IdentifierEditorViewModel extends AbstractEditorViewModel {
    private BooleanProperty validIdentifierIsNotPresent = new SimpleBooleanProperty(true);
    private BooleanProperty identifierLookupInProgress = new SimpleBooleanProperty(false);
    private BooleanProperty idFetcherAvailable = new SimpleBooleanProperty(true);
    private ObjectProperty<Optional<? extends Identifier>> identifier = new SimpleObjectProperty<>();
    private TaskExecutor taskExecutor;
    private DialogService dialogService;

    public IdentifierEditorViewModel(String fieldName, AutoCompleteSuggestionProvider<?> suggestionProvider, TaskExecutor taskExecutor, DialogService dialogService, FieldCheckers fieldCheckers) {
        super(fieldName, suggestionProvider, fieldCheckers);

        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        identifier.bind(
                EasyBind.map(text, input -> IdentifierParser.parse(fieldName, input))
        );

        validIdentifierIsNotPresent.bind(
                EasyBind.map(identifier, parsedIdentifier -> !parsedIdentifier.isPresent())
        );

        idFetcherAvailable.setValue(WebFetchers.getIdFetcherForField(fieldName).isPresent());
    }

    public boolean isIdFetcherAvailable() {
        return idFetcherAvailable.get();
    }

    public BooleanProperty idFetcherAvailableProperty() {
        return idFetcherAvailable;
    }

    public boolean getValidIdentifierIsNotPresent() {
        return validIdentifierIsNotPresent.get();
    }

    public BooleanProperty validIdentifierIsNotPresentProperty() {
        return validIdentifierIsNotPresent;
    }

    public void openExternalLink() {
        identifier.get().flatMap(Identifier::getExternalURI).ifPresent(
                url -> {
                    try {
                        JabRefDesktop.openBrowser(url);
                    } catch (IOException ex) {
                        dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), ex);
                    }
                }
        );

    }

    public boolean getIdentifierLookupInProgress() {
        return identifierLookupInProgress.get();
    }

    public BooleanProperty identifierLookupInProgressProperty() {
        return identifierLookupInProgress;
    }

    public FetchAndMergeEntry fetchInformationByIdentifier(BibEntry entry) {
        return new FetchAndMergeEntry(entry, fieldName);
    }

    public void lookupIdentifier(BibEntry entry) {
        WebFetchers.getIdFetcherForField(fieldName).ifPresent(idFetcher -> {
            BackgroundTask
                    .wrap(() -> idFetcher.findIdentifier(entry))
                    .onRunning(() -> identifierLookupInProgress.setValue(true))
                    .onFinished(() -> identifierLookupInProgress.setValue(false))
                    .onSuccess(identifier -> {
                        if (identifier.isPresent()) {
                            entry.setField(fieldName, identifier.get().getNormalized());
                        } else {
                            dialogService.notify(Localization.lang("No %0 found", FieldName.getDisplayName(fieldName)));
                        }
                    })
                    .onFailure(dialogService::showErrorDialogAndWait)
                    .executeWith(taskExecutor);
        });
    }
}
