package org.jabref.logic.importer.fileformat.pdf;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.importer.ImportFormatPreferences;
import org.jabref.logic.importer.ParseException;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.importer.fileformat.BibtexParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.PdfUtils;
import org.jabref.model.entry.BibEntry;

import org.apache.pdfbox.pdmodel.PDDocument;

/// This importer imports a verbatim BibTeX entry from the first page of the PDF.
public class PdfVerbatimBibtexImporter extends PdfImporter {

    /// Matches the start of a BibTeX entry: an entry type followed by an opening brace or parenthesis.
    /// Other "@" occurrences in the page text (such as author email addresses) do not match.
    private static final Pattern ENTRY_START = Pattern.compile("@\\s*[a-zA-Z]+\\s*[{(]");

    private final ImportFormatPreferences importFormatPreferences;

    public PdfVerbatimBibtexImporter(ImportFormatPreferences importFormatPreferences) {
        this.importFormatPreferences = importFormatPreferences;
    }

    @Override
    public ParserResult importDatabase(Path filePath, PDDocument document) throws IOException, ParseException {
        String firstPageContents = PdfUtils.getFirstPageContents(document);

        // BibtexParser treats every "@" as the start of an entry. A stray "@" before the actual entry
        // (typically an author email address) not only fails to parse itself, but its failed lookahead
        // consumes the "@" of the real entry, which is then lost. Therefore, parsing starts at the
        // first plausible entry start instead of the beginning of the page.
        Matcher entryStart = ENTRY_START.matcher(firstPageContents);
        if (!entryStart.find()) {
            return new ParserResult();
        }

        BibtexParser parser = new BibtexParser(importFormatPreferences);
        List<BibEntry> result = parser.parseEntries(firstPageContents.substring(entryStart.start()));

        // TODO: Check if it's needed in {@link PdfImporter}.
        //       Note: Needed if BibTeX is prepended with text
        result.forEach(entry -> entry.setCommentsBeforeEntry(""));

        return new ParserResult(result);
    }

    @Override
    public String getId() {
        return "pdfVerbatimBibtex";
    }

    @Override
    public String getName() {
        return Localization.lang("Verbatim BibTeX in PDF");
    }

    @Override
    public String getDescription() {
        return Localization.lang("Scrapes the first page of a PDF for BibTeX information.");
    }
}
