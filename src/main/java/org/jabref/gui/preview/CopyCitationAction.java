package org.jabref.gui.preview;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.scene.input.ClipboardContent;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.StateManager;
import org.jabref.gui.actions.ActionHelper;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreferencesService;

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
    private final PreferencesService preferencesService;
    private final JournalAbbreviationRepository abbreviationRepository;

    public CopyCitationAction(CitationStyleOutputFormat outputFormat,
                              DialogService dialogService,
                              StateManager stateManager,
                              ClipBoardManager clipBoardManager,
                              TaskExecutor taskExecutor,
                              PreferencesService preferencesService,
                              JournalAbbreviationRepository abbreviationRepository) {
        this.outputFormat = outputFormat;
        this.dialogService = dialogService;
        this.stateManager = stateManager;
        this.selectedEntries = stateManager.getSelectedEntries();
        this.clipBoardManager = clipBoardManager;
        this.taskExecutor = taskExecutor;
        this.preferencesService = preferencesService;
        this.abbreviationRepository = abbreviationRepository;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        BackgroundTask.wrap(this::generateCitations)
                      .onFailure(ex -> LOGGER.error("Error while copying citations to the clipboard", ex))
                      .onSuccess(this::setClipBoardContent)
                      .executeWith(taskExecutor);
    }

    private List<String> generateCitations() throws IOException {
        // This worker stored the style as filename. The CSLAdapter and the CitationStyleCache store the source of the
        // style. Therefore, we extract the style source from the file.
        String styleSource = null;
        PreviewLayout previewLayout = preferencesService.getPreviewPreferences().getSelectedPreviewLayout();

        if (previewLayout instanceof CitationStylePreviewLayout citationStyleLayout) {
            styleSource = citationStyleLayout.getSource();
        }

        if (styleSource != null) {
            return CitationStyleGenerator.generateCitations(selectedEntries, styleSource, outputFormat, stateManager.getActiveDatabase().get(), Globals.entryTypesManager);
        } else {
            return generateTextBasedPreviewLayoutCitations();
        }
    }

    private List<String> generateTextBasedPreviewLayoutCitations() throws IOException {
        if (stateManager.getActiveDatabase().isEmpty()) {
            return Collections.emptyList();
        }

        TextBasedPreviewLayout customPreviewLayout = preferencesService.getPreviewPreferences().getCustomPreviewLayout();
        StringReader customLayoutReader = new StringReader(customPreviewLayout.getText().replace("__NEWLINE__", "\n"));
        Layout layout = new LayoutHelper(customLayoutReader, preferencesService.getLayoutFormatterPreferences(), abbreviationRepository)
                .getLayoutFromText();

        List<String> citations = new ArrayList<>(selectedEntries.size());
        for (BibEntry entry : selectedEntries) {
            citations.add(layout.doLayout(entry, stateManager.getActiveDatabase().get().getDatabase()));
        }
        return citations;
    }

    /**
     * Generates a plain text string out of the preview and copies it additionally to the html to the clipboard (WYSIWYG Editors use the HTML, plain text editors the text)
     */
    protected static ClipboardContent processPreview(List<String> citations) {
        ClipboardContent content = new ClipboardContent();
        content.putHtml(String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations));
        content.putString(String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations));
        return content;
    }

    /**
     * Joins every citation with a newline and returns it.
     */
    protected static ClipboardContent processText(List<String> citations) {
        ClipboardContent content = new ClipboardContent();
        content.putString(String.join(CitationStyleOutputFormat.TEXT.getLineSeparator(), citations));
        return content;
    }

    /**
     * Inserts each citation into a HTML body and copies it to the clipboard
     */
    protected static ClipboardContent processHtml(List<String> citations) {
        String result = "<!DOCTYPE html>" + OS.NEWLINE +
                "<html>" + OS.NEWLINE +
                "   <head>" + OS.NEWLINE +
                "      <meta charset=\"utf-8\">" + OS.NEWLINE +
                "   </head>" + OS.NEWLINE +
                "   <body>" + OS.NEWLINE + OS.NEWLINE;

        result += String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations);
        result += OS.NEWLINE +
                "   </body>" + OS.NEWLINE +
                "</html>" + OS.NEWLINE;

        ClipboardContent content = new ClipboardContent();
        content.putString(result);
        content.putHtml(result);
        return content;
    }

    private void setClipBoardContent(List<String> citations) {
        PreviewLayout previewLayout = preferencesService.getPreviewPreferences().getSelectedPreviewLayout();

        // if it's not a citation style take care of the preview
        if (!(previewLayout instanceof CitationStylePreviewLayout)) {
            clipBoardManager.setContent(processPreview(citations));
        } else {
            // if it's generated by a citation style take care of each output format
            ClipboardContent content;
            switch (outputFormat) {
                case HTML -> content = processHtml(citations);
                case TEXT -> content = processText(citations);
                default -> {
                    LOGGER.warn("unknown output format: '" + outputFormat + "', processing it via the default.");
                    content = processText(citations);
                }
            }
            clipBoardManager.setContent(content);
        }

        dialogService.notify(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
    }
}
