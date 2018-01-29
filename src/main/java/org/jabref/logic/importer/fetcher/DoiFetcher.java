package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.cleanup.FieldFormatterCleanup;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.DummyFileUpdateMonitor;

public class DoiFetcher implements IdBasedFetcher, EntryBasedFetcher {
    public static final String NAME = "DOI";

    private final ImportFormatPreferences preferences;

    public DoiFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return DoiFetcher.NAME;
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOI;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DOI> doi = DOI.parse(identifier);

        try {
            if (doi.isPresent()) {
                URL doiURL = new URL(doi.get().getURIAsASCIIString());

                // BibTeX data
                URLDownload download = new URLDownload(doiURL);
                download.addHeader("Accept", "application/x-bibtex");
                String bibtexString = download.asString();

                // BibTeX entry
                Optional<BibEntry> fetchedEntry = BibtexParser.singleFromString(bibtexString, preferences, new DummyFileUpdateMonitor());
                fetchedEntry.ifPresent(this::doPostCleanup);
                return fetchedEntry;
            } else {
                throw new FetcherException(Localization.lang("Invalid DOI: '%0'.", identifier));
            }
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Connection error"), e);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX entry", e);
        }
    }

    private void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(FieldName.PAGES, new NormalizePagesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.URL, new ClearFormatter()).cleanup(entry);
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<BibEntry> bibEntry = performSearchById(entry.getField(FieldName.DOI).orElse(""));
        List<BibEntry> list = new ArrayList<>();
        bibEntry.ifPresent(list::add);
        return list;
    }
}
