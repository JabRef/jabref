package net.sf.jabref.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.importer.ImportInspector;
import net.sf.jabref.importer.OutputPrinter;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DOItoBibTeXFetcher implements EntryFetcher {

    private static final Log LOGGER = LogFactory.getLog(DOItoBibTeXFetcher.class);

    private final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();

    protected static Log getLogger() {
        return LOGGER;
    }

    protected ProtectTermsFormatter getProtectTermsFormatter() {
        return protectTermsFormatter;
    }

    protected UnitsToLatexFormatter getUnitsToLatexFormatter() {
        return unitsToLatexFormatter;
    }

    @Override
    public void stopFetching() {
        // not needed as the fetching is a single HTTP GET
    }

    @Override
    public String getTitle() {
        return "DOI to BibTeX";
    }

    @Override
    public boolean processQuery(String query, ImportInspector inspector, OutputPrinter status) {
        ParserResult parserResult = new ParserResult();

        Optional<BibEntry> entry = getEntryFromDOI(query, parserResult);

        if (parserResult.hasWarnings()) {
            status.showMessage(parserResult.getErrorMessage());
        }
        entry.ifPresent(e -> inspector.addEntry(e));

        return entry.isPresent();
    }

    public Optional<BibEntry> getEntryFromDOI(String doiStr) {
        return getEntryFromDOI(doiStr, null);
    }

    public Optional<BibEntry> getEntryFromDOI(String doiStr, ParserResult parserResult) {
        Optional<DOI> doi = DOI.build(doiStr);

        if (!doi.isPresent()) {
            if (parserResult != null) {
                parserResult.addWarning(Localization.lang("Invalid DOI: '%0'.", doiStr));
            }
            return Optional.empty();
        }

        try {
            URL doiURL = new URL(doi.get().getURIAsASCIIString());

            // BibTeX data
            URLDownload download = new URLDownload(doiURL);
            download.addParameters("Accept", "application/x-bibtex");
            String bibtexString = download.downloadToString(StandardCharsets.UTF_8);
            bibtexString = cleanupEncoding(bibtexString);

            // BibTeX entry
            BibEntry entry = BibtexParser.singleFromString(bibtexString);

            if (entry == null) {
                return Optional.empty();
            }
            // Optionally re-format BibTeX entry
            formatTitleField(entry);
            return Optional.of(entry);
        } catch (MalformedURLException e) {
            LOGGER.warn("Bad DOI URL", e);
            return Optional.empty();
        } catch (FileNotFoundException e) {
            if (parserResult != null) {
                parserResult.addWarning(Localization.lang("Unknown DOI: '%0'.", doi.get().getDOI()));
            }
            LOGGER.debug("Unknown DOI", e);
            return Optional.empty();
        } catch (IOException e) {
            LOGGER.warn("Communication problems", e);
            return Optional.empty();
        }
    }


    private void formatTitleField(BibEntry entry) {
        // Optionally add curly brackets around key words to keep the case
        entry.getFieldOptional("title").ifPresent(title -> {
            // Unit formatting
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH)) {
                title = unitsToLatexFormatter.format(title);
            }
            // Case keeping
            if (Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH)) {
                title = protectTermsFormatter.format(title);
            }
            entry.setField("title", title);
        });
    }

    private String cleanupEncoding(String bibtex) {
        // Usually includes an en-dash in the page range. Char is in cp1252 but not
        // ISO 8859-1 (which is what latex expects). For convenience replace here.
        return bibtex.replaceAll("(pages=\\{[0-9]+)\u2013([0-9]+\\})", "$1--$2");
    }
}
