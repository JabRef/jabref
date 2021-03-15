package org.jabref.gui.openoffice;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bibtex.comparator.FieldComparator;
import org.jabref.logic.bibtex.comparator.FieldComparatorStack;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.Layout;
import org.jabref.logic.openoffice.CitationEntry;
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.OOPreFormatter;
import org.jabref.logic.openoffice.OOUtil;
import org.jabref.logic.openoffice.UndefinedBibtexEntry;
import org.jabref.logic.openoffice.UndefinedParagraphFormatException;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;

import com.sun.star.awt.Point;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNamed;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.document.XUndoManager;
import com.sun.star.document.XUndoManagerSupplier;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.Locale;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XFootnote;
import com.sun.star.text.XReferenceMarksSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextRange;
import com.sun.star.text.XTextRangeCompare;
import com.sun.star.text.XTextSection;
import com.sun.star.text.XTextSectionsSupplier;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.InvalidStateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for manipulating the Bibliography of the currently started
 * document in OpenOffice.
 */
@AllowedToUseAwt("Requires AWT for italics and bold")
class OOBibBase {
    private static final OOPreFormatter POSTFORMATTER = new OOPreFormatter();

    private static final String BIB_SECTION_NAME = "JR_bib";
    private static final String BIB_SECTION_END_NAME = "JR_bib_end";

    private static final String BIB_CITATION = "JR_cite";
    private static final Pattern CITE_PATTERN =
            Pattern.compile(BIB_CITATION + "(\\d*)_(\\d*)_(.*)");

    private static final String ZERO_WIDTH_SPACE = "\u200b";

    // for debugging we may want visible bracket
    private static final boolean
    REFERENCE_MARK_USE_INVISIBLE_BRACKETS = true; // !debug;

    private static final String
    REFERENCE_MARK_LEFT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : "<";

    private static final String
    REFERENCE_MARK_RIGHT_BRACKET = REFERENCE_MARK_USE_INVISIBLE_BRACKETS ? ZERO_WIDTH_SPACE : ">";

    /** Should we always fully remove reference mark brackets? */
    private static final boolean
    REFERENCE_MARK_ALWAYS_REMOVE_BRACKETS = true;

    /* Types of in-text citation. (itcType)
     * Their numeric values are used in reference mark names.
     */
    private static final int AUTHORYEAR_PAR = 1;
    private static final int AUTHORYEAR_INTEXT = 2;
    private static final int INVISIBLE_CIT = 3;

    private static final Logger LOGGER =
        LoggerFactory.getLogger(OOBibBase.class);

    /* variables  */
    private final DialogService dialogService;
    private final XDesktop xDesktop;
    private final Comparator<BibEntry> entryComparator;
    private final Comparator<BibEntry> yearAuthorTitleComparator;


    /**
     * Created when connected to a document.
     *
     * Cleared (to null) when we discover we lost the connection.
     */
    private DocumentConnection xDocumentConnection;

    /**
     *  Map citation keys to letters ("a", "b") that
     *  make the citation markers unique.
     *
     *  Used directly (apart from passing around as `uniqueLetters`):
     *  refreshCiteMarkers, rebuildBibTextSection.
     *
     *  Depends on: style, citations and their order.
     */
    // private final Map<String, String> xUniqueLetters = new HashMap<>();

    /*
     * Constructor
     */
    public
    OOBibBase(Path loPath,
              DialogService dialogService)
        throws
        BootstrapException,
        CreationException {

        this.dialogService = dialogService;

        FieldComparator a = new FieldComparator(StandardField.AUTHOR);
        FieldComparator y = new FieldComparator(StandardField.YEAR);
        FieldComparator t = new FieldComparator(StandardField.TITLE);

        List<Comparator<BibEntry>> ayt = new ArrayList<>(3);
        ayt.add(a);
        ayt.add(y);
        ayt.add(t);
        this.entryComparator = new FieldComparatorStack<>(ayt);

        List<Comparator<BibEntry>> yat = new ArrayList<>(3);
        yat.add(y);
        yat.add(a);
        yat.add(t);
        this.yearAuthorTitleComparator = new FieldComparatorStack<>(yat);

        this.xDesktop = simpleBootstrap(loPath);
    }

    /* *****************************
     *
     *  Establish connection
     *
     * *****************************/

    private XDesktop
    simpleBootstrap(Path loPath)
        throws
        CreationException,
        BootstrapException {

        // Get the office component context:
        XComponentContext context = org.jabref.gui.openoffice.Bootstrap.bootstrap(loPath);
        XMultiComponentFactory sem = context.getServiceManager();

        // Create the desktop, which is the root frame of the
        // hierarchy of frames that contain viewable components:
        Object desktop;
        try {
            desktop = sem.createInstanceWithContext("com.sun.star.frame.Desktop", context);
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        XDesktop result = unoQI(XDesktop.class, desktop);

        unoQI(XComponentLoader.class, desktop);

        return result;
    }

    private static List<XTextDocument>
    getTextDocuments(XDesktop desktop)
        throws
        NoSuchElementException,
        WrappedTargetException {

        List<XTextDocument> result = new ArrayList<>();

        XEnumerationAccess enumAccess = desktop.getComponents();
        XEnumeration compEnum = enumAccess.createEnumeration();

        while (compEnum.hasMoreElements()) {
            Object next = compEnum.nextElement();
            XComponent comp = unoQI(XComponent.class, next);
            XTextDocument doc = unoQI(XTextDocument.class, comp);
            if (doc != null) {
                result.add(doc);
            }
        }
        return result;
    }

    /**
     *  Run a dialog allowing the user to choose among the documents in `list`.
     *
     * @return Null if no document was selected. Otherwise the
     *         document selected.
     *
     */
    private static XTextDocument
    selectDocumentDialog(
        List<XTextDocument> list,
        DialogService dialogService) {

        class DocumentTitleViewModel {

            private final XTextDocument xTextDocument;
            private final String description;

            public DocumentTitleViewModel(XTextDocument xTextDocument) {
                this.xTextDocument = xTextDocument;
                this.description = DocumentConnection.getDocumentTitle(xTextDocument).orElse("");
            }

            public XTextDocument getXtextDocument() {
                return xTextDocument;
            }

            @Override
            public String toString() {
                return description;
            }
        }

        List<DocumentTitleViewModel> viewModel =
            list.stream()
            .map(DocumentTitleViewModel::new)
            .collect(Collectors.toList());

        // This whole method is part of a background task when
        // auto-detecting instances, so we need to show dialog in FX
        // thread
        Optional<DocumentTitleViewModel> selectedDocument =
            dialogService
            .showChoiceDialogAndWait(
                Localization.lang("Select document"),
                Localization.lang("Found documents:"),
                Localization.lang("Use selected document"),
                viewModel
                );

        return
            selectedDocument
            .map(DocumentTitleViewModel::getXtextDocument)
            .orElse(null);
    }

    /**
     * Choose a document to work with.
     *
     * Assumes we have already connected to LibreOffice or OpenOffice.
     *
     * If there is a single document to choose from, selects that.
     * If there are more than one, shows selection dialog.
     * If there are none, throws NoDocumentException
     *
     * After successful selection connects to the selected document
     * and extracts some frequently used parts (starting points for
     * managing its content).
     *
     * Finally initializes this.xDocumentConnection with the selected
     * document and parts extracted.
     *
     */
    public void
    selectDocument()
        throws
        NoDocumentException,
        NoSuchElementException,
        WrappedTargetException {

        XTextDocument selected;
        List<XTextDocument> textDocumentList = getTextDocuments(this.xDesktop);
        if (textDocumentList.isEmpty()) {
            throw new NoDocumentException("No Writer documents found");
        } else if (textDocumentList.size() == 1) {
            // Get the only one
            selected = textDocumentList.get(0);
        } else {
            // Bring up a dialog
            selected =
                OOBibBase.selectDocumentDialog(
                    textDocumentList,
                    this.dialogService);
        }

        if (selected == null) {
            return;
        }

        this.xDocumentConnection = new DocumentConnection(selected);

    }

    /**
     * Mark the current document as missing.
     *
     */
    private void
    forgetDocument() {
        this.xDocumentConnection = null;
    }

    /**
     * A simple test for document availability.
     *
     * See also `documentConnectionMissing` for a test
     * actually attempting to use teh connection.
     *
     */
    public boolean isConnectedToDocument() {
        return this.xDocumentConnection != null;
    }

    /**
     * @return true if we are connected to a document
     */
    public boolean documentConnectionMissing() {
        if (this.xDocumentConnection == null) {
            return true;
        }
        boolean res = this.xDocumentConnection.documentConnectionMissing();
        if (res) {
            forgetDocument();
        }
        return res;
    }

    /**
     * Either return a valid DocumentConnection or throw
     * NoDocumentException.
     */
    private DocumentConnection
    getDocumentConnectionOrThrow()
        throws NoDocumentException {
        if (documentConnectionMissing()) {
            throw new NoDocumentException("Not connected to document");
        }
        return this.xDocumentConnection;
    }

    /**
     *  The title of the current document, or Optional.empty()
     */
    public Optional<String>
    getCurrentDocumentTitle() {
        if (documentConnectionMissing()) {
            return Optional.empty();
        } else {
            return this.xDocumentConnection.getDocumentTitle();
        }
    }

    /* ****************************
     *
     *           Misc
     *
     * ****************************/

    /**
     * unoQI : short for UnoRuntime.queryInterface
     *
     * @return A reference to the requested UNO interface type if available,
     * otherwise null
     */
    private static <T> T
    unoQI(
        Class<T> zInterface,
        Object object) {
        return UnoRuntime.queryInterface(zInterface, object);
    }

    /* ***************************************
     *
     *     Storage/retrieve of citations
     *
     *
     *  We store some information in the document about
     *
     *    Citations : citation key, pageInfo, citation group.
     *        Each belongs to exactly one group.
     *
     *    Citation groups (in case of multiple citation keys also
     *       known as "multicite", "merged citations"):
     *
     *       Which citations belong to the group.
     *       Range of text owned (where the citation marks go).
     *
     *    From these, the databases and the style we create and update
     *    the presentation (citation marks)
     *
     *    How:
     *      database lookup yields: (BibEntry,whichDatabase)
     *                              (UndefinedBibtexEntry,null) if not found
     *
     *      Local order
     *          presentation order within groups from (style,BibEntry)
     *
     *      Global order:
     *          visualPosition (for first appearance order)
     *          bibliography-order
     *
     *      Make them unique
     *         numbering
     *         uniqueLetters from (Set<BibEntry>, firstAppearanceOrder, style)
     *
     *
     *  Bibliography uses parts of the information above:
     *      citation keys,
     *      location of citation groups (if ordered and/or numbered by first appearance)
     *
     *      and
     *      the range of text controlled (storage)
     *
     *      And fills the bibliography (presentation)
     *
     * **************************************/

    /**
     * Produce a reference mark name for JabRef for the given citation
     * key and itcType that does not yet appear among the reference
     * marks of the document.
     *
     * @param bibtexKey The citation key.
     * @param itcType   Encodes the effect of withText and
     *                  inParenthesis options.
     *
     * The first occurrence of bibtexKey gets no serial number, the
     * second gets 0, the third 1 ...
     *
     * Or the first unused in this series, after removals.
     */
    private String
    getUniqueReferenceMarkName(
        DocumentConnection documentConnection,
        String bibtexKey,
        int itcType)
        throws NoDocumentException {

        XNameAccess xNamedRefMarks = documentConnection.getReferenceMarks();
        int i = 0;
        String name = BIB_CITATION + '_' + itcType + '_' + bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = BIB_CITATION + i + '_' + itcType + '_' + bibtexKey;
            i++;
        }
        return name;
    }

    /**
     * This is what we get back from parsing a refMarkName.
     *
     */
    private static class ParsedRefMark {
        /**  "", "0", "1" ... */
        public String i;
        /** in-text-citation type */
        public int itcType;
        /** Citation keys embedded in the reference mark. */
        public List<String> citedKeys;

        ParsedRefMark(String i, int itcType, List<String> citedKeys) {
            this.i = i;
            this.itcType = itcType;
            this.citedKeys = citedKeys;
        }
    }

    /**
     * Parse a JabRef reference mark name.
     *
     * @return Optional.empty() on failure.
     *
     */
    private static Optional<ParsedRefMark>
    parseRefMarkName(String refMarkName) {

        Matcher citeMatcher = CITE_PATTERN.matcher(refMarkName);
        if (!citeMatcher.find()) {
            return Optional.empty();
        }

        List<String> keys = Arrays.asList(citeMatcher.group(3).split(","));
        String i = citeMatcher.group(1);
        int itcType = Integer.parseInt(citeMatcher.group(2));
        return (Optional.of(new OOBibBase.ParsedRefMark(i, itcType, keys)));
    }

    /**
     * Extract the list of citation keys from a reference mark name.
     *
     * @param name The reference mark name.
     * @return The list of citation keys encoded in the name.
     *
     *         In case of duplicated citation keys,
     *         only the first occurrence.
     *         Otherwise their order is preserved.
     *
     *         If name does not match CITE_PATTERN,
     *         an empty list of strings is returned.
     */
    private List<String>
    parseRefMarkNameToUniqueCitationKeys(String name) {
        Optional<ParsedRefMark> op = parseRefMarkName(name);
        return
            op.map(
                parsedRefMark ->
                parsedRefMark.citedKeys.stream()
                .distinct()
                .collect(Collectors.toList())
                )
            .orElseGet(ArrayList::new);
    }

    /**
     * @return true if name matches the pattern used for JabRef
     * reference mark names.
     */
    private static boolean
    isJabRefReferenceMarkName(String name) {
        return (CITE_PATTERN.matcher(name).find());
    }

    /**
     * Filter a list of reference mark names by `isJabRefReferenceMarkName`
     *
     * @param names The list to be filtered.
     */
    private static List<String>
    filterIsJabRefReferenceMarkName(List<String> names) {
        return (names
                .stream()
                .filter(OOBibBase::isJabRefReferenceMarkName)
                .collect(Collectors.toList())
        );
    }

    /**
     * Get reference mark names from the document matching the pattern
     * used for JabRef reference mark names.
     *
     * Note: the names returned are in arbitrary order.
     *
     * See also: `getJabRefReferenceMarkNamesSortedByPosition`
     *
     */
    private List<String>
    getJabRefReferenceMarkNames(DocumentConnection documentConnection)
        throws NoDocumentException {
        List<String> allNames = documentConnection.getReferenceMarkNames();
        return filterIsJabRefReferenceMarkName(allNames);
    }

    /**
     * For each name in referenceMarkNames set types[i] and
     * bibtexKeys[i] to values parsed from referenceMarkNames.get(i)
     *
     * @param referenceMarkNames Should only contain parsable names.
     * @param types              OUT Must be same length as referenceMarkNames.
     * @param bibtexKeys         OUT First level must be same length as referenceMarkNames.
     */
    private static void
    parseRefMarkNamesToArrays(
        List<String> referenceMarkNames,
        int[] types,
        String[][] bibtexKeys
        ) {
        final int nRefMarks = referenceMarkNames.size();
        assert (types.length == nRefMarks);
        assert (bibtexKeys.length == nRefMarks);
        for (int i = 0; i < nRefMarks; i++) {
            final String name = referenceMarkNames.get(i);
            Optional<ParsedRefMark> op = parseRefMarkName(name);
            if (op.isEmpty()) {
                // We have a problem. We want types[i] and bibtexKeys[i]
                // to correspond to referenceMarkNames.get(i).
                // And do not want null in bibtexKeys (or error code in types)
                // on return.
                throw new IllegalArgumentException(
                    "parseRefMarkNamesToArrays expects parsable referenceMarkNames"
                    );
            }
            ParsedRefMark ov = op.get();
            types[i] = ov.itcType;
            bibtexKeys[i] = ov.citedKeys.toArray(String[]::new);
        }
    }

    /**
     * Extract citation keys from names of referenceMarks in the document.
     *
     * Each citation key is listed only once, in the order of first appearance
     * (in `names`, which itself is in arbitrary order)
     *
     * doc.referenceMarks.names.map(parse).flatten.unique
     */
    private List<String>
    findCitedKeys(DocumentConnection documentConnection)
        throws
        NoSuchElementException,
        WrappedTargetException,
        NoDocumentException {

        List<String> names = getJabRefReferenceMarkNames(documentConnection);

        // assert it supports XTextContent
        XNameAccess xNamedMarks = documentConnection.getReferenceMarks();
        for (String name1 : names) {
            Object bookmark = xNamedMarks.getByName(name1);
            assert (null != DocumentConnection.asTextContent(bookmark));
        }

        // Collect to a flat list while keep only the first appearance.
        List<String> keys = new ArrayList<>();
        for (String name1 : names) {
            List<String> newKeys = parseRefMarkNameToUniqueCitationKeys(name1);
            for (String key : newKeys) {
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    /**
     *  Given the name of a reference mark, get the corresponding
     *  pageInfo text.
     *
     *  @param documentConnection Connection to a document.
     *  @param name Name of the custom property to query.
     *  @return "" for missing or empty pageInfo
     */
    private static String
    getPageInfoForReferenceMarkName(
        DocumentConnection documentConnection,
        String name)
        throws WrappedTargetException,
        UnknownPropertyException {

        Optional<String> pageInfo = documentConnection.getCustomProperty(name);
        if (pageInfo.isEmpty() || pageInfo.get().isEmpty()) {
            return "";
        }
        return pageInfo.get();
    }

    /*
     * At the start of GUI actions we may want to check the state of the document.
     */
    class CitationGroups {
        private final Map<String, Integer> indexByName;
        private final String[] names;
        // After parsing
        private final int[] itcTypes;
        private final String[][] citationKeys;

        // Probably wrong, but currently pageInfo belongs to the group
        private final String[] pageInfo;

        // For custom properties belonging to us, but
        // without a corresponding reference mark.
        // These can be deleted.
        private List<String> pageInfoThrash;

        // After database lookup:
        // private BibEntry[][] entries;
        // private BibDatabase[][] entryDatabases;

        public CitationGroups(DocumentConnection documentConnection)
            throws NoDocumentException {
            // Get the names
            this.names =
                getJabRefReferenceMarkNames(documentConnection)
                .toArray(String[]::new);

            // Fill indexByName
            this.indexByName = new HashMap<>();
            for (int i = 0; i < this.names.length; i++) {
                indexByName.put(names[i], i);
            }
            // collect pageInfo
            this.pageInfo = new String[names.length];
            List<String> jabrefPropertyNames =
                documentConnection.getCustomPropertyNames()
                .stream()
                .filter(OOBibBase::isJabRefReferenceMarkName)
                .collect(Collectors.toList());
            // For each name: either put into place or
            // put into thrash collector.
            this.pageInfoThrash = new ArrayList<>();
            for (String n : jabrefPropertyNames) {
                if (indexByName.containsKey(n)) {
                    int i = indexByName.get(n);
                    pageInfo[i] = n;
                } else {
                    pageInfoThrash.add(n);
                }
            }
            // parse names
            this.itcTypes = new int[names.length];
            this.citationKeys = new String[names.length][];
            for (int i = 0; i < names.length; i++) {
                final String name = names[i];
                Optional<ParsedRefMark> op = parseRefMarkName(name);
                if (op.isEmpty()) {
                    // We have a problem. We want types[i] and bibtexKeys[i]
                    // to correspond to referenceMarkNames.get(i).
                    // And do not want null in bibtexKeys (or error code in types)
                    // on return.
                    throw new IllegalArgumentException(
                        "citationGroups: found unparsable referenceMarkName"
                        );
                }
                ParsedRefMark ov = op.get();
                itcTypes[i] = ov.itcType;
                citationKeys[i] = ov.citedKeys.toArray(String[]::new);
            }
            // Now we have almost every information from the document about citations.
            // What is left out: the ranges controlled by the reference marks.
            // But (I guess) those change too easily, so we only ask when actually needed.
        }

        /*
         * ranges controlled by citation groups should not overlap with each other.
         *
         *
         */
        public XTextRange
        getReferenceMarkRangeOrNull(DocumentConnection documentConnection, int i)
            throws
            NoDocumentException,
            WrappedTargetException {
            return documentConnection.getReferenceMarkRangeOrNull(names[i]);
        }

        /**
         * Assumes a.getText() == b.getText(), and both belong to documentConnection.xText
         *
         * Note: OpenOffice XTextRangeCompare and java use different
         *       (opposite) conventions. The "java" prefix in the
         *       functions name is intended to emphasize, that the
         *       value returned is adjusted to use java conventions
         *       (by multiplying the result with (-1)).
         *
         * @return -1 for (a &lt; b), 1 for (a &gt; b); 0 for equal.
         *
         * @throws RuntimeException if a and b are not comparable
         *
         */
        /*
        public int
        javaCompareRegionStarts(RangeForOverlapCheck a,
                                RangeForOverlapCheck b) {
            //
            // XTextRange cannot be compared, only == or != is available.
            //
            // XTextRangeCompare: compares the positions of two TextRanges within a Text.
            // Only TextRange instances within the same Text can be compared.
            // And XTextRangeCompare must be obtained from their Text.

            XTextRange ra = a.range;
            XTextRange rb = b.range;
            if (ra.getText() != rb.getText()) {
                throw new RuntimeException(
                    String.format(
                        "OOBibBase.CitationGroups.javaCompareRegionStarts:"
                        + " incomparable regions: %s %s",
                        a.format(),
                        b.format())
                    );
            }

            try {
                return DocumentConnection.javaCompareRegionStarts(ra, rb);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(
                    String.format(
                        "OOBibBase.CitationGroups.javaCompareRegionStarts:"
                        + " caught IllegalArgumentException: %s %s",
                        a.format(),
                        b.format()
                        )
                    );
            }
        }
        */
        /**
         *
         */
        /*
        public int
        javaCompareRegionEndToStart(RangeForOverlapCheck a,
                                    RangeForOverlapCheck b) {

            XTextRange ra = a.range;
            XTextRange rb = b.range;
            if (ra.getText() != rb.getText()) {
                throw new RuntimeException(
                    String.format(
                        "OOBibBase.CitationGroups.javaCompareRegionEndToStart:"
                        + " incomparable regions: %s %s",
                        a.format(),
                        b.format())
                    );
            }

            try {
                return DocumentConnection.javaCompareRegionStarts(ra.getEnd(), rb.getStart());
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(
                    String.format(
                        "OOBibBase.CitationGroups.javaCompareRegionEndToStart:"
                        + " caught IllegalArgumentException: %s %s",
                        a.format(),
                        b.format()
                        )
                    );
            }
        }
        */

        /**
         * @return A RangeForOverlapCheck for each citation group.
         */
        List<RangeForOverlapCheck>
        citationRanges(DocumentConnection documentConnection)
            throws
            NoDocumentException,
            WrappedTargetException {

            List<RangeForOverlapCheck> xs = new ArrayList<>(names.length);
            for (int i = 0; i < names.length; i++) {
                XTextRange r = this.getReferenceMarkRangeOrNull(documentConnection, i);
                xs.add(new RangeForOverlapCheck(
                           r, i,
                           RangeForOverlapCheck.REFERENCE_MARK_KIND,
                           names[i]
                           ));
            }
            return xs;
        }

        /**
         * @return A range for each footnote mark where the footnote
         *         contains at least one citation group.
         *
         *  Purpose: We do not want markers of footnotes containing
         *  reference marks to overlap with reference
         *  marks. Overwriting these footnote marks might kill our
         *  reference marks in the footnote.
         *
         */
        List<RangeForOverlapCheck>
        footnoteMarkRanges(DocumentConnection documentConnection)
            throws
            NoDocumentException,
            WrappedTargetException {

            // Avoid inserting the same mark twice.
            // Could use RangeSet if we had that.
            RangeKeyedMap<Boolean> seen = new RangeKeyedMap<>();

            List<RangeForOverlapCheck> xs = new ArrayList<>();

            for (int i = 0; i < names.length; i++) {
                XTextRange r = this.getReferenceMarkRangeOrNull(documentConnection, i);

                XTextRange footnoteMarkRange =
                    DocumentConnection.getFootnoteMarkRangeOrNull(r);

                if (footnoteMarkRange != null) {
                    // Problem: quadratic complexity. Each new footnoteMarkRange
                    // is compared to all we have seen before.
                    boolean seenContains = seen.containsKey( footnoteMarkRange );
                    if (!seenContains) {
                        seen.put(footnoteMarkRange, true);
                    xs.add(new RangeForOverlapCheck(
                               footnoteMarkRange,
                               i, // index of citation group
                               RangeForOverlapCheck.FOOTNOTE_MARK_KIND,
                               "FootnoteMark for " + names[i]
                               ));
                    }
                }
            }
            return xs;
        }

    } // class citationGroups



    /**
     * @param requireSeparation Report range pairs that only share a boundary.
     * @param atMost Limit number of overlaps reported (0 for no limit)
     */
    public void
    checkRangeOverlaps(
        CitationGroups cgs,
        DocumentConnection documentConnection,
        boolean requireSeparation,
        int atMost
        )
        throws
        NoDocumentException,
        WrappedTargetException,
        JabRefException {

        final boolean debugPartitions = false;

        List<RangeForOverlapCheck> xs = cgs.citationRanges(documentConnection);
        xs.addAll(cgs.footnoteMarkRanges(documentConnection));

        RangeKeyedMapList<RangeForOverlapCheck> xall = new RangeKeyedMapList<>();
        for (RangeForOverlapCheck x : xs) {
            XTextRange key = x.range;
            xall.add(key,x);
        }

        List<RangeKeyedMapList<RangeForOverlapCheck>.RangeOverlap> ovs =
            xall.findOverlappingRanges(atMost, requireSeparation);

        //checkSortedPartitionForOverlap(requireSeparation, oxs);
        if (ovs.size() > 0) {
            String msg = "";
            for (RangeKeyedMapList<RangeForOverlapCheck>.RangeOverlap e : ovs) {
                String l =
                    (": "
                     + (e.vs.stream()
                        .map(v -> String.format("'%s'", v.format()))
                        .collect(Collectors.joining(", ")))
                     + "\n");

                switch (e.kind) {
                case EQUAL_RANGE:
                    msg = msg + Localization.lang("Found identical ranges") + l;
                    break;
                case OVERLAP:
                    msg = msg + Localization.lang("Found overlapping ranges") + l;
                    break;
                case TOUCH:
                    msg = msg + Localization.lang("Found touching ranges") + l;
                    break;
                }
            }
            throw new JabRefException(
                "Found overlapping or touching ranges",
                msg
                );
        }
    }

    /**
     * GUI: Get a list of CitationEntry objects corresponding to citations
     * in the document.
     *
     * Called from: ManageCitationsDialogViewModel constructor.
     *
     * @return A list with entries corresponding to citations in the
     *         text, in arbitrary order (same order as from
     *         getJabRefReferenceMarkNames).
     *
     *               Note: visual or alphabetic order could be more
     *               manageable for the user. We could provide these
     *               here, but switching between them needs change on
     *               GUI (adding a toggle or selector).
     *
     *               Note: CitationEntry implements Comparable, where
     *                     compareTo() and equals() are based on refMarkName.
     *                     The order used in the "Manage citations" dialog
     *                     does not seem to use that.
     *
     *                     The 1st is labeled "Citation" (show citation in bold,
     *                     and some context around it).
     *
     *                     The columns can be sorted by clicking on the column title.
     *                     For the "Citation" column, the sorting is based on the content,
     *                     (the context before the citation), not on the citation itself.
     *
     *                     In the "Extra information ..." column some visual indication
     *                     of the editable part could be helpful.
     *
     *         Wish: selecting an entry (or a button in the line) in
     *               the GUI could move the cursor in the document to
     *               the entry.
     */
    public List<CitationEntry>
    getCitationEntries()
        throws
        UnknownPropertyException,
        WrappedTargetException,
        NoDocumentException {

        DocumentConnection documentConnection = this.getDocumentConnectionOrThrow();

        List<String> names = this.getJabRefReferenceMarkNames(documentConnection);

        List<CitationEntry> citations = new ArrayList<>(names.size());
        for (String name : names) {
            CitationEntry entry =
                new CitationEntry(
                    name,
                    this.getCitationContext(documentConnection, name, 30, 30, true),
                    documentConnection.getCustomProperty(name)
                    );
            citations.add(entry);
        }
        return citations;
    }

    /**
     *  Get the text belonging to refMarkName with up to
     *  charBefore and charAfter characters of context.
     *
     *  The actual context may be smaller than requested.
     *
     *  @param refMarkName Name of a reference mark.
     *  @param charBefore Number of characters requested.
     *  @param charAfter  Number of characters requested.
     *  @param htmlMarkup If true, the text belonging to the
     *  reference mark is surrounded by bold html tag.
     */
    public String
    getCitationContext(
        DocumentConnection documentConnection,
        String refMarkName,
        int charBefore,
        int charAfter,
        boolean htmlMarkup
        )
        throws
        WrappedTargetException,
        NoDocumentException {

        XNameAccess nameAccess = documentConnection.getReferenceMarks();
        XTextContent mark =
            DocumentConnection.nameAccessGetTextContentByNameOrNull(nameAccess, refMarkName);
        if (null == mark) {
            LOGGER.warn(String.format(
                    "OOBibBase.getCitationContext:"
                    + " lost reference mark: '%s'",
                    refMarkName
            ));
            return String.format("(Could not retrieve context for %s)", refMarkName);
        }
        XTextCursor cursor = DocumentConnection.getTextCursorOfTextContent(mark);

        String citPart = cursor.getString();

        // extend cursor range left
        int flex = 8;
        for (int i = 0; i < charBefore; i++) {
            try {
                cursor.goLeft((short) 1, true);
                // If we are close to charBefore and see a space,
                // then cut here. Might avoid cutting a word in half.
                if ((i >= (charBefore - flex))
                    && Character.isWhitespace(cursor.getString().charAt(0))) {
                    break;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.warn("Problem going left", ex);
            }
        }

        int lengthWithBefore = cursor.getString().length();
        int addedBefore = lengthWithBefore - citPart.length();

        cursor.collapseToStart();
        for (int i = 0; i < (charAfter + lengthWithBefore); i++) {
            try {
                cursor.goRight((short) 1, true);
                if (i >= ((charAfter + lengthWithBefore) - flex)) {
                    String strNow = cursor.getString();
                    if (Character.isWhitespace(strNow.charAt(strNow.length() - 1))) {
                        break;
                    }
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.warn("Problem going right", ex);
            }
        }

        String result = cursor.getString();
        if (htmlMarkup) {
            result =
                result.substring(0, addedBefore)
                + "<b>" + citPart + "</b>"
                + result.substring(lengthWithBefore);
        }
        return result.trim();
    }

    /**
     * Apply editable parts of citationEntries to the document: store
     * pageInfo.
     *
     * Does not change presentation.
     *
     * Note: we use no undo context here, because only
     *       documentConnection.setCustomProperty() is called,
     *       and Undo in LO will not undo that.
     *
     * GUI: "Manage citations" dialog "OK" button.
     * Called from: ManageCitationsDialogViewModel.storeSettings
     *
     * <p>
     * Currently the only editable part is pageInfo.
     * <p>
     * Since the only call to applyCitationEntries() only changes
     * pageInfo w.r.t those returned by getCitationEntries(), we can
     * do with the following restrictions:
     * <ul>
     * <li> Missing pageInfo means no action.</li>
     * <li> Missing CitationEntry means no action (no attempt to remove
     *      citation from the text).</li>
     * </ul>
     */
    public void
    applyCitationEntries(
        List<CitationEntry> citationEntries)
        throws
        UnknownPropertyException,
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException,
        IllegalArgumentException,
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        for (CitationEntry entry : citationEntries) {
            Optional<String> pageInfo = entry.getPageInfo();
            if (pageInfo.isPresent()) {
                documentConnection.setCustomProperty(
                    entry.getRefMarkName(),
                    pageInfo.get());
            }
        }
    }

    /* *************************************
     *
     *         Look up in databases
     *
     * *************************************/

    /**
     * The collection of data returned by findCitedKeys.
     */
    private static class FindCitedEntriesResult {
        /**
         *  entries : LinkedHashMap with iteration order as the
         *            citedKeys parameter of findCitedEntries
         */
        Map<BibEntry, BibDatabase> entries;

        /**
         * citeKeyToBibEntry : HashMap (no order)
         */
        Map<String, BibEntry> citeKeyToBibEntry;

        FindCitedEntriesResult(
            Map<BibEntry, BibDatabase> entries,
            Map<String, BibEntry> citeKeyToBibEntry
            ) {
            this.entries = entries;
            this.citeKeyToBibEntry = citeKeyToBibEntry;
        }
    }

    /**
     * Look up a single citation key in a list of databases.
     *
     * @param key Citation key to look up.
     * @param databases Key is looked up in these, in this order.
     * @return The BibEntry at the first match, or Optional.empty().
     */
    Optional<BibEntry>
    lookupEntryInDatabases(String key, List<BibDatabase> databases) {
        for (BibDatabase database : databases) {
            Optional<BibEntry> entry = database.getEntryByCitationKey(key);
            if (entry.isPresent()) {
                return entry;
            }
        }
        return Optional.empty();
    }

    /**
     *  Look up a list of citation keys in a list of databases.
     *
     * @param keys Citation keys to look up.
     * @param databases Keys are looked up in these, in this order.
     * @return The BibEntry objects found.
     *
     * The order of keys is kept in the result, but unresolved keys
     * have no representation in the result, so result.get(i) does not
     * necessarily belong to keys.get(i)
     *
     */
    List<BibEntry>
    lookupEntriesInDatabasesSkipMissing(
        List<String> keys,
        List<BibDatabase> databases
        ) {
        List<BibEntry> entries = new ArrayList<>();
        for (String key : keys) {
            lookupEntryInDatabases(key, databases).ifPresent(entries::add);
        }
        return entries;
    }

    /**
     * @return A FindCitedEntriesResult containing
     *
     *    entries: A LinkedHashMap, from BibEntry to BibDatabase with
     *             iteration order as the citedKeys parameter.
     *
     *             Stores: in which database was the entry found.
     *
     *    citeKeyToBibEntry:
     *            A HashMap from citation key to BibEntry.
     *            Stores: result of lookup.
     *
     *    For citation keys not found, a new
     *    UndefinedBibtexEntry(citedKey) is created and added to both
     *    maps, with a null BibDatabase.
     *
     *    How is the result used?
     *
     *    entries : Allows to recover the list of keys in original
     *              order, and finding the corresponding databases.
     *
     *              Well, it does, but not by a simple
     *              entry.getCitationKey(), because
     *              UndefinedBibtexEntry.getCitationKey() returns
     *              Optional.empty().
     *
     *              For UndefinedBibtexEntry,
     *              UndefinedBibtexEntry.getKey() returns the
     *              original key we stored.
     *
     *    citeKeyToBibEntry:
     *
     *        Caches the result of lookup performed here, with "not
     *        found" encoded as an instance of UndefinedBibtexEntry.
     *
     */
    private FindCitedEntriesResult
    findCitedEntries(
        List<String> citedKeys,
        List<BibDatabase> databases
    ) {
        Map<String, BibEntry> citeKeyToBibEntry = new HashMap<>();

        // LinkedHashMap, iteration order as in citedKeys
        Map<BibEntry, BibDatabase> entries = new LinkedHashMap<>();
        for (String citedKey : citedKeys) {
            boolean found = false;
            for (BibDatabase database : databases) {
                Optional<BibEntry> entry = database.getEntryByCitationKey(citedKey);
                if (entry.isPresent()) {
                    entries.put(entry.get(), database);
                    citeKeyToBibEntry.put(citedKey, entry.get());
                    found = true;
                    break;
                }
            }

            if (!found) {
                BibEntry x = new UndefinedBibtexEntry(citedKey);
                entries.put(x, null);
                citeKeyToBibEntry.put(citedKey, x);
            }
        }
        return new FindCitedEntriesResult(entries, citeKeyToBibEntry);
    }

    static String
    recoverCitationKeyFromPossiblyUndefinedBibEntry(BibEntry entry) {
        if (entry instanceof UndefinedBibtexEntry) {
            return ((UndefinedBibtexEntry) entry).getKey();
        } else {
            Optional<String> optKey = entry.getCitationKey();
            return optKey.get(); // may throw, but should not happen
        }
    }

    /**
     *  @return The list of citation keys from `instanceof
     *          UndefinedBibtexEntry` elements of (keys of) `entries`.
     *
     *  Intent: Get list of unresolved citation keys.
     */
    private static List<String>
    unresolvedKeysFromEntries(Map<BibEntry, BibDatabase> entries) {
        // Collect and return unresolved citation keys.
        List<String> unresolvedKeys = new ArrayList<>();
        for (BibEntry entry : entries.keySet()) {
            if (entry instanceof UndefinedBibtexEntry) {
                String key = ((UndefinedBibtexEntry) entry).getKey();
                if (!unresolvedKeys.contains(key)) {
                    unresolvedKeys.add(key);
                }
            }
        }
        return unresolvedKeys;
    }

    /* ***************************************
     *
     *     Local order: Presentation order within citation groups
     *
     * **************************************/

    /**
     *  The comparator used to sort within a group of merged
     *  citations.
     *
     *  The term used here is "multicite". The option controlling the
     *  order is "MultiCiteChronological" in style files.
     */
    private Comparator<BibEntry>
    comparatorForMulticite(OOBibStyle style) {
        if (style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL)) {
            return this.yearAuthorTitleComparator;
        } else {
            return this.entryComparator;
        }
    }

    /**
     * Sort entries within a group of merged citations.
     *
     * Note: the sort is in-place, modifies the argument.
     */
    private void
    sortBibEntryListForMulticite(List<BibEntry> entries,
                                 OOBibStyle style) {
        if (entries.size() <= 1) {
            return;
        }
        entries.sort(comparatorForMulticite(style));
    }

    /**
     *  Look up citation keys from a map caching earlier look up, sort result within
     *  each reference mark.
     *
     * @param bibtexKeys Citation keys, bibtexKeys[i][j] is for the ith reference mark.
     * @param citeKeyToBibEntry Cached lookup from keys to entries.
     */
    private BibEntry[][]
    getBibEntriesSortedWithinReferenceMarks(
        String[][] bibtexKeys,
        Map<String, BibEntry> citeKeyToBibEntry,
        OOBibStyle style
        ) {
        return
            Arrays.stream(bibtexKeys)
            .map(bibtexKeysOfAReferenceMark ->
                 Arrays.stream(bibtexKeysOfAReferenceMark)
                 .map(citeKeyToBibEntry::get)
                 .sorted(comparatorForMulticite(style)) // sort within referenceMark
                 .toArray(BibEntry[]::new)
                )
            .toArray(BibEntry[][]::new);
    }

    /* ***************************************
     *
     *     Global order: by first appearance or by bibliography order
     *
     * **************************************/

    /* bibliography order */

    /**
     * @return Citation keys from `entries`, ordered as in the bibliography.
     */
    private List<String>
    citationKeysOrNullInBibliographyOrderFromEntries(
        Map<BibEntry, BibDatabase> entries
        ) {

        // Sort entries to order in bibliography
        // Belongs to global order.
        Map<BibEntry, BibDatabase> sortedEntries =
            sortEntriesByComparator(entries, entryComparator);

        // Citation keys, in the same order as sortedEntries
        return
            sortedEntries.keySet().stream()
            .map(
                entry ->
                entry.getCitationKey()
                // entries came from looking up by citation key,
                // so Optional.empty() is only possible here for UndefinedBibtexEntry.
                .orElse(null)
                )
            .collect(Collectors.toList());
    }

    /**
     *  Return a TreeMap(entryComparator) copy of entries.
     *
     *  For sorting the bibliography.
     */
    SortedMap<BibEntry, BibDatabase>
    sortEntriesByComparator(
        Map<BibEntry, BibDatabase> entries,
        Comparator<BibEntry> entryComparator
        ) {
        SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
        for (Map.Entry<BibEntry, BibDatabase> kv : entries.entrySet()) {
            newMap.put(
                kv.getKey(),
                kv.getValue());
        }
        return newMap;
    }

    /* first appearance order, based on visual order */

    /**
     *  Given a location, return its position:
     *  coordinates relative to the top left position
     *   of the first page of the document.
     *
     * @param range  Location.
     * @param cursor To get the position, we need az XTextViewCursor.
     *               It will be moved to the range.
     */
    private static Point
    findPositionOfTextRange(XTextRange range, XTextViewCursor cursor) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
    }

    /**
     * A reference mark name paired with its visual position.
     *
     * Comparison is based on (Y,X): vertical compared first, horizontal second.
     *
     * Used for sorting reference marks by their visual positions.
     *
     * Note: for text layouts with two or more columns, this gives the wrong order.
     *
     */
    private static class ComparableMark implements Comparable<ComparableMark> {

        private final String name;
        private final Point position;

        public ComparableMark(String name, Point position) {
            this.name = name;
            this.position = position;
        }

        @Override
        public int compareTo(ComparableMark other) {
            if (position.Y == other.position.Y) {
                return position.X - other.position.X;
            } else {
                return position.Y - other.position.Y;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }

            if (o instanceof ComparableMark) {
                ComparableMark other = (ComparableMark) o;
                return ((this.position.X == other.position.X)
                        && (this.position.Y == other.position.Y)
                        && Objects.equals(this.name, other.name));
            }
            return false;
        }

        public String getName() {
            return name;
        }

        @Override
        public int hashCode() {
            return Objects.hash(position, name);
        }
    }

    /**
     *  Read reference mark names from the document, keep only those
     *  with JabRef naming convention, get their visual positions,
     *
     *  @return JabRef reference mark names sorted by these positions.
     *
     *  Limitation: for two column layout visual (top-down,
     *        left-right) order does not match the expected (textual)
     *        order.
     *
     */
    private List<String>
    getJabRefReferenceMarkNamesSortedByPosition(
        DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoDocumentException {

        if (documentConnection.hasControllersLocked()) {
            LOGGER.warn(
                "getJabRefReferenceMarkNamesSortedByPosition:"
                + " with ControllersLocked, viewCursor.gotoRange"
                + " is probably useless"
                );
        }

        List<String> names = getJabRefReferenceMarkNames(documentConnection);

        // find coordinates
        List<Point> positions = new ArrayList<>(names.size());

        XNameAccess nameAccess = documentConnection.getReferenceMarks();
        XTextViewCursor viewCursor = documentConnection.getViewCursor();
        // initialPos: to be restored before return
        XTextRange initialPos = viewCursor.getStart();
        for (String name : names) {

            XTextContent textContent =
                DocumentConnection.nameAccessGetTextContentByNameOrNull(nameAccess, name);
            // unoQI(XTextContent.class, nameAccess.getByName(name));
            if (null == textContent) {
                LOGGER.warn(String.format(
                        "OOBibBase.getJabRefReferenceMarkNames:"
                        + " could not retrieve reference mark: '%s'",
                        name
                ));
                continue; // just skip it
            }

            XTextRange range = textContent.getAnchor();

            // Adjust range if we are inside a footnote:
            if (unoQI(XFootnote.class, range.getText()) != null) {
                // Find the linking footnote marker:
                XFootnote footer = unoQI(XFootnote.class, range.getText());
                // The footnote's anchor gives the correct position in the text:
                range = footer.getAnchor();
            }
            positions.add(findPositionOfTextRange(range, viewCursor));
        }
        // restore cursor position
        viewCursor.gotoRange(initialPos, false);

        // order by position
        Set<ComparableMark> set = new TreeSet<>();
        for (int i = 0; i < positions.size(); i++) {
            set.add(new ComparableMark(names.get(i), positions.get(i)));
        }

        // collect referenceMarkNames in order
        List<String> result = new ArrayList<>(set.size());
        for (ComparableMark mark : set) {
            result.add(mark.getName());
        }

        return result;
    }

    /**
     *  Return bibliography entries sorted according to the order of
     *  first appearance in referenceMarkNames.
     *
     * @param referenceMarkNames Names of reference marks.
     * @param citeKeyToBibEntry  Helps to find the entries
     * @return LinkedHashMap from BibEntry to BibDatabase with
     *         iteration order as first appearance in referenceMarkNames.
     *
     * Note: Within citation group (a reference mark) the order is
     *       as appears there.
     *
     */
    private Map<BibEntry, BibDatabase>
    sortEntriesByRefMarkNames(
        List<String> referenceMarkNames,
        Map<String, BibEntry> citeKeyToBibEntry,
        Map<BibEntry, BibDatabase> entries
        ) {
        // LinkedHashMap: iteration order is insertion-order, not
        // affected if a key is re-inserted.
        Map<BibEntry, BibDatabase> newList = new LinkedHashMap<>();

        for (String name : referenceMarkNames) {
            Optional<ParsedRefMark> op = parseRefMarkName(name);
            if (op.isEmpty()) {
                continue;
            }

            List<String> keys = op.get().citedKeys;
            // no need to look in the database again
            for (String key : keys) {
                BibEntry origEntry = citeKeyToBibEntry.get(key);
                if (origEntry != null) {
                    if (!newList.containsKey(origEntry)) {
                        BibDatabase database = entries.get(origEntry);
                        newList.put(origEntry, database);
                    }
                } else {
                    LOGGER.info("Citation key not found: '" + key + "'");
                    LOGGER.info("Problem with reference mark: '" + name + "'");
                    newList.put(new UndefinedBibtexEntry(key), null);
                }
            }
        }
        return newList;
    }

    /* ***************************************
     *
     *     Make them unique: uniqueLetters or numbers
     *
     * **************************************/

    private String[][]
    normalizedCitationMarkersForNormalStyle(
        BibEntry[][] cEntriesForAll,
        Map<BibEntry, BibDatabase> entries,
        OOBibStyle style
        ) {

        final int nRefMarks = cEntriesForAll.length;

        String[][] normCitMarkers = new String[nRefMarks][];
        for (int i = 0; i < nRefMarks; i++) {
            BibEntry[] cEntries = cEntriesForAll[i];
            // We need "normalized" (in parenthesis) markers
            // for uniqueness checking purposes:
            normCitMarkers[i] =
                Arrays.stream(cEntries)
                .map(ce ->
                     style.getCitationMarker(
                         Collections.singletonList(undefinedBibEntryToNull(ce)),
                         entries,
                         true,
                         null,
                         new int[] {-1} // no limit on authors
                         )
                    )
                .toArray(String[]::new);
        }
        return normCitMarkers;
    }

    /**
     * Given bibtexKeys for each reference mark and the corresponding
     * normalized citation markers for each, fills uniqueLetters.
     *
     * We expect to see data for all JabRef reference marks here, and
     * clear uniqueLetters before filling.
     *
     * On return: uniqueLetters.get(bibtexkey) provides letter to be
     * added after the year (null for none).
     *
     * Note: bibtexKeys[i][j] may be null (from UndefinedBibtexEntry)
     */
    Map<String, String>
    createUniqueLetters(
        String[][] bibtexKeys,
        String[][] normCitMarkers
        ) {

        final int nRefMarks = bibtexKeys.length;
        assert nRefMarks == normCitMarkers.length;

        // refKeys: (normCitMarker) to (list of citation keys sharing it).
        //          The entries in the lists are ordered as in
        //          normCitMarkers[i][j]
        Map<String, List<String>> refKeys = new HashMap<>();

        for (int i = 0; i < nRefMarks; i++) {
            // Compare normalized markers, since the actual
            // markers can be different.
            String[] markers = normCitMarkers[i];
            for (int j = 0; j < markers.length; j++) {
                String marker = markers[j];
                String currentKey = bibtexKeys[i][j];
                // containsKey(null) is OK, contains(null) is OK.
                if (refKeys.containsKey(marker)) {
                    // Ok, we have seen this exact marker before.
                    if (!refKeys.get(marker).contains(currentKey)) {
                        // ... but not for this entry.
                        refKeys.get(marker).add(currentKey);
                    }
                } else {
                    // add as new entry
                    List<String> l = new ArrayList<>(1);
                    l.add(currentKey);
                    refKeys.put(marker, l);
                }
            }
        }

        // uniqueLetters.clear();
        Map<String, String> uniqueLetters = new HashMap<>();

        // Go through the collected lists and see where we need to
        // add unique letters to the year.
        for (Map.Entry<String, List<String>> stringListEntry : refKeys.entrySet()) {
            List<String> clashingKeys = stringListEntry.getValue();
            if (clashingKeys.size() > 1) {
                // This marker appears for more than one unique entry:
                int nextUniqueLetter = 'a';
                for (String key : clashingKeys) {
                    // Update the map of uniqueLetters for the
                    // benefit of both the following generation of
                    // new citation markers, and for the method
                    // that builds the bibliography:
                    uniqueLetters.put(key, String.valueOf((char) nextUniqueLetter));
                    nextUniqueLetter++;
                }
            }
        }
        return uniqueLetters;
    }

    /**
     * Number source for (1-based) numbering of citations.
     */
    private static class CitationNumberingState {
        /**
         * numbers : Remembers keys we have seen
         *           and what number did they receive.
         */
        private final Map<String, Integer> numbers;

        /**
         * The highest number we ever allocated.
         */
        private int lastNum;

        CitationNumberingState() {
            this.numbers = new HashMap<>();
            this.lastNum = 0;
        }

        /**
         * The first call returns 1.
         */
        public int getOrAllocateNumber(String key) {
            int result;
            if (numbers.containsKey(key)) {
                // Already seen
                result = numbers.get(key);
            } else {
                // First time to see. Allocate number.
                lastNum++;
                numbers.put(key, lastNum);
                result = lastNum;
            }
            return result;
        }
    }

    /**
     * Gets number for a BibEntry. (-1) for UndefinedBibtexEntry
     *
     * BibEntry.getCitationKey() must not be Optional.empty(), except
     * for UndefinedBibtexEntry.
     *
     */
    private static int
    numberPossiblyUndefinedBibEntry(
        BibEntry ce,
        CitationNumberingState cns
        ) {

        if (ce instanceof UndefinedBibtexEntry) {
            return (-1);
        }

        String key = (ce.getCitationKey()
                      .orElseThrow(IllegalArgumentException::new));

        return cns.getOrAllocateNumber(key);
    }

    /**
     * Resolve the citation key from a citation reference marker name,
     * and look up the index of the key in a list of keys.
     *
     * @param keysCitedHere   The citation keys needing indices.
     * @param orderedCiteKeys A List of citation keys representing the
     *                        entries in the bibliography.
     *
     * @return The (1-based) indices of the cited keys,
     *         or (-1) if a key is not found.
     */
    private static List<Integer>
    findCitedEntryIndices(
        List<String> keysCitedHere,
        List<String> orderedCiteKeys
        ) {
        List<Integer> result = new ArrayList<>(keysCitedHere.size());
        for (String key : keysCitedHere) {
            int ind = orderedCiteKeys.indexOf(key);
            result.add(
                ind == -1
                ? -1
                : 1 + ind // 1-based
                );
        }
        return result;
    }

    /* ***************************************
     *
     *     Calculate presentation of citation groups
     *     (create citMarkers)
     *
     * **************************************/

    /**
     * Given the withText and inParenthesis options,
     * return the corresponding itcType.
     *
     * @param withText False means invisible citation (no text).
     * @param inParenthesis True means "(Au and Thor 2000)".
     *                      False means "Au and Thor (2000)".
     */
    private static int
    citationTypeFromOptions(boolean withText, boolean inParenthesis) {
        if (!withText) {
            return OOBibBase.INVISIBLE_CIT;
        }
        return (inParenthesis
                ? OOBibBase.AUTHORYEAR_PAR
                : OOBibBase.AUTHORYEAR_INTEXT);
    }

    /**
     * @return Null if cEntry is an UndefinedBibtexEntry,
     *         otherwise return cEntry itself.
     */
    private static BibEntry
    undefinedBibEntryToNull(BibEntry cEntry) {
        if (cEntry instanceof UndefinedBibtexEntry) {
            return null;
        }
        return cEntry;
    }

    /**
     *   result[i][j] = cEntriesForAll[i][j].getCitationKey()
     */
    private String[][]
    mapBibEntriesToCitationKeysOrNullForAll(BibEntry[][] cEntriesForAll) {
        return
            Arrays.stream(cEntriesForAll)
            .map(cEntries ->
                 Arrays.stream(cEntries)
                 .map(ce -> ce.getCitationKey().orElse(null))
                 .toArray(String[]::new)
                )
            .toArray(String[][]::new);
    }

    /**
     * Checks that every element of `keys` can be found in `citeKeyToBibEntry`.
     *
     * Collects the missing keys, and if there is any, throws
     * BibEntryNotFoundException (currently mentioning only the first
     * missing key).
     *
     * @param keys An array of citation keys, we expect to appear as
     *             keys in citeKeyToBibEntry.
     *
     * @param citeKeyToBibEntry Should map each key in keys to a BibEntry.
     * @param referenceMarkName The reference mark these keys belong to.
     *                          Mentioned in the exception.
     */
    private static void
    assertKeysInCiteKeyToBibEntry(
        String[] keys, // citeKeys
        Map<String, BibEntry> citeKeyToBibEntry,
        String referenceMarkName, // for reporting
        String where
    )
        throws BibEntryNotFoundException {

        // check keys
        List<String> unresolvedKeys =
            Arrays.stream(keys)
            .filter(key -> null == citeKeyToBibEntry.get(key))
            .collect(Collectors.toList());

        for (String key : unresolvedKeys) {
            LOGGER.info("assertKeysInCiteKeyToBibEntry: Citation key not found: '" + key + '\'');
            LOGGER.info("Problem with reference mark: '" + referenceMarkName + "' " + where);
            String msg =
                Localization.lang(
                    "Could not resolve BibTeX entry"
                    + " for citation marker '%0'.",
                    referenceMarkName);
            throw new BibEntryNotFoundException(referenceMarkName, msg);
        }
    }

    /**
     * For each reference mark name: check the corresponding element of
     * bibtexKeys with assertKeysInCiteKeyToBibEntry.
     */
    private static void
    assertAllKeysInCiteKeyToBibEntry(
        List<String> referenceMarkNames,
        String[][] bibtexKeys,
        Map<String, BibEntry> citeKeyToBibEntry,
        String where
        )
        throws BibEntryNotFoundException {

        final int nRefMarks = referenceMarkNames.size();
        assert (nRefMarks == bibtexKeys.length);

        for (int i = 0; i < nRefMarks; i++) {
            assertKeysInCiteKeyToBibEntry(
                bibtexKeys[i],
                citeKeyToBibEntry,
                referenceMarkNames.get(i),
                where
            );
        }
    }

    /**
     *  Produce citation markers for the case when the citation
     *  markers are the citation keys themselves, separated by commas.
     */
    private static String[]
    produceCitationMarkersForIsCitationKeyCiteMarkers(
        List<String> referenceMarkNames,
        String[][] bibtexKeys,
        Map<String, BibEntry> citeKeyToBibEntry,
        OOBibStyle style
        )
        throws BibEntryNotFoundException {

        assert style.isCitationKeyCiteMarkers();

        final int nRefMarks = referenceMarkNames.size();

        assert nRefMarks == bibtexKeys.length;
        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeys, citeKeyToBibEntry,
                                         "produceCitationMarkersForIsCitationKeyCiteMarkers");

        String[] citMarkers = new String[nRefMarks];
        for (int i = 0; i < nRefMarks; i++) {
            citMarkers[i] =
                Arrays.stream(bibtexKeys[i])
                .map(citeKeyToBibEntry::get)
                .map(c -> c.getCitationKey().orElse(""))
                .collect(Collectors.joining(","));
        }
        return citMarkers;
    }

    /**
     * Produce citation markers for the case of numbered citations
     * with bibliography sorted by first appearance in the text.
     *
     * @param referenceMarkNames Names of reference marks.
     *
     * @param bibtexKeys Expects bibtexKeys[i] to correspond to
     *                   referenceMarkNames.get(i)
     *
     * @param citeKeyToBibEntry Look up BibEntry by bibtexKey.
     *                          Must contain all bibtexKeys,
     *                          but may map to UndefinedBibtexEntry.
     *
     * @return Numbered citation markers for bibtexKeys.
     *         Numbering is according to first encounter
     *         in bibtexKeys[i][j]
     *
     */
    private static String[]
    produceCitationMarkersForIsNumberEntriesIsSortByPosition(
        List<String> referenceMarkNames,
        String[][] bibtexKeys,
        Map<String, BibEntry> citeKeyToBibEntry,
        OOBibStyle style
        )
        throws BibEntryNotFoundException {

        assert style.isNumberEntries();
        assert style.isSortByPosition();

        final int nRefMarks = referenceMarkNames.size();
        assert (nRefMarks == bibtexKeys.length);
        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeys, citeKeyToBibEntry,
                                         "produceCitationMarkersForIsNumberEntriesIsSortByPosition");

        String[] citMarkers = new String[nRefMarks];

        CitationNumberingState cns = new CitationNumberingState();

        final int minGroupingCount =
            style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);

        for (int i = 0; i < referenceMarkNames.size(); i++) {

            // numbers: Numbers for cited sources. (-1) for UndefinedBibtexEntry.
            List<Integer> numbers =
                Arrays.stream(bibtexKeys[i])
                .map(citeKeyToBibEntry::get)
                .map(ce -> numberPossiblyUndefinedBibEntry(ce, cns))
                .collect(Collectors.toList());

            citMarkers[i] =
                style.getNumCitationMarker(numbers, minGroupingCount, false);
        }
        return citMarkers;
    }

    /**
     * Produce citation markers for the case of numbered citations
     * when the bibliography is not sorted by position.
     */
    private String[]
    produceCitationMarkersForIsNumberEntriesNotSortByPosition(
        List<String> referenceMarkNames,
        String[][] bibtexKeys,
        Map<BibEntry, BibDatabase> entries,
        OOBibStyle style
        ) {
        assert style.isNumberEntries();
        assert !style.isSortByPosition();

        final int nRefMarks = referenceMarkNames.size();
        assert (nRefMarks == bibtexKeys.length);
        String[] citMarkers = new String[nRefMarks];

        List<String> sortedCited =
            citationKeysOrNullInBibliographyOrderFromEntries(entries);

        final int minGroupingCount =
            style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);

        for (int i = 0; i < referenceMarkNames.size(); i++) {
            List<Integer> numbers =
                findCitedEntryIndices(
                    Arrays.asList(bibtexKeys[i]),
                    sortedCited
                    );
            citMarkers[i] = style.getNumCitationMarker(numbers, minGroupingCount, false);
        }
        return citMarkers;
    }

    /**
     * Return type used by produceCitationMarkersForNormalStyle
     */
    static class CitationMarkersWithUniqueLetters {
        String[] citMarkers;
        Map<String, String> uniqueLetters;

        CitationMarkersWithUniqueLetters(
            String[] citMarkers,
            Map<String, String> uniqueLetters) {
            this.citMarkers = citMarkers;
            this.uniqueLetters = uniqueLetters;
        }
    }

    /**
     * Produce citMarkers for normal
     * (!isCitationKeyCiteMarkers &amp;&amp; !isNumberEntries) styles.
     *
     * @param referenceMarkNames Names of reference marks.
     * @param bibtexKeysIn       Bibtex citation keys.
     * @param citeKeyToBibEntry  Maps citation keys to BibEntry.
     * @param itcTypes           Citation types.
     * @param entries            Map BibEntry to BibDatabase.
     * @param style              Bibliography style.
     */
    CitationMarkersWithUniqueLetters
    produceCitationMarkersForNormalStyle(
        List<String> referenceMarkNames,
        String[][] bibtexKeysIn,
        Map<String, BibEntry> citeKeyToBibEntry,
        int[] itcTypes,
        Map<BibEntry, BibDatabase> entries,
        OOBibStyle style
        )
        throws BibEntryNotFoundException {

        assert !style.isCitationKeyCiteMarkers();
        assert !style.isNumberEntries();
        // Citations in (Au1, Au2 2000) form

        final int nRefMarks = referenceMarkNames.size();
        assert (bibtexKeysIn.length == nRefMarks);
        assert (itcTypes.length == nRefMarks);
        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeysIn, citeKeyToBibEntry,
                                         "produceCitationMarkersForNormalStyle");

        BibEntry[][] cEntriesForAll =
            getBibEntriesSortedWithinReferenceMarks(bibtexKeysIn, citeKeyToBibEntry, style);

        // Update bibtexKeys to match the new sorting (within each referenceMark)
        String[][] bibtexKeys = mapBibEntriesToCitationKeysOrNullForAll(cEntriesForAll);
        // Note: bibtexKeys[i][j] may be null, for UndefinedBibtexEntry

        assert (bibtexKeys.length == nRefMarks);

        String[][] normCitMarkers =
            normalizedCitationMarkersForNormalStyle(cEntriesForAll, entries, style);

        Map<String, String> uniqueLetters =
            createUniqueLetters(bibtexKeys, normCitMarkers);

        // Finally, go through all citation markers, and update
        // those referring to entries in our current list:
        final int maxAuthorsFirst = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST);
        Set<String> seenBefore = new HashSet<>();

        String[] citMarkers = new String[nRefMarks];

        for (int i = 0; i < nRefMarks; i++) {

            final int nCitedEntries = bibtexKeys[i].length;
            int[] firstLimAuthors = new int[nCitedEntries];
            String[] uniqueLetterForCitedEntry = new String[nCitedEntries];

            for (int j = 0; j < nCitedEntries; j++) {
                String currentKey = bibtexKeys[i][j]; // nullable

                // firstLimAuthors will be (-1) except at the first
                // refMark it appears at, where a positive
                // maxAuthorsFirst may override. This is why:
                // https://discourse.jabref.org/t/
                // number-of-authors-in-citations-style-libreoffice/747/3
                // "Some citation styles require to list the full
                // names of the first 4 authors for the first
                // time. Later it is sufficient to have only maybe
                // (Author A and Author B 2019 et al.)"
                firstLimAuthors[j] = -1;
                if (maxAuthorsFirst > 0) {
                    if (!seenBefore.contains(currentKey)) {
                        firstLimAuthors[j] = maxAuthorsFirst;
                    }
                    seenBefore.add(currentKey);
                }

                String uniqueLetterForKey = uniqueLetters.get(currentKey);
                uniqueLetterForCitedEntry[j] =
                    (uniqueLetterForKey == null
                     ? ""
                     : uniqueLetterForKey);
            }

            List<BibEntry> cEntries =
                Arrays.stream(cEntriesForAll[i])
                .map(OOBibBase::undefinedBibEntryToNull)
                .collect(Collectors.toList());

            citMarkers[i] =
                style.getCitationMarker(
                    cEntries,
                    entries,
                    itcTypes[i] == OOBibBase.AUTHORYEAR_PAR,
                    uniqueLetterForCitedEntry,
                    firstLimAuthors
                    );
        }

        return new CitationMarkersWithUniqueLetters(citMarkers, uniqueLetters);
    }

    /* ***********************************
     *
     *  modifies both storage and presentation
     *
     * ***********************************/

    /**
     *  Insert {@code n} spaces in a way that reference
     *  marks just before or just after the cursor are not affected.
     *
     *  This is based on the observation, that starting two
     *  new paragraphs separates us from a reference mark on either side.
     *
     *  The pattern used is:
     *  {@code safeInsertSpaces(n): para, para, left, space(n), right-delete, left(n), left-delete}
     *
     *  @param position Where to insert (at position.getStart())
     *  @param n  Number of spaces to insert.
     *
     *  @return a new cursor, covering the just-inserted spaces.
     *
     *  This could be generalized to insert arbitrary text safely
     *  between two reference marks. But we do not need that now.
     */
    private static XTextCursor
    safeInsertSpacesBetweenReferenceMarks(XTextRange position, int n) {
        // Start with an empty cursor at position.getStart();
        XText text = position.getText();
        XTextCursor cursor = text.createTextCursorByRange(position.getStart());
        text.insertString(cursor, "\r\r", false); // para, para
        cursor.goLeft((short) 1, false); // left
        text.insertString(cursor, " ".repeat(n), false); // space(n)
        cursor.goRight((short) 1, true);
        cursor.setString(""); // right-delete
        cursor.goLeft((short) n, false); // left(n)
        cursor.goLeft((short) 1, true);
        cursor.setString(""); // left-delete
        cursor.goRight((short) n, true);
        return cursor;
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
     *  @param name For the reference mark.
     *  @param position Collapsed to its end.
     *  @param insertSpaceAfter We insert a space after the mark, that
     *                          carries on format of characters from
     *                          the original position.
     *
     *  @param withoutBrackets  Force empty reference mark (no brackets).
     *                          For use with INVISIBLE_CIT.
     *
     */
    private static void
    createReferenceMarkForCitationGroup(
        DocumentConnection documentConnection,
        String name,
        XTextCursor position,
        boolean insertSpaceAfter,
        boolean withoutBrackets
        )
        throws
        CreationException,
        NoDocumentException,
        WrappedTargetException {

        // The cursor we received: we push it before us.
        position.collapseToEnd();

        XTextCursor cursor = safeInsertSpacesBetweenReferenceMarks(position.getEnd(), 2);

        // cursors before the first and after the last space
        XTextCursor cursorBefore =
            cursor.getText().createTextCursorByRange(cursor.getStart());
        XTextCursor cursorAfter =
            cursor.getText().createTextCursorByRange(cursor.getEnd());

        cursor.collapseToStart();
        cursor.goRight((short) 1, false);
        // now we are between two spaces

        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();
        String bracketedContent = (withoutBrackets
                                   ? ""
                                   : left + right);

        cursor.getText().insertString(
            cursor,
            bracketedContent,
            true);

        /* XNamed mark = */ documentConnection.insertReferenceMark(
            name,
            cursor,
            true // absorb
            );

        cursorBefore.goRight((short) 1, true);
        cursorBefore.setString("");
        if (!insertSpaceAfter) {
            cursorAfter.goLeft((short) 1, true);
            cursorAfter.setString("");
        }
    }

    /**
     * Remove brackets, but if the result would become empty, leave
     * them; if the result would be a single characer, leave the left bracket.
     *
     * @param removeBracketsFromEmpty is intended to force removal if
     *        we are working on an "Empty citation" (INVISIBLE_CIT).
     */
    private static void
    cleanFillCursorForCitationGroup(
        DocumentConnection documentConnection,
        String name, // Identifies group
        boolean removeBracketsFromEmpty,
        boolean alwaysRemoveBrackets
        )
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();

        XTextContent markAsTextContent =
            documentConnection.getReferenceMarkAsTextContentOrNull(name);

        if (markAsTextContent == null) {
            throw new RuntimeException(
                String.format(
                    "cleanFillCursorForCitationGroup: markAsTextContent(%s) == null",
                    name
                    ));
        }
        XTextCursor full =
            DocumentConnection.getTextCursorOfTextContent(
                markAsTextContent);
        if (full == null) {
            throw new RuntimeException(
                "cleanFillCursorForCitationGroup: full == null"
                );
        }
        final String fullText = full.getString();
        final int fullTextLength = fullText.length();

        XTextCursor alpha = full.getText().createTextCursorByRange(full);
        alpha.collapseToStart();

        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight(leftLength, false);

        XTextCursor omega = full.getText().createTextCursorByRange(full);
        omega.collapseToEnd();

        if (!fullText.startsWith(left)) {
            throw new RuntimeException(
                String.format(
                    "cleanFillCursorForCitationGroup:"
                    + " (%s) does not start with REFERENCE_MARK_LEFT_BRACKET",
                    name
                    ));
        }

        if (!fullText.endsWith(right)) {
            throw new RuntimeException(
                String.format(
                    "cleanFillCursorForCitationGroup:"
                    + " (%s) does not end with REFERENCE_MARK_RIGHT_BRACKET",
                    name
                    ));
        }

        final int contentLength = (fullTextLength - (leftLength + rightLength));
        if (contentLength < 0) {
            throw new RuntimeException(
                String.format(
                    "cleanFillCursorForCitationGroup: length(%s) < 0",
                    name
                    ));
        }

        boolean removeRight = (
            // have at least 1 character content
            (contentLength >= 1)
            || ((contentLength == 0) && removeBracketsFromEmpty)
            || alwaysRemoveBrackets
            );
        boolean removeLeft = (
            // have at least 2 character content
            (contentLength >= 2)
            || ((contentLength == 0) && removeBracketsFromEmpty)
            || alwaysRemoveBrackets
            );

        if (removeRight) {
            omega.goLeft(rightLength, true);
            omega.setString("");
        }

        if (removeLeft) {
            alpha.goRight(leftLength, true);
            alpha.setString("");
        }
    }

    private static XTextCursor
    getFillCursorForCitationGroup(
        DocumentConnection documentConnection,
        String name // Identifies group
        )
        throws
        NoDocumentException,
        WrappedTargetException,
        CreationException {

        final boolean debugThisFun = false;
        final String left = REFERENCE_MARK_LEFT_BRACKET;
        final String right = REFERENCE_MARK_RIGHT_BRACKET;
        final short leftLength = (short) left.length();
        final short rightLength = (short) right.length();

        XTextCursor full = null;
        String fullText = null;
        for (int i = 1; i <= 2; i++) {
            XTextContent markAsTextContent =
                documentConnection.getReferenceMarkAsTextContentOrNull(name);

            if (markAsTextContent == null) {
                throw new RuntimeException(
                    String.format(
                        "getFillCursorForCitationGroup: markAsTextContent(%s) == null (attempt %d)",
                        name, i
                        ));
            }
            full =
                DocumentConnection.getTextCursorOfTextContent(
                    markAsTextContent);
            if (full == null) {
                throw new RuntimeException(
                    String.format(
                        "getFillCursorForCitationGroup: full == null (attempt %d)", i
                        )
                    );
            }

            fullText = full.getString();
            if (debugThisFun) {
                System.out.printf("getFillCursor: fulltext = '%s'%n", fullText);
            }

            if (fullText.length() >= 2) {
                break;
            } else {
                // (fullText.length() < 2)
                if (i == 2) {
                    throw new RuntimeException(
                        String.format(
                            "getFillCursorForCitationGroup:"
                            + " (fullText.length() < 2) (attempt %d)", i
                            )
                        );
                }
                // too short, recreate
                if (debugThisFun) {
                    System.out.println("getFillCursor: too short, recreate");
                }
                full.setString("");
                try {
                    documentConnection.removeReferenceMark(name);
                } catch (NoSuchElementException ex) {
                    LOGGER.warn(String.format(
                                    "getFillCursorForCitationGroup got NoSuchElementException"
                                    + " for '%s'", name));
                }
                createReferenceMarkForCitationGroup(
                    documentConnection,
                    name,
                    full,
                    false, // insertSpaceAfter
                    false  // withoutBrackets
                    );
            }
        }

        if (full == null) {
            throw new RuntimeException(
                "getFillCursorForCitationGroup: full == null (after loop)"
                );
        }
        if (fullText == null) {
            throw new RuntimeException(
                "getFillCursorForCitationGroup: fullText == null (after loop)"
                );
        }

        // we have at least two characters inside
        XTextCursor alpha = full.getText().createTextCursorByRange(full);
        alpha.collapseToStart();
        XTextCursor omega = full.getText().createTextCursorByRange(full);
        omega.collapseToEnd();

        XTextCursor beta = full.getText().createTextCursorByRange(full);
        beta.collapseToStart();
        beta.goRight((short) 1, false);
        beta.goRight((short) (fullText.length() - 2), true);
        beta.setString(left + right);
        beta.collapseToEnd();
        beta.goLeft(rightLength, false);
        // drop the initial character
        alpha.goRight((short) 1, true);
        alpha.setString("");
        // drop the last character
        omega.goLeft((short) 1, true);
        omega.setString("");
        return beta;
    }

    private static void
    fillCitationMarkInCursor(
        DocumentConnection documentConnection,
        String name, // citationGroup
        XTextCursor cursor,
        String citationText,
        boolean withText,
        OOBibStyle style
        )
        throws
        UnknownPropertyException,
        WrappedTargetException,
        PropertyVetoException,
        IllegalArgumentException,
        UndefinedCharacterFormatException {

        // Last minute editing: If there is "page info" for this
        // citation mark, we should inject it into the citation marker
        // when creating.

        String citText;
        String pageInfo =
            getPageInfoForReferenceMarkName(documentConnection, name);
        citText =
            pageInfo.isEmpty()
            ? citationText
            : style.insertPageInfo(citationText, pageInfo);

        if (withText) {
            // setString: All styles are removed when applying this method.
            cursor.setString(citText);
            DocumentConnection.setCharLocaleNone(cursor);
            if (style.isFormatCitations()) {
                String charStyle = style.getCitationCharacterFormat();
                DocumentConnection.setCharStyle(cursor, charStyle);
            }
        } else {
            cursor.setString("");
        }

        // documentConnection.insertReferenceMark(name, cursor, true);

        // Last minute editing: find "et al." (OOBibStyle.ET_AL_STRING) and
        //                      format it as italic.

        // Check if we should italicize the "et al." string in citations:
        boolean italicize = style.getBooleanCitProperty(OOBibStyle.ITALIC_ET_AL);
        if (italicize) {
            String etAlString = style.getStringCitProperty(OOBibStyle.ET_AL_STRING);
            for (int index = citText.indexOf(etAlString);
                 index >= 0;
                 index = citText.indexOf(etAlString, index + 1)) {
                italicizeRangeFromPosition(cursor, index, index + etAlString.length());
            }
        }
    }

    /**
     * Taking position.getStart() as a reference point, italicize the range (ref+start,ref+end)
     *
     * @param position  : position.getStart() is out reference point.
     * @param start     : start of range to italicize w.r.t position.getStart().
     * @param end       : end of range  to italicize w.r.t position.getStart().
     *
     *  Why this API?  This is used after finding "et al." string in a
     *  citation marker.
     */
    private static void
    italicizeRangeFromPosition(
        XTextCursor position,
        int start,
        int end
        )
        throws
        UnknownPropertyException,
        PropertyVetoException,
        IllegalArgumentException,
        WrappedTargetException {

        XTextRange range = position.getStart();
        XTextCursor cursor = position.getText().createTextCursorByRange(range);
        cursor.goRight((short) start, false);
        cursor.goRight((short) (end - start), true);

        DocumentConnection.setCharFormatItalic(cursor);
    }

    /**
     *
     *  Insert a reference mark: creates and fills it.
     *
     * @param documentConnection Connection to a document.
     *
     * @param name Name of the reference mark to be created and also
     *             the name of the custom property holding the pageInfo part.
     *
     * @param position OUT: left collapsed, just after the space inserted,
     *                      or after the reference mark inserted.
     */
    private void
    insertReferenceMark(
        DocumentConnection documentConnection,
        String name,
        String citationText,
        XTextCursor position,
        boolean withText,
        OOBibStyle style,
        boolean insertSpaceAfter
        )
        throws
        UnknownPropertyException,
        WrappedTargetException,
        PropertyVetoException,
        IllegalArgumentException,
        UndefinedCharacterFormatException,
        CreationException,
        NoDocumentException {

        createReferenceMarkForCitationGroup(documentConnection,
                                            name,
                                            position,
                                            insertSpaceAfter,
                                            !withText);

        if (withText) {
            XTextCursor c2 = getFillCursorForCitationGroup(documentConnection,
                                                           name);

            fillCitationMarkInCursor(documentConnection,
                                     name,
                                     c2,
                                     citationText,
                                     withText,
                                     style);

            cleanFillCursorForCitationGroup(documentConnection,
                                            name,
                                            !withText,
                                            REFERENCE_MARK_ALWAYS_REMOVE_BRACKETS);
        }
        position.collapseToEnd();
    }

    /**
     * Test if we have a problem applying character style prescribe by
     * the style.
     *
     * If the style prescribes an character style, we insert a
     * character, format it and delete it.
     *
     * An UndefinedCharacterFormatException may be raised, indicating
     * that the style requested is not available in the document.
     *
     * @param cursor Provides location where we insert, format and
     * remove a character.
     */
    void assertCitationCharacterFormatIsOK(
        XTextCursor cursor,
        OOBibStyle style
        )
        throws UndefinedCharacterFormatException {
        if (!style.isFormatCitations()) {
            return;
        }

        /* We do not want to change the cursor passed in, so using a copy. */
        XTextCursor c2 =
            cursor.getText().createTextCursorByRange(cursor.getEnd());

        /*
         * Inserting, formatting and removing a single character
         * still leaves a style change in place.
         * Let us try with two characters, formatting only the first.
         */
        c2
         .getText()
         .insertString(c2, "@*", false);

        String charStyle = style.getCitationCharacterFormat();
        try {
            c2.goLeft((short) 1, false); // step over '*'
            c2.goLeft((short) 1, true);  // select '@'
            // The next line may throw
            // UndefinedCharacterFormatException(charStyle).
            // We let that propagate.
            DocumentConnection.setCharStyle(c2, charStyle);
        } finally {
            // Before leaving this scope, always delete the character we
            // inserted:
            c2.collapseToStart();
            c2.goRight((short) 2, true);  // select '@*'
            c2.setString("");
        }
    }

    /**
     * Called from: OpenOfficePanel.pushEntries, a GUI action for
     * "Cite", "Cite in-text", "Cite special" and "Insert empty
     * citation".
     *
     * Uses LO undo context "Insert citation".
     *
     * Note: Undo does not remove custom properties. Presumably
     * neither does it reestablish them.
     *
     * This method inserts a reference mark in the text (at the
     * cursor) citing the entries, and (if sync is true) refreshes the
     * citation markers and the bibliography.
     *
     * @param entries       The entries to cite.
     *
     * @param database      The database the entries belong to (all of them).
     *                      Used when creating the citation mark.
     *
     * @param allBases      Used if sync is true. The list of all databases
     *                      we may need to refresh the document.
     *
     * @param style         The bibliography style we are using.
     * @param inParenthesis Indicates whether it is an in-text
     *                      citation or a citation in parenthesis.
     *                      This is not relevant if
     *                      numbered citations are used.
     * @param withText      Indicates whether this should be a visible
     *                      citation (true) or an empty (invisible) citation (false).
     *
     * @param pageInfo      A single page-info for these entries. Stored in custom property
     *                      with the same name as the reference mark.
     *
     *                      Related https://latex.org/forum/viewtopic.php?t=14331
     *                      """
     *                      Q: What I would like is something like this:
     *                      (Jones, 2010, p. 12; Smith, 2003, pp. 21 - 23)
     *                      A: Not in a single \citep, no.
     *                         Use \citetext{\citealp[p.~12]{jones2010};
     *                                       \citealp[pp.~21--23]{smith2003}}
     *                      """
     *
     * @param sync          Indicates whether the reference list and in-text citations
     *                      should be refreshed in the document.
     *
     *
     */
    public void
    insertCitation(
        List<BibEntry> entries,
        BibDatabase database,
        List<BibDatabase> allBases,
        OOBibStyle style,
        boolean inParenthesis,
        boolean withText,
        String pageInfo,
        boolean sync
        )
        throws
        JabRefException,
        IllegalArgumentException,
        UnknownPropertyException,
        NotRemoveableException,
        PropertyExistException,
        IllegalTypeException,
        UndefinedCharacterFormatException,
        WrappedTargetException,
        NoSuchElementException,
        PropertyVetoException,
        IOException,
        CreationException,
        BibEntryNotFoundException,
        UndefinedParagraphFormatException,
        NoDocumentException,
        InvalidStateException {

        styleIsRequired(style);

        if (entries == null || entries.size() == 0) {
            // System.out.println("insertCitation: throwing JabRefException");
            throw new JabRefException(
                "No bibliography entries selected",
                Localization.lang(
                    "No bibliography entries are selected for citation.")
                + "\n"
                + Localization.lang("Select some before citing.")
                );
        }

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        boolean useUndoContext = true;

        try {
            if (useUndoContext) {
                documentConnection.enterUndoContext("Insert citation");
            }
            XTextCursor cursor;
            // Get the cursor positioned by the user.
            try {
                cursor = documentConnection.getViewCursor();
            } catch (RuntimeException ex) {
                // com.sun.star.uno.RuntimeException
                throw new JabRefException(
                    "Could not get the cursor",
                    Localization.lang(
                        "Could not get the cursor.")
                    );
            }

            sortBibEntryListForMulticite(entries, style);

            String keyString =
                entries.stream()
                .map(entry -> entry.getCitationKey().orElse(""))
                .collect(Collectors.joining(","));
            // Generate unique mark-name
            int itcType = citationTypeFromOptions(withText, inParenthesis);
            String newName =
                getUniqueReferenceMarkName(
                    documentConnection,
                    keyString,
                    itcType);

            // If we should store metadata for page info, do that now:
            //
            // Note: the (single) pageInfo here gets associated with
            //       the citation group. At presentation it is inject
            //       to before the final parenthesis, appearing to
            //       belong to the last entry added here.
            //
            // But: (1) the last entry depends on the above
            //      sortBibEntryListForMulticite call; (2) On
            //      "Separate" it belongs to nobody.
            //
            if (pageInfo != null) {
                LOGGER.info("Storing page info: " + pageInfo);
                documentConnection.setCustomProperty(newName, pageInfo);
            }
            // else: branch ???
            // Note: if (pageInfo is null), we might inadvertently
            // pick up a pageInfo from an earlier citation. The user
            // may have removed the citation, thus the reference mark,
            // but pageInfo stored separately stays there.

            assertCitationCharacterFormatIsOK(cursor, style);

            // Insert reference mark and text
            // {
                // Create a BibEntry to BibDatabase map (to make
                // style.getCitationMarker happy?)
                Map<BibEntry, BibDatabase> databaseMap = new HashMap<>();
                for (BibEntry entry : entries) {
                    // Using the same database for each entry.
                    // Probably the GUI limits selection to a single database.
                    databaseMap.put(entry, database);
                }

                // The text we insert
                String citeText =
                    style.isNumberEntries()
                    ? "[-]" // A dash only. Presumably we expect a refresh later.
                    : style.getCitationMarker(
                        entries,
                        databaseMap,
                        inParenthesis,
                        null,
                        null);
                if (citeText.equals("")) {
                    citeText = "[?]";
                }
                insertReferenceMark(
                    documentConnection,
                    newName,
                    citeText,
                    cursor,
                    withText,
                    style,
                    true // insertSpaceAfter
                    );
                // } // end of scope for databaseMap, citeText

                // Move to the right of the space and remember this
                // position: we will come back here in the end.
                // cursor.collapseToEnd();
                // cursor.goRight((short) 1, false);
            XTextRange position = cursor.getEnd();

            if (sync) {
                // To account for numbering and for uniqueLetters, we
                // must refresh the cite markers:
                ProduceCitationMarkersResult x =
                    produceCitationMarkers(
                        documentConnection,
                        allBases,
                        style
                        );

                try {
                    documentConnection.lockControllers();

                    applyNewCitationMarkers(
                        documentConnection,
                        x.jabRefReferenceMarkNamesSortedByPosition,
                        x.citMarkers,
                        x.itcTypes,
                        style);

                    // Insert it at the current position:
                    rebuildBibTextSection(
                        documentConnection,
                        style,
                        x.jabRefReferenceMarkNamesSortedByPosition,
                        x.uniqueLetters,
                        x.fce
                        );
                } finally {
                    documentConnection.unlockControllers();
                }

                /*
                 * Problem: insertEntry in bibliography
                 * Reference is destroyed when we want to get there.
                 */
                // Go back to the relevant position:
                try {
                    cursor.gotoRange(position, false);
                } catch (com.sun.star.uno.RuntimeException ex) {
                    LOGGER.warn("OOBibBase.insertEntry:"
                            + " Could not go back to end of in-text citation", ex);
                }
            }
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO JAR file).
            throw new ConnectionLostException(ex.getMessage());
        } finally {
            if (useUndoContext) {
                documentConnection.leaveUndoContext();
            }
        }
    }

    /* **************************************************
     *
     *  modifies both storage and presentation, but should only affect presentation
     *
     * **************************************************/

    static class ExportCitedHelperResult {
        /**
         * null: not done; isempty: no unresolved
         */
        List<String> unresolvedKeys;
        BibDatabase newDatabase;
    }

    /**
     * Helper for GUI action "Export cited"
     *
     * Refreshes citation markers, (although the user did not ask for that).
     * Does not refresh the bibliography.
     */
    public ExportCitedHelperResult exportCitedHelper(
        List<BibDatabase> databases,
        OOBibStyle style
        )
        throws
        WrappedTargetException,
        NoSuchElementException,
        NoDocumentException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        PropertyVetoException,
        IOException,
        CreationException,
        BibEntryNotFoundException,
        InvalidStateException {

        ExportCitedHelperResult res = new ExportCitedHelperResult();
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        try {
            documentConnection.enterUndoContext("Changes during \"Export cited\"");

            ProduceCitationMarkersResult x =
                produceCitationMarkers(
                    documentConnection,
                    databases,
                    style);
                res.unresolvedKeys = x.unresolvedKeys;
                res.newDatabase = this.generateDatabase(databases);
        } finally {
            documentConnection.leaveUndoContext();
        }
        return res;
    }

    /**
     * Visit each reference mark in referenceMarkNames, overwrite its
     * text content.
     *
     * After each insertReferenceMark call check if we lost the
     * OOBibBase.BIB_SECTION_NAME bookmark and recreate it if we did.
     *
     * @param referenceMarkNames Reference mark names
     *
     * @param citMarkers Corresponding text for each reference mark,
     *                   that replaces the old text.
     *
     * @param types itcType codes for each reference mark.
     *
     * @param style Bibliography style to use.
     *
     */
    private void
    applyNewCitationMarkers(
        DocumentConnection documentConnection,
        List<String> referenceMarkNames,
        String[] citMarkers,
        int[] types,
        OOBibStyle style
        )
        throws
        NoDocumentException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        CreationException,
        WrappedTargetException,
        PropertyVetoException {

        final int nRefMarks = referenceMarkNames.size();
        assert (citMarkers.length == nRefMarks);
        assert (types.length == nRefMarks);

        final boolean hadBibSection =
            (documentConnection.getBookmarkRangeOrNull(OOBibBase.BIB_SECTION_NAME) != null);

        // If we are supposed to set character format for citations,
        // must run a test before we delete old citation
        // markers. Otherwise, if the specified character format
        // doesn't exist, we end up deleting the markers before the
        // process crashes due to a the missing format, with
        // catastrophic consequences for the user.
        boolean mustTestCharFormat = style.isFormatCitations();

        for (int i = 0; i < nRefMarks; i++) {

            final String name = referenceMarkNames.get(i);

            boolean withText = (types[i] != OOBibBase.INVISIBLE_CIT);
            if (withText) {
                XTextCursor cursor =
                    getFillCursorForCitationGroup(
                        documentConnection,
                        name // Identifies group
                        );

                if (mustTestCharFormat) {
                    assertCitationCharacterFormatIsOK(cursor, style);
                    mustTestCharFormat = false;
                }

                fillCitationMarkInCursor(
                    documentConnection,
                    name, // citationGroup
                    cursor,
                    citMarkers[i], // citationText,
                    withText,
                    style
                    );

                cleanFillCursorForCitationGroup(documentConnection,
                                                name,
                                                !withText,
                                                REFERENCE_MARK_ALWAYS_REMOVE_BRACKETS);
            }

            if (hadBibSection
                && (documentConnection.getBookmarkRangeOrNull(OOBibBase.BIB_SECTION_NAME) == null)) {
                // if (true) {
                    // Overwriting text already there is too harsh.
                    // I am making it an error, to see if we ever get here.
                    throw new RuntimeException(
                        "OOBibBase.applyNewCitationMarkers:"
                        + " just overwrote the bibliography section marker. Sorry.");
                // } else {
                    // We have overwritten the marker for the start of the reference list.
                    // We need to add it again.
                    // ---
                    // cursor.collapseToEnd();
                    // OOUtil.insertParagraphBreak(documentConnection.xText, cursor);
                    // documentConnection.insertBookmark(OOBibBase.BIB_SECTION_NAME, cursor, true);
                    // cursor.collapseToEnd();
                // }
            }
        }
    }

    /**
     * The main field is citMarkers, the rest is for reuse in caller.
     */
    static class ProduceCitationMarkersResult {
        /** JabRef reference mark names, sorted by position */
        List<String> jabRefReferenceMarkNamesSortedByPosition;
        /** AUTHORYEAR_PAR, AUTHORYEAR_INTEXT or INVISIBLE_CIT */
        int[] itcTypes;
        /** citation keys */
        String[][] bibtexKeys;
        /** citation markers */
        String[] citMarkers;
        /** Letters making cited sources unique. */
        Map<String, String> uniqueLetters;
        /** Results from findCitedEntries */
        FindCitedEntriesResult fce;
        /** Citation keys not found in databases */
        List<String> unresolvedKeys;

        ProduceCitationMarkersResult(
            List<String> jabRefReferenceMarkNamesSortedByPosition,
            int[] itcTypes,
            String[][] bibtexKeys,
            String[] citMarkers,
            Map<String, String> uniqueLetters,
            FindCitedEntriesResult fce,
            List<String> unresolvedKeys
            ) {
            this.jabRefReferenceMarkNamesSortedByPosition = jabRefReferenceMarkNamesSortedByPosition;
            this.itcTypes = itcTypes;
            this.bibtexKeys = bibtexKeys;
            this.citMarkers = citMarkers;
            this.uniqueLetters = uniqueLetters;
            this.fce = fce;
            this.unresolvedKeys = unresolvedKeys;
        }
    }

    private ProduceCitationMarkersResult
    produceCitationMarkers(
        DocumentConnection documentConnection,
        List<BibDatabase> databases,
        OOBibStyle style
        )
        throws
        WrappedTargetException,
        IllegalArgumentException,
        NoSuchElementException,
        BibEntryNotFoundException,
        NoDocumentException {

        // Normally we sort the reference marks according to their
        // order of appearance:

        List<String> jabRefReferenceMarkNamesSortedByPosition =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        List<String> referenceMarkNames = jabRefReferenceMarkNamesSortedByPosition;

        final int nRefMarks = referenceMarkNames.size();

        int[] itcTypes = new int[nRefMarks];
        String[][] bibtexKeys = new String[nRefMarks][];
        parseRefMarkNamesToArrays(referenceMarkNames, itcTypes, bibtexKeys);

        FindCitedEntriesResult fce =
            findCitedEntries(
                findCitedKeys(documentConnection),
                databases
                );
        // fce.entries are in same order as returned by findCitedKeys

        // citMarkers[i] = what goes in the text at referenceMark[i]
        String[] citMarkers;

        // fill citMarkers
        Map<String, String> uniqueLetters = new HashMap<>();

        if (style.isCitationKeyCiteMarkers()) {
            citMarkers =
                produceCitationMarkersForIsCitationKeyCiteMarkers(
                    referenceMarkNames,
                    bibtexKeys,
                    fce.citeKeyToBibEntry,
                    style);
        } else if (style.isNumberEntries()) {
            if (style.isSortByPosition()) {
                citMarkers =
                    produceCitationMarkersForIsNumberEntriesIsSortByPosition(
                        referenceMarkNames,
                        bibtexKeys,
                        fce.citeKeyToBibEntry,
                        style);
            } else {
                citMarkers =
                    produceCitationMarkersForIsNumberEntriesNotSortByPosition(
                        referenceMarkNames,
                        bibtexKeys,
                        fce.entries,
                        style);
            }
        } else /* Normal case, (!isCitationKeyCiteMarkers && !isNumberEntries) */ {
            CitationMarkersWithUniqueLetters x =
                produceCitationMarkersForNormalStyle(
                    referenceMarkNames,
                    bibtexKeys,
                    fce.citeKeyToBibEntry,
                    itcTypes,
                    fce.entries,
                    style);
                citMarkers = x.citMarkers;
                uniqueLetters = x.uniqueLetters;
        }

        return new ProduceCitationMarkersResult(
                jabRefReferenceMarkNamesSortedByPosition,
                itcTypes,
                bibtexKeys,
                citMarkers,
                uniqueLetters,
                fce,
                unresolvedKeysFromEntries(fce.entries)
        );
    }

    /* **************************************************
     *
     *     Bibliography: needs uniqueLetters or numbers
     *
     * **************************************************/

    /**
     * Rebuilds the bibliography.
     *
     *  Note: assumes fresh `jabRefReferenceMarkNamesSortedByPosition`
     *  if `style.isSortByPosition()`
     */
    private void
    rebuildBibTextSection(
        DocumentConnection documentConnection,
        OOBibStyle style,
        List<String> jabRefReferenceMarkNamesSortedByPosition,
        final Map<String, String> uniqueLetters,
        FindCitedEntriesResult fce
        )
        throws
        NoSuchElementException,
        WrappedTargetException,
        IllegalArgumentException,
        CreationException,
        PropertyVetoException,
        UnknownPropertyException,
        UndefinedParagraphFormatException {

        Map<BibEntry, BibDatabase> entries;

        if (style.isSortByPosition()) {
            // We need to sort the entries according to their order of appearance:
            entries =
                sortEntriesByRefMarkNames(
                    jabRefReferenceMarkNamesSortedByPosition,
                    fce.citeKeyToBibEntry,
                    fce.entries
                    );
        } else {
            entries = sortEntriesByComparator(fce.entries, entryComparator);
        }

        clearBibTextSectionContent2(documentConnection);

        populateBibTextSection(
            documentConnection,
            entries,
            style,
            uniqueLetters);
    }

    /**
     * Insert body of bibliography at `cursor`.
     *
     * @param documentConnection Connection.
     * @param cursor  Where to
     * @param entries Its iteration order defines order in bibliography.
     * @param style Style.
     * @param parFormat Passed to OOUtil.insertFullReferenceAtCurrentLocation
     * @param uniqueLetters
     *
     * Only called from populateBibTextSection (and that from rebuildBibTextSection)
     */
    private void
    insertFullReferenceAtCursor(
        DocumentConnection documentConnection,
        XTextCursor cursor,
        Map<BibEntry, BibDatabase> entries,
        OOBibStyle style,
        String parFormat,
        final Map<String, String> uniqueLetters
        )
        throws
        UndefinedParagraphFormatException,
        IllegalArgumentException,
        UnknownPropertyException,
        PropertyVetoException,
        WrappedTargetException {

        int number = 1;
        for (Map.Entry<BibEntry, BibDatabase> entry : entries.entrySet()) {

            // skip unresolved entries
            if (entry.getKey() instanceof UndefinedBibtexEntry) {
                continue;
            }

            OOUtil.insertParagraphBreak(documentConnection.xText, cursor);

            // insert marker
            if (style.isNumberEntries()) {
                // Note: minGroupingCount is pointless here, we are
                // formatting a single entry.
                // int minGroupingCount = style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);
                int minGroupingCount = 2;
                List<Integer> numbers = Collections.singletonList(number++);
                String marker =
                    style.getNumCitationMarker(
                        numbers,
                        minGroupingCount,
                        true);

                OOUtil.insertTextAtCurrentLocation(
                    documentConnection.xText,
                    cursor,
                    marker,
                    Collections.emptyList()
                    );
            }

            // insert the actual details.
            Layout layout = style.getReferenceFormat(entry.getKey().getType());
            layout.setPostFormatter(POSTFORMATTER);
            OOUtil.insertFullReferenceAtCurrentLocation(
                documentConnection.xText,
                cursor,
                layout,
                parFormat,
                entry.getKey(),
                entry.getValue(),
                uniqueLetters.get(entry.getKey()
                                  .getCitationKey()
                                  .orElse(null))
                );
        }
    }

    /**
     * Insert a paragraph break and create a text section for the bibliography.
     *
     * Only called from `clearBibTextSectionContent2`
     */
    private void
    createBibTextSection2(DocumentConnection documentConnection)
        throws
        IllegalArgumentException,
        CreationException {

        // Always creating at the end of documentConnection.xText
        // Alternatively, we could receive a cursor.
        XTextCursor textCursor = documentConnection.xText.createTextCursor();
        textCursor.gotoEnd(false);

        OOUtil.insertParagraphBreak(documentConnection.xText, textCursor);

        documentConnection.insertTextSection(
            OOBibBase.BIB_SECTION_NAME,
            textCursor,
            false
            );
    }

    /**
     *  Find and clear the text section OOBibBase.BIB_SECTION_NAME to "",
     *  or create it.
     *
     * Only called from: `rebuildBibTextSection`
     *
     */
    private void
    clearBibTextSectionContent2(DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        IllegalArgumentException,
        CreationException {

        XNameAccess nameAccess = documentConnection.getTextSections();
        if (!nameAccess.hasByName(OOBibBase.BIB_SECTION_NAME)) {
            createBibTextSection2(documentConnection);
            return;
        }

        try {
            Any a = ((Any) nameAccess.getByName(OOBibBase.BIB_SECTION_NAME));
            XTextSection section = (XTextSection) a.getObject();
            // Clear it:

            XTextCursor cursor =
                documentConnection.xText.createTextCursorByRange(section.getAnchor());

            cursor.gotoRange(section.getAnchor(), false);
            cursor.setString("");
        } catch (NoSuchElementException ex) {
            // NoSuchElementException: is thrown by child access
            // methods of collections, if the addressed child does
            // not exist.

            // We got this exception from nameAccess.getByName() despite
            // the nameAccess.hasByName() check just above.

            // Try to create.
            LOGGER.warn("Could not get section '" + OOBibBase.BIB_SECTION_NAME + "'", ex);
            createBibTextSection2(documentConnection);
        }
    }

    /**
     * Only called from: `rebuildBibTextSection`
     *
     * Assumes the section named `OOBibBase.BIB_SECTION_NAME` exists.
     */
    private void
    populateBibTextSection(
        DocumentConnection documentConnection,
        Map<BibEntry, BibDatabase> entries,
        OOBibStyle style,
        final Map<String, String> uniqueLetters)
        throws
        NoSuchElementException,
        WrappedTargetException,
        PropertyVetoException,
        UnknownPropertyException,
        UndefinedParagraphFormatException,
        IllegalArgumentException,
        CreationException {

        XTextSection section =
            documentConnection.getTextSectionByName(OOBibBase.BIB_SECTION_NAME);

        XTextCursor cursor =
            documentConnection.xText
            .createTextCursorByRange(section.getAnchor());

        OOUtil.insertTextAtCurrentLocation(
            documentConnection.xText,
            cursor,
            (String) style.getProperty(OOBibStyle.TITLE),
            (String) style.getProperty(OOBibStyle.REFERENCE_HEADER_PARAGRAPH_FORMAT)
        );

        String refParaFormat =
            (String) style.getProperty(OOBibStyle.REFERENCE_PARAGRAPH_FORMAT);

        insertFullReferenceAtCursor(
            documentConnection,
            cursor,
            entries,
            style,
            refParaFormat,
            uniqueLetters
        );

        documentConnection.insertBookmark(
            OOBibBase.BIB_SECTION_END_NAME,
            cursor,
            true);
        cursor.collapseToEnd();
    }

    /* *************************
     *
     *   GUI level
     *
     **************************/

    /* GUI: Manage citations */

    /**
     * GUI action "Merge citations"
     *
     */
    public void
    combineCiteMarkers(
        List<BibDatabase> databases,
        OOBibStyle style
        )
        throws
        IOException,
        WrappedTargetException,
        NoSuchElementException,
        IllegalArgumentException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        PropertyVetoException,
        CreationException,
        BibEntryNotFoundException,
        NoDocumentException,
        JabRefException,
        InvalidStateException {

        styleIsRequired(style);

        Objects.requireNonNull(databases);
        Objects.requireNonNull(style);

        final boolean useLockControllers = true;
        DocumentConnection documentConnection = this.getDocumentConnectionOrThrow();
        try {
            documentConnection.enterUndoContext("Merge citations");

        boolean madeModifications = false;

        // The testing for whitespace-only between (pivot) and (pivot+1) assumes that
        // referenceMarkNames are in textual order: textually consecutive pairs
        // must appear as neighbours (and in textual order).
        // We have a bit of a clash here: referenceMarkNames is sorted by visual position,
        // but we are testing if they are textually neighbours.
        // In a two-column layout
        //  | a | c |
        //  | b | d |
        // abcd is the textual order, but the visual order is acbd.
        // So we will not find out that a and b are only separated by white space.
        List<String> referenceMarkNames =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        final int nRefMarks = referenceMarkNames.size();
        int[] itcTypes = new int[nRefMarks];
        String[][] bibtexKeys = new String[nRefMarks][];
        parseRefMarkNamesToArrays(referenceMarkNames, itcTypes, bibtexKeys);

        try {

            if (useLockControllers) {
                documentConnection.lockControllers();
            }

            /*
             * joinableGroups collects lists of indices of referenceMarkNames
             * that we think are joinable.
             *
             * joinableGroupsCursors provides the range for each group
             */
            List<List<Integer>> joinableGroups = new ArrayList<>();
            List<XTextCursor> joinableGroupsCursors = new ArrayList<>();

            if (referenceMarkNames.size() > 0) {
                // current group
                List<Integer> currentGroup = new ArrayList<>();
                XTextCursor currentGroupCursor = null;
                XTextCursor cursorBetween = null;
                Integer prev = null;
                XTextRange prevRange = null;

                for (int i = 0; i < referenceMarkNames.size(); i++) {
                    final String name = referenceMarkNames.get(i);
                    XTextRange currentRange = documentConnection.getReferenceMarkRangeOrNull(name);
                    Objects.requireNonNull(currentRange);

                    boolean addToGroup = true;
                    /*
                     * Decide if we add name to the group
                     */

                    // Only combine (Author 2000) type citations
                    if (itcTypes[i] != OOBibBase.AUTHORYEAR_PAR
                        // allow "Author (2000)"
                        // && itcTypes[i] != OOBibBase.AUTHORYEAR_INTEXT
                        ) {
                        addToGroup = false;
                    }

                    // Even if we combine AUTHORYEAR_INTEXT citations, we
                    // would not mix them with AUTHORYEAR_PAR
                    if (addToGroup && (prev != null)) {
                        if (itcTypes[i] != itcTypes[prev]) {
                            addToGroup = false;
                        }
                    }

                    if (addToGroup && prev != null) {
                        Objects.requireNonNull(prevRange);
                        Objects.requireNonNull(currentRange);
                        if (!DocumentConnection.comparableRanges(prevRange, currentRange)) {
                            addToGroup = false;
                        } else {

                            int textOrder =
                                DocumentConnection.javaCompareRegionStarts(
                                    prevRange,
                                    currentRange);

                            if (textOrder != (-1)) {
                                String msg = String.format(
                                    "combineCiteMarkers: \"%s\" supposed to be followed by \"%s\", but %s",
                                    prevRange.getString(),
                                    currentRange.getString(),
                                    ((textOrder == 0)
                                     ? "they start at the same position"
                                     : "the start of the latter precedes the start of the first")
                                    );
                                LOGGER.warn(msg);
                                addToGroup = false;
                            }
                        }
                    }

                    if (addToGroup && (cursorBetween != null)) {
                        Objects.requireNonNull(currentGroupCursor);
                        // assume: currentGroupCursor.getEnd() == cursorBetween.getEnd()
                        if (DocumentConnection.javaCompareRegionEnds(
                                cursorBetween, currentGroupCursor) != 0) {
                            throw new RuntimeException(
                                "combineCiteMarkers: cursorBetween.end != currentGroupCursor.end");
                        }

                        XTextRange rangeStart = currentRange.getStart();

                        boolean couldExpand = true;
                        XTextCursor thisCharCursor =
                            currentRange.getText()
                            .createTextCursorByRange(cursorBetween.getEnd());
                        while (couldExpand &&
                               (DocumentConnection.javaCompareRegionEnds(
                                   cursorBetween, rangeStart) < 0)) {
                            couldExpand = cursorBetween.goRight((short) 1, true);
                            currentGroupCursor.goRight((short) 1, true);
                            //
                            thisCharCursor.goRight((short) 1, true);
                            String thisChar = thisCharCursor.getString();
                            thisCharCursor.collapseToEnd();
                            if (thisChar.isEmpty()
                                || thisChar.equals("\n")
                                || !thisChar.trim().isEmpty()) {
                                couldExpand = false;
                            }
                            if (DocumentConnection.javaCompareRegionEnds(
                                    cursorBetween, currentGroupCursor) != 0) {
                                throw new RuntimeException(
                                    "combineCiteMarkers:"
                                    + " cursorBetween.end != currentGroupCursor.end"
                                    + " (during expand)");
                            }
                        }

                        if (!couldExpand) {
                            addToGroup = false;
                        }
                    }

                    /*
                     * Even if we do not add it to an existing group,
                     * we might use it to start a new group.
                     *
                     * Can it start a new group?
                     */
                    boolean canStartGroup = (itcTypes[i] == OOBibBase.AUTHORYEAR_PAR);

                    if (!addToGroup) {
                        // close currentGroup
                        if (currentGroup.size() > 1) {
                            joinableGroups.add(currentGroup);
                            joinableGroupsCursors.add(currentGroupCursor);
                        }
                        // Start a new, empty group
                        currentGroup = new ArrayList<>();
                        currentGroupCursor = null;
                        cursorBetween = null;
                        prev = null;
                        prevRange = null;
                    }

                    if (addToGroup || canStartGroup) {
                        // Add the current entry to a group.
                        currentGroup.add(i);
                        // ... and start new cursorBetween
                        // Set up cursorBetween
                        //
                        XTextRange rangeEnd = currentRange.getEnd();
                        cursorBetween =
                            currentRange.getText().createTextCursorByRange(rangeEnd);
                        // If new group, create currentGroupCursor
                        if (currentGroupCursor == null) {
                            currentGroupCursor =
                                currentRange.getText()
                                .createTextCursorByRange(currentRange.getStart());
                        }
                        // include self in currentGroupCursor
                        currentGroupCursor.goRight(
                            (short) (currentRange.getString().length()), true);

                        if (DocumentConnection.javaCompareRegionEnds(
                                cursorBetween, currentGroupCursor) != 0) {
                            /*
                             * A problem discovered using this check: when
                             * viewing the document in
                             * two-pages-side-by-side mode, our visual
                             * firstAppearanceOrder follows the visual
                             * ordering on the screen. The problem this
                             * caused: it sees a citation on the 2nd line
                             * of the 1st page as appearing after one at
                             * the 1st line of 2nd page. Since we create
                             * cursorBetween at the end of range1Full (on
                             * 1st page), it is now BEFORE
                             * currentGroupCursor (on 2nd page).
                             */
                            throw new RuntimeException(
                                "combineCiteMarkers: "
                                + "cursorBetween.end != currentGroupCursor.end"
                                + String.format(
                                    " (after %s)", addToGroup ? "addToGroup" : "startGroup")
                                + (addToGroup
                                   ? String.format(
                                       " comparisonResult: %d\n"
                                       + "cursorBetween: '%s'\n"
                                       + "currentRange: '%s'\n"
                                       + "currentGroupCursor: '%s'\n",
                                       DocumentConnection.javaCompareRegionEnds(
                                           cursorBetween, currentGroupCursor),
                                       cursorBetween.getString(),
                                       currentRange.getString(),
                                       currentGroupCursor.getString()
                                       )
                                   : "")
                                );
                        }
                        prev = i;
                        prevRange = currentRange;
                    }
                }

                // close currentGroup
                if (currentGroup.size() > 1) {
                    joinableGroups.add(currentGroup);
                    joinableGroupsCursors.add(currentGroupCursor);
                }
            }

            if (joinableGroups.size() > 0) {
                XTextCursor textCursor = joinableGroupsCursors.get(0);
                assertCitationCharacterFormatIsOK(textCursor, style);
            }

            /*
             * Now we can process the joinable groups
             */
            for (int gi = 0; gi < joinableGroups.size(); gi++) {

                List<String> allKeys = new ArrayList<>();
                for (int gj = 0; gj < joinableGroups.get(gi).size(); gj++) {
                    int rk = joinableGroups.get(gi).get(gj);
                    allKeys.addAll(Arrays.asList(bibtexKeys[rk]));
                }

                // Note: silently drops duplicate keys.
                //       What if they have different pageInfo fields?

                //  combineCiteMarkers: merging for same citation keys,
                //       but different pageInfo looses information.
                List<String> uniqueKeys =
                    (allKeys.stream()
                     .distinct()
                     .collect(Collectors.toList()));

                // Remove the old referenceMarkNames from the document.
                // We might want to do this via backends.
                for (int gj = 0; gj < joinableGroups.get(gi).size(); gj++) {
                    int rk = joinableGroups.get(gi).get(gj);
                    documentConnection.removeReferenceMark(referenceMarkNames.get(rk));
                }

                XTextCursor textCursor = joinableGroupsCursors.get(gi);
                // Also remove the spaces between.
                textCursor.setString("");

                boolean oldStrategy = false;
                List<BibEntry> entries;
                String keyString;
                if (oldStrategy) {
                    // Note: citation keys not found are silently left
                    //       out from the combined reference mark
                    //       name. Losing information.
                    entries = lookupEntriesInDatabasesSkipMissing(uniqueKeys, databases);
                    entries.sort(new FieldComparator(StandardField.YEAR));
                    keyString =
                        entries.stream()
                        .map(c -> c.getCitationKey().orElse(""))
                        .collect(Collectors.joining(","));
                } else {
                    FindCitedEntriesResult fcr = findCitedEntries(uniqueKeys, databases);
                    // entries contains UndefinedBibtexEntry for keys not found in databases
                    entries = new ArrayList<>();
                    for (Map.Entry<BibEntry, BibDatabase> kv : fcr.entries.entrySet()) {
                        entries.add(kv.getKey());
                    }
                    // Now we do not sort the entries here.
                    //
                    // TODO: the sorting skipped here should be done
                    // when creating the presentation. Check if it is there.
                    //
                    // entries.sort(new FieldComparator(StandardField.YEAR));
                    keyString =
                        entries.stream()
                        .map(OOBibBase::recoverCitationKeyFromPossiblyUndefinedBibEntry)
                        .collect(Collectors.joining(","));
                }

                // Insert reference mark:
                String newName =
                    getUniqueReferenceMarkName(
                        documentConnection,
                        keyString,
                        OOBibBase.AUTHORYEAR_PAR);

                insertReferenceMark(
                    documentConnection,
                    newName,
                    "tmp",
                    textCursor,
                    true, // withText
                    style,
                    false // insertSpaceAfter: no, it is already there (or could be)
                    );
            } // for gi

            madeModifications = (joinableGroups.size() > 0);

        } finally {
            if (useLockControllers) {
                documentConnection.unlockControllers();
            }
        }

        if (madeModifications) {
            // List<String> jabRefReferenceMarkNamesSortedByPosition =
            //    getJabRefReferenceMarkNamesSortedByPosition(documentConnection);
            ProduceCitationMarkersResult x =
                produceCitationMarkers(
                    documentConnection,
                    databases,
                    style
                    );
            try {
                if (useLockControllers) {
                    documentConnection.lockControllers();
                }
                // refreshCiteMarkers(databases, style, jabRefReferenceMarkNamesSortedByPosition);
                applyNewCitationMarkers(
                    documentConnection,
                    x.jabRefReferenceMarkNamesSortedByPosition,
                    x.citMarkers,
                    x.itcTypes,
                    style);
            } finally {
                if (useLockControllers) {
                    documentConnection.unlockControllers();
                }
            }
        }
        } finally {
            documentConnection.leaveUndoContext();
        }
    }

    /**
     * GUI action "Separate citations".
     *
     * Do the opposite of combineCiteMarkers.
     * Combined markers are split, with a space inserted between.
     */
    public void
    unCombineCiteMarkers(
        List<BibDatabase> databases,
        OOBibStyle style
        )
        throws
        IOException,
        WrappedTargetException,
        NoSuchElementException,
        IllegalArgumentException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        PropertyVetoException,
        CreationException,
        BibEntryNotFoundException,
        NoDocumentException,
        JabRefException,
        InvalidStateException {

        styleIsRequired(style);
        Objects.requireNonNull(databases);
        Objects.requireNonNull(style);

        final boolean useLockControllers = true;
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        try {
            documentConnection.enterUndoContext("Separate citations");
        boolean madeModifications = false;
        List<String> names =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);
        try {
            if (useLockControllers) {
                documentConnection.lockControllers();
            }

            int pivot = 0;
            boolean setCharStyleTested = false;
            XNameAccess nameAccess = documentConnection.getReferenceMarks();

            while (pivot < (names.size())) {
                XTextRange range1 =
                    DocumentConnection.asTextContent(
                        nameAccess.getByName(names.get(pivot)))
                    .getAnchor();

                XTextCursor textCursor =
                    range1.getText().createTextCursorByRange(range1);

                // If we are supposed to set character format for
                // citations, test this before making any changes. This
                // way we can throw an exception before any reference
                // marks are removed, preventing damage to the user's
                // document:
                if (!setCharStyleTested) {
                    assertCitationCharacterFormatIsOK(textCursor, style);
                    setCharStyleTested = true;
                }

                List<String> keys = parseRefMarkNameToUniqueCitationKeys(names.get(pivot));
                if (keys.size() <= 1) {
                    pivot++;
                    continue;
                }

                documentConnection.removeReferenceMark(names.get(pivot));

                // Insert bookmark for each key
                int last = keys.size() - 1;
                int i = 0;
                for (String key : keys) {
                    // Note: instead of generating a new name, we should explicitly
                    //       recover the original. Otherwise ...
                    String newName = getUniqueReferenceMarkName(
                        documentConnection,
                        key,
                        OOBibBase.AUTHORYEAR_PAR);

                    boolean insertSpaceAfter = (i != last);
                    insertReferenceMark(
                        documentConnection,
                        newName,
                        "tmp",
                        textCursor,
                        /* withText should be itcType != OOBibBase.INVISIBLE_CIT */
                        true,
                        style,
                        insertSpaceAfter
                        );
                    textCursor.collapseToEnd();
                    i++;
                }
                madeModifications = true;

                pivot++;
            }
        } finally {
            if (useLockControllers) {
                documentConnection.unlockControllers();
            }
        }
        if (madeModifications) {
            // List<String> jabRefReferenceMarkNamesSortedByPosition =
            //    getJabRefReferenceMarkNamesSortedByPosition(documentConnection);
            ProduceCitationMarkersResult x =
                produceCitationMarkers(
                    documentConnection,
                    databases,
                    style
                    );
            try {
                if (useLockControllers) {
                    documentConnection.lockControllers();
                }
                // refreshCiteMarkers(databases, style, jabRefReferenceMarkNamesSortedByPosition);
                applyNewCitationMarkers(
                    documentConnection,
                    x.jabRefReferenceMarkNamesSortedByPosition,
                    x.citMarkers,
                    x.itcTypes,
                    style);
            } finally {
                if (useLockControllers) {
                    documentConnection.unlockControllers();
                }
            }
        }
        } finally {
            documentConnection.leaveUndoContext();
        }
    }

    /**
     * Used from GUI: "Export cited"
     *
     * @param databases The databases to look up the citation keys in the document from.
     * @return A new database, with cloned entries.
     *
     * If a key is not found, it is silently ignored.
     *
     * Cross references (in StandardField.CROSSREF) are followed (not recursively):
     * if the referenced entry is found, it is included in the result.
     * If it is not found, it is silently ignored.
     */
    public BibDatabase
    generateDatabase(List<BibDatabase> databases)
        throws
        NoSuchElementException,
        WrappedTargetException,
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        BibDatabase resultDatabase = new BibDatabase();
        List<String> cited = findCitedKeys(documentConnection);
        List<BibEntry> entriesToInsert = new ArrayList<>();

        // For each cited key
        for (String key : cited) {
            // Loop through the available databases
            for (BibDatabase loopDatabase : databases) {
                Optional<BibEntry> entry = loopDatabase.getEntryByCitationKey(key);
                if (entry.isEmpty()) {
                    continue;
                }
                // If entry found
                BibEntry clonedEntry = (BibEntry) entry.get().clone();
                // Insert a copy of the entry
                entriesToInsert.add(clonedEntry);

                // Check if the cloned entry has a cross-reference field
                clonedEntry
                    .getField(StandardField.CROSSREF)
                    .ifPresent(crossReference -> {
                            // If the crossReference entry is not already in the database

                            // broken logic here:

                            // we just created resultDatabase, and
                            // added nothing yet.

                            // Question: why do we use
                            // entriesToInsert instead of directly adding to resultDatabase?
                            // And why do we not look for it in entriesToInsert?
                            // With the present code we are always adding it.
                            // How does BibDatabase handle this situation?
                            boolean isNew = (resultDatabase
                                             .getEntryByCitationKey(crossReference)
                                             .isEmpty());
                            if (isNew) {
                                // Add it if it is in the current library
                                loopDatabase
                                    .getEntryByCitationKey(crossReference)
                                    .ifPresent(entriesToInsert::add);
                            }
                        });

                    // Be happy with the first found BibEntry and move on to next key
                    break;
            }
            // key not found here. No action.
        }

        resultDatabase.insertEntries(entriesToInsert);
        return resultDatabase;
    }

    void styleIsRequired(OOBibStyle style)
        throws JabRefException {
        if (style == null) {
            throw new JabRefException(
                "This operation requires a style",
                Localization.lang("This operation requires a style.")
                + "\n"
                + Localization.lang("Please select one.")
                );
        }
    }

    /**
     * GUI action, refreshes citation markers and bibliography.
     *
     * @param databases Must have at least one.
     * @param style Style.
     * @return List of unresolved citation keys.
     *
     */
    public List<String>
    updateDocumentActionHelper(
        List<BibDatabase> databases,
        OOBibStyle style)
        throws
        NoSuchElementException,
        WrappedTargetException,
        IllegalArgumentException,
        CreationException,
        PropertyVetoException,
        UnknownPropertyException,
        UndefinedParagraphFormatException,
        NoDocumentException,
        UndefinedCharacterFormatException,
        BibEntryNotFoundException,
        IOException,
        JabRefException,
        InvalidStateException {

        styleIsRequired(style);

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        try {
            documentConnection.enterUndoContext("Refresh bibliography");

        boolean requireSeparation = false; // may loose citation without requireSeparation=true
        CitationGroups cgs = new CitationGroups(documentConnection);
        int maxReportedOverlaps = 10;
        checkRangeOverlaps(cgs, this.xDocumentConnection, requireSeparation, maxReportedOverlaps);
        final boolean useLockControllers = true;
        try {
            ProduceCitationMarkersResult x =
                produceCitationMarkers(
                    documentConnection,
                    databases,
                    style
                    );

            if (useLockControllers) {
                documentConnection.lockControllers();
            }
            applyNewCitationMarkers(
                documentConnection,
                x.jabRefReferenceMarkNamesSortedByPosition,
                x.citMarkers,
                x.itcTypes,
                style);
            rebuildBibTextSection(
                documentConnection,
                style,
                x.jabRefReferenceMarkNamesSortedByPosition,
                x.uniqueLetters,
                x.fce
                );
            return x.unresolvedKeys;
        } finally {
            if (useLockControllers) {
                documentConnection.unlockControllers();
            }
        }
        } finally {
            documentConnection.leaveUndoContext();
        }
    }

} // end of OOBibBase
