package net.sf.jabref.logic.citationstyle;

import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.fieldeditors.HtmlTransferable;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.PreviewPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CitationStyleToClipboardWorker extends SwingWorker<List<String>, Void> {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleToClipboardWorker.class);
    private static final Pattern REMOVE_HTML = Pattern.compile("<(?!br)(?!BR).*?>");
    private static final Pattern WHITESPACE  = Pattern.compile("\\v+");
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
            if (CitationStyle.isCitationStyleFile(style)) {
                String result = "";
                switch (outputFormat) {
                    case XSLFO:
                        result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + OS.NEWLINE +
                                "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">" + OS.NEWLINE +
                                "   <fo:layout-master-set>" + OS.NEWLINE +
                                "      <fo:simple-page-master master-name=\"citations\">" + OS.NEWLINE +
                                "         <fo:region-body/>" + OS.NEWLINE +
                                "      </fo:simple-page-master>" + OS.NEWLINE +
                                "   </fo:layout-master-set>" + OS.NEWLINE +
                                "   <fo:page-sequence master-reference=\"citations\">" + OS.NEWLINE +
                                "      <fo:flow flow-name=\"xsl-region-body\">" + OS.NEWLINE + OS.NEWLINE;

                        for (String citation : citations) {
                            result += citation + OS.NEWLINE;
                        }

                        result +="      </fo:flow>" + OS.NEWLINE +
                                "   </fo:page-sequence>" + OS.NEWLINE +
                                "</fo:root>" + OS.NEWLINE;
                        break;
                    case HTML:
                        result = "<!DOCTYPE html>" + OS.NEWLINE +
                                "<html>" + OS.NEWLINE +
                                "   <head>" + OS.NEWLINE +
                                "      <meta charset=\\\"utf-8\\\">" + OS.NEWLINE +
                                "   </head>" + OS.NEWLINE +
                                "   <body>" + OS.NEWLINE + OS.NEWLINE;

                        for (String citation : citations) {
                            result += citation + OS.NEWLINE;
                        }

                        result +="   </body>" + OS.NEWLINE +
                                "</html>" + OS.NEWLINE;
                        break;
                    default:
                        for (String citation : citations) {
                            result = citation + OS.NEWLINE;
                        }
                        break;
                }

                new ClipBoardManager().setClipboardContents(result);

            } else {
                String html = "";
                String plain = "";
                for (String citation : citations) {
                    html += citation;

                    String tmp = WHITESPACE.matcher(citation).replaceAll("");
                    tmp = REMOVE_HTML.matcher(tmp).replaceAll("");
                    plain += HTML_NEWLINE.matcher(tmp).replaceAll(OS.NEWLINE) + OS.NEWLINE + OS.NEWLINE;
                }

                ClipboardOwner owner = (clipboard, content) -> {};
                HtmlTransferable transferable = new HtmlTransferable(html, plain);
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transferable, owner);
            }
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while copying citations to the clipboard", e);
        }
        basePanel.frame().setStatus(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
    }

}
