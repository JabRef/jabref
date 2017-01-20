/**
 *
 */
package net.sf.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;

import net.sf.jabref.logic.importer.EntryBasedFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.MrDLibImporter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;

/**
 *  This class is responible to get the recommendations from MDL
 *
 */
public class MrDLibFetcher implements EntryBasedFetcher {

    private static final String NAME = "MDL_FETCHER";
    private static final Log LOGGER = LogFactory.getLog(MrDLibFetcher.class);
    private final String LANGUAGE;
    private final String VERSION;


    public MrDLibFetcher(String language, String version) {
        LANGUAGE = language;
        VERSION = version;
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        String response = makeServerRequest(entry.getLatexFreeField(FieldName.TITLE).get());
        MrDLibImporter importer = new MrDLibImporter();
        ParserResult parserResult = new ParserResult();
        try {
            if (importer.isRecognizedFormat(new BufferedReader(new StringReader(response)))) {
                parserResult = importer.importDatabase(new BufferedReader(new StringReader(response)));
            } else {
                // For displaying An ErrorMessage
                BibEntry errorBibEntry = new BibEntry();
                errorBibEntry.setField("html_representation",
                        Localization.lang("Error_while_fetching_from_%0", "Mr.DLib"));
                BibDatabase errorBibDataBase = new BibDatabase();
                errorBibDataBase.insertEntry(errorBibEntry);
                parserResult = new ParserResult(errorBibDataBase);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
            throw new FetcherException("XML Parser IOException.");
        }
        return parserResult.getDatabase().getEntries();
    }

    /**
     * Contact the server with the title of the selected item
     * @param query: The query holds the title of the selected entry. Used to make a query to the MDL Server
     * @return Returns the server response. This is an XML document as a String.
     * @throws FetcherException
     */
    private String makeServerRequest(String query) throws FetcherException {
        query = constructQuery(query);
        String response = "";
        SSLContext sslContext = null;

        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new FetcherException("SSL Error.");
        }

        SSLConnectionSocketFactory sslSocketFacktory = new SSLConnectionSocketFactory(sslContext);

        try (CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslSocketFacktory).build()) {
            Unirest.setHttpClient(httpclient);
            try {
                response = Unirest.get(query).asString().getBody();
            } catch (UnirestException e) {
                LOGGER.error(e.getMessage(), e);
                throw new FetcherException(e.getMessage(), Localization.lang("Error_while_fetching_from_%0", "Mr.DLib"),
                        e);
            }
            //Conversion of < and >
            response = response.replaceAll("&gt;", ">");
            response = response.replaceAll("&lt;", "<");
        } catch (IOException e1) {
            LOGGER.error(e1.getMessage(), e1);
            throw new FetcherException("IOException by trying to get HTTP response.");
        }
        return response;
    }

    /**
     * Constructs the query based on title of the bibentry. Adds statistical stuff to the url.
     * @param query: the title of the bib entry.
     * @return the string used to make the query at mdl server
     */
    private String constructQuery(String query) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append("http://api-dev.mr-dlib.org/v1/documents/");
        try {
            queryBuffer.append(URLEncoder.encode(query, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e.getMessage(), e);
        }
        queryBuffer.append("/related_documents?");
        queryBuffer.append("partner_id=jabref");
        queryBuffer.append("&app_id=jabref_desktop");
        queryBuffer.append("&app_version=" + VERSION);
        queryBuffer.append("&app_lang=" + LANGUAGE);
        return queryBuffer.toString();
    }



}
