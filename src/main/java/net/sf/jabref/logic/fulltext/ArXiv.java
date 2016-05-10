package net.sf.jabref.logic.fulltext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at arXiv.
 *
 * @see http://arxiv.org/help/api/index
 */
public class ArXiv implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(ArXiv.class);

    private static final String API_URL = "http://export.arxiv.org/api/query";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // 1. DOI
        Optional<DOI> doi = DOI.build(entry.getField("doi"));
        // 2. Eprint
        String eprint = entry.getField("eprint");

        if (doi.isPresent()) {
            String doiString = doi.get().getDOI();
            // Available in catalog?
            try {
                Document doc = queryApi(doiString);

                NodeList nodes = doc.getElementsByTagName("arxiv:doi");
                Node doiTag = nodes.item(0);

                if ((doiTag != null) && doiTag.getTextContent().equals(doiString)) {
                    // Lookup PDF link
                    NodeList links = doc.getElementsByTagName("link");

                    for (int i = 0; i < links.getLength(); i++) {
                        Node link = links.item(i);
                        NamedNodeMap attr = link.getAttributes();
                        String rel = attr.getNamedItem("rel").getNodeValue();
                        String href = attr.getNamedItem("href").getNodeValue();

                        if ("related".equals(rel) && "pdf".equals(attr.getNamedItem("title").getNodeValue())) {
                            pdfLink = Optional.of(new URL(href));
                            LOGGER.info("Fulltext PDF found @ arXiv.");
                        }
                    }
                }
            } catch (UnirestException | ParserConfigurationException | SAXException | IOException e) {
                LOGGER.warn("arXiv DOI API request failed", e);
            }
        } else if (eprint != null && !eprint.isEmpty()) {
            try {
                // only lookup on id field
                Document doc = queryApi("id:" + eprint);

                // Lookup PDF link
                NodeList links = doc.getElementsByTagName("link");

                for (int i = 0; i < links.getLength(); i++) {
                    Node link = links.item(i);
                    NamedNodeMap attr = link.getAttributes();
                    String rel = attr.getNamedItem("rel").getNodeValue();
                    String href = attr.getNamedItem("href").getNodeValue();

                    if ("related".equals(rel) && "pdf".equals(attr.getNamedItem("title").getNodeValue())) {
                        pdfLink = Optional.of(new URL(href));
                        LOGGER.info("Fulltext PDF found @ arXiv.");
                    }
                }
            } catch (UnirestException | ParserConfigurationException | SAXException | IOException e) {
                LOGGER.warn("arXiv eprint API request failed", e);
            }
        }
        return pdfLink;
    }

    private Document queryApi(String query) throws SAXException, ParserConfigurationException, IOException, UnirestException {
        HttpResponse<InputStream> response = Unirest.get(API_URL)
                .queryString("search_query", query)
                .queryString("max_results", 1)
                .asBinary();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        return builder.parse(response.getBody());
    }
}
