package org.jabref.logic.importer.fetcher;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.control.ListView;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.preferences.JabRefPreferences;

public class CitationRelationFetcher implements EntryBasedFetcher {

    private final SearchType searchType;
    private static final String BASIC_URL = "https://opencitations.net/index/api/v1/metadata/";

    public enum SearchType {
        CITING,
        CITEDBY
    }

    public CitationRelationFetcher(SearchType searchType, ListView<String> listView) {
        this.searchType = searchType;
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        String doi = entry.getField(StandardField.DOI).orElse("");

        if (searchType.equals(SearchType.CITING)) {
            return getCiting(doi);
        } else if (searchType.equals(SearchType.CITEDBY)) {
            return getCitedBy(doi);
        } else {
            return null;
        }
    }

    private List<BibEntry> getCiting(String doi) {
        List<BibEntry> list = new ArrayList<>();
        try {
            JSONArray json = readJsonFromUrl(BASIC_URL + doi);
            System.out.println(json.toString());
            for (int i = 0; i < json.length(); i++) {
                String citingDoi = json.getJSONObject(i).getString("cited");
                System.out.println(citingDoi.substring(8));
                if (!doi.equals(citingDoi.substring(8))) {
                    DoiFetcher doiFetcher = new DoiFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
                    try {
                        doiFetcher.performSearchById(citingDoi.substring(8)).ifPresent(list::add);
                    } catch (FetcherException fetcherException) {

                    }
                }
            }
            System.out.println("Finished.");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private List<BibEntry> getCitedBy(String doi) {
        List<BibEntry> list = new ArrayList<>();
        try {
            JSONArray json = readJsonFromUrl(BASIC_URL + doi);
            System.out.println(json.toString());
            for (int i = 0; i < json.length(); i++) {
                String citedByDoi = json.getJSONObject(i).getString("citing");
                System.out.println(citedByDoi.substring(8));
                if (!doi.equals(citedByDoi.substring(8))) {
                    DoiFetcher doiFetcher = new DoiFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
                    try {
                        doiFetcher.performSearchById(citedByDoi.substring(8)).ifPresent(list::add);
                    } catch (FetcherException fetcherException) {

                    }
                }
            }
            System.out.println("Finished.");
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    private static String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
        try (InputStream is = new URL(url).openStream()) {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            return new JSONArray(jsonText);
        }
    }

    @Override
    public String getName() {
        return null;
    }

}
