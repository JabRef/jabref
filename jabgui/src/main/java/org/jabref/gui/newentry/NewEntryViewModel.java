package org.jabref.gui.newentry;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.importer.BookCoverFetcher;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.gui.validation.ValidationConstraints;
import org.jabref.gui.validation.ValidationMessage;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.plaincitation.PlainCitationParserFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.logic.layout.format.DOIStrip;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.TransferInformation;
import org.jabref.model.TransferMode;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.util.FileUpdateMonitor;

import org.jfxcore.validation.property.ConstrainedObjectProperty;
import org.jfxcore.validation.property.ConstrainedStringProperty;
import org.jfxcore.validation.property.SimpleConstrainedObjectProperty;
import org.jfxcore.validation.property.SimpleConstrainedStringProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryViewModel.class);

    private static final LayoutFormatter DOI_STRIP = new DOIStrip();

    private final GuiPreferences preferences;
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UiTaskExecutor taskExecutor;
    private final FileUpdateMonitor fileUpdateMonitor;
    private final AiService aiService;

    private final BookCoverFetcher bookCoverFetcher;

    private final BooleanProperty executing;
    private final BooleanProperty executedSuccessfully;

    private final ConstrainedStringProperty<ValidationMessage> idText;
    private final ConstrainedStringProperty<ValidationMessage> duplicateDoi;
    private final ListProperty<IdBasedFetcher> idFetchers;
    private final ConstrainedObjectProperty<IdBasedFetcher, ValidationMessage> idFetcher;
    private Task<Optional<BibEntry>> idLookupWorker;

    private final ConstrainedStringProperty<ValidationMessage> interpretText;
    private final ListProperty<PlainCitationParserChoice> interpretParsers;
    private final ObjectProperty<PlainCitationParserChoice> interpretParser;
    private Task<Optional<List<BibEntry>>> interpretWorker;

    private final ConstrainedStringProperty<ValidationMessage> bibtexText;
    private Task<Optional<List<BibEntry>>> bibtexWorker;
    private final Map<String, BibEntry> doiCache;
    private BibEntry duplicateEntry;

    public NewEntryViewModel(GuiPreferences preferences,
                             LibraryTab libraryTab,
                             DialogService dialogService,
                             StateManager stateManager,
                             UiTaskExecutor taskExecutor,
                             FileUpdateMonitor fileUpdateMonitor,
                             AiService aiService) {
        this.preferences = preferences;
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.fileUpdateMonitor = fileUpdateMonitor;
        this.aiService = aiService;

        this.bookCoverFetcher = new BookCoverFetcher(preferences.getExternalApplicationsPreferences());

        executing = new SimpleBooleanProperty(false);
        executedSuccessfully = new SimpleBooleanProperty(false);
        doiCache = new HashMap<>();

        idText = new SimpleConstrainedStringProperty<>("",
                ValidationConstraints.predicate(StringUtil::isNotBlank,
                        ValidationMessage.error(Localization.lang("You must specify an identifier."))));

        duplicateDoi = new SimpleConstrainedStringProperty<>("",
                ValidationConstraints.function(this::checkDOI));
        // Two independent validators over the same underlying text, mirrored via listener rather than merged into
        // one property, since idTextValidator and duplicateDoi drive different, independent parts of the UI.
        idText.addListener((_, _, newValue) -> duplicateDoi.setValue(newValue));

        idFetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
        idFetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences()));
        idFetcher = new SimpleConstrainedObjectProperty<IdBasedFetcher, ValidationMessage>(
                ValidationConstraints.predicate(Objects::nonNull,
                        ValidationMessage.error(Localization.lang("You must select an identifier type."))));
        idLookupWorker = null;

        interpretText = new SimpleConstrainedStringProperty<>("",
                ValidationConstraints.predicate(StringUtil::isNotBlank,
                        ValidationMessage.error(Localization.lang("You must specify one (or more) citations."))));
        interpretParsers = new SimpleListProperty<>(FXCollections.observableArrayList());
        interpretParsers.addAll(PlainCitationParserChoice.values());
        if (!preferences.getAiPreferences().getAiFeaturesEnabled()) {
            interpretParsers.remove(PlainCitationParserChoice.LLM);
        }
        interpretParser = new SimpleObjectProperty<>();
        interpretWorker = null;

        bibtexText = new SimpleConstrainedStringProperty<>("",
                ValidationConstraints.predicate(StringUtil::isNotBlank,
                        ValidationMessage.error(Localization.lang("You must specify a Bib(La)TeX source."))));
        bibtexWorker = null;
    }

    public void populateDOICache() {
        doiCache.clear();
        stateManager.getActiveDatabase()
                    .map(BibDatabaseContext::getEntries)
                    .stream().flatMap(List::stream)
                    .forEach(entry -> {
                        entry.getField(StandardField.DOI)
                             .ifPresent(doi -> {
                                 doiCache.put(doi, entry);
                             });
                    });
    }

    public Optional<ValidationMessage> checkDOI(String doiInput) {
        if (StringUtil.isBlank(doiInput)) {
            return Optional.empty();
        }
        String normalized = DOI_STRIP.format(doiInput.toLowerCase());
        if (doiCache.containsKey(normalized)) {
            duplicateEntry = doiCache.get(normalized);
            return Optional.of(ValidationMessage.warning(Localization.lang("Entry already exists in a library")));
        }

        return Optional.empty();
    }

    public BibEntry getDuplicateEntry() {
        return duplicateEntry;
    }

    public ReadOnlyBooleanProperty executingProperty() {
        return executing;
    }

    public ReadOnlyBooleanProperty executedSuccessfullyProperty() {
        return executedSuccessfully;
    }

    public ConstrainedStringProperty<ValidationMessage> idTextProperty() {
        return idText;
    }

    public ReadOnlyBooleanProperty idTextValidatorProperty() {
        return idText.validProperty();
    }

    public ConstrainedStringProperty<ValidationMessage> duplicateDoiProperty() {
        return duplicateDoi;
    }

    public ListProperty<IdBasedFetcher> idFetchersProperty() {
        return idFetchers;
    }

    public ConstrainedObjectProperty<IdBasedFetcher, ValidationMessage> idFetcherProperty() {
        return idFetcher;
    }

    public ReadOnlyBooleanProperty idFetcherValidatorProperty() {
        return idFetcher.validProperty();
    }

    public ConstrainedStringProperty<ValidationMessage> interpretTextProperty() {
        return interpretText;
    }

    public ReadOnlyBooleanProperty interpretTextValidatorProperty() {
        return interpretText.validProperty();
    }

    public ListProperty<PlainCitationParserChoice> interpretParsersProperty() {
        return interpretParsers;
    }

    public ObjectProperty<PlainCitationParserChoice> interpretParserProperty() {
        return interpretParser;
    }

    public ConstrainedStringProperty<ValidationMessage> bibtexTextProperty() {
        return bibtexText;
    }

    public ReadOnlyBooleanProperty bibtexTextValidatorProperty() {
        return bibtexText.validProperty();
    }

    private BibEntry withCoversDownloaded(BibEntry entry) {
        if (preferences.getPreviewPreferences().shouldDownloadCovers()) {
            bookCoverFetcher.downloadCoversForEntry(entry);
        }
        return entry;
    }

    private class WorkerLookupId extends Task<Optional<BibEntry>> {
        @Override
        protected Optional<BibEntry> call() throws FetcherException {
            String text = idText.getValue();
            if (StringUtil.isBlank(text)) {
                return Optional.empty();
            }

            CompositeIdFetcher fetcher = new CompositeIdFetcher(preferences.getImportFormatPreferences());
            return fetcher.performSearchById(text)
                          .map(entry -> withCoversDownloaded(entry));
        }
    }

    private class WorkerLookupTypedId extends Task<Optional<BibEntry>> {
        @Override
        protected Optional<BibEntry> call() throws FetcherException {
            String text = idText.getValue();
            if (StringUtil.isBlank(text)) {
                return Optional.empty();
            }

            boolean textValid = idText.isValid();
            if (!textValid) {
                return Optional.empty();
            }

            IdBasedFetcher fetcher = idFetcher.getValue();
            if (fetcher == null) {
                return Optional.empty();
            }

            return fetcher.performSearchById(text)
                          .map(entry -> withCoversDownloaded(entry));
        }
    }

    public void executeLookupIdentifier(boolean searchComposite) {
        executing.setValue(true);

        cancel();
        if (searchComposite) {
            idLookupWorker = new WorkerLookupId();
        } else {
            idLookupWorker = new WorkerLookupTypedId();
        }

        idLookupWorker.setOnFailed(_ -> {
            final Throwable exception = idLookupWorker.getException();
            final String exceptionMessage = exception.getMessage();
            final String textString = idText.getValue();
            final String fetcherName = idFetcher.getValue().getName();

            final String dialogTitle = Localization.lang("Failed to lookup identifier");

            if (exception instanceof FetcherClientException) {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "Bibliographic data could not be retrieved.\n" +
                                        "This is likely due to an issue with your input, or your network connection.\n" +
                                        "Check your provided identifier (and identifier type), and try again.\n" +
                                        "%0",
                                exceptionMessage));
            } else if (exception instanceof FetcherServerException) {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "Bibliographic data could not be retrieved.\n" +
                                        "This is likely due to an issue being experienced by the server.\n" +
                                        "Try again later.\n" +
                                        "%0",
                                exceptionMessage));
            } else {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "Bibliographic data could not be retrieved.\n" +
                                        "The following error was encountered:\n" +
                                        "%0",
                                exceptionMessage));
            }

            LOGGER.error("An exception occurred with the '{}' fetcher when resolving '{}'.", fetcherName, textString, exception);

            executing.set(false);
        });

        idLookupWorker.setOnSucceeded(_ -> {
            final Optional<BibEntry> result = idLookupWorker.getValue();

            if (result.isEmpty()) {
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Invalid result returned"),
                        Localization.lang(
                                "An unknown error has occurred.\n" +
                                        "This entry may need to be added manually."));
                executing.set(false);
                return;
            }

            final ImportHandler handler = new ImportHandler(
                    libraryTab.getBibDatabaseContext(),
                    preferences,
                    fileUpdateMonitor,
                    libraryTab.getUndoManager(),
                    stateManager,
                    dialogService,
                    taskExecutor);
            handler.importEntryWithDuplicateCheck(new TransferInformation(libraryTab.getBibDatabaseContext(), TransferMode.NONE), result.get());

            executedSuccessfully.set(true);
            executing.set(false);
        });

        taskExecutor.execute(idLookupWorker);
    }

    private class WorkerInterpretCitations extends Task<Optional<List<BibEntry>>> {
        @Override
        protected Optional<List<BibEntry>> call() throws FetcherException {
            final String text = interpretText.getValue();
            final boolean textValid = interpretText.isValid();
            final PlainCitationParserChoice parserChoice = interpretParser.getValue();

            if (text == null || !textValid || parserChoice == null) {
                return Optional.empty();
            }

            final PlainCitationParser parser;
            if (parserChoice == PlainCitationParserChoice.LLM) {
                parser = PlainCitationParserFactory.getLlmPlainCitationParser(
                        preferences.getImportFormatPreferences(),
                        preferences.getAiPreferences(),
                        aiService.getCurrentChatModel());
            } else {
                parser = PlainCitationParserFactory.getPlainCitationParser(
                        parserChoice,
                        preferences.getCitationKeyPatternPreferences(),
                        preferences.getGrobidPreferences(),
                        preferences.getImportFormatPreferences());
            }

            final List<BibEntry> entries = parser.parseMultiplePlainCitations(text);

            if (entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(entries);
        }
    }

    public void executeInterpretCitations() {
        executing.setValue(true);

        cancel();
        interpretWorker = new WorkerInterpretCitations();

        interpretWorker.setOnFailed(_ -> {
            final Throwable exception = interpretWorker.getException();
            final String exceptionMessage = exception.getMessage();
            final String parserName = interpretParser.getValue().getLocalizedName();
            LOGGER.error("An exception occurred with the '{}' parser.", parserName, exception);

            final String dialogTitle = Localization.lang("Failed to interpret citations");
            if (exception instanceof FetcherException) {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "Failed to interpret citations.\n" +
                                        "The following error was encountered:\n" +
                                        "%0",
                                exceptionMessage));
            } else {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "The following error occurred:\n" +
                                        "%0",
                                exceptionMessage));
            }

            executing.set(false);
        });

        interpretWorker.setOnSucceeded(_ -> {
            final Optional<List<BibEntry>> result = interpretWorker.getValue();

            if (result.isEmpty()) {
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Invalid result"),
                        Localization.lang(
                                "An unknown error has occurred.\n" +
                                        "Entries may need to be added manually."));
                LOGGER.error("An invalid result was returned when parsing citations.");
                executing.set(false);
                return;
            }

            final ImportHandler handler = new ImportHandler(
                    libraryTab.getBibDatabaseContext(),
                    preferences,
                    fileUpdateMonitor,
                    libraryTab.getUndoManager(),
                    stateManager,
                    dialogService,
                    taskExecutor);
            handler.importEntriesWithDuplicateCheck(null, result.get());

            executedSuccessfully.set(true);
            executing.set(false);
        });

        taskExecutor.execute(interpretWorker);
    }

    private class WorkerSpecifyBibtex extends Task<Optional<List<BibEntry>>> {
        @Override
        protected Optional<List<BibEntry>> call() throws ParseException {
            final String text = bibtexText.getValue();
            final boolean textValid = bibtexText.isValid();

            if (text == null || !textValid) {
                return Optional.empty();
            }

            final BibtexParser parser = new BibtexParser(preferences.getImportFormatPreferences());
            final List<BibEntry> entries = parser.parseEntries(text);

            if (entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(entries);
        }
    }

    public void executeSpecifyBibtex() {
        executing.setValue(true);

        cancel();
        bibtexWorker = new WorkerSpecifyBibtex();

        bibtexWorker.setOnFailed(_ -> {
            final Throwable exception = interpretWorker.getException();
            final String exceptionMessage = exception.getMessage();

            final String dialogTitle = Localization.lang("Failed to parse Bib(La)TeX");

            if (exception instanceof ParseException) {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "Failed to parse entries.\n" +
                                        "The following error was encountered:\n" +
                                        "%0",
                                exceptionMessage));
            } else {
                dialogService.showInformationDialogAndWait(
                        dialogTitle,
                        Localization.lang(
                                "The following error occurred:\n" +
                                        "%0",
                                exceptionMessage));
            }

            LOGGER.error("An exception occurred when parsing Bib(La)TeX entries.", exception);

            executing.set(false);
        });

        bibtexWorker.setOnSucceeded(_ -> {
            final Optional<List<BibEntry>> result = bibtexWorker.getValue();

            if (result.isEmpty()) {
                dialogService.showWarningDialogAndWait(
                        Localization.lang("Invalid result"),
                        Localization.lang(
                                "An unknown error has occurred.\n" +
                                        "Entries may need to be added manually."));
                LOGGER.error("An invalid result was returned when parsing Bib(La)TeX entries.");
                executing.set(false);
                return;
            }

            final ImportHandler handler = new ImportHandler(
                    libraryTab.getBibDatabaseContext(),
                    preferences,
                    fileUpdateMonitor,
                    libraryTab.getUndoManager(),
                    stateManager,
                    dialogService,
                    taskExecutor);
            handler.importEntriesWithDuplicateCheck(null, result.get());

            executedSuccessfully.set(true);
            executing.set(false);
        });

        taskExecutor.execute(bibtexWorker);
    }

    public void cancel() {
        if (idLookupWorker != null) {
            idLookupWorker.cancel();
        }
        if (interpretWorker != null) {
            interpretWorker.cancel();
        }
        if (bibtexWorker != null) {
            bibtexWorker.cancel();
        }
    }
}
