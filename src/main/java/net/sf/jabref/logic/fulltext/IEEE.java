package net.sf.jabref.logic.fulltext;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for finding PDF URLs for entries on IEEE
 * Will first look for URLs of the type http://ieeexplore.ieee.org/stamp/stamp.jsp?[tp=&]arnumber=...
 * If not found, will resolve the DOI, if it starts with 10.1109, and try to find a similar link on the HTML page
 */
public class IEEE implements FullTextFinder {

    private static final Log LOGGER = LogFactory.getLog(IEEE.class);
    private static final Pattern STAMP_PATTERN = Pattern.compile("(/stamp/stamp.jsp\\?t?p?=?&?arnumber=[0-9]+)");
    private static final Pattern PDF_PATTERN = Pattern
            .compile("\"(http://ieeexplore.ieee.org/ielx[0-9/]+\\.pdf[^\"]+)\"");
    private static final String IEEE_DOI = "10.1109";
    private static final String BASE_URL = "http://ieeexplore.ieee.org";


    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        String stampString = "";
        // Try URL first -- will primarily work for entries from the old IEEE search
        if (entry.hasField("url")) {
            // Is the URL a direct link to IEEE?
            Matcher matcher = STAMP_PATTERN.matcher(entry.getField("url"));
            if (matcher.find()) {
                // Found it
                stampString = matcher.group(1);
            }
        }

        // If not, try DOI
        if (stampString.isEmpty()) {
            Optional<DOI> doi = DOI.build(entry.getField("doi"));
            if (doi.isPresent() && doi.get().getDOI().startsWith(IEEE_DOI) && doi.get().getURI().isPresent()) {
                // Download the HTML page from IEEE
                String resolvedDOIPage = new URLDownload(doi.get().getURI().get().toURL())
                        .downloadToString(StandardCharsets.UTF_8);
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
        String framePage = new URLDownload(new URL(BASE_URL + stampString)).downloadToString(StandardCharsets.UTF_8);
        // Try to find the direct PDF link
        Matcher matcher = PDF_PATTERN.matcher(framePage);
        if (matcher.find()) {
            // The PDF was found
            LOGGER.debug("Full text document found on IEEE Xplore");
            return Optional.of(new URL(matcher.group(1)));
        }
        return Optional.empty();
    }

}
