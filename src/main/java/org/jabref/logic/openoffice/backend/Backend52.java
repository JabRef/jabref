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
        List<String> allNames = this.citationStorageManager.nrmGetUsedNames(doc);
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

        // Collect unused jabrefPropertyNames
        Set<String> citationGroupNamesSet = citationGroupNames.stream().collect(Collectors.toSet());

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
            return Optional.empty(); // "Backend52: found no unused pageInfo data";
        }
        String msg =
            "Backend52: found unused pageInfo data, with names listed below.\n"
            + "In LibreOffice you may remove these in [File]/[Properties]/[Custom Properties]\n";
        msg += "" + String.join("\n", pageInfoThrash) + "";
        return Optional.of(msg);
    }

    private static void setPageInfoInDataInitial(List<Citation> citations, Optional<OOText> pageInfo) {
        // attribute to last citation (initially localOrder == storageOrder)
        if (!citations.isEmpty()) {
            citations.get(citations.size() - 1).setPageInfo(pageInfo);
        }
    }

    private static Optional<OOText> getPageInfoFromData(CitationGroup cg) {
        List<Citation> citations = cg.getCitationsInLocalOrder();
        if (citations.isEmpty()) {
            return Optional.empty();
        }
        return citations.get(citations.size() - 1).getPageInfo();
    }

    /**
     *  We have circular dependency here: backend uses
     *  class from ...
     */
    public CitationGroup readCitationGroupFromDocumentOrThrow(XTextDocument doc, String refMarkName)
        throws
        WrappedTargetException,
        NoDocumentException {

        Optional<Codec52.ParsedMarkName> optionalParsed = Codec52.parseMarkName(refMarkName);
        if (optionalParsed.isEmpty()) {
            throw new IllegalArgumentException("readCitationGroupFromDocumentOrThrow:"
                                               + " found unparsable referenceMarkName");
        }
        Codec52.ParsedMarkName parsed = optionalParsed.get();
        List<Citation> citations = (parsed.citationKeys.stream()
                                    .map(Citation::new)
                                    .collect(Collectors.toList()));

        Optional<OOText> pageInfo = (UnoUserDefinedProperty.getStringValue(doc, refMarkName)
                                     .map(OOText::fromString));
        pageInfo = PageInfo.normalizePageInfo(pageInfo);

        setPageInfoInDataInitial(citations, pageInfo);

        Optional<NamedRange> namedRange = citationStorageManager.nrmGetFromDocument(doc, refMarkName);

        if (namedRange.isEmpty()) {
            throw new IllegalArgumentException("readCitationGroupFromDocumentOrThrow:"
                                               + " referenceMarkName is not in the document");
        }

        CitationGroupId cgid = new CitationGroupId(refMarkName);
        CitationGroup cg = new CitationGroup(OODataModel.JabRef52,
                                             cgid,
                                             parsed.citationType,
                                             citations,
                                             Optional.of(refMarkName));
        this.cgidToNamedRange.put(cgid, namedRange.get());
        return cg;
    }

    /**
     *  Create a reference mark with the given name, at the end of position.
     *
     *  On return {@code position} is collapsed, and is after the inserted space, or at the end of
     *  the reference mark.
     *
     *  @param position Collapsed to its end.
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
            throw new IllegalArgumentException("pageInfos.size != citationKeys.size");
        }

        // Get a new refMarkName
        Set<String> usedNames = new HashSet<>(this.citationStorageManager.nrmGetUsedNames(doc));
        String xkey = (citationKeys.stream().collect(Collectors.joining(",")));
        String refMarkName = Codec52.getUniqueMarkName(usedNames, xkey, citationType);

        CitationGroupId cgid = new CitationGroupId(refMarkName);

        final int nCitations = citationKeys.size();
        final int last = nCitations - 1;

        // Build citations, add pageInfo to each citation
        List<Citation> citations = new ArrayList<>(nCitations);
        for (int i = 0; i < nCitations; i++) {
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
         * Apply to document
         */
        boolean withoutBrackets = (citationType == CitationType.INVISIBLE_CIT);
        NamedRange namedRange =
            this.citationStorageManager.nrmCreate(doc, refMarkName, position, insertSpaceAfter,
                                                  withoutBrackets);

        switch (dataModel) {
        case JabRef52:
            Optional<OOText> pageInfo = PageInfo.normalizePageInfo(pageInfos.get(last));

            if (pageInfo.isPresent()) {
                String pageInfoString = OOText.toString(pageInfo.get());
                UnoUserDefinedProperty.setStringProperty(doc, refMarkName, pageInfoString);
            } else {
                // do not inherit from trash
                UnoUserDefinedProperty.removeIfExists(doc, refMarkName);
            }
            CitationGroup cg = new CitationGroup(OODataModel.JabRef52,
                                                 cgid,
                                                 citationType, citations,
                                                 Optional.of(refMarkName));
            this.cgidToNamedRange.put(cgid, namedRange);
            return cg;
        default:
            throw new IllegalStateException("Backend52 requires JabRef52 dataModel");
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
            // collect to cgPageInfos
            List<Optional<OOText>> cgPageInfos = OOListUtil.map(joinableGroup,
                                                                Backend52::getPageInfoFromData);

            // Try to do something of the cgPageInfos.
            String cgPageInfo = (cgPageInfos.stream()
                                 .filter(pi -> pi.isPresent())
                                 .map(pi -> OOText.toString(pi.get()))
                                 .distinct()
                                 .collect(Collectors.joining("; ")));

            int nCitations = (joinableGroup.stream()
                              .map(CitationGroup::numberOfCitations)
                              .mapToInt(Integer::intValue).sum());
            if ("".equals(cgPageInfo)) {
                cgPageInfo = null;
            }
            return OODataModel.fakePageInfos(cgPageInfo, nCitations);

        case JabRef60:
            return (joinableGroup.stream()
                    .flatMap(cg -> (cg.citationsInStorageOrder.stream()
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

    private NamedRange getNamedRangeOrThrow(CitationGroup cg) {
        NamedRange namedRange = this.cgidToNamedRange.get(cg.cgid);
        if (namedRange == null) {
            throw new IllegalStateException("getNamedRange: could not lookup namedRange");
        }
        return namedRange;
    }

    public void removeCitationGroup(CitationGroup cg, XTextDocument doc)
        throws
        WrappedTargetException,
        NoDocumentException,
        NotRemoveableException {
        NamedRange namedRange = getNamedRangeOrThrow(cg);
        String refMarkName = namedRange.nrGetRangeName();
        namedRange.nrRemoveFromDocument(doc);
        UnoUserDefinedProperty.removeIfExists(doc, refMarkName);
        this.cgidToNamedRange.remove(cg.cgid);
    }

    /**
     * @return Optional.empty if the reference mark is missing.
     */
    public Optional<XTextRange> getMarkRange(CitationGroup cg, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {

        NamedRange namedRange = getNamedRangeOrThrow(cg);
        return namedRange.nrGetMarkRange(doc);
    }

    /**
     * Cursor for the reference marks as is: not prepared for filling, but does not need
     * cleanFillCursorForCitationGroup either.
     */
    public Optional<XTextCursor> getRawCursorForCitationGroup(CitationGroup cg, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {
        NamedRange namedRange = getNamedRangeOrThrow(cg);
        return namedRange.nrGetRawCursor(doc);
    }

    /**
     * Must be followed by call to cleanFillCursorForCitationGroup
     */
    public XTextCursor getFillCursorForCitationGroup(CitationGroup cg, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        NamedRange namedRange = getNamedRangeOrThrow(cg);
        return namedRange.nrGetFillCursor(doc);
    }

    /** To be called after getFillCursorForCitationGroup */
    public void cleanFillCursorForCitationGroup(CitationGroup cg, XTextDocument doc)
        throws
        NoDocumentException,
        WrappedTargetException {
        NamedRange namedRange = getNamedRangeOrThrow(cg);
        namedRange.nrCleanFillCursor(doc);
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
            for (CitationGroup cg : cgs.getCitationGroupsUnordered()) {
                String name = cg.cgid.citationGroupIdAsString();
                XTextCursor cursor = (this
                                      .getRawCursorForCitationGroup(cg, doc)
                                      .orElseThrow(IllegalStateException::new));
                String context = GetContext.getCursorStringWithContext(cursor, 30, 30, true);
                Optional<String> pageInfo = (cg.numberOfCitations() > 0
                                             ? (getPageInfoFromData(cg)
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

