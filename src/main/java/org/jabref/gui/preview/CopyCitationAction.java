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
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the selected entries and formats them with the selected citation style (or preview), then it is copied to the clipboard. This worker cannot be reused.
 */
public class CopyCitationAction extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CopyCitationAction.class);

    private final List<BibEntry> selectedEntries;
    private final StateManager stateManager;
    private final PreviewLayout style;
    private final String previewStyle;
    private final CitationStyleOutputFormat outputFormat;
    private final DialogService dialogService;
    private final ClipBoardManager clipBoardManager;

    public CopyCitationAction(CitationStyleOutputFormat outputFormat, DialogService dialogService, StateManager stateManager, ClipBoardManager clipBoardManager, PreviewPreferences previewPreferences) {
        this.selectedEntries = stateManager.getSelectedEntries();
        this.style = previewPreferences.getCurrentPreviewStyle();
        this.previewStyle = previewPreferences.getPreviewStyle();
        this.outputFormat = outputFormat;
        this.clipBoardManager = clipBoardManager;
        this.dialogService = dialogService;
        this.stateManager = stateManager;

        this.executable.bind(ActionHelper.needsEntriesSelected(stateManager));
    }

    @Override
    public void execute() {
        BackgroundTask.wrap(this::generateCitations)
                      .onFailure(ex -> LOGGER.error("Error while copying citations to the clipboard", ex))
                      .onSuccess(this::setClipBoardContent)
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private List<String> generateCitations() throws IOException {
        // This worker stored the style as filename. The CSLAdapter and the CitationStyleCache store the source of the
        // style. Therefore, we extract the style source from the file.
        String styleSource = null;
        if (style instanceof CitationStylePreviewLayout) {
            styleSource = ((CitationStylePreviewLayout) style).getSource();
        }
        if (styleSource != null) {
            return CitationStyleGenerator.generateCitations(selectedEntries, styleSource, outputFormat);
        } else {
            if (stateManager.getActiveDatabase().isEmpty()) {
                return Collections.emptyList();
            }

            StringReader sr = new StringReader(previewStyle.replace("__NEWLINE__", "\n"));
            LayoutFormatterPreferences layoutFormatterPreferences = Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationRepository);
            Layout layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();

            List<String> citations = new ArrayList<>(selectedEntries.size());
            for (BibEntry entry : selectedEntries) {
                citations.add(layout.doLayout(entry, stateManager.getActiveDatabase().get().getDatabase()));
            }
            return citations;
        }
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
     * Converts the citations into the RTF format.
     */
    protected static ClipboardContent processRtf(List<String> citations) {
        String result = "{\\rtf" + OS.NEWLINE +
                String.join(CitationStyleOutputFormat.RTF.getLineSeparator(), citations) +
                "}";
        ClipboardContent content = new ClipboardContent();
        content.putString(result);
        content.putRtf(result);
        return content;
    }

    /**
     * Inserts each citation into a XLSFO body and copies it to the clipboard
     */
    protected static ClipboardContent processXslFo(List<String> citations) {
        String result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + OS.NEWLINE +
                "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">" + OS.NEWLINE +
                "   <fo:layout-master-set>" + OS.NEWLINE +
                "      <fo:simple-page-master master-name=\"citations\">" + OS.NEWLINE +
                "         <fo:region-body/>" + OS.NEWLINE +
                "      </fo:simple-page-master>" + OS.NEWLINE +
                "   </fo:layout-master-set>" + OS.NEWLINE +
                "   <fo:page-sequence master-reference=\"citations\">" + OS.NEWLINE +
                "      <fo:flow flow-name=\"xsl-region-body\">" + OS.NEWLINE + OS.NEWLINE;

        result += String.join(CitationStyleOutputFormat.XSL_FO.getLineSeparator(), citations);

        result += OS.NEWLINE +
                "      </fo:flow>" + OS.NEWLINE +
                "   </fo:page-sequence>" + OS.NEWLINE +
                "</fo:root>" + OS.NEWLINE;

        ClipboardContent content = new ClipboardContent();
        content.putString(result);
        content.put(ClipBoardManager.XML, result);
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
        // if it's not a citation style take care of the preview
        if (!(style instanceof CitationStylePreviewLayout)) {
            clipBoardManager.setContent(processPreview(citations));
        } else {
            // if it's generated by a citation style take care of each output format
            ClipboardContent content;
            switch (outputFormat) {
                case HTML -> content = processHtml(citations);
                case RTF -> content = processRtf(citations);
                case XSL_FO -> content = processXslFo(citations);
                case ASCII_DOC, TEXT -> content = processText(citations);
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
