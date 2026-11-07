package org.jabref.logic.importer.fetcher;

import java.util.Optional;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;

public class TitleFetcher implements IdBasedFetcher {

    private final ImportFormatPreferences preferences;
    private final ImporterPreferences importerPreferences;

    public TitleFetcher(ImportFormatPreferences preferences) {
        this(preferences, ImporterPreferences.getDefault());
    }

    public TitleFetcher(ImportFormatPreferences preferences, ImporterPreferences importerPreferences) {
        this.preferences = preferences;
        this.importerPreferences = importerPreferences;
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

        BibEntry entry = new BibEntry().withField(StandardField.TITLE, identifier);

        Optional<DOI> doi = WebFetchers.getIdFetcherForIdentifier(DOI.class, importerPreferences).findIdentifier(entry);
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        DoiFetcher doiFetcher = new DoiFetcher(this.preferences);
        return doiFetcher.performSearchById(doi.get().asString());
    }
}
