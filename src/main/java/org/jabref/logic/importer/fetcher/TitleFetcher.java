package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.strings.StringUtil;

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
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_TITLE);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        if (StringUtil.isBlank(identifier)) {
            return Optional.empty();
        }

        BibEntry entry = new BibEntry();
        entry.setField(StandardField.TITLE, identifier);

        Optional<DOI> doi = WebFetchers.getIdFetcherForIdentifier(DOI.class).findIdentifier(entry);
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        DoiFetcher doiFetcher = new DoiFetcher(this.preferences);
        return doiFetcher.performSearchById(doi.get().getDOI());
    }
}
