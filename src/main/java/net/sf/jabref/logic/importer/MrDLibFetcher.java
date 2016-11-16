/**
 *
 */
package net.sf.jabref.logic.importer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.jabref.model.entry.BibEntry;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 *
 */
public class MrDLibFetcher implements SearchBasedFetcher {

    private static final String NAME = "Mr. DLib Fetcher";
    private List<BibEntry> bibEntryList;
    private final BibEntry selectedEntry;
    private List<String> htmlSnippets;


    public MrDLibFetcher(BibEntry selectedEntry) throws Exception {
        this.selectedEntry = selectedEntry;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public List<BibEntry> performSearch(String query) throws FetcherException {
        String response = makeServerRequest(formatTitleFromBibEntry(selectedEntry));
        bibEntryList = convertToBibEntry(response);
        htmlSnippets = convertToHtml(response);
        return bibEntryList;
    }

    private String formatTitleFromBibEntry(BibEntry selectedEntry) {
        return selectedEntry.getField("title").toString().replaceAll("\\{|\\[|\\]|\\}|(Optional)", "");
    }

    /**
     * Contact the server with the title of the selected item
     * @param query
     * @return
     */
    private String makeServerRequest(String query) {
        String response = "";
        //Makes a request to the RESTful MDL-API. Example document.
        //Servers-side functionality in implementation.

        //Returns true, accepts all certificates. To change.
        HostnameVerifier allHostsValid = new HostnameVerifier() {

            @Override
            public boolean verify(String hostname, SSLSession session) {
                // TODO Auto-generated method stub
                return true;
            }
        };

        SSLContext sslcontext = null;

        try {
            sslcontext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }


        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
        try (CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build()) {
        Unirest.setHttpClient(httpclient);

        try {
            response = Unirest
                        .get("https://api-dev.mr-dlib.org/v1/documents/gesis-smarth-0000000300/related_documents/")
                    .asString().getBody();
        } catch (UnirestException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        //Conversion. Server delivers false format, conversion here, TODO to fix
        response = response.replaceAll("&gt;", ">");
        response = response.replaceAll("&lt;", "<");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return response;
    }

    /**
     * Converts the xml document responded by the mdl server to an bib entry. Is there a parser somewhere?
     * @param xmlDocument
     * @return
     */
    private List<BibEntry> convertToBibEntry(String xmlDocument) {
        // Parser stuff goes here todo
        bibEntryList = null;
        return bibEntryList;
    }

    /**
     * Converts the xmlDocument to htmlSnippets using a SaxParser
     * @param xmlDocument
     * @return
     */
    private List<String> convertToHtml(String xmlDocument) {
        ArrayList<RankedHtmlSnippet> rankedHtmlSnippet = new ArrayList<>();
        //Parsing the response with a SAX parser
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            DefaultHandler handler = new DefaultHandler() {

                int position = 1;

                boolean snippet = false;
                boolean rank = false;
                boolean start = false;
                String htmlSnippetSingle = null;
                int htmlSnippetSingleRank = -1;


                @Override
                public void startElement(String uri, String localName, String qName, Attributes attributes)
                        throws SAXException {

                    if (qName.equalsIgnoreCase("related_article")) {
                        htmlSnippetSingle = null;
                        htmlSnippetSingleRank = -1;
                    }

                    if (qName.equalsIgnoreCase("snippet")) {
                        if (attributes.getValue(0).equalsIgnoreCase("html_fully_formatted")) {
                            snippet = true;
                        }
                    }
                    if (qName.equalsIgnoreCase("suggested_rank")) {
                        rank = true;
                    }
                }

                @Override
                public void endElement(String uri, String localName, String qName) throws SAXException {
                    if (qName.equalsIgnoreCase("related_article")) {
                        rankedHtmlSnippet.add(new RankedHtmlSnippet(htmlSnippetSingle, htmlSnippetSingleRank));
                    }
                }

                @Override
                public void characters(char ch[], int start, int length) throws SAXException {

                    if (rank) {
                        htmlSnippetSingleRank = Integer.parseInt(new String(ch, start, length));
                        rank = false;
                    }

                    if (snippet) {
                        htmlSnippetSingle = new String(ch, start, length);
                        snippet = false;
                    }

                }

            };

            try {
                InputStream stream = new ByteArrayInputStream(xmlDocument.getBytes());
                saxParser.parse(stream, handler);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            rankedHtmlSnippet.sort(new Comparator<RankedHtmlSnippet>() {

                @Override
                public int compare(RankedHtmlSnippet o1, RankedHtmlSnippet o2) {
                    return o1.rank.compareTo(o2.rank);
                }
            });
            htmlSnippets = rankedHtmlSnippet.stream().map(e -> e.snippet).collect(Collectors.toList());
        } catch (ParserConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SAXException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return htmlSnippets;
    }


    /**
     *
     * small class to ensure the ranking is correct
     *
     */
    private class RankedHtmlSnippet {

        public String snippet;
        public Integer rank;

        public RankedHtmlSnippet(String snippet, Integer rank) {
            this.rank = rank;
            this.snippet = snippet;
        }
    }


    /**
    * Returns null until {@link StateValue#DONE} was fired.
    * @return The recommendations as BibEntries
    */
    public List<BibEntry> getRecommendationsAsBibEntryList() {
        return bibEntryList;
    }

    /**
     * Returns null until {@link StateValue#DONE} was fired.
     * @return The recommendations as HTML elements, sorted by descending score.
     */
    public List<String> getRecommendationsAsHTML() {
        return htmlSnippets;
    }

}
