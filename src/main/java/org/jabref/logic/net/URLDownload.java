package org.jabref.logic.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jabref.Logger;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.util.FileHelper;

import com.mashape.unirest.http.Unirest;
import org.apache.commons.logging.LogFactory;

/**
 * URL download to a string.
 * <p>
 * Example:
 * URLDownload dl = new URLDownload(URL);
 * String content = dl.asString(ENCODING);
 * dl.toFile(Path); // available in FILE
 * String contentType = dl.getMimeType();
 *
 * Each call to a public method creates a new HTTP connection. Nothing is cached.
 */
public class URLDownload {
    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/56.0.2924.87 Safari/537.36";

    private final URL source;
    private final Map<String, String> parameters = new HashMap<>();
    private String postData = "";

    /**
     * @param source the URL to download from
     * @throws MalformedURLException if no protocol is specified in the source, or an unknown protocol is found
     */
    public URLDownload(String source) throws MalformedURLException {
        this(new URL(source));
    }

    /**
     * @param source The URL to download.
     */
    public URLDownload(URL source) {
        this.source = source;
        this.addHeader("User-Agent", URLDownload.USER_AGENT);
    }

    /**
     * Older java VMs does not automatically trust the zbMATH certificate. In this case the following exception is
     * thrown: sun.security.validator.ValidatorException: PKIX path building failed:
     * sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested
     * target JM > 8u101 may trust the certificate by default according to http://stackoverflow.com/a/34111150/873661
     *
     * We will fix this issue by accepting all (!) certificates. This is ugly; but as JabRef does not rely on
     * security-relevant information this is kind of OK (no, actually it is not...).
     *
     * Taken from http://stackoverflow.com/a/6055903/873661
     */
    public static void bypassSSLVerification() {
        LogFactory.getLog(URLDownload.class).warn("Fix SSL exceptions by accepting ALL certificates");

        // Create a trust manager that does not validate certificate chains
        TrustManager[] trustAllCerts = {new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }};

        // Install the all-trusting trust manager
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
        } catch (Exception e) {
            LogFactory.getLog(URLDownload.class).error("A problem occurred when bypassing SSL verification", e);
        }
    }

    public URL getSource() {
        return source;
    }

    public String getMimeType() throws IOException {
        Unirest.setDefaultHeader("User-Agent", "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6");

        String contentType;
        // Try to use HEAD request to avoid downloading the whole file
        try {
            contentType = Unirest.head(source.toString()).asString().getHeaders().get("Content-Type").get(0);
            if (contentType != null && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (Exception e) {
            Logger.debug(this, "Error getting MIME type of URL via HEAD request", e);
        }

        // Use GET request as alternative if no HEAD request is available
        try {
            contentType = Unirest.get(source.toString()).asString().getHeaders().get("Content-Type").get(0);
            if (contentType != null && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (Exception e) {
            Logger.debug(this, "Error getting MIME type of URL via GET request", e);
        }

        // Try to resolve local URIs
        try {
            URLConnection connection = new URL(source.toString()).openConnection();

            contentType = connection.getContentType();
            if (contentType != null && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (IOException e) {
            Logger.debug(this, "Error trying to get MIME type of local URI", e);
        }

        return "";
    }

    public boolean isMimeType(String type) throws IOException {
        String mime = getMimeType();

        if (mime.isEmpty()) {
            return false;
        }

        return mime.startsWith(type);
    }

    public boolean isPdf() throws IOException {
        return isMimeType("application/pdf");
    }

    public void addHeader(String key, String value) {
        this.parameters.put(key, value);
    }

    public void setPostData(String postData) {
        if (postData != null) {
            this.postData = postData;
        }
    }

    /**
     * Downloads the web resource to a String.
     *
     * @param encoding the desired String encoding
     * @return the downloaded string
     */
    public String asString(Charset encoding) throws IOException {
        try (InputStream input = new BufferedInputStream(this.openConnection().getInputStream());
             Writer output = new StringWriter()) {
            copy(input, output, encoding);
            return output.toString();
        }
    }

    /**
     * Downloads the web resource to a String.
     * Uses UTF-8 as encoding.
     *
     * @return the downloaded string
     */
    public String asString() throws IOException {
        return asString(StandardCharsets.UTF_8);
    }

    public List<HttpCookie> getCookieFromUrl() throws IOException {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        URLConnection con = this.openConnection();
        con.getHeaderFields(); // must be read to store the cookie

        try {
            return cookieManager.getCookieStore().get(this.source.toURI());
        } catch (URISyntaxException e) {
            Logger.error(this, "Unable to convert download URL to URI", e);
            return Collections.emptyList();
        }
    }

    /**
     * Downloads the web resource to a file.
     *
     * @param destination the destination file path.
     */
    public void toFile(Path destination) throws IOException {
        try (InputStream input = new BufferedInputStream(this.openConnection().getInputStream())) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            Logger.warn(this, "Could not copy input", e);
            throw e;
        }
    }

    /**
     * Takes the web resource as the source for a monitored input stream.
     */
    public ProgressInputStream asInputStream() throws IOException {
        URLConnection urlConnection = this.openConnection();
        long fileSize = urlConnection.getContentLength();
        return new ProgressInputStream(new BufferedInputStream(urlConnection.getInputStream()), fileSize);
    }

    /**
     * Downloads the web resource to a temporary file.
     *
     * @return the path of the temporary file.
     */
    public Path toTemporaryFile() throws IOException {
        // Determine file name and extension from source url
        String sourcePath = source.getPath();

        // Take everything after the last '/' as name + extension
        String fileNameWithExtension = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
        String fileName = FileUtil.getFileName(fileNameWithExtension);
        String extension = "." + FileHelper.getFileExtension(fileNameWithExtension).orElse("tmp");

        // Create temporary file and download to it
        Path file = Files.createTempFile(fileName, extension);
        toFile(file);

        return file;
    }

    @Override
    public String toString() {
        return "URLDownload{source=" + this.source + '}';
    }

    private void copy(InputStream in, Writer out, Charset encoding) throws IOException {
        InputStream monitoredInputStream = in;
        Reader r = new InputStreamReader(monitoredInputStream, encoding);
        try (BufferedReader read = new BufferedReader(r)) {

            String line;
            while ((line = read.readLine()) != null) {
                out.write(line);
                out.write("\n");
            }
        }
    }

    private URLConnection openConnection() throws IOException {
        URLConnection connection = this.source.openConnection();
        for (Entry<String, String> entry : this.parameters.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (!this.postData.isEmpty()) {
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(this.postData);
            }

        }

        if (connection instanceof HttpURLConnection) {
            // normally, 3xx is redirect
            int status = ((HttpURLConnection) connection).getResponseCode();
            if (status != HttpURLConnection.HTTP_OK) {
                if (status == HttpURLConnection.HTTP_MOVED_TEMP
                        || status == HttpURLConnection.HTTP_MOVED_PERM
                        || status == HttpURLConnection.HTTP_SEE_OTHER) {
                    // get redirect url from "location" header field
                    String newUrl = connection.getHeaderField("Location");
                    // open the new connnection again
                    connection = new URLDownload(newUrl).openConnection();
                }
            }
        }

        // this does network i/o: GET + read returned headers
        connection.connect();

        return connection;
    }

}
