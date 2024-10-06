package org.jabref.gui.plaincitationparser;

import java.util.List;

import javax.swing.undo.UndoManager;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.externalfiles.ImportHandler;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.ai.AiService;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.plaincitation.GrobidPlainCitationParser;
import org.jabref.logic.importer.plaincitation.LlmPlainCitationParser;
import org.jabref.logic.importer.plaincitation.PlainCitationParserChoice;
import org.jabref.logic.importer.plaincitation.RuleBasedPlainCitationParser;
import org.jabref.logic.importer.plaincitation.SeveralPlainCitationParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.util.FileUpdateMonitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlainCitationParserViewModel {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlainCitationParserViewModel.class);

    private final DialogService dialogService;
    private final AiService aiService;
    private final CliPreferences preferences;
    private final TaskExecutor taskExecutor;
    private final ImportHandler importHandler;

    private final StringProperty inputTextProperty = new SimpleStringProperty("");
    private final ListProperty<PlainCitationParserChoice> plainCitationParsers
            = new SimpleListProperty<>(FXCollections.observableArrayList(List.of(PlainCitationParserChoice.RULE_BASED)));
    private final ObjectProperty<PlainCitationParserChoice> parserChoice;

    public PlainCitationParserViewModel(BibDatabaseContext bibdatabaseContext,
                                        DialogService dialogService,
                                        AiService aiService,
                                        GuiPreferences preferences,
                                        FileUpdateMonitor fileUpdateMonitor,
                                        TaskExecutor taskExecutor,
                                        UndoManager undoManager,
                                        StateManager stateManager
    ) {
        this.dialogService = dialogService;
        this.aiService = aiService;
        this.preferences = preferences;
        this.taskExecutor = taskExecutor;
        this.importHandler = new ImportHandler(
                bibdatabaseContext,
                preferences,
                fileUpdateMonitor,
                undoManager,
                stateManager,
                dialogService,
                taskExecutor);

        if (preferences.getGrobidPreferences().isGrobidEnabled()) {
            plainCitationParsers.add(PlainCitationParserChoice.GROBID);
        }

        if (preferences.getAiPreferences().getEnableAi()) {
            plainCitationParsers.add(PlainCitationParserChoice.LLM);
        }

        this.parserChoice = new SimpleObjectProperty<>(preferences.getImporterPreferences().defaultPlainCitationParserProperty().get());
    }

    public void startParsing() {
        BackgroundTask
                .wrap(() -> new SeveralPlainCitationParser(
                                switch (parserChoice.get()) {
                                    case RULE_BASED ->
                                            new RuleBasedPlainCitationParser();
                                    case GROBID ->
                                            new GrobidPlainCitationParser(preferences.getGrobidPreferences(), preferences.getImportFormatPreferences());
                                    case LLM ->
                                            new LlmPlainCitationParser(preferences.getImportFormatPreferences(), aiService.getChatLanguageModel());
                                }
                        ).parseSeveralPlainCitations(inputTextProperty.getValue()))
                .onRunning(() -> dialogService.notify(Localization.lang("Your text is being parsed...")))
                .onFailure(e -> {
                    if (e instanceof FetcherException) {
                        String msg = Localization.lang("Unable to parse plain citations. Detailed information: %0",
                                e.getMessage());
                        dialogService.notify(msg);
                    } else {
                        LOGGER.warn("Missing exception handling.", e);
                    }
                })
                .onSuccess(parsedEntries -> {
                    dialogService.notify(Localization.lang("%0 entries were parsed from your query.", String.valueOf(parsedEntries.size())));
                    importHandler.importEntries(parsedEntries);
                }).executeWith(taskExecutor);
    }

    public StringProperty inputTextProperty() {
        return this.inputTextProperty;
    }

    public ListProperty<PlainCitationParserChoice> plainCitationParsers() {
        return this.plainCitationParsers;
    }

    public ObjectProperty<PlainCitationParserChoice> parserChoice() {
        return this.parserChoice;
    }
}
