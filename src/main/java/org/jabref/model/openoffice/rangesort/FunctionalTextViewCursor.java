package org.jabref.model.openoffice.rangesort;

import java.util.Arrays;
import java.util.Objects;

import org.jabref.model.openoffice.uno.UnoCursor;
import org.jabref.model.openoffice.uno.UnoSelection;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursor;

/*
 * A problem with XTextViewCursor: if it is not in text, then we get a crippled version that does
 * not support viewCursor.getStart() or viewCursor.gotoRange(range,false), and will throw an
 * exception instead.
 *
 * Here we manipulate the cursor via XSelectionSupplier.getSelection and XSelectionSupplier.select
 * to move it to the text.
 *
 * Seems to work when the user selected a frame or image.
 * In these cases restoring the selection works, too.
 *
 * When the cursor is in a comment (referred to as "annotation" in OO API) then initialSelection is
 * null, and select() fails to get a functional viewCursor.
 *
 * If FunctionalTextViewCursor.get() reports error, we have to ask the user to move the cursor into
 * the text part of the document.
 *
 * Usage:
 *
 *  OOResult<FunctionalTextViewCursor, String> fcursor = FunctionalTextViewCursor.get(doc, msg);
 *  if (fcursor.isError()) {
 *     ...
 *  } else {
 *      XTextViewCursor viewCursor = fcursor.get().getViewCursor();
 *      ...
 *      fc.restore();
 *  }
 *
 */
public class FunctionalTextViewCursor {

    /* The initial position of the cursor or null. */
    private XTextRange initialPosition;

    /* The initial selection in the document or null. */
    private XServiceInfo initialSelection;

    /* The view cursor, potentially moved from its original location. */
    private XTextViewCursor viewCursor;

    private FunctionalTextViewCursor(XTextRange initialPosition,
                                     XServiceInfo initialSelection,
                                     XTextViewCursor viewCursor) {
        this.initialPosition = initialPosition;
        this.initialSelection = initialSelection;
        this.viewCursor = viewCursor;
    }

    /*
     * Get a functional XTextViewCursor or an error message.
     *
     * The cursor position may differ from the location provided by the user.
     *
     * On failure the constructor restores the selection. On success, the caller may want to call
     * instance.restore() after finished using the cursor.
     */
    public static OOResult<FunctionalTextViewCursor, String> get(XTextDocument doc) {

        Objects.requireNonNull(doc);

        XTextRange initialPosition = null;
        XServiceInfo initialSelection = UnoSelection.getSelectionAsXServiceInfo(doc).orElse(null);
        XTextViewCursor viewCursor = UnoCursor.getViewCursor(doc).orElse(null);
        if (viewCursor != null) {
            try {
                initialPosition = UnoCursor.createTextCursorByRange(viewCursor);
                viewCursor.getStart();
                return OOResult.ok(new FunctionalTextViewCursor(initialPosition, initialSelection, viewCursor));
            } catch (com.sun.star.uno.RuntimeException ex) {
                // bad cursor
                viewCursor = null;
                initialPosition = null;
            }
        }

        if (initialSelection == null) {
            String errorMessage = ("Selection is not available: cannot provide a functional view cursor");
            return OOResult.error(errorMessage);
        } else if (!Arrays.stream(initialSelection.getSupportedServiceNames())
                   .anyMatch("com.sun.star.text.TextRanges"::equals)) {
            // initialSelection does not support TextRanges.
            // We need to change it (and the viewCursor with it).
            XTextRange newSelection = doc.getText().getStart();
            UnoSelection.select(doc, newSelection);
            viewCursor = UnoCursor.getViewCursor(doc).orElse(null);
        }

        if (viewCursor == null) {
            restore(doc, initialPosition, initialSelection);
            String errorMessage = "Could not get the view cursor";
            return OOResult.error(errorMessage);
        }

        try {
            viewCursor.getStart();
        } catch (com.sun.star.uno.RuntimeException ex) {
            restore(doc, initialPosition, initialSelection);
            String errorMessage = "The view cursor failed the functionality test";
            return OOResult.error(errorMessage);
        }

        return OOResult.ok(new FunctionalTextViewCursor(initialPosition, initialSelection, viewCursor));
    }

    public XTextViewCursor getViewCursor() {
        return viewCursor;
    }

    private static void restore(XTextDocument doc,
                                XTextRange initialPosition,
                                XServiceInfo initialSelection) {

        if (initialPosition != null) {
            XTextViewCursor viewCursor = UnoCursor.getViewCursor(doc).orElse(null);
            if (viewCursor != null) {
                viewCursor.gotoRange(initialPosition, false);
                return;
            }
        }
        if (initialSelection != null) {
            UnoSelection.select(doc, initialSelection);
        }
    }

    /* Restore initial state of viewCursor (possibly by restoring selection) if possible. */
    public void restore(XTextDocument doc) {
        FunctionalTextViewCursor.restore(doc, initialPosition, initialSelection);
    }
}
