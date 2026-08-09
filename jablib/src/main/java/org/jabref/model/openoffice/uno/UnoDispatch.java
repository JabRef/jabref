package org.jabref.model.openoffice.uno;

import java.util.Optional;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NullMarked
public class UnoDispatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoDispatch.class);
    private static final String RESET_ATTRIBUTES_COMMAND = ".uno:ResetAttributes";
    private static final PropertyValue[] EMPTY_DISPATCH_ARGUMENTS = new PropertyValue[0];

    private UnoDispatch() {
    }

    /// Prevents a reference mark ending at the current view cursor from expanding into the next typed character.
    ///
    /// `.uno:ResetAttributes` reaches LibreOffice's `DontExpandFormat` path, which has no direct UNO equivalent.
    /// The command acts on the view cursor and is visible in undo history as "Clear Direct Formatting", so this method
    /// first checks that the view cursor is collapsed exactly at the mark end. The flag is one-shot and does not protect
    /// against the caret returning to the same boundary later.
    public static void resetAttributesAtRangeEnd(XComponentContext context, XTextDocument document, XTextRange range) {
        try {
            if (!isViewCursorAtRangeEnd(document, range)) {
                return;
            }

            execute(context, document, RESET_ATTRIBUTES_COMMAND, EMPTY_DISPATCH_ARGUMENTS);
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not reset attributes at the end of the reference mark", exception);
        }
    }

    /// Executes a LibreOffice UI command against the document's controller.
    ///
    /// Dispatch commands are UI-level mitigations here. Failures are logged at debug level and never propagated.
    public static void execute(XComponentContext context,
                               XTextDocument document,
                               String unoUrl,
                               PropertyValue[] arguments) {
        try {
            getDispatchProvider(document).ifPresent(dispatchProvider ->
                    createDispatchHelper(context).ifPresent(dispatchHelper ->
                            executeDispatch(dispatchProvider, dispatchHelper, unoUrl, arguments)));
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not execute UNO dispatch command: {}", unoUrl, exception);
        }
    }

    /// Make sure cursor does not select texts
    private static boolean isViewCursorAtRangeEnd(XTextDocument document, XTextRange range) {
        return UnoCursor.getViewCursor(document)
                        .filter(XTextViewCursor::isCollapsed)
                        .map(viewCursor -> rangeEndsAtSamePosition(viewCursor, range))
                        .orElse(false);
    }

    private static boolean rangeEndsAtSamePosition(XTextRange first, XTextRange second) {
        try {
            return UnoCast.cast(XTextRangeCompare.class, second.getText())
                          .map(textRangeCompare -> textRangeCompare.compareRegionEnds(first, second) == 0)
                          .orElse(false);
        } catch (com.sun.star.uno.RuntimeException exception) {
            LOGGER.debug("Could not compare text ranges before resetting attributes", exception);
            return false;
        }
    }

    private static Optional<XDispatchProvider> getDispatchProvider(XTextDocument document) {
        return UnoTextDocument.getCurrentController(document)
                              .map(XController::getFrame)
                              .flatMap(frame -> UnoCast.cast(XDispatchProvider.class, frame));
    }

    private static Optional<XDispatchHelper> createDispatchHelper(XComponentContext context) {
        XMultiComponentFactory serviceManager = context.getServiceManager();
        if (serviceManager == null) {
            return Optional.empty();
        }

        try {
            Object dispatchHelper = serviceManager.createInstanceWithContext(
                    "com.sun.star.frame.DispatchHelper",
                    context);
            return UnoCast.cast(XDispatchHelper.class, dispatchHelper);
        } catch (com.sun.star.uno.Exception exception) {
            LOGGER.debug("Could not create UNO dispatch helper", exception);
            return Optional.empty();
        }
    }

    private static void executeDispatch(XDispatchProvider dispatchProvider,
                                        XDispatchHelper dispatchHelper,
                                        String unoUrl,
                                        PropertyValue[] arguments) {
        dispatchHelper.executeDispatch(dispatchProvider, unoUrl, "", 0, arguments);
    }
}
