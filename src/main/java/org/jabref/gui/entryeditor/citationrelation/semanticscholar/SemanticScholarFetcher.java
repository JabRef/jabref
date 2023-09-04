package org.jabref.gui.entryeditor.citationrelation.semanticscholar;

import com.google.gson.Gson;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

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
                    .append("&limit=10");
            System.out.println(urlBuilder.toString());
            try {
                URL citationsUrl = URI.create(urlBuilder.toString()).toURL();

                URLDownload urlDownload = new URLDownload(citationsUrl);
                CitationsResponse citationsResponse = new Gson()
                        .fromJson(urlDownload.asString(), CitationsResponse.class);

                return citationsResponse.getData()
                        .stream().map(citationDataItem -> {
                    PaperDetails citingPaperDetails = citationDataItem.getCitingPaper();
                    if (citingPaperDetails != null) {
                        BibEntry bibEntry = new BibEntry();
                        bibEntry.setField(StandardField.TITLE, citingPaperDetails.getTitle());
                        bibEntry.setField(StandardField.YEAR, citingPaperDetails.getYear());

                        return bibEntry;
                    } else {
                        System.out.println("Warning !");
                        return null;
                    }
                }).toList();

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
                    .append("&limit=100");
            try {
                URL citationsUrl = URI.create(urlBuilder.toString()).toURL();

                URLDownload urlDownload = new URLDownload(citationsUrl);
                ReferencesResponse referencesResponse = new Gson()
                        .fromJson(urlDownload.asString(), ReferencesResponse.class);

                return referencesResponse.getData()
                        .stream().map(referenceDataItem -> {
                            PaperDetails citedPaperDetails = referenceDataItem.getCitedPaper();
                            if (citedPaperDetails != null) {
                                BibEntry bibEntry = new BibEntry();
                                bibEntry.setField(StandardField.TITLE, citedPaperDetails.getTitle());
                                bibEntry.setField(StandardField.YEAR, citedPaperDetails.getYear());

                                return bibEntry;
                            } else {
                                System.out.println("Warning !");
                                return null;
                            }
                        }).toList();

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
