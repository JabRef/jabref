package net.sf.jabref.gui.importer.fetcher;

import java.io.IOException;
import java.util.List;

import javax.swing.JOptionPane;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.fetcher.ScienceDirectFetcher;
import net.sf.jabref.logic.l10n.Localization;

/**
 *
 * The current ScienceDirect fetcher implementation does no longer work
 *
 */
@Deprecated
public class ScienceDirectFetcherGUI extends ScienceDirectFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_SCIENCEDIRECT;
    }

    @Override
    public boolean processQuery(String query, ImportInspector dialog, OutputPrinter status) {
        setStopFetching(false);
        try {
            List<String> citations = getCitations(query);
            if (citations == null) {
                return false;
            }
            if (citations.isEmpty()) {
                status.showMessage(Localization.lang("No entries found for the search string '%0'", query),
                        Localization.lang("Search %0", getScienceDirect()), JOptionPane.INFORMATION_MESSAGE);
                return false;
            }
            return true;

        } catch (IOException e) {
            getLogger().warn("Communcation problems", e);
            status.showMessage(
                    Localization.lang("Error while fetching from %0", getScienceDirect()) + ": " + e.getMessage());
        }
        return false;
    }

}
