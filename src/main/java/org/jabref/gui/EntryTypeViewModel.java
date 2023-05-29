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

import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.importer.NewEntryAction;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;
import org.jabref.preferences.PreferencesService;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryTypeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeViewModel.class);

    private final PreferencesService preferencesService;
    private final BooleanProperty searchingProperty = new SimpleBooleanProperty();
    private final BooleanProperty searchSuccesfulProperty = new SimpleBooleanProperty();
    private final ObjectProperty<IdBasedFetcher> selectedItemProperty = new SimpleObjectProperty<>();
    private final ListProperty<IdBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty idText = new SimpleStringProperty();
    private final BooleanProperty focusAndSelectAllProperty = new SimpleBooleanProperty();
    private Task<Optional<BibEntry>> fetcherWorker = new FetcherWorker();
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final Validator idFieldValidator;
    private final StateManager stateManager;
    private final TaskExecutor taskExecutor;
    private final FileUpdateMonitor fileUpdateMonitor;

    public EntryTypeViewModel(PreferencesService preferences,
                              LibraryTab libraryTab,
                              DialogService dialogService,
                              StateManager stateManager,
                              TaskExecutor taskExecutor,
                              FileUpdateMonitor fileUpdateMonitor) {
        this.libraryTab = libraryTab;
        this.preferencesService = preferences;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.fileUpdateMonitor = fileUpdateMonitor;

        fetchers.addAll(WebFetchers.getIdBasedFetchers(
                preferences.getImportFormatPreferences(),
                preferences.getImporterPreferences()));
        selectedItemProperty.setValue(getLastSelectedFetcher());
        idFieldValidator = new FunctionBasedValidator<>(
                idText,
                StringUtil::isNotBlank,
                ValidationMessage.error(Localization.lang("Required field \"%0\" is empty.", Localization.lang("ID"))));
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
        preferencesService.getGuiPreferences().setLastSelectedIdBasedFetcher(selectedItemProperty.getValue().getName());
    }

    private IdBasedFetcher getLastSelectedFetcher() {
        return fetchers.stream().filter(fetcher -> fetcher.getName()
                                                          .equals(preferencesService.getGuiPreferences()
                                                                                    .getLastSelectedIdBasedFetcher()))
                       .findFirst()
                       .orElse(new DoiFetcher(preferencesService.getImportFormatPreferences()));
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
        protected Optional<BibEntry> call() throws FetcherException {
            searchingProperty().setValue(true);
            storeSelectedFetcher();
            fetcher = selectedItemProperty().getValue();
            searchID = idText.getValue();
            if (searchID.isEmpty()) {
                return Optional.empty();
            }
            return fetcher.performSearchById(searchID);
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

            if (exception instanceof FetcherClientException) {
                dialogService.showInformationDialogAndWait(Localization.lang("Failed to import by ID"), Localization.lang("Bibliographic data not found. Cause is likely the client side. Please check connection and identifier for correctness.") + "\n" + fetcherExceptionMessage);
            } else if (exception instanceof FetcherServerException) {
                dialogService.showInformationDialogAndWait(Localization.lang("Failed to import by ID"), Localization.lang("Bibliographic data not found. Cause is likely the server side. Please try again later.") + "\n" + fetcherExceptionMessage);
            } else {
                dialogService.showInformationDialogAndWait(Localization.lang("Failed to import by ID"), Localization.lang("Error message %0", fetcherExceptionMessage));
            }

            LOGGER.error(String.format("Exception during fetching when using fetcher '%s' with entry id '%s'.", searchId, fetcher), exception);

            searchingProperty.set(false);
            fetcherWorker = new FetcherWorker();
        });

        fetcherWorker.setOnSucceeded(evt -> {
            Optional<BibEntry> result = fetcherWorker.getValue();
            if (result.isPresent()) {
                final BibEntry entry = result.get();

                ImportHandler handler = new ImportHandler(
                        libraryTab.getBibDatabaseContext(),
                        preferencesService,
                        fileUpdateMonitor,
                        libraryTab.getUndoManager(),
                        stateManager,
                        dialogService,
                        taskExecutor);
                handler.importEntryWithDuplicateCheck(libraryTab.getBibDatabaseContext(), entry);

                searchSuccesfulProperty.set(true);
            } else if (StringUtil.isBlank(idText.getValue())) {
                dialogService.showWarningDialogAndWait(Localization.lang("Empty search ID"), Localization.lang("The given search ID was empty."));
            } else {
                // result is empty

                String fetcher = selectedItemProperty().getValue().getName();
                String searchId = idText.getValue();

                // When DOI ID is not found, allow the user to either return to the dialog or add entry manually
                boolean addEntryFlag = dialogService.showConfirmationDialogAndWait(Localization.lang("Identifier not found"),
                        Localization.lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher, searchId),
                        Localization.lang("Add entry manually"),
                        Localization.lang("Return to dialog"));
                if (addEntryFlag) {
                    new NewEntryAction(
                            libraryTab.frame(),
                            StandardEntryType.Article,
                            dialogService,
                            preferencesService,
                            stateManager).execute();
                    searchSuccesfulProperty.set(true);
                }
            }
            fetcherWorker = new FetcherWorker();

            focusAndSelectAllProperty.set(true);
            searchingProperty().setValue(false);
        });
    }
}
