package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.jabref.logic.help.HelpFile;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.PagedSearchBasedParserFetcher;
import org.jabref.logic.importer.Parser;
import org.jabref.logic.importer.fetcher.transformers.ISIDOREQueryTransformer;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;

import jakarta.ws.rs.core.MediaType;
import org.apache.http.client.utils.URIBuilder;
import org.apache.lucene.queryparser.flexible.core.nodes.QueryNode;
import org.jooq.lambda.Unchecked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Fetcher for <a href="https://isidore.science">ISIDORE</a>```
 * Will take in the link to the website or the last six digits that identify the reference
 * Uses <a href="https://isidore.science/api">ISIDORE's API</a>.
 */
public class ISIDOREFetcher implements PagedSearchBasedParserFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ISIDOREFetcher.class);

    private static final String SOURCE_WEB_SEARCH = "https://api.isidore.science/resource/search";

    private final DocumentBuilderFactory factory;

    public ISIDOREFetcher() {
        this.factory = DocumentBuilderFactory.newInstance();
    }

    @Override
    public Parser getParser() {
        return xmlData -> {
            try {
                PushbackInputStream pushbackInputStream = new PushbackInputStream(xmlData);
                int data = pushbackInputStream.read();
                if (data == -1) {
                    return List.of();
                }
                if (pushbackInputStream.available() < 5) {
                    // We guess, it's an error if less than 5
                    pushbackInputStream.unread(data);
                    String error = new String(pushbackInputStream.readAllBytes(), StandardCharsets.UTF_8);
                    throw new FetcherException(error);
                }

                pushbackInputStream.unread(data);
                DocumentBuilder builder = this.factory.newDocumentBuilder();
                Document document = builder.parse(pushbackInputStream);

                // Assuming the root element represents an entry
                Element entryElement = document.getDocumentElement();

                if (entryElement == null) {
                    return Collections.emptyList();
                }

                return parseXMl(entryElement);
            } catch (FetcherException e) {
                Unchecked.throwChecked(e);
            } catch (ParserConfigurationException |
                     IOException |
                     SAXException e) {
                Unchecked.throwChecked(new FetcherException("Issue with parsing link", e));
            }
            return null;
        };
    }

    @Override
    public URLDownload getUrlDownload(URL url) {
        URLDownload download = new URLDownload(url);
        download.addHeader("Accept", MediaType.APPLICATION_XML);
        return download;
    }

    @Override
    public URL getURLForQuery(QueryNode luceneQuery, int pageNumber) throws URISyntaxException, MalformedURLException, FetcherException {
        ISIDOREQueryTransformer queryTransformer = new ISIDOREQueryTransformer();
        String transformedQuery = queryTransformer.transformLuceneQuery(luceneQuery).orElse("");
        URIBuilder uriBuilder = new URIBuilder(SOURCE_WEB_SEARCH);
        uriBuilder.addParameter("q", transformedQuery);
        if (pageNumber > 1) {
            uriBuilder.addParameter("page", String.valueOf(pageNumber));
        }
        uriBuilder.addParameter("replies", String.valueOf(getPageSize()));
        uriBuilder.addParameter("lang", "en");
        uriBuilder.addParameter("output", "xml");
        queryTransformer.getParameterMap().forEach((k, v) -> {
            uriBuilder.addParameter(k, v);
        });

        URL url = uriBuilder.build().toURL();
        LOGGER.debug("URl for query {}", url);
        return url;
    }

    private List<BibEntry> parseXMl(Element element) {
        var list = element.getElementsByTagName("isidore");
        List<BibEntry> bibEntryList = new ArrayList<>();

        for (int i = 0; i < list.getLength(); i++) {
            Element elem = (Element) list.item(i);
            var bibEntry = xmlItemToBibEntry(elem);
            bibEntryList.add(bibEntry);
        }
        return bibEntryList;
    }

    private BibEntry xmlItemToBibEntry(Element itemElement) {
        return new BibEntry(getType(itemElement.getElementsByTagName("types").item(0).getChildNodes()))
                .withField(StandardField.TITLE, itemElement.getElementsByTagName("title").item(0).getTextContent().replace("\"", ""))
                .withField(StandardField.AUTHOR, getAuthor(itemElement.getElementsByTagName("enrichedCreators").item(0)))
                .withField(StandardField.YEAR, itemElement.getElementsByTagName("date").item(0).getChildNodes().item(1).getTextContent().substring(0, 4))
                .withField(StandardField.JOURNAL, getJournal(itemElement.getElementsByTagName("dc:source")))
                .withField(StandardField.PUBLISHER, getPublishers(itemElement.getElementsByTagName("publishers").item(0)))
                .withField(StandardField.DOI, getDOI(itemElement.getElementsByTagName("ore").item(0).getChildNodes()));
    }

    private String getDOI(NodeList list) {
        for (int i = 0; i < list.getLength(); i++) {
            String content = list.item(i).getTextContent();
            if (content.contains("DOI:")) {
                return content.replace("DOI: ", "");
            }
            if (list.item(i).getTextContent().contains("doi:")) {
                return content.replace("info:doi:", "");
            }
        }
        return "";
    }

    /**
     * Get the type of the document, ISIDORE only seems to have select types, also their types are different to
     * those used by JabRef.
     */
    private EntryType getType(NodeList list) {
        for (int i = 0; i < list.getLength(); i++) {
            String type = list.item(i).getTextContent();
            if (type.contains("article") || type.contains("Article")) {
                return StandardEntryType.Article;
            }
            if (type.contains("thesis") || type.contains("Thesis")) {
                return StandardEntryType.Thesis;
            }
            if (type.contains("book") || type.contains("Book")) {
                return StandardEntryType.Book;
            }
        }
        return StandardEntryType.Misc;
    }

    private String getAuthor(Node itemElement) {
        // Gets all the authors, separated with the word "and"
        // For some reason the author field sometimes has extra numbers and letters.
        StringJoiner stringJoiner = new StringJoiner(" and ");
        for (int i = 1; i < itemElement.getChildNodes().getLength(); i += 2) {
            String next = removeNumbers(itemElement.getChildNodes().item(i).getTextContent()).replaceAll("\\s+", " ");
            next = next.replace("\n", "");
            if (next.isBlank()) {
                continue;
            }
            stringJoiner.add(next);
        }
        return (stringJoiner.toString().substring(0, stringJoiner.length())).trim().replaceAll("\\s+", " ");
    }

    /**
     * Remove numbers from a string and everything after the number, (helps with the author field).
     */
    private String removeNumbers(String string) {
        for (int i = 0; i < string.length(); i++) {
            if (Character.isDigit(string.charAt(i))) {
                return string.substring(0, i);
            }
        }
        return string;
    }

    private String getPublishers(Node itemElement) {
        // In the XML file the publishers node often lists multiple publisher e.g.
        // <publisher origin="HAL CCSD">HAL CCSD</publisher>
        // <publisher origin="Elsevier">Elsevier</publisher>
        // Therefore this function simply gets all of them.
        if (itemElement == null) {
            return "";
        }
        StringJoiner stringJoiner = new StringJoiner(", ");
        for (int i = 0; i < itemElement.getChildNodes().getLength(); i++) {
            if (itemElement.getChildNodes().item(i).getTextContent().isBlank()) {
                continue;
            }
            stringJoiner.add(itemElement.getChildNodes().item(i).getTextContent().trim());
        }
        return stringJoiner.toString();
    }

    private String getJournal(NodeList list) {
        if (list.getLength() == 0) {
            return "";
        }
        String reference = list.item(list.getLength() - 1).getTextContent();
        for (int i = 0; i < reference.length(); i++) {
            if (reference.charAt(i) == ',') {
                return reference.substring(0, i);
            }
        }
        return "";
    }

    @Override
    public String getName() {
        return "ISIDORE";
    }

    @Override
    public Optional<HelpFile> getHelpPage() {
        return Optional.of(HelpFile.FETCHER_ISIDORE);
    }
}
