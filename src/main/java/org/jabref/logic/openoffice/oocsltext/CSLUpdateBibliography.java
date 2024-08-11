package org.jabref.logic.openoffice.oocsltext;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoTextSection;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

public class CSLUpdateBibliography {

    private static final String CSL_BIB_SECTION_NAME = "CSL_bibliography";
    private static final Logger LOGGER = Logger.getLogger(CSLUpdateBibliography.class.getName());

    private CSLUpdateBibliography() {
    }

    public static Optional<XTextRange> getBibliographyRange(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        LOGGER.info("Attempting to get bibliography range");
        Optional<XTextRange> range = UnoTextSection.getAnchor(doc, CSL_BIB_SECTION_NAME);
        LOGGER.info("Bibliography range found: " + range.isPresent());
        return range;
    }

    /**
     * Rebuilds the bibliography using CSL.
     */
    public static void rebuildCSLBibliography(XTextDocument doc,
                                              CSLCitationOOAdapter cslAdapter,
                                              List<BibEntry> entries,
                                              CitationStyle citationStyle,
                                              BibDatabaseContext bibDatabaseContext,
                                              BibEntryTypesManager bibEntryTypesManager)
            throws WrappedTargetException, NoDocumentException, CreationException {
        LOGGER.info("Starting to rebuild CSL bibliography");

        // Ensure the bibliography section exists
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isEmpty()) {
            LOGGER.info("Bibliography section not found. Creating new section.");
            createCSLBibTextSection(doc);
        } else {
            LOGGER.info("Bibliography section found. Clearing content.");
            clearCSLBibTextSectionContent(doc);
        }

        populateCSLBibTextSection(doc, cslAdapter, entries, citationStyle, bibDatabaseContext, bibEntryTypesManager);
        LOGGER.info("Finished rebuilding CSL bibliography");
    }

    private static void createCSLBibTextSection(XTextDocument doc)
            throws CreationException {
        LOGGER.info("Creating new CSL bibliography section");
        XTextCursor textCursor = doc.getText().createTextCursor();
        textCursor.gotoEnd(false);
        DocumentAnnotation annotation = new DocumentAnnotation(doc, CSL_BIB_SECTION_NAME, textCursor, false);
        UnoTextSection.create(annotation);
        LOGGER.info("CSL bibliography section created");
    }

    private static void clearCSLBibTextSectionContent(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        LOGGER.info("Clearing CSL bibliography section content");
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isPresent()) {
            XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange.get());
            cursor.setString("");
            LOGGER.info("CSL bibliography section content cleared");
        } else {
            LOGGER.warning("Failed to clear CSL bibliography section: section not found");
        }
    }

    private static void populateCSLBibTextSection(XTextDocument doc,
                                                  CSLCitationOOAdapter cslAdapter,
                                                  List<BibEntry> entries,
                                                  CitationStyle citationStyle,
                                                  BibDatabaseContext bibDatabaseContext,
                                                  BibEntryTypesManager bibEntryTypesManager)
            throws WrappedTargetException, NoDocumentException, CreationException {
        LOGGER.info("Populating CSL bibliography section");

        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isEmpty()) {
            LOGGER.severe("Bibliography section not found when trying to populate");
            throw new IllegalStateException("Bibliography section not found");
        }

        XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange.get());

        // Use CSLCitationOOAdapter to insert the bibliography
        cslAdapter.insertBibliography(cursor, citationStyle, entries, bibDatabaseContext, bibEntryTypesManager);
        LOGGER.info("Bibliography inserted using CSLCitationOOAdapter");

        cursor.collapseToEnd();
        LOGGER.info("CSL bibliography section population completed");
    }
}
