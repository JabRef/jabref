package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.ClearFormatter;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.importer.util.MediaTypes;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.util.DummyFileUpdateMonitor;
import org.jabref.model.util.OptionalUtil;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DoiFetcher implements IdBasedFetcher, EntryBasedFetcher {
    public static final String NAME = "DOI";

    private static final Logger LOGGER = LoggerFactory.getLogger(DoiFetcher.class);

    private final ImportFormatPreferences preferences;

    public DoiFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    @Override
    public String getName() {
        return DoiFetcher.NAME;
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_DOI);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DOI> doi = DOI.parse(identifier);
        try {
            if (doi.isPresent()) {
                Optional<BibEntry> fetchedEntry;

                // mEDRA does not return a parsable bibtex string
                if (getAgency(doi.get()).isPresent() && "medra".equalsIgnoreCase(getAgency(doi.get()).get())) {
                    return new Medra().performSearchById(identifier);
                }

                URL doiURL = new URL(doi.get().getURIAsASCIIString());

                // BibTeX data
                URLDownload download = getUrlDownload(doiURL);
                download.addHeader("Accept", MediaTypes.APPLICATION_BIBTEX);
                String bibtexString = download.asString();

                // BibTeX entry
                fetchedEntry = BibtexParser.singleFromString(bibtexString, preferences, new DummyFileUpdateMonitor());
                fetchedEntry.ifPresent(this::doPostCleanup);

                return fetchedEntry;
            } else {
                throw new FetcherException(Localization.lang("Invalid DOI: '%0'.", identifier));
            }
        } catch (IOException e) {
            throw new FetcherException(Localization.lang("Connection error"), e);
        } catch (ParseException e) {
            throw new FetcherException("Could not parse BibTeX entry", e);
        } catch (JSONException e) {
            throw new FetcherException("Could not retrieve Registration Agency", e);
        }
    }

    private void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(StandardField.URL, new ClearFormatter()).cleanup(entry);
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> doi = entry.getField(StandardField.DOI);
        if (doi.isPresent()) {
            return OptionalUtil.toList(performSearchById(doi.get()));
        } else {
            return Collections.emptyList();
        }
    }

    /**
     * Returns registration agency. Optional.empty() if no agency is found.
     *
     * @param doi the DOI to be searched
     */
    public Optional<String> getAgency(DOI doi) throws IOException {
        Optional<String> agency = Optional.empty();
        try {
            URLDownload download = getUrlDownload(new URL(DOI.AGENCY_RESOLVER + "/" + doi.getDOI()));
            JSONObject response = new JSONArray(download.asString()).getJSONObject(0);
            if (response != null) {
                agency = Optional.ofNullable(response.optString("RA"));
            }
        } catch (JSONException e) {
            LOGGER.error("Cannot parse agency fetcher repsonse to JSON");
            return Optional.empty();
        }

        return agency;
    }
}
