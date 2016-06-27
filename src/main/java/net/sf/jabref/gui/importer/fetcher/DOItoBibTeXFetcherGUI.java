package net.sf.jabref.gui.importer.fetcher;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.fetcher.DOItoBibTeXFetcher;

public class DOItoBibTeXFetcherGUI extends DOItoBibTeXFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_DOI_TO_BIBTEX;
    }
}
