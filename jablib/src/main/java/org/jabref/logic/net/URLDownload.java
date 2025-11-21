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
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

import org.jabref.logic.importer.FetcherClientException;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FetcherServerException;
import org.jabref.logic.importer.ImporterPreferences;
import org.jabref.logic.importer.fetcher.UnpaywallFetcher;
import org.jabref.logic.util.URLUtil;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.http.SimpleHttpResponse;

import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import kong.unirest.core.UnirestException;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
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

    public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:130.0) Gecko/20100101 Firefox/130.0";
    private static final Logger LOGGER = LoggerFactory.getLogger(URLDownload.class);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final int MAX_RETRIES = 3;

    private final URL source;
    private final Map<String, String> parameters = new HashMap<>();

    // In case Unpaywall should be supported, this should be non-null
    private @Nullable ImporterPreferences importerPreferences;

    private String postData = "";
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private SSLContext sslContext;

    static {
        Unirest.config()
               .followRedirects(true)
               .enableCookieManagement(true)
               .setDefaultHeader("User-Agent", USER_AGENT);
    }

    /**
     * @param source the URL to download from
     * @throws MalformedURLException if no protocol is specified in the source, or an unknown protocol is found
     */
    public URLDownload(String source) throws MalformedURLException {
        this(URLUtil.create(source));
    }

    /**
     * @param source The URL to download.
     */
    public URLDownload(URL source) {
        this.source = source;
        this.addHeader("User-Agent", URLDownload.USER_AGENT);

        try {
            sslContext = SSLContext.getInstance("TLSv1.2");
            sslContext.init(null, null, new SecureRandom());
            // Note: SSL certificates are installed at {@link TrustStoreManager#configureTrustStore(Path)}
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            LOGGER.error("Could not initialize SSL context", e);
            sslContext = null;
        }
    }

    public URLDownload(@NonNull ImporterPreferences importerPreferences, @NonNull URL citationsUrl) {
        this(citationsUrl);
        this.importerPreferences = importerPreferences;
    }

    public URL getSource() {
        return source;
    }

    public Optional<String> getMimeType() {
        String contentType;

        int retries = 0;
        // Try to use HEAD request to avoid downloading the whole file
        try {
            String urlToCheck = source.toString();
            String locationHeader;
            // @formatter:off
            do {
                // @formatter:on
                retries++;
                HttpResponse<String> response = Unirest.head(urlToCheck).asString();
                // Check if we have redirects, e.g. arxiv will give otherwise content type html for the original url
                // We need to do it "manually", because ".followRedirects(true)" only works for GET not for HEAD
                locationHeader = response.getHeaders().getFirst("location");
                if (!StringUtil.isNullOrEmpty(locationHeader)) {
                    urlToCheck = locationHeader;
                }
                // while loop, because there could be multiple redirects
            } while (!StringUtil.isNullOrEmpty(locationHeader) && retries <= MAX_RETRIES);
            contentType = Unirest.head(urlToCheck).asString().getHeaders().getFirst("Content-Type");
            if ((contentType != null) && !contentType.isEmpty()) {
                return Optional.of(contentType);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via HEAD request", e);
        }

        // Use GET request as alternative if no HEAD request is available
        try {
            contentType = Unirest.get(source.toString()).asString().getHeaders().get("Content-Type").getFirst();
            if (!StringUtil.isNullOrEmpty(contentType)) {
                return Optional.of(contentType);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting MIME type of URL via GET request", e);
        }

        // Try to resolve local URIs
        try {
            URLConnection connection = URLUtil.create(source.toString()).openConnection();
            contentType = connection.getContentType();
            if (!StringUtil.isNullOrEmpty(contentType)) {
                return Optional.of(contentType);
            }
        } catch (IOException e) {
            LOGGER.debug("Error trying to get MIME type of local URI", e);
        }

        return Optional.empty();
    }

    /**
     * Check the connection by using the HEAD request.
     * UnirestException can be thrown for invalid request.
     *
     * @return the status code of the response
     */
    public boolean canBeReached() throws UnirestException {

        int statusCode = Unirest.head(source.toString()).asString().getStatus();
        return (statusCode >= 200) && (statusCode < 300);
    }

    public boolean isMimeType(String type) {
        return getMimeType().map(mimeType -> mimeType.startsWith(type)).orElse(false);
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
     * @param encoding   the desired String encoding
     * @param connection an existing connection
     * @return the downloaded string
     */
    private static String asString(Charset encoding, URLConnection connection) throws FetcherException {
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
            return List.of();
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

    /// Uses the web resource as source and creates a monitored input stream.
    public ProgressInputStream asInputStream() throws FetcherException {
        HttpURLConnection urlConnection = (HttpURLConnection) this.openConnection();
        return asInputStream(urlConnection);
    }

    /// Uses the web resource as source and creates a monitored input stream.
    ///
    /// Exposing the urlConnection is required for dynamic API limiting of CrossRef
    public ProgressInputStream asInputStream(HttpURLConnection urlConnection) throws FetcherException {
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
        Path file;
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
     * If accessing an HTTP URL, remember to close the resulting connection after usage.
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

                if (newUrl.startsWith("https://api.unpaywall.org/")) {
                    if (importerPreferences == null) {
                        LOGGER.warn("importerPreferences not set, but call to Unpaywall");
                    } else {
                        Optional<String> apiKey = importerPreferences.getApiKey(UnpaywallFetcher.FETCHER_NAME);
                        if (StringUtil.isBlank(apiKey)) { // No checking for enablement of Unpaywall, because used differently
                            LOGGER.warn("No email configured for Unpaywall");
                        } else {
                            newUrl = newUrl.replace("<INSERT_YOUR_EMAIL>", apiKey.get());
                        }
                    }
                }

                // open the new connection again
                try {
                    httpURLConnection.disconnect();
                    // multiple redirects are implemented by this recursion
                    connection = new URLDownload(newUrl).openConnection();
                } catch (MalformedURLException e) {
                    throw new FetcherException("Could not open URL Download", e);
                }
            } else if (status >= 400) {
                // in case of an error, propagate the error message
                SimpleHttpResponse httpResponse = new SimpleHttpResponse(httpURLConnection);
                LOGGER.info("{}: {}", FetcherException.getRedactedUrl(this.source.toString()), httpResponse);
                if (status < 500) {
                    throw new FetcherClientException(this.source, httpResponse);
                } else {
                    throw new FetcherServerException(this.source, httpResponse);
                }
            }
        }
        return connection;
    }

    private URLConnection getUrlConnection() throws IOException {
        URLConnection connection = this.source.openConnection();

        if (connection instanceof HttpURLConnection httpConnection) {
            httpConnection.setInstanceFollowRedirects(true);
        }

        if ((sslContext != null) && (connection instanceof HttpsURLConnection httpsConnection)) {
            httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }

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
