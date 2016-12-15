/**
 *
 */
package net.sf.jabref.logic.importer.fetcher;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import javax.net.ssl.SSLContext;

import net.sf.jabref.logic.importer.EntryBasedFetcher;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.MrDLibImporter;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

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
 *
 *
 */
public class MrDLibFetcher implements EntryBasedFetcher {

    private static final String NAME = "MDL_FETCHER";
    private static final Log LOGGER = LogFactory.getLog(MrDLibFetcher.class);




    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        String response = makeServerRequest(entry.getLatexFreeField("title").get());
        MrDLibImporter importer = new MrDLibImporter();
        ParserResult parserResult = new ParserResult();
        try {
            if (importer.isRecognizedFormat(new BufferedReader(new StringReader(response)))) {
                parserResult = importer.importDatabase(new BufferedReader(new StringReader(response)));
            } else {
                // For displaying An ErrorMessage
                BibEntry errorBibEntry = new BibEntry();
                errorBibEntry.setField("html_representation", "An error occured. We are sorry for that.");
                BibDatabase errorBibDataBase = new BibDatabase();
                errorBibDataBase.insertEntry(errorBibEntry);
                parserResult = new ParserResult(errorBibDataBase);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return parserResult.getDatabase().getEntries();
    }

    /**
     * Contact the server with the title of the selected item
     * @param query
     * @return
     */
    private String makeServerRequest(String query) {
        query = constructQuery(query);
        String response = "";

        //Makes a request to the RESTful MDL-API. Example document.
        //Servers-side functionality in implementation.

        SSLContext sslContext = null;

        try {
            sslContext = SSLContexts.custom().loadTrustMaterial(null, new TrustSelfSignedStrategy()).build();
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            LOGGER.error(e1.getMessage(), e1);
        }

        SSLConnectionSocketFactory sslSocketFacktory = new SSLConnectionSocketFactory(sslContext);

        try (CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslSocketFacktory).build()) {
            Unirest.setHttpClient(httpclient);
            try {
                response = Unirest.get(query).asString().getBody();
            } catch (UnirestException e) {
                LOGGER.error(e.getMessage(), e);
                return "could not get a response.";
            }

            //Conversion. Server delivers false format, conversion here, TODO to fix
            response = response.replaceAll("&gt;", ">");
            response = response.replaceAll("&lt;", "<");
        } catch (IOException e1) {
            LOGGER.error(e1.getMessage(), e1);
        }
        return response;
    }

    /**
     * Constructs the query based on title of the bibentry. Adds statistical stuff to the url.
     * @param query: the title of the bib entry.
     * @return
     */
    private String constructQuery(String query) {
        StringBuffer queryBuffer = new StringBuffer();
        //queryBuffer.append("https://api-dev.mr-dlib.org/v1/documents/");
        queryBuffer.append("http://localhost:8080/mdl-server/documents/");
        queryBuffer.append(query.replaceAll(" ", "_"));
        queryBuffer.append("/related_documents?");
        queryBuffer.append("partner_id=jabref");
        queryBuffer.append("&app_id=jabref_desktop");
        queryBuffer.append("&app_version=" + "42.3.1415");//todo: where to find
        queryBuffer.append("&app_lang=" + "selectedLanguage");//todo: where to find
        return queryBuffer.toString();
        //return "https://api-dev.mr-dlib.org/v1/documents/gesis-smarth-0000009273/related_documents/";
    }



}
