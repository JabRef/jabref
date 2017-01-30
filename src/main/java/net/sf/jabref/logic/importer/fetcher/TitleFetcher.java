package net.sf.jabref.logic.importer.fetcher;

import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class TitleFetcher implements IdBasedFetcher {

    private ImportFormatPreferences preferences;

    public TitleFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "Title";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_TITLE;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        BibEntry entry = new BibEntry();
        entry.setField(FieldName.TITLE, identifier);
        Optional<DOI> doi = DOI.fromBibEntry(entry);
        if (!doi.isPresent()) {
            return Optional.empty();
        }

        DoiFetcher doiFetcher = new DoiFetcher(this.preferences);

        return doiFetcher.performSearchById(doi.get().getDOI());
    }

}
