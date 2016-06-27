package net.sf.jabref.gui.importer.fetcher;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.fetcher.DBLPFetcher;

public class DBLPFetcherGUI extends DBLPFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_DBLP;
    }
}
