package net.sf.jabref.logic.citationstyle;

import java.io.StringReader;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;
import net.sf.jabref.logic.layout.LayoutFormatterPreferences;
import net.sf.jabref.logic.layout.LayoutHelper;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.preferences.PreviewPreferences;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class CitationStyleToClipboardWorker extends SwingWorker<String, Void> {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleToClipboardWorker.class);

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
    protected String doInBackground() throws Exception {
        if (CitationStyle.isCitationStyleFile(style)) {
            return CitationStyleGenerator.generateCitations(selectedEntries, style, outputFormat);
        } else {
            StringReader sr = new StringReader(previewStyle.replace("__NEWLINE__", "\n"));
            LayoutFormatterPreferences layoutFormatterPreferences = Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader);
            Layout layout = new LayoutHelper(sr, layoutFormatterPreferences).getLayoutFromText();

            StringBuilder sb = new StringBuilder();
            for (BibEntry entry : selectedEntries) {
                sb.append(layout.doLayout(entry, basePanel.getDatabase()));
            }
            return sb.toString();
        }
    }

    @Override
    public void done() {
        try {
            String result = get();
            switch (outputFormat) {
                case FO:
                    result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + OS.NEWLINE +
                            "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">" + OS.NEWLINE +
                            "   <fo:layout-master-set>" + OS.NEWLINE +
                            "      <fo:simple-page-master master-name=\"citations\">" + OS.NEWLINE +
                            "         <fo:region-body/>" + OS.NEWLINE +
                            "      </fo:simple-page-master>" + OS.NEWLINE +
                            "   </fo:layout-master-set>" + OS.NEWLINE +
                            "   <fo:page-sequence master-reference=\"citations\">" + OS.NEWLINE +
                            "      <fo:flow flow-name=\"xsl-region-body\">" + OS.NEWLINE +

                            OS.NEWLINE + result + OS.NEWLINE +

                            "      </fo:flow>" + OS.NEWLINE +
                            "   </fo:page-sequence>" + OS.NEWLINE +
                            "</fo:root>" + OS.NEWLINE;
                    break;
                case HTML:
                    result = "<!DOCTYPE html>" + OS.NEWLINE +
                            "<html>" + OS.NEWLINE +
                            "   <head>" + OS.NEWLINE +
                            "      <meta charset=\\\"utf-8\\\">" + OS.NEWLINE +
                            "   </head>" + OS.NEWLINE +
                            "   <body>" + OS.NEWLINE +

                            OS.NEWLINE + result + get() + OS.NEWLINE +

                            "   </body>" + OS.NEWLINE +
                            "</html>" + OS.NEWLINE;
                    break;
            }

            new ClipBoardManager().setClipboardContents(result);
            basePanel.frame().setStatus(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while copying citations to the clipboard", e);
        }
    }

}
