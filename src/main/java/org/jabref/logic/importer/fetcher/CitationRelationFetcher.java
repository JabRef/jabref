package org.jabref.logic.importer.fetcher;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

public class CitationRelationFetcher implements EntryBasedFetcher {

    private String title;
    private String description;
    private static final String BASIC_URL = "https://opencitations.net/index/api/v1/citations/";

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        System.out.println("Searching...");

        List<BibEntry> list = new ArrayList<>();
        try {
            JSONArray json = readJsonFromUrl(BASIC_URL + entry.getField(StandardField.DOI).orElseThrow());
            System.out.println(json.toString());
            for (int i = 0; i < json.length(); i++) {
                String citedDoi = json.getJSONObject(i).getString("citing");
                //System.out.println(citedDoi);

                BibEntry newEntry = new BibEntry();
                newEntry.setField(StandardField.DOI, citedDoi.substring(8));
                list.add(newEntry);
            }
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
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            String jsonText = readAll(rd);
            JSONArray json = new JSONArray(jsonText);
            return json;
        } finally {
            is.close();
        }
    }

    @Override
    public String getName() {
        return null;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

}
