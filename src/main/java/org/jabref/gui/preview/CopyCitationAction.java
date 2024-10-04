package org.jabref.gui.preview;

import java.io.IOException;
import java.util.List;

import javafx.scene.input.ClipboardContent;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.entry.BibEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the selected entries and formats them with the selected citation style (or preview), then it is copied to the clipboard. This worker cannot be reused.
 */
public class CopyCitationAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyCitationAction.class);

    private final List<BibEntry> selectedEntries;
    private final CitationStyleOutputFormat outputFormat;

    private final StateManager stateManager;
    private final DialogService dialogService;
    private final ClipBoardManager clipBoardManager;
    private final TaskExecutor taskExecutor;
    private final ClipboardContentGenerator clipboardContentGenerator;

    public CopyCitationAction(CitationStyleOutputFormat outputFormat,
                              DialogService dialogService,
                              StateManager stateManager,
                              ClipBoardManager clipBoardManager,
                              TaskExecutor taskExecutor,
                              GuiPreferences preferences,
                              JournalAbbreviationRepository abbreviationRepository) {
        this.outputFormat = outputFormat;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.selectedEntries = stateManager.getSelectedEntries();
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.clipboardContentGenerator = new ClipboardContentGenerator(preferences.getPreviewPreferences(), preferences.getLayoutFormatterPreferences(), abbreviationRepository);

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        BackgroundTask.wrap(this::generateCitations)
                      .onFailure(ex -> LOGGER.error("Error while copying citations to the clipboard", ex))
                      .onSuccess(this::setClipBoardContent)
                      .executeWith(taskExecutor);
    }

    private ClipboardContent generateCitations() throws IOException {
        return clipboardContentGenerator.generate(selectedEntries, outputFormat, stateManager.getActiveDatabase().get());
    }

    private void setClipBoardContent(ClipboardContent clipBoardContent) {
        clipBoardManager.setContent(clipBoardContent);
        dialogService.notify(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
    }
}
