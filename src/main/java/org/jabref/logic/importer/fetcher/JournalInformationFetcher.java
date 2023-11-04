package org.jabref.logic.importer.fetcher;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import javafx.util.Pair;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.journals.JournalInformation;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.ISSN;

import kong.unirest.HttpResponse;
import kong.unirest.JsonNode;
import kong.unirest.Unirest;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONException;
import kong.unirest.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches journal information from the JabRef Web API
 */
public class JournalInformationFetcher implements WebFetcher {
    public static final String NAME = "Journal Information";
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalInformationFetcher.class);
    // Uses JabRef Online APIs
    private static final String API_URL = "https://jabref.org/api";
    private static final Pattern QUOTES_BRACKET_PATTERN = Pattern.compile("[\"\\[\\]]");

    @Override
    public String getName() {
        return JournalInformationFetcher.NAME;
    }

    public Optional<JournalInformation> getJournalInformation(String issnString, String journalName) throws FetcherException {
        ISSN issn = new ISSN(issnString);
        String cleanedISSN = "";

        if (issn.isValidFormat() || issn.isCanBeCleaned()) {
            cleanedISSN = issn.getCleanedISSN();
        } else {
            LOGGER.warn(Localization.lang("Incorrect ISSN format"));
        }

        Optional<JournalInformation> journalInformationOptional = Optional.empty();

        JSONObject postData = buildPostData(cleanedISSN, journalName);

        HttpResponse<JsonNode> httpResponse = Unirest.post(API_URL)
                                                     .header("Content-Type", "application/json")
                                                     .body(postData)
                                                     .asJson();

        if (httpResponse.getBody() != null) {
            JSONObject responseJsonObject = httpResponse.getBody().getObject();
            journalInformationOptional = Optional.of(parseResponse(responseJsonObject));
        }

        return journalInformationOptional;
    }

    private JournalInformation parseResponse(JSONObject responseJsonObject) throws FetcherException {
        String title = "";
        String publisher = "";
        String coverageStartYear = "";
        String coverageEndYear = "";
        String subjectArea = "";
        String country = "";
        String categories = "";
        String scimagoId = "";
        String hIndex = "";
        String issn = "";
        List<Pair<Integer, Double>> sjrArray = new ArrayList<>();
        List<Pair<Integer, Double>> snipArray = new ArrayList<>();
        List<Pair<Integer, Double>> docsThisYear = new ArrayList<>();
        List<Pair<Integer, Double>> docsPrevious3Years = new ArrayList<>();
        List<Pair<Integer, Double>> citableDocsPrevious3Years = new ArrayList<>();
        List<Pair<Integer, Double>> citesOutgoing = new ArrayList<>();
        List<Pair<Integer, Double>> citesOutgoingPerDoc = new ArrayList<>();
        List<Pair<Integer, Double>> citesIncomingByRecentlyPublished = new ArrayList<>();
        List<Pair<Integer, Double>> citesIncomingPerDocByRecentlyPublished = new ArrayList<>();

        try {
            if (responseJsonObject.has("errors")) {
                JSONArray errors = responseJsonObject.optJSONArray("errors");
                if (errors != null && !errors.isEmpty()) {
                    JSONObject error = errors.getJSONObject(0);
                    String errorMessage = error.optString("message", "");
                    LOGGER.error("Error accessing catalog: {}", errorMessage);
                }
                throw new FetcherException(Localization.lang("Error accessing catalog"));
            }

            if (responseJsonObject.has("data")) {
                JSONObject data = responseJsonObject.getJSONObject("data");
                if (data.has("journal") && data.get("journal") != null) {
                    JSONObject journalData = data.getJSONObject("journal");

                    title = journalData.optString("name", "");
                    publisher = journalData.optString("publisher", "");
                    coverageStartYear = journalData.optString("coverageStartYear", "");
                    coverageEndYear = journalData.optString("coverageEndYear", "");
                    scimagoId = journalData.optString("scimagoId", "");
                    country = journalData.optString("country", "");
                    issn = getConcatenatedString(journalData, "issn");
                    subjectArea = getConcatenatedString(journalData, "areas");
                    categories = getConcatenatedString(journalData, "categories");
                    hIndex = journalData.optString("hIndex", "");

                    JSONArray citationInfo = journalData.optJSONArray("citationInfo");
                    if (citationInfo != null) {
                        docsThisYear = parseCitationInfo(citationInfo, "docsThisYear");
                        docsPrevious3Years = parseCitationInfo(citationInfo, "docsPrevious3Years");
                        citableDocsPrevious3Years = parseCitationInfo(citationInfo, "citableDocsPrevious3Years");
                        citesOutgoing = parseCitationInfo(citationInfo, "citesOutgoing");
                        citesOutgoingPerDoc = parseCitationInfo(citationInfo, "citesOutgoingPerDoc");
                        citesIncomingByRecentlyPublished = parseCitationInfo(citationInfo, "citesIncomingByRecentlyPublished");
                        citesIncomingPerDocByRecentlyPublished = parseCitationInfo(citationInfo, "citesIncomingPerDocByRecentlyPublished");
                        sjrArray = parseCitationInfo(citationInfo, "sjrIndex");
                        snipArray = parseCitationInfo(citationInfo, "snipIndex");
                    }
                } else {
                    throw new FetcherException(Localization.lang("ISSN and/or journal name not found in catalog"));
                }
            } else {
                throw new FetcherException(Localization.lang("ISSN and/or journal name not found in catalog"));
            }
        } catch (JSONException e) {
            throw new FetcherException(Localization.lang("Parsing error"), e);
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
                snipArray,
                docsThisYear,
                docsPrevious3Years,
                citableDocsPrevious3Years,
                citesOutgoing,
                citesOutgoingPerDoc,
                citesIncomingByRecentlyPublished,
                citesIncomingPerDocByRecentlyPublished
        );
    }

    private static String getConcatenatedString(JSONObject jsonObject, String key) {
        JSONArray jsonArray = jsonObject.optJSONArray(key);
        if (jsonArray != null) {
            return QUOTES_BRACKET_PATTERN.matcher(jsonArray.join(", ")).replaceAll("");
        } else {
            return "";
        }
    }

    private JSONObject buildPostData(String issn, String journalName) {
        String query = """
                query GetJournal($issn: String, $name: String) {
                  journal(issn: $issn, name: $name) {
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
        postData.put("operationName", "GetJournal");

        JSONObject variables = new JSONObject();
        variables.put("issn", issn);
        variables.put("name", journalName);
        postData.put("variables", variables);

        return postData;
    }

    private List<Pair<Integer, Double>> parseCitationInfo(JSONArray jsonArray, String key) {
        List<Pair<Integer, Double>> parsedArray = new ArrayList<>();
        Set<Integer> yearSet = new HashSet<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject item = jsonArray.getJSONObject(i);

            if (item.has("year") && item.has(key)) {
                int year = item.getInt("year");
                double value = item.getDouble(key);
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
