package net.sf.jabref.gui.importer.fetcher;

import java.util.Optional;

import javax.swing.JPanel;

import net.sf.jabref.Globals;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ImportInspector;
import net.sf.jabref.logic.importer.OutputPrinter;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fetcher.DOItoBibTeX;
import net.sf.jabref.model.entry.BibEntry;

public class DOItoBibTeXFetcher implements EntryFetcher {


    @Override
    public void stopFetching() {
        // not needed as the fetching is a single HTTP GET
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        ParserResult parserResult = new ParserResult();
        Optional<BibEntry> entry = DOItoBibTeX.getEntryFromDOI(query, parserResult,
                ImportFormatPreferences.fromPreferences(Globals.prefs));
        if (parserResult.hasWarnings()) {
            status.showMessage(parserResult.getErrorMessage());
        }
        entry.ifPresent(e -> inspector.addEntry(e));

        return entry.isPresent();
    }

    @Override
    public String getTitle() {
        return "DOI to BibTeX";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOI_TO_BIBTEX;
    }

    @Override
    public JPanel getOptionsPanel() {
        // no additional options available
        return null;
    }

}
