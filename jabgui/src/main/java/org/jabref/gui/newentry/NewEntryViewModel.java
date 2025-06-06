package org.jabref.gui.newentry;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;

import org.jabref.gui.DialogService;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.CompositeIdFetcher;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.plaincitation.GrobidPlainCitationParser;
import org.jabref.logic.importer.plaincitation.LlmPlainCitationParser;
import org.jabref.logic.importer.plaincitation.PlainCitationParser;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.plaincitation.RuleBasedPlainCitationParser;
import org.jabref.logic.importer.plaincitation.SeveralPlainCitationParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.FileUpdateMonitor;

import de.saxsys.mvvmfx.utils.validation.FunctionBasedValidator;
import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewEntryViewModel {

    private static final Logger LOGGER = LoggerFactory.getLogger(NewEntryViewModel.class);

    private final GuiPreferences preferences;
    private final LibraryTab libraryTab;
    private final DialogService dialogService;
    private final StateManager stateManager;
    private final UiTaskExecutor taskExecutor;
    private final AiService aiService;
    private final FileUpdateMonitor fileUpdateMonitor;

    private final BooleanProperty executing;
    private final BooleanProperty executedSuccessfully;

    private final StringProperty idText;
    private final Validator idTextValidator;
    private final ListProperty<IdBasedFetcher> idFetchers;
    private final ObjectProperty<IdBasedFetcher> idFetcher;
    private final Validator idFetcherValidator;
    private Task<Optional<BibEntry>> idLookupWorker;

    private final StringProperty interpretText;
    private final Validator interpretTextValidator;
    private final ListProperty<PlainCitationParserChoice> interpretParsers;
    private final ObjectProperty<PlainCitationParserChoice> interpretParser;
    private Task<Optional<List<BibEntry>>> interpretWorker;

    private final StringProperty bibtexText;
    private final Validator bibtexTextValidator;
    private Task<Optional<List<BibEntry>>> bibtexWorker;

    public NewEntryViewModel(GuiPreferences preferences,
                                    LibraryTab libraryTab,
                                    DialogService dialogService,
                                    StateManager stateManager,
                                    UiTaskExecutor taskExecutor,
                                    AiService aiService,
                                    FileUpdateMonitor fileUpdateMonitor) {
        this.preferences = preferences;
        this.libraryTab = libraryTab;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.taskExecutor = taskExecutor;
        this.aiService = aiService;
        this.fileUpdateMonitor = fileUpdateMonitor;

        executing = new SimpleBooleanProperty(false);
        executedSuccessfully = new SimpleBooleanProperty(false);

        idText = new SimpleStringProperty();
        idTextValidator = new FunctionBasedValidator<>(
            idText,
            StringUtil::isNotBlank,
            ValidationMessage.error(Localization.lang("You must specify an identifier.")));
        idFetchers = new SimpleListProperty<>(FXCollections.observableArrayList());
        idFetchers.addAll(WebFetchers.getIdBasedFetchers(preferences.getImportFormatPreferences(), preferences.getImporterPreferences()));
        idFetcher = new SimpleObjectProperty<>();
        idFetcherValidator = new FunctionBasedValidator<>(
            idFetcher,
            Objects::nonNull,
            ValidationMessage.error(Localization.lang("You must select an identifier type.")));
        idLookupWorker = null;

        interpretText = new SimpleStringProperty();
        interpretTextValidator = new FunctionBasedValidator<>(
            interpretText,
            StringUtil::isNotBlank,
            ValidationMessage.error(Localization.lang("You must specify one (or more) citations.")));
        interpretParsers = new SimpleListProperty<>(FXCollections.observableArrayList());
        interpretParsers.addAll(PlainCitationParserChoice.values());
        interpretParser = new SimpleObjectProperty<>();
        interpretWorker = null;

        bibtexText = new SimpleStringProperty();
        bibtexTextValidator = new FunctionBasedValidator<>(
            bibtexText,
            StringUtil::isNotBlank,
            ValidationMessage.error(Localization.lang("You must specify a Bib(La)TeX source.")));
        bibtexWorker = null;
    }

    public ReadOnlyBooleanProperty executingProperty() {
        return executing;
    }

    public ReadOnlyBooleanProperty executedSuccessfullyProperty() {
        return executedSuccessfully;
    }

    public StringProperty idTextProperty() {
        return idText;
    }

    public ReadOnlyBooleanProperty idTextValidatorProperty() {
        return idTextValidator.getValidationStatus().validProperty();
    }

    public ListProperty<IdBasedFetcher> idFetchersProperty() {
        return idFetchers;
    }

    public ObjectProperty<IdBasedFetcher> idFetcherProperty() {
        return idFetcher;
    }

    public ReadOnlyBooleanProperty idFetcherValidatorProperty() {
        return idFetcherValidator.getValidationStatus().validProperty();
    }

    public StringProperty interpretTextProperty() {
        return interpretText;
    }

    public ReadOnlyBooleanProperty interpretTextValidatorProperty() {
        return interpretTextValidator.getValidationStatus().validProperty();
    }

    public ListProperty<PlainCitationParserChoice> interpretParsersProperty() {
        return interpretParsers;
    }

    public ObjectProperty<PlainCitationParserChoice> interpretParserProperty() {
        return interpretParser;
    }

    public StringProperty bibtexTextProperty() {
        return bibtexText;
    }

    public ReadOnlyBooleanProperty bibtexTextValidatorProperty() {
        return bibtexTextValidator.getValidationStatus().validProperty();
    }

    private class WorkerLookupId extends Task<Optional<BibEntry>> {
        @Override
        protected Optional<BibEntry> call() throws FetcherException {
            final String text = idText.getValue();
            final CompositeIdFetcher fetcher = new CompositeIdFetcher(preferences.getImportFormatPreferences());

            if (text == null || text.isEmpty()) {
                return Optional.empty();
            }

            return fetcher.performSearchById(text);
        }
    }

    private class WorkerLookupTypedId extends Task<Optional<BibEntry>> {
        @Override
        protected Optional<BibEntry> call() throws FetcherException {
            final String text = idText.getValue();
            final boolean textValid = idTextValidator.getValidationStatus().isValid();
            final IdBasedFetcher fetcher = idFetcher.getValue();

            if (text == null || !textValid || fetcher == null) {
                return Optional.empty();
            }

            return fetcher.performSearchById(text);
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

            LOGGER.error("An exception occurred with the '{}' fetcher when resolving '{}': '{}'.", fetcherName, textString, exception);

            executing.set(false);
        });

        idLookupWorker.setOnSucceeded(event -> {
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
            handler.importEntryWithDuplicateCheck(libraryTab.getBibDatabaseContext(), result.get());

            executedSuccessfully.set(true);
            executing.set(false);
        });

        taskExecutor.execute(idLookupWorker);
    }

    private class WorkerInterpretCitations extends Task<Optional<List<BibEntry>>> {
        @Override
        protected Optional<List<BibEntry>> call() throws FetcherException {
            final String text = interpretText.getValue();
            final boolean textValid = interpretTextValidator.getValidationStatus().isValid();
            final PlainCitationParserChoice parserChoice = interpretParser.getValue();

            if (text == null || !textValid || parserChoice == null) {
                return Optional.empty();
            }

            final PlainCitationParser parser = switch (parserChoice) {
                case PlainCitationParserChoice.RULE_BASED -> new RuleBasedPlainCitationParser();
                case PlainCitationParserChoice.GROBID -> new GrobidPlainCitationParser(preferences.getGrobidPreferences(), preferences.getImportFormatPreferences());
                case PlainCitationParserChoice.LLM -> new LlmPlainCitationParser(aiService.getTemplatesService(), preferences.getImportFormatPreferences(), aiService.getChatLanguageModel());
            };

            final SeveralPlainCitationParser setParser = new SeveralPlainCitationParser(parser);
            final List<BibEntry> entries = setParser.parseSeveralPlainCitations(text);

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

        interpretWorker.setOnFailed(event -> {
            final Throwable exception = interpretWorker.getException();
            final String exceptionMessage = exception.getMessage();
            final String parserName = interpretParser.getValue().getLocalizedName();

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

            LOGGER.error("An exception occurred with the '{}' parser: '{}'.", parserName, exception);

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
            handler.importEntriesWithDuplicateCheck(libraryTab.getBibDatabaseContext(), result.get());

            executedSuccessfully.set(true);
            executing.set(false);
        });

        taskExecutor.execute(interpretWorker);
    }

    private class WorkerSpecifyBibtex extends Task<Optional<List<BibEntry>>> {
        @Override
        protected Optional<List<BibEntry>> call() throws ParseException {
            final String text = bibtexText.getValue();
            final boolean textValid = bibtexTextValidator.getValidationStatus().isValid();

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

            final String dialogTitle = Localization.lang("Failed to parse Bib(La)Tex");

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

            LOGGER.error("An exception occurred when parsing Bib(La)Tex entries: '{}'.", exception);

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
                LOGGER.error("An invalid result was returned when parsing Bib(La)Tex entries.");
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
            handler.importEntriesWithDuplicateCheck(libraryTab.getBibDatabaseContext(), result.get());

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
