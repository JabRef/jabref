package org.jabref.logic.openoffice.frontend;

import java.util.Optional;

import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.OOFormatBibliography;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.style.CitedKeys;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoBookmark;
import org.jabref.model.openoffice.uno.UnoTextSection;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;

/*
 * Update document: citation marks and bibliography
 */
public class UpdateBibliography {

    private static final String BIB_SECTION_NAME = "JR_bib";
    private static final String BIB_SECTION_END_NAME = "JR_bib_end";

    private UpdateBibliography() {
        /**/
    }

    public static Optional<XTextRange> getBibliographyRange(XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {
        return UnoTextSection.getAnchor(doc, BIB_SECTION_NAME);
    }

    /**
     * Rebuilds the bibliography.
     */
    public static void rebuildBibTextSection(XTextDocument doc,
                                             OOFrontend frontend,
                                             CitedKeys bibliography,
                                             OOBibStyle style,
                                             boolean alwaysAddCitedOnPages)
        throws
        WrappedTargetException,
        CreationException,
        NoDocumentException {

        clearBibTextSectionContent2(doc);

        populateBibTextSection(doc,
                               frontend,
                               bibliography,
                               style,
                               alwaysAddCitedOnPages);
    }

    /**
     * Insert a paragraph break and create a text section for the bibliography.
     *
     * Only called from `clearBibTextSectionContent2`
     */
    private static void createBibTextSection2(XTextDocument doc)
        throws
        CreationException {

        // Always creating at the end of the document.
        // Alternatively, we could receive a cursor.
        XTextCursor textCursor = doc.getText().createTextCursor();
        textCursor.gotoEnd(false);
        UnoTextSection.create(doc, BIB_SECTION_NAME, textCursor, false);
    }

    /**
     *  Find and clear the text section BIB_SECTION_NAME to "",
     *  or create it.
     *
     * Only called from: `rebuildBibTextSection`
     *
     */
    private static void clearBibTextSectionContent2(XTextDocument doc)
        throws
        CreationException,
        NoDocumentException,
        WrappedTargetException {

        // Optional<XTextRange> sectionRange = UnoTextSection.getAnchor(doc, BIB_SECTION_NAME);
        Optional<XTextRange> sectionRange = getBibliographyRange(doc);
        if (sectionRange.isEmpty()) {
            createBibTextSection2(doc);
            return;
        } else {
            // Clear it
            XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange.get());
            cursor.setString("");
        }
    }

    /**
     * Only called from: `rebuildBibTextSection`
     *
     * Assumes the section named BIB_SECTION_NAME exists.
     */
    private static void populateBibTextSection(XTextDocument doc,
                                               OOFrontend frontend,
                                               CitedKeys bibliography,
                                               OOBibStyle style,
                                               boolean alwaysAddCitedOnPages)
        throws
        CreationException,
        IllegalArgumentException,
        NoDocumentException,
        WrappedTargetException {

        XTextRange sectionRange = getBibliographyRange(doc).orElseThrow(IllegalStateException::new);

        XTextCursor cursor = doc.getText().createTextCursorByRange(sectionRange);

        // emit the title of the bibliography
        OOTextIntoOO.removeDirectFormatting(cursor);
        OOText bibliographyText = OOFormatBibliography.formatBibliography(frontend.citationGroups,
                                                                          bibliography,
                                                                          style,
                                                                          alwaysAddCitedOnPages);
        OOTextIntoOO.write(doc, cursor, bibliographyText);
        cursor.collapseToEnd();

        // remove the initial empty paragraph from the section.
        sectionRange = getBibliographyRange(doc).orElseThrow(IllegalStateException::new);
        XTextCursor initialParagraph = doc.getText().createTextCursorByRange(sectionRange);
        initialParagraph.collapseToStart();
        initialParagraph.goRight((short) 1, true);
        initialParagraph.setString("");

        UnoBookmark.removeIfExists(doc, BIB_SECTION_END_NAME);
        UnoBookmark.create(doc, BIB_SECTION_END_NAME, cursor, true);

        cursor.collapseToEnd();
    }

}
