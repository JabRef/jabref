package net.sf.jabref.gui.worker;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.exporter.RtfTransferable;
import net.sf.jabref.gui.fieldeditors.HtmlTransferable;
import net.sf.jabref.gui.fieldeditors.XmlTransferable;
import net.sf.jabref.logic.citationstyle.CitationStyle;
import net.sf.jabref.logic.citationstyle.CitationStyleGenerator;
import net.sf.jabref.logic.citationstyle.CitationStyleOutputFormat;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.PreviewPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Copies the selected entries and formats them with the selected citation style (or preview), then it is copied to the clipboard.
 * This worker cannot be reused.
 */
public class CitationStyleToClipboardWorker extends SwingWorker<List<String>, Void> {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleToClipboardWorker.class);
    private static final Pattern REMOVE_HTML = Pattern.compile("<(?!br)(?!BR).*?>");
    private static final Pattern WHITESPACE = Pattern.compile("(?m)^\\s|\\v+");
    private static final Pattern HTML_NEWLINE = Pattern.compile("<br>|<BR>");

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
        if (CitationStyle.isCitationStyleFile(style)) {
            return CitationStyleGenerator.generateCitations(selectedEntries, style, outputFormat);
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

    @Override
    public void done() {
        try {
            List<String> citations = get();

            // if it's not a citation style take care of the preview
            if (!CitationStyle.isCitationStyleFile(style)) {
                new ClipBoardManager().setTransferableClipboardContents(processPreview(citations));

            } else {
                // if it's generated by a citation style take care of each output format
                Transferable transferable;
                switch (outputFormat) {
                    case HTML:
                        transferable = processHtml(citations);
                        break;
                    case RTF:
                        transferable = processRtf(citations);
                        break;
                    case XSL_FO:
                        transferable = processXslFo(citations);
                        break;
                    case ASCII_DOC:
                    case TEXT:
                        transferable = processText(citations);
                        break;
                    default:
                        LOGGER.warn("unknown output format: '" + outputFormat + "', processing it via the default.");
                        transferable = processText(citations);
                        break;
                }
                new ClipBoardManager().setTransferableClipboardContents(transferable);
            }

            basePanel.frame().setStatus(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while copying citations to the clipboard", e);
        }
    }

    /**
     * Generates a plain text string out of the preview and copies it additionally to the html to the clipboard
     * (WYSIWYG Editors use the HTML, plain text editors the text)
     */
    protected static HtmlTransferable processPreview(List<String> citations) {
        String html = String.join(CitationStyleOutputFormat.HTML.getLineSeparator(), citations);
        String plain = "";
        for (String citation : citations) {
            String tmp = WHITESPACE.matcher(citation).replaceAll("");
            tmp = REMOVE_HTML.matcher(tmp).replaceAll("");
            plain += HTML_NEWLINE.matcher(tmp).replaceAll(OS.NEWLINE) + OS.NEWLINE;
        }
        return new HtmlTransferable(html, plain);
    }

    /**
     * Joins every citation with a newline and returns it.
     */
    protected static StringSelection processText(List<String> citations) {
        return new StringSelection(String.join(CitationStyleOutputFormat.TEXT.getLineSeparator(), citations));
    }

    /**
     * Converts the citations into the RTF format.
     */
    protected static RtfTransferable processRtf(List<String> citations) {
        String result = "{\\rtf" + OS.NEWLINE +
                String.join(CitationStyleOutputFormat.RTF.getLineSeparator(), citations) +
                "}";
        return new RtfTransferable(result);
    }

    /**
     * Inserts each citation into a XLSFO body and copies it to the clipboard
     */
    protected static XmlTransferable processXslFo(List<String> citations) {
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

        return new XmlTransferable(result);
    }

    /**
     * Inserts each citation into a HTML body and copies it to the clipboard
     */
    protected static HtmlTransferable processHtml(List<String> citations) {
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

        return new HtmlTransferable(result);
    }

}
