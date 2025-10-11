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
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.importer.fetcher.TrustLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SpringerLink fetcher adapted to JabRef interfaces.
 * Attempts a direct PDF URL first, then falls back to article page heuristics.
 */
public class SpringerLinkFetcher implements FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringerLinkFetcher.class);

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException, FetcherException {
        Optional<String> doiOpt = entry.getField(StandardField.DOI);
        if (doiOpt.isEmpty()) {
            LOGGER.debug("SpringerLinkFetcher: no DOI present, skipping.");
            return Optional.empty();
        }
        String doi = doiOpt.get().trim();
        if (doi.isEmpty()) {
            return Optional.empty();
        }

        try {
            String encodedDoi = URLEncoder.encode(doi, StandardCharsets.UTF_8.name());
            // try direct content/pdf pattern
            String pdfUrl1 = "https://link.springer.com/content/pdf/" + encodedDoi + ".pdf";
            LOGGER.debug("SpringerLinkFetcher: trying direct pdf URL {}", pdfUrl1);
            Optional<URL> direct = tryDownloadPdfReturningUrl(pdfUrl1);
            if (direct.isPresent()) {
                return direct;
            }

            // try article page and search for pdf link
            String articlePage = "https://link.springer.com/article/" + encodedDoi;
            LOGGER.debug("SpringerLinkFetcher: trying article page {}", articlePage);

            URL pageUrl = new URI(articlePage).toURL();
            HttpURLConnection conn = (HttpURLConnection) pageUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            conn.connect();

            int code = conn.getResponseCode();
            if (code >= 200 && code < 400) {
                try (InputStream in = conn.getInputStream()) {
                    String body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    String pdfLink = findPdfLinkInHtml(body);
                    if (pdfLink != null) {
                        URL resolved = new URL(pageUrl, pdfLink);
                        Optional<URL> got = tryDownloadPdfReturningUrl(resolved.toString());
                        if (got.isPresent()) {
                            return got;
                        }
                    }
                }
            } else {
                LOGGER.debug("SpringerLinkFetcher: article page returned code {}", code);
            }

        } catch (IOException ioe) {
            LOGGER.warn("SpringerLinkFetcher: IO error: {}", ioe.getMessage());
            throw ioe;
        } catch (Exception e) {
            LOGGER.warn("SpringerLinkFetcher: unexpected error: {}", e.getMessage());
            throw new FetcherException("Springer fetch error: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

    private Optional<URL> tryDownloadPdfReturningUrl(String urlStr) {
        try {
            URL url = new URI(urlStr).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/pdf,*/*");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(60_000);
            conn.connect();

            int code = conn.getResponseCode();
            String contentType = conn.getContentType();
            LOGGER.debug("tryDownloadPdfReturningUrl: url={} code={} contentType={}", urlStr, code, contentType);

            if (code == 200 && contentType != null && contentType.toLowerCase().contains("pdf")) {
                File tmpPdf = File.createTempFile("jabref-springer-", ".pdf");
                try (InputStream in = conn.getInputStream(); FileOutputStream out = new FileOutputStream(tmpPdf)) {
                    copyStream(in, out);
                }
                LOGGER.info("SpringerLinkFetcher: downloaded pdf to {}", tmpPdf.getAbsolutePath());
                return Optional.of(tmpPdf.toURI().toURL());
            }
        } catch (Exception e) {
            LOGGER.debug("tryDownloadPdfReturningUrl failed for {}: {}", urlStr, e.getMessage());
        }
        return Optional.empty();
    }

    private String findPdfLinkInHtml(String html) {
        int idx = html.indexOf(".pdf");
        if (idx > 0) {
            int start = html.lastIndexOf("href=\"", idx);
            if (start >= 0) {
                start += 6;
                int end = html.indexOf("\"", idx + 4);
                if (end > start) {
                    return html.substring(start, end);
                }
            }
            start = html.lastIndexOf("src=\"", idx);
            if (start >= 0) {
                start += 5;
                int end = html.indexOf("\"", idx + 4);
                if (end > start) {
                    return html.substring(start, end);
                }
            }
        }
        return null;
    }

    private static void copyStream(InputStream in, FileOutputStream out) throws IOException {
        byte[] buffer = new byte[8 * 1024];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }
}
