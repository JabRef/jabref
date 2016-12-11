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
            String result;
            switch (outputFormat) {
                case FO:
                    result = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<fo:root xmlns:fo=\"http://www.w3.org/1999/XSL/Format\">\n" +
                            "   <fo:layout-master-set>\n" +
                            "      <fo:simple-page-master master-name=\"citations\">\n" +
                            "         <fo:region-body/>\n" +
                            "      </fo:simple-page-master>\n" +
                            "   </fo:layout-master-set>\n" +
                            "   <fo:page-sequence master-reference=\"citations\">\n" +
                            "      <fo:flow flow-name=\"xsl-region-body\">\n" +
                            get() + "\n" +
                            "      </fo:flow>\n" +
                            "   </fo:page-sequence>\n" +
                            "</fo:root>";
                    break;
                default:
                    result = get();
                    break;
            }

            new ClipBoardManager().setClipboardContents(result);
            basePanel.frame().setStatus(Localization.lang("Copied %0 citations.", String.valueOf(selectedEntries.size())));
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while copying citations to the clipboard", e);
        }
    }

}
