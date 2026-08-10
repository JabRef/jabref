package org.jabref.model.openoffice.uno;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Executes a LibreOffice UI command against the document's controller.
///
/// Used to reach SwTextNode::DontExpandFormat, which has no UNO equivalent:
/// `.uno:ResetAttributes` -> SwDoc::ResetAttrs(bTextAttr=true) -> DontExpandFormat,
/// which sets the DontExpand flag on text hints ending at the view cursor, stopping
/// a ReferenceMark from absorbing the next typed character. See tdf#81720.
///
/// The flag is one-shot, but one is enough: after a character survives, the caret sits
/// past the mark's end and the bleed condition can no longer hold. What this does not
/// protect against is the caret returning to that boundary (click, delete, undo) — see
/// the repair path in `CSLReferenceMarkManager`.
@NullMarked
public class UnoDispatch {
    private static final Logger LOGGER = LoggerFactory.getLogger(UnoDispatch.class);
    private static final String DISPATCH_HELPER_SERVICE = "com.sun.star.frame.DispatchHelper";
    private static final String RESET_ATTRIBUTES_COMMAND = ".uno:ResetAttributes";
    private static final PropertyValue[] EMPTY_DISPATCH_ARGUMENTS = new PropertyValue[0];

    private UnoDispatch() {
    }

    /// Arms LibreOffice's one-shot `DontExpand` protection when the insertion cursor ends exactly
    /// at the citation boundary.
    ///
    /// `.uno:ResetAttributes` acts on the view cursor and shows up in undo history as
    /// "Clear Direct Formatting", so this helper first verifies that the live insertion cursor is
    /// collapsed and ends at the same position as `endRange`.
    public static void resetAttributesAtRangeEnd(XComponentContext context,
                                                 XTextDocument doc,
                                                 XTextCursor cursor,
                                                 XTextRange endRange) {
        try {
            XTextRangeCompare compare = UnoRuntime.queryInterface(XTextRangeCompare.class, cursor.getText());
            if ((compare == null) || !cursor.isCollapsed()) {
                return;
            }

            if (compare.compareRegionEnds(cursor, endRange) == 0) {
                execute(context, doc, RESET_ATTRIBUTES_COMMAND, EMPTY_DISPATCH_ARGUMENTS);
            }
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not compare insertion cursor with citation end", exception);
        }
    }

    /// Re-arms `DontExpand` for a recreated mark when the user's view cursor is still at that
    /// boundary after a numeric citation update.
    public static void resetAttributesAtRangeEnd(XComponentContext context,
                                                 XTextDocument doc,
                                                 XTextRange endRange) {
        try {
            UnoCursor.getViewCursor(doc)
                     .ifPresent(viewCursor -> resetAttributesAtRangeEnd(context, doc, viewCursor, endRange));
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not resolve view cursor for resetting attributes", exception);
        }
    }

    public static void execute(XComponentContext context,
                               XTextDocument doc,
                               String unoUrl,
                               PropertyValue[] arguments) {
        try {
            Object dispatchHelperObject = context.getServiceManager().createInstanceWithContext(DISPATCH_HELPER_SERVICE, context);
            XDispatchHelper dispatchHelper = UnoRuntime.queryInterface(XDispatchHelper.class, dispatchHelperObject);
            if (dispatchHelper == null) {
                LOGGER.debug("Could not query XDispatchHelper for UNO dispatch {}", unoUrl);
                return;
            }

            XDispatchProvider dispatchProvider = UnoRuntime.queryInterface(XDispatchProvider.class, doc.getCurrentController());
            if (dispatchProvider == null) {
                LOGGER.debug("Could not query XDispatchProvider for UNO dispatch {}", unoUrl);
                return;
            }

            dispatchHelper.executeDispatch(dispatchProvider, unoUrl, "", 0, arguments);
        } catch (com.sun.star.uno.Exception | com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not execute UNO dispatch {}", unoUrl, exception);
        }
    }
}
