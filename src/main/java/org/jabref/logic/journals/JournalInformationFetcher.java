package org.jabref.logic.journals;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.util.Pair;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches journal information from the JabRef Web API
 */
public class JournalInformationFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalInformationFetcher.class);
    private static final String API_URL = "https://mango-pebble-0224c3803-2067.westeurope.1.azurestaticapps.net/api";

    public JournalInformation getJournalInformation(String issn) {
        JournalInformation journalInformation = null;

        try {
            Integer issnInt = Integer.parseInt(issn);
            JSONObject postData = buildPostData(issnInt);

            HttpResponse<JsonNode> httpResponse = Unirest.post(API_URL)
                                                         .header("Content-Type", "application/json")
                                                         .body(postData)
                                                         .asJson();

            if (httpResponse.getBody() != null) {
                JSONObject responseJsonObject = httpResponse.getBody().getObject();
                journalInformation = parseResponse(responseJsonObject);
            }
        } catch (URISyntaxException | MalformedURLException e) {
            LOGGER.error("Malformed URI.", e);
        } catch (IOException e) {
            LOGGER.error("An IOException occurred when fetching journal info.", e);
        }

        return journalInformation;
    }

    private JournalInformation parseResponse(JSONObject responseJsonObject) throws IOException, URISyntaxException {
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

    private JSONObject buildPostData(Integer issn) {
        String query = """
                query GetJournalByIssn($issn: Int) {
                  journal(issn: $issn) {
                    id
                    name
                    issn
                    scimagoId
                    country
                    publisher
                    areas
                    categories
                    citationInfo {
                      year
                      docsThisYear
                      docsPrevious3Years
                      citableDocsPrevious3Years
                      citesOutgoing
                      citesOutgoingPerDoc
                      citesIncomingByRecentlyPublished
                      citesIncomingPerDocByRecentlyPublished
                      sjrIndex
                    }
                    hIndex
                  }
                }""";

        JSONObject postData = new JSONObject();
        postData.put("query", query);
        postData.put("operationName", "GetJournalByIssn");

        JSONObject variables = new JSONObject();
        variables.put("issn", issn);
        postData.put("variables", variables);

        return postData;
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
}
