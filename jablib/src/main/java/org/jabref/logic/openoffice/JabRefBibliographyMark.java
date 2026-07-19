package org.jabref.logic.openoffice;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.oocsltext.CSLCitationOOAdapter;
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
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class JabRefBibliographyMark {
    private static final String BIBLIOGRAPHY_SECTION_NAME = "JR_bib";
    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefBibliographyMark.class);

    private Optional<XTextRange> getBibliographyRange(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        LOGGER.debug("Attempting to get bibliography range");
        Optional<XTextRange> range = UnoTextSection.getAnchor(doc, BIBLIOGRAPHY_SECTION_NAME);
        LOGGER.debug("Bibliography range found: {}", range.isPresent());
        return range;
    }

    public void rebuildCSLBibliography(XTextDocument doc,
                                       CSLCitationOOAdapter cslCitationOOAdapter,
                                       List<BibEntry> entries,
                                       CitationStyle citationStyle,
                                       BibDatabaseContext bibDatabaseContext,
                                       BibEntryTypesManager bibEntryTypesManager)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        LOGGER.debug("Starting to rebuild CSL bibliography");

        // Ensure the bibliography section exists
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isEmpty()) {
            LOGGER.debug("Bibliography section not found. Creating new section.");
            createCSLBibTextSection(doc);
        } else {
            LOGGER.debug("Bibliography section found. Clearing content.");
            clearCSLBibTextSectionContent(doc);
        }

        populateCSLBibTextSection(doc, cslCitationOOAdapter, entries, citationStyle, bibDatabaseContext, bibEntryTypesManager);
        LOGGER.debug("Finished rebuilding CSL bibliography");
    }

    private void createCSLBibTextSection(XTextDocument doc)
            throws CreationException {
        LOGGER.debug("Creating new CSL bibliography section");
        XTextCursor textCursor = doc.getText().createTextCursor();
        textCursor.gotoEnd(false);
        DocumentAnnotation annotation = new DocumentAnnotation(doc, BIBLIOGRAPHY_SECTION_NAME, textCursor, false);
        UnoTextSection.create(annotation);
        LOGGER.debug("CSL bibliography section created");
    }

    private void clearCSLBibTextSectionContent(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        LOGGER.debug("Clearing CSL bibliography section content");
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        sectionRange.ifPresentOrElse(
                range -> {
                    XTextCursor cursor = doc.getText().createTextCursorByRange(range);
                    cursor.setString("");
                    LOGGER.debug("CSL bibliography section content cleared");
                },
                () -> LOGGER.warn("Failed to clear CSL bibliography section: section not found"));
    }

    private void populateCSLBibTextSection(XTextDocument doc,
                                           CSLCitationOOAdapter cslCitationOOAdapter,
                                           List<BibEntry> entries,
                                           CitationStyle citationStyle,
                                           BibDatabaseContext bibDatabaseContext,
                                           BibEntryTypesManager bibEntryTypesManager)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        LOGGER.debug("Populating CSL bibliography section");

        XTextRange sectionRange = getBibliographyRange(doc).orElseThrow(() -> {
            LOGGER.error("Bibliography section not found when trying to populate");
            return new CreationException("Bibliography section not found");
        });
        XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange);

        cslCitationOOAdapter.insertBibliography(cursor, citationStyle, entries, bibDatabaseContext, bibEntryTypesManager);

        LOGGER.debug("CSL bibliography section population completed");
    }
}
