package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.formatter.bibtexfields.ClearFormatter;
import net.sf.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.EntryBasedFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParseException;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class DoiFetcher implements IdBasedFetcher, EntryBasedFetcher {

    private final ImportFormatPreferences preferences;

    public DoiFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return "DOI";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOI;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DOI> doi = DOI.build(identifier);

        try {
            if (doi.isPresent()) {
                URL doiURL = new URL(doi.get().getURIAsASCIIString());

                // BibTeX data
                URLDownload download = new URLDownload(doiURL);
                download.addParameters("Accept", "application/x-bibtex");
                String bibtexString = download.downloadToString(StandardCharsets.UTF_8);

                // BibTeX entry
                Optional<BibEntry> fetchedEntry = BibtexParser.singleFromString(bibtexString, preferences);
                fetchedEntry.ifPresent(this::doPostCleanup);
                return fetchedEntry;
            } else {
                throw new FetcherException(Localization.lang("Invalid_DOI:_'%0'.", identifier));
            }
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Invalid URL"), e);
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
