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
import org.jabref.model.openoffice.uno.UnoNameAccess;
import org.jabref.model.openoffice.uno.UnoReferenceMark;
import org.jabref.model.openoffice.uno.UnoTextSection;

import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;

@NullMarked
public final class ZoteroBibliographyMark {
    public static final String PREFIX = "ZOTERO_BIBL ";

    private static final String CODE = "{\"uncited\":[],\"omitted\":[],\"custom\":[]}";
    private static final String BIBLIOGRAPHY_SUFFIX = " CSL_BIBLIOGRAPHY";

    public static String buildName() {
        return PREFIX + CODE + BIBLIOGRAPHY_SUFFIX + " " + ZoteroReferenceMark.createRandomSuffix();
    }

    public static boolean isBibliographyMarkName(String name) {
        return name.startsWith(PREFIX);
    }

    public void rebuildCSLBibliography(XTextDocument doc,
                                       CSLCitationOOAdapter cslCitationOOAdapter,
                                       List<BibEntry> entries,
                                       CitationStyle citationStyle,
                                       BibDatabaseContext bibDatabaseContext,
                                       BibEntryTypesManager bibEntryTypesManager)
            throws com.sun.star.uno.Exception, NoDocumentException, CreationException {
        Optional<XTextContent> bibliographyTextSection = getTextSection(doc);
        if (bibliographyTextSection.isPresent()) {
            XTextRange range = bibliographyTextSection.get().getAnchor();
            XTextCursor cursor = range.getText().createTextCursorByRange(range);
            cursor.setString("");
            cslCitationOOAdapter.insertZoteroBibliography(cursor, citationStyle, entries, bibDatabaseContext, bibEntryTypesManager);
            return;
        }

        Optional<XTextContent> bibliographyReferenceMark = getReferenceMark(doc);
        XTextCursor cursor;
        if (bibliographyReferenceMark.isPresent()) {
            XTextContent referenceMark = bibliographyReferenceMark.orElseThrow();
            Optional<XTextRange> range = Optional.ofNullable(referenceMark.getAnchor());
            if (range.isPresent()) {
                XTextRange existingBibliographyRange = range.orElseThrow();
                XText text = existingBibliographyRange.getText();
                cursor = text.createTextCursorByRange(existingBibliographyRange);
                cursor.setString("");
                text.removeTextContent(referenceMark);
            } else {
                cursor = doc.getText().createTextCursor();
                cursor.gotoEnd(false);
            }
        } else {
            cursor = doc.getText().createTextCursor();
            cursor.gotoEnd(false);
        }

        XNamed zoteroBibliographyTextSection = UnoTextSection.create(new DocumentAnnotation(
                doc,
                buildName(),
                cursor,
                false));
        XTextRange sectionRange = UnoTextSection.getAnchor(doc, zoteroBibliographyTextSection.getName())
                                                .orElseThrow(() -> new CreationException("Could not create Zotero bibliography text section"));
        XTextCursor sectionCursor = sectionRange.getText().createTextCursorByRange(sectionRange);
        cslCitationOOAdapter.insertZoteroBibliography(sectionCursor, citationStyle, entries, bibDatabaseContext, bibEntryTypesManager);
    }

    private Optional<XTextContent> getTextSection(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        XNameAccess nameAccess = UnoTextSection.getNameAccess(doc);
        for (String name : nameAccess.getElementNames()) {
            if (isBibliographyMarkName(name)) {
                return UnoNameAccess.getTextContentByName(nameAccess, name);
            }
        }
        return Optional.empty();
    }

    private Optional<XTextContent> getReferenceMark(XTextDocument doc)
            throws NoDocumentException, WrappedTargetException {
        for (String name : UnoReferenceMark.getListOfNames(doc)) {
            if (isBibliographyMarkName(name)) {
                return UnoReferenceMark.getAsTextContent(doc, name);
            }
        }
        return Optional.empty();
    }
}
