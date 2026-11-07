package org.jabref.logic.openoffice.uno;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.document.XRedlinesSupplier;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.uno.UnoRuntime;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Change tracking and Redlines
public class UnoRedlines {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoRedlines.class);
    private static final String REDLINE_TYPE_DELETE = "Delete";

    public static boolean getRecordChanges(XTextDocument doc)
            throws WrappedTargetException {

        // https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Settings
        // "Properties of com.sun.star.text.TextDocument"

        XPropertySet propertySet = UnoCast.cast(XPropertySet.class, doc).get();

        try {
            return (boolean) propertySet.getPropertyValue("RecordChanges");
        } catch (UnknownPropertyException _) {
            throw new IllegalStateException("Caught UnknownPropertyException on 'RecordChanges'");
        }
    }

    public static void setRecordChanges(XTextDocument doc, boolean recordChanges)
            throws WrappedTargetException {

        XPropertySet propertySet = UnoCast.cast(XPropertySet.class, doc).get();

        try {
            propertySet.setPropertyValue("RecordChanges", recordChanges);
        } catch (UnknownPropertyException | PropertyVetoException
                 | com.sun.star.lang.IllegalArgumentException ex) {
            throw new IllegalStateException("Could not set 'RecordChanges'", ex);
        }
    }

    private static Optional<XRedlinesSupplier> getRedlinesSupplier(XTextDocument doc) {
        return UnoCast.cast(XRedlinesSupplier.class, doc);
    }

    /// Document ranges covered by unaccepted deletions, regardless of the current recording setting.
    /// A redline's own `XTextRange` may belong to detached deleted text; only its `RedlineStart`
    /// and `RedlineEnd` properties identify the real document anchors, including hidden changes.
    ///
    /// @throws NoDocumentException if pending changes cannot be inspected safely
    public static List<XTextRange> getDeletedRanges(XTextDocument doc) throws NoDocumentException {
        Optional<XRedlinesSupplier> supplier = getRedlinesSupplier(doc);
        if (supplier.isEmpty()) {
            return List.of();
        }
        try {
            XEnumerationAccess access = Optional.ofNullable(supplier.orElseThrow().getRedlines())
                                                .orElseThrow(() -> inspectionFailure("Missing Writer redline collection"));
            XEnumeration changes = Optional.ofNullable(access.createEnumeration())
                                           .orElseThrow(() -> inspectionFailure("Cannot enumerate pending Writer changes"));
            return collectDeletedRanges(changes);
        } catch (com.sun.star.uno.RuntimeException exception) {
            throw inspectionFailure("Cannot inspect pending Writer changes", exception);
        }
    }

    private static List<XTextRange> collectDeletedRanges(XEnumeration changes) throws NoDocumentException {
        List<XTextRange> result = new ArrayList<>();
        while (changes.hasMoreElements()) {
            Object change;
            try {
                change = changes.nextElement();
            } catch (NoSuchElementException | WrappedTargetException exception) {
                throw inspectionFailure("Cannot read pending Writer change", exception);
            }
            XPropertySet properties = UnoCast.cast(XPropertySet.class, change)
                                             .orElseThrow(() -> inspectionFailure("Missing Writer redline properties"));
            if (!REDLINE_TYPE_DELETE.equals(redlineProperty(properties, "RedlineType"))) {
                continue;
            }
            XTextRange start = UnoCast.cast(XTextRange.class, redlineProperty(properties, "RedlineStart"))
                                      .orElseThrow(() -> inspectionFailure("Missing Writer deletion start anchor"));
            XTextRange end = UnoCast.cast(XTextRange.class, redlineProperty(properties, "RedlineEnd"))
                                    .orElseThrow(() -> inspectionFailure("Missing Writer deletion end anchor"));
            if (!UnoRuntime.areSame(start.getText(), end.getText())) {
                throw inspectionFailure("Writer deletion anchors belong to different text containers");
            }
            XTextCursor range = start.getText().createTextCursorByRange(start);
            range.gotoRange(end, true);
            result.add(range);
        }
        return result;
    }

    public static int countDeletedRangesTouching(XTextDocument doc, List<XTextRange> ranges) throws NoDocumentException {
        int count = 0;
        for (XTextRange deletedRange : getDeletedRanges(doc)) {
            for (XTextRange range : ranges) {
                if (!UnoRuntime.areSame(deletedRange.getText(), range.getText())) {
                    continue;
                }
                XTextRangeCompare compare = UnoCast.cast(XTextRangeCompare.class, deletedRange.getText())
                                                   .orElseThrow(() -> inspectionFailure("Cannot compare Writer deletion anchors"));
                boolean disjoint;
                try {
                    disjoint = UnoTextRange.compareStartsUnsafe(compare, deletedRange, range.getEnd()) > 0
                            || UnoTextRange.compareStartsUnsafe(compare, range, deletedRange.getEnd()) > 0;
                } catch (com.sun.star.uno.RuntimeException exception) {
                    throw inspectionFailure("Cannot compare Writer deletion anchors", exception);
                }
                if (!disjoint) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static Object redlineProperty(XPropertySet properties, String name) throws NoDocumentException {
        try {
            return properties.getPropertyValue(name);
        } catch (UnknownPropertyException | WrappedTargetException exception) {
            throw inspectionFailure("Cannot read Writer redline property " + name, exception);
        }
    }

    private static NoDocumentException inspectionFailure(String message) {
        LOGGER.warn(message);
        return new NoDocumentException(message);
    }

    private static NoDocumentException inspectionFailure(String message, Throwable cause) {
        LOGGER.warn(message, cause);
        NoDocumentException failure = new NoDocumentException(message);
        failure.initCause(cause);
        return failure;
    }

    /// Run `action` with change recording switched off, then restore the previous setting.
    ///
    /// Refreshing a citation marker means deleting its old text and writing new text. While
    /// recording is on, the delete only *marks* the old text as deleted, so the old and the new
    /// marker end up side by side and the citation appears twice. Suspending recording for the
    /// duration of our own edits is what keeps that from happening.
    ///
    /// JabRef's own rewrite operations are not recorded as tracked changes.
    /// TODO: Add this warning to user documentation
    public static void withRecordChangesSuspended(XTextDocument doc, RedlineAction action)
            throws WrappedTargetException, TrackChangesRestoreException {

        boolean wasRecording = getRecordChanges(doc);
        if (wasRecording) {
            setRecordChanges(doc, false);
        }

        try {
            action.run();
        } catch (WrappedTargetException | RuntimeException actionFailure) {
            restoreTrackChanges(doc, wasRecording, actionFailure);
            throw actionFailure;
        }

        restoreTrackChanges(doc, wasRecording, null);
    }

    private static void restoreTrackChanges(XTextDocument doc, boolean wasRecording, @Nullable Throwable actionFailure)
            throws WrappedTargetException, TrackChangesRestoreException {
        if (!wasRecording) {
            return;
        }

        try {
            setRecordChanges(doc, true);
        } catch (WrappedTargetException | RuntimeException restoreFailure) {
            if (actionFailure != null) {
                actionFailure.addSuppressed(restoreFailure);
                return;
            }
            throw new TrackChangesRestoreException(restoreFailure);
        }
    }

    public static final class TrackChangesRestoreException extends Exception {
        public TrackChangesRestoreException(Throwable cause) {
            super("Could not restore Track Changes", cause);
        }
    }

    @FunctionalInterface
    public interface RedlineAction {
        void run() throws WrappedTargetException;
    }
}
