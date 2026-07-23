package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
public class BSTCitationOOAdapter {

    private static final Logger LOGGER = LoggerFactory.getLogger(BSTCitationOOAdapter.class);

    private final XTextDocument document;
    private final BSTReferenceMarkManager markManager;
    private final PandocLatexConverter pandoc;
    private final OpenOfficePreferences openOfficePreferences;

    public BSTCitationOOAdapter(XTextDocument document, OpenOfficePreferences openOfficePreferences)
            throws WrappedTargetException, NoSuchElementException {
        this.document = document;
        this.openOfficePreferences = openOfficePreferences;
        this.markManager = new BSTReferenceMarkManager(document);
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

        BstVM bstVM;
        try {
            bstVM = style.createBstVM();
        } catch (IOException e) {
            LOGGER.warn("Could not load BST style: {}", style.getPath(), e);
            throw e;
        }
        BstEntryRenderer renderer = new BstEntryRenderer(bstVM);

        // Bibliography ordering strategy:
        // - NUMERIC mode: first-appearance (manager's citation numbers)
        // - AUTHOR_YEAR mode: style-defined order from the BST VM (e.g., APA alphabetical)
        boolean useNumberedBibliography = openOfficePreferences.getBstCitationFormat() == BstCitationFormat.NUMERIC;

        List<BibEntry> sorted = new ArrayList<>(entries);
        BibDatabase database = ctx.getDatabase();

        if (useNumberedBibliography) {
            // Keep numeric styles (e.g., IEEEtran) on first-appearance
            sorted.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(keyOrId(entry))));
        } else {
            // Compute the style-driven order once and sort accordingly
            Map<String, Integer> styleOrder = computeStyleOrderNumbers(renderer, bstVM, sorted, database);
            sorted.sort(Comparator.comparingInt(entry -> styleOrder.getOrDefault(keyOrId(entry), Integer.MAX_VALUE)));
        }

        for (BibEntry entry : sorted) {
            String identifier = keyOrId(entry);
            String latex = renderer.renderEntryToLatex(entry, database);

            String norm = BSTFormatUtils.normalizeLegacyForPandoc(latex);

            String html = pandoc.latexToHtml(norm);

            String body = BSTFormatUtils.convertPandocHtmlToOOText(html);

            String finalLine;
            if (useNumberedBibliography) {
                int number = markManager.getCitationNumber(identifier);
                finalLine = "[" + number + "] " + body;
            } else {
                finalLine = body;
            }

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
        return markManager.hasCitationForIdentifier(keyOrId(entry));
    }

    private String buildNumericCitation(List<BibEntry> entries) {
        // Use the manager's current numbering (may be style-order or first-appearance),
        // but present numbers in ascending order inside a multi-entry bracket.
        List<Integer> numbers = new ArrayList<>(entries.size());
        for (BibEntry entry : entries) {
            numbers.add(markManager.getCitationNumber(keyOrId(entry)));
        }
        numbers.sort(Integer::compareTo);
        StringJoiner joiner = new StringJoiner(", ", "[", "]");
        numbers.forEach(number -> joiner.add(String.valueOf(number)));
        return joiner.toString();
    }

    @VisibleForTesting
    static String buildAuthorYearCitation(List<BibEntry> entries, BibDatabaseContext ctx) {
        StringJoiner joiner = new StringJoiner("; ", "(", ")");
        for (BibEntry entry : entries) {
            String authorPart = extractFirstAuthorLastName(entry);
            String year = entry.getResolvedFieldOrAlias(StandardField.YEAR, ctx.getDatabase())
                               .map(String::trim)
                               .filter(y -> !y.isEmpty())
                               .orElse("n.d."); // apa-like fallback when year is missing
            joiner.add(authorPart + ", " + year);
        }
        return joiner.toString();
    }

    @VisibleForTesting
    static String extractFirstAuthorLastName(BibEntry entry) {
        // use natbib-like author formatting: 1 -> "Last", 2 -> "Last and Last", 3+ -> "Last et al."
        return entry.getField(StandardField.AUTHOR)
                    .map(AuthorList::parse)
                    .map(AuthorList::getAsNatbib)
                    .orElse("?");
    }

    @VisibleForTesting
    static String keyOrId(BibEntry entry) {
        return entry.getCitationKey().orElse(entry.getId());
    }

    // Compute identifier -> number map using the BST VM’s bibliography sort order.
    // For entries missing a citation key, we temporarily assign keyOrId as the key,
    // so the emitted \bibitem{...} contains a stable identifier we can parse back.
    @VisibleForTesting
    static Map<String, Integer> computeStyleOrderNumbers(BstEntryRenderer renderer, BstVM bstVM, List<BibEntry> entries, BibDatabase database) {
        // Clone entries and ensure each has a non-empty key matching keyOrId
        List<BibEntry> normalized = new ArrayList<>(entries.size());
        for (BibEntry entry : entries) {
            BibEntry entryCopy = new BibEntry(entry);
            if (entryCopy.getCitationKey().isEmpty()) {
                entryCopy = entryCopy.withCitationKey(keyOrId(entry));
            }
            normalized.add(entryCopy);
        }
        // Render all entries at once to get style-driven order in thebibliography
        String renderedBibliography = bstVM.render(normalized, database);
        Pattern bibitemPattern = Pattern.compile("\\\\bibitem(?:\\[[^]]*])?\\{([^}]*)}");
        Matcher bibitemMatcher = bibitemPattern.matcher(renderedBibliography);
        int order = 1;
        Map<String, Integer> emittedKeyOrder = new LinkedHashMap<>();
        while (bibitemMatcher.find()) {
            String key = bibitemMatcher.group(1);
            if (!emittedKeyOrder.containsKey(key)) {
                emittedKeyOrder.put(key, order++);
            }
        }
        // Map from emitted key back to identifier (for entries where we substituted)
        Map<String, String> keyToIdentifier = new HashMap<>();
        for (BibEntry entry : normalized) {
            String key = entry.getCitationKey().orElse("");
            if (!key.isEmpty()) {
                keyToIdentifier.put(key, keyOrId(entry));
            }
        }
        Map<String, Integer> identifierToNumber = new LinkedHashMap<>();
        emittedKeyOrder.forEach((key, index) -> identifierToNumber.put(keyToIdentifier.getOrDefault(key, key), index));
        return identifierToNumber;
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
