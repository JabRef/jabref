package org.jabref.model.openoffice.uno;

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
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.jspecify.annotations.Nullable;

/// Change tracking and Redlines
public class UnoRedlines {

    private static final String REDLINE_TYPE_DELETE = "Delete";

    public static boolean getRecordChanges(XTextDocument doc)
            throws WrappedTargetException {

        // https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/Settings
        // "Properties of com.sun.star.text.TextDocument"

        XPropertySet propertySet = UnoCast.cast(XPropertySet.class, doc).get();

        try {
            return (boolean) propertySet.getPropertyValue("RecordChanges");
        } catch (UnknownPropertyException ex) {
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

    /// The ranges covered by not-yet-accepted deletions.
    public static List<XTextRange> getDeletedRanges(XTextDocument doc) {
        Optional<XRedlinesSupplier> supplier = getRedlinesSupplier(doc);
        if (supplier.isEmpty()) {
            return List.of();
        }
        XEnumerationAccess enumerationAccess = supplier.get().getRedlines();
        XEnumeration enumeration = enumerationAccess.createEnumeration();
        if (enumeration == null) {
            return List.of();
        }

        List<XTextRange> result = new ArrayList<>();
        while (enumeration.hasMoreElements()) {
            Object redline;
            try {
                redline = enumeration.nextElement();
            } catch (NoSuchElementException | WrappedTargetException ex) {
                break;
            }
            if (isDeleteRedline(redline)) {
                getRedlineRange(redline).ifPresent(result::add);
            }
        }
        return result;
    }

    public static int countDeletedRangesTouching(XTextDocument doc, List<XTextRange> ranges) {
        int count = 0;
        for (XTextRange deletedRange : getDeletedRanges(doc)) {
            for (XTextRange range : ranges) {
                if (!UnoTextRange.comparables(deletedRange, range)) {
                    continue;
                }
                boolean disjoint = UnoTextRange.compareStarts(deletedRange, range.getEnd()) > 0
                        || UnoTextRange.compareStarts(range, deletedRange.getEnd()) > 0;
                if (!disjoint) {
                    count++;
                    break;
                }
            }
        }
        return count;
    }

    private static boolean isDeleteRedline(Object redline) {
        Optional<XPropertySet> propertySet = UnoCast.cast(XPropertySet.class, redline);
        if (propertySet.isEmpty()) {
            return false;
        }
        try {
            return REDLINE_TYPE_DELETE.equals(propertySet.get().getPropertyValue("RedlineType"));
        } catch (UnknownPropertyException | WrappedTargetException ex) {
            return false;
        }
    }

    private static Optional<XTextRange> getRedlineRange(Object redline) {
        Optional<XTextRange> asRange = UnoCast.cast(XTextRange.class, redline);
        if (asRange.isPresent()) {
            return asRange;
        }
        Optional<XPropertySet> propertySet = UnoCast.cast(XPropertySet.class, redline);
        if (propertySet.isEmpty()) {
            return Optional.empty();
        }
        try {
            return UnoCast.cast(XTextRange.class, propertySet.get().getPropertyValue("RedlineStart"));
        } catch (UnknownPropertyException | WrappedTargetException ex) {
            return Optional.empty();
        }
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
