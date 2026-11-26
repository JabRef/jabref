package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.uno.NamedTextContentPlacement;
import org.jabref.logic.openoffice.uno.OOTextWriter;
import org.jabref.logic.openoffice.uno.UnoBookmark;
import org.jabref.logic.openoffice.uno.UnoTextSection;
import org.jabref.logic.openoffice.uno.WriterDocumentAccessException;
import org.jabref.logic.openoffice.uno.WriterOperationException;
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

    public static Optional<XTextRange> range(XTextDocument document) throws WriterDocumentAccessException, WrappedTargetException {
        return UnoTextSection.getAnchor(document, NAME);
    }

    public void rebuild(XTextDocument document, List<OOText> paragraphs, boolean jStyle)
            throws WriterDocumentAccessException, WrappedTargetException, WriterOperationException {
        Optional<XTextRange> existing = range(document);
        if (existing.isEmpty()) {
            XTextCursor end = document.getText().createTextCursor();
            end.gotoEnd(false);
            UnoTextSection.create(new NamedTextContentPlacement(document, NAME, end, false));
        }
        XTextRange anchor = range(document).orElseThrow(() -> new WriterOperationException("Cannot create bibliography section"));
        XTextCursor cursor = anchor.getText().createTextCursorByRange(anchor);
        cursor.setString("");
        OOTextWriter.removeDirectFormatting(cursor);
        for (OOText paragraph : paragraphs) {
            OOTextWriter.write(document, cursor, paragraph);
            cursor.collapseToEnd();
        }
        UnoBookmark.removeIfExists(document, END);
        if (jStyle) {
            XTextRange populated = range(document).orElseThrow(() -> new WriterOperationException("Bibliography section disappeared"));
            XTextCursor initial = populated.getText().createTextCursorByRange(populated);
            initial.collapseToStart();
            if (initial.goRight((short) 1, true) && "\n".equals(initial.getString())) {
                initial.setString("");
            }
            UnoBookmark.create(new NamedTextContentPlacement(document, END, cursor, true));
        }
    }
}
