package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.DocumentAnnotation;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoBookmark;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.model.openoffice.ootext.OOText;

import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.NullMarked;

/// Owns the non-Zotero bibliography section. All external rendering has completed before rebuilding.
@NullMarked
public final class BibliographySection {

    public static final String NAME = "JR_bib";
    private static final String END = "JR_bib_end";

    public static Optional<XTextRange> range(XTextDocument document) throws NoDocumentException, WrappedTargetException {
        return UnoTextSection.getAnchor(document, NAME);
    }

    public void rebuild(XTextDocument document, List<OOText> paragraphs, boolean jStyle)
            throws NoDocumentException, WrappedTargetException, CreationException {
        Optional<XTextRange> existing = range(document);
        if (existing.isEmpty()) {
            XTextCursor end = document.getText().createTextCursor();
            end.gotoEnd(false);
            UnoTextSection.create(new DocumentAnnotation(document, NAME, end, false));
        }
        XTextRange anchor = range(document).orElseThrow(() -> new CreationException("Cannot create bibliography section"));
        XTextCursor cursor = anchor.getText().createTextCursorByRange(anchor);
        cursor.setString("");
        OOTextWriter.removeDirectFormatting(cursor);
        for (OOText paragraph : paragraphs) {
            OOTextWriter.write(document, cursor, paragraph);
            cursor.collapseToEnd();
        }
        UnoBookmark.removeIfExists(document, END);
        if (jStyle) {
            XTextRange populated = range(document).orElseThrow(() -> new CreationException("Bibliography section disappeared"));
            XTextCursor initial = populated.getText().createTextCursorByRange(populated);
            initial.collapseToStart();
            if (initial.goRight((short) 1, true) && "\n".equals(initial.getString())) {
                initial.setString("");
            }
            UnoBookmark.create(new DocumentAnnotation(document, END, cursor, true));
        }
    }
}
