package org.jabref.gui;

import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.Worker;

import org.jabref.Globals;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.citationkeypattern.CitationKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryTypeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeViewModel.class);

    private final JabRefPreferences prefs;
    private final BooleanProperty searchingProperty = new SimpleBooleanProperty();
    private final BooleanProperty searchSuccesfulProperty = new SimpleBooleanProperty();
    private final ObjectProperty<IdBasedFetcher> selectedItemProperty = new SimpleObjectProperty<>();
    private final ListProperty<IdBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty idText = new SimpleStringProperty();
    private final BooleanProperty focusAndSelectAllProperty = new SimpleBooleanProperty();
    private Task<Optional<BibEntry>> fetcherWorker = new FetcherWorker();
    private final BasePanel basePanel;
    private final DialogService dialogService;
    private final Validator idFieldValidator;
    private final StateManager stateManager;

    public EntryTypeViewModel(JabRefPreferences preferences, BasePanel basePanel, DialogService dialogService, StateManager stateManager) {
        this.basePanel = basePanel;
        this.prefs = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        fetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences()));
        selectedItemProperty.setValue(getLastSelectedFetcher());
        idFieldValidator = new FunctionBasedValidator<>(idText, StringUtil::isNotBlank, ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("ID"))));
    }

    public BooleanProperty searchSuccesfulProperty() {
        return searchSuccesfulProperty;
    }

    public BooleanProperty searchingProperty() {
        return searchingProperty;
    }

    public ObjectProperty<IdBasedFetcher> selectedItemProperty() {
        return selectedItemProperty;
    }

    public ValidationStatus idFieldValidationStatus() {
        return idFieldValidator.getValidationStatus();
    }

    public StringProperty idTextProperty() {
        return idText;
    }

    public BooleanProperty getFocusAndSelectAllProperty() {
        return focusAndSelectAllProperty;
    }

    public void storeSelectedFetcher() {
        prefs.setIdBasedFetcherForEntryGenerator(selectedItemProperty.getValue().getName());
    }

    private IdBasedFetcher getLastSelectedFetcher() {
        return fetchers.stream().filter(fetcher -> fetcher.getName().equals(prefs.getIdBasedFetcherForEntryGenerator()))
                       .findFirst().orElse(new DoiFetcher(prefs.getImportFormatPreferences()));
    }

    public ListProperty<IdBasedFetcher> fetcherItemsProperty() {
        return fetchers;
    }

    public void stopFetching() {
        if (fetcherWorker.getState() == Worker.State.RUNNING) {
            fetcherWorker.cancel(true);
        }
    }

    private class FetcherWorker extends Task<Optional<BibEntry>> {
        private IdBasedFetcher fetcher = null;
        private String searchID = "";

        @Override
        protected Optional<BibEntry> call() throws InterruptedException, FetcherException {
            Optional<BibEntry> bibEntry = Optional.empty();

            searchingProperty().setValue(true);
            storeSelectedFetcher();
            fetcher = selectedItemProperty().getValue();
            searchID = idText.getValue();
            if (!searchID.isEmpty()) {
                bibEntry = fetcher.performSearchById(searchID);
            }
            return bibEntry;
        }
    }

    public void runFetcherWorker() {
        searchSuccesfulProperty.set(false);
        fetcherWorker.run();
        fetcherWorker.setOnFailed(event -> {
            Throwable exception = fetcherWorker.getException();
            String fetcherExceptionMessage = exception.getMessage();
            String fetcher = selectedItemProperty().getValue().getName();
            String searchId = idText.getValue();
            if (exception instanceof FetcherException) {
                dialogService.showErrorDialogAndWait(Localization.lang("Error"), Localization.lang("Error while fetching from %0", fetcher + "." + "\n" + fetcherExceptionMessage));
            } else {
                dialogService.showErrorDialogAndWait(Localization.lang("No files found.", Localization.lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher, searchId) + "\n" + fetcherExceptionMessage));
            }
            LOGGER.error(String.format("Exception during fetching when using fetcher '%s' with entry id '%s'.", searchId, fetcher), exception);

            searchingProperty.set(false);

            fetcherWorker = new FetcherWorker();
        });

        fetcherWorker.setOnSucceeded(evt -> {
            Optional<BibEntry> result = fetcherWorker.getValue();
            if (result.isPresent()) {
                final BibEntry entry = result.get();
                Optional<BibEntry> duplicate = new DuplicateCheck(Globals.entryTypesManager).containsDuplicate(basePanel.getDatabase(), entry, basePanel.getBibDatabaseContext().getMode());
                if ((duplicate.isPresent())) {
                    DuplicateResolverDialog dialog = new DuplicateResolverDialog(entry, duplicate.get(), DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK, basePanel.getBibDatabaseContext(), stateManager);
                    switch (dialog.showAndWait().orElse(DuplicateResolverDialog.DuplicateResolverResult.BREAK)) {
                        case KEEP_LEFT:
                            basePanel.getDatabase().removeEntry(duplicate.get());
                            basePanel.getDatabase().insertEntry(entry);
                            break;
                        case KEEP_BOTH:
                            basePanel.getDatabase().insertEntry(entry);
                            break;
                        case KEEP_MERGE:
                            basePanel.getDatabase().removeEntry(duplicate.get());
                            basePanel.getDatabase().insertEntry(dialog.getMergedEntry());
                            break;
                        default:
                            // Do nothing
                            break;
                    }
                } else {
                    // Regenerate CiteKey of imported BibEntry
                    new CitationKeyGenerator(basePanel.getBibDatabaseContext(), prefs.getCitationKeyPatternPreferences()).generateAndSetKey(entry);
                    basePanel.insertEntry(entry);
                }
                searchSuccesfulProperty.set(true);
            } else if (StringUtil.isBlank(idText.getValue())) {
                dialogService.showWarningDialogAndWait(Localization.lang("Empty search ID"), Localization.lang("The given search ID was empty."));
            }
            fetcherWorker = new FetcherWorker();

            focusAndSelectAllProperty.set(true);
            searchingProperty().setValue(false);
        });
    }
}
