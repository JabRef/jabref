package org.jabref.logic.importer.fetcher;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

    public CitationRelationFetcher(SearchType searchType, ListView<BibEntry> listView) {
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
            System.out.println(BASIC_URL + doi);
            JSONArray json = readJsonFromUrl(BASIC_URL + doi);
            System.out.println(json.toString());
            String[] items = json.getJSONObject(0).getString("reference").split("; ");
            for (String item : items) {
                System.out.println(item);
                if (!doi.equals(item) && !item.equals("")) {
                    DoiFetcher doiFetcher = new DoiFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
                    try {
                        doiFetcher.performSearchById(item).ifPresent(list::add);
                    } catch (FetcherException fetcherException) {

                    }
                    /*try {
                        JSONArray article = readJsonFromUrl(BASIC_URL + item);
                        BibEntry newEntry = new BibEntry();
                        newEntry.setField(StandardField.TITLE, article.getJSONObject(0).getString("title"));
                        newEntry.setField(StandardField.AUTHOR, article.getJSONObject(0).getString("author"));
                        newEntry.setField(StandardField.DOI, article.getJSONObject(0).getString("doi"));
                        list.add(newEntry);
                    } catch (JSONException jsonException) {

                    }*/
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
            System.out.println(BASIC_URL + doi);
            JSONArray json = readJsonFromUrl(BASIC_URL + doi);
            System.out.println(json.toString());
            String[] items = json.getJSONObject(0).getString("citation").split("; ");
            for (String item : items) {
                System.out.println(item);
                if (!doi.equals(item) && !item.equals("")) {
                    DoiFetcher doiFetcher = new DoiFetcher(JabRefPreferences.getInstance().getImportFormatPreferences());
                    try {
                        doiFetcher.performSearchById(item).ifPresent(list::add);
                    } catch (FetcherException fetcherException) {

                    }
                    /*try {
                        JSONArray article = readJsonFromUrl(BASIC_URL + item);
                        BibEntry newEntry = new BibEntry();
                        newEntry.setField(StandardField.TITLE, article.getJSONObject(0).getString("title"));
                        newEntry.setField(StandardField.AUTHOR, article.getJSONObject(0).getString("author"));
                        newEntry.setField(StandardField.DOI, article.getJSONObject(0).getString("doi"));
                        list.add(newEntry);
                    } catch (JSONException jsonException) {

                    }*/
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
