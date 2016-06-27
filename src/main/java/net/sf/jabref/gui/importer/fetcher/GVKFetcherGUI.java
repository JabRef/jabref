package net.sf.jabref.gui.importer.fetcher;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.fetcher.GVKFetcher;

public class GVKFetcherGUI extends GVKFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_GVK;
    }
}
