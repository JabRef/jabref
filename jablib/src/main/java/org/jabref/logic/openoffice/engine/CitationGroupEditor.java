package org.jabref.logic.openoffice.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.JabRefException;
import org.jabref.logic.openoffice.CitationIds;
import org.jabref.logic.openoffice.engine.storage.MarkWriter;
import org.jabref.logic.openoffice.frontend.DocumentCitations;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoTextRange;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.util.OOResult;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.XComponentContext;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Prepares structural citation changes without modifying Writer. The engine renders the proposed
/// groups before applying the plan and its final markers in one caller-owned [DocumentSession].
/// This editor neither locks controllers nor changes recording state.
@NullMarked
public final class CitationGroupEditor {

    private static final Logger LOGGER = LoggerFactory.getLogger(CitationGroupEditor.class);
    private static final OOText PLACEHOLDER = OOText.fromString("[?]");

    private final XTextDocument document;
    private final XComponentContext context;
    private final MarkWriter writer;

    public CitationGroupEditor(XTextDocument document, XComponentContext context, MarkWriter writer) {
        this.document = document;
        this.context = context;
        this.writer = writer;
    }

    /// Creates the stored group using the uid prepared before rendering and registers it in the
    /// action-local model. The caller supplies the rendered marker afterwards.
    public CitationGroup insert(DocumentCitations documentCitations, List<String> keys, List<Optional<OOText>> pageInfos,
                                CitationType type, XTextCursor position, String uniqueId,
                                boolean insertSpaceBefore, boolean insertSpaceAfter)
            throws CreationException, NoDocumentException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        CitationGroup group = documentCitations.markStore.createCitationGroup(document, context, keys, pageInfos, type, position,
                insertSpaceBefore, insertSpaceAfter, uniqueId);
        documentCitations.citationGroups.afterCreateCitationGroup(group);
        return group;
    }

    public PreparedEdit prepareMerge(DocumentCitations documentCitations, List<CitationGroup> groupsInDocumentOrder)
            throws NoDocumentException, WrappedTargetException, CreationException {
        List<Replacement> replacements = new ArrayList<>();
        for (List<CitationGroup> originals : scan(documentCitations)) {
            List<Citation> citations = originals.stream().flatMap(group -> group.citationsInStorageOrder.stream()).toList();
            CitationGroup merged = preparedGroup(citations);
            replacements.add(replacement(documentCitations, originals, List.of(merged)));
        }
        return plan(documentCitations, groupsInDocumentOrder, replacements);
    }

    public PreparedEdit prepareSeparate(DocumentCitations documentCitations, List<CitationGroup> groupsInDocumentOrder)
            throws NoDocumentException, WrappedTargetException, CreationException {
        List<Replacement> replacements = new ArrayList<>();
        for (CitationGroup original : groupsInDocumentOrder) {
            if (original.citationType != CitationType.AUTHORYEAR_PAR || original.numberOfCitations() <= 1) {
                continue;
            }
            List<CitationGroup> separated = new ArrayList<>();
            for (int index = 0; index < original.citationsInStorageOrder.size(); index++) {
                separated.add(preparedGroup(List.of(original.citationsInStorageOrder.get(index))));
            }
            replacements.add(replacement(documentCitations, List.of(original), separated));
        }
        return plan(documentCitations, groupsInDocumentOrder, replacements);
    }

    /// Applies only structural changes, without placeholders, rendering, or bibliography I/O.
    /// The returned order matches the prepared order, but newly stored groups have new id objects;
    /// their uid strings and canonical names are exactly those used during preparation.
    public List<CitationGroup> apply(DocumentCitations documentCitations, PreparedEdit plan)
            throws CreationException, NoDocumentException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        if (plan.documentCitations != documentCitations) {
            throw new CreationException("Citation edit plan belongs to another document snapshot");
        }
        for (Replacement replacement : plan.replacements) {
            for (CitationGroup original : replacement.originals()) {
                if (documentCitations.getMarkRange(document, original).isEmpty()) {
                    throw new NoDocumentException("Citation range disappeared after preparing group edits");
                }
            }
        }
        Map<CitationGroupId, CitationGroup> storedGroups = new LinkedHashMap<>();
        for (Replacement replacement : plan.replacements) {
            documentCitations.removeCitationGroups(replacement.originals(), document);
            XTextCursor position = replacement.range();
            position.setString("");
            for (int index = 0; index < replacement.proposed().size(); index++) {
                CitationGroup prepared = replacement.proposed().get(index);
                String uid = prepared.groupId.citationGroupIdAsString();
                CitationGroup stored = insert(documentCitations,
                        prepared.citationsInStorageOrder.stream().map(Citation::getCitationKey).toList(),
                        prepared.citationsInStorageOrder.stream().map(Citation::getPageInfo).toList(),
                        prepared.citationType, position, uid, false, index < replacement.proposed().size() - 1);
                writer.invalidate(uid);
                storedGroups.put(prepared.groupId, stored);
                position.collapseToEnd();
            }
        }
        List<CitationGroup> ordered = plan.groupsInDocumentOrder.stream()
                                                                .map(group -> storedGroups.getOrDefault(group.groupId, group)).toList();
        documentCitations.citationGroups.setGlobalOrder(new ArrayList<>(ordered.stream().map(group -> group.groupId).toList()));
        return ordered;
    }

    /// Compatibility entry point for structural-only callers already inside a write session.
    public OOResult<Boolean, JabRefException> merge(DocumentCitations documentCitations) {
        try {
            return applyPlaceholders(documentCitations, prepareMerge(documentCitations, currentOrder(documentCitations)));
        } catch (CreationException | IllegalTypeException | NoDocumentException | NotRemoveableException | PropertyVetoException | WrappedTargetException exception) {
            LOGGER.warn("Could not merge canonical citation groups", exception);
            return OOResult.error(new JabRefException(exception.toString(), exception));
        }
    }

    /// Compatibility entry point for structural-only callers already inside a write session.
    public OOResult<Boolean, JabRefException> separate(DocumentCitations documentCitations) {
        try {
            return applyPlaceholders(documentCitations, prepareSeparate(documentCitations, currentOrder(documentCitations)));
        } catch (CreationException | IllegalTypeException | NoDocumentException | NotRemoveableException | PropertyVetoException | WrappedTargetException | IllegalArgumentException exception) {
            LOGGER.warn("Could not separate canonical citation groups", exception);
            return OOResult.error(new JabRefException(exception.toString(), exception));
        }
    }

    private OOResult<Boolean, JabRefException> applyPlaceholders(DocumentCitations documentCitations, PreparedEdit plan)
            throws CreationException, NoDocumentException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        if (!plan.hasChanges()) {
            return OOResult.ok(false);
        }
        Set<String> created = new HashSet<>();
        plan.replacements.forEach(replacement -> replacement.proposed().forEach(group -> created.add(group.groupId.citationGroupIdAsString())));
        for (CitationGroup group : apply(documentCitations, plan)) {
            if (created.contains(group.groupId.citationGroupIdAsString())) {
                writer.write(documentCitations.markStore, group, PLACEHOLDER);
            }
        }
        return OOResult.ok(true);
    }

    private static List<CitationGroup> currentOrder(DocumentCitations documentCitations) {
        return documentCitations.citationGroups.hasGlobalOrder()
               ? documentCitations.citationGroups.getCitationGroupsInGlobalOrder()
               : documentCitations.citationGroups.getCitationGroupsUnordered();
    }

    private Replacement replacement(DocumentCitations documentCitations, List<CitationGroup> originals, List<CitationGroup> proposed)
            throws NoDocumentException, WrappedTargetException {
        XTextRange first = documentCitations.getMarkRange(document, originals.getFirst())
                                            .orElseThrow(() -> new NoDocumentException("Missing citation range during group-edit preparation"));
        XTextRange last = originals.size() == 1 ? first : documentCitations.getMarkRange(document, originals.getLast())
                                                                           .orElseThrow(() -> new NoDocumentException("Missing citation range during group-edit preparation"));
        XTextCursor range = first.getText().createTextCursorByRange(first.getStart());
        range.gotoRange(last.getEnd(), true);
        return new Replacement(List.copyOf(originals), List.copyOf(proposed), range);
    }

    private static CitationGroup preparedGroup(List<Citation> originals) throws CreationException {
        String uid = CitationIds.newId();
        List<Citation> citations = new ArrayList<>();
        for (Citation original : originals) {
            Citation copy = new Citation(original.citationKey);
            copy.setPageInfo(original.getPageInfo());
            citations.add(copy);
        }
        JabRefMarkData identity;
        try {
            identity = new JabRefMarkData(citations.stream().map(Citation::getCitationKey).toList(), uid, CitationType.AUTHORYEAR_PAR);
        } catch (MarkCodecException exception) {
            LOGGER.debug("Could not prepare canonical citation identity", exception);
            throw new CreationException("Invalid prepared citation identity: " + exception.reason());
        }
        return new CitationGroup(new CitationGroupId(uid), identity.citationType(), citations, Optional.of(MarkCodec.encode(identity)));
    }

    private static PreparedEdit plan(DocumentCitations documentCitations, List<CitationGroup> ordered, List<Replacement> replacements) throws CreationException {
        Set<CitationGroupId> current = new HashSet<>();
        documentCitations.citationGroups.getCitationGroupsUnordered().forEach(group -> current.add(group.groupId));
        Set<CitationGroupId> supplied = new HashSet<>();
        ordered.forEach(group -> supplied.add(group.groupId));
        if (supplied.size() != ordered.size() || !current.equals(supplied)) {
            throw new CreationException("Citation edit order does not match the document snapshot");
        }
        Map<CitationGroupId, Replacement> byOriginal = new LinkedHashMap<>();
        for (Replacement replacement : replacements) {
            for (CitationGroup original : replacement.originals()) {
                byOriginal.put(original.groupId, replacement);
            }
        }
        if (!current.containsAll(byOriginal.keySet())) {
            throw new CreationException("Citation replacement does not belong to the document snapshot");
        }
        Set<CitationGroupId> emitted = new HashSet<>();
        List<CitationGroup> proposed = new ArrayList<>();
        for (CitationGroup original : ordered) {
            Optional<Replacement> replacement = Optional.ofNullable(byOriginal.get(original.groupId));
            if (replacement.isEmpty()) {
                proposed.add(original);
            } else if (emitted.add(replacement.orElseThrow().originals().getFirst().groupId)) {
                proposed.addAll(replacement.orElseThrow().proposed());
            }
        }
        Set<String> uniqueIds = new HashSet<>();
        for (CitationGroup group : proposed) {
            if (!uniqueIds.add(group.groupId.citationGroupIdAsString())) {
                throw new CreationException("Prepared citation groups have duplicate uids");
            }
        }
        return new PreparedEdit(documentCitations, proposed, replacements);
    }

    private List<List<CitationGroup>> scan(DocumentCitations documentCitations) throws NoDocumentException, WrappedTargetException {
        List<List<CitationGroup>> result = new ArrayList<>();
        List<CitationGroup> current = new ArrayList<>();
        Optional<XTextRange> previous = Optional.empty();
        for (CitationGroup group : documentCitations.getCitationGroupsSortedWithinPartitions(document, false)) {
            XTextRange range = documentCitations.getMarkRange(document, group)
                                                .orElseThrow(() -> new NoDocumentException("Missing citation range during merge scan"));
            if (group.citationType != CitationType.AUTHORYEAR_PAR || previous.filter(prior -> separatedOnlyByInlineBlanks(prior, range)).isEmpty()) {
                if (current.size() > 1) {
                    result.add(List.copyOf(current));
                }
                current.clear();
            }
            if (group.citationType == CitationType.AUTHORYEAR_PAR) {
                current.add(group);
                previous = Optional.of(range);
            } else {
                previous = Optional.empty();
            }
        }
        if (current.size() > 1) {
            result.add(List.copyOf(current));
        }
        return result;
    }

    private static boolean separatedOnlyByInlineBlanks(XTextRange previous, XTextRange next) {
        if (!UnoTextRange.comparables(previous, next) || UnoTextRange.compareStarts(previous.getEnd(), next.getStart()) > 0) {
            return false;
        }
        XTextCursor between = previous.getText().createTextCursorByRange(previous.getEnd());
        between.gotoRange(next.getStart(), true);
        String text = between.getString();
        return text.isBlank() && !text.contains("\n") && !text.contains("\r");
    }

    @NullMarked
    public static final class PreparedEdit {
        private final DocumentCitations documentCitations;
        private final List<CitationGroup> groupsInDocumentOrder;
        private final List<Replacement> replacements;

        private PreparedEdit(DocumentCitations documentCitations, List<CitationGroup> groupsInDocumentOrder, List<Replacement> replacements) {
            this.documentCitations = documentCitations;
            this.groupsInDocumentOrder = List.copyOf(groupsInDocumentOrder);
            this.replacements = List.copyOf(replacements);
        }

        public List<CitationGroup> groupsInDocumentOrder() {
            return groupsInDocumentOrder;
        }

        public boolean hasChanges() {
            return !replacements.isEmpty();
        }
    }

    @NullMarked
    private record Replacement(List<CitationGroup> originals, List<CitationGroup> proposed, XTextCursor range) {
    }
}
