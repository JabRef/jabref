package net.sf.jabref.external;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.logic.fetcher.*;
import net.sf.jabref.logic.net.URLDownload;

/**
 * Utility class for trying to resolve URLs to full-text PDF for articles.
 */
public class FindFullText {

    private final List<FullTextFinder> finders = new ArrayList<>();

    public FindFullText() {
        // Ordering is important, authorities first!
        // Publisher
        finders.add(new ScienceDirect());
        finders.add(new SpringerLink());
        finders.add(new ACS());
        finders.add(new ArXiv());
        // Meta search
        finders.add(new GoogleScholar());
    }

    public Optional<URL> findFullText(BibEntry entry) {
        for (FullTextFinder finder : finders) {
            try {
                Optional<URL> result = finder.findFullText(entry);

                if (result.isPresent()) {
                    // TODO: recheck this!
                    // Check the MIME type of this URL to see if it is a PDF. If not,
                    // it could be because the user doesn't have access:
                    // FIXME: redirection break this!
                    // Property-based software engineering measurement
                    // http://drum.lib.umd.edu/bitstream/1903/19/2/CS-TR-3368.pdf
                    // FIXME:
                    // INFO: Fulltext PDF found @ Google: https://www.uni-bamberg.de/fileadmin/uni/fakultaeten/wiai_lehrstuehle/praktische_informatik/Dateien/Publikationen/sose14-towards-application-portability-in-paas.pdf
                    // javax.net.ssl.SSLProtocolException: handshake alert:  unrecognized_name
                    // http://stackoverflow.com/questions/7615645/ssl-handshake-alert-unrecognized-name-error-since-upgrade-to-java-1-7-0
                    String mimeType = new URLDownload(result.get()).determineMimeType();
                    if ((mimeType != null) && "application/pdf".equals(mimeType.toLowerCase())) {
                        return Optional.of(result.get());
                    } else {
                        // TODO log
                    }
                }
            } catch (IOException ex) {
                // TODO log
                continue;
            }
        }
        return Optional.empty();
    }
}
