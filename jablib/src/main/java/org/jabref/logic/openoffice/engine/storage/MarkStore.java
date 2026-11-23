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
import org.jabref.logic.openoffice.engine.JabRefMarkCodec;
import org.jabref.logic.openoffice.engine.JabRefMarkCodecException;
import org.jabref.logic.openoffice.engine.JabRefMarkData;
import org.jabref.logic.openoffice.uno.UnoUserDefinedProperty;
import org.jabref.logic.openoffice.uno.WriterDocumentAccessException;
import org.jabref.logic.openoffice.uno.WriterOperationException;
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

    public CitationGroups readAll(XTextDocument document) throws WriterDocumentAccessException, WrappedTargetException {
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

    public List<String> getJabRefReferenceMarkNames(XTextDocument document) throws WriterDocumentAccessException {
        return ranges.names(document).stream().filter(JabRefMarkCodec::isCanonicalMarkName).toList();
    }

    public CitationGroup readCitationGroupFromDocumentOrThrow(XTextDocument document, String name)
            throws WriterDocumentAccessException, WrappedTargetException {
        JabRefMarkData data;
        try {
            data = JabRefMarkCodec.decode(name);
        } catch (JabRefMarkCodecException exception) {
            LOGGER.debug("Not a canonical JabRef reference mark: {}", name, exception);
            throw new WriterDocumentAccessException("Not a canonical JabRef reference mark: " + name);
        }
        WriterReferenceMark range = ranges.find(document, name, BoundaryPolicy.PERSISTENT)
                                          .orElseThrow(() -> new WriterDocumentAccessException("Missing citation range: " + name));
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
            throws WriterOperationException, WriterDocumentAccessException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        return createCitationGroup(document, context, keys, pageInfos, type, position,
                insertSpaceBefore, insertSpaceAfter, CitationIds.newId());
    }

    public CitationGroup createCitationGroup(XTextDocument document, XComponentContext context,
                                             List<String> keys, List<Optional<OOText>> pageInfos,
                                             CitationType type, XTextCursor position,
                                             boolean insertSpaceBefore, boolean insertSpaceAfter, String uniqueId)
            throws WriterOperationException, WriterDocumentAccessException, WrappedTargetException, NotRemoveableException,
            PropertyVetoException, IllegalTypeException {
        if (keys.size() != pageInfos.size()) {
            throw new WriterOperationException("A page-info slot is required for every citation key");
        }
        JabRefMarkData data;
        try {
            data = new JabRefMarkData(keys, uniqueId, type);
        } catch (JabRefMarkCodecException exception) {
            LOGGER.debug("Cannot encode citation identity", exception);
            throw new WriterOperationException("Invalid canonical citation identity: " + exception.reason());
        }
        String name = JabRefMarkCodec.encode(data);
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

    public WriterReferenceMark referenceMark(CitationGroup group) throws WriterDocumentAccessException {
        return stored(group).range();
    }

    private StoredMark stored(CitationGroup group) throws WriterDocumentAccessException {
        return Optional.ofNullable(marks.get(group.groupId))
                       .orElseThrow(() -> new WriterDocumentAccessException("Unknown citation group: " + group.groupId.citationGroupIdAsString()));
    }

    public void removeCitationGroup(CitationGroup group, XTextDocument document)
            throws WriterDocumentAccessException, WrappedTargetException, NotRemoveableException {
        StoredMark mark = stored(group);
        mark.range().removeFromDocument(document);
        for (int index = 0; index < mark.data().citationKeys().size(); index++) {
            UndoableDocumentProperties.remove(document, pageInfoProperty(mark.data().uniqueId(), index));
        }
        marks.remove(group.groupId);
    }

    public Optional<XTextRange> getMarkRange(CitationGroup group, XTextDocument document)
            throws WriterDocumentAccessException, WrappedTargetException {
        return stored(group).range().getMarkRange(document);
    }

    public Optional<XTextCursor> getRawCursorForCitationGroup(CitationGroup group, XTextDocument document)
            throws WriterDocumentAccessException, WrappedTargetException {
        return stored(group).range().getRawCursor(document);
    }

    public XTextCursor getFillCursorForCitationGroup(CitationGroup group, XTextDocument document)
            throws WriterDocumentAccessException, WrappedTargetException, WriterOperationException {
        return stored(group).range().getFillCursor(document);
    }

    public void cleanFillCursorForCitationGroup(CitationGroup group, XTextDocument document)
            throws WriterDocumentAccessException, WrappedTargetException {
        stored(group).range().cleanFillCursor(document);
    }

    public void savePageInfos(XTextDocument document, CitationGroup group)
            throws WriterDocumentAccessException, WrappedTargetException, NotRemoveableException, PropertyVetoException, IllegalTypeException {
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
    public void removeOrphanPageInfos(XTextDocument document) throws WriterDocumentAccessException, NotRemoveableException, WrappedTargetException {
        Set<String> liveProperties = new HashSet<>();
        for (String name : getJabRefReferenceMarkNames(document)) {
            try {
                JabRefMarkData data = JabRefMarkCodec.decode(name);
                for (int index = 0; index < data.citationKeys().size(); index++) {
                    liveProperties.add(pageInfoProperty(data.uniqueId(), index));
                }
            } catch (JabRefMarkCodecException exception) {
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
            throws WriterDocumentAccessException, WrappedTargetException {
        List<CitationEntry> result = new ArrayList<>();
        for (CitationGroup group : groups.getCitationGroupsUnordered()) {
            XTextCursor cursor = getRawCursorForCitationGroup(group, document)
                    .orElseThrow(() -> new WriterDocumentAccessException("Missing citation range"));
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
