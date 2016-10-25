package net.sf.jabref.gui.worker;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.SwingWorker;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.PreviewPanel;
import net.sf.jabref.logic.citationstyle.CitationStyle;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.search.MatchesHighlighter;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Generates a citation and updates the linked preview panel
 */
public class CitationStyleWorker extends SwingWorker<String, Void> {

    private static final Log LOGGER = LogFactory.getLog(CitationStyleWorker.class);

    private final PreviewPanel previewPanel;


    public CitationStyleWorker(PreviewPanel previewPanel, JEditorPane previewPane) {
        this.previewPanel = Objects.requireNonNull(previewPanel);
        Objects.requireNonNull(previewPane);

        Optional<BasePanel> basePanel = previewPanel.getBasePanel();
        if (basePanel.isPresent()){
            CitationStyle citationStyle = basePanel.get().getCitationStyleCache().getCitationStyle();
            previewPane.setText("<i>" + Localization.lang("Processing %0", Localization.lang("Citation Style")) +
                    ": " + citationStyle.getTitle() + " ..." + "</i>");
        }
        previewPane.revalidate();
    }

    @Override
    protected String doInBackground() throws Exception {
        Optional<BasePanel> basePanel = this.previewPanel.getBasePanel();
        BibEntry entry = this.previewPanel.getEntry();
        Optional<Pattern> highlightPattern = this.previewPanel.getHighlightPattern();

        String fieldText = "";
        if (entry != null && basePanel.isPresent()) {
            fieldText = basePanel.get().getCitationStyleCache().getCitationFor(entry);
            fieldText = MatchesHighlighter.highlightWordsWithHTML(fieldText, highlightPattern);
        }
        return fieldText;
    }

    @Override
    public void done() {
        if (this.isCancelled()) {
            return;
        }
        String text;
        try {
            text = this.get();
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error("Error while generating citation style", e);
            text = Localization.lang("Error while generating citation style");
        }

        previewPanel.setPreviewLabel(text);
    }

}
