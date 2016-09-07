package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.importer.FulltextFetcher;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * FulltextFetcher implementation that attempts to find a PDF URL at ACS.
 */
public class ACS implements FulltextFetcher {
    private static final Log LOGGER = LogFactory.getLog(ACS.class);

    private static final String SOURCE = "http://pubs.acs.org/doi/abs/%s";

    /**
     * Tries to find a fulltext URL for a given BibTex entry.
     *
     * Currently only uses the DOI if found.
     *
     * @param entry The Bibtex entry
     * @return The fulltext PDF URL Optional, if found, or an empty Optional if not found.
     * @throws NullPointerException if no BibTex entry is given
     * @throws java.io.IOException
     */
    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);
        Optional<URL> pdfLink = Optional.empty();

        // DOI search
        Optional<DOI> doi = entry.getField(FieldName.DOI).flatMap(DOI::build);

        if(doi.isPresent()) {
            String source = String.format(SOURCE, doi.get().getDOI());
            // Retrieve PDF link
            Document html = Jsoup.connect(source).ignoreHttpErrors(true).get();
            Element link = html.select(".pdf-high-res a").first();

            if(link != null) {
                LOGGER.info("Fulltext PDF found @ ACS.");
                pdfLink = Optional.of(new URL(source.replaceFirst("/abs/", "/pdf/")));
            }
        }
        return pdfLink;
    }
}
