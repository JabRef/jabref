package org.jabref.logic.openoffice.oocsltext;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.style.BstStyle;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.DocumentAnnotation;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoTextSection;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Manages the named bibliography text section used by the BST path, mirroring [CSLUpdateBibliography].
///
/// On the first call the section is created at the end of the document.
/// On subsequent calls the existing section is cleared and repopulated in place,
/// so repeated "Sync bibliography" calls never duplicate the References section.
public class BstUpdateBibliography {

    private static final String BST_BIBLIOGRAPHY_SECTION_NAME = "JR_bst_bib";
    private static final Logger LOGGER = LoggerFactory.getLogger(BstUpdateBibliography.class);

    /// Creates or clears the BST bibliography section, then populates it.
    public void rebuildBstBibliography(XTextDocument doc,
                                       BstCitationOOAdapter bstCitationOOAdapter,
                                       BstStyle style,
                                       List<BibEntry> entries,
                                       BibDatabaseContext ctx)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException,
            IOException, InterruptedException, WrappedTargetException {
        LOGGER.debug("Starting BST bibliography rebuild");

        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isEmpty()) {
            LOGGER.debug("BST bibliography section not found, creating new section");
            createBstBibTextSection(doc);
        } else {
            LOGGER.debug("BST bibliography section found, clearing content");
            clearBstBibTextSectionContent(doc);
        }

        populateBstBibTextSection(doc, bstCitationOOAdapter, style, entries, ctx);
        LOGGER.debug("BST bibliography rebuild complete");
    }

    public Optional<XTextRange> getBibliographyRange(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        return UnoTextSection.getAnchor(doc, BST_BIBLIOGRAPHY_SECTION_NAME);
    }

    private void createBstBibTextSection(XTextDocument doc) throws CreationException {
        XTextCursor textCursor = doc.getText().createTextCursor();
        textCursor.gotoEnd(false);
        DocumentAnnotation annotation = new DocumentAnnotation(doc, BST_BIBLIOGRAPHY_SECTION_NAME, textCursor, false);
        UnoTextSection.create(annotation);
        LOGGER.debug("BST bibliography section created");
    }

    private void clearBstBibTextSectionContent(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isPresent()) {
            XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange.get());
            cursor.setString("");
            LOGGER.debug("BST bibliography section content cleared");
        } else {
            LOGGER.warn("Could not clear BST bibliography section: section not found");
        }
    }

    private void populateBstBibTextSection(XTextDocument doc,
                                           BstCitationOOAdapter bstCitationOOAdapter,
                                           BstStyle style,
                                           List<BibEntry> entries,
                                           BibDatabaseContext ctx)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException,
            IOException, InterruptedException, WrappedTargetException {
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isEmpty()) {
            throw new IllegalStateException("BST bibliography section not found when trying to populate");
        }
        XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange.get());
        bstCitationOOAdapter.insertBibliography(cursor, style, entries, ctx);
        LOGGER.debug("BST bibliography section populated");
    }
}
