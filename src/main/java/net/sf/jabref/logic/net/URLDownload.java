/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
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
import java.net.CookieHandler;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import net.sf.jabref.Globals;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
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
     * URL download to a string.
     * <p>
     * Example
     * URLDownload dl = new URLDownload(URL);
     * String content = dl.downloadToString(ENCODING);
     * dl.downloadToFile(FILE); // available in FILE
     * String contentType = dl.determineMimeType();
     *
     * @param source The URL to download.
     */

    public URLDownload(URL source) {
        this.source = source;

        addParameters("User-Agent", "JabRef");

        URLDownload.setCookieHandler();
    }

    public URL getSource() {
        return source;
    }

    private static void setCookieHandler() {
        try {
            // This should set up JabRef to receive cookies properly
            if (CookieHandler.getDefault() == null) {
                CookieHandler.setDefault(new CookieHandlerImpl());
            }
        } catch (SecurityException ignored) {
            // Setting or getting the system default cookie handler is forbidden
            // In this case cookie handling is not possible.
        }
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
            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream());) {
                wr.writeBytes(postData);
            }

        }
        // this does network i/o: GET + read returned headers
        connection.connect();

        return connection;
    }

    /**
     * Encoding will be determined from JabRefPreferences.DEFAULT_ENCODING
     *
     * @return the downloaded string
     * @throws IOException
     */
    public String downloadToString() throws IOException {
        return downloadToString(Globals.prefs.getDefaultEncoding());
    }

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
