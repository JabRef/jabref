package org.jabref.gui.openoffice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.JabRefException;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;

import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextRange;
import com.sun.star.uno.UnoRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Backend52 {
    public final Compat.DataModel dataModel;
    public final StorageBase.NamedRangeManager citationStorageManager;
    // uses: Codec52
    Backend52() {
        this.dataModel = Compat.DataModel.JabRef52;
        this.citationStorageManager = new StorageBaseRefMark.Manager();
    }

    /**
     * Get reference mark names from the document matching the pattern
     * used for JabRef reference mark names.
     *
     * Note: the names returned are in arbitrary order.
     *
     */
    public List<String> getJabRefReferenceMarkNames(DocumentConnection documentConnection)
        throws
        NoDocumentException {
        List<String> allNames = this.citationStorageManager.getUsedNames(documentConnection);
        return Codec52.filterIsJabRefReferenceMarkName(allNames);
    }

    /**
     * Names of custom properties belonging to us, but without a
     * corresponding reference mark.  These can be deleted.
     *
     * @param citationGroupNames These are the names that are used.
     *
     */
    private List<String> findUnusedJabrefPropertyNames(DocumentConnection documentConnection,
                                                       List<String> citationGroupNames) {
        // Collect unused jabrefPropertyNames
        Set<String> citationGroupNamesSet =
            citationGroupNames.stream().collect(Collectors.toSet());

        List<String> pageInfoThrash = new ArrayList<>();
        List<String> jabrefPropertyNames =
            documentConnection.getCustomPropertyNames()
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
     *  @return null if all is OK, message text otherwise.
     */
    public String healthReport(DocumentConnection documentConnection)
        throws
        NoDocumentException {
        List<String> pageInfoThrash =
            this.findUnusedJabrefPropertyNames(documentConnection,
                                               this.getJabRefReferenceMarkNames(documentConnection));
        if ( pageInfoThrash.isEmpty() ){
            return null; //"Backend52: found no unused pageInfo data";
        }
        String msg = (
            "Backend52: found unused pageInfo data, with names listed below.\n"
            + "In LibreOffice you may remove these in [File]/[Properties]/[Custom Properties]\n"
            );
        msg += "" + String.join( "\n",  pageInfoThrash ) + "";
        return msg;
    }

    /**
     *  We have circular dependency here: backend uses
     *  class from ...
     */
    public CitationGroup readCitationGroupFromDocumentOrThrow(DocumentConnection documentConnection,
                                                              String refMarkName)
        throws
        WrappedTargetException,
        NoDocumentException {

        Optional<Codec52.ParsedMarkName> op = Codec52.parseMarkName(refMarkName);
        if (op.isEmpty()) {
            // We have a problem. We want types[i] and bibtexKeys[i]
            // to correspond to referenceMarkNames.get(i).
            // And do not want null in bibtexKeys (or error code in types)
            // on return.
            throw new IllegalArgumentException("readCitationGroupFromDocumentOrThrow:"
                                               + " found unparsable referenceMarkName");
        }
        Codec52.ParsedMarkName ov = op.get();
        CitationGroupID id = new CitationGroupID(refMarkName);
        List<Citation> citations = ((ov.citationKeys == null)
                                    ? new ArrayList<>()
                                    : (ov.citationKeys.stream()
                                       .map(Citation::new)
                                       .collect(Collectors.toList())));

        Optional<String> pageInfo = documentConnection.getCustomProperty(refMarkName);

        StorageBase.NamedRange sr = citationStorageManager.getFromDocumentOrNull(documentConnection,
                                                                                 refMarkName);

        if (sr == null) {
            throw new IllegalArgumentException(
                "readCitationGroupFromDocumentOrThrow: referenceMarkName is not in the document");
        }

        CitationGroup cg = new CitationGroup(id,
                                             sr,
                                             ov.itcType,
                                             citations,
                                             pageInfo,
                                             refMarkName);
        return cg;
    }

    /**
     *  Create a reference mark with the given name, at the
     *  end of position.
     *
     *  To reduce the difference from the original representation, we
     *  only insist on having at least two characters inside reference
     *  marks. These may be ZERO_WIDTH_SPACE characters or other
     *  placeholder not likely to appear in a citation mark.
     *
     *  This placeholder is only needed if the citation mark is
     *  otherwise empty (e.g. when we just create it).
     *
     *  getFillCursorForCitationGroup yields a bracketed cursor, that
     *  can be used to fill in / overwrite the value inside.
     *
     *  After each getFillCursorForCitationGroup, we require a call to
     *  cleanFillCursorForCitationGroup, which removes the brackets,
     *  unless if it would make the content less than two
     *  characters. If we need only one placeholder, we keep the left
     *  bracket.  If we need two, then the content is empty. The
     *  removeBracketsFromEmpty parameter of
     *  cleanFillCursorForCitationGroup overrides this, and for empty
     *  citations it will remove the brackets, leaving an empty
     *  reference mark. The idea behind this is that we do not need to
     *  refill empty marks (itcTypes INVISIBLE_CIT), and the caller
     *  can tell us that we are dealing with one of these.
     *
     *  Thus the only user-visible difference in citation marks is
     *  that instead of empty marks we use two brackets, for
     *  single-character marks we add a left bracket before.
     *
     *  Character-attribute inheritance: updates inherit from the
     *  first character inside, not from the left.
     *
     *  On return {@code position} is collapsed, and is after the
     *  inserted space, or at the end of the reference mark.
     *
     *  @param documentConnection Connection to document.
     *  @param position Collapsed to its end.
     *  @param insertSpaceAfter We insert a space after the mark, that
     *                          carries on format of characters from
     *                          the original position.
     *
     *  @param withoutBrackets  Force empty reference mark (no brackets).
     *                          For use with INVISIBLE_CIT.
     *
     */
    public CitationGroup createCitationGroup(DocumentConnection documentConnection,
                                             List<String> citationKeys,
                                             // Optional<String> pageInfo,
                                             List<String> pageInfosForCitations,
                                             int itcType,
                                             XTextCursor position,
                                             boolean insertSpaceAfter,
                                             boolean withoutBrackets)
        throws
        CreationException,
        NoDocumentException,
        WrappedTargetException,
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException {

        String xkey =
            citationKeys.stream()
            .collect(Collectors.joining(","));

        Set<String> usedNames = new HashSet<>( this.citationStorageManager
                                               .getUsedNames(documentConnection) );
        String refMarkName =
            Codec52.getUniqueMarkName(usedNames,
                                      xkey,
                                      itcType);

        CitationGroupID cgid = new CitationGroupID(refMarkName);

        List<Citation> citations = new ArrayList<>(citationKeys.size());
        for (int i = 0; i < citationKeys.size(); i++) {
            Citation cit = new Citation(citationKeys.get(i));
            citations.add(cit);
            // cit.setPageInfo(getJabRef53PageInfo(pageInfosForCitations, i));
        }

        /*
         * Apply to document
         */
        StorageBase.NamedRange sr = this.citationStorageManager.create(documentConnection,
                                                                       refMarkName,
                                                                       position,
                                                                       insertSpaceAfter,
                                                                       withoutBrackets);

        switch (dataModel){
        case JabRef52:
            Optional<String> pageInfo = getJabRef52PageInfoFromList( pageInfosForCitations );
            if ( pageInfo.isPresent() && !pageInfo.get().equals("") ) {
                documentConnection.setCustomProperty(refMarkName, pageInfo.get());
            } else {
                documentConnection.removeCustomProperty(refMarkName);
            }
            CitationGroup cg = new CitationGroup(cgid,
                                                 sr,
                                                 itcType,
                                                 citations,
                                                 pageInfo,
                                                 refMarkName);
            return cg;
        default:
            throw new RuntimeException("Backend52 requires JabRef52 dataModel");
        }
    }

    Optional<String> getJabRef52PageInfoFromList(List<String> pageInfosForCitations) {
        if (pageInfosForCitations == null) {
            return Optional.empty();
        }
        int n = pageInfosForCitations.size();
        if (n == 0) {
            return Optional.empty();
        }
        return Optional.ofNullable(pageInfosForCitations.get(n-1));
    }

    /**
     * @param pageInfo Nullable.
     */
    static List<String> fakePageInfosForCitations(String pageInfo,
                                                  int nCitations,
                                                  boolean mayReturnNull) {
        // JabRef53 style pageInfo list, or null
        List<String> pageInfosForCitations = null;
        if (!mayReturnNull || (pageInfo != null && !pageInfo.equals(""))) {
            pageInfosForCitations = new ArrayList<>(nCitations);
            for (int i = 0; i < nCitations; i++) {
                if (i == nCitations - 1) {
                    pageInfosForCitations.add(pageInfo);
                } else {
                    pageInfosForCitations.add(null);
                }
            }
        }
        return pageInfosForCitations;
    }

    /**
     * @return List of nullable pageInfo values, one for each citation.
     *         Result contains null for missing pageInfo values.
     *         The list itself is not null.
     *
     *         For Compat.DataModel.JabRef52 the last citation gets
     *         the CitationGroup.pageInfo
     *
     *         The result is passed to OOBibStyle.getCitationMarker or
     *          OOBibStyle.getNumCitationMarker
     *
     *          TODO: we may want class DataModel52, DataModel53 and split this.
     */
    static List<String> getPageInfosForCitations(Compat.DataModel dataModel, CitationGroup cg) {
        switch (dataModel){
        case JabRef52:
            // check conformance to dataModel
            for ( int i = 0; i < cg.citations.size(); i++ ) {
                if ( cg.citations.get(i).pageInfo.isPresent() ) {
                    throw new RuntimeException("getPageInfosForCitations:"
                                               + " found Citation.pageInfo under JabRef52 dataModel");
                }
            }
            // A list of null values, except the last that comes from this.pageInfo
            return Backend52.fakePageInfosForCitations(cg.pageInfo.orElse(null),
                                                       cg.citations.size(),
                                                       false /*mayReturnNull*/);

        case JabRef53:
            // check conformance to dataModel
            if (cg.pageInfo.isPresent()) {
                throw new RuntimeException("getPageInfosForCitations:"
                                           + " found CitationGroup.pageInfo under JabRef53 dataModel");
            }
            // pageInfo values from citations, empty mapped to null.
            return (cg.citations.stream()
                    .map(cit -> cit.pageInfo.orElse(null))
                    .collect(Collectors.toList()));

        default:
            throw new RuntimeException("getPageInfosForCitations:"
                                       + "unhandled dataModel");
        }
    }

    /**
     * @return A list with one nullable pageInfo entry for each citation in
     *         joinableGroups.
     */
    static List<String> combinePageInfos(Compat.DataModel dataModel,
                                         List<CitationGroup> joinableGroup ) {
        switch (dataModel) {
        case JabRef52:
            // collect to cgPageInfos
            List<Optional<String>> cgPageInfos = (joinableGroup.stream()
                                                  .map(cg -> cg.pageInfo)
                                                  .collect(Collectors.toList()));

            // Try to do something of the cgPageInfos.
            String cgPageInfo = (cgPageInfos.stream()
                                 .filter(pi -> pi.isPresent())
                                 .map(pi -> pi.get())
                                 .distinct()
                                 .collect(Collectors.joining("; ")));

            int nCitations = (joinableGroup.stream()
                              .map(cg -> cg.citations.size())
                              .mapToInt(Integer::intValue).sum());

            return Backend52.fakePageInfosForCitations(cgPageInfo,
                                                       nCitations,
                                                       false /*mayReturnNull*/);

        case JabRef53:
            return (joinableGroup.stream()
                    .flatMap( cg -> (cg.citations.stream()
                                     .map(cit -> cit.pageInfo.orElse(null))))
                    .collect(Collectors.toList()));
        default:
            throw new RuntimeException("unhandled dataModel here");
        }
    }

    /**
     * 
     */
    public List<String> combinePageInfos(List<CitationGroup> joinableGroup ) {
        return combinePageInfos(this.dataModel, joinableGroup);
    }

    /*
     * @return A list of nullable pageInfo values, one for each citation.
     *         Result contains null for missing pageInfo values.
     *         The list itself is not null.
     *
     */
    public List<String> getPageInfosForCitations(CitationGroup cg) {
        return getPageInfosForCitations(this.dataModel, cg);
    }

    public void removeCitationGroup(CitationGroup cg, DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException,
        NoSuchElementException,
        NotRemoveableException,
        IllegalTypeException,
        PropertyExistException {

        String refMarkName = cg.cgRangeStorage.getName();
        cg.cgRangeStorage.removeFromDocument(documentConnection);
        documentConnection.removeCustomProperty(refMarkName);
    }

    /**
     *
     * @return Null if the reference mark is missing.
     *
     */
    public XTextRange getMarkRangeOrNull(CitationGroup cg,
                                         DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException {

        return cg.cgRangeStorage.getMarkRangeOrNull(documentConnection);
    }

    /**
     * Cursor for the reference marks as is, not prepared for filling,
     * but does not need cleanFillCursorForCitationGroup either.
     */
    public XTextCursor getRawCursorForCitationGroup(CitationGroup cg,
                                                    DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {
        return cg.cgRangeStorage.getRawCursor(documentConnection);
    }

    /**
     * Must be followed by call to cleanFillCursorForCitationGroup
     */
    public XTextCursor getFillCursorForCitationGroup(CitationGroup cg,
                                                     DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        return cg.cgRangeStorage.getFillCursor(documentConnection);
    }

    /** To be called after getFillCursorForCitationGroup */
    public void cleanFillCursorForCitationGroup(CitationGroup cg,
                                                DocumentConnection documentConnection)
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {
        cg.cgRangeStorage.cleanFillCursor(documentConnection);
    }

} // end Backend52

