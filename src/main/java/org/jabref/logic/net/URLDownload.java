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
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

import org.jabref.http.dto.SimpleHttpResponse;
import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.util.io.FileUtil;

import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * URL download to a string.
 * <p>
 * Example:
 * <code>
 * URLDownload dl = new URLDownload(URL);
 * String content = dl.asString(ENCODING);
 * dl.toFile(Path); // available in FILE
 * String contentType = dl.getMimeType();
 * </code>
 * <br/><br/>
 * Almost each call to a public method creates a new HTTP connection (except for {@link #asString(Charset, URLConnection) asString},
 * which uses an already opened connection). Nothing is cached.
 */
public class URLDownload {

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36";
    private static final Logger LOGGER = LoggerFactory.getLogger(URLDownload.class);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);

    private final URL source;
    private final Map<String, String> parameters = new HashMap<>();
    private String postData = "";
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;

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
     * @param socketFactory trust manager
     * @param verifier      host verifier
     */
    public static void setSSLVerification(SSLSocketFactory socketFactory, HostnameVerifier verifier) {
        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(socketFactory);
            HttpsURLConnection.setDefaultHostnameVerifier(verifier);
        } catch (Exception e) {
            LOGGER.error("A problem occurred when reset SSL verification", e);
        }
    }

    public URL getSource() {
        return source;
    }

    public String getMimeType() {
        Unirest.config().setDefaultHeader("User-Agent", "Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6");

        String contentType;
        // Try to use HEAD request to avoid downloading the whole file
        try {
            contentType = Unirest.head(source.toString()).asString().getHeaders().get("Content-Type").getFirst();
            if ((contentType != null) && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via HEAD request", e);
        }

        // Use GET request as alternative if no HEAD request is available
        try {
            contentType = Unirest.get(source.toString()).asString().getHeaders().get("Content-Type").getFirst();
            if ((contentType != null) && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via GET request", e);
        }

        // Try to resolve local URIs
        try {
            URLConnection connection = new URL(source.toString()).openConnection();

            contentType = connection.getContentType();
            if ((contentType != null) && !contentType.isEmpty()) {
                return contentType;
            }
        } catch (IOException e) {
            LOGGER.debug("Error trying to get MIME type of local URI", e);
        }

        return "";
    }

    /**
     * Check the connection by using the HEAD request.
     * UnirestException can be thrown for invalid request.
     *
     * @return the status code of the response
     */
    public boolean canBeReached() throws UnirestException {
        // new unirest version does not support apache http client any longer
        Unirest.config().reset()
               .followRedirects(true)
               .enableCookieManagement(true)
               .setDefaultHeader("User-Agent", USER_AGENT);

        int statusCode = Unirest.head(source.toString()).asString().getStatus();
        return (statusCode >= 200) && (statusCode < 300);
    }

    public boolean isMimeType(String type) {
        String mime = getMimeType();

        if (mime.isEmpty()) {
            return false;
        }

        return mime.startsWith(type);
    }

    public boolean isPdf() {
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
     * Downloads the web resource to a String. Uses UTF-8 as encoding.
     *
     * @return the downloaded string
     */
    public String asString() throws FetcherException {
        return asString(StandardCharsets.UTF_8, this.openConnection());
    }

    /**
     * Downloads the web resource to a String.
     *
     * @param encoding the desired String encoding
     * @return the downloaded string
     */
    public String asString(Charset encoding) throws FetcherException {
        return asString(encoding, this.openConnection());
    }

    /**
     * Downloads the web resource to a String from an existing connection. Uses UTF-8 as encoding.
     *
     * @param existingConnection an existing connection
     * @return the downloaded string
     */
    public static String asString(URLConnection existingConnection) throws FetcherException {
        return asString(StandardCharsets.UTF_8, existingConnection);
    }

    /**
     * Downloads the web resource to a String.
     *
     * @param encoding   the desired String encoding
     * @param connection an existing connection
     * @return the downloaded string
     */
    public static String asString(Charset encoding, URLConnection connection) throws FetcherException {
        try (InputStream input = new BufferedInputStream(connection.getInputStream());
             Writer output = new StringWriter()) {
            copy(input, output, encoding);
            return output.toString();
        } catch (IOException e) {
            throw new FetcherException("Error downloading", e);
        }
    }

    public List<HttpCookie> getCookieFromUrl() throws FetcherException {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        URLConnection con = this.openConnection();
        con.getHeaderFields(); // must be read to store the cookie

        try {
            return cookieManager.getCookieStore().get(this.source.toURI());
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to convert download URL to URI", e);
            return Collections.emptyList();
        }
    }

    /**
     * Downloads the web resource to a file.
     *
     * @param destination the destination file path.
     */
    public void toFile(Path destination) throws FetcherException {
        try (InputStream input = new BufferedInputStream(this.openConnection().getInputStream())) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not copy input", e);
            throw new FetcherException("Could not copy input", e);
        }
    }

    /**
     * Takes the web resource as the source for a monitored input stream.
     */
    public ProgressInputStream asInputStream() throws FetcherException {
        HttpURLConnection urlConnection = (HttpURLConnection) this.openConnection();

        int responseCode;
        try {
            responseCode = urlConnection.getResponseCode();
        } catch (IOException e) {
            throw new FetcherException("Error getting response code", e);
        }
        LOGGER.debug("Response code: {}", responseCode); // We could check for != 200, != 204
        if (responseCode >= 300) {
            SimpleHttpResponse simpleHttpResponse = new SimpleHttpResponse(urlConnection);
            LOGGER.error("Failed to read from url: {}", simpleHttpResponse);
            throw FetcherException.of(this.source, simpleHttpResponse);
        }
        long fileSize = urlConnection.getContentLengthLong();
        InputStream inputStream;
        try {
            inputStream = urlConnection.getInputStream();
        } catch (IOException e) {
            throw new FetcherException("Error getting input stream", e);
        }
        return new ProgressInputStream(new BufferedInputStream(inputStream), fileSize);
    }

    /**
     * Downloads the web resource to a temporary file.
     *
     * @return the path of the temporary file.
     */
    public Path toTemporaryFile() throws FetcherException {
        // Determine file name and extension from source url
        String sourcePath = source.getPath();

        // Take everything after the last '/' as name + extension
        String fileNameWithExtension = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
        String fileName = "jabref-" + FileUtil.getBaseName(fileNameWithExtension);
        String extension = "." + FileUtil.getFileExtension(fileNameWithExtension).orElse("tmp");

        // Create temporary file and download to it
        Path file = null;
        try {
            file = Files.createTempFile(fileName, extension);
        } catch (IOException e) {
            throw new FetcherException("Could not create temporary file", e);
        }
        file.toFile().deleteOnExit();
        toFile(file);

        return file;
    }

    @Override
    public String toString() {
        return "URLDownload{" + "source=" + this.source + '}';
    }

    private static void copy(InputStream in, Writer out, Charset encoding) throws IOException {
        Reader r = new InputStreamReader(in, encoding);
        try (BufferedReader read = new BufferedReader(r)) {
            String line;
            while ((line = read.readLine()) != null) {
                out.write(line);
                out.write("\n");
            }
        }
    }

    /**
     * Open a connection to this object's URL (with specified settings).
     * <p>
     * If accessing an HTTP URL, remeber to close the resulting connection after usage.
     *
     * @return an open connection
     */
    public URLConnection openConnection() throws FetcherException {
        URLConnection connection;
        try {
            connection = getUrlConnection();
        } catch (IOException e) {
            throw new FetcherException("Error opening connection", e);
        }

        if (connection instanceof HttpURLConnection httpURLConnection) {
            int status;
            try {
                // this does network i/o: GET + read returned headers
                status = httpURLConnection.getResponseCode();
            } catch (IOException e) {
                LOGGER.error("Error getting response code", e);
                throw new FetcherException("Error getting response code", e);
            }

            if ((status == HttpURLConnection.HTTP_MOVED_TEMP)
                    || (status == HttpURLConnection.HTTP_MOVED_PERM)
                    || (status == HttpURLConnection.HTTP_SEE_OTHER)) {
                // get redirect url from "location" header field
                String newUrl = connection.getHeaderField("location");
                // open the new connection again
                try {
                    connection = new URLDownload(newUrl).openConnection();
                } catch (MalformedURLException e) {
                    throw new FetcherException("Could not open URL Download", e);
                }
            } else if (status >= 400) {
                // in case of an error, propagate the error message
                SimpleHttpResponse httpResponse = new SimpleHttpResponse(httpURLConnection);
                LOGGER.info("{}", httpResponse);
                if ((status >= 400) && (status < 500)) {
                    throw new FetcherClientException(this.source, httpResponse);
                } else if (status >= 500) {
                    throw new FetcherServerException(this.source, httpResponse);
                }
            }
        }
        return connection;
    }

    private URLConnection getUrlConnection() throws IOException {
        URLConnection connection = this.source.openConnection();
        connection.setConnectTimeout((int) connectTimeout.toMillis());
        for (Entry<String, String> entry : this.parameters.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (!this.postData.isEmpty()) {
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(this.postData);
            }
        }
        return connection;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        if (connectTimeout != null) {
            this.connectTimeout = connectTimeout;
        }
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }
}
