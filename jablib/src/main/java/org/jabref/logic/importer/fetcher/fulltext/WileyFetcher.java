package org.jabref.logic.importer.fetcher.fulltext;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.fetcher.TrustLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wiley TDM fetcher adapted to JabRef interfaces.
 */
public class WileyFetcher implements FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(WileyFetcher.class);
    private static final String ENV_TDM_TOKEN = "WILEY_TDM_TOKEN";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Optional<String> doiOpt = entry.getField(StandardField.DOI);
        if (doiOpt.isEmpty()) {
            LOGGER.debug("WileyFetcher: no DOI present, skipping.");
            return Optional.empty();
        }
        String doi = doiOpt.get().trim();
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        String token = System.getenv(ENV_TDM_TOKEN);
        if (token == null || token.isBlank()) {
            LOGGER.debug("WileyFetcher: WILEY_TDM_TOKEN not set, skipping.");
            return Optional.empty();
        }

        try {
            String encodedDoi = URLEncoder.encode(doi, StandardCharsets.UTF_8.name());
            String apiUrl = "https://api.wiley.com/onlinelibrary/tdm/v1/articles/" + encodedDoi;
            LOGGER.debug("WileyFetcher: requesting {}", apiUrl);

            URL url = new URI(apiUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("TDM-Client-Token", token);
            conn.setRequestProperty("Accept", "*/*");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.connect();

            int code = conn.getResponseCode();
            String contentType = conn.getContentType();
            LOGGER.debug("Wiley API responded: code={}, contentType={}", code, contentType);

            // if API returned PDF directly
            if (code == 200 && contentType != null && contentType.toLowerCase().contains("pdf")) {
                File tmpPdf = File.createTempFile("jabref-wiley-", ".pdf");
                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tmpPdf)) {
                    copyStream(in, out);
                }
                LOGGER.info("WileyFetcher: downloaded direct PDF to {}", tmpPdf.getAbsolutePath());
                return Optional.of(tmpPdf.toURI().toURL());
            }

            InputStream inStream = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
            if (inStream == null) {
                LOGGER.debug("WileyFetcher: no response stream");
                return Optional.empty();
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(inStream);

            String pdfUrl = findPdfUrlRecursively(root);
            if (pdfUrl != null) {
                LOGGER.debug("WileyFetcher: found pdf url in JSON: {}", pdfUrl);

                URL pdfDownloadUrl = new URI(pdfUrl).toURL();
                HttpURLConnection pdfConn = (HttpURLConnection) pdfDownloadUrl.openConnection();
                pdfConn.setRequestProperty("TDM-Client-Token", token);
                pdfConn.setConnectTimeout(15_000);
                pdfConn.setReadTimeout(60_000);
                pdfConn.connect();

                int pdfStatus = pdfConn.getResponseCode();
                String pdfContentType = pdfConn.getContentType();
                LOGGER.debug("Downloading pdf: code={}, contentType={}", pdfStatus, pdfContentType);

                if (pdfStatus == 200 && pdfContentType != null && pdfContentType.toLowerCase().contains("pdf")) {
                    File tmpPdf = File.createTempFile("jabref-wiley-", ".pdf");
                    try (InputStream pin = pdfConn.getInputStream(); FileOutputStream pout = new FileOutputStream(tmpPdf)) {
                        copyStream(pin, pout);
                    }
                    LOGGER.info("WileyFetcher: downloaded pdf to {}", tmpPdf.getAbsolutePath());
                    return Optional.of(tmpPdf.toURI().toURL());
                } else {
                    LOGGER.debug("WileyFetcher: pdf download link did not return PDF (status {}).", pdfStatus);
                }
            } else {
                LOGGER.debug("WileyFetcher: no pdf link found in JSON.");
            }

        } catch (IOException ioe) {
            LOGGER.warn("WileyFetcher: IO error while fetching DOI {}: {}", doi, ioe.getMessage());
            throw ioe;
        } catch (Exception e) {
            LOGGER.warn("WileyFetcher: error fetching DOI {}: {}", doi, e.getMessage());
            throw new FetcherException("Wiley fetch error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    private static void copyStream(InputStream in, FileOutputStream out) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    private static String findPdfUrlRecursively(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            String text = node.asText();
            if (text.toLowerCase().contains(".pdf")) {
                return text;
            }
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                String found = findPdfUrlRecursively(e.getValue());
                if (found != null) {
                    return found;
                }
            }
        }
        if (node.isArray()) {
            for (JsonNode item : node) {
                String found = findPdfUrlRecursively(item);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
