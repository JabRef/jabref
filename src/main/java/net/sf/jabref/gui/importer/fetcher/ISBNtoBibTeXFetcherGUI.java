package net.sf.jabref.gui.importer.fetcher;

import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.importer.fetcher.ISBNtoBibTeXFetcher;

public class ISBNtoBibTeXFetcherGUI extends ISBNtoBibTeXFetcher implements EntryFetcherGUI {

    @Override
    public HelpFiles getHelpPage() {
        return HelpFiles.FETCHER_ISBN_TO_BIBTEX;
    }

}
