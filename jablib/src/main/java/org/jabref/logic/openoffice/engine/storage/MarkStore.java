package org.jabref.logic.openoffice.engine.storage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.openoffice.CitationIds;
import org.jabref.logic.openoffice.backend.WriterReferenceMark;
import org.jabref.logic.openoffice.backend.WriterReferenceMark.BoundaryPolicy;
import org.jabref.logic.openoffice.backend.WriterReferenceMarks;
import org.jabref.logic.openoffice.backend.WriterTextContext;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.engine.MarkCodec;
import org.jabref.logic.openoffice.engine.MarkCodecException;
import org.jabref.logic.openoffice.uno.CreationException;
import org.jabref.logic.openoffice.uno.NoDocumentException;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.PageInfo;

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

/// Stores canonical citation identity and per-citation page info, never derived numbers or rendered text.
/// A fresh read replaces the range index; rendering must retain the citations' storage order.
@NullMarked
public final class MarkStore {

    public static final String PAGE_INFO_PREFIX = "JabRef_page_info:";
    private static final Logger LOGGER = LoggerFactory.getLogger(MarkStore.class);

    private final WriterReferenceMarks ranges = new WriterReferenceMarks();
    private final Map<CitationGroupId, StoredMark> marks = new LinkedHashMap<>();

    public CitationGroups readAll(XTextDocument document) throws NoDocumentException, WrappedTargetException {
        marks.clear();
        Map<CitationGroupId, CitationGroup> groups = new LinkedHashMap<>();
        Set<String> uniqueIds = new HashSet<>();
        for (String name : getJabRefReferenceMarkNames(document)) {
            CitationGroup group = readCitationGroupFromDocumentOrThrow(document, name);
            if (!uniqueIds.add(group.groupId.citationGroupIdAsString())) {
                LOGGER.warn("Ignoring canonical reference mark with duplicate uid: {}", name);
                marks.remove(group.groupId);
                continue;
            }
            groups.put(group.groupId, group);
        }
        return new CitationGroups(groups);
    }

    public List<String> getJabRefReferenceMarkNames(XTextDocument document) throws NoDocumentException {
        return ranges.names(document).stream().filter(MarkCodec::isCanonicalMarkName).toList();
    }

    public CitationGroup readCitationGroupFromDocumentOrThrow(XTextDocument document, String name)
            throws NoDocumentException, WrappedTargetException {
        JabRefMarkData data;
        try {
            data = MarkCodec.decode(name);
        } catch (MarkCodecException exception) {
            LOGGER.debug("Not a canonical JabRef reference mark: {}", name, exception);
            throw new NoDocumentException("Not a canonical JabRef reference mark: " + name);
        }
        WriterReferenceMark range = ranges.find(document, name, BoundaryPolicy.PERSISTENT)
                                          .orElseThrow(() -> new NoDocumentException("Missing citation range: " + name));
        List<Citation> citations = new ArrayList<>(data.citationKeys().size());
        for (int index = 0; index < data.citationKeys().size(); index++) {
            Citation citation = new Citation(data.citationKeys().get(index));
            citation.setPageInfo(PageInfo.normalizePageInfo(UnoUserDefinedProperty.getStringValue(document, pageInfoProperty(data.uniqueId(), index)).map(OOText::fromString)));
            citations.add(citation);
        }
        CitationGroupId id = new CitationGroupId(data.uniqueId());
        CitationGroup group = new CitationGroup(id, data.citationType(), citations, Optional.of(name));
        marks.put(id, new StoredMark(data, range));
        return group;
    }

    public CitationGroup createCitationGroup(XTextDocument document, XComponentContext context,
                                             List<String> keys, List<Optional<OOText>> pageInfos,
                                             CitationType type, XTextCursor position,
                                             boolean insertSpaceBefore, boolean insertSpaceAfter)
            throws CreationException, NoDocumentException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        return createCitationGroup(document, context, keys, pageInfos, type, position,
                insertSpaceBefore, insertSpaceAfter, CitationIds.newId());
    }

    public CitationGroup createCitationGroup(XTextDocument document, XComponentContext context,
                                             List<String> keys, List<Optional<OOText>> pageInfos,
                                             CitationType type, XTextCursor position,
                                             boolean insertSpaceBefore, boolean insertSpaceAfter, String uniqueId)
            throws CreationException, NoDocumentException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        if (keys.size() != pageInfos.size()) {
            throw new CreationException("A page-info slot is required for every citation key");
        }
        JabRefMarkData data;
        try {
            data = new JabRefMarkData(keys, uniqueId, type);
        } catch (MarkCodecException exception) {
            LOGGER.debug("Cannot encode citation identity", exception);
            throw new CreationException("Invalid canonical citation identity: " + exception.reason());
        }
        String name = MarkCodec.encode(data);
        WriterReferenceMark range = ranges.create(document, context, name, position,
                insertSpaceBefore && !hasAdjacentSpace(position, false),
                insertSpaceAfter && !hasAdjacentSpace(position, true), BoundaryPolicy.PERSISTENT);
        List<Citation> citations = new ArrayList<>(keys.size());
        for (int index = 0; index < keys.size(); index++) {
            Citation citation = new Citation(keys.get(index));
            citation.setPageInfo(PageInfo.normalizePageInfo(pageInfos.get(index)));
            writePageInfo(document, data.uniqueId(), index, citation.getPageInfo());
            citations.add(citation);
        }
        CitationGroupId id = new CitationGroupId(data.uniqueId());
        CitationGroup group = new CitationGroup(id, type, citations, Optional.of(name));
        marks.put(id, new StoredMark(data, range));
        return group;
    }

    private static boolean hasAdjacentSpace(XTextCursor position, boolean after) {
        XTextCursor adjacent = position.getText().createTextCursorByRange(position.getEnd());
        boolean moved = after ? adjacent.goRight((short) 1, true) : adjacent.goLeft((short) 1, true);
        return moved && !adjacent.getString().isEmpty() && adjacent.getString().isBlank();
    }

    public Optional<JabRefMarkData> getMarkData(CitationGroup group) {
        return Optional.ofNullable(marks.get(group.groupId)).map(StoredMark::data);
    }

    public WriterReferenceMark referenceMark(CitationGroup group) throws NoDocumentException {
        return stored(group).range();
    }

    private StoredMark stored(CitationGroup group) throws NoDocumentException {
        return Optional.ofNullable(marks.get(group.groupId))
                       .orElseThrow(() -> new NoDocumentException("Unknown citation group: " + group.groupId.citationGroupIdAsString()));
    }

    public void removeCitationGroup(CitationGroup group, XTextDocument document)
            throws NoDocumentException, WrappedTargetException, NotRemoveableException {
        StoredMark mark = stored(group);
        mark.range().removeFromDocument(document);
        for (int index = 0; index < mark.data().citationKeys().size(); index++) {
            UndoableDocumentProperties.remove(document, pageInfoProperty(mark.data().uniqueId(), index));
        }
        marks.remove(group.groupId);
    }

    public Optional<XTextRange> getMarkRange(CitationGroup group, XTextDocument document)
            throws NoDocumentException, WrappedTargetException {
        return stored(group).range().getMarkRange(document);
    }

    public Optional<XTextCursor> getRawCursorForCitationGroup(CitationGroup group, XTextDocument document)
            throws NoDocumentException, WrappedTargetException {
        return stored(group).range().getRawCursor(document);
    }

    public XTextCursor getFillCursorForCitationGroup(CitationGroup group, XTextDocument document)
            throws NoDocumentException, WrappedTargetException, CreationException {
        return stored(group).range().getFillCursor(document);
    }

    public void cleanFillCursorForCitationGroup(CitationGroup group, XTextDocument document)
            throws NoDocumentException, WrappedTargetException {
        stored(group).range().cleanFillCursor(document);
    }

    public void savePageInfos(XTextDocument document, CitationGroup group)
            throws NoDocumentException, WrappedTargetException, NotRemoveableException, PropertyVetoException, IllegalTypeException {
        String uid = stored(group).data().uniqueId();
        for (int index = 0; index < group.citationsInStorageOrder.size(); index++) {
            writePageInfo(document, uid, index, group.citationsInStorageOrder.get(index).getPageInfo());
        }
    }

    public static String pageInfoProperty(String uid, int storageIndex) {
        return PAGE_INFO_PREFIX + uid + ":" + storageIndex;
    }

    private static void writePageInfo(XTextDocument document, String uid, int index, Optional<OOText> pageInfo)
            throws NotRemoveableException, WrappedTargetException, PropertyVetoException, IllegalTypeException {
        UndoableDocumentProperties.set(document, pageInfoProperty(uid, index), pageInfo.map(text -> text.toString()));
    }

    /// Call after a successful update, not while reading or before rendering can fail.
    public void removeOrphanPageInfos(XTextDocument document) throws NoDocumentException, NotRemoveableException, WrappedTargetException {
        Set<String> liveProperties = new HashSet<>();
        for (String name : getJabRefReferenceMarkNames(document)) {
            try {
                JabRefMarkData data = MarkCodec.decode(name);
                for (int index = 0; index < data.citationKeys().size(); index++) {
                    liveProperties.add(pageInfoProperty(data.uniqueId(), index));
                }
            } catch (MarkCodecException exception) {
                LOGGER.debug("Ignoring invalid mark during page-info cleanup: {}", name, exception);
            }
        }
        for (String property : UnoUserDefinedProperty.getListOfNames(document)) {
            if (property.startsWith(PAGE_INFO_PREFIX) && !liveProperties.contains(property)) {
                UndoableDocumentProperties.remove(document, property);
            }
        }
    }

    /// The legacy dialog edits the last storage slot only; other citations' page infos remain intact.
    public List<CitationEntry> getCitationEntries(XTextDocument document, CitationGroups groups)
            throws NoDocumentException, WrappedTargetException {
        List<CitationEntry> result = new ArrayList<>();
        for (CitationGroup group : groups.getCitationGroupsUnordered()) {
            XTextCursor cursor = getRawCursorForCitationGroup(group, document)
                    .orElseThrow(() -> new NoDocumentException("Missing citation range"));
            result.add(new CitationEntry(stored(group).range().getRangeName(),
                    WriterTextContext.getCursorStringWithContext(cursor, 30, 30, true),
                    group.citationsInStorageOrder.getLast().getPageInfo().map(pageInfo -> pageInfo.toString())));
        }
        return result;
    }

    public void applyCitationEntries(XTextDocument document, List<CitationEntry> entries)
            throws NotRemoveableException, WrappedTargetException, PropertyVetoException, IllegalTypeException {
        for (CitationEntry entry : entries) {
            Optional<StoredMark> stored = marks.values().stream().filter(mark -> mark.range().getRangeName().equals(entry.getRefMarkName())).findFirst();
            if (stored.isPresent()) {
                JabRefMarkData data = stored.orElseThrow().data();
                writePageInfo(document, data.uniqueId(), data.citationKeys().size() - 1,
                        PageInfo.normalizePageInfo(entry.getPageInfo().map(OOText::fromString)));
            }
        }
    }

    @NullMarked
    private record StoredMark(JabRefMarkData data, WriterReferenceMark range) {
    }
}
