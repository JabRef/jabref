package net.sf.jabref.gui.actions;

import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.util.List;
import java.util.stream.Collectors;


import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.fieldeditors.HtmlTransferable;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.citationstyle.CitationStyleCache;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;

import static net.sf.jabref.logic.util.OS.NEWLINE;

public class CopyPreviewOfSelectedEntriesToClipboardAction extends AbstractWorker {

    private JabRefFrame frame;

    /**
     * written by run() and read by update()
     */
    private String message;


    public CopyPreviewOfSelectedEntriesToClipboardAction(JabRefFrame frame) {
        this.frame = frame;
    }

    @Override
    public void run() {
        BasePanel panel = frame.getCurrentBasePanel();
        if (panel == null) {
            return;
        }

        List<BibEntry> entries = panel.getSelectedEntries();
        if (entries.isEmpty()) {
            message = Localization.lang("This operation requires one or more entries to be selected.");
            getCallBack().update();
            return;
        }

        frame.output(Localization.lang("Exporting" ) + "...");

        CitationStyleCache citationStyleCache = panel.getCitationStyleCache();

        String htmlHead = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>" + NEWLINE;

        String allCitationsHtml = entries.stream().map(entry -> citationStyleCache.getCitationFor(entry)).collect(Collectors.joining("",
                htmlHead, "</body></html>"));

        // strip HTML tags
        String allCitationsTxt = allCitationsHtml.replaceAll("\\<.*?>","");
        // line endings are mixed somehow - normalize
        allCitationsTxt = allCitationsTxt.replaceAll("\r", "");
        // trim leading white spaces and replace couble new lines
        allCitationsTxt = allCitationsTxt.replaceAll("\n[ \t]*", "\n").replaceAll("\n+", "\n");

        ClipboardOwner owner = (clipboard, content) -> {
            // Do nothing
        };
        HtmlTransferable transferable = new HtmlTransferable(allCitationsHtml, allCitationsTxt);
        Toolkit.getDefaultToolkit().getSystemClipboard()
                .setContents(transferable, owner);
        message = Localization.lang("Entries exported to clipboard") + ": " + entries.size();
    }

    @Override
    public void update() {
        if (message != null) {
            frame.output(message);
        }
    }

}
