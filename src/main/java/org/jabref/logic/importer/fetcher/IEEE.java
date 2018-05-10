package org.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.FulltextFetcher;
import org.jabref.logic.net.URLDownload;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for finding PDF URLs for entries on IEEE
 * Will first look for URLs of the type https://ieeexplore.ieee.org/stamp/stamp.jsp?[tp=&]arnumber=...
 * If not found, will resolve the DOI, if it starts with 10.1109, and try to find a similar link on the HTML page
 */
public class IEEE implements FulltextFetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IEEE.class);
    private static final String STAMP_BASE_STRING_DOCUMENT = "/stamp/stamp.jsp?tp=&arnumber=";
    private static final Pattern STAMP_PATTERN = Pattern.compile("(/stamp/stamp.jsp\\?t?p?=?&?arnumber=[0-9]+)");
    private static final Pattern DOCUMENT_PATTERN = Pattern.compile("document/([0-9]+)/");

    private static final Pattern PDF_PATTERN = Pattern.compile("\"(https://ieeexplore.ieee.org/ielx[0-9/]+\\.pdf[^\"]+)\"");
    private static final String IEEE_DOI = "10.1109";
    private static final String BASE_URL = "https://ieeexplore.ieee.org";

    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        String stampString = "";

        // Try URL first -- will primarily work for entries from the old IEEE search
        Optional<String> urlString = entry.getField(FieldName.URL);
        if (urlString.isPresent()) {

            Matcher documentUrlMatcher = DOCUMENT_PATTERN.matcher(urlString.get());
            if (documentUrlMatcher.find()) {
                String docId = documentUrlMatcher.group(1);
                stampString = STAMP_BASE_STRING_DOCUMENT + docId;
            }

            //You get this url if you export bibtex from IEEE
            Matcher stampMatcher = STAMP_PATTERN.matcher(urlString.get());
            if (stampMatcher.find()) {
                // Found it
                stampString = stampMatcher.group(1);
            }

        }

        // If not, try DOI
        if (stampString.isEmpty()) {
            Optional<DOI> doi = entry.getField(FieldName.DOI).flatMap(DOI::parse);
            if (doi.isPresent() && doi.get().getDOI().startsWith(IEEE_DOI) && doi.get().getExternalURI().isPresent()) {
                // Download the HTML page from IEEE
                URLDownload urlDownload = new URLDownload(doi.get().getExternalURI().get().toURL());
                //We don't need to modify the cookies, but we need support for them
                urlDownload.getCookieFromUrl();

                String resolvedDOIPage = urlDownload.asString();
                // Try to find the link
                Matcher matcher = STAMP_PATTERN.matcher(resolvedDOIPage);
                if (matcher.find()) {
                    // Found it
                    stampString = matcher.group(1);
                }
            }
        }

        // Any success?
        if (stampString.isEmpty()) {
            return Optional.empty();
        }

        // Download the HTML page containing a frame with the PDF
        URLDownload urlDownload = new URLDownload(BASE_URL + stampString);
        //We don't need to modify the cookies, but we need support for them
        urlDownload.getCookieFromUrl();

        String framePage = urlDownload.asString();
        // Try to find the direct PDF link
        Matcher matcher = PDF_PATTERN.matcher(framePage);
        if (matcher.find()) {
            // The PDF was found
            LOGGER.debug("Full text document found on IEEE Xplore");
            return Optional.of(new URL(matcher.group(1)));
        }
        return Optional.empty();
    }

    @Override
    public TrustLevel getTrustLevel() {
        return TrustLevel.PUBLISHER;
    }

}
