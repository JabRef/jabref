package org.jabref.gui;

import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

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

import org.jabref.gui.importer.ImportInspectionDialog;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryTypeViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryTypeViewModel.class);

    private final JabRefPreferences prefs;
    private final BooleanProperty searchingProperty = new SimpleBooleanProperty();
    private final ObjectProperty<IdBasedFetcher> selectedItemProperty = new SimpleObjectProperty<>();
    private final ListProperty<IdBasedFetcher> fetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final StringProperty idText = new SimpleStringProperty();
    private Task<Optional<BibEntry>> fetcherWorker = new FetcherWorker();
    private final BasePanel basePanel;
    private final DialogService dialogService;

    public EntryTypeViewModel(JabRefPreferences preferences, BasePanel basePanel, DialogService dialogService) {
        this.basePanel = basePanel;
        this.prefs = preferences;
        this.dialogService = dialogService;
        fetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences()));
        selectedItemProperty.setValue(getLastSelectedFetcher());
    }

    public BooleanProperty searchingProperty() {
        return searchingProperty;
    }

    public ObjectProperty<IdBasedFetcher> selectedItemProperty() {
        return selectedItemProperty;
    }

    public StringProperty idTextProperty() {
        return idText;
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

        private boolean fetcherException = false;
        private String fetcherExceptionMessage = "";
        private IdBasedFetcher fetcher = null;
        private String searchID = "";

        @Override
        protected void done() {
            try {
                Optional<BibEntry> result = get();
                if (result.isPresent()) {
                    final BibEntry bibEntry = result.get();
                    if ((DuplicateCheck.containsDuplicate(basePanel.getDatabase(), bibEntry, basePanel.getBibDatabaseContext().getMode()).isPresent())) {
                        //If there are duplicates starts ImportInspectionDialog
                        final BasePanel panel = basePanel;

                        ImportInspectionDialog diag = new ImportInspectionDialog(basePanel.frame(), panel, Localization.lang("Import"), false);
                        diag.addEntries(Arrays.asList(bibEntry));
                        diag.entryListComplete();
                        diag.setVisible(true);
                        diag.toFront();
                    } else {
                        // Regenerate CiteKey of imported BibEntry
                        new BibtexKeyGenerator(basePanel.getBibDatabaseContext(), prefs.getBibtexKeyPatternPreferences()).generateAndSetKey(bibEntry);
                        // Update Timestamps
                        if (prefs.getTimestampPreferences().includeCreatedTimestamp()) {
                            bibEntry.setField(prefs.getTimestampPreferences().getTimestampField(), prefs.getTimestampPreferences().now());
                        }
                        basePanel.insertEntry(bibEntry);
                    }

                    // close();
                } else if (StringUtil.isBlank(searchID)) {
                    dialogService.showWarningDialogAndWait(Localization.lang("Empty search ID"), Localization.lang("The given search ID was empty."));
                } else if (!fetcherException) {
                    dialogService.showErrorDialogAndWait(Localization.lang("No files found.", Localization.lang("Fetcher '%0' did not find an entry for id '%1'.", fetcher.getName(), searchID) + "\n" + fetcherExceptionMessage));
                } else {
                    dialogService.showErrorDialogAndWait(Localization.lang("Error"), Localization.lang("Error while fetching from %0", fetcher.getName()) + "." + "\n" + fetcherExceptionMessage);
                }
                fetcherWorker = new FetcherWorker();

                /* idTextField.requestFocus();
                idTextField.selectAll();
                */
                searchingProperty().setValue(false);

            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error(String.format("Exception during fetching when using fetcher '%s' with entry id '%s'.", searchID, fetcher.getName()), e);
            }
        }

        @Override
        protected Optional<BibEntry> call() throws Exception {
            Optional<BibEntry> bibEntry = Optional.empty();

            searchingProperty().setValue(true);
            storeSelectedFetcher();
            fetcher = selectedItemProperty().getValue();
            searchID = idText.getValue();
            if (!searchID.isEmpty()) {
                try {
                    bibEntry = fetcher.performSearchById(searchID);
                } catch (FetcherException e) {
                    LOGGER.error(e.getMessage(), e);
                    fetcherException = true;
                    fetcherExceptionMessage = e.getMessage();
                }
            }
            return bibEntry;
        }
    }

    public void runFetcherWorker() {
        fetcherWorker.run();
    }
}
