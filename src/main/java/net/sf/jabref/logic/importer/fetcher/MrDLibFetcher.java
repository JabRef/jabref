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

    private static final String NAME = "Mr. DLib Fetcher";
    private static final Log LOGGER = LogFactory.getLog(MrDLibFetcher.class);


    public MrDLibFetcher() throws Exception {
    }

    @Override
    public String getName() {
        return NAME;
    }


    @Override
    public List<BibEntry> performSearch(BibEntry entry) throws FetcherException {
        BufferedReader response = makeServerRequest(entry.getLatexFreeField("title").get());
        MrDLibImporter importer = new MrDLibImporter();
        BibDatabase bibDatabase = new BibDatabase();
        try {
            if (importer.isRecognizedFormat(response)) {
                importer.importDatabase(response);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return bibDatabase.getEntries();
    }

    /**
     * Contact the server with the title of the selected item
     * @param query
     * @return
     */
    private BufferedReader makeServerRequest(String query) {
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
            }

            //Conversion. Server delivers false format, conversion here, TODO to fix
            response = response.replaceAll("&gt;", ">");
            response = response.replaceAll("&lt;", "<");
        } catch (IOException e1) {
            LOGGER.error(e1.getMessage(), e1);
        }
        return new BufferedReader(new StringReader(response));
    }

    private String constructQuery(String query) {
        StringBuffer queryBuffer = new StringBuffer();
        queryBuffer.append("https://api-dev.mr-dlib.org/v1/documents/");
        queryBuffer.append(query);
        queryBuffer.append("/related_documents/");
        queryBuffer.append("version");
        System.out.println("querybuffer: " + queryBuffer.toString());
        //return queryBuffer.toString();
        return "https://api-dev.mr-dlib.org/v1/documents/gesis-smarth-0000003284/related_documents/";
    }



}
