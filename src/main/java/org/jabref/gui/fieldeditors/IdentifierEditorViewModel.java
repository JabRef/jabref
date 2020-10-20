package org.jabref.gui.fieldeditors;

import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefGUI;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.mergeentries.FetchAndMergeEntry;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.util.IdentifierParser;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.identifier.Identifier;

import com.tobiasdiez.easybind.EasyBind;

public class IdentifierEditorViewModel extends AbstractEditorViewModel {
    private final BooleanProperty validIdentifierIsNotPresent = new SimpleBooleanProperty(true);
    private final BooleanProperty identifierLookupInProgress = new SimpleBooleanProperty(false);
    private final BooleanProperty idFetcherAvailable = new SimpleBooleanProperty(true);
    private final ObjectProperty<Optional<? extends Identifier>> identifier = new SimpleObjectProperty<>();
    private final TaskExecutor taskExecutor;
    private final DialogService dialogService;

    public IdentifierEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, TaskExecutor taskExecutor, DialogService dialogService, FieldCheckers fieldCheckers) {
        super(field, suggestionProvider, fieldCheckers);

        this.taskExecutor = taskExecutor;
        this.dialogService = dialogService;

        identifier.bind(
                EasyBind.map(text, input -> IdentifierParser.parse(field, input))
        );

        validIdentifierIsNotPresent.bind(
                EasyBind.map(identifier, parsedIdentifier -> parsedIdentifier.isEmpty())
        );

        idFetcherAvailable.setValue(WebFetchers.getIdFetcherForField(field).isPresent());
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

    public void fetchInformationByIdentifier(BibEntry entry) {
        new FetchAndMergeEntry(JabRefGUI.getMainFrame().getCurrentLibraryTab(), taskExecutor).fetchAndMerge(entry, field);
    }

    public void lookupIdentifier(BibEntry entry) {
        WebFetchers.getIdFetcherForField(field).ifPresent(idFetcher -> {
            BackgroundTask
                    .wrap(() -> idFetcher.findIdentifier(entry))
                    .onRunning(() -> identifierLookupInProgress.setValue(true))
                    .onFinished(() -> identifierLookupInProgress.setValue(false))
                    .onSuccess(identifier -> {
                        if (identifier.isPresent()) {
                            entry.setField(field, identifier.get().getNormalized());
                        } else {
                            dialogService.notify(Localization.lang("No %0 found", field.getDisplayName()));
                        }
                    })
                    .onFailure(dialogService::showErrorDialogAndWait)
                    .executeWith(taskExecutor);
        });
    }
}
