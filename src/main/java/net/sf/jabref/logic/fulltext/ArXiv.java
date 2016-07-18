package net.sf.jabref.logic.fulltext;

import java.io.IOException;
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

import net.sf.jabref.importer.fetcher.GeneralFetcher;
import net.sf.jabref.logic.fetcher.FetcherException;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.logic.util.io.XMLUtil;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Fetcher for the arXiv.
 *
 * @see <a href="http://arxiv.org/help/api/index">ArXiv API</a> for an overview of the API
 * @see <a href="http://arxiv.org/help/api/user-manual#_calling_the_api">ArXiv API User's Manual</a> for a detailed
 *      description on how to use the API
 *
 * Similar implementions:
 *      <a href="https://github.com/nathangrigg/arxiv2bib">arxiv2bib</a>
 *      <a herf="https://gitlab.c3sl.ufpr.br/portalmec/dspace-portalmec/blob/aa209d15082a9870f9daac42c78a35490ce77b52/dspace-api/src/main/java/org/dspace/submit/lookup/ArXivService.java">dspace-portalmec</a>
 */
public class ArXiv implements FullTextFinder {

    private static final Log LOGGER = LogFactory.getLog(ArXiv.class);

    private static final String API_URL = "http://export.arxiv.org/api/query";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        // 1. Eprint
        Optional<String> identifier = entry.getFieldOptional("eprint");
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
        Optional<DOI> doi = entry.getFieldOptional(FieldName.DOI).flatMap(DOI::build);
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

    private List<ArXivEntry> queryApi(String searchQuery, List<String> ids, int start, int maxResults)
            throws FetcherException {
        Document result = callApi(searchQuery, ids, start, maxResults);
        NodeList entries = result.getElementsByTagName("entry");
        return XMLUtil.asList(entries).stream().map(ArXivEntry::new).collect(Collectors.toList());
    }

    /**
     * Queries the API.
     *
     * If only {@code searchQuery} is given, then the API will return results for each article that matches the query.
     * If only {@code ids} is given, then the API will return results for each article in the list.
     * If both {@code searchQuery} and {@code ids} are given, then the API will return each article in
     *         {@code ids} that matches {@code searchQuery}. This allows the API to act as a results filter.
     *
     * @param searchQuery the search query used to find articles;
     *                    <a href="http://arxiv.org/help/api/user-manual#query_details">details</a>
     * @param ids a list of arXiv identifiers
     * @param start the index of the first returned result (zero-based)
     * @param maxResults the number of maximal results (has to be smaller than 2000)
     * @return the response from the API as a XML document (Atom 1.0)
     * @throws FetcherException if there was a problem while building the URL or the API was not accessible
     */
    private Document callApi(String searchQuery, List<String> ids, int start, int maxResults) throws FetcherException {
        if (maxResults > 2000) {
            throw new IllegalArgumentException("The arXiv API limits the number of maximal results to be 2000");
        }

        try {
            URIBuilder uriBuilder = new URIBuilder(API_URL);
            uriBuilder.addParameter("search_query", searchQuery);
            uriBuilder.addParameter("id_list", StringUtils.join(ids, ','));
            uriBuilder.addParameter("start", String.valueOf(start));
            uriBuilder.addParameter("max_results", String.valueOf(maxResults));
            URL url = uriBuilder.build().toURL();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(url.openStream());
        } catch (SAXException | ParserConfigurationException | IOException | URISyntaxException exception) {
            throw new FetcherException("arXiv API request failed", exception);
        }
    }

    private static class ArXivEntry {

        private final Optional<String> title;
        private final Optional<String> urlAbstractPage;
        private final Optional<String> publishedDate;
        private final Optional<String> abstractText;
        private final List<String> authorNames;
        private final List<String> categories;
        private final Optional<URL> pdfUrl;
        private final Optional<String> doiUrl;
        private final Optional<String> journalReferenceText;

        public ArXivEntry(Node item) {
            // see http://arxiv.org/help/api/user-manual#_details_of_atom_results_returned

            // Title of the article
            title = XMLUtil.getNodeContent(item, "title");

            // The url leading to the abstract page
            urlAbstractPage = XMLUtil.getNodeContent(item, "id");

            // Date on which the first version was published
            publishedDate = XMLUtil.getNodeContent(item, "published");

            // Abstract of the article
            abstractText = XMLUtil.getNodeContent(item, "summary");

            // Authors of the article
            authorNames = new ArrayList<>();
            for (Node authorNode : XMLUtil.getNodesByName(item, "author")) {
                Optional<String> authorName = XMLUtil.getNodeContent(authorNode, "name");
                authorName.ifPresent(authorNames::add);
            }

            // Categories (arXiv, ACM, or MSC classification)
            categories = new ArrayList<>();
            for (Node categoryNode : XMLUtil.getNodesByName(item, "category")) {
                Optional<String> category = XMLUtil.getAttributeContent(categoryNode, "name");
                category.ifPresent(categories::add);
            }

            // Links
            Optional<URL> pdfUrlParsed = Optional.empty();
            Optional<String> doiParsed = Optional.empty();
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
                } else if (linkTitle.equals(Optional.of("doi"))) {
                    // Associated DOI url
                    doiParsed = XMLUtil.getAttributeContent(linkNode, "href");
                }
            }
            pdfUrl = pdfUrlParsed;
            doiUrl = doiParsed;

            // Journal reference (as provided by the author)
            journalReferenceText = XMLUtil.getNodeContent(item, "arxiv:journal_ref");
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

        public BibEntry toBibEntry() {
            BibEntry bibEntry = new BibEntry();
            bibEntry.setType(BibtexEntryTypes.ARTICLE);
            bibEntry.setField("author", StringUtils.join(authorNames, " and "));
            getId().ifPresent(id -> bibEntry.setField("eprint", id));
            title.ifPresent(title -> bibEntry.setField("title", title));
            doiUrl.ifPresent(doi -> bibEntry.setField("doi", doi));
            /*
            ("ArchivePrefix", "arXiv"),
            ("PrimaryClass", self.category),
            ("Abstract", self.summary),
            y, m = published[:4], published[5:7]
            ("Year", self.year),
            ("Month", self.month),
            ("Note", self.note),
            ("Url", self.url),
            ("File", self.id + ".pdf"),
            */
            return bibEntry;
        }
    }
}
