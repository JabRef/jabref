package net.sf.jabref.logic.importer.fetcher;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import net.sf.jabref.logic.formatter.bibtexfields.UnitsToLatexFormatter;
import net.sf.jabref.logic.formatter.casechanger.ProtectTermsFormatter;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.URLDownload;
import net.sf.jabref.logic.util.DOI;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DOItoBibTeX {

    private static final Log LOGGER = LogFactory.getLog(DOItoBibTeX.class);

    private static final ProtectTermsFormatter protectTermsFormatter = new ProtectTermsFormatter();
    private static final UnitsToLatexFormatter unitsToLatexFormatter = new UnitsToLatexFormatter();


    public static Optional<BibEntry> getEntryFromDOI(String doiStr, ImportFormatPreferences importFormatPreferences) {
        return getEntryFromDOI(doiStr, null, importFormatPreferences);
    }

    public static Optional<BibEntry> getEntryFromDOI(String doiStr, ParserResult parserResult,
            ImportFormatPreferences importFormatPreferences) {
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
            Optional<BibEntry> bibEntry = BibtexParser.singleFromString(bibtexString, importFormatPreferences);

            bibEntry.ifPresent(entry -> formatTitleField(entry, importFormatPreferences));

            return bibEntry;
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

    private static void formatTitleField(BibEntry entry, ImportFormatPreferences importFormatPreferences) {
        // Optionally add curly brackets around key words to keep the case
        entry.getFieldOptional(FieldName.TITLE).ifPresent(title -> {
            // Unit formatting
            if (importFormatPreferences.isConvertUnitsOnSearch()) {
                title = unitsToLatexFormatter.format(title);
            }

            // Case keeping
            if (importFormatPreferences.isUseCaseKeeperOnSearch()) {
                title = protectTermsFormatter.format(title);
            }
            entry.setField(FieldName.TITLE, title);
        });
    }

    private static String cleanupEncoding(String bibtex) {
        // Usually includes an en-dash in the page range. Char is in cp1252 but not
        // ISO 8859-1 (which is what latex expects). For convenience replace here.
        return bibtex.replaceAll("(pages=\\{[0-9]+)\u2013([0-9]+\\})", "$1--$2");
    }

}
