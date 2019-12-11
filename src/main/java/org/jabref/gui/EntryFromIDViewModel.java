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

import java.util.regex.Pattern;
import java.util.HashMap;
import  java.util.function.Predicate;

import javafx.scene.control.TextArea;
import org.jabref.Globals;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.logic.bibtex.DuplicateCheck;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.NoFetcherFoundException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fetcher.*;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntryFromIDViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(EntryFromIDViewModel.class);

    private final JabRefPreferences prefs;
    private final BooleanProperty searchingProperty = new SimpleBooleanProperty();
    private final BooleanProperty searchSuccesfulProperty = new SimpleBooleanProperty();
    private final StringProperty idText = new SimpleStringProperty();
    private  String fetcherName;
    private final BooleanProperty focusAndSelectAllProperty = new SimpleBooleanProperty();
    private Task<Optional<BibEntry>> fetcherWorker = new FetcherWorker();
    private HashMap <Predicate<String>,IdBasedFetcher> fetcherMatchPattern = new HashMap<>();
    private final BasePanel basePanel;
    private final DialogService dialogService;

    public EntryFromIDViewModel(JabRefPreferences preferences, BasePanel basePanel, DialogService dialogService) {
        this.basePanel = basePanel;
        this.prefs = preferences;
        this.dialogService = dialogService;

        //DOI validate hors http
        Pattern pdoi = Pattern.compile("10\\.[0-9]{4}/([0-9]{13}|[a-zA-Z]+\\.[0-9]{4}\\.[0-9]{2}|[-_0-9]+)");
        fetcherMatchPattern.put(pdoi.asPredicate(),new DoiFetcher(preferences.getImportFormatPreferences()));

        //HTTPDOI
        Pattern pHTTPdoi = Pattern.compile("10\\.[0-9]{4}/([0-9]{13}|[a-zA-Z]+\\.[0-9]{4}\\.[0-9]{2}|[-_0-9]+)");
        fetcherMatchPattern.put(pHTTPdoi.asPredicate(),new DoiFetcher(preferences.getImportFormatPreferences()));

        //ISBN validate
        Pattern pisbn = Pattern.compile("^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$");
        fetcherMatchPattern.put(pisbn.asPredicate(),new IsbnFetcher(preferences.getImportFormatPreferences()));
        //Rajouter pour long isbn

        //DIVA Validate
        Pattern pdiva = Pattern.compile("diva2:[0-9]{6}");
        fetcherMatchPattern.put(pdiva.asPredicate(), new DiVA(preferences.getImportFormatPreferences()));

        //MedlineFetcher Validate
        Pattern pmd = Pattern.compile("[0-9]{8}");
        fetcherMatchPattern.put(pmd.asPredicate(), new MedlineFetcher());

        //MathSciNetId validate
        Pattern pMathSciNetId = Pattern.compile("[0-9]{7}");
        fetcherMatchPattern.put(pMathSciNetId.asPredicate(), new MathSciNet(preferences.getImportFormatPreferences()));

        //LibraryOfCongress validate
        Pattern pLibraryOfCongress= Pattern.compile("[0-9]{10}");
        fetcherMatchPattern.put(pLibraryOfCongress.asPredicate(), new LibraryOfCongress(preferences.getImportFormatPreferences()));

        //RfcFetcher validate
        Pattern pRfcFetcher = Pattern.compile("[rR]*[fF]*[cC]*[0-9]{4}");
        fetcherMatchPattern.put(pRfcFetcher.asPredicate(),new RfcFetcher(preferences.getImportFormatPreferences()));

        //Arxiv
        Pattern parxiv = Pattern.compile("[arXiv:]*[0-9][0-9](0[1-9]|1[0-2])\\.[0-9]{4}([v][0-9]+)*");
        fetcherMatchPattern.put(parxiv.asPredicate(), new ArXiv(preferences.getImportFormatPreferences()));

        //AstrophysicsDataSystem
        Pattern pads = Pattern.compile("10\\.[0-9]{5}/([0-9]{13}|[a-zA-Z]+\\.[0-9]{4}\\.[0-9]{2}|[-_0-9]+)");
        fetcherMatchPattern.put(pads.asPredicate(),new AstrophysicsDataSystem(preferences.getImportFormatPreferences()));

        //TitleFetcher
        //CrossRef -> DOI

        //IacrEprintFetcher "Report 2017/1118 "
        Pattern pIacr = Pattern.compile("[a-zA-Z]*\\s*[0-9]{4}/[0-9]{4}");
        fetcherMatchPattern.put(pIacr.asPredicate(),new IacrEprintFetcher(preferences.getImportFormatPreferences()));


    }

    public BooleanProperty searchSuccesfulProperty() {
        return searchSuccesfulProperty;
    }

    public BooleanProperty searchingProperty() {
        return searchingProperty;
    }

    public StringProperty idTextProperty() {
        return idText;
    }

    public BooleanProperty getFocusAndSelectAllProperty() {
        return focusAndSelectAllProperty;
    }


    public void stopFetching() {
        if (fetcherWorker.getState() == Worker.State.RUNNING) {
            fetcherWorker.cancel(true);
        }
    }

    private class FetcherWorker extends Task<Optional<BibEntry>> {

        private IdBasedFetcher fetcher = null;
        private String searchID = "";

        public IdBasedFetcher findFetcher (String id) {

            return fetcherMatchPattern.entrySet().stream()
                    .filter(e -> e.getKey().test(id))
                    .map(e -> e.getValue())
                    .findFirst()
                    .orElseThrow(()-> new NoFetcherFoundException("Can't find fetcher for this id :" + id));

        }

        @Override
        protected Optional<BibEntry> call() throws InterruptedException, FetcherException {
            Optional<BibEntry> bibEntry = Optional.empty();

            searchingProperty().setValue(true);

            searchID = idText.getValue();

            if (!searchID.isEmpty()) {
                try {
                    fetcher = this.findFetcher(searchID);
                    fetcherName=fetcher.getName();
                    bibEntry = fetcher.performSearchById(searchID);
                } catch (NoFetcherFoundException e) {
                    dialogService.showErrorDialogAndWait(Localization.lang("No fetcher found."), Localization.lang("Error while searching fetcher from %0", searchID));
                }

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
            String fetcher = fetcherName;
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
                    DuplicateResolverDialog dialog = new DuplicateResolverDialog(entry, duplicate.get(), DuplicateResolverDialog.DuplicateResolverType.IMPORT_CHECK, basePanel.getBibDatabaseContext());
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
                    new BibtexKeyGenerator(basePanel.getBibDatabaseContext(), prefs.getBibtexKeyPatternPreferences()).generateAndSetKey(entry);
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

    public void runFetcherWorkerForLookUp(TextArea lookUpField) {
        searchSuccesfulProperty.set(false);
        fetcherWorker.run();
        fetcherWorker.setOnFailed(event -> {
            Throwable exception = fetcherWorker.getException();
            String fetcherExceptionMessage = exception.getMessage();
            String fetcher = fetcherName;
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

                String entryString = "Found with : " + fetcherName + " Title : " + entry.getTitle();
                lookUpField.setText(entryString);

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
