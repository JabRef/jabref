package net.sf.jabref.logic.net;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

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

    private final URL source;

    private static final Log LOGGER = LogFactory.getLog(URLDownload.class);

    private final Map<String, String> parameters = new HashMap<>();

    private String postData = "";

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

        addParameters("User-Agent", "JabRef");

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
        URLConnection connection = source.openConnection();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
        if (!postData.isEmpty()) {
            connection.setDoOutput(true);
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(postData);
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

    public void downloadToFile(File destination) throws IOException {

        try (InputStream input = new BufferedInputStream(openConnection().getInputStream());
             OutputStream output = new BufferedOutputStream(new FileOutputStream(destination))) {
            copy(input, output);
        } catch (IOException e) {
            LOGGER.warn("Could not copy input", e);
            throw e;
        }
    }

    private void copy(InputStream in, OutputStream out) throws IOException {
        try (InputStream monitorInputStream = monitorInputStream(in)) {
            byte[] buffer = new byte[512];
            while (true) {
                int bytesRead = monitorInputStream.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    protected InputStream monitorInputStream(InputStream in) {
        return in;
    }

    @Override
    public String toString() {
        return "URLDownload{" + "source=" + source + '}';
    }
}
