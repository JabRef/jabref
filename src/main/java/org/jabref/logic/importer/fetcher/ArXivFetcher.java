package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.cleanup.EprintCleanup;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.IdBasedFetcher;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.ArXivQueryTransformer;
import org.jabref.logic.util.io.XMLUtil;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

import com.google.common.collect.ImmutableMap;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Fetcher for ArXiv that merges fields from arXiv-issued DOIs (and user-issued ones when applicable) to get more information overall.
 * <p>
 * These are the post-processing steps applied to the original fetch from ArXiv's API:
 * <ol>
 *     <li>Use ArXiv-issued DOI to get more merge more data with original entry, overwriting some of those fields;</li>
 *     <li>Use user-issued DOI (if it was provided) to merge even more data with the result of the previous step, overwriting some of those fields;</li>
 *     <li>Modify keywords: remove repetitions and adapt some edge cases (commas in keyword transformed into forward slashes).</li>
 * </ol>
 *
 * @see <a href="https://blog.arxiv.org/2022/02/17/new-arxiv-articles-are-now-automatically-assigned-dois/">arXiv.org blog </a> for more info about arXiv-issued DOIs
 * @see <a href="https://arxiv.org/help/api/index">ArXiv API</a> for an overview of the API
 * @see <a href="https://arxiv.org/help/api/user-manual#_calling_the_api">ArXiv API User's Manual</a> for a detailed description on how to use the API
 */
public class ArXivFetcher implements FulltextFetcher, PagedSearchBasedFetcher, IdBasedFetcher, IdFetcher<ArXivIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArXivFetcher.class);

    // See https://blog.arxiv.org/2022/02/17/new-arxiv-articles-are-now-automatically-assigned-dois/
    private static final String DOI_PREFIX = "10.48550/arXiv.";

    /*
     * Reason behind choice of these fields:
     *   - KEYWORDS: More descriptive
     *   - AUTHOR: Better formatted (last name, rest of name)
     */
    private static final Set<Field> CHOSEN_AUTOMATIC_DOI_FIELDS = Set.of(StandardField.KEYWORDS, StandardField.AUTHOR);

    /*
     * Reason behind choice of these fields:
     *   - DOI: give preference to DOIs manually inputted by users, instead of automatic ones
     *   - PUBLISHER: ArXiv-issued DOIs give 'ArXiv' as entry publisher. While this can be true, prefer using one from external sources,
     *      if applicable
     *   - KEY_FIELD: Usually, the KEY_FIELD retrieved from user-assigned DOIs are 'nicer' (instead of a DOI link, it's usually contains one author and the year)
     */
    private static final Set<Field> CHOSEN_MANUAL_DOI_FIELDS = Set.of(StandardField.DOI, StandardField.PUBLISHER, InternalField.KEY_FIELD);

    private static final Map<String, String> ARXIV_KEYWORDS_WITH_COMMA_REPLACEMENTS = ImmutableMap.of(
            "Computational Engineering, Finance, and Science", "Computational Engineering / Finance / Science",
            "Distributed, Parallel, and Cluster Computing", "Distributed / Parallel / Cluster Computing");

    private final ArXiv arXiv;
    private final DoiFetcher doiFetcher;
    private final ImportFormatPreferences importFormatPreferences;

    public ArXivFetcher(ImportFormatPreferences importFormatPreferences) {
        this(importFormatPreferences, new DoiFetcher(importFormatPreferences));
    }

    /**
     * @param doiFetcher The fetcher, maybe be NULL if no additional search is desired.
     */
    public ArXivFetcher(ImportFormatPreferences importFormatPreferences, DoiFetcher doiFetcher) {
        this.arXiv = new ArXiv(importFormatPreferences);
        this.doiFetcher = doiFetcher;
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        return arXiv.findFullText(entry);
    }

    @Override
    public TrustLevel getTrustLevel() {
        return arXiv.getTrustLevel();
    }

    @Override
    public String getName() {
        return arXiv.getName();
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return arXiv.getHelpPage();
    }

    /**
     * Remove duplicate values on "KEYWORD" field, if any. Al
     *
     * @param bibEntry A BibEntry to modify
     */
    private void adaptKeywordsFrom(BibEntry bibEntry) {
        Optional<String> allKeywords = bibEntry.getField(StandardField.KEYWORDS);
        if (allKeywords.isPresent()) {
            // With the use of ArXiv-issued DOI's KEYWORDS field, some of those keywords might contain comma. As this is the
            // default keyword separator, replace the commas of these instances with some other character
            // (see ARXIV_KEYWORDS_WITH_COMMA_REPLACEMENTS variable)
            for (Map.Entry<String, String> entry : ARXIV_KEYWORDS_WITH_COMMA_REPLACEMENTS.entrySet()) {
                allKeywords = Optional.of(allKeywords.get().replaceAll(entry.getKey(), entry.getValue()));
            }

            String filteredKeywords = KeywordList.merge(allKeywords.get(), "", importFormatPreferences.getKeywordSeparator()).toString();
            bibEntry.setField(StandardField.KEYWORDS, filteredKeywords);
        }
    }

    /**
     * Get ArXiv-issued DOI from the entry's arXiv ID
     * <br/><br/>
     * ArXiv-issued DOIs are identifiers associated with every ArXiv entry. They are composed of a fixed
     * {@link #DOI_PREFIX} + the entry's ArXiv ID
     *
     * @param arXivId An ArXiv ID
     * @return ArXiv-issued DOI
     */
    private static String getAutomaticDoi(String arXivId) {
        return DOI_PREFIX + arXivId;
    }

    /**
     * Get ArXiv-issued DOI from the arXiv entry itself.
     * <br/><br/>
     * ArXiv-issued DOIs are identifiers associated with every ArXiv entry. They are composed of a fixed {@link #DOI_PREFIX} + the entry's ArXiv ID
     *
     * @param arXivBibEntry A Bibtex Entry, formatted as a ArXiv entry. Must contain an EPRINT field
     * @return ArXiv-issued DOI, or Empty, if method could not retrieve it
     */
    private static Optional<String> getAutomaticDoi(BibEntry arXivBibEntry) {
        // As the input should always contain a EPRINT if created from inner 'ArXiv' class, don't bother doing a check that might call
        // ArXiv's API again (method 'findIdentifier')
        Optional<String> entryEprint = arXivBibEntry.getField(StandardField.EPRINT);
        if (entryEprint.isEmpty()) {
            LOGGER.error("Cannot infer ArXiv-issued DOI from BibEntry: no 'EPRINT' field found");
            return Optional.empty();
        } else {
            return Optional.of(ArXivFetcher.getAutomaticDoi(entryEprint.get()));
        }
    }

    /**
     * Get ArXiv-issued DOI from ArXiv Identifier object
     * <br/><br/>
     * ArXiv-issued DOIs are identifiers associated with every ArXiv entry. They are composed of a fixed {@link #DOI_PREFIX} + the entry's ArXiv ID
     *
     * @param arXivId An ArXiv ID as internal object
     * @return ArXiv-issued DOI
     */
    private static String getAutomaticDoi(ArXivIdentifier arXivId) {
        return getAutomaticDoi(arXivId.getNormalizedWithoutVersion());
    }

    /**
     * Check if a specific DOI is user-assigned.
     */
    private static boolean isManualDoi(String doi) {
        return !doi.toLowerCase().contains(DOI_PREFIX.toLowerCase());
    }

    /**
     * Get user-issued DOI from ArXiv Bibtex entry, if any
     * <br/><br/>
     * User-issued DOIs are identifiers associated with some ArXiv entries that can associate an entry with an external service, like
     * <a href="https://link.springer.com/">Springer Link</a>.
     *
     * @param arXivBibEntry An ArXiv Bibtex entry from where the DOI is extracted
     * @return User-issued DOI, if any field exists and if it's not an automatic one (see {@link #getAutomaticDoi(ArXivIdentifier)})
     */
    private static Optional<String> getManualDoi(BibEntry arXivBibEntry) {
        return arXivBibEntry.getField(StandardField.DOI).filter(ArXivFetcher::isManualDoi);
    }

    /**
     * Get the Bibtex Entry from a Future API request (uses blocking) and treat exceptions.
     *
     * @param bibEntryFuture A CompletableFuture that parallelize the API fetching process
     * @return the fetch result
     */
    private static Optional<BibEntry> waitForBibEntryRetrieval(CompletableFuture<Optional<BibEntry>> bibEntryFuture) throws FetcherException {
        try {
            return bibEntryFuture.join();
        } catch (CompletionException e) {
            if (!(e.getCause() instanceof FetcherException)) {
                LOGGER.error("The supplied future should only throw a FetcherException.", e);
                throw e;
            }
            throw (FetcherException) e.getCause();
        }
    }

    /**
     * Eventually merge the ArXiv Bibtex entry with a Future Bibtex entry (ArXiv/user-assigned DOIs)
     *
     * @param arXivEntry The entry to merge into
     * @param bibEntryFuture A future result of the fetching process
     * @param priorityFields Which fields from "bibEntryFuture" to prioritize, replacing them on "arXivEntry"
     * @param id Identifier used in initiating the "bibEntryFuture" future (for logging). This is usually a DOI, but can be anything.
     */
    private void mergeArXivEntryWithFutureDoiEntry(BibEntry arXivEntry, CompletableFuture<Optional<BibEntry>> bibEntryFuture, Set<Field> priorityFields, String id) {
        Optional<BibEntry> bibEntry;
        try {
            bibEntry = waitForBibEntryRetrieval(bibEntryFuture);
        } catch (FetcherException | CompletionException e) {
            LOGGER.error("Failed to fetch future BibEntry with id '{}' (skipping merge).", id, e);
            return;
        }

        if (bibEntry.isPresent()) {
            adaptKeywordsFrom(bibEntry.get());
            arXivEntry.mergeWith(bibEntry.get(), priorityFields);
        } else {
            LOGGER.error("Future BibEntry for id '{}' was completed, but no entry was found (skipping merge).", id);
        }
    }

    /**
     * Infuse arXivBibEntryPromise with additional fields in an asynchronous way
     *
     * @param arXivBibEntry An existing entry to be updated with new/modified fields
     */
    private void inplaceAsyncInfuseArXivWithDoi(BibEntry arXivBibEntry) {
        CompletableFuture<Optional<BibEntry>> arXivBibEntryCompletedFuture = CompletableFuture.completedFuture(Optional.of(arXivBibEntry));
        Optional<ArXivIdentifier> arXivBibEntryId = arXivBibEntry.getField(StandardField.EPRINT).flatMap(ArXivIdentifier::parse);

        try {
            this.inplaceAsyncInfuseArXivWithDoi(arXivBibEntryCompletedFuture, arXivBibEntryId);
        } catch (FetcherException e) {
            LOGGER.error("FetcherException should not be found here, as main Bibtex Entry already exists " +
                    "(and failing additional fetches should be skipped)", e);
        }
    }

    /**
     * Infuse arXivBibEntryFuture with additional fields in an asynchronous way, accelerating the process by providing a valid ArXiv ID
     *
     * @param arXivBibEntryFuture A future entry that (if it exists) will be updated with new/modified fields
     * @param arXivId An ArXiv ID for the main reference (from ArXiv), so that the retrieval of ArXiv-issued DOI metadata can be faster
     * @throws FetcherException when failed to fetch the main ArtXiv Bibtex entry ('arXivBibEntryFuture').
     */
    private void inplaceAsyncInfuseArXivWithDoi(CompletableFuture<Optional<BibEntry>> arXivBibEntryFuture, Optional<ArXivIdentifier> arXivId) throws FetcherException {

        Optional<CompletableFuture<Optional<BibEntry>>> automaticDoiBibEntryFuture;
        Optional<BibEntry> arXivBibEntry;

        Optional<String> automaticDoi;
        Optional<String> manualDoi;

        // We can accelerate the processing time by initiating a parallel request for DOIFetcher with an ArXiv-issued DOI alongside the ArXiv fetching itself,
        // BUT ONLY IF we have a valid arXivId. If not, the ArXiv entry must be retrieved before, which invalidates this optimization (although we can still speed
        // up the process by running both the ArXiv-assigned and user-assigned DOI fetching at the same time, if an entry has this last information)
        if (arXivId.isPresent()) {
            automaticDoi = Optional.of(ArXivFetcher.getAutomaticDoi(arXivId.get()));
            automaticDoiBibEntryFuture = Optional.of(doiFetcher.asyncPerformSearchById(automaticDoi.get()));

            arXivBibEntry = ArXivFetcher.waitForBibEntryRetrieval(arXivBibEntryFuture);
            if (arXivBibEntry.isEmpty()) {
                return;
            }
        } else {
            // If ArXiv fetch fails (FetcherException), exception must be passed onwards for the transparency of this class (original ArXiv fetcher does the same)
            arXivBibEntry = ArXivFetcher.waitForBibEntryRetrieval(arXivBibEntryFuture);
            if (arXivBibEntry.isEmpty()) {
                return;
            }

            automaticDoi = ArXivFetcher.getAutomaticDoi(arXivBibEntry.get());
            automaticDoiBibEntryFuture = automaticDoi.map(arXiv::asyncPerformSearchById);
        }

        manualDoi = ArXivFetcher.getManualDoi(arXivBibEntry.get());
        Optional<CompletableFuture<Optional<BibEntry>>> manualDoiBibEntryFuture = manualDoi.map(doiFetcher::asyncPerformSearchById);

        automaticDoiBibEntryFuture.ifPresent(future ->
                mergeArXivEntryWithFutureDoiEntry(arXivBibEntry.get(), future, CHOSEN_AUTOMATIC_DOI_FIELDS, automaticDoi.get()));
        manualDoiBibEntryFuture.ifPresent(future ->
                mergeArXivEntryWithFutureDoiEntry(arXivBibEntry.get(), future, CHOSEN_MANUAL_DOI_FIELDS, manualDoi.get()));
    }

    /**
     * Constructs a complex query string using the field prefixes specified at https://arxiv.org/help/api/user-manual
     * and modify resulting BibEntries with additional info from the ArXiv-issued DOI
     *
     * @param luceneQuery the root node of the lucene query
     * @return A list of entries matching the complex query
     */
    @Override
    public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {

        Page<BibEntry> result = arXiv.performSearchPaged(luceneQuery, pageNumber);
        if (this.doiFetcher == null) {
            return result;
        }

        ExecutorService executor = Executors.newFixedThreadPool(getPageSize() * 2);

        Collection<CompletableFuture<BibEntry>> futureSearchResult = result.getContent()
                                                                       .stream()
                                                                       .map(bibEntry ->
                                                                               CompletableFuture.supplyAsync(() -> {
                                                                                   this.inplaceAsyncInfuseArXivWithDoi(bibEntry);
                                                                                   return bibEntry;
                                                                               }, executor))
                                                                       .toList();

        Collection<BibEntry> modifiedSearchResult = futureSearchResult.stream()
                                      .map(CompletableFuture::join)
                                      .collect(Collectors.toList());

        return new Page<>(result.getQuery(), result.getPageNumber(), modifiedSearchResult);
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        CompletableFuture<Optional<BibEntry>> arXivBibEntryPromise = arXiv.asyncPerformSearchById(identifier);
        if (this.doiFetcher != null) {
            inplaceAsyncInfuseArXivWithDoi(arXivBibEntryPromise, ArXivIdentifier.parse(identifier));
        }
        return arXivBibEntryPromise.join();
    }

    @Override
    public Optional<ArXivIdentifier> findIdentifier(BibEntry entry) throws FetcherException {
        return arXiv.findIdentifier(entry);
    }

    @Override
    public String getIdentifierName() {
        return arXiv.getIdentifierName();
    }

    /**
     * Fetcher for the arXiv.
     *
     * @see <a href="https://arxiv.org/help/api/index">ArXiv API</a> for an overview of the API
     * @see <a href="https://arxiv.org/help/api/user-manual#_calling_the_api">ArXiv API User's Manual</a> for a detailed
     * description on how to use the API
     * <p>
     * Similar implementions:
     * <a href="https://github.com/nathangrigg/arxiv2bib">arxiv2bib</a> which is <a href="https://arxiv2bibtex.org/">live</a>
     * <a herf="https://gitlab.c3sl.ufpr.br/portalmec/dspace-portalmec/blob/aa209d15082a9870f9daac42c78a35490ce77b52/dspace-api/src/main/java/org/dspace/submit/lookup/ArXivService.java">dspace-portalmec</a>
     */
    protected class ArXiv implements FulltextFetcher, PagedSearchBasedFetcher, IdBasedFetcher, IdFetcher<ArXivIdentifier> {

        private static final Logger LOGGER = LoggerFactory.getLogger(org.jabref.logic.importer.fetcher.ArXivFetcher.ArXiv.class);

        private static final String API_URL = "https://export.arxiv.org/api/query";

        private final ImportFormatPreferences importFormatPreferences;

        public ArXiv(ImportFormatPreferences importFormatPreferences) {
            this.importFormatPreferences = importFormatPreferences;
        }

        @Override
        public Optional<URL> findFullText(BibEntry entry) throws IOException {
            Objects.requireNonNull(entry);

            try {
                Optional<URL> pdfUrl = searchForEntries(entry).stream()
                                                              .map(ArXivEntry::getPdfUrl)
                                                              .filter(Optional::isPresent)
                                                              .map(Optional::get)
                                                              .findFirst();
                pdfUrl.ifPresent(url -> LOGGER.info("Fulltext PDF found @ arXiv."));
                return pdfUrl;
            } catch (FetcherException e) {
                LOGGER.warn("arXiv API request failed", e);
            }

            return Optional.empty();
        }

        @Override
        public TrustLevel getTrustLevel() {
            return TrustLevel.PREPRINT;
        }

        private Optional<ArXivEntry> searchForEntry(String searchQuery) throws FetcherException {
            List<ArXivEntry> entries = queryApi(searchQuery, Collections.emptyList(), 0, 1);
            if (entries.size() == 1) {
                return Optional.of(entries.get(0));
            } else {
                return Optional.empty();
            }
        }

        private Optional<ArXivEntry> searchForEntryById(String id) throws FetcherException {
            Optional<ArXivIdentifier> identifier = ArXivIdentifier.parse(id);
            if (identifier.isEmpty()) {
                return Optional.empty();
            }

            List<ArXivEntry> entries = queryApi("", Collections.singletonList(identifier.get()), 0, 1);
            if (entries.size() >= 1) {
                return Optional.of(entries.get(0));
            } else {
                return Optional.empty();
            }
        }

        private List<ArXivEntry> searchForEntries(BibEntry originalEntry) throws FetcherException {
            // We need to clone the entry, because we modify it by a cleanup job.
            final BibEntry entry = (BibEntry) originalEntry.clone();

            // 1. Check for Eprint
            new EprintCleanup().cleanup(entry);
            Optional<String> identifier = entry.getField(StandardField.EPRINT);
            if (StringUtil.isNotBlank(identifier)) {
                try {
                    // Get pdf of entry with the specified id
                    return OptionalUtil.toList(searchForEntryById(identifier.get()));
                } catch (FetcherException e) {
                    LOGGER.warn("arXiv eprint API request failed", e);
                }
            }

            // 2. DOI and other fields
            String query;
            Optional<String> doiString = entry.getField(StandardField.DOI)
                                              .flatMap(DOI::parse)
                                              .map(DOI::getNormalized);

            // ArXiv-issued DOIs seem to be unsearchable from ArXiv API's "query string", so ignore it
            if (doiString.isPresent() && ArXivFetcher.isManualDoi(doiString.get())) {
                query = "doi:" + doiString.get();
            } else {
                Optional<String> authorQuery = entry.getField(StandardField.AUTHOR).map(author -> "au:" + author);
                Optional<String> titleQuery = entry.getField(StandardField.TITLE).map(title -> "ti:" + StringUtil.ignoreCurlyBracket(title));
                query = String.join("+AND+", OptionalUtil.toList(authorQuery, titleQuery));
            }

            Optional<ArXivEntry> arxivEntry = searchForEntry(query);
            if (arxivEntry.isPresent()) {
                // Check if entry is a match
                StringSimilarity match = new StringSimilarity();
                String arxivTitle = arxivEntry.get().title.orElse("");
                String entryTitle = StringUtil.ignoreCurlyBracket(entry.getField(StandardField.TITLE).orElse(""));
                if (match.isSimilar(arxivTitle, entryTitle)) {
                    return OptionalUtil.toList(arxivEntry);
                }
            }

            return Collections.emptyList();
        }

        private List<ArXivEntry> searchForEntries(String searchQuery, int pageNumber) throws FetcherException {
            return queryApi(searchQuery, Collections.emptyList(), getPageSize() * pageNumber, getPageSize());
        }

        private List<ArXivEntry> queryApi(String searchQuery, List<ArXivIdentifier> ids, int start, int maxResults)
                throws FetcherException {
            Document result = callApi(searchQuery, ids, start, maxResults);
            List<Node> entries = XMLUtil.asList(result.getElementsByTagName("entry"));

            return entries.stream().map(ArXivEntry::new).collect(Collectors.toList());
        }

        /**
         * Queries the API.
         * <p>
         * If only {@code searchQuery} is given, then the API will return results for each article that matches the query.
         * If only {@code ids} is given, then the API will return results for each article in the list.
         * If both {@code searchQuery} and {@code ids} are given, then the API will return each article in
         * {@code ids} that matches {@code searchQuery}. This allows the API to act as a results filter.
         *
         * @param searchQuery the search query used to find articles;
         *                    <a href="http://arxiv.org/help/api/user-manual#query_details">details</a>
         * @param ids         a list of arXiv identifiers
         * @param start       the index of the first returned result (zero-based)
         * @param maxResults  the number of maximal results (has to be smaller than 2000)
         * @return the response from the API as a XML document (Atom 1.0)
         * @throws FetcherException if there was a problem while building the URL or the API was not accessible
         */
        private Document callApi(String searchQuery, List<ArXivIdentifier> ids, int start, int maxResults) throws FetcherException {
            if (maxResults > 2000) {
                throw new IllegalArgumentException("The arXiv API limits the number of maximal results to be 2000");
            }

            try {
                URIBuilder uriBuilder = new URIBuilder(API_URL);
                // The arXiv API has problems with accents, so we remove them (i.e. Fréchet -> Frechet)
                if (StringUtil.isNotBlank(searchQuery)) {
                    uriBuilder.addParameter("search_query", StringUtil.stripAccents(searchQuery));
                }
                if (!ids.isEmpty()) {
                    uriBuilder.addParameter("id_list",
                            ids.stream().map(ArXivIdentifier::getNormalized).collect(Collectors.joining(",")));
                }
                uriBuilder.addParameter("start", String.valueOf(start));
                uriBuilder.addParameter("max_results", String.valueOf(maxResults));
                URL url = uriBuilder.build().toURL();

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                if (connection.getResponseCode() == 400) {
                    // Bad request error from server, try to get more information
                    throw getException(builder.parse(connection.getErrorStream()));
                } else {
                    return builder.parse(connection.getInputStream());
                }
            } catch (SAXException | ParserConfigurationException | IOException | URISyntaxException exception) {
                throw new FetcherException("arXiv API request failed", exception);
            }
        }

        private FetcherException getException(Document error) {
            List<Node> entries = XMLUtil.asList(error.getElementsByTagName("entry"));

            // Check if the API returned an error
            // In case of an error, only one entry will be returned with the error information. For example:
            // https://export.arxiv.org/api/query?id_list=0307015
            // <entry>
            //      <id>https://arxiv.org/api/errors#incorrect_id_format_for_0307015</id>
            //      <title>Error</title>
            //      <summary>incorrect id format for 0307015</summary>
            // </entry>
            if (entries.size() == 1) {
                Node node = entries.get(0);
                Optional<String> id = XMLUtil.getNodeContent(node, "id");
                Boolean isError = id.map(idContent -> idContent.startsWith("http://arxiv.org/api/errors")).orElse(false);
                if (isError) {
                    String errorMessage = XMLUtil.getNodeContent(node, "summary").orElse("Unknown error");
                    return new FetcherException(errorMessage);
                }
            }
            return new FetcherException("arXiv API request failed");
        }

        @Override
        public String getName() {
            return "ArXiv";
        }

        @Override
        public Optional<HelpFile> getHelpPage() {
            return Optional.of(HelpFile.FETCHER_OAI2_ARXIV);
        }

        /**
         * Constructs a complex query string using the field prefixes specified at https://arxiv.org/help/api/user-manual
         *
         * @param luceneQuery the root node of the lucene query
         * @return A list of entries matching the complex query
         */
        @Override
        public Page<BibEntry> performSearchPaged(QueryNode luceneQuery, int pageNumber) throws FetcherException {
            ArXivQueryTransformer transformer = new ArXivQueryTransformer();
            String transformedQuery = transformer.transformLuceneQuery(luceneQuery).orElse("");
            List<BibEntry> searchResult = searchForEntries(transformedQuery, pageNumber).stream()
                                                                                        .map((arXivEntry) -> arXivEntry.toBibEntry(importFormatPreferences.getKeywordSeparator()))
                                                                                        .collect(Collectors.toList());
            return new Page<>(transformedQuery, pageNumber, filterYears(searchResult, transformer));
        }

        private List<BibEntry> filterYears(List<BibEntry> searchResult, ArXivQueryTransformer transformer) {
            return searchResult.stream()
                               .filter(entry -> entry.getField(StandardField.DATE).isPresent())
                               // Filter the date field for year only
                               .filter(entry -> transformer.getEndYear().isEmpty() || Integer.parseInt(entry.getField(StandardField.DATE).get().substring(0, 4)) <= transformer.getEndYear().get())
                               .filter(entry -> transformer.getStartYear().isEmpty() || Integer.parseInt(entry.getField(StandardField.DATE).get().substring(0, 4)) >= transformer.getStartYear().get())
                               .collect(Collectors.toList());
        }

        protected CompletableFuture<Optional<BibEntry>> asyncPerformSearchById(String identifier) throws CompletionException {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return performSearchById(identifier);
                } catch (FetcherException e) {
                    throw new CompletionException(e);
                }
            });
        }

        @Override
        public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
            return searchForEntryById(identifier)
                    .map((arXivEntry) -> arXivEntry.toBibEntry(importFormatPreferences.getKeywordSeparator()));
        }

        @Override
        public Optional<ArXivIdentifier> findIdentifier(BibEntry entry) throws FetcherException {
            return searchForEntries(entry).stream()
                                          .map(ArXivEntry::getId)
                                          .filter(Optional::isPresent)
                                          .map(Optional::get)
                                          .findFirst();
        }

        @Override
        public String getIdentifierName() {
            return "ArXiv";
        }

        private static class ArXivEntry {

            private final Optional<String> title;
            private final Optional<String> urlAbstractPage;
            private final Optional<String> publishedDate;
            private final Optional<String> abstractText;
            private final List<String> authorNames;
            private final List<String> categories;
            private final Optional<URL> pdfUrl;
            private final Optional<String> doi;
            private final Optional<String> journalReferenceText;
            private final Optional<String> primaryCategory;

            public ArXivEntry(Node item) {
                // see https://arxiv.org/help/api/user-manual#_details_of_atom_results_returned

                // Title of the article
                // The result from the arXiv contains hard line breaks, try to remove them
                title = XMLUtil.getNodeContent(item, "title").map(ArXivEntry::correctLineBreaks);

                // The url leading to the abstract page
                urlAbstractPage = XMLUtil.getNodeContent(item, "id");

                // Date on which the first version was published
                publishedDate = XMLUtil.getNodeContent(item, "published");

                // Abstract of the article
                abstractText = XMLUtil.getNodeContent(item, "summary").map(ArXivEntry::correctLineBreaks)
                                      .map(String::trim);

                // Authors of the article
                authorNames = new ArrayList<>();
                for (Node authorNode : XMLUtil.getNodesByName(item, "author")) {
                    Optional<String> authorName = XMLUtil.getNodeContent(authorNode, "name").map(String::trim);
                    authorName.ifPresent(authorNames::add);
                }

                // Categories (arXiv, ACM, or MSC classification)
                categories = new ArrayList<>();
                for (Node categoryNode : XMLUtil.getNodesByName(item, "category")) {
                    Optional<String> category = XMLUtil.getAttributeContent(categoryNode, "term");
                    category.ifPresent(categories::add);
                }

                // Links
                Optional<URL> pdfUrlParsed = Optional.empty();
                for (Node linkNode : XMLUtil.getNodesByName(item, "link")) {
                    Optional<String> linkTitle = XMLUtil.getAttributeContent(linkNode, "title");
                    if (linkTitle.equals(Optional.of("pdf"))) {
                        pdfUrlParsed = XMLUtil.getAttributeContent(linkNode, "href").map(url -> {
                            try {
                                return new URL(url);
                            } catch (MalformedURLException e) {
                                return null;
                            }
                        });
                    }
                }
                pdfUrl = pdfUrlParsed;

                // Associated DOI
                doi = XMLUtil.getNodeContent(item, "arxiv:doi");

                // Journal reference (as provided by the author)
                journalReferenceText = XMLUtil.getNodeContent(item, "arxiv:journal_ref");

                // Primary category
                // Ex: <arxiv:primary_category xmlns:arxiv="https://arxiv.org/schemas/atom" term="math-ph" scheme="http://arxiv.org/schemas/atom"/>
                primaryCategory = XMLUtil.getNode(item, "arxiv:primary_category")
                                         .flatMap(node -> XMLUtil.getAttributeContent(node, "term"));
            }

            public static String correctLineBreaks(String s) {
                String result = s.replaceAll("\\n(?!\\s*\\n)", " ");
                result = result.replaceAll("\\s*\\n\\s*", "\n");
                return result.replaceAll(" {2,}", " ").replaceAll("(^\\s*|\\s+$)", "");
            }

            /**
             * Returns the url of the linked pdf
             */
            public Optional<URL> getPdfUrl() {
                return pdfUrl;
            }

            /**
             * Returns the arXiv identifier
             */
            public Optional<String> getIdString() {
                return urlAbstractPage.flatMap(ArXivIdentifier::parse).map(ArXivIdentifier::getNormalizedWithoutVersion);
            }

            public Optional<ArXivIdentifier> getId() {
                return getIdString().flatMap(ArXivIdentifier::parse);
            }

            /**
             * Returns the date when the first version was put on the arXiv
             */
            public Optional<String> getDate() {
                // Publication string also contains time, e.g. 2014-05-09T14:49:43Z
                return publishedDate.map(date -> {
                    if (date.length() < 10) {
                        return null;
                    } else {
                        return date.substring(0, 10);
                    }
                });
            }

            public BibEntry toBibEntry(Character keywordDelimiter) {
                BibEntry bibEntry = new BibEntry(StandardEntryType.Article);
                bibEntry.setField(StandardField.EPRINTTYPE, "arXiv");
                bibEntry.setField(StandardField.AUTHOR, String.join(" and ", authorNames));
                bibEntry.addKeywords(categories, keywordDelimiter);
                getIdString().ifPresent(id -> bibEntry.setField(StandardField.EPRINT, id));
                title.ifPresent(titleContent -> bibEntry.setField(StandardField.TITLE, titleContent));
                doi.ifPresent(doiContent -> bibEntry.setField(StandardField.DOI, doiContent));
                abstractText.ifPresent(abstractContent -> bibEntry.setField(StandardField.ABSTRACT, abstractContent));
                getDate().ifPresent(date -> bibEntry.setField(StandardField.DATE, date));
                primaryCategory.ifPresent(category -> bibEntry.setField(StandardField.EPRINTCLASS, category));
                journalReferenceText.ifPresent(journal -> bibEntry.setField(StandardField.JOURNAL, journal));
                getPdfUrl().ifPresent(url -> bibEntry.setFiles(Collections.singletonList(new LinkedFile(url, "PDF"))));
                return bibEntry;
            }
        }
    }
}
