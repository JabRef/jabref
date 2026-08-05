package org.jabref.logic.importer.fetcher;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javafx.util.Pair;

import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ImporterPreferences;
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
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Fetches journal information from Crossref and OpenAlex.
// [impl->req~fetchers.journal-information~1]
public class JournalInformationFetcher implements WebFetcher {
    public static final String NAME = "Journal Information";
    private static final Logger LOGGER = LoggerFactory.getLogger(JournalInformationFetcher.class);
    private static final String CROSSREF_API_URL = "https://api.crossref.org/journals/";
    private static final String OPENALEX_API_URL = "https://api.openalex.org/sources";
    private static final String CROSSREF_PROVIDER = "Crossref";
    private static final FetcherRateLimiter CROSSREF_RATE_LIMITER = FetcherRateLimiter.ofRequestsPerSecond(CROSSREF_PROVIDER, 50.0);
    private static final Pattern JOURNAL_NAME_SEPARATOR_PATTERN = Pattern.compile("[\\p{Punct}\\s]+");
    private final ImporterPreferences importerPreferences;

    public JournalInformationFetcher(ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    public JournalInformationFetcher() {
        this(ImporterPreferences.getDefault());
    }

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

    private Optional<JournalIdentity> getCrossrefInformation(String issn) throws FetcherException {
        if (issn.isBlank()) {
            return Optional.empty();
        }

        CROSSREF_RATE_LIMITER.acquire(issn);
        return getJson(CROSSREF_API_URL + issn, CROSSREF_PROVIDER)
                .map(response -> response.optJSONObject("message"))
                .flatMap(this::parseCrossrefInformation);
    }

    private Optional<OpenAlexInformation> getOpenAlexInformation(String issn, String journalName) throws FetcherException {
        Optional<JSONObject> response = getJson(getOpenAlexUrl(issn, journalName), OpenAlex.FETCHER_NAME);
        if (!issn.isBlank()) {
            return response.flatMap(this::parseOpenAlexInformation);
        }
        return response.stream()
                       .flatMap(this::getOpenAlexSources)
                       .map(this::parseOpenAlexInformation)
                       .flatMap(Optional::stream)
                       .filter(journal -> haveMatchingNames(journal.title(), journalName))
                       .findFirst();
    }

    String getOpenAlexUrl(String issn, String journalName) {
        String url = issn.isBlank()
                     ? OPENALEX_API_URL + "?search=" + URLEncoder.encode(journalName, StandardCharsets.UTF_8) + "&per-page=10"
                     : OPENALEX_API_URL + "/issn:" + issn;
        return importerPreferences.getApiKey(OpenAlex.FETCHER_NAME)
                                  .map(apiKey -> url + (issn.isBlank() ? "&" : "?") + "api_key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8))
                                  .orElse(url);
    }

    private Optional<JSONObject> getJson(String url, String provider) throws FetcherException {
        try {
            HttpResponse<JsonNode> response = Unirest.get(url)
                                                     .header("Accept", "application/json")
                                                     .asJson();
            if (response.getStatus() == 404) {
                LOGGER.debug("Journal information request to {} returned HTTP {}", provider, response.getStatus());
                return Optional.empty();
            }
            if ((response.getStatus() < 200) || (response.getStatus() >= 300)) {
                throw new FetcherException("%s returned HTTP %d".formatted(provider, response.getStatus()));
            }
            if (response.getBody() == null) {
                throw new FetcherException("%s returned an empty response".formatted(provider));
            }
            return Optional.ofNullable(response.getBody().getObject());
        } catch (UnirestException e) {
            throw new FetcherException("Could not retrieve journal information from " + provider, e);
        }
    }

    private Stream<JSONObject> getOpenAlexSources(JSONObject response) {
        JSONArray results = response.optJSONArray("results");
        if (results == null) {
            return Stream.of(response);
        }
        return IntStream.range(0, results.length())
                        .mapToObj(index -> Optional.ofNullable(results.optJSONObject(index)))
                        .flatMap(Optional::stream);
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

    static boolean haveMatchingNames(String firstName, String secondName) {
        return normalizeJournalName(firstName).equals(normalizeJournalName(secondName));
    }

    private static String normalizeJournalName(String journalName) {
        return JOURNAL_NAME_SEPARATOR_PATTERN.matcher(journalName)
                                             .replaceAll("")
                                             .toLowerCase(Locale.ROOT);
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

    @NullMarked
    private record JournalIdentity(String title, String publisher, String issn) {
    }

    @NullMarked
    private record OpenAlexInformation(
            String title,
            String publisher,
            String issn,
            String hIndex,
            List<Pair<Integer, Double>> worksCount,
            List<Pair<Integer, Double>> citedByCount) {
    }
}
