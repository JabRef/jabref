package net.sf.jabref.logic.net;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

public class NetUtil {

    /**
     * Download the URL and return contents as a String.
     *
     * @param source
     * @return
     * @throws IOException
     */
    public static String getResults(URL source) throws IOException {

        return net.sf.jabref.logic.net.NetUtil.getResultsWithEncoding(source.openConnection(), null);
    }

    /**
     * Download the URL and return contents as a String.
     *
     * @param source
     * @return
     * @throws IOException
     */
    public static String getResults(URLConnection source) throws IOException {
        return net.sf.jabref.logic.net.NetUtil.getResultsWithEncoding(source, null);
    }

    /**
     * Download the URL using specified encoding and return contents as a String.
     *
     * @param source encoding
     * @return
     * @throws IOException
     */
    public static String getResultsWithEncoding(URL source, Charset encoding) throws IOException {
        return net.sf.jabref.logic.net.NetUtil.getResultsWithEncoding(source.openConnection(), encoding);
    }

    /**
     * Download the URL using specified encoding and return contents as a String.
     *
     * @param source encoding
     * @return
     * @throws IOException
     */
    public static String getResultsWithEncoding(URLConnection source, Charset encoding) throws IOException {

        // set user-agent to avoid being blocked as a crawler
        source.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");

        InputStreamReader in;
        if (encoding == null) {
            in = new InputStreamReader(source.getInputStream());
        } else {
            in = new InputStreamReader(source.getInputStream(), encoding);
        }

        StringBuilder sb = new StringBuilder();
        while (true) {
            int byteRead = in.read();
            if (byteRead == -1) {
                break;
            }
            sb.append((char) byteRead);
        }
        return sb.toString();
    }

    /**
     * Get the results of HTTP post on a URL and return contents as a String.
     *
     * @param source postData encoding
     * @return
     * @throws IOException
     */
    public static String getPostResults(URL source, String postData, Charset encoding) throws IOException {
        HttpURLConnection con = (HttpURLConnection) source.openConnection();
        return NetUtil.getPostResults(con, postData, encoding);
    }

    /**
     * Get the results of HTTP post on a URL and return contents as a String.
     *
     * @param source postData encoding
     * @return
     * @throws IOException
     */
    public static String getPostResults(HttpURLConnection source, String postData, Charset encoding)
            throws IOException {

        //add a default request header
        source.setRequestMethod("POST");
        source.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 5.1; rv:31.0) Gecko/20100101 Firefox/31.0");
        source.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

        // Send post request
        source.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(source.getOutputStream());) {
            wr.writeBytes(postData);
        }

        String inputLine;
        StringBuffer response = new StringBuffer();

        try (BufferedReader in = new BufferedReader(new InputStreamReader(source.getInputStream(), encoding));) {
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }

        return response.toString();
    }

    /**
     * Read results from a file instead of an URL. Just for faster debugging.
     *
     * @param f
     * @return
     * @throws IOException
     */
    public static String getResultsFromFile(File f) throws IOException {
        try (InputStream in = new BufferedInputStream(new FileInputStream(f))) {
            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[256];
            while (true) {
                int bytesRead = in.read(buffer);
                if (bytesRead == -1) {
                    break;
                }
                for (int i = 0; i < bytesRead; i++) {
                    sb.append((char) buffer[i]);
                }
            }
            return sb.toString();
        }
    }

}
