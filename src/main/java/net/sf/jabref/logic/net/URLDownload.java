package net.sf.jabref.logic.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jabref.logic.util.io.FileUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * URL download to a string.
 * <p>
 * Example:
 * URLDownload dl = new URLDownload(URL);
 * String content = dl.downloadToString(ENCODING);
 * dl.downloadToFile(FILE); // available in FILE
 * String contentType = dl.determineMimeType();
 *
 * Each call to a public method creates a new HTTP connection. Nothing is cached.
 *
 * @author Erik Putrycz erik.putrycz-at-nrc-cnrc.gc.ca
 * @author Simon Harrer
 */
public class URLDownload {
    private static final Log LOGGER = LogFactory.getLog(URLDownload.class);

    private static final String USER_AGENT= "JabRef";

    private final URL source;
    private final Map<String, String> parameters = new HashMap<>();

    private String postData = "";

    public static URLDownload createURLDownloadWithBrowserUserAgent(String address) throws MalformedURLException {
        URLDownload downloader = new URLDownload(address);
        downloader.addParameters("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
        return downloader;
    }

    /**
     * @param address the URL to download from
     * @throws MalformedURLException if no protocol is specified in the address, or an unknown protocol is found
     */
    public URLDownload(String address) throws MalformedURLException {
        this(new URL(address));
    }

    /**
     * @param source The URL to download.
     */
    public URLDownload(URL source) {
        this.source = source;
        addParameters("User-Agent", USER_AGENT);
    }

    public URL getSource() {
        return source;
    }


    public String determineMimeType() throws IOException {
        // this does not cause a real performance issue as the underlying HTTP/TCP connection is reused
        URLConnection urlConnection = openConnection();
        try {
            return urlConnection.getContentType();
        } finally {
            try {
                urlConnection.getInputStream().close();
            } catch (IOException ignored) {
                // Ignored
            }
        }
    }

    public void addParameters(String key, String value) {
        parameters.put(key, value);
    }

    public void setPostData(String postData) {
        if (postData != null) {
            this.postData = postData;
        }
    }

    private URLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) source.openConnection();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (!postData.isEmpty()) {
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(postData);
            }

        }

        // normally, 3xx is redirect
        int status = connection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                // get redirect url from "location" header field
                String newUrl = connection.getHeaderField("Location");
                // open the new connnection again
                connection = (HttpURLConnection) new URLDownload(newUrl).openConnection();
            }
        }

        // this does network i/o: GET + read returned headers
        connection.connect();

        return connection;
    }

    /**
     *
     * @return the downloaded string
     * @throws IOException
     */

    public String downloadToString(Charset encoding) throws IOException {

        try (InputStream input = new BufferedInputStream(openConnection().getInputStream());
             Writer output = new StringWriter()) {
            copy(input, output, encoding);
            return output.toString();
        } catch (IOException e) {
            LOGGER.warn("Could not copy input", e);
            throw e;
        }
    }

    public List<HttpCookie> getCookieFromUrl() throws IOException {
        CookieManager cookieManager = new CookieManager();
        CookieHandler.setDefault(cookieManager);
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ALL);

        URLConnection con = openConnection();
        con.getHeaderFields(); // must be read to store the cookie

        try {
            return cookieManager.getCookieStore().get(source.toURI());
        } catch (URISyntaxException e) {
            LOGGER.error("Unable to convert download URL to URI", e);
            return Collections.emptyList();
        }

    }

    private void copy(InputStream in, Writer out, Charset encoding) throws IOException {
        InputStream monitoredInputStream = monitorInputStream(in);
        Reader r = new InputStreamReader(monitoredInputStream, encoding);
        try (BufferedReader read = new BufferedReader(r)) {

            String line;
            while ((line = read.readLine()) != null) {
                out.write(line);
                out.write("\n");
            }
        }
    }

    /**
     * @deprecated use {@link #downloadToFile(Path)}
     */
    @Deprecated
    public void downloadToFile(File destination) throws IOException {
        downloadToFile(destination.toPath());
    }

    public void downloadToFile(Path destination) throws IOException {

        try (InputStream input = monitorInputStream(new BufferedInputStream(openConnection().getInputStream()))) {
            Files.copy(input, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            LOGGER.warn("Could not copy input", e);
            throw e;
        }
    }

    /**
     * Downloads the web resource to a temporary file.
     *
     * @return the path to the downloaded file.
     */
    public Path downloadToTemporaryFile() throws IOException {
        // Determine file name and extension from source url
        String sourcePath = source.getPath();

        // Take everything after the last '/' as name + extension
        String fileNameWithExtension = sourcePath.substring(sourcePath.lastIndexOf('/') + 1);
        String fileName = FileUtil.getFileName(fileNameWithExtension);
        String extension = "." + FileUtil.getFileExtension(fileNameWithExtension).orElse("tmp");

        // Create temporary file and download to it
        Path file = Files.createTempFile(fileName, extension);
        downloadToFile(file);
        return file;
    }

    protected InputStream monitorInputStream(InputStream in) {
        return in;
    }

    @Override
    public String toString() {
        return "URLDownload{" + "source=" + source + '}';
    }
}
