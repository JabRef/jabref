package org.jabref.logic.journals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.util.Pair;

import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.OS;

import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches journal information from the Elsevier Scopus API
 *
 * @see <a href="https://dev.elsevier.com/sc_apis.html">API documentation</a> for further details
 */
public class JournalInformationFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalInformationFetcher.class);
    private static final String API_URL = "https://api.elsevier.com/content/serial/title/issn/";
    private static final String API_KEY = "***";
    private static final Integer YEAR_INTERVAL = 10;

    public JournalInformation getJournalInformation(String issn) {
        JournalInformation journalInformation = null;

        try {
            URL urlForQuery = getURLForQuery(issn);
            InputStream stream = new URLDownload(urlForQuery).asInputStream();
            journalInformation = parseResponse(stream);
        } catch (URISyntaxException | MalformedURLException e) {
            LOGGER.error("Malformed Search URI.", e);
        } catch (IOException e) {
            LOGGER.error("An IOException occurred when fetching journal info.", e);
        }

        return journalInformation;
    }

    private JournalInformation parseResponse(InputStream inputStream) {
        String response = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining(OS.NEWLINE));
        JSONObject responseJsonObject = new JSONObject(response);
        String title = "";
        String publisher = "";
        String coverageStartYear = "";
        String coverageEndYear = "";
        String subjectArea = "";
        // TODO: extract from json
        String country = "United States";
        String categories = "Biology, Microbiology";
        String scimagoId = "A12345";
        String hIndex = "247";
        String issn = "[15230864, 15577716]";
        List<Pair<Integer, Double>> sjrArray = new ArrayList<>();
        List<Pair<Integer, Double>> snipArray = new ArrayList<>();

        if (responseJsonObject.has("serial-metadata-response")) {
            JSONObject serialMetadata = responseJsonObject.getJSONObject("serial-metadata-response");
            if (serialMetadata.has("entry")) {
                JSONArray entryArray = serialMetadata.getJSONArray("entry");
                if (!entryArray.isEmpty()) {
                    JSONObject entry = entryArray.getJSONObject(0);

                    title = entry.optString("dc:title", "");
                    publisher = entry.optString("dc:publisher", "");
                    coverageStartYear = entry.optString("coverageStartYear", "");
                    coverageEndYear = entry.optString("coverageEndYear", "");

                    if (entry.has("subject-area")) {
                        JSONArray subjectAreaArray = entry.getJSONArray("subject-area");
                        if (!subjectAreaArray.isEmpty()) {
                            JSONObject subjectAreaJsonObject = subjectAreaArray.getJSONObject(0);
                            subjectArea = subjectAreaJsonObject.optString("@abbrev", "") + "-" +
                                    subjectAreaJsonObject.optString("$", "");
                        }
                    }

                    if (entry.has("SNIPList") && entry.getJSONObject("SNIPList").has("SNIP")) {
                        JSONArray snipJsonArray = entry.getJSONObject("SNIPList").getJSONArray("SNIP");
                        snipArray = parseYearlyArray(snipJsonArray);
                    }

                    if (entry.has("SJRList") && entry.getJSONObject("SJRList").has("SJR")) {
                        JSONArray sjrJsonArray = entry.getJSONObject("SJRList").getJSONArray("SJR");
                        sjrArray = parseYearlyArray(sjrJsonArray);
                    }
                }
            }
        }

        return new JournalInformation(
                title,
                publisher,
                coverageStartYear,
                coverageEndYear,
                subjectArea,
                country,
                categories,
                scimagoId,
                hIndex,
                issn,
                sjrArray,
                snipArray
        );
    }

    private List<Pair<Integer, Double>> parseYearlyArray(JSONArray jsonArray) {
        List<Pair<Integer, Double>> parsedArray = new ArrayList<>();
        Set<Integer> yearSet = new HashSet<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);

            if (item.has("@year") && item.has("$")) {
                int year = item.getInt("@year");
                double value = item.getDouble("$");
                if (!yearSet.contains(year)) {
                    parsedArray.add(new Pair<>(year, value));
                    yearSet.add(year);
                }
            }
        }

        parsedArray.sort(Comparator.comparing(Pair::getKey));
        return parsedArray;
    }

    private URL getURLForQuery(String issn) throws URISyntaxException, MalformedURLException {
        int currentYear = Year.now().getValue();
        int startingYear = currentYear - YEAR_INTERVAL;
        String dateParameter = startingYear + "-" + currentYear;

        URIBuilder uriBuilder = new URIBuilder(API_URL + issn);
        uriBuilder.addParameter("apiKey", API_KEY);
        uriBuilder.addParameter("view", "citescore");
        uriBuilder.addParameter("date", dateParameter);

        return uriBuilder.build().toURL();
    }
}
