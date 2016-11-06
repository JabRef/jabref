package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.logic.TypedBibEntry;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.FulltextFetcher;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.SearchBasedFetcher;
import net.sf.jabref.logic.importer.util.OAI2Handler;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.io.XMLUtil;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.BibtexEntryTypes;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.ParsedFileField;
import net.sf.jabref.model.strings.StringUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Fetcher for the arXiv.
 *
 * @see <a href="http://arxiv.org/help/api/index">ArXiv API</a> for an overview of the API
 * @see <a href="http://arxiv.org/help/api/user-manual#_calling_the_api">ArXiv API User's Manual</a> for a detailed
 * description on how to use the API
 *
 * Similar implementions:
 * <a href="https://github.com/nathangrigg/arxiv2bib">arxiv2bib</a> which is <a href="https://arxiv2bibtex.org/">live</a>
 * <a herf="https://gitlab.c3sl.ufpr.br/portalmec/dspace-portalmec/blob/aa209d15082a9870f9daac42c78a35490ce77b52/dspace-api/src/main/java/org/dspace/submit/lookup/ArXivService.java">dspace-portalmec</a>
 */
public class ArXiv implements FulltextFetcher, SearchBasedFetcher, IdBasedFetcher {
    private static final Log LOGGER = LogFactory.getLog(ArXiv.class);

    private static final String API_URL = "http://export.arxiv.org/api/query";
    private final ImportFormatPreferences importFormatPreferences;

    public ArXiv(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        // 1. Eprint
        Optional<String> identifier = entry.getField(FieldName.EPRINT);
        if (StringUtil.isNotBlank(identifier)) {
            try {
                // Get pdf of entry with the specified id
                Optional<URL> pdfUrl = searchForEntryById(identifier.get()).flatMap(ArXivEntry::getPdfUrl);
                if (pdfUrl.isPresent()) {
                    LOGGER.info("Fulltext PDF found @ arXiv.");
                    return pdfUrl;
                }
            } catch (FetcherException e) {
                LOGGER.warn("arXiv eprint API request failed", e);
            }
        }

        // 2. DOI
        Optional<DOI> doi = entry.getField(FieldName.DOI).flatMap(DOI::build);
        if (doi.isPresent()) {
            String doiString = doi.get().getDOI();
            // Search for an entry in the ArXiv which is linked to the doi
            try {
                Optional<URL> pdfUrl = searchForEntry("doi:" + doiString).flatMap(ArXivEntry::getPdfUrl);
                if (pdfUrl.isPresent()) {
                    LOGGER.info("Fulltext PDF found @ arXiv.");
                    return pdfUrl;
                }
            } catch (FetcherException e) {
                LOGGER.warn("arXiv DOI API request failed", e);
            }
        }

        return Optional.empty();
    }

    private Optional<ArXivEntry> searchForEntry(String searchQuery) throws FetcherException {
        List<ArXivEntry> entries = queryApi(searchQuery, Collections.emptyList(), 0, 1);
        if (entries.size() == 1) {
            return Optional.of(entries.get(0));
        } else {
            return Optional.empty();
        }
    }

    private Optional<ArXivEntry> searchForEntryById(String identifier) throws FetcherException {
        List<ArXivEntry> entries = queryApi("", Collections.singletonList(identifier), 0, 1);
        if (entries.size() == 1) {
            return Optional.of(entries.get(0));
        } else {
            return Optional.empty();
        }
    }

    private List<ArXivEntry> searchForEntries(String searchQuery) throws FetcherException {
        return queryApi(searchQuery, Collections.emptyList(), 0, 10);
    }

    private List<ArXivEntry> queryApi(String searchQuery, List<String> ids, int start, int maxResults)
            throws FetcherException {
        Document result = callApi(searchQuery, ids, start, maxResults);
        List<Node> entries = XMLUtil.asList(result.getElementsByTagName("entry"));

        return entries.stream().map(ArXivEntry::new).collect(Collectors.toList());
    }

    /**
     * Queries the API.
     *
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
    private Document callApi(String searchQuery, List<String> ids, int start, int maxResults) throws FetcherException {
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
                uriBuilder.addParameter("id_list", String.join(",", ids));
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
        // http://export.arxiv.org/api/query?id_list=0307015
        // <entry>
        //      <id>http://arxiv.org/api/errors#incorrect_id_format_for_0307015</id>
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
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_OAI2_ARXIV;
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        return searchForEntries(query).stream().map(
                (arXivEntry) -> arXivEntry.toBibEntry(importFormatPreferences.getKeywordSeparator())).collect(Collectors.toList());
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        return searchForEntryById(identifier).map(
                (arXivEntry) -> arXivEntry.toBibEntry(importFormatPreferences.getKeywordSeparator()));
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
            // see http://arxiv.org/help/api/user-manual#_details_of_atom_results_returned

            // Title of the article
            // The result from the arXiv contains hard line breaks, try to remove them
            title = XMLUtil.getNodeContent(item, "title").map(OAI2Handler::correctLineBreaks);

            // The url leading to the abstract page
            urlAbstractPage = XMLUtil.getNodeContent(item, "id");

            // Date on which the first version was published
            publishedDate = XMLUtil.getNodeContent(item, "published");

            // Abstract of the article
            abstractText = XMLUtil.getNodeContent(item, "summary").map(OAI2Handler::correctLineBreaks)
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
            // Ex: <arxiv:primary_category xmlns:arxiv="http://arxiv.org/schemas/atom" term="math-ph" scheme="http://arxiv.org/schemas/atom"/>
            primaryCategory = XMLUtil.getNode(item, "arxiv:primary_category")
                    .flatMap(node -> XMLUtil.getAttributeContent(node, "term"));
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
        public Optional<String> getId() {
            // remove leading http://arxiv.org/abs/ from abstract url to get arXiv ID
            String prefix = "http://arxiv.org/abs/";
            return urlAbstractPage.map(abstractUrl -> {
                if (abstractUrl.startsWith(prefix)) {
                    return abstractUrl.substring(prefix.length());
                } else {
                    return abstractUrl;
                }
            });
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
            BibEntry bibEntry = new BibEntry();
            bibEntry.setType(BibtexEntryTypes.ARTICLE);
            bibEntry.setField(FieldName.EPRINTTYPE, "arXiv");
            bibEntry.setField(FieldName.AUTHOR, String.join(" and ", authorNames));
            bibEntry.addKeywords(categories, keywordDelimiter);
            getId().ifPresent(id -> bibEntry.setField(FieldName.EPRINT, id));
            title.ifPresent(titleContent -> bibEntry.setField(FieldName.TITLE, titleContent));
            doi.ifPresent(doiContent -> bibEntry.setField(FieldName.DOI, doiContent));
            abstractText.ifPresent(abstractContent -> bibEntry.setField(FieldName.ABSTRACT, abstractContent));
            getDate().ifPresent(date -> bibEntry.setField(FieldName.DATE, date));
            primaryCategory.ifPresent(category -> bibEntry.setField(FieldName.EPRINTCLASS, category));
            journalReferenceText.ifPresent(journal -> bibEntry.setField(FieldName.JOURNALTITLE, journal));
            getPdfUrl().ifPresent(url -> (new TypedBibEntry(bibEntry, BibDatabaseMode.BIBLATEX))
                    .setFiles(Collections.singletonList(new ParsedFileField("online", url, "PDF"))));

            return bibEntry;
        }
    }
}
