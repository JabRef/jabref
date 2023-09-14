package org.jabref.gui.entryeditor.citationrelationtab.semanticscholar;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;

import com.google.gson.Gson;

public class SemanticScholarFetcher implements CitationFetcher {
    private static final String SEMANTIC_SCHOLAR_API = "https://api.semanticscholar.org/graph/v1/";

    @Override
    public List<BibEntry> searchCitedBy(BibEntry entry) throws FetcherException {
        if (entry.getDOI().isPresent()) {
            StringBuilder urlBuilder = new StringBuilder(SEMANTIC_SCHOLAR_API)
                    .append("paper/")
                    .append("DOI:").append(entry.getDOI().get().getDOI())
                    .append("/citations")
                    .append("?fields=").append("title,authors,year,citationCount,referenceCount")
                    .append("&limit=1000");

            try {
                URL citationsUrl = URI.create(urlBuilder.toString()).toURL();

                URLDownload urlDownload = new URLDownload(citationsUrl);
                CitationsResponse citationsResponse = new Gson()
                        .fromJson(urlDownload.asString(), CitationsResponse.class);

                return citationsResponse.getData()
                            .stream().filter(citationDataItem -> citationDataItem.getCitingPaper() != null)
                            .map(citationDataItem -> citationDataItem.getCitingPaper().toBibEntry()).toList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new ArrayList<>();
    }

    @Override
    public List<BibEntry> searchCiting(BibEntry entry) {
        if (entry.getDOI().isPresent()) {
            StringBuilder urlBuilder = new StringBuilder(SEMANTIC_SCHOLAR_API)
                    .append("paper/")
                    .append("DOI:").append(entry.getDOI().get().getDOI())
                    .append("/references")
                    .append("?fields=").append("title,authors,year,citationCount,referenceCount")
                    .append("&limit=1000");
            try {
                URL referencesUrl = URI.create(urlBuilder.toString()).toURL();

                URLDownload urlDownload = new URLDownload(referencesUrl);
                ReferencesResponse referencesResponse = new Gson()
                        .fromJson(urlDownload.asString(), ReferencesResponse.class);

                return referencesResponse.getData()
                             .stream()
                             .filter(citationDataItem -> citationDataItem.getCitedPaper() != null)
                             .map(referenceDataItem -> referenceDataItem.getCitedPaper().toBibEntry()).toList();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new ArrayList<>();
    }

    @Override
    public String getName() {
        return "Semantic Scholar Citations Fetcher";
    }
}
