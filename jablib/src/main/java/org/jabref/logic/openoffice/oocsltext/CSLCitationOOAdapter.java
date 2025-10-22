package org.jabref.logic.openoffice.oocsltext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStyleGenerator;
import org.jabref.logic.citationstyle.CitationStyleOutputFormat;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.style.OOStyle;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.ootext.OOFormat;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.uno.CreationException;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Exception;

/**
 * This class processes CSL citations in JabRef and interacts directly with LibreOffice using an XTextDocument instance.
 * It is tightly coupled with {@link CSLReferenceMarkManager} for management of reference marks tied to the CSL citations.
 * It uses {@link OpenOfficePreferences} to retrieve the initial style (last selected style), the bibliography title and its paragraph style.
 * Any method in this class is NOT supposed to be moved (OR internally refactored without complete understanding - see implementation note).
 *
 * @implNote UNO API calls are expensive, and any additional operation slows down the net "macro-task" we are trying to achieve in the document.
 * These "additional" operations may or may not be visible at the level of code in the form of additional function calls.
 * In some cases, the same macro-task may be achieved by two different orders of actions, which may look semantically the same overall, but one order may result into more UNO API calls.
 * For example, see the comment inside {@link CSLCitationOOAdapter#insertCitation(XTextCursor, CitationStyle, List, BibDatabaseContext, BibEntryTypesManager) insertCitation}.
 */
public class CSLCitationOOAdapter {

    private static final CitationStyleOutputFormat HTML_OUTPUT_FORMAT = CitationStyleOutputFormat.HTML;

    private final XTextDocument document;
    private final CSLReferenceMarkManager markManager;
    private final Supplier<List<BibDatabaseContext>> databasesSupplier;
    private final BibEntryTypesManager bibEntryTypesManager;
    private final OpenOfficePreferences openOfficePreferences;

    private CitationStyle currentStyle;
    private boolean styleChanged;

    public CSLCitationOOAdapter(XTextDocument doc, Supplier<List<BibDatabaseContext>> databasesSupplier, OpenOfficePreferences openOfficePreferences, BibEntryTypesManager bibEntryTypesManager) throws WrappedTargetException, NoSuchElementException {
        this.document = doc;
        this.markManager = new CSLReferenceMarkManager(doc);
        this.databasesSupplier = databasesSupplier;
        this.bibEntryTypesManager = bibEntryTypesManager;
        this.openOfficePreferences = openOfficePreferences;

        OOStyle initialStyle = openOfficePreferences.getCurrentStyle(); // may be a jstyle, can still be used for detecting subsequent style changes in context of CSL
        if (initialStyle instanceof CitationStyle citationStyle) {
            this.currentStyle = citationStyle; // else the currentStyle purposely stays null, still causing a difference with the subsequent style if CSL (valid comparison)
        }

        markManager.readAndUpdateExistingMarks();
    }

    public void setStyle(CitationStyle newStyle) {
        if (currentStyle == null || !currentStyle.getName().equals(newStyle.getName())) {
            styleChanged = true;
            currentStyle = newStyle;
        } else {
            styleChanged = false;
        }
    }

    /**
     * Inserts a citation for a group of entries.
     * Comparable to LaTeX's \cite command.
     */
    public void insertCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws CreationException, Exception {
        setStyle(selectedStyle);

        // Placing this at the beginning reduces the number of updates needed by 1 (in the positive case)
        if (styleChanged) {
            updateAllCitationsWithNewStyle(currentStyle, false);
            styleChanged = false;
        }

        String style = selectedStyle.getSource();
        boolean isNumericStyle = selectedStyle.isNumericStyle();
        boolean isAlphanumericStyle = selectedStyle.isAlphanumericStyle();

        String citation;

        if (isAlphanumericStyle) {
            citation = CSLFormatUtils.generateAlphanumericCitation(entries, bibDatabaseContext);
        } else {
            citation = CitationStyleGenerator.generateCitation(entries, style, HTML_OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager);
        }

        String formattedCitation = CSLFormatUtils.transformHTML(citation);

        if (isNumericStyle) {
            formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, entries);
        }

        OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedCitation));
        insertReferences(cursor, entries, ooText, isNumericStyle);
    }

    /**
     * Inserts in-text citations for a group of entries.
     * Comparable to LaTeX's \citet command.
     *
     * @implNote Very similar to the {@link #insertCitation(XTextCursor, CitationStyle, List, BibDatabaseContext, BibEntryTypesManager) insertCitation} method.
     */
    public void insertInTextCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws CreationException, Exception {
        setStyle(selectedStyle);

        if (styleChanged) {
            updateAllCitationsWithNewStyle(currentStyle, true);
            styleChanged = false;
        }

        String style = selectedStyle.getSource();
        boolean isNumericStyle = selectedStyle.isNumericStyle();
        boolean isAlphanumericStyle = selectedStyle.isAlphanumericStyle();

        Iterator<BibEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            BibEntry currentEntry = iterator.next();

            String inTextCitation;

            if (isAlphanumericStyle) {
                inTextCitation = CSLFormatUtils.generateAlphanumericInTextCitation(currentEntry, bibDatabaseContext);
            } else {
                inTextCitation = CitationStyleGenerator.generateCitation(List.of(currentEntry), style, HTML_OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager);
            }

            String formattedCitation = CSLFormatUtils.transformHTML(inTextCitation);
            String finalText;

            if (isNumericStyle) {
                formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, List.of(currentEntry));
                String prefix = CSLFormatUtils.generateAuthorPrefix(currentEntry, bibDatabaseContext);
                finalText = prefix + formattedCitation;
            } else if (isAlphanumericStyle) {
                finalText = formattedCitation;
            } else {
                finalText = CSLFormatUtils.changeToInText(formattedCitation);
            }

            if (iterator.hasNext()) {
                finalText += ",";
            }

            OOText ooText = OOFormat.setLocaleNone(OOText.fromString(finalText));
            insertReferences(cursor, List.of(currentEntry), ooText, isNumericStyle);
        }
    }

    /**
     * Inserts "empty" citations for a list of entries at the cursor to the document.
     * Adds the entries to the list for which bibliography is to be generated.
     */
    public void insertEmptyCitation(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries)
            throws CreationException, Exception {
        OOText emptyOOText = OOFormat.setLocaleNone(OOText.fromString(""));
        insertReferences(cursor, entries, emptyOOText, selectedStyle.isNumericStyle());
    }

    /**
     * Creates a "Bibliography" section in the document and inserts a list of references.
     * The list is generated based on the existing citations, in-text citations and empty citations in the document.
     */
    public void insertBibliography(XTextCursor cursor, CitationStyle selectedStyle, List<BibEntry> entries, BibDatabaseContext bibDatabaseContext, BibEntryTypesManager bibEntryTypesManager)
            throws WrappedTargetException, CreationException {
        if (!selectedStyle.hasBibliography()) {
            return;
        }

        boolean isNumericStyle = selectedStyle.isNumericStyle();

        OOText title = OOFormat.paragraph(OOText.fromString(openOfficePreferences.getCslBibliographyTitle()), openOfficePreferences.getCslBibliographyHeaderFormat());
        OOTextIntoOO.write(document, cursor, OOText.fromString(title.toString()));
        OOText ooBreak = OOFormat.paragraph(OOText.fromString(""), openOfficePreferences.getCslBibliographyBodyFormat());
        OOTextIntoOO.write(document, cursor, ooBreak);

        String style = selectedStyle.getSource();

        if (isNumericStyle) {
            // Sort entries based on their order of appearance in the document
            entries.sort(Comparator.comparingInt(entry -> markManager.getCitationNumber(entry.getCitationKey().orElse(""))));

            for (BibEntry entry : entries) {
                String bibliographyEntry = CitationStyleGenerator.generateBibliography(List.of(entry), style, HTML_OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager).getFirst();
                String citationKey = entry.getCitationKey().orElse("");
                int currentNumber = markManager.getCitationNumber(citationKey);
                String formattedBibliographyEntry = CSLFormatUtils.transformHTML(bibliographyEntry);
                formattedBibliographyEntry = CSLFormatUtils.updateSingleBibliographyNumber(formattedBibliographyEntry, currentNumber);

                OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedBibliographyEntry));
                OOTextIntoOO.write(document, cursor, ooText);
            }
        } else {
            // Ordering will be according to citeproc item data provider (default)
            List<String> bibliographyEntries = CitationStyleGenerator.generateBibliography(entries, style, HTML_OUTPUT_FORMAT, bibDatabaseContext, bibEntryTypesManager);

            for (String bibliographyEntry : bibliographyEntries) {
                String formattedBibliographyEntry = CSLFormatUtils.transformHTML(bibliographyEntry);

                OOText ooText = OOFormat.setLocaleNone(OOText.fromString(formattedBibliographyEntry));
                OOTextIntoOO.write(document, cursor, ooText);
            }
        }
    }

    /**
     * Inserts references and also adds a space before the citation if not already present ("smart space").
     */
    private void insertReferences(XTextCursor cursor, List<BibEntry> entries, OOText ooText, boolean isNumericStyle)
            throws CreationException, Exception {
        boolean preceedingSpaceExists;
        XTextCursor checkCursor = cursor.getText().createTextCursorByRange(cursor.getStart());

        // Check if we're at the start of the document - if yes we set the flag and don't insert a space
        if (!checkCursor.goLeft((short) 1, true)) {
            // We're at the start of the document
            preceedingSpaceExists = true;
        } else {
            // If not at the start of document, check if there is a space before
            preceedingSpaceExists = " ".equals(checkCursor.getString());
            // If not a space, check if it's a paragraph break
            if (!preceedingSpaceExists) {
                preceedingSpaceExists = checkCursor.getString().matches("\\R");
            }
        }
        markManager.insertReferenceIntoOO(entries, document, cursor, ooText, !preceedingSpaceExists, openOfficePreferences.getAddSpaceAfter());
        markManager.setRealTimeNumberUpdateRequired(isNumericStyle);
        markManager.readAndUpdateExistingMarks();
    }

    /**
     * Transforms the numbers in the citation to globally-unique (and thus, reusable) numbers.
     */
    private String updateSingleOrMultipleCitationNumbers(String citation, List<BibEntry> entries) {
        Pattern pattern = Pattern.compile("(\\D*)(\\d+)(\\D*)");
        Matcher matcher = pattern.matcher(citation);
        StringBuilder sb = new StringBuilder();
        Iterator<BibEntry> iterator = entries.iterator();

        while (matcher.find() && iterator.hasNext()) {
            String prefix = matcher.group(1);
            String suffix = matcher.group(3);

            int currentNumber = markManager.getCitationNumber(iterator.next().getCitationKey().orElse(""));

            matcher.appendReplacement(sb, Matcher.quoteReplacement(prefix + currentNumber + suffix));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * Ideally, the methods of this class are supposed to work with {@link CSLReferenceMarkManager}, and not {@link CSLReferenceMark} directly.
     * However, all "generation" of CSL style citations (via {@link CitationStyleGenerator}) occur in this class, and not in {@link CSLReferenceMarkManager}.
     * Furthermore, {@link CSLReferenceMarkManager} is not composed of {@link CitationStyle}.
     * Hence, we keep {@link CSLReferenceMarkManager} independent of {@link CitationStyleGenerator} and {@link CitationStyle}, and keep the following two methods here.
     */
    private void updateAllCitationsWithNewStyle(CitationStyle style, boolean isInTextStyle)
            throws Exception, CreationException {
        boolean isNumericStyle = style.isNumericStyle();
        boolean isAlphaNumericStyle = style.isAlphanumericStyle();

        /*
        Entries from multiple libraries may need to be updated, and new libraries could have been opened after the document connection
        So, to get all databases in real time without having to refresh the connection, we obtain all open databases via the state manager
        */

        // Collect all open databases
        List<BibDatabaseContext> databaseContexts = databasesSupplier.get();
        List<BibDatabase> databases = new ArrayList<>();
        for (BibDatabaseContext databaseContext : databaseContexts) {
            databases.add(databaseContext.getDatabase());
        }

        // We first get a list of all cited entries to create a unified database context
        List<BibEntry> citedEntries = databases.stream()
                                               .flatMap(db -> db.getEntries().stream())
                                               .filter(this::isCitedEntry)
                                               .toList();

        BibDatabase unifiedDatabase = new BibDatabase(citedEntries);
        BibDatabaseContext unifiedBibDatabaseContext = new BibDatabaseContext.Builder()
                .database(unifiedDatabase)
                .build();

        // Next, we get the list of reference marks sorted in order of appearance in the document
        List<CSLReferenceMark> marksInOrder = markManager.getMarksInOrder();

        if (isInTextStyle) {
            // Now, for each such reference mark, we get the entries to be updated
            for (CSLReferenceMark mark : marksInOrder) {
                List<String> citationKeys = mark.getCitationKeys();
                List<BibEntry> entries = citationKeys.stream()
                                                     .map(unifiedDatabase::getEntryByCitationKey)
                                                     .flatMap(Optional::stream)
                                                     .toList();

                StringBuilder finalText = new StringBuilder();
                Iterator<BibEntry> iterator = entries.iterator();

                while (iterator.hasNext()) {
                    BibEntry currentEntry = iterator.next();

                    // We re-generate the citation in the new style and update it in the document
                    String newCitation;

                    if (isAlphaNumericStyle) {
                        newCitation = CSLFormatUtils.generateAlphanumericInTextCitation(currentEntry, unifiedBibDatabaseContext);
                    } else {
                        newCitation = CitationStyleGenerator.generateCitation(List.of(currentEntry), style.getSource(), HTML_OUTPUT_FORMAT, unifiedBibDatabaseContext, bibEntryTypesManager);
                    }

                    String formattedCitation = CSLFormatUtils.transformHTML(newCitation);

                    if (isNumericStyle) {
                        formattedCitation = updateSingleOrMultipleCitationNumbers(formattedCitation, List.of(currentEntry));
                        String prefix = CSLFormatUtils.generateAuthorPrefix(currentEntry, unifiedBibDatabaseContext);
                        formattedCitation = prefix + formattedCitation;
                    } else if (!isAlphaNumericStyle) {
                        formattedCitation = CSLFormatUtils.changeToInText(formattedCitation);
                    }

                    finalText.append(formattedCitation);

                    if (iterator.hasNext()) {
                        finalText.append(",");
                    }
                }

                markManager.updateMarkAndTextWithNewStyle(mark, finalText.toString());
            }
        } else {
            // Same flow as above - for each such reference mark, we get the entries to be updated
            for (CSLReferenceMark mark : marksInOrder) {
                List<String> citationKeys = mark.getCitationKeys();
                List<BibEntry> entries = citationKeys.stream()
                                                     .map(unifiedDatabase::getEntryByCitationKey)
                                                     .flatMap(Optional::stream)
                                                     .toList();

                // We re-generate the citation in the new style and update it in the document
                String newCitation;

                if (isAlphaNumericStyle) {
                    newCitation = CSLFormatUtils.generateAlphanumericCitation(entries, unifiedBibDatabaseContext);
                } else {
                    newCitation = CitationStyleGenerator.generateCitation(entries, style.getSource(),
                            HTML_OUTPUT_FORMAT, unifiedBibDatabaseContext, bibEntryTypesManager);
                }

                String formattedCitation = CSLFormatUtils.transformHTML(newCitation);

                markManager.updateMarkAndTextWithNewStyle(mark, formattedCitation);
            }
        }
    }

    /**
     * Checks if an entry has already been cited before in the document.
     * Required for consistent numbering of numeric citations - if present, the number is to be reused, else a new number is to be assigned.
     */
    public boolean isCitedEntry(BibEntry entry) {
        String citationKey = entry.getCitationKey().orElse("");
        return markManager.hasCitationForKey(citationKey);
    }
}
