package net.sf.jabref.logic.fetcher;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.sf.jabref.model.entry.BibtexEntry;
import net.sf.jabref.logic.util.DOI;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

/**
 * FullTextFinder implementation that attempts to find a PDF URL at arXiv.
 *
 * @see http://arxiv.org/help/api/index
 */
public class ArXiv implements FullTextFinder {
    private static final Log LOGGER = LogFactory.getLog(ArXiv.class);

    private static final String API_URL = "http://export.arxiv.org/api/query";

    @Override
    public Optional<URL> findFullText(BibtexEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // Try unique DOI first
        Optional<DOI> doi = DOI.build(entry.getField("doi"));

        if(doi.isPresent()) {
            String doiString = doi.get().getDOI();
            // Available in catalog?
            try {
                HttpResponse<InputStream> response = Unirest.get(API_URL)
                        .queryString("search_query", doiString)
                        .queryString("max_results", 1)
                        .asBinary();

                // Xml response
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(response.getBody());

                NodeList nodes = doc.getElementsByTagName("arxiv:doi");
                Node doiTag = nodes.item(0);

                if(doiTag != null) {
                    if(doiTag.getTextContent().equals(doiString)) {
                        // Lookup PDF link
                        NodeList links = doc.getElementsByTagName("link");

                        for (int i = 0; i < links.getLength(); i++) {
                            Node link = links.item(i);
                            NamedNodeMap attr = link.getAttributes();
                            String rel = attr.getNamedItem("rel").getNodeValue();
                            String href = attr.getNamedItem("href").getNodeValue();

                            if (rel.equals("related") && attr.getNamedItem("title").getNodeValue().equals("pdf")) {
                                pdfLink = Optional.of(new URL(href));
                                LOGGER.info("Fulltext PDF found @ arXiv.");
                            }
                        }
                    }
                }
            } catch(UnirestException | ParserConfigurationException | SAXException e) {
                LOGGER.warn("arXiv API request failed: " + e.getMessage());
            }
        }
        // TODO: title search
        // We can also get abstract automatically!
        return pdfLink;
    }
}
