package org.jabref.logic.openoffice.frontend;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.JabRefException;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.ootext.OOTextIntoOO;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Update document: citation marks and bibliography
 */
public class UpdateCitationMarkers {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateCitationMarkers.class);

    /**
     * Visit each reference mark in referenceMarkNames, overwrite its
     * text content.
     *
     * After each fillCitationMarkInCursor call check if we lost the
     * BIB_SECTION_NAME bookmark and recreate it if we did.
     *
     * @param fr
     *
     * @param style Bibliography style to use.
     *
     */
    public static void applyNewCitationMarkers(XTextDocument doc, OOFrontend fr, OOBibStyle style)
        throws
        NoDocumentException,
        UnknownPropertyException,
        CreationException,
        WrappedTargetException,
        PropertyVetoException,
        NoSuchElementException,
        JabRefException {

        CitationGroups citationGroups = fr.citationGroups;

        for (CitationGroup cg : citationGroups.getCitationGroupsUnordered()) {

            boolean withText = (cg.citationType != CitationType.INVISIBLE_CIT);
            Optional<OOText> marker = cg.getCitationMarker();

            if (!marker.isPresent()) {
                String msg = String.format("applyNewCitationMarkers: no marker for %s",
                                           cg.cgid.citationGroupIdAsString());
                LOGGER.warn(msg);
                continue;
            }

            if (withText && marker.isPresent()) {

                XTextCursor cursor = fr.getFillCursorForCitationGroup(doc, cg);

                fillCitationMarkInCursor(doc, cursor, marker.get(), withText, style);

                fr.cleanFillCursorForCitationGroup(doc, cg);
            }

        }
    }

    public static void fillCitationMarkInCursor(XTextDocument doc,
                                                XTextCursor cursor,
                                                OOText citationText,
                                                boolean withText,
                                                OOBibStyle style)
    throws
        UnknownPropertyException,
        PropertyVetoException,
        WrappedTargetException,
        NoSuchElementException,
        CreationException,
        IllegalArgumentException {

        Objects.requireNonNull(cursor);
        Objects.requireNonNull(citationText);
        Objects.requireNonNull(style);

        if (withText) {
            OOText citationText2 = style.decorateCitationMarker(citationText);
            // inject a ZERO_WIDTH_SPACE to hold the initial character format
            final String ZERO_WIDTH_SPACE = "\u200b";
            citationText2 = OOText.fromString(ZERO_WIDTH_SPACE + citationText2.toString());
            OOTextIntoOO.write(doc, cursor, citationText2);
        } else {
            cursor.setString("");
        }
    }

    /**
     *  Inserts a citation group in the document: creates and fills it.
     *
     * @param citationKeys BibTeX keys of
     * @param pageInfos
     * @param citationType
     *
     * @param citationText Text for the citation. A citation mark or
     *             placeholder if not yet available.
     *
     * @param position Location to insert at.
     * @param style
     * @param insertSpaceAfter A space inserted after the reference
     *             mark makes it easier to separate from the text
     *             coming after. But is not wanted when we recreate a
     *             reference mark.
     */
    public static void createAndFillCitationGroup(OOFrontend fr,
                                                  XTextDocument doc,
                                                  List<String> citationKeys,
                                                  List<Optional<OOText>> pageInfos,
                                                  CitationType citationType,
                                                  OOText citationText,
                                                  XTextCursor position,
                                                  OOBibStyle style,
                                                  boolean insertSpaceAfter)
        throws
        UnknownPropertyException,
        NotRemoveableException,
        PropertyExistException,
        PropertyVetoException,
        WrappedTargetException,
        PropertyVetoException,
        IllegalArgumentException,
        CreationException,
        NoDocumentException,
        IllegalTypeException,
        NoSuchElementException {

        Objects.requireNonNull(pageInfos);
        if (pageInfos.size() != citationKeys.size()) {
            throw new IllegalArgumentException("pageInfos.size != citationKeys.size");
        }
        CitationGroup cg = fr.createCitationGroup(doc,
                                                  citationKeys,
                                                  pageInfos,
                                                  citationType,
                                                  position,
                                                  insertSpaceAfter);

        final boolean withText = citationType.withText();

        if (withText) {
            XTextCursor c2 = fr.getFillCursorForCitationGroup(doc, cg);

            UpdateCitationMarkers.fillCitationMarkInCursor(doc, c2, citationText, withText, style);

            fr.cleanFillCursorForCitationGroup(doc, cg);
        }
        position.collapseToEnd();
    }

}
