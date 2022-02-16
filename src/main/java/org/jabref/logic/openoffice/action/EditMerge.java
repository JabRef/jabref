package org.jabref.logic.openoffice.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.logic.openoffice.frontend.OOFrontend;
import org.jabref.logic.openoffice.frontend.UpdateCitationMarkers;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoScreenRefresh;
import org.jabref.model.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.util.OOListUtil;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditMerge {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditMerge.class);

    private EditMerge() {
        // hide constructor
    }

    /*
     * @return true if modified document
     */
    public static boolean mergeCitationGroups(XTextDocument doc, OOFrontend frontend, OOBibStyle style)
        throws
        CreationException,
        IllegalArgumentException,
        IllegalTypeException,
        NoDocumentException,
        NotRemoveableException,
        PropertyVetoException,
        WrappedTargetException {

        boolean madeModifications;

        try {
            UnoScreenRefresh.lockControllers(doc);

            List<JoinableGroupData> joinableGroups = EditMerge.scan(doc, frontend);

            for (JoinableGroupData joinableGroupData : joinableGroups) {

                List<CitationGroup> groups = joinableGroupData.group;

                List<Citation> newCitations = (groups.stream()
                                               .flatMap(group -> group.citationsInStorageOrder.stream())
                                               .collect(Collectors.toList()));

                CitationType citationType = groups.get(0).citationType;
                List<Optional<OOText>> pageInfos = frontend.backend.combinePageInfos(groups);

                frontend.removeCitationGroups(groups, doc);
                XTextCursor textCursor = joinableGroupData.groupCursor;
                textCursor.setString(""); // Also remove the spaces between.

                List<String> citationKeys = OOListUtil.map(newCitations, Citation::getCitationKey);

                /* insertSpaceAfter: no, it is already there (or could be) */
                boolean insertSpaceAfter = false;
                UpdateCitationMarkers.createAndFillCitationGroup(frontend,
                                                                 doc,
                                                                 citationKeys,
                                                                 pageInfos,
                                                                 citationType,
                                                                 OOText.fromString("tmp"),
                                                                 textCursor,
                                                                 style,
                                                                 insertSpaceAfter);
            }

            madeModifications = !joinableGroups.isEmpty();

        } finally {
            UnoScreenRefresh.unlockControllers(doc);
        }

        return madeModifications;
    }

    private static class JoinableGroupData {
        /** A list of consecutive citation groups only separated by spaces. */
        List<CitationGroup> group;

        /** A cursor covering the XTextRange of each entry in group (and the spaces between them) */
        XTextCursor groupCursor;

        JoinableGroupData(List<CitationGroup> group, XTextCursor groupCursor) {
            this.group = group;
            this.groupCursor = groupCursor;
        }
    }

    private static class ScanState {

        // Citation groups in the current group
        List<CitationGroup> currentGroup;

        // A cursor that covers the Citation groups in currentGroup, including the space between
        // them.
        // null if currentGroup.isEmpty()
        XTextCursor currentGroupCursor;

        // A cursor starting at the end of the last CitationGroup in
        // currentGroup. null if currentGroup.isEmpty()
        XTextCursor cursorBetween;

        // The last element of currentGroup.
        // null if currentGroup.isEmpty()
        CitationGroup prev;

        // The XTextRange for prev.
        // null if currentGroup.isEmpty()
        XTextRange prevRange;

        ScanState() {
            reset();
        }

        void reset() {
            currentGroup = new ArrayList<>();
            currentGroupCursor = null;
            cursorBetween = null;
            prev = null;
            prevRange = null;
        }
    }

    /**
     * Decide if group could be added to state.currentGroup
     *
     * @param group The CitationGroup to test
     * @param currentRange The XTextRange corresponding to group.
     *
     * @return false if cannot add, true if can.  If returned true, then state.cursorBetween and
     *  state.currentGroupCursor are expanded to end at the start of currentRange.
     */
    private static boolean checkAddToGroup(ScanState state, CitationGroup group, XTextRange currentRange) {

        if (state.currentGroup.isEmpty()) {
            return false;
        }

        Objects.requireNonNull(state.currentGroupCursor);
        Objects.requireNonNull(state.cursorBetween);
        Objects.requireNonNull(state.prev);
        Objects.requireNonNull(state.prevRange);

        // Only combine (Author 2000) type citations
        if (group.citationType != CitationType.AUTHORYEAR_PAR) {
            return false;
        }

        if (state.prev != null) {

            // Even if we combine AUTHORYEAR_INTEXT citations, we would not mix them with AUTHORYEAR_PAR
            if (group.citationType != state.prev.citationType) {
                return false;
            }

            if (!UnoTextRange.comparables(state.prevRange, currentRange)) {
                return false;
            }

            // Sanity check: the current range should start later than the previous.
            int textOrder = UnoTextRange.compareStarts(state.prevRange, currentRange);
            if (textOrder != (-1)) {
                String msg =
                    String.format("MergeCitationGroups:"
                                  + " \"%s\" supposed to be followed by \"%s\","
                                  + " but %s",
                                  state.prevRange.getString(),
                                  currentRange.getString(),
                                  ((textOrder == 0)
                                   ? "they start at the same position"
                                   : "the start of the latter precedes the start of the first"));
                LOGGER.warn(msg);
                return false;
            }
        }

        if (state.cursorBetween == null) {
            return false;
        }

        Objects.requireNonNull(state.cursorBetween);
        Objects.requireNonNull(state.currentGroupCursor);

        // assume: currentGroupCursor.getEnd() == cursorBetween.getEnd()
        if (UnoTextRange.compareEnds(state.cursorBetween, state.currentGroupCursor) != 0) {
            LOGGER.warn("MergeCitationGroups: cursorBetween.end != currentGroupCursor.end");
            throw new IllegalStateException("MergeCitationGroups failed");
        }

        /*
         * Try to expand state.currentGroupCursor and state.cursorBetween by going right to reach
         * rangeStart.
         */
        XTextRange rangeStart = currentRange.getStart();
        boolean couldExpand = true;
        XTextCursor thisCharCursor =
            (currentRange.getText().createTextCursorByRange(state.cursorBetween.getEnd()));

        while (couldExpand && (UnoTextRange.compareEnds(state.cursorBetween, rangeStart) < 0)) {
            //
            // Check that we only walk through inline whitespace.
            //
            couldExpand = thisCharCursor.goRight((short) 1, true);
            String thisChar = thisCharCursor.getString();
            thisCharCursor.collapseToEnd();
            if (thisChar.isEmpty() || "\n".equals(thisChar) || !thisChar.trim().isEmpty()) {
                couldExpand = false;
                if (!thisChar.isEmpty()) {
                    thisCharCursor.goLeft((short) 1, false);
                }
                break;
            }
            state.cursorBetween.goRight((short) 1, true);
            state.currentGroupCursor.goRight((short) 1, true);

            // These two should move in sync:
            if (UnoTextRange.compareEnds(state.cursorBetween, state.currentGroupCursor) != 0) {
                LOGGER.warn("MergeCitationGroups: cursorBetween.end != currentGroupCursor.end (during expand)");
                throw new IllegalStateException("MergeCitationGroups failed");
            }
        }

        return couldExpand;
    }

    /**
     * Add group to state.currentGroup
     * Set state.cursorBetween to start at currentRange.getEnd()
     * Expand state.currentGroupCursor to also cover currentRange
     * Set state.prev to group, state.prevRange to currentRange
     */
    private static void addToCurrentGroup(ScanState state, CitationGroup group, XTextRange currentRange) {
        final boolean isNewGroup = state.currentGroup.isEmpty();
        if (!isNewGroup) {
            Objects.requireNonNull(state.currentGroupCursor);
            Objects.requireNonNull(state.cursorBetween);
            Objects.requireNonNull(state.prev);
            Objects.requireNonNull(state.prevRange);
        }

        // Add the current entry to a group.
        state.currentGroup.add(group);

        // Set up cursorBetween to start at currentRange.getEnd()
        XTextRange rangeEnd = currentRange.getEnd();
        state.cursorBetween = currentRange.getText().createTextCursorByRange(rangeEnd);

        // If new group, create currentGroupCursor
        if (isNewGroup) {
            state.currentGroupCursor = (currentRange.getText()
                                        .createTextCursorByRange(currentRange.getStart()));
        }

        // include currentRange in currentGroupCursor
        state.currentGroupCursor.goRight((short) (currentRange.getString().length()), true);

        if (UnoTextRange.compareEnds(state.cursorBetween, state.currentGroupCursor) != 0) {
            LOGGER.warn("MergeCitationGroups: cursorBetween.end != currentGroupCursor.end");
            throw new IllegalStateException("MergeCitationGroups failed");
        }

        /* Store data about last entry in currentGroup */
        state.prev = group;
        state.prevRange = currentRange;
    }

    /**
     *  Scan the document for joinable groups. Return those found.
     */
    private static List<JoinableGroupData> scan(XTextDocument doc, OOFrontend frontend)
        throws
        NoDocumentException,
        WrappedTargetException {
        List<JoinableGroupData> result = new ArrayList<>();

        List<CitationGroup> groups = frontend.getCitationGroupsSortedWithinPartitions(doc, false /* mapFootnotesToFootnoteMarks */);
        if (groups.isEmpty()) {
            return result;
        }

        ScanState state = new ScanState();

        for (CitationGroup group : groups) {

            XTextRange currentRange = (frontend.getMarkRange(doc, group)
                                       .orElseThrow(IllegalStateException::new));

            /*
             * Decide if we add group to the group. False when the group is empty.
             */
            boolean addToGroup = checkAddToGroup(state, group, currentRange);

            /*
             * Even if we do not add it to an existing group, we might use it to start a new group.
             *
             * Can it start a new group?
             */
            boolean canStartGroup = (group.citationType == CitationType.AUTHORYEAR_PAR);

            if (!addToGroup) {
                // close currentGroup
                if (state.currentGroup.size() > 1) {
                    result.add(new JoinableGroupData(state.currentGroup, state.currentGroupCursor));
                }
                // Start a new, empty group
                state.reset();
            }

            if (addToGroup || canStartGroup) {
                addToCurrentGroup(state, group, currentRange);
            }
        }

        // close currentGroup
        if (state.currentGroup.size() > 1) {
            result.add(new JoinableGroupData(state.currentGroup, state.currentGroupCursor));
        }
        return result;
    }

}
