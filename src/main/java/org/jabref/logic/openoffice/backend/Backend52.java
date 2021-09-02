package org.jabref.logic.openoffice.backend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.openoffice.CitationEntry;
import org.jabref.model.openoffice.backend.NamedRange;
import org.jabref.model.openoffice.backend.NamedRangeManager;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationGroups;
import org.jabref.model.openoffice.style.CitationType;
import org.jabref.model.openoffice.style.OODataModel;
import org.jabref.model.openoffice.style.PageInfo;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.uno.UnoUserDefinedProperty;
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

/**
 * Backend52, Codec52 and OODataModel.JabRef52 refer to the mode of storage, encoding and
 * what-is-stored in the document under JabRef version 5.2. These basically did not change up to
 * JabRef 5.4.
 */
public class Backend52 {
    private static final Logger LOGGER = LoggerFactory.getLogger(Backend52.class);
    public final OODataModel dataModel;
    private final NamedRangeManager citationStorageManager;
    private final Map<CitationGroupId, NamedRange> cgidToNamedRange;

    // uses: Codec52
    public Backend52() {
        this.dataModel = OODataModel.JabRef52;
        this.citationStorageManager = new NamedRangeManagerReferenceMark();
        this.cgidToNamedRange = new HashMap<>();
    }

    /**
     * Get reference mark names from the document matching the pattern
     * used for JabRef reference mark names.
     *
     * Note: the names returned are in arbitrary order.
     *
     */
    public List<String> getJabRefReferenceMarkNames(XTextDocument doc)
        throws
        NoDocumentException {
        List<String> allNames = this.citationStorageManager.getUsedNames(doc);
        return Codec52.filterIsJabRefReferenceMarkName(allNames);
    }

    /**
     * Names of custom properties belonging to us, but without a corresponding reference mark.
     * These can be deleted.
     *
     * @param citationGroupNames These are the names that are used.
     *
     */
    private List<String> findUnusedJabrefPropertyNames(XTextDocument doc,
                                                       List<String> citationGroupNames) {

        Set<String> citationGroupNamesSet = new HashSet<>(citationGroupNames);

        List<String> pageInfoThrash = new ArrayList<>();
        List<String> jabrefPropertyNames =
            UnoUserDefinedProperty.getListOfNames(doc)
            .stream()
            .filter(Codec52::isJabRefReferenceMarkName)
            .collect(Collectors.toList());
        for (String pn : jabrefPropertyNames) {
            if (!citationGroupNamesSet.contains(pn)) {
                pageInfoThrash.add(pn);
            }
        }
        return pageInfoThrash;
    }

    /**
     *  @return Optional.empty if all is OK, message text otherwise.
     */
    public Optional<String> healthReport(XTextDocument doc)
        throws
        NoDocumentException {
        List<String> pageInfoThrash =
            this.findUnusedJabrefPropertyNames(doc, this.getJabRefReferenceMarkNames(doc));
        if (pageInfoThrash.isEmpty()) {
            return Optional.empty();
        }

        StringBuilder msg = new StringBuilder("Backend52: found unused pageInfo data, with names listed below.\n");
        msg.append("In LibreOffice you may remove these in [File]/[Properties]/[Custom Properties]\n");
        msg.append(String.join("\n", pageInfoThrash));
        return Optional.of(msg.toString());
    }

    private static void setPageInfoInDataInitial(List<Citation> citations, Optional<OOText> pageInfo) {
        // attribute to last citation (initially localOrder == storageOrder)
        if (!citations.isEmpty()) {
            citations.get(citations.size() - 1).setPageInfo(pageInfo);
        }
    }

    private static Optional<OOText> getPageInfoFromData(CitationGroup group) {
        List<Citation> citations = group.getCitationsInLocalOrder();
        if (citations.isEmpty()) {
            return Optional.empty();
        }
        return citations.get(citations.size() - 1).getPageInfo();
    }

    /**
     * @param markName Reference mark name
     */
    public CitationGroup readCitationGroupFromDocumentOrThrow(XTextDocument doc, String markName)
        throws
        WrappedTargetException,
        NoDocumentException {

        Codec52.ParsedMarkName parsed = Codec52.parseMarkName(markName).orElseThrow(IllegalArgumentException::new);

        List<Citation> citations = (parsed.citationKeys.stream()
                                    .map(Citation::new)
                                    .collect(Collectors.toList()));

        Optional<OOText> pageInfo = (UnoUserDefinedProperty.getStringValue(doc, markName)
                                     .map(OOText::fromString));
        pageInfo = PageInfo.normalizePageInfo(pageInfo);

        setPageInfoInDataInitial(citations, pageInfo);

        NamedRange namedRange = (citationStorageManager.getNamedRangeFromDocument(doc, markName)
                                 .orElseThrow(IllegalArgumentException::new));

        CitationGroupId groupId = new CitationGroupId(markName);
        CitationGroup group = new CitationGroup(OODataModel.JabRef52,
                                                groupId,
                                                parsed.citationType,
                                                citations,
                                                Optional.of(markName));
        this.cgidToNamedRange.put(groupId, namedRange);
        return group;
    }

    /**
     *  Create a reference mark at the end of {@code position} in the document.
     *
     *  On return {@code position} is collapsed, and is after the inserted space, or at the end of
     *  the reference mark.
     *
     *  @param citationKeys Keys to be cited.
     *
     *  @param pageInfos An optional pageInfo for each citation key.
     *                   Backend52 only uses and stores the last pageInfo,
     *                   all others should be Optional.empty()
     *
     *  @param position Collapsed to its end.
     *
     *  @param insertSpaceAfter We insert a space after the mark, that carries on format of
     *                          characters from the original position.
     */
    public CitationGroup createCitationGroup(XTextDocument doc,
                                             List<String> citationKeys,
                                             List<Optional<OOText>> pageInfos,
                                             CitationType citationType,
                                             XTextCursor position,
                                             boolean insertSpaceAfter)
        throws
        CreationException,
        NoDocumentException,
        WrappedTargetException,
        NotRemoveableException,
        PropertyVetoException,
        IllegalTypeException {

        Objects.requireNonNull(pageInfos);
        if (pageInfos.size() != citationKeys.size()) {
            throw new IllegalArgumentException();
        }

        final int numberOfCitations = citationKeys.size();
        final int last = numberOfCitations - 1;

        // Build citations, add pageInfo to each citation
        List<Citation> citations = new ArrayList<>(numberOfCitations);
        for (int i = 0; i < numberOfCitations; i++) {
            Citation cit = new Citation(citationKeys.get(i));
            citations.add(cit);

            Optional<OOText> pageInfo = PageInfo.normalizePageInfo(pageInfos.get(i));
            switch (dataModel) {
                case JabRef52:
                    if (i == last) {
                        cit.setPageInfo(pageInfo);
                    } else {
                        if (pageInfo.isPresent()) {
                            LOGGER.warn("dataModel JabRef52"
                                        + " only supports pageInfo for the last citation of a group");
                        }
                    }
                    break;
                case JabRef60:
                    cit.setPageInfo(pageInfo);
                    break;
                default:
                    throw new IllegalStateException("Unhandled dataModel in Backend52.createCitationGroup");
            }
        }

        /*
         * Backend52 uses reference marks to (1) mark the location of the citation in the text and (2) to encode
         * the citation keys and citation type in the name of the reference mark. The name of the reference mark
         * has to be unique in the document.
         */
        final String markName = Codec52.getUniqueMarkName(new HashSet<>(citationStorageManager.getUsedNames(doc)),
                                                          citationKeys,
                                                          citationType);

        final CitationGroupId groupId = new CitationGroupId(markName);

        /*
         * Apply to document
         */
        boolean withoutBrackets = (citationType == CitationType.INVISIBLE_CIT);
        NamedRange namedRange = this.citationStorageManager.createNamedRange(doc,
                                                                             markName,
                                                                             position,
                                                                             insertSpaceAfter,
                                                                             withoutBrackets);

        switch (dataModel) {
            case JabRef52:
                Optional<OOText> pageInfo = PageInfo.normalizePageInfo(pageInfos.get(last));

                if (pageInfo.isPresent()) {
                    String pageInfoString = OOText.toString(pageInfo.get());
                    UnoUserDefinedProperty.setStringProperty(doc, markName, pageInfoString);
                } else {
                    // do not inherit from trash
                    UnoUserDefinedProperty.removeIfExists(doc, markName);
                }
                CitationGroup group = new CitationGroup(OODataModel.JabRef52,
                                                        groupId,
                                                        citationType, citations,
                                                        Optional.of(markName));
                this.cgidToNamedRange.put(groupId, namedRange);
                return group;
            case JabRef60:
                throw new IllegalStateException("createCitationGroup for JabRef60 is not implemented yet");
            default:
                throw new IllegalStateException("Unhandled dataModel in Backend52.createCitationGroup");
        }
    }

    /**
     * @return A list with a nullable pageInfo entry for each citation in joinableGroups.
     *
     *  TODO: JabRef52 combinePageInfos is not reversible. Should warn user to check the result. Or
     *        ask what to do.
     */
    public static List<Optional<OOText>>
    combinePageInfosCommon(OODataModel dataModel, List<CitationGroup> joinableGroup) {
        switch (dataModel) {
            case JabRef52:
                // collect to pageInfos
                List<Optional<OOText>> pageInfos = OOListUtil.map(joinableGroup,
                                                                  Backend52::getPageInfoFromData);

                // Try to do something of the pageInfos.
                String singlePageInfo = (pageInfos.stream()
                                         .filter(Optional::isPresent)
                                         .map(pi -> OOText.toString(pi.get()))
                                         .distinct()
                                         .collect(Collectors.joining("; ")));

                int totalCitations = (joinableGroup.stream()
                                      .map(CitationGroup::numberOfCitations)
                                      .mapToInt(Integer::intValue).sum());
                if ("".equals(singlePageInfo)) {
                    singlePageInfo = null;
                }
                return OODataModel.fakePageInfos(singlePageInfo, totalCitations);

            case JabRef60:
                return (joinableGroup.stream()
                        .flatMap(group -> (group.citationsInStorageOrder.stream()
                                           .map(Citation::getPageInfo)))
                        .collect(Collectors.toList()));
            default:
                throw new IllegalArgumentException("unhandled dataModel here");
        }
    }

    /**
     *
     */
    public List<Optional<OOText>> combinePageInfos(List<CitationGroup> joinableGroup) {
        return combinePageInfosCommon(this.dataModel, joinableGroup);
    }

    private NamedRange getNamedRangeOrThrow(CitationGroup group) {
        NamedRange namedRange = this.cgidToNamedRange.get(group.groupId);
        if (namedRange == null) {
            throw new IllegalStateException("getNamedRange: could not lookup namedRange");
        }
        return namedRange;
    }

    public void removeCitationGroup(CitationGroup group, XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException,
        NotRemoveableException {
        NamedRange namedRange = getNamedRangeOrThrow(group);
        String refMarkName = namedRange.getRangeName();
        namedRange.removeFromDocument(doc);
        UnoUserDefinedProperty.removeIfExists(doc, refMarkName);
        this.cgidToNamedRange.remove(group.groupId);
    }

    /**
     * @return Optional.empty if the reference mark is missing.
     */
    public Optional<XTextRange> getMarkRange(CitationGroup group, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        NamedRange namedRange = getNamedRangeOrThrow(group);
        return namedRange.getMarkRange(doc);
    }

    /**
     * Cursor for the reference marks as is: not prepared for filling, but does not need
     * cleanFillCursorForCitationGroup either.
     */
    public Optional<XTextCursor> getRawCursorForCitationGroup(CitationGroup group, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {
        NamedRange namedRange = getNamedRangeOrThrow(group);
        return namedRange.getRawCursor(doc);
    }

    /**
     * Must be followed by call to cleanFillCursorForCitationGroup
     */
    public XTextCursor getFillCursorForCitationGroup(CitationGroup group, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        NamedRange namedRange = getNamedRangeOrThrow(group);
        return namedRange.getFillCursor(doc);
    }

    /** To be called after getFillCursorForCitationGroup */
    public void cleanFillCursorForCitationGroup(CitationGroup group, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {
        NamedRange namedRange = getNamedRangeOrThrow(group);
        namedRange.cleanFillCursor(doc);
    }

    public List<CitationEntry> getCitationEntries(XTextDocument doc, CitationGroups cgs)
        throws
        WrappedTargetException,
        NoDocumentException {

        switch (dataModel) {
            case JabRef52:
                // One context per CitationGroup: Backend52 (DataModel.JabRef52)
                // For DataModel.JabRef60 (Backend60) we need one context per Citation
                List<CitationEntry> citations = new ArrayList<>(cgs.numberOfCitationGroups());
                for (CitationGroup group : cgs.getCitationGroupsUnordered()) {
                    String name = group.groupId.citationGroupIdAsString();
                    XTextCursor cursor = (this
                                          .getRawCursorForCitationGroup(group, doc)
                                          .orElseThrow(IllegalStateException::new));
                    String context = GetContext.getCursorStringWithContext(cursor, 30, 30, true);
                    Optional<String> pageInfo = (group.numberOfCitations() > 0
                                                 ? (getPageInfoFromData(group)
                                                    .map(e -> OOText.toString(e)))
                                                 : Optional.empty());
                    CitationEntry entry = new CitationEntry(name, context, pageInfo);
                    citations.add(entry);
                }
                return citations;
            case JabRef60:
                // xx
                throw new IllegalStateException("getCitationEntries for JabRef60 is not implemented yet");
            default:
                throw new IllegalStateException("getCitationEntries: unhandled dataModel ");
        }
    }

    /*
     * Only applies to storage. Citation markers are not changed.
     */
    public void applyCitationEntries(XTextDocument doc, List<CitationEntry> citationEntries)
        throws
        PropertyVetoException,
        IllegalTypeException,
        IllegalArgumentException,
        WrappedTargetException {

        switch (dataModel) {
            case JabRef52:
                for (CitationEntry entry : citationEntries) {
                    Optional<OOText> pageInfo = entry.getPageInfo().map(OOText::fromString);
                    pageInfo = PageInfo.normalizePageInfo(pageInfo);
                    if (pageInfo.isPresent()) {
                        String name = entry.getRefMarkName();
                        UnoUserDefinedProperty.setStringProperty(doc, name, pageInfo.get().toString());
                    }
                }
                break;
            case JabRef60:
                // xx
                throw new IllegalStateException("applyCitationEntries for JabRef60 is not implemented yet");
            default:
                throw new IllegalStateException("applyCitationEntries: unhandled dataModel ");
        }
    }

}

