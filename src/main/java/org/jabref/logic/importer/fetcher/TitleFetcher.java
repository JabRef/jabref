package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

public class TitleFetcher implements IdBasedFetcher {

    private final ImportFormatPreferences preferences;

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
        Optional<DOI> doi = WebFetchers.getIdFetcherForIdentifier(DOI.class).findIdentifier(entry);
        if (!doi.isPresent()) {
            return Optional.empty();
        }

        DoiFetcher doiFetcher = new DoiFetcher(this.preferences);

        return doiFetcher.performSearchById(doi.get().getDOI());
    }

}
