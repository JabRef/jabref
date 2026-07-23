package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.bst.BstVM;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.BSTFormatUtils;
import org.jabref.logic.openoffice.bst.BstEntryRenderer;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Inserts BST-styled citations and bibliography into a LibreOffice document.
///
/// In-text citation format (numeric `[n]` or author-year `(Name, Year)`) is controlled by
/// [OpenOfficePreferences.getBstCitationFormat]. The bibliography is always rendered by the
/// BST engine regardless of the citation format setting.
@NullMarked
public class BstCitationOOAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BstCitationOOAdapter.class);

    private final XTextDocument document;
    private final CSLReferenceMarkManager markManager;
    private final PandocLatexConverter pandoc;
    private final OpenOfficePreferences openOfficePreferences;

    public BstCitationOOAdapter(XTextDocument document, OpenOfficePreferences openOfficePreferences)
            throws WrappedTargetException, NoSuchElementException {
        this.document = document;
        this.openOfficePreferences = openOfficePreferences;
        this.markManager = new CSLReferenceMarkManager(document);
        this.pandoc = new PandocLatexConverter(openOfficePreferences.getPandocPath());
        markManager.readAndUpdateExistingMarks();
    }

    /// Inserts an in-text citation mark. Format depends on [OpenOfficePreferences.getBstCitationFormat]:
    /// - [BstCitationFormat.NUMERIC]: `[1]`, `[1, 3]`, ...
    /// - [BstCitationFormat.AUTHOR_YEAR]: `(Cooper et al., 2007)`, ...
    public void insertCitation(XTextCursor cursor, List<BibEntry> entries, BibDatabaseContext ctx)
            throws CreationException, com.sun.star.uno.Exception {
        String citationText = switch (openOfficePreferences.getBstCitationFormat()) {
            case NUMERIC ->
                    buildNumericCitation(entries);
            case AUTHOR_YEAR ->
                    buildAuthorYearCitation(entries, ctx);
        };

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(citationText));
        boolean preceedingSpaceExists = checkPreceedingSpace(cursor);
        markManager.insertReferenceIntoOO(
                entries, document, cursor, ooText,
                !preceedingSpaceExists,
                openOfficePreferences.getAddSpaceAfter(),
                CSLCitationType.NORMAL);
        markManager.setRealTimeNumberUpdateRequired(
                openOfficePreferences.getBstCitationFormat() == BstCitationFormat.NUMERIC);
        markManager.readAndUpdateExistingMarks();
    }

    /// Inserts the bibliography by rendering each cited entry through BST → pandoc → OOText.
    /// Entries are sorted by first-appearance (citation-number) order.
    public void insertBibliography(XTextCursor cursor, BstStyle style, List<BibEntry> entries,
                                   BibDatabaseContext ctx)
            throws IOException, InterruptedException, com.sun.star.uno.Exception, CreationException {
        if (!pandoc.isAvailable()) {
            throw new IllegalStateException(
                    "pandoc is not available at the configured path. "
                            + "Please ensure pandoc is installed and configure its path via the OO settings menu.");
        }

        OOText title = OOFormat.paragraph(
                OOText.fromString(openOfficePreferences.getCslBibliographyTitle()),
                openOfficePreferences.getCslBibliographyHeaderFormat());
        OOTextIntoOO.write(document, cursor, OOText.fromString(title.toString()));
        OOText titleBreak = OOFormat.paragraph(
                OOText.fromString(""),
                openOfficePreferences.getCslBibliographyBodyFormat());
        OOTextIntoOO.write(document, cursor, titleBreak);

        BstVM vm;
        try {
            vm = style.createBstVM();
        } catch (IOException e) {
            LOGGER.warn("Could not load BST style: {}", style.getPath(), e);
            throw e;
        }
        BstEntryRenderer renderer = new BstEntryRenderer(vm);

        List<BibEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingInt(
                entry -> markManager.getCitationNumber(entry.getCitationKey().orElse(""))));

        boolean useNumberedBibliography = openOfficePreferences.getBstCitationFormat() == BstCitationFormat.NUMERIC;
        BibDatabase database = ctx.getDatabase();

        for (BibEntry entry : sorted) {
            String key = entry.getCitationKey().orElse("");
            String latex = renderer.renderEntryToLatex(entry, database);
            LOGGER.warn("BST bibliography LaTeX [{}]: {}", key, latex);

            String norm = BSTFormatUtils.normalizeLegacyForPandoc(latex);
            if (!norm.equals(latex)) {
                LOGGER.warn("Normalized LaTeX for pandoc [{}]: {}", key, norm);
            }

            String html = pandoc.latexToHtml(norm);
            LOGGER.warn("Pandoc HTML [{}]: {}", key, html);
            if (html.contains("class=\"math inline\"")) {
                LOGGER.warn("Pandoc HTML [{}] contains inline math spans", key);
                try {
                    Pattern mathSpan = Pattern.compile("(?s)<span\\s+class=\\\"math inline\\\"[^>]*>(.*?)</span>");
                    Matcher m = mathSpan.matcher(html);
                    int count = 0;
                    while (m.find() && count < 3) { // log first few to avoid noise
                        String inner = m.group(1);
                        // Collapse whitespace for readability in logs
                        inner = inner.replaceAll("\\s+", " ").trim();
                        LOGGER.warn("Inline math span [{} #{}]: {}", key, count + 1, inner);
                        count++;
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to inspect inline math spans for [{}]", key, e);
                }
            }

            String body = BSTFormatUtils.convertPandocHtmlToOOText(html);
            LOGGER.warn("OOText body [{}]: {}", key, body);

            String finalLine;
            if (useNumberedBibliography) {
                int number = markManager.getCitationNumber(key);
                finalLine = "[" + number + "] " + body;
            } else {
                finalLine = body;
            }
            LOGGER.warn("Final OOText [{}]: {}", key, finalLine);

            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(finalLine));
            OOTextIntoOO.write(document, cursor, ooText);

            OOText ooBreak = OOFormat.paragraph(
                    OOText.fromString(""),
                    openOfficePreferences.getCslBibliographyBodyFormat());
            OOTextIntoOO.write(document, cursor, ooBreak);
        }
    }

    /// Returns `true` if the given entry has already been cited in the document.
    public boolean isCitedEntry(BibEntry entry) {
        return markManager.hasCitationForKey(entry.getCitationKey().orElse(""));
    }

    private String buildNumericCitation(List<BibEntry> entries) {
        StringJoiner sj = new StringJoiner(", ", "[", "]");
        for (BibEntry entry : entries) {
            int number = markManager.getCitationNumber(entry.getCitationKey().orElse(""));
            sj.add(String.valueOf(number));
        }
        return sj.toString();
    }

    @VisibleForTesting
    static String buildAuthorYearCitation(List<BibEntry> entries, BibDatabaseContext ctx) {
        StringJoiner sj = new StringJoiner("; ", "(", ")");
        for (BibEntry entry : entries) {
            String authorPart = extractFirstAuthorLastName(entry);
            String year = entry.getResolvedFieldOrAlias(StandardField.YEAR, ctx.getDatabase())
                               .map(String::trim)
                               .filter(y -> !y.isEmpty())
                               .orElse("n.d."); // apa-like fallback when year is missing
            sj.add(authorPart + ", " + year);
        }
        return sj.toString();
    }

    @VisibleForTesting
    static String extractFirstAuthorLastName(BibEntry entry) {
        // use natbib-like author formatting: 1 -> "Last", 2 -> "Last and Last", 3+ -> "Last et al."
        return entry.getField(StandardField.AUTHOR)
                    .map(AuthorList::parse)
                    .map(AuthorList::getAsNatbib)
                    .orElse("?");
    }

    private boolean checkPreceedingSpace(XTextCursor cursor) {
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());
        if (!checkCursor.goLeft((short) 1, true)) {
            return true;
        }
        String s = checkCursor.getString();
        return " ".equals(s) || s.matches("\\R");
    }
}
