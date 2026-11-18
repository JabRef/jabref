package org.jabref.logic.openoffice.rangesort;

import java.util.Arrays;

import org.jabref.logic.openoffice.uno.UnoCursor;
import org.jabref.logic.openoffice.uno.UnoSelection;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.uno.RuntimeException;
import org.jspecify.annotations.NonNull;

/// Provides a usable [XTextViewCursor] while retaining the original cursor position or selection for restoration.
///
/// A view cursor outside text may not support position queries or movement to a range.
/// Acquisition may temporarily select document text when a frame or image is selected;
/// a cursor in a comment may remain unavailable.
///
/// Call `RestorableTextViewCursor.get(doc)` and check the returned [OOResult] for failure.
/// On success, use [#getViewCursor()] to access the cursor and call `restore(doc)` after use
/// to attempt to restore the original position or selection.
public class RestorableTextViewCursor {

    /* The initial position of the cursor or null. */
    private final XTextRange initialPosition;

    /* The initial selection in the document or null. */
    private final XServiceInfo initialSelection;

    /* The view cursor, potentially moved from its original location. */
    private final XTextViewCursor viewCursor;

    private RestorableTextViewCursor(XTextRange initialPosition,
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
     * instance.restore(doc) after finishing use of the cursor.
     */
    public static OOResult<RestorableTextViewCursor, String> get(@NonNull XTextDocument doc) {
        XTextRange initialPosition = null;
        XServiceInfo initialSelection = UnoSelection.getSelectionAsXServiceInfo(doc).orElse(null);
        XTextViewCursor viewCursor = UnoCursor.getViewCursor(doc).orElse(null);
        if (viewCursor != null) {
            try {
                initialPosition = UnoCursor.createTextCursorByRange(viewCursor);
                viewCursor.getStart();
                return OOResult.ok(new RestorableTextViewCursor(initialPosition, initialSelection, viewCursor));
            } catch (RuntimeException _) {
                // bad cursor
                viewCursor = null;
                initialPosition = null;
            }
        }

        if (initialSelection == null) {
            String errorMessage = "Selection is not available: cannot provide a functional view cursor";
            return OOResult.error(errorMessage);
        } else if (Arrays.stream(initialSelection.getSupportedServiceNames())
                         .noneMatch("com.sun.star.text.TextRanges"::equals)) {
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
        } catch (RuntimeException _) {
            restore(doc, initialPosition, initialSelection);
            String errorMessage = "The view cursor failed the functionality test";
            return OOResult.error(errorMessage);
        }

        return OOResult.ok(new RestorableTextViewCursor(initialPosition, initialSelection, viewCursor));
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
        RestorableTextViewCursor.restore(doc, initialPosition, initialSelection);
    }
}
