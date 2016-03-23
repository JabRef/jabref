package net.sf.jabref.logic.fetcher;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Charsets;

import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

public class IEEE implements FullTextFinder {

    private static final Log LOGGER = LogFactory.getLog(IEEE.class);
    private static final Pattern STAMP_PATTERN = Pattern.compile("(/stamp/stamp.jsp\\?tp=&arnumber=[0-9]+)");
    private static final Pattern PDF_PATTERN = Pattern
            .compile("\"(http://ieeexplore.ieee.org/ielx[0-9/]+\\.pdf[^\"]+)\"");
    private static final String IEEE_DOI = "10.1109";
    private static final String BASE_URL = "http://ieeexplore.ieee.org";


    @Override
    public Optional<URL> findFullText(BibEntry entry) throws IOException {
        Objects.requireNonNull(entry);

        Optional<DOI> doi = DOI.build(entry.getField("doi"));
        if (!doi.isPresent() || !doi.get().getDOI().startsWith(IEEE_DOI) || !doi.get().getURI().isPresent()) {
            return Optional.empty();
        }

        String resolvedDOIPage = new URLDownload(doi.get().getURI().get().toURL()).downloadToString(Charsets.UTF_8);
        Matcher matcher = STAMP_PATTERN.matcher(resolvedDOIPage);
        if (matcher.find()) {
            String framePage = new URLDownload(new URL(BASE_URL + matcher.group(1)))
                    .downloadToString(Charsets.UTF_8);
            matcher = PDF_PATTERN.matcher(framePage);
            if (matcher.find()) {
                LOGGER.debug("Full text document found on IEEE Xplore");
                return Optional.of(new URL(matcher.group(1)));
            }
        }
        return Optional.empty();
    }

}
