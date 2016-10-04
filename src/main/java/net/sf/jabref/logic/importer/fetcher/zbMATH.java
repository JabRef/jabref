package net.sf.jabref.logic.importer.fetcher;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import net.sf.jabref.logic.cleanup.MoveFieldCleanup;
import net.sf.jabref.logic.formatter.bibtexfields.RemoveBracesFormatter;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.Parser;
import net.sf.jabref.logic.importer.SearchBasedParserFetcher;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.cleanup.FieldFormatterCleanup;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

/**
 * Fetches data from the Zentralblatt Math (https://www.zbmath.org/)
 */
public class zbMATH implements SearchBasedParserFetcher {

    private static final Log LOGGER = LogFactory.getLog(zbMATH.class);

    private final ImportFormatPreferences preferences;

    public zbMATH(ImportFormatPreferences preferences) {
        this.preferences = Objects.requireNonNull(preferences);
    }

    @Override
    public String getName() {
        return "zbMATH";
    }

    /**
     * TODO: Implement EntryBasedParserFetcher
     * We use the zbMATH Citation matcher (https://www.zbmath.org/citationmatching/)
     * instead of the usual search since this tool is optimized for finding a publication based on partial information.
     */
    /*
    @Override
    public URL getURLForEntry(BibEntry entry) throws URISyntaxException, MalformedURLException, FetcherException {
        // Example: https://zbmath.org/citationmatching/match?q=Ratiu
    }
    */

    @Override
    public URL getURLForQuery(String query) throws URISyntaxException, MalformedURLException, FetcherException {
        URIBuilder uriBuilder = new URIBuilder("https://zbmath.org/bibtexoutput/");
        uriBuilder.addParameter("q", query); // search all fields
        uriBuilder.addParameter("start", "0"); // start index
        uriBuilder.addParameter("count", "200"); // should return up to 200 items (instead of default 100)

        fixSSLVerification();

        return uriBuilder.build().toURL();
    }

    /**
     * Older java VMs does not automatically trust the zbMATH certificate. In this case the following exception is thrown:
     *  sun.security.validator.ValidatorException: PKIX path building failed:
     *  sun.security.provider.certpath.SunCertPathBuilderException: unable to find
     *  valid certification path to requested target
     * JM > 8u101 may trust the certificate by default according to http://stackoverflow.com/a/34111150/873661
     *
     * We will fix this issue by accepting all (!) certificates. This is ugly; but as JabRef does not rely on
     * security-relevant information this is kind of OK (no, actually it is not...).
     *
     * Taken from http://stackoverflow.com/a/6055903/873661
     */
    private void fixSSLVerification() {

        LOGGER.warn("Fix SSL exception by accepting ALL certificates");

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        } };

        // Install the all-trusting trust manager
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (Exception e) {
            LOGGER.error("SSL problem", e);
        }
    }

    @Override
    public Parser getParser() {
        return new BibtexParser(preferences);
    }

    @Override
    public void doPostCleanup(BibEntry entry) {
        new MoveFieldCleanup("msc2010", FieldName.KEYWORDS).cleanup(entry);
        new MoveFieldCleanup("fjournal", FieldName.JOURNAL).cleanup(entry);
        new FieldFormatterCleanup(FieldName.JOURNAL, new RemoveBracesFormatter()).cleanup(entry);
        new FieldFormatterCleanup(FieldName.TITLE, new RemoveBracesFormatter()).cleanup(entry);
    }

}
