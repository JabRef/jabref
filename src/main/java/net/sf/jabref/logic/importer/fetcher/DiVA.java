package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.UnicodeToLatexFormatter;
import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DiVA implements IdBasedFetcher {

    private static final Log LOGGER = LogFactory.getLog(DiVA.class);

    private static final String URL_PATTERN = "http://www.diva-portal.org/smash/getreferences?referenceFormat=BibTex&pids=%s";


    @Override
    public String getName() {
        return "DiVA";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DIVA_TO_BIBTEX;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        String q;
        try {
            q = URLEncoder.encode(identifier, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // this should never happen
            LOGGER.warn("Encoding issues", e);
            return Optional.empty();
        }

        String urlString = String.format(DiVA.URL_PATTERN, q);

        // Send the request
        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            LOGGER.warn("Bad URL", e);
            return Optional.empty();
        }

        String bibtexString;
        try {
            URLDownload dl = new URLDownload(url);

            bibtexString = dl.downloadToString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FetcherException("Communication problems", e);
        }

        Optional<BibEntry> result = Optional.ofNullable(BibtexParser.singleFromString(bibtexString));

        result.ifPresent(entry -> entry.getFieldOptional(FieldName.INSTITUTION).ifPresent(institution -> entry
                .setField(FieldName.INSTITUTION, new UnicodeToLatexFormatter().format(institution))));

        return result;

    }

}
