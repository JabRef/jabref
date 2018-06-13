package org.jabref.gui.worker;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import javafx.scene.input.ClipboardContent;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.ClipBoardManager;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.layout.LayoutHelper;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.BibEntry;
import org.jabref.preferences.PreviewPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copies the selected entries and formats them with the selected citation style (or preview), then it is copied to the clipboard.
 * This worker cannot be reused.
 */
public class CitationStyleToClipboardWorker extends SwingWorker<List<String>, Void> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationStyleToClipboardWorker.class);

    private final BasePanel basePanel;
    private final List<BibEntry> selectedEntries;
    private final String style;
    private final String previewStyle;
    private final CitationStyleOutputFormat outputFormat;


    public CitationStyleToClipboardWorker(BasePanel basePanel, CitationStyleOutputFormat outputFormat) {
        this.basePanel = basePanel;
        this.selectedEntries = basePanel.getSelectedEntries();
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();
        this.style = previewPreferences.getPreviewCycle().get(previewPreferences.getPreviewCyclePosition());
        this.previewStyle = Globals.prefs.getPreviewPreferences().getPreviewStyle();
        this.outputFormat = outputFormat;

        basePanel.frame().setStatus(Localization.lang("Copying..."));
    }

    @Override
    protected List<String> doInBackground() throws Exception {
        // This worker stored the style as filename. The CSLAdapter and the CitationStyleCache store the source of the
        // style. Therefore, we extract the style source from the file.
        String styleSource = null;
        if (CitationStyle.isCitationStyleFile(style)) {
            styleSource = CitationStyle.createCitationStyleFromFile(style)
                    .filter(citationStyleFromFile -> !citationStyleFromFile.getSource().isEmpty())
                    .map(CitationStyle::getSource)
                    .orElse(null);
        }
        if (styleSource != null) {
            return CitationStyleGenerator.generateCitations(selectedEntries, styleSource, outputFormat);
        } else {
            StringReader sr = new StringReader(previewStyle.replace("__NEWLINE__", "\n"));
            LayoutFormatterPreferences layoutFormatterPreferences = Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
            Layout layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();

            List<String> citations = new ArrayList<>(selectedEntries.size());
            for (BibEntry entry : selectedEntries) {
                citations.add(layout.doLayout(entry, basePanel.getDatabase()));
            }
            return citations;
        }
    }

    /**
     * Generates a plain text string out of the preview and copies it additionally to the html to the clipboard
     * (WYSIWYG Editors use the HTML, plain text editors the text)
     */
    protected static String processPreview(List<String> citations) {
        return String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations);
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
        content.putHtml(result);
        return content;
    }

    @Override
    public void done() {
        try {
            List<String> citations = get();

            // if it's not a citation style take care of the preview
            if (!CitationStyle.isCitationStyleFile(style)) {
                Globals.clipboardManager.setHtmlContent(processPreview(citations));
            } else {
                // if it's generated by a citation style take care of each output format
                ClipboardContent content;
                switch (outputFormat) {
                    case HTML:
                        content = processHtml(citations);
                        break;
                    case RTF:
                        content = processRtf(citations);
                        break;
                    case XSL_FO:
                        content = processXslFo(citations);
                        break;
                    case ASCII_DOC:
                    case TEXT:
                        content = processText(citations);
                        break;
                    default:
                        LOGGER.warn("unknown output format: '" + outputFormat + "', processing it via the default.");
                        content = processText(citations);
                        break;
                }
                Globals.clipboardManager.setContent(content);
            }

            basePanel.frame().setStatus(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while copying citations to the clipboard", e);
        }
    }
}
