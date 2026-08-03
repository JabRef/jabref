package org.jabref.logic.openoffice;

import java.util.ArrayList;
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
import org.jabref.model.openoffice.uno.UnoNameAccess;
import org.jabref.model.openoffice.uno.UnoTextSection;

import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public final class CSLBibliographyMark {
    private static final String JABREF_BIBLIOGRAPHY_SECTION_NAME = "JR_bib";
    private static final String ZOTERO_BIBLIOGRAPHY_SECTION_NAME_PREFIX = "ZOTERO_BIBL ";
    private static final String ZOTERO_BIBLIOGRAPHY_CODE = "{\"uncited\":[],\"omitted\":[],\"custom\":[]}";
    private static final String ZOTERO_BIBLIOGRAPHY_SUFFIX = " CSL_BIBLIOGRAPHY";
    private static final Logger LOGGER = LoggerFactory.getLogger(CSLBibliographyMark.class);

    private CSLBibliographyMark() {
    }

    public static void rebuildCSLBibliography(XTextDocument doc,
                                              CSLCitationOOAdapter cslCitationOOAdapter,
                                              List<BibEntry> entries,
                                              CitationStyle citationStyle,
                                              BibDatabaseContext bibDatabaseContext,
                                              BibEntryTypesManager bibEntryTypesManager,
                                              OpenOfficeReferenceMarkFormat referenceMarkFormat)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        LOGGER.debug("Starting to rebuild CSL bibliography");

        clearAllCSLBibTextSectionContent(doc);
        Optional<XTextRange> sectionRange = getBibliographyRange(doc, referenceMarkFormat);
        if (sectionRange.isEmpty()) {
            LOGGER.debug("Bibliography section not found. Creating new section.");
            createCSLBibTextSection(doc, referenceMarkFormat);
        }

        populateCSLBibTextSection(doc, cslCitationOOAdapter, entries, citationStyle, bibDatabaseContext, bibEntryTypesManager, referenceMarkFormat);
        LOGGER.debug("Finished rebuilding CSL bibliography");
    }

    private static Optional<XTextRange> getBibliographyRange(XTextDocument doc, OpenOfficeReferenceMarkFormat referenceMarkFormat)
            throws NoDocumentException, WrappedTargetException {
        LOGGER.debug("Attempting to get bibliography range");
        Optional<XTextRange> range = getBibliographyRanges(doc, referenceMarkFormat).stream().findFirst();
        LOGGER.debug("Bibliography range found: {}", range.isPresent());
        return range;
    }

    private static List<XTextRange> getBibliographyRanges(XTextDocument doc, OpenOfficeReferenceMarkFormat referenceMarkFormat)
            throws NoDocumentException, WrappedTargetException {
        return switch (referenceMarkFormat) {
            case JABREF_ONLY ->
                    UnoTextSection.getAnchor(doc, JABREF_BIBLIOGRAPHY_SECTION_NAME).stream().toList();
            case ZOTERO_COMPATIBLE ->
                    getZoteroBibliographyRanges(doc);
        };
    }

    private static List<XTextRange> getZoteroBibliographyRanges(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        List<XTextRange> ranges = new ArrayList<>();
        XNameAccess nameAccess = UnoTextSection.getNameAccess(doc);
        for (String name : nameAccess.getElementNames()) {
            if (name.startsWith(ZOTERO_BIBLIOGRAPHY_SECTION_NAME_PREFIX)) {
                UnoNameAccess.getTextContentByName(nameAccess, name)
                             .map(XTextContent::getAnchor)
                             .ifPresent(ranges::add);
            }
        }
        return ranges;
    }

    private static void createCSLBibTextSection(XTextDocument doc, OpenOfficeReferenceMarkFormat referenceMarkFormat)
            throws CreationException {
        LOGGER.debug("Creating new CSL bibliography section");
        XTextCursor textCursor = doc.getText().createTextCursor();
        textCursor.gotoEnd(false);
        DocumentAnnotation annotation = new DocumentAnnotation(doc, createBibliographySectionName(referenceMarkFormat), textCursor, false);
        UnoTextSection.create(annotation);
        LOGGER.debug("CSL bibliography section created");
    }

    private static String createBibliographySectionName(OpenOfficeReferenceMarkFormat referenceMarkFormat) {
        return switch (referenceMarkFormat) {
            case JABREF_ONLY ->
                    JABREF_BIBLIOGRAPHY_SECTION_NAME;
            case ZOTERO_COMPATIBLE ->
                    ZOTERO_BIBLIOGRAPHY_SECTION_NAME_PREFIX
                            + ZOTERO_BIBLIOGRAPHY_CODE
                            + ZOTERO_BIBLIOGRAPHY_SUFFIX
                            + " "
                            + ZoteroReferenceMark.createRandomSuffix();
        };
    }

    private static void clearAllCSLBibTextSectionContent(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        LOGGER.debug("Clearing CSL bibliography section content");
        for (OpenOfficeReferenceMarkFormat referenceMarkFormat : OpenOfficeReferenceMarkFormat.values()) {
            for (XTextRange range : getBibliographyRanges(doc, referenceMarkFormat)) {
                XTextCursor cursor = range.getText().createTextCursorByRange(range);
                cursor.setString("");
                LOGGER.debug("CSL bibliography section content cleared");
            }
        }
    }

    private static void populateCSLBibTextSection(XTextDocument doc,
                                                  CSLCitationOOAdapter cslCitationOOAdapter,
                                                  List<BibEntry> entries,
                                                  CitationStyle citationStyle,
                                                  BibDatabaseContext bibDatabaseContext,
                                                  BibEntryTypesManager bibEntryTypesManager,
                                                  OpenOfficeReferenceMarkFormat referenceMarkFormat)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        LOGGER.debug("Populating CSL bibliography section");

        XTextRange sectionRange = getBibliographyRange(doc, referenceMarkFormat).orElseThrow(() -> {
            LOGGER.error("Bibliography section not found when trying to populate");
            return new CreationException("Bibliography section not found");
        });
        XTextCursor cursor = sectionRange.getText().createTextCursorByRange(sectionRange);

        switch (referenceMarkFormat) {
            case JABREF_ONLY ->
                    cslCitationOOAdapter.insertJabRefBibliography(cursor, citationStyle, entries, bibDatabaseContext, bibEntryTypesManager);
            case ZOTERO_COMPATIBLE ->
                    cslCitationOOAdapter.insertZoteroBibliography(cursor, citationStyle, entries, bibDatabaseContext, bibEntryTypesManager);
        }

        LOGGER.debug("CSL bibliography section population completed");
    }
}
