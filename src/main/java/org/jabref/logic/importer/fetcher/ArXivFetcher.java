package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.lang.reflect.MalformedParametersException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.PagedSearchBasedFetcher;
import org.jabref.logic.importer.fetcher.transformers.ArXivQueryTransformer;
import org.jabref.logic.util.io.XMLUtil;
import org.jabref.logic.util.strings.StringSimilarity;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.LinkedFile;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.paging.Page;
import org.jabref.model.strings.StringUtil;
import org.jabref.model.util.OptionalUtil;

import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Fetcher for the arXiv - with post-processing:
 * <ul>
 *     <li>Merges fields from arXiv-issued DOIs to get more information overall.</li>
 * </ul>
 *<p>
 * @see <a href="https://blog.arxiv.org/2022/02/17/new-arxiv-articles-are-now-automatically-assigned-dois/">arXiv.org blog </a> for more info about arXiv-issued DOIs
 * @see <a href="https://arxiv.org/help/api/index">ArXiv API</a> for an overview of the API
 * @see <a href="https://arxiv.org/help/api/user-manual#_calling_the_api">ArXiv API User's Manual</a> for a detailed
 * description on how to use the API
 * <p>
 * Similar implementions:
 * <a href="https://github.com/nathangrigg/arxiv2bib">arxiv2bib</a> which is <a href="https://arxiv2bibtex.org/">live</a>
 * <a herf="https://gitlab.c3sl.ufpr.br/portalmec/dspace-portalmec/blob/aa209d15082a9870f9daac42c78a35490ce77b52/dspace-api/src/main/java/org/dspace/submit/lookup/ArXivService.java">dspace-portalmec</a>
 */
public class ArXivFetcher implements FulltextFetcher, PagedSearchBasedFetcher, IdBasedFetcher, IdFetcher<ArXivIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArXivFetcher.class);
    private static final String DOI_PREFIX = "10.48550/arXiv.";

    private final ArXiv arXiv;
    private final DoiFetcher doiFetcher;
    private final ImporterPreferences importerPreferences;

    public ArXivFetcher(ImportFormatPreferences importFormatPreferences, ImporterPreferences importerPreferences) {
        this.arXiv = new ArXiv(importFormatPreferences);
        this.doiFetcher = new DoiFetcher(importFormatPreferences);
        this.importerPreferences = importerPreferences;
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

    private String getGeneratedArXivDoi(String arXivId) {
        return DOI_PREFIX + arXivId;
    }

    /**
     * Fuse ArXiv bib entry with the ArXiv-issued DOI bib entry
     *
     * @param arXivBibEntry A BibEntry from ArXiv
     * @return A new BibEntry with (possibly) more fields
     */
    private BibEntry getFusedBibEntry(BibEntry arXivBibEntry) throws FetcherException, MalformedParametersException {

        String arXivId = findIdentifier(arXivBibEntry).orElseThrow(
                () -> new MalformedParametersException(String.format("Provided BibEntry with id '%s' is not from arXiv", arXivBibEntry.getId())))
                .getNormalizedWithoutVersion();

        BibEntry doiEntry = doiFetcher.performSearchById(getGeneratedArXivDoi(arXivId)).orElseThrow(
                () -> new FetcherException(String.format("Failed to retrieve entry from ArXiv-issued DOI '%s'", getGeneratedArXivDoi(arXivId))));

        return arXivBibEntry.merge(doiEntry);
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

        Page<BibEntry> originalResult = arXiv.performSearchPaged(luceneQuery, pageNumber);

        if (!this.importerPreferences.shouldUseArXivDoiForMoreInfo()) {
            return originalResult;
        }

        Collection<BibEntry> modifiedSearchResult = new ArrayList<>();
        for (BibEntry arXivEntry : originalResult.getContent()) {
            try {
                modifiedSearchResult.add(getFusedBibEntry(arXivEntry));
            } catch (MalformedParametersException | FetcherException e) {
                LOGGER.error(e.getMessage());
                modifiedSearchResult.add(arXivEntry);
            }
        }

        return new Page<>(originalResult.getQuery(), originalResult.getPageNumber(), modifiedSearchResult);
    }

    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<BibEntry> originalResult = arXiv.performSearchById(identifier);

        if (!this.importerPreferences.shouldUseArXivDoiForMoreInfo()) {
            return originalResult;
        }

        if (originalResult.isEmpty()) {
            return originalResult;
        }

        try {
            return Optional.of(getFusedBibEntry(originalResult.get()));
        } catch (MalformedParametersException | FetcherException e) {
            LOGGER.error(e.getMessage());
            return originalResult;
        }
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
    private class ArXiv implements FulltextFetcher, PagedSearchBasedFetcher, IdBasedFetcher, IdFetcher<ArXivIdentifier> {

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
            String query = entry.getField(StandardField.DOI)
                                .flatMap(DOI::parse)
                                .map(DOI::getNormalized)
                                .map(doiString -> "doi:" + doiString)
                                .orElseGet(() -> {
                                    Optional<String> authorQuery = entry.getField(StandardField.AUTHOR).map(author -> "au:" + author);
                                    Optional<String> titleQuery = entry.getField(StandardField.TITLE).map(title -> "ti:" + StringUtil.ignoreCurlyBracket(title));
                                    return String.join("+AND+", OptionalUtil.toList(authorQuery, titleQuery));
                                });
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
                journalReferenceText.ifPresent(journal -> bibEntry.setField(StandardField.JOURNALTITLE, journal));
                getPdfUrl().ifPresent(url -> bibEntry.setFiles(Collections.singletonList(new LinkedFile(url, "PDF"))));
                return bibEntry;
            }
        }
    }
}
