package org.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jabref.logic.importer.EntryBasedFetcher;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.WorldCatImporter;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.BuildInfo;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * EntryBasedFetcher that searches the WorldCat database
 *
 * @see <a href="https://www.oclc.org/developer/develop/web-services/worldcat-search-api/bibliographic-resource.en.html">Worldcat
 * API documentation</a>
 */
public class WorldCatFetcher implements EntryBasedFetcher {

    private String WORLDCAT_OPEN_SEARCH_URL = "https://www.worldcat.org/webservices/catalog/search/opensearch?wskey=";
    private String WORLDCAT_READ_URL = "https://www.worldcat.org/webservices/catalog/content/{OCLC-NUMBER}?recordSchema=info%3Asrw%2Fschema%2F1%2Fdc&wskey=";

    public WorldCatFetcher(String worldCatKey) {
        if (StringUtil.isBlank(worldCatKey)) {
            worldCatKey = new BuildInfo().worldCatAPIKey;
        }
        WORLDCAT_OPEN_SEARCH_URL += worldCatKey;
        WORLDCAT_READ_URL += worldCatKey;
    }

    @Override
    public String getName() {
        return "Worldcat Fetcher";
    }

    /**
     * Create a open search query with specified title
     *
     * @param title the title to include in the query
     * @return the earch query for the api
     */
    private String getOpenSearchURL(String title) throws MalformedURLException {
        String query = "&q=srw.ti+all+" + URLEncoder.encode("\"" + title + "\"", StandardCharsets.UTF_8);
        URL url = new URL(WORLDCAT_OPEN_SEARCH_URL + query);
        return url.toString();
    }

    /**
     * Make request to open search API of WorlCcat, with specified title
     *
     * @param title the title of the search
     * @return the body of the HTTP response
     */
    private String makeOpenSearchRequest(String title) throws FetcherException {
        try {
            URLDownload urlDownload = new URLDownload(getOpenSearchURL(title));
            URLDownload.bypassSSLVerification();
            return urlDownload.asString();
        } catch (MalformedURLException e) {
            throw new FetcherException("Bad url", e);
        } catch (IOException e) {
            throw new FetcherException("Error with Open Search Request (Worldcat)", e);
        }
    }

    /**
     * Get more information about a article through its OCLC id. Picks the first element with this tag
     *
     * @param id the oclc id
     * @return the Node of the XML element that contains all tags
     */
    private Node getSpecificInfoOnOCLC(String id) throws IOException {
        URLDownload urlDownload = new URLDownload(WORLDCAT_READ_URL.replace("{OCLC-NUMBER}", id));
        URLDownload.bypassSSLVerification();
        String resp = urlDownload.asString();

        Document mainDoc = parse(resp);

        return mainDoc.getElementsByTagName("oclcdcs").item(0);
    }

    /**
     * Parse a string to an xml document
     *
     * @param s the string to be parsed
     * @return XML document representing the content of s
     * @throws IllegalArgumentException if s is badly formatted or other exception occurs during parsing
     */
    private Document parse(String s) throws IOException {
        try (BufferedReader r = new BufferedReader(new StringReader(s))) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            return builder.parse(new InputSource(r));
        } catch (ParserConfigurationException e) {
            throw new IOException("Parser Config Exception", e);
        } catch (SAXException e) {
            throw new IOException("SAX Exception", e);
        }
    }

    /**
     * Parse OpenSearch XML to retrieve OCLC-ids and fetch details about those. Returns new XML doc in the form
     * <pre>
     * {@literal <entries>}
     *      {@literal <entry>}
     *          OCLC DETAIL
     *      {@literal </entry>}
     *      ...
     * {@literal </entries>}
     * </pre>
     *
     * @param doc the open search result
     * @return the new xml document
     * @throws FetcherException if the fetcher fails to parse the result
     */
    private Document parseOpenSearchXML(Document doc) throws FetcherException {
        try {
            Element feed = (Element) doc.getElementsByTagName("feed").item(0);
            NodeList entryXMLList = feed.getElementsByTagName("entry");

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            Document newDoc = docBuilder.newDocument();

            Element root = newDoc.createElement("entries");
            newDoc.appendChild(root);
            for (int i = 0; i < entryXMLList.getLength(); i++) {
                Element xmlEntry = (Element) entryXMLList.item(i);

                String oclc = xmlEntry.getElementsByTagName("oclcterms:recordIdentifier").item(0).getTextContent();
                Element detailedInfo = (Element) newDoc.importNode(getSpecificInfoOnOCLC(oclc), true);

                Element newEntry = newDoc.createElement("entry");
                newEntry.appendChild(detailedInfo);

                root.appendChild(newEntry);
            }
            return newDoc;
        } catch (ParserConfigurationException e) {
            throw new FetcherException("Error with XML creation (WorldCat fetcher)", e);
        } catch (IOException e) {
            throw new FetcherException("Error with OCLC parsing (WorldCat fetcher)", e);
        }
    }

    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        Optional<String> entryTitle = entry.getLatexFreeField(StandardField.TITLE);
        if (entryTitle.isEmpty()) {
            return Collections.emptyList();
        }

        String openSearchXMLResponse = makeOpenSearchRequest(entryTitle.get());
        Document openSearchDocument = null;
        try {
            openSearchDocument = parse(openSearchXMLResponse);
        } catch (IOException e) {
            throw new FetcherException("Could not parse response", e);
        }

        Document detailedXML = parseOpenSearchXML(openSearchDocument);
        String detailedXMLString;
        try (StringWriter sw = new StringWriter()) {
            // Transform XML to String
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            t.transform(new DOMSource(detailedXML), new StreamResult(sw));
            detailedXMLString = sw.toString();
        } catch (TransformerException e) {
            throw new FetcherException("Could not transform XML", e);
        } catch (IOException e) {
            throw new FetcherException("Could not close StringWriter", e);
        }

        WorldCatImporter importer = new WorldCatImporter();
        try {
            if (importer.isRecognizedFormat(detailedXMLString)) {
                ParserResult parserResult = importer.importDatabase(detailedXMLString);
                return parserResult.getDatabase().getEntries();
            } else {
                return Collections.emptyList();
            }
        } catch (IOException e) {
            throw new FetcherException("Could not perform search (WorldCat) ", e);
        }
    }
}
