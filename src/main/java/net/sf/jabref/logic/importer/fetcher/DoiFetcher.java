package net.sf.jabref.logic.importer.fetcher;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.logic.help.HelpFile;
import net.sf.jabref.logic.importer.FetcherException;
import net.sf.jabref.logic.importer.IdBasedFetcher;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DoiFetcher implements IdBasedFetcher {

    private ImportFormatPreferences preferences;

    private static final Log LOGGER = LogFactory.getLog(DoiFetcher.class);

    public DoiFetcher(ImportFormatPreferences preferences) {
        this.preferences = preferences;
    }

    public static Optional<BibEntry> getEntryFromDOI(String doi, ParserResult parserResult, ImportFormatPreferences preferences){
        try {
            return new DoiFetcher(preferences).performSearchById(doi);
        } catch (FetcherException e) {
            LOGGER.warn("Invalid DOI", e);
            parserResult.addWarning(Localization.lang("Invalid DOI: '%0'.", doi));
        }
        return Optional.empty();
    }

    private static String cleanupEncoding(String bibtex) {
        // Usually includes an en-dash in the page range. Char is in cp1252 but not
        // ISO 8859-1 (which is what latex expects). For convenience replace here.
        return bibtex.replaceAll("(pages=\\{[0-9]+)\u2013([0-9]+\\})", "$1--$2");
    }

    @Override
    public String getName() {
        return "DOI to BibTeX";
    }

    @Override
    public HelpFile getHelpPage() {
        return HelpFile.FETCHER_DOI_TO_BIBTEX;
    }

    @Override
    public Optional<BibEntry> performSearchById(String identifier) throws FetcherException {
        Optional<DOI> doi = DOI.build(identifier);

        try {
            if (doi.isPresent()) {
                URL doiURL = new URL(doi.get().getURIAsASCIIString());

                // BibTeX data
                URLDownload download = new URLDownload(doiURL);
                download.addParameters("Accept", "application/x-bibtex");
                String bibtexString = download.downloadToString(StandardCharsets.UTF_8);

                // BibTeX entry
                return BibtexParser.singleFromString(cleanupEncoding(bibtexString), preferences);
            } else {
                LOGGER.warn(Localization.lang("Invalid DOI: '%0'.", identifier));
                return Optional.empty();
            }
        } catch (IOException e) {
            throw new FetcherException("Bad URL when fetching DOI info", e);
        }
    }
}
