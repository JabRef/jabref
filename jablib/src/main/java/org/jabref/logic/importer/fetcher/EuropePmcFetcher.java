package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.cleanup.FieldFormatterCleanup;
import org.jabref.logic.formatter.bibtexfields.NormalizePagesFormatter;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.IdBasedParserFetcher;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.SearchBasedParserFetcher;
import org.jabref.logic.importer.fetcher.transformers.DefaultSearchQueryTransformer;
import org.jabref.logic.importer.util.JsonReader;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.search.query.BaseQueryNode;

import kong.unirest.core.json.JSONArray;
import kong.unirest.core.json.JSONException;
import kong.unirest.core.json.JSONObject;
import org.apache.hc.core5.net.URIBuilder;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EuropePmcFetcher implements IdBasedParserFetcher, SearchBasedParserFetcher, FulltextFetcher, CustomizableKeyFetcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EuropePmcFetcher.class);
    private static final String WILEY_TDM_API_URL = "https://api.wiley.com/onlinelibrary/tdm/v1/articles/";

    @Nullable
    private final ImporterPreferences importerPreferences;

    public EuropePmcFetcher(@Nullable ImporterPreferences importerPreferences) {
        this.importerPreferences = importerPreferences;
    }

    @Override
    public URL getUrlForIdentifier(String identifier) throws URISyntaxException, MalformedURLException {
        return new URI("https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=" + identifier + "&resultType=core&format=json").toURL();
    }

    @Override
    public URL getURLForQuery(BaseQueryNode queryList) throws URISyntaxException, MalformedURLException {
        DefaultSearchQueryTransformer transformer = new DefaultSearchQueryTransformer();
        String query = transformer.transformSearchQuery(queryList).orElse("");
        URIBuilder uriBuilder = new URIBuilder("https://www.ebi.ac.uk/europepmc/webservices/rest/search");
        // Europe PMC expects a Lucene-like query in the 'query' parameter
        uriBuilder.addParameter("query", query);
        uriBuilder.addParameter("resultType", "core");
        uriBuilder.addParameter("format", "json");
        return uriBuilder.build().toURL();
    }

    @Override
    public Parser getParser() {
        return inputStream -> {
            JSONObject response = JsonReader.toJsonObject(inputStream);
            if (response.isEmpty()) {
                return List.of();
            }
            return List.of(jsonItemToBibEntry(response));
        };
    }

    private BibEntry jsonItemToBibEntry(JSONObject item) throws ParseException {
        try {
            JSONObject result = item.getJSONObject("resultList").getJSONArray("result").getJSONObject(0);

            LOGGER.debug(result.toString(2));

            // Determine entry type from pubTypeList if available
            EntryType entryType = determineEntryType(result);

            BibEntry entry = new BibEntry(entryType);

            entry.withField(StandardField.TITLE, result.optString("title"))
                 .withField(StandardField.ABSTRACT, result.optString("abstractText"))
                 .withField(StandardField.YEAR, result.optString("pubYear"))
                 .withField(StandardField.PAGES, result.optString("pageInfo"));

            String doi = result.optString("doi");
            entry.withField(StandardField.DOI, doi)
                 .withField(StandardField.PMID, result.optString("pmid"));

            // Prefer fulltext URLs (e.g., PDF) when available, otherwise fall back to DOI or PubMed page
            String bestUrl = extractBestFullTextUrl(result).orElseGet(() -> {
                if (result.has("pmid")) {
                    return "https://pubmed.ncbi.nlm.nih.gov/" + result.optString("pmid") + "/";
                }
                if (doi != null && !doi.isBlank()) {
                    return "https://doi.org/" + doi;
                }
                return null;
            });
            if (bestUrl != null && !bestUrl.isBlank()) {
                entry.setField(StandardField.URL, bestUrl);
            }

            if (result.has("journalInfo") && result.getJSONObject("journalInfo").has("issn")) {
                entry.setField(StandardField.ISSN, result.getJSONObject("journalInfo").getString("issn"));
            }
            // Prefer a full ISO date if provided
            final String datePattern = "\\d{4}-\\d{2}-\\d{2}";
            String printPubDate = result.optString("printPublicationDate");
            String dateOfPublication = result.optString("dateOfPublication");
            if (printPubDate != null && printPubDate.matches(datePattern)) {
                entry.setField(StandardField.DATE, printPubDate);
            } else if (dateOfPublication != null && dateOfPublication.matches(datePattern)) {
                entry.setField(StandardField.DATE, dateOfPublication);
            }

            // Handle authors
            if (result.has("authorList") && result.getJSONObject("authorList").has("author")) {
                JSONArray authors = result.getJSONObject("authorList").getJSONArray("author");

                List<Author> authorList = new ArrayList<>();

                for (int i = 0; i < authors.length(); i++) {
                    JSONObject author = authors.getJSONObject(i);

                    String lastName = author.optString("lastName", "");
                    String firstName = author.optString("firstName", "");
                    authorList.add(new Author(firstName, "", "", lastName, ""));

                    entry.setField(StandardField.AUTHOR, AuthorList.of(authorList).getAsLastFirstNamesWithAnd(false));
                }
            }

            if (result.has("pubTypeList") && result.getJSONObject("pubTypeList").has("pubType")) {
                JSONArray pubTypes = result.getJSONObject("pubTypeList").getJSONArray("pubType");
                if (!pubTypes.isEmpty()) {
                    entry.setField(StandardField.PUBSTATE, pubTypes.getString(0));
                }
            }

            if (result.has("keywordList") && result.getJSONObject("keywordList").has("keyword")) {
                JSONArray keywords = result.getJSONObject("keywordList").getJSONArray("keyword");
                for (int i = 0; i < keywords.length(); i++) {
                    if (!keywords.isNull(i)) {
                        String keyword = keywords.optString(i, "").trim();
                        if (!keyword.isEmpty()) {
                            entry.addKeyword(keyword, ',');
                        }
                    }
                }
            }
            if (result.has("meshHeadingList") && result.getJSONObject("meshHeadingList").has("meshHeading")) {
                JSONArray mesh = result.getJSONObject("meshHeadingList").getJSONArray("meshHeading");
                for (int i = 0; i < mesh.length(); i++) {
                    JSONObject meshHeading = mesh.optJSONObject(i);
                    if (meshHeading != null) {
                        String descriptor = meshHeading.optString("descriptorName", "").trim();
                        if (!descriptor.isEmpty()) {
                            entry.addKeyword(descriptor, ',');
                        }
                    } else if (!mesh.isNull(i)) {
                        // Sometimes MeSH heading may be a plain string
                        String meshPlain = mesh.optString(i, "").trim();
                        if (!meshPlain.isEmpty()) {
                            entry.addKeyword(meshPlain, ',');
                        }
                    }
                }
            }

            if (result.has("pubModel")) {
                Optional.ofNullable(result.optString("pubModel")).ifPresent(pubModel -> entry.withField(StandardField.HOWPUBLISHED, pubModel));
            }
            if (result.has("publicationStatus")) {
                Optional.ofNullable(result.optString("publicationStatus")).ifPresent(pubStatus -> entry.withField(StandardField.PUBSTATE, pubStatus));
            }

            if (result.has("journalInfo")) {
                JSONObject journalInfo = result.getJSONObject("journalInfo");
                Optional.ofNullable(journalInfo.optString("issue")).ifPresent(issue -> entry.setField(StandardField.ISSUE, issue));
                Optional.ofNullable(journalInfo.optString("volume")).ifPresent(volume -> entry.setField(StandardField.VOLUME, volume));
                Optional.of(journalInfo.optInt("yearOfPublication")).ifPresent(year -> entry.setField(StandardField.YEAR, year.toString()));
                Optional.of(journalInfo.optInt("monthOfPublication"))
                        .flatMap(month -> Month.parse(month.toString()))
                        .ifPresent(parsedMonth -> entry.setField(StandardField.MONTH, parsedMonth.getJabRefFormat()));
                if (journalInfo.has("journal")) {
                    JSONObject journal = journalInfo.getJSONObject("journal");
                    Optional.ofNullable(journal.optString("title")).ifPresent(title -> entry.setField(StandardField.JOURNAL, title));
                    Optional.ofNullable(journal.optString("nlmid")).ifPresent(nlmid -> entry.setField(new UnknownField("nlmid"), nlmid));
                    Optional.ofNullable(journal.optString("issn")).ifPresent(issn -> entry.setField(StandardField.ISSN, issn));
                }
            }

            return entry;
        } catch (JSONException e) {
            throw new ParseException("Error parsing EuropePMC response", e);
        }
    }

    private EntryType determineEntryType(JSONObject result) {
        EntryType defaultType = StandardEntryType.Article;
        if (!(result.has("pubTypeList") && result.getJSONObject("pubTypeList").has("pubType"))) {
            return defaultType;
        }
        JSONArray pubTypes = result.getJSONObject("pubTypeList").getJSONArray("pubType");
        List<String> types = new ArrayList<>();
        for (int i = 0; i < pubTypes.length(); i++) {
            types.add(pubTypes.optString(i, "").toLowerCase());
        }
        if (matchesAny(types, "book chapter") || matchesAny(types, "chapter")) {
            return StandardEntryType.InCollection;
        }
        if (matchesAny(types, "book")) {
            return StandardEntryType.Book;
        }
        if (matchesAny(types, "conference") || matchesAny(types, "proceedings") || matchesAny(types, "conference paper") || matchesAny(types, "proceedings paper")) {
            return StandardEntryType.InProceedings;
        }
        if (matchesAny(types, "phd") || matchesAny(types, "phd thesis") || matchesAny(types, "doctoral thesis")) {
            return StandardEntryType.PhdThesis;
        }
        if (matchesAny(types, "master") || matchesAny(types, "masters thesis") || matchesAny(types, "master's thesis")) {
            return StandardEntryType.MastersThesis;
        }
        // Letters, reviews, editorials are usually articles
        return defaultType;
    }

    // substring matches
    private boolean matchesAny(List<String> list, String searchString) {
        return list.stream().anyMatch(entry -> entry.contains(searchString));
    }

    private Optional<String> extractBestFullTextUrl(JSONObject result) {
        try {
            if (!(result.has("fullTextUrlList") && result.getJSONObject("fullTextUrlList").has("fullTextUrl"))) {
                return Optional.empty();
            }
            JSONArray urls = result.getJSONObject("fullTextUrlList").getJSONArray("fullTextUrl");
            // First pass: prefer open/free PDF
            for (int i = 0; i < urls.length(); i++) {
                JSONObject urlEntry = urls.getJSONObject(i);
                String style = urlEntry.optString("documentStyle", "").toLowerCase();
                String availability = urlEntry.optString("availability", "").toLowerCase();
                String url = urlEntry.optString("url", "");
                if (url == null || url.isBlank()) {
                    continue;
                }
                if ((availability.contains("open") || availability.contains("free")) && style.contains("pdf")) {
                    return Optional.of(url);
                }
            }
            // Second pass: any PDF
            for (int i = 0; i < urls.length(); i++) {
                JSONObject urlEntry = urls.getJSONObject(i);
                String style = urlEntry.optString("documentStyle", "").toLowerCase();
                String url = urlEntry.optString("url", "");
                if (url == null || url.isBlank()) {
                    continue;
                }
                if (style.contains("pdf")) {
                    return Optional.of(url);
                }
            }
            return Optional.empty();
        } catch (JSONException e) {
            LOGGER.error("Error parsing EuropePMC response for {}", result, e);
            return Optional.empty();
        }
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new FieldFormatterCleanup(StandardField.PAGES, new NormalizePagesFormatter()).cleanup(entry);
    }

    @Override
    public String getName() {
        return "Europe/PMCID";
    }

    @Override
    public Optional<URL> findFullText(@NonNull BibEntry entry) throws IOException, FetcherException {
        // Check if this is a Wiley publication
        if (!isWileyPublication(entry)) {
            return Optional.empty();
        }

        // Get DOI from the entry
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isEmpty()) {
            LOGGER.debug("No DOI found for entry, cannot fetch Wiley fulltext");
            return Optional.empty();
        }

        try {
            return fetchWileyFulltext(doi.get());
        } catch (Exception e) {
            LOGGER.warn("Failed to fetch Wiley fulltext for DOI: {}", doi.get(), e);
            return Optional.empty();
        }
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    /**
     * Checks if the given BibEntry is from Wiley publisher.
     * This is determined by checking the DOI prefix or publisher field.
     */
    private boolean isWileyPublication(BibEntry entry) {
        // Check DOI prefix for Wiley (10.1002)
        Optional<DOI> doi = entry.getField(StandardField.DOI).flatMap(DOI::parse);
        if (doi.isPresent() && doi.get().toString().startsWith("10.1002/")) {
            return true;
        }

        // Check publisher field
        Optional<String> publisher = entry.getField(StandardField.PUBLISHER);
        if (publisher.isPresent()) {
            String pub = publisher.get().toLowerCase();
            return pub.contains("wiley") || pub.contains("john wiley");
        }

        return false;
    }

    /**
     * Fetches fulltext PDF URL from Wiley TDM API.
     * This requires a TDM-Client-Token for authentication.
     */
    private Optional<URL> fetchWileyFulltext(DOI doi) throws IOException {
        // First try: Wiley TDM API with token (best option for subscribed content)
        Optional<URL> tdmResult = tryWileyTdmApi(doi);
        if (tdmResult.isPresent()) {
            return tdmResult;
        }

        // Second try: Check Europe PMC for open access version
        Optional<URL> openAccessResult = tryEuropePmcOpenAccess(doi);
        if (openAccessResult.isPresent()) {
            LOGGER.info("Found open access version of Wiley article via Europe PMC: {}", doi);
            return openAccessResult;
        }

        // Third try: Check if Wiley has made it freely available
        Optional<URL> directWileyResult = tryDirectWileyAccess(doi);
        if (directWileyResult.isPresent()) {
            LOGGER.info("Found freely available Wiley article: {}", doi);
            return directWileyResult;
        }

        LOGGER.debug("No fulltext found for Wiley DOI: {}", doi);
        return Optional.empty();
    }

    /**
     * Attempts to fetch fulltext from Wiley TDM API using authentication token.
     *
     * @param doi the DOI to fetch
     * @return URL to the PDF if successful, empty otherwise
     */
    private Optional<URL> tryWileyTdmApi(DOI doi) {
        if (importerPreferences == null) {
            return Optional.empty();
        }

        Optional<String> apiKey = importerPreferences.getApiKey(getName());
        if (apiKey.isEmpty()) {
            return Optional.empty();
        }

        try {
            String apiUrl = WILEY_TDM_API_URL + doi.toString();
            LOGGER.debug("Trying Wiley TDM API: {}", apiUrl);

            URLDownload download = new URLDownload(apiUrl);
            download.addHeader("Wiley-TDM-Client-Token", apiKey.get());
            download.addHeader("Accept", "application/pdf");

            try {
                download.asInputStream().close(); // Test the connection
            } catch (FetcherException e) {
                return Optional.empty();
            }

            return Optional.of(new URI(apiUrl).toURL());
        } catch (IOException | URISyntaxException e) {
            LOGGER.debug("Wiley TDM API failed: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Searches Europe PMC for open access versions of Wiley articles.
     *
     * @param doi the DOI to search for
     * @return URL to the open access PDF if found, empty otherwise
     */
    private Optional<URL> tryEuropePmcOpenAccess(DOI doi) {
        try {
            // Query Europe PMC for this DOI to check if there's an OA version
            String queryUrl = "https://www.ebi.ac.uk/europepmc/webservices/rest/search?query=DOI:" +
                    doi.toString() + "&resultType=core&format=json";
            URLDownload download = new URLDownload(queryUrl);
            String response = download.asString();

            // Parse and check for fulltext URLs in the response
            JSONObject json = new JSONObject(response);
            if (json.has("resultList")) {
                JSONObject resultList = json.getJSONObject("resultList");
                if (resultList.has("result") && resultList.getJSONArray("result").length() > 0) {
                    JSONObject result = resultList.getJSONArray("result").getJSONObject(0);
                    Optional<String> urlString = extractBestFullTextUrl(result);
                    if (urlString.isPresent()) {
                        return Optional.of(new URI(urlString.get()).toURL());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Europe PMC open access check failed: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    /**
     * Attempts direct access to Wiley PDF URLs for freely available articles.
     * Some Wiley articles are made freely available without requiring authentication.
     *
     * @param doi the DOI to check
     * @return URL to the PDF if freely accessible, empty otherwise
     */
    private Optional<URL> tryDirectWileyAccess(DOI doi) {
        try {
            // Try direct PDF URL patterns that Wiley sometimes uses for open access
            String pdfUrl = "https://onlinelibrary.wiley.com/doi/pdf/" + doi.toString();
            URLDownload download = new URLDownload(pdfUrl);
            download.addHeader("Accept", "application/pdf");

            // Try to check if PDF is accessible
            Optional<String> contentType = download.getMimeType();
            if (contentType.isPresent() && contentType.get().contains("pdf")) {
                return Optional.of(new URI(pdfUrl).toURL());
            }
        } catch (Exception e) {
            LOGGER.debug("Direct Wiley access check failed: {}", e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public String getTestUrl() {
        // Use a known Wiley DOI for testing the TDM token
        return WILEY_TDM_API_URL + "10.1002/andp.19053221004";
    }
}
