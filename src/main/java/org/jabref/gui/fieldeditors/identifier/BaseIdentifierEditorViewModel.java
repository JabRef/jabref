package org.jabref.gui.fieldeditors.identifier;

import java.io.IOException;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import org.jabref.gui.DialogService;
import org.jabref.gui.autocompleter.SuggestionProvider;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.fieldeditors.AbstractEditorViewModel;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.util.IdentifierParser;
import org.jabref.logic.integrity.FieldCheckers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.preferences.PreferencesService;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BaseIdentifierEditorViewModel<T extends Identifier> extends AbstractEditorViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseIdentifierEditorViewModel.class);
    protected BooleanProperty isInvalidIdentifier = new SimpleBooleanProperty();
    protected final BooleanProperty identifierLookupInProgress = new SimpleBooleanProperty(false);
    protected final BooleanProperty canLookupIdentifier = new SimpleBooleanProperty(true);
    protected final BooleanProperty canFetchBibliographyInformationById = new SimpleBooleanProperty();
    protected IdentifierParser identifierParser;
    protected final ObjectProperty<Optional<T>> identifier = new SimpleObjectProperty<>(Optional.empty());
    protected DialogService dialogService;
    protected TaskExecutor taskExecutor;
    protected PreferencesService preferences;

    public BaseIdentifierEditorViewModel(Field field, SuggestionProvider<?> suggestionProvider, FieldCheckers fieldCheckers, DialogService dialogService, TaskExecutor taskExecutor, PreferencesService preferences) {
        super(field, suggestionProvider, fieldCheckers);
        this.dialogService = dialogService;
        this.taskExecutor = taskExecutor;
        this.preferences = preferences;
    }

    /**
     * Since it's not possible to perform the same actions on all identifiers, specific implementations can call the {@code configure}
     * method to tell the actions they can perform and the actions they can't. Based on this configuration, the view will enable/disable or
     * show/hide certain UI elements for certain identifier editors.
     * <p>
     * <b>NOTE: This method MUST be called by all the implementation view models in their principal constructor</b>
     * */
    protected final void configure(boolean canFetchBibliographyInformationById, boolean canLookupIdentifier) {
        this.canLookupIdentifier.set(canLookupIdentifier);
        this.canFetchBibliographyInformationById.set(canFetchBibliographyInformationById);
    }

    protected Optional<T> updateIdentifier() {
        if (identifierParser == null) {
            return Optional.empty();
        }

        identifier.set((Optional<T>) identifierParser.parse(field));
        return identifier.get();
    }

    protected void handleIdentifierFetchingError(Exception exception, IdFetcher<T> fetcher) {
        LOGGER.error("Error while fetching identifier", exception);
        if (exception instanceof FetcherClientException) {
            dialogService.showInformationDialogAndWait(Localization.lang("Look up %0", fetcher.getName()), Localization.lang("No data was found for the identifier"));
        } else if (exception instanceof FetcherServerException) {
            dialogService.showInformationDialogAndWait(Localization.lang("Look up %0", fetcher.getName()), Localization.lang("Server not available"));
        } else if (exception.getCause() != null) {
            dialogService.showWarningDialogAndWait(Localization.lang("Look up %0", fetcher.getName()), Localization.lang("Error occured %0", exception.getCause().getMessage()));
        } else {
            dialogService.showWarningDialogAndWait(Localization.lang("Look up %0", fetcher.getName()), Localization.lang("Error occured %0", exception.getCause().getMessage()));
        }
    }

    public BooleanProperty canFetchBibliographyInformationByIdProperty() {
        return canFetchBibliographyInformationById;
    }

    public boolean getCanFetchBibliographyInformationById() {
        return canFetchBibliographyInformationById.get();
    }

    public BooleanProperty canLookupIdentifierProperty() {
        return canLookupIdentifier;
    }

    public boolean getCanLookupIdentifier() {
        return canLookupIdentifier.get();
    }

    public BooleanProperty isInvalidIdentifierProperty() {
        return isInvalidIdentifier;
    }

    public boolean getIsInvalidIdentifier() {
        return isInvalidIdentifier.get();
    }

    public boolean getIdentifierLookupInProgress() {
        return identifierLookupInProgress.get();
    }

    public BooleanProperty identifierLookupInProgressProperty() {
        return identifierLookupInProgress;
    }

    public void fetchBibliographyInformation(BibEntry bibEntry) {
        LOGGER.warn("Unable to fetch bibliography information using the '{}' identifier", field.getDisplayName());
    }

    public void lookupIdentifier(BibEntry bibEntry) {
        LOGGER.warn("Unable to lookup identifier for '{}'", field.getDisplayName());
    }

    public void openExternalLink() {
        identifier.get().flatMap(Identifier::getExternalURI).ifPresent(url -> {
                    try {
                        JabRefDesktop.openBrowser(url);
                    } catch (IOException ex) {
                        dialogService.showErrorDialogAndWait(Localization.lang("Unable to open link."), ex);
                    }
                }
        );
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        super.bindToEntry(entry);
        identifierParser = new IdentifierParser(entry);
        EasyBind.subscribe(textProperty(), ignored -> updateIdentifier());
        EasyBind.subscribe(identifier, newIdentifier -> isInvalidIdentifier.set(newIdentifier.isEmpty()));
    }
}
