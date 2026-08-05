package org.jabref.logic.importer.fetcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import javafx.util.Pair;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetcher;
import org.jabref.logic.journals.JournalInformation;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.identifier.ISSN;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetches journal information from Crossref and OpenAlex.
// [impl->req~fetchers.journal-information~1]
public class JournalInformationFetcher implements WebFetcher {
    public static final String NAME = "Journal Information";
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalInformationFetcher.class);
    private static final String CROSSREF_API_URL = "https://api.crossref.org/journals/";
    private static final String OPENALEX_API_URL = "https://api.openalex.org/sources";

    @Override
    public String getName() {
        return NAME;
    }

    public Optional<JournalInformation> getJournalInformation(String issnString, String journalName) throws FetcherException {
        String cleanedIssn = getCleanedIssn(new ISSN(issnString));
        if (cleanedIssn.isEmpty() && journalName.isBlank()) {
            throw journalNotFoundException();
        }

        Optional<JournalIdentity> crossrefInformation = getCrossrefInformation(cleanedIssn);
        Optional<OpenAlexInformation> openAlexInformation = getOpenAlexInformation(cleanedIssn, journalName);
        if (crossrefInformation.isEmpty() && openAlexInformation.isEmpty()) {
            throw journalNotFoundException();
        }

        return Optional.of(createJournalInformation(crossrefInformation, openAlexInformation));
    }

    private static FetcherException journalNotFoundException() {
        return new FetcherException(Localization.lang("ISSN and/or journal name not found in catalog"));
    }

    private String getCleanedIssn(ISSN issn) {
        if (issn.isValidFormat() || issn.isCanBeCleaned()) {
            return issn.getCleanedISSN();
        }
        if (!issn.asString().isBlank()) {
            LOGGER.warn(Localization.lang("Incorrect ISSN format"));
        }
        return "";
    }

    private Optional<JournalIdentity> getCrossrefInformation(String issn) {
        if (issn.isBlank()) {
            return Optional.empty();
        }

        return getJson(CROSSREF_API_URL + issn)
                .map(response -> response.optJSONObject("message"))
                .flatMap(this::parseCrossrefInformation);
    }

    private Optional<OpenAlexInformation> getOpenAlexInformation(String issn, String journalName) {
        String url = issn.isBlank()
                     ? OPENALEX_API_URL + "?search=" + URLEncoder.encode(journalName, StandardCharsets.UTF_8) + "&per-page=1"
                     : OPENALEX_API_URL + "/issn:" + issn;
        Optional<OpenAlexInformation> journalInformation = getJson(url)
                .flatMap(this::getOpenAlexSource)
                .flatMap(this::parseOpenAlexInformation);
        if (!issn.isBlank()) {
            return journalInformation;
        }
        return journalInformation.filter(journal -> journal.title().equalsIgnoreCase(journalName.trim()));
    }

    private Optional<JSONObject> getJson(String url) {
        try {
            HttpResponse<JsonNode> response = Unirest.get(url)
                                                     .header("Accept", "application/json")
                                                     .asJson();
            if ((response.getStatus() < 200) || (response.getStatus() >= 300) || (response.getBody() == null)) {
                LOGGER.debug("Journal information request to {} returned HTTP {}", url, response.getStatus());
                return Optional.empty();
            }
            return Optional.ofNullable(response.getBody().getObject());
        } catch (UnirestException e) {
            LOGGER.debug("Could not retrieve journal information from {}", url, e);
            return Optional.empty();
        }
    }

    private Optional<JSONObject> getOpenAlexSource(JSONObject response) {
        JSONArray results = response.optJSONArray("results");
        if (results == null) {
            return Optional.of(response);
        }
        return Optional.ofNullable(results.optJSONObject(0));
    }

    private Optional<JournalIdentity> parseCrossrefInformation(JSONObject response) {
        if (response == null) {
            return Optional.empty();
        }
        return Optional.of(new JournalIdentity(
                response.optString("title"),
                response.optString("publisher"),
                getJoinedArray(response, "ISSN")
        ));
    }

    private Optional<OpenAlexInformation> parseOpenAlexInformation(JSONObject response) {
        if (response == null) {
            return Optional.empty();
        }
        JSONObject summaryStats = response.optJSONObject("summary_stats");
        return Optional.of(new OpenAlexInformation(
                response.optString("display_name"),
                response.optString("host_organization_name"),
                getJoinedArray(response, "issn"),
                (summaryStats != null) && summaryStats.has("h_index") ? Integer.toString(summaryStats.getInt("h_index")) : "",
                getYearlyValues(response.optJSONArray("counts_by_year"), "works_count"),
                getYearlyValues(response.optJSONArray("counts_by_year"), "cited_by_count")
        ));
    }

    private JournalInformation createJournalInformation(
            Optional<JournalIdentity> crossrefInformation,
            Optional<OpenAlexInformation> openAlexInformation) {
        JournalIdentity crossref = crossrefInformation.orElse(new JournalIdentity("", "", ""));
        OpenAlexInformation openAlex = openAlexInformation.orElse(new OpenAlexInformation("", "", "", "", List.of(), List.of()));
        return new JournalInformation(
                chooseValue(crossref.title(), openAlex.title()),
                chooseValue(crossref.publisher(), openAlex.publisher()),
                openAlex.hIndex(),
                chooseValue(crossref.issn(), openAlex.issn()),
                openAlex.worksCount(),
                openAlex.citedByCount()
        );
    }

    private static String chooseValue(String preferredValue, String fallbackValue) {
        return preferredValue.isBlank() ? fallbackValue : preferredValue;
    }

    private static String getJoinedArray(JSONObject response, String key) {
        JSONArray array = response.optJSONArray(key);
        if (array == null) {
            return "";
        }
        List<String> values = new ArrayList<>(array.length());
        for (int index = 0; index < array.length(); index++) {
            values.add(array.optString(index));
        }
        return String.join(", ", values);
    }

    private static List<Pair<Integer, Double>> getYearlyValues(JSONArray yearlyCounts, String key) {
        if (yearlyCounts == null) {
            return List.of();
        }
        List<Pair<Integer, Double>> values = new ArrayList<>(yearlyCounts.length());
        for (int index = 0; index < yearlyCounts.length(); index++) {
            JSONObject yearlyCount = yearlyCounts.optJSONObject(index);
            if ((yearlyCount != null) && yearlyCount.has("year") && yearlyCount.has(key)) {
                values.add(new Pair<>(yearlyCount.getInt("year"), yearlyCount.getDouble(key)));
            }
        }
        return values.stream().sorted(Comparator.comparing(Pair::getKey)).toList();
    }

    private record JournalIdentity(String title, String publisher, String issn) {
    }

    private record OpenAlexInformation(
            String title,
            String publisher,
            String issn,
            String hIndex,
            List<Pair<Integer, Double>> worksCount,
            List<Pair<Integer, Double>> citedByCount) {
    }
}
