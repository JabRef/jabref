package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.StringJoiner;

import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.bst.BstEntryRenderer;
import org.jabref.logic.openoffice.bst.BstHtmlToOOText;
import org.jabref.logic.openoffice.bst.PandocLatexConverter;
import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

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
                    buildAuthorYearCitation(entries);
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

        org.jabref.logic.bst.BstVM vm;
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

        BibDatabase database = ctx.getDatabase();

        for (BibEntry entry : sorted) {
            String latex = renderer.renderEntryToLatex(entry, database);
            String html = pandoc.latexToHtml(latex);
            String body = BstHtmlToOOText.convert(html);

            int number = markManager.getCitationNumber(entry.getCitationKey().orElse(""));
            String withNumber = "[" + number + "] " + body;

            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(withNumber));
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

    private String buildAuthorYearCitation(List<BibEntry> entries) {
        StringJoiner sj = new StringJoiner("; ", "(", ")");
        for (BibEntry entry : entries) {
            String authorPart = extractFirstAuthorLastName(entry);
            String year = entry.getField(StandardField.YEAR).orElse("?");
            sj.add(authorPart + ", " + year);
        }
        return sj.toString();
    }

    private String extractFirstAuthorLastName(BibEntry entry) {
        return entry.getField(StandardField.AUTHOR)
                    .map(AuthorList::parse)
                    .filter(list -> !list.isEmpty())
                    .map(list -> {
                        Author first = list.getAuthor(0);
                        String lastName = first.getFamilyName().orElse("?");
                        return list.getNumberOfAuthors() > 1 ? lastName + " et al." : lastName;
                    })
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
