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

import org.jabref.architecture.AllowedToUseAwt;
import org.jabref.gui.DialogService;
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
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.Locale;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XBookmarksSupplier;
import com.sun.star.text.XDocumentIndexesSupplier;
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

    private static final String CHAR_STYLE_NAME = "CharStyleName";

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
    private final boolean atEnd;
    private final Comparator<BibEntry> entryComparator;
    private final Comparator<BibEntry> yearAuthorTitleComparator;

    /**
     * Document-connection related variables.
     */
    private static class DocumentConnection {
        public XMultiServiceFactory mxDocFactory;
        public XTextDocument mxDoc;
        public XText xText;
        public XTextViewCursorSupplier xViewCursorSupplier;
        public XComponent xCurrentComponent;
        public XPropertySet propertySet;
        public XPropertyContainer userProperties;
        private final Logger LOGGER;

        DocumentConnection(
            XMultiServiceFactory mxDocFactory,
            XTextDocument mxDoc,
            XText xText,
            XTextViewCursorSupplier xViewCursorSupplier,
            XComponent xCurrentComponent,
            XPropertySet propertySet,
            XPropertyContainer userProperties,
            Logger LOGGER
            ) {
            this.mxDocFactory = mxDocFactory;
            this.mxDoc = mxDoc;
            this.xText = xText;
            this.xViewCursorSupplier = xViewCursorSupplier;
            this.xCurrentComponent = xCurrentComponent;
            this.propertySet = propertySet;
            this.userProperties = userProperties;
            this.LOGGER = LOGGER;
        }

        /**
         *  @return True if we cannot reach the current document.
         */
        public boolean
        documentConnectionMissing() {

            // These are set by selectDocument, via DocumentConnection
            // constructor.
            if (null == this.xCurrentComponent
                || null == this.mxDoc
                || null == this.xViewCursorSupplier
                || null == this.xText
                || null == this.mxDocFactory
                || null == this.userProperties
                || null == this.propertySet) {
                return true;
            }

            // Attempt to check document is really available
            try {
                getReferenceMarks();
            } catch (NoDocumentException ex) {
                return true;
            }
            return false;
        }

        /**
         *  Get the title of the connected document.
         */
        public Optional<String>
        getDocumentTitle() {
            return OOBibBase.getDocumentTitle(this.mxDoc);
        }

        /**
         * @param property Name of a custom document property in the
         *        current document.
         *
         * @return The value of the property or Optional.empty()
         *
         * These properties are used to store extra data about
         * individual citation. In particular, the `pageInfo` part.
         *
         */
        private Optional<String>
        getCustomProperty(String property)
            throws
            UnknownPropertyException,
            WrappedTargetException {

            assert (this.propertySet != null);

            XPropertySetInfo psi = (this.propertySet
                                    .getPropertySetInfo());

            if (psi.hasPropertyByName(property)) {
                String v =
                    this.propertySet
                    .getPropertyValue(property)
                    .toString();
                return Optional.ofNullable(v);
            }
            return Optional.empty();
        }

        /**
         * @param property Name of a custom document property in the
         *        current document.
         *
         * @param value The value to be stored.
         *
         * @return The value of the property or Optional.empty()
         */
        private void
        setCustomProperty(String property, String value)
            throws
            UnknownPropertyException,
            NotRemoveableException,
            PropertyExistException,
            IllegalTypeException,
            IllegalArgumentException {

            XPropertySetInfo psi = this.propertySet.getPropertySetInfo();

            if (psi.hasPropertyByName(property)) {
                this.userProperties.removeProperty(property);
            }

            if (value != null) {
                this.userProperties
                    .addProperty(
                        property,
                        com.sun.star.beans.PropertyAttribute.REMOVEABLE,
                        new Any(Type.STRING, value));
            }
        }

        /**
         * @throws NoDocumentException If cannot get reference marks
         *
         * Note: also used by `documentConnectionMissing` to test if
         * we have a working connecion.
         *
         */
        private XNameAccess
        getReferenceMarks()
            throws NoDocumentException {

            XReferenceMarksSupplier supplier =
                unoQI(
                    XReferenceMarksSupplier.class,
                    this.xCurrentComponent
                    );
            try {
                return supplier.getReferenceMarks();
            } catch (DisposedException ex) {
                // LOGGER.warn("getReferenceMarks caught: ", ex);
                throw new NoDocumentException("getReferenceMarks failed with" + ex);
            }
        }

        /**
         * Names of all reference marks.
         *
         * Empty list for nothing.
         */
        List<String>
        getReferenceMarkNames()
            throws NoDocumentException {

            XNameAccess nameAccess = getReferenceMarks();
            String[] names = nameAccess.getElementNames();
            if (names == null) {
                return new ArrayList<>();
            }
            return Arrays.asList(names);
        }

        /**
         * Create a textcursor for a textContent.
         *
         * @return null if makr is null, otherwise cursor.
         */
        static XTextCursor
        getTextCursorOfTextContent(XTextContent mark) {
            if ( mark == null ){
                return null;
            }
            XTextRange markAnchor = mark.getAnchor();
            XTextCursor cursor =
                markAnchor.getText()
                .createTextCursorByRange(markAnchor);
            return cursor;
        }

        XTextContent
        nameAccessGetTextContentByName(XNameAccess nameAccess, String name)
            throws WrappedTargetException {

            if (!nameAccess.hasByName(name)) {
                return null;
            }
            try {
                Object referenceMark = nameAccess.getByName(name);
                XTextContent mark = unoQI(XTextContent.class, referenceMark);
                return mark;
            } catch (NoSuchElementException ex) {
                LOGGER.warn(String.format(
                                "nameAccessGetTextContentByName got NoSuchElementException"
                                + " for '%s'", name));
                return null;
            }
        }

        /**
         * Remove the named reference mark.
         *
         * Removes both the text and the mark itself.
         */
        public void
        removeReferenceMark(String name)
            throws
            WrappedTargetException,
            NoDocumentException,
            NoSuchElementException {

            XNameAccess xReferenceMarks = this.getReferenceMarks();

            if (xReferenceMarks.hasByName(name)) {
                XTextContent mark = nameAccessGetTextContentByName( xReferenceMarks, name );
                if ( mark == null ){
                    return;
                }
                this.xText.removeTextContent(mark);
            }
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
            String refMarkName,
            int charBefore,
            int charAfter,
            boolean htmlMarkup)
            throws
            NoSuchElementException,
            WrappedTargetException,
            NoDocumentException {

            XNameAccess nameAccess = getReferenceMarks();
            XTextContent mark = nameAccessGetTextContentByName(nameAccess, refMarkName);
            XTextCursor cursor = getTextCursorOfTextContent(mark);

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
         * Get the cursor positioned by the user.
         *
         */
        public XTextViewCursor
        getViewCursor() {
            return this.xViewCursorSupplier.getViewCursor();
        }

        /**
         * Provides access to bookmarks by name.
         */
        public XNameAccess
        getBookmarks() {
            // query XBookmarksSupplier from document model
            // and get bookmarks collection
            XBookmarksSupplier xBookmarksSupplier =
                unoQI(XBookmarksSupplier.class,
                      this.xCurrentComponent);
            return xBookmarksSupplier.getBookmarks();
        }

        /**
         * Get the XTextRange corresponding to the named bookmark.
         *
         * @param name The name of the bookmark to find.
         * @return The XTextRange for the bookmark, or null.
         */
        public XTextRange
        getBookmarkRange(String name)
            throws
            WrappedTargetException {

            XNameAccess xNamedBookmarks = this.getBookmarks();
            XTextContent xFoundBookmark =
                nameAccessGetTextContentByName( xNamedBookmarks, name );
            if (xFoundBookmark == null){
                return null;
            }
            return xFoundBookmark.getAnchor();
        }

        /**
         * Insert a bookmark with the given name at the cursor provided.
         *
         * @param name For the bookmark.
         * @param position Cursor marking the location or range for
         * the bookmark. If it is a range, its content will be replaced.
         */
        public void
        insertBookMark(
            String name,
            XTextCursor position)
            throws
            IllegalArgumentException,
            CreationException {

            Object bookmark;
            try {
                bookmark = (this.mxDocFactory
                            .createInstance("com.sun.star.text.Bookmark"));
            } catch (Exception e) {
                throw new CreationException(e.getMessage());
            }

            // name the bookmark
            XNamed xNamed = unoQI(XNamed.class, bookmark);
            xNamed.setName(name);

            // get XTextContent interface
            XTextContent xTextContent = unoQI(XTextContent.class, bookmark);

            // insert bookmark at position, overwrite text in position
            this.xText.insertTextContent(position, xTextContent, true);
        }

        /**
         *  Get an XTextSection by name.
         */
        public XTextSection
        getTextSectionByName(String name)
            throws
            NoSuchElementException,
            WrappedTargetException {

            XTextSectionsSupplier supplier =
                unoQI(XTextSectionsSupplier.class,
                      this.mxDoc);

            return ((XTextSection)
                    ((Any) supplier.getTextSections().getByName(name))
                    .getObject());
        }
    } // end DocumentConnection

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
     */
    private final Map<String, String> xUniqueLetters = new HashMap<>();

    /**
     * Names of reference marks belonging to JabRef sorted by visual
     * position.
     *
     */
    private List<String> jabRefReferenceMarkNamesSortedByPosition;

    /*
     * Constructor
     */
    public
    OOBibBase(Path loPath,
              boolean atEnd,
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

        this.atEnd = atEnd;
        this.xDesktop = simpleBootstrap(loPath);
    }

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

    /**
     *  Used by selectDocument
     */
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
     *  @param doc The XTextDocument we want the title for. Null allowed.
     *  @return The title or Optional.empty()
     */
    private static Optional<String>
    getDocumentTitle(XTextDocument doc) {

        if (doc == null) {
            return Optional.empty();
        }

        try {
            XFrame frame = doc.getCurrentController().getFrame();
            Object frameTitleObj = OOUtil.getProperty(frame, "Title");
            String frameTitleString = String.valueOf(frameTitleObj);
            return Optional.of(frameTitleString);
        } catch (UnknownPropertyException | WrappedTargetException e) {
            LOGGER.warn("Could not get document title", e);
            return Optional.empty();
        }
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
                this.description = OOBibBase.getDocumentTitle(xTextDocument).orElse("");
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
     * Assumes we have already connected to Libroffice or OpenOffice.
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

        XTextDocument mxDoc;

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
        mxDoc = selected;

        XComponent component = unoQI(XComponent.class, mxDoc);

        unoQI(XDocumentIndexesSupplier.class, component);

        XTextViewCursorSupplier viewCursorSupplier;

        XModel mo = unoQI(XModel.class, component);
        XController co = mo.getCurrentController();
        viewCursorSupplier = unoQI(XTextViewCursorSupplier.class, co);

        // get a reference to the body text of the document
        XText text = mxDoc.getText();

        // Access the text document's multi service factory:
        XMultiServiceFactory mxDocFactory = unoQI(XMultiServiceFactory.class, mxDoc);

        XPropertyContainer userProperties;

        XDocumentPropertiesSupplier supp =
            unoQI(XDocumentPropertiesSupplier.class, mxDoc);
        userProperties = supp.getDocumentProperties().getUserDefinedProperties();

        XPropertySet propertySet = unoQI(XPropertySet.class, userProperties);

        this.xDocumentConnection = new DocumentConnection(
            mxDocFactory,
            mxDoc,
            text,
            viewCursorSupplier,
            component,
            propertySet,
            userProperties,
            LOGGER
        );

        // TODO: maybe we should install an event handler for document
        // close: addCloseListener
        // Reference:
        // https://www.openoffice.org/api/docs/common/ref/com/sun/star/
        //         util/XCloseBroadcaster.html#addCloseListener
    }

    /**
     * Mark the current document as missing.
     *
     * TODO: GUI should be notified
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
     * Note: the sort is inplace, modifies the argument.
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
     * Get reference mark naems from the document matching the pattern
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
     * Get a list of CitationEntry objects corresponding to citations
     * in the document.
     *
     * @return A list with entries corresponding to citations in the
     *         text, in arbitrary order (same order as from
     *         getJabRefReferenceMarkNames).
     *
     *         TODO: Note: visual or alphabetic order could be more
     *               managable for the user. We could provide these
     *               here, but switching between them needs change on
     *               GUI (adding a toggle or selector).
     *
     *         Wish: selecting an entry in the GUI cold move cursor in
     *               the document.
     */
    public List<CitationEntry>
    getCitationEntries()
        throws
        NoSuchElementException,
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
                    documentConnection.getCitationContext(name, 30, 30, true),
                    documentConnection.getCustomProperty(name)
                    );
            citations.add(entry);
        }
        return citations;
    }

    /**
     * Apply editable parts of citationEntries to the document.
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
            // TODO: if pageInfo is not present, or is empty:
            // maybe we should remove it from the document.
        }
    }

    /**
     * Produce a reference mark name for JabRef for the given citation
     * key and itcType that does not yet appear among the reference
     * marks of the document.
     *
     * @param bibtexKey The citation key.
     * @param itcType   Encodes the effect of <code>withText</code> and
     *                  <code>inParenthesis</code> options.
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
     * This is what we get back from parsing a refMarkName
     *
     * TODO: We have one itcType per refMarkName. Merge reduces the
     * number of itcType values.
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
     * This method inserts a reference mark in the text (at the cursor)
     * citing the <code>entries</code>, and may refresh the bibliography.
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
     * @param pageInfo      A single pageinfo for these entries. Stored in custom property
     *                      with the same name as the reference mark.
     *
     *                      Related https://latex.org/forum/viewtopic.php?t=14331
     *
     *                      Q: What I would like is something like this:
     *                      (Jones, 2010, p. 12; Smith, 2003, pp. 21 - 23)
     *                      A: Not in a single \citep, no.
     *                         Use \citetext{\citealp[p.~12]{jones2010};
     *                                       \citealp[pp.~21--23]{smith2003}}
     *
     * @param sync          Indicates whether the reference list and in-text citations
     *                      should be refreshed in the document.
     *
     * TODO:
     * https://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XUndoManager.html
     * Group changes into a single Undo context.
     */
    public void
    insertEntry(
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
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        try {
            // Get the cursor positioned by the user.
            XTextCursor cursor = documentConnection.getViewCursor();

            sortBibEntryListForMulticite(entries, style);

            String keyString =
                entries.stream()
                .map(entry -> entry.getCitationKey().orElse(""))
                .collect(Collectors.joining(","));
            // Generate unique bookmark-name
            int citationType = citationTypeFromOptions(withText, inParenthesis);
            String newName = getUniqueReferenceMarkName(
                documentConnection,
                keyString,
                citationType);

            // If we should store metadata for page info, do that now:
            if (pageInfo != null) {
                LOGGER.info("Storing page info: " + pageInfo);
                documentConnection.setCustomProperty(newName, pageInfo);
            }

            // insert space
            cursor
                .getText()
                .insertString(cursor, " ", false);

            // format the space inserted
            // TOOD: extract applyCharacterStyle()
            if (style.isFormatCitations()) {
                XPropertySet xCursorProps = unoQI(XPropertySet.class, cursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch (UnknownPropertyException
                        | PropertyVetoException
                        | IllegalArgumentException
                        | WrappedTargetException ex
                ) {
                    // Setting the character format failed, so we
                    // throw an exception that will result in an
                    // error message for the user.

                    // Before that, delete the space we inserted:
                    cursor.goLeft((short) 1, true);
                    cursor.setString("");
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }

            // go back to before the space
            cursor.goLeft((short) 1, false);

            // Insert rerefence mark and text
            //{
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
                    ? "-" // A dash only. Presumably we expect a refresh later.
                    : style.getCitationMarker(
                        entries,
                        databaseMap,
                        inParenthesis,
                        null,
                        null);
                insertReferenceMark(documentConnection, newName, citeText,
                                    cursor, withText, style);
             //} // end of scope for databaseMap, citeText

            // Move to the right of the space and remember this
            // position: we will come back here in the end.
            cursor.collapseToEnd();
            cursor.goRight((short) 1, false);
            XTextRange position = cursor.getEnd();

            if (sync) {
                // To account for numbering and for uniqueLetters, we
                // must refresh the cite markers:
                updateSortedReferenceMarks();
                refreshCiteMarkers(allBases, style);

                // Insert it at the current position:
                rebuildBibTextSection(allBases, style);

                /*
                 * TODO: inserting a reference in the "References" section
                 * provokes an "Unknown Source" exception here, because
                 * position was deleted by rebuildBibTextSection()
                 *
                 * at com.sun.proxy.$Proxy44.gotoRange(Unknown Source)
                 * at org.jabref@100.0.0/org.jabref.gui.openoffice
                 *      .OOBibBase.insertEntry(OOBibBase.java:609)
                 *
                 * Maybe we should refuse to insert in places to be
                 * overwritten: bibliography, reference marks.
                 *
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
        }
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
     * Extract citation keys from names of referenceMarks in the document.
     *
     * Each citation key is listed only once, in the order of first appearance.
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
            assert (null != unoQI(XTextContent.class, bookmark));
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

    /**
     * Refresh all citation markers in the document.
     *
     * @param databases The databases to get entries from.
     * @param style     The bibliography style to use.
     * @return A list of those referenced citation keys that could not be resolved.
     */
    public List<String>
    refreshCiteMarkers(
        List<BibDatabase> databases,
        OOBibStyle style)
        throws
        WrappedTargetException,
        IllegalArgumentException,
        NoSuchElementException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        PropertyVetoException,
        IOException,
        CreationException,
        BibEntryNotFoundException,
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        try {
            return
                refreshCiteMarkersInternal(
                    documentConnection,
                    databases,
                    style,
                    this.xUniqueLetters);
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO JAR file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    /**
     * @return Null if cEntry is an UndefinedBibtexEntry,
     *         otherwise return cEntry itself.
     */
    private static BibEntry
    undefinedBibentryToNull(BibEntry cEntry) {
        if (cEntry instanceof UndefinedBibtexEntry) {
            return null;
        }
        return cEntry;
    }

    /**
     *  @return A copy of the input with UndefinedBibtexEntry
     *          instances replaced with null.
     */
    private static BibEntry[]
    mapUndefinedBibEntriesToNull(BibEntry[] cEntries) {
        return
            Arrays.stream(cEntries)
            .map(OOBibBase::undefinedBibentryToNull)
            .toArray(BibEntry[]::new);
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
        String referenceMarkName // for reporting
    )
        throws BibEntryNotFoundException {

        // check keys
        List<String> unresolvedKeys =
            Arrays.stream(keys)
            .filter(key -> null == citeKeyToBibEntry.get(key))
            .collect(Collectors.toList());

        for (String key : unresolvedKeys) {
            LOGGER.info("assertKeysInCiteKeyToBibEntry: Citation key not found: '" + key + '\'');
            LOGGER.info("Problem with reference mark: '" + referenceMarkName + '\'');
            String msg =
                Localization.lang(
                    "Could not resolve BibTeX entry"
                    + " for citation marker '%0'.",
                    referenceMarkName );
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
        Map<String, BibEntry> citeKeyToBibEntry
        )
        throws BibEntryNotFoundException {

        final int nRefMarks = referenceMarkNames.size();
        assert (nRefMarks == bibtexKeys.length);

        for (int i = 0; i < nRefMarks; i++) {
            assertKeysInCiteKeyToBibEntry(
                bibtexKeys[i],
                citeKeyToBibEntry,
                referenceMarkNames.get(i));
        }
    }

    // TODO: is mapCiteKeysToBibEntryArray is still needed?
    //       We still have a call site, followed by mapUndefinedBibEntriesToNull
    /**
     * Map an array of citation keys to the corresponding BibEntry
     * objects using citeKeyToBibEntry.
     *
     * @param citeKeys
     * @param citeKeyToBibEntry
     * @param referenceMarkName For reporting keys missing from citeKeyToBibEntry.
     */
    private static BibEntry[]
    mapCiteKeysToBibEntryArray(
        String[] citeKeys,
        Map<String, BibEntry> citeKeyToBibEntry,
        String referenceMarkName
        )
        throws BibEntryNotFoundException {

        assertKeysInCiteKeyToBibEntry(citeKeys, citeKeyToBibEntry, referenceMarkName);

        return
            Arrays.stream(citeKeys)
            .map(citeKeyToBibEntry::get)
            .toArray(BibEntry[]::new);
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
        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeys, citeKeyToBibEntry);

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
     * Number source for (1-based) numbering of citations.
     *
     * For numbered citation style with first appearance order.
     */
    private static class CitationNumberingState {
        /**
         * numbers : Remembers keys we have seen
         *           and what number did they receive.
         */
        private Map<String, Integer> numbers;

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
     * BibEntry.getCitationKey() must not be Optional.empty().
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
        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeys, citeKeyToBibEntry);

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

        // Sort entries to order in bibliography
        Map<BibEntry, BibDatabase> sortedEntries =
            sortEntriesByComparator(entries, entryComparator);

        // Adjust order of cited to match sortedEntries
        List<String> sortedCited = new ArrayList<>(entries.size());
        for (BibEntry entry : sortedEntries.keySet()) {
            sortedCited.add(entry.getCitationKey().orElse(null));
        }

        final int minGroupingCount =
            style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);

        for (int i = 0; i < referenceMarkNames.size(); i++) {
            List<Integer> numbers =
                findCitedEntryIndices(Arrays.asList(bibtexKeys[i]), sortedCited);
            citMarkers[i] = style.getNumCitationMarker(numbers, minGroupingCount, false);
        }
        return citMarkers;
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

    /**
     * Visit each reference mark in referenceMarkNames, remove its
     * text content, call insertReferenceMark.
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
        NoSuchElementException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        CreationException,
        WrappedTargetException,
        PropertyVetoException {

        final int nRefMarks = referenceMarkNames.size();
        assert (citMarkers.length == nRefMarks);
        assert (types.length == nRefMarks);

        XNameAccess nameAccess =
            documentConnection.getReferenceMarks();

        final boolean hadBibSection =
            (documentConnection.getBookmarkRange(OOBibBase.BIB_SECTION_NAME) != null);

        // If we are supposed to set character format for citations,
        // must run a test before we delete old citation
        // markers. Otherwise, if the specified character format
        // doesn't exist, we end up deleting the markers before the
        // process crashes due to a the missing format, with
        // catastrophic consequences for the user.
        boolean mustTestCharFormat = style.isFormatCitations();

        for (int i = 0; i < nRefMarks; i++) {
            final String name = referenceMarkNames.get(i);

            XTextContent mark = documentConnection.nameAccessGetTextContentByName(nameAccess, name);
            XTextCursor cursor = documentConnection.getTextCursorOfTextContent(mark);

            if (mustTestCharFormat) {
                mustTestCharFormat = false; // need to do this only once
                testFormatCitations(cursor, style);
            }

            documentConnection.xText.removeTextContent(mark);

            insertReferenceMark(
                documentConnection,
                name,
                citMarkers[i],
                cursor,
                types[i] != OOBibBase.INVISIBLE_CIT,
                style
                );

            if (hadBibSection
                && (documentConnection.getBookmarkRange(OOBibBase.BIB_SECTION_NAME) == null)) {
                // We have overwritten the marker for the start of the reference list.
                // We need to add it again.
                cursor.collapseToEnd();
                OOUtil.insertParagraphBreak(documentConnection.xText, cursor);
                documentConnection.insertBookMark(OOBibBase.BIB_SECTION_NAME, cursor);
                cursor.collapseToEnd();
            }
        }
    }

    /**
     * For each name in referenceMarkNames set types[i] and
     * bibtexKeys[i] to values parsed from referenceMarkNames.get(i)
     *
     * @param referenceMarkNames Should only contain parsable names.
     * @param types              OUT Must be same length as referenceMarkNames.
     * @param bibtexKeys         OUT First level must be same length as referenceMarkNames.
     */
    private static void parseRefMarkNamesToArrays(List<String> referenceMarkNames,
                                                  int[] types,
                                                  String[][] bibtexKeys) {
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
                assert false : "parseRefMarkNamesToArrays expects parsable referenceMarkNames";
                continue;
            }
            ParsedRefMark ov = op.get();
            types[i] = ov.itcType;
            bibtexKeys[i] = ov.citedKeys.toArray(String[]::new);
        }
    }

    private static List<String> unresolvedKeysFromEntries(Map<BibEntry, BibDatabase> entries) {
        // Collect and return unresolved citation keys.
        // uses: entries
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


    /**
     * Produce citMarkers for normal (!isCitationKeyCiteMarkers && ! isNumberEntries) styles.
     *
     * @param referenceMarkNames Names of reference marks.
     * @param bibtexKeysIn       Bibtex citation keys.
     * @param citeKeyToBibEntry  Maps citation keys to BibEntry.
     * @param itcTypes           Citation types.
     * @param entries            Map BibEntry to BibDatabase.
     * @param uniqueLetters      Filled with new values here.
     * @param style              Bibliography style.
     */
    String[]
    produceCitationMarkersForNormalStyle(List<String> referenceMarkNames,
                                     String[][] bibtexKeysIn,
                                     Map<String, BibEntry> citeKeyToBibEntry,
                                     int[] itcTypes,
                                     Map<BibEntry, BibDatabase> entries,
                                     final Map<String, String> uniqueLetters,
                                     OOBibStyle style
    )
            throws BibEntryNotFoundException {
        uniqueLetters.clear();

        assert !style.isCitationKeyCiteMarkers();
        assert !style.isNumberEntries();
        // Citations in (Au1, Au2 2000) form

        final int nRefMarks = referenceMarkNames.size();
        assert (bibtexKeysIn.length == nRefMarks);
        assert (itcTypes.length == nRefMarks);
        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeysIn, citeKeyToBibEntry);

        BibEntry[][] cEntriesForAll =
                Arrays.stream(bibtexKeysIn)
                      .map(bibtexKeysOfAReferenceMark ->
                              Arrays.stream(bibtexKeysOfAReferenceMark)
                                    .map(citeKeyToBibEntry::get)
                                    .sorted(comparatorForMulticite(style)) // sort within referenceMark
                                    .toArray(BibEntry[]::new)
                      )
                      .toArray(BibEntry[][]::new);

        // Update bibtexKeys to match the new sorting (within each referenceMark)
        String[][] bibtexKeys =
                Arrays.stream(cEntriesForAll)
                      .map(cEntries ->
                              Arrays.stream(cEntries)
                                    .map(ce -> ce.getCitationKey().orElse(null))
                                    .toArray(String[]::new)
                      )
                      .toArray(String[][]::new);

        assertAllKeysInCiteKeyToBibEntry(referenceMarkNames, bibtexKeysIn, citeKeyToBibEntry);
        assert (bibtexKeys.length == nRefMarks);

        String[] citMarkers = new String[nRefMarks];
        for (int i = 0; i < nRefMarks; i++) {
            BibEntry[] cEntries = cEntriesForAll[i];
            int type = itcTypes[i];
            citMarkers[i] = style.getCitationMarker(Arrays.asList(cEntries), // entries
                    entries, // database
                    type == OOBibBase.AUTHORYEAR_PAR,
                    null,
                    null
            );
        }

        //    normCitMarkers[i][j] = for unification
        String[][] normCitMarkers = new String[nRefMarks][];
        for (int i = 0; i < nRefMarks; i++) {
            BibEntry[] cEntries = cEntriesForAll[i];
            // We need "normalized" (in parenthesis) markers
            // for uniqueness checking purposes:
            normCitMarkers[i] =
                    Arrays.stream(cEntries)
                          .map(ce ->
                                  style.getCitationMarker(
                                          Collections.singletonList(ce),
                                          entries,
                                          true,
                                          null,
                                          new int[] {-1} // no limit on authors
                                  )
                          )
                          .toArray(String[]::new);
        }

        uniqueLetters.clear();

        // The following block
        // changes: citMarkers[i], uniqueLetters
        // uses: nRefMarks, normCitMarkers, bibtexKeys,
        //       style (style.getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST))
        //       citeKeyToBibEntry, entries, types

        if (!style.isCitationKeyCiteMarkers() && !style.isNumberEntries()) {
            // Only for normal citations. Numbered citations and
            // citeKeys are already unique.

            // See if there are duplicate citations marks referring to
            // different entries. If so, we need to use uniqueLetters:

            // refKeys: normCitMarker to list of bibtexkeys sharing it.
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

            // Finally, go through all citation markers, and update
            // those referring to entries in our current list:
            final int maxAuthorsFirst = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST);
            Set<String> seenBefore = new HashSet<>();

            for (int i = 0; i < nRefMarks; i++) {
                final String referenceMarkName = referenceMarkNames.get(i);
                final int nCitedEntries = bibtexKeys[i].length;
                boolean needsChange = false;
                int[] firstLimAuthors = new int[nCitedEntries];
                String[] uniqueLetterForCitedEntry = new String[nCitedEntries];

                assertKeysInCiteKeyToBibEntry(bibtexKeys[i], citeKeyToBibEntry, referenceMarkName);
                BibEntry[] cEntries =
                    Arrays.stream(bibtexKeys[i])
                    .map(citeKeyToBibEntry::get)
                    .toArray(BibEntry[]::new);

                cEntries =
                    Arrays.stream(cEntries)
                    .map(OOBibBase::undefinedBibentryToNull)
                    .toArray(BibEntry[]::new);


                for (int j = 0; j < nCitedEntries; j++) {
                    String currentKey = bibtexKeys[i][j];

                    // firstLimAuthors will be (-1) except at the first
                    // refMark it appears at, where a positive maxAuthorsFirst
                    // may override. This is why:
                    // https://discourse.jabref.org/t/
                    //    number-of-authors-in-citations-style-libreoffice/747/3
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
                        if (uniqueLetterForKey == null) {
                            uniqueLetterForCitedEntry[j] = "";
                        } else {
                            uniqueLetterForCitedEntry[j] = uniqueLetterForKey;
                            needsChange = true;
                        }

                    if (firstLimAuthors[j] > 0) {
                        needsChange = true;
                    }
                } // for j

                if (needsChange) {
                    citMarkers[i] =
                            style.getCitationMarker(Arrays.asList(cEntries),
                                    entries,
                                    itcTypes[i] == OOBibBase.AUTHORYEAR_PAR,
                                    uniqueLetterForCitedEntry,
                                    firstLimAuthors
                            );
                }
            } // for i
        } // if normalStyle
        return citMarkers;
    }

    private List<String>
    refreshCiteMarkersInternal(DocumentConnection documentConnection,
                               List<BibDatabase> databases,
                               OOBibStyle style,
                               final Map<String, String> uniqueLetters)
        throws
        WrappedTargetException,
        IllegalArgumentException,
        NoSuchElementException,
        UndefinedCharacterFormatException,
        UnknownPropertyException,
        PropertyVetoException,
        CreationException,
        BibEntryNotFoundException,
        NoDocumentException {

        // Normally we sort the reference marks according to their
        // order of appearance:
        List<String> referenceMarkNames = jabRefReferenceMarkNamesSortedByPosition;

        // Compute citation markers for all citations:
        final int nRefMarks = referenceMarkNames.size();

        // fill:
        //    types[i]      = ov.itcType
        //    bibtexKeys[i] = ov.citedKeys.toArray()
        int[] types = new int[nRefMarks];
        String[][] bibtexKeys = new String[nRefMarks][];
        parseRefMarkNamesToArrays(referenceMarkNames, types, bibtexKeys);

        FindCitedEntriesResult fce =
                findCitedEntries(
                    findCitedKeys(documentConnection),
                    databases
                    // TODO: why are we scanning the document
                    // if we already hev the referenceMarkNames?
                );
        // fce.entries are in same order as returned by findCitedKeys

        // citMarkers[i] = what goes in the text at referenceMark[i]
        String[] citMarkers;

        // fill citMarkers
        uniqueLetters.clear(); /* ModifiesParameter */
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

            citMarkers =
                produceCitationMarkersForNormalStyle(
                    referenceMarkNames,
                    bibtexKeys,
                    fce.citeKeyToBibEntry,
                    types,
                    fce.entries,
                    uniqueLetters,
                    style);

        }

        // Refresh all reference marks with the citation markers we computed:
        applyNewCitationMarkers(
            documentConnection,
            referenceMarkNames,
            citMarkers,
            types,
            style);

        return unresolvedKeysFromEntries(fce.entries);
    }

    // Position as in a document on the screen.
    // Probably to get the correct order with
    // referenceMarks in footnotes
    private static Point
    findPositionOfTextRange(XTextViewCursor cursor, XTextRange range) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
        // the cursor's coordinates relative to the top left position
        // of the first page of the document.
    }

    /**
     *
     */
    private List<String>
    getJabRefReferenceMarkNamesSortedByPosition(
        DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        NoSuchElementException,
        NoDocumentException {

        List<String> names = getJabRefReferenceMarkNames(documentConnection);

        // find coordinates
        List<Point> positions = new ArrayList<>(names.size());

        XNameAccess nameAccess = documentConnection.getReferenceMarks();
        XTextViewCursor viewCursor = documentConnection.getViewCursor();
        // initialPos: to be restored before return
        XTextRange initialPos = viewCursor.getStart();
        for (String name : names) {

            XTextContent textContent =
                //documentConnection.nameAccessGetTextContentByName( nameAccess, name );
                unoQI(XTextContent.class, nameAccess.getByName(name));
            XTextRange range = textContent.getAnchor();

            // Adjust range if we are inside a footnote:
            if (unoQI(XFootnote.class, range.getText()) != null) {
                // Find the linking footnote marker:
                XFootnote footer = unoQI(XFootnote.class, range.getText());
                // The footnote's anchor gives the correct position in the text:
                range = footer.getAnchor();
            }
            positions.add(findPositionOfTextRange(viewCursor, range));
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

    public void updateSortedReferenceMarks()
            throws WrappedTargetException,
            NoSuchElementException,
            NoDocumentException {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        this.jabRefReferenceMarkNamesSortedByPosition =
                getJabRefReferenceMarkNamesSortedByPosition(documentConnection);
    }

    /**
     * GUI action
     *
     * @return unresolvedKeys
     */
    public List<String> updateDocumentActionHelper(
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
        IOException {
        updateSortedReferenceMarks();
        List<String> unresolvedKeys = refreshCiteMarkers(databases, style);
        rebuildBibTextSection(databases, style);
        return unresolvedKeys;
    }

    public void rebuildBibTextSection(
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
        NoDocumentException {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        FindCitedEntriesResult fce =
                findCitedEntries(
                    findCitedKeys(documentConnection),
                    databases
                );

        Map<BibEntry, BibDatabase> entries;

        if (style.isSortByPosition()) {
            // We need to sort the entries according to their order of appearance:
            entries = sortEntriesByRefMarkNames(
                    jabRefReferenceMarkNamesSortedByPosition,
                    fce.citeKeyToBibEntry,
                    fce.entries
            );
        } else {
            entries = sortEntriesByComparator(fce.entries, entryComparator);
        }
        clearBibTextSectionContent2(documentConnection);
        populateBibTextSection(documentConnection, entries, style, this.xUniqueLetters);
    }

    SortedMap<BibEntry, BibDatabase>
    sortEntriesByComparator(Map<BibEntry, BibDatabase> entries,
                            Comparator<BibEntry> entryComparator) {
        SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
        for (Map.Entry<BibEntry, BibDatabase> kv : entries.entrySet()) {
            newMap.put(kv.getKey(),
                    kv.getValue());
        }
        return newMap;
    }

    /**
     * @param referenceMarkNames Names of reference marks.
     * @param citeKeyToBibEntry  Helps to find the entries
     * @return LinkedHashMap from BibEntry to BibDatabase with
     * iteration order as first appearance in referenceMarkNames.
     */
    private Map<BibEntry, BibDatabase>
    sortEntriesByRefMarkNames(List<String> referenceMarkNames,
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

    /**
     * Only called from populateBibTextSection (and that from rebuildBibTextSection)
     */
    private void insertFullReferenceAtCursor(DocumentConnection documentConnection,
                                             XTextCursor cursor,
                                             Map<BibEntry, BibDatabase> entries,
                                             OOBibStyle style,
                                             String parFormat,
                                             final Map<String, String> uniqueLetters)
            throws UndefinedParagraphFormatException,
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
                // NOte: minGroupingCount is pointless here, we are
                // formatting a single entry.
                // int minGroupingCount = style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);
                int minGroupingCount = 2;
                List<Integer> numbers = Collections.singletonList(number++);
                String marker = style.getNumCitationMarker(numbers,
                        minGroupingCount,
                        true);

                OOUtil.insertTextAtCurrentLocation(documentConnection.xText,
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
                    uniqueLetters.get(entry.getKey().getCitationKey().orElse(null))
            );
        }
    }

    private void
    createAndInsertSection(
        DocumentConnection documentConnection,
        String sectionName,
        XTextCursor textCursor
        )
        throws
        IllegalArgumentException,
        CreationException {

        // Create a new TextSection from the document factory
        // and access it's XNamed interface
        XNamed xChildNamed;
        try {
            xChildNamed =
                unoQI(XNamed.class,
                      (documentConnection.mxDocFactory
                       .createInstance("com.sun.star.text.TextSection")));
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // Set the new sections name to 'Child_Section'
        xChildNamed.setName(sectionName);
        // Access the Child_Section's XTextContent interface and insert it into the document
        XTextContent xChildSection = unoQI(XTextContent.class, xChildNamed);
        documentConnection.xText.insertTextContent(textCursor, xChildSection, false);
    }

    private void createBibTextSection2(DocumentConnection documentConnection,
                                       boolean end)
            throws IllegalArgumentException,
            CreationException {

        XTextCursor textCursor = documentConnection.xText.createTextCursor();
        if (end) {
            textCursor.gotoEnd(false);
        }
        // where does textCursor point to if end is false?
        // TODO: are we using this.atEnd == false?
        // If we do, what happens (or expected to happen) here?

        OOUtil.insertParagraphBreak(documentConnection.xText, textCursor);
        createAndInsertSection(
            documentConnection,
            OOBibBase.BIB_SECTION_NAME,
            textCursor
            );
    }

    private XNameAccess
    getTextSections(DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        IllegalArgumentException,
        CreationException  {
        // Check if the section exists:
        XTextSectionsSupplier supplier =
            unoQI(XTextSectionsSupplier.class, documentConnection.mxDoc);

        return supplier.getTextSections();
    }

    private void clearBibTextSectionContent2(DocumentConnection documentConnection)
        throws
        WrappedTargetException,
        IllegalArgumentException,
        CreationException {

        XNameAccess ts = getTextSections( documentConnection );
        if (!ts.hasByName(OOBibBase.BIB_SECTION_NAME)) {
            createBibTextSection2(documentConnection, this.atEnd);
            return;
        }

        try {
            Any a = ((Any) ts.getByName(OOBibBase.BIB_SECTION_NAME));
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

            // We got this exception from ts.getByName() despite
            // the ts.hasByName() check just above.

            // Try to create.
            LOGGER.warn("Could not get section '" + OOBibBase.BIB_SECTION_NAME + "'", ex);
            createBibTextSection2(documentConnection, this.atEnd);
        }
    }

    /**
     * Only called from: rebuildBibTextSection
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

        documentConnection.insertBookMark(OOBibBase.BIB_SECTION_END_NAME, cursor);
        cursor.collapseToEnd();
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

    /**
     *  Insert a reference mark.
     *
     * @param documentConnection Connection to a document.
     * @param name Name of the reference mark to be created and also
     *        the name of the custom property holding the pageInfo part.
     */
    private void insertReferenceMark(DocumentConnection documentConnection,
                                     String name,
                                     String citationText,
                                     XTextCursor position,
                                     boolean withText,
                                     OOBibStyle style)
            throws UnknownPropertyException,
            WrappedTargetException,
            PropertyVetoException,
            IllegalArgumentException,
            UndefinedCharacterFormatException,
            CreationException {
        // TODO: last minute editing is hacky. Move pageInfo stuff to
        //  citation marker generation.
        // If there is "page info" for this citation, insert it into
        // the citation text before inserting the citation:
        String citText;
        String pageInfo =
                getPageInfoForReferenceMarkName(documentConnection, name);
        citText =
            pageInfo.isEmpty()
            ? citationText
            : style.insertPageInfo(citationText, pageInfo);

        Object bookmark;
        try {
            bookmark =
                    documentConnection.mxDocFactory
                            .createInstance("com.sun.star.text.ReferenceMark");
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // Name the reference
        XNamed xNamed = unoQI(XNamed.class, bookmark);
        xNamed.setName(name);

        if (withText) {
            position.setString(citText);
            XPropertySet xCursorProps = unoQI(XPropertySet.class, position);

            // Set language to [None]:
            xCursorProps.setPropertyValue("CharLocale", new Locale("zxx", "", ""));
            if (style.isFormatCitations()) {
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch (UnknownPropertyException
                        | PropertyVetoException
                        | IllegalArgumentException
                        | WrappedTargetException ex) {
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }
        } else {
            position.setString("");
        }

        // get XTextContent interface
        XTextContent xTextContent = unoQI(XTextContent.class, bookmark);

        position.getText().insertTextContent(position, xTextContent, true);

        // Are we sure that OOBibStyle.ET_AL_STRING cannot be part of author names
        // in any language?
        // TODO: could we move italicizing "et al." to a more proper place?
        // Check if we should italicize the "et al." string in citations:
        boolean italicize = style.getBooleanCitProperty(OOBibStyle.ITALIC_ET_AL);
        if (italicize) {
            String etAlString = style.getStringCitProperty(OOBibStyle.ET_AL_STRING);
            int index = citText.indexOf(etAlString);
            if (index >= 0) {
                italicizeRangeFromPosition(position, index, index + etAlString.length());
            }
        }

        position.collapseToEnd();
    }

    /**
     * Taking ref=position.getStart(), italicize the range (ref+start,ref+end)
     *
     * @param position  : position.getStart() is out reference point.
     * @param start     : start of range to italicize w.r.t position.getStart().
     * @param end       : end of range  to italicize w.r.t position.getStart().
     *
     *  Why this API?  This is used after finding "et al." string in a
     *  citation marker.
     */
    private void italicizeRangeFromPosition(XTextCursor position,
                                            int start,
                                            int end)
        throws
        UnknownPropertyException,
        PropertyVetoException,
        IllegalArgumentException,
        WrappedTargetException {
        XTextRange range = position.getStart();
        XTextCursor cursor = position.getText().createTextCursorByRange(range);
        cursor.goRight((short) start, false);
        cursor.goRight((short) (end - start), true);

        XPropertySet xcp = unoQI(XPropertySet.class, cursor);
        xcp.setPropertyValue("CharPosture", com.sun.star.awt.FontSlant.ITALIC);
        // xcp.setPropertyValue("CharWeight", com.sun.star.awt.FontWeight.BOLD);
    }

    /**
     * Look up a single citation key in a list of databases.
     *
     * @param key Citation key to look up.
     * @param databases Key is looked up in these, in this order.
     * @return The BibEntry at the first match, or Optional.empty().
     */
    Optional<BibEntry> lookupEntryInDatabases(String key, List<BibDatabase> databases) {
        for (BibDatabase database : databases) {
            Optional<BibEntry> entry = database.getEntryByCitationKey(key);
            if (entry.isPresent()) {
                return entry;
            }
        }
        return Optional.empty();
    }

    /**
     * @param keys Citation keys to look up.
     * @param databases Keys are looked up in these, in this order.
     * @return The BibEntry objects found.
     *
     * The order of keys is kept in the result, but unresolved keys
     * have no representation in the result, so result.get(i) does not
     * necessarily belong to keys.get(i)
     *
     */
    List<BibEntry> lookupEntriesInDatabasesSkipMissing(List<String> keys,
                                                       List<BibDatabase> databases) {
        List<BibEntry> entries = new ArrayList<>();
        for (String key : keys) {
            lookupEntryInDatabases(key, databases).ifPresent(entries::add);
        }
        return entries;
    }

    private void testFormatCitations(XTextCursor textCursor, OOBibStyle style)
            throws UndefinedCharacterFormatException {
        XPropertySet xCursorProps = unoQI(XPropertySet.class, textCursor);
        String charStyle = style.getCitationCharacterFormat();
        try {
            xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
        } catch (UnknownPropertyException
                | PropertyVetoException
                | IllegalArgumentException
                | WrappedTargetException ex) {
            // Setting the character format failed, so we throw an exception that
            // will result in an error message for the user:
            throw new UndefinedCharacterFormatException(charStyle);
        }
    }

    /**
     * GUI action
     *
     * combineCiteMarkers does not work with citations in footnotes.
     *
     * Note: citations can be inserted in footnotes and they appear in
     *       the bibliography. They are also updated on style change+refresh
     *
     *       The same (insertable, appears in bibliography, update on
     *       style change) is true for citations in tables.
     *
     *       Merge (combineCiteMarkers) and Separate (unCombineCiteMarkers)
     *       seem to work in a cell of a table.
     *
     *       In footnotes: "Separate" (on merged citations inserted by
     *       selecting multiple entries then "Cite") leaves first of
     *       two citation marks with text "tmp", which can be
     *       corrected by a few repetions of pressing the "refresh"
     *       button.
     *       With 3 citations, "Separate" left the 2nd and 3rd as "tmp".
     *       Three refresh corrected the 2nd.  The 4th refresh corrected the 3rd citation.
     *
     */
    public void combineCiteMarkers(List<BibDatabase> databases, OOBibStyle style)
            throws IOException,
            WrappedTargetException,
            NoSuchElementException,
            IllegalArgumentException,
            UndefinedCharacterFormatException,
            UnknownPropertyException,
            PropertyVetoException,
            CreationException,
            BibEntryNotFoundException,
            NoDocumentException {
        DocumentConnection documentConnection = this.getDocumentConnectionOrThrow();

        List<String> names =
                getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                documentConnection.xText);

        int pivot = 0;
        boolean madeModifications = false;
        XNameAccess nameAccess = documentConnection.getReferenceMarks();

        while (pivot < (names.size() - 1)) {

            XTextRange range1 =
                    unoQI(XTextContent.class,
                            nameAccess.getByName(names.get(pivot)))
                            .getAnchor()
                            .getEnd();

            XTextRange range2 =
                    unoQI(XTextContent.class,
                            nameAccess.getByName(names.get(pivot + 1)))
                            .getAnchor()
                            .getStart();

            if (range1.getText() != range2.getText()) {
                /* pivot and (pivot+1) belong to different XText entities?
                 * Maybe to different footnotes?
                 * Cannot combine across boundaries skip.
                 */
                pivot++;
                continue;
            }

            // Start from end of text for pivot.
            XTextCursor textCursor =
                    range1.getText().createTextCursorByRange(range1);

            // Select next character, and more, as long as we can and
            // do not reach stat of (pivot+1), which we now know to be
            // under the same XText entity.
            textCursor.goRight((short) 1, true);
            boolean couldExpand = true;
            while (couldExpand && (compare.compareRegionEnds(textCursor, range2) > 0)) {
                couldExpand = textCursor.goRight((short) 1, true);
            }

            // Take what we selected
            String cursorText = textCursor.getString();

            // Check if the string contains line breaks and any  non-whitespace.
            if ((cursorText.indexOf('\n') != -1) || !cursorText.trim().isEmpty()) {
                pivot++;
                continue;
            }

            // If we are supposed to set character format for
            // citations, test this before making any changes. This
            // way we can throw an exception before any reference
            // marks are removed, preventing damage to the user's
            // document:
            if (style.isFormatCitations()) {
                testFormatCitations(textCursor, style);
            }

            /*
             * This only gets the keys: itcType is discarded.
             *
             * AUTHORYEAR_PAR:   "(X and Y 2000)"
             * AUTHORYEAR_INTEXT: "X and Y (2000)"
             * INVISIBLE_CIT: ""
             *
             * TODO: We probably only want to collect citations with
             *       AUTHORYEAR_PAR itcType.
             *
             *       No, "X and Y (2000,2001)" appears a meaningful
             *       case as well.
             *
             *       Proposed rules:
             *       (1) Do not combine citations with different itcType
             *       (2) INVISIBLE_CIT: leave it alone
             *       (3) AUTHORYEAR_PAR: combine, present as AUTHORYEAR_PAR
             *
             *       (4) AUTHORYEAR_INTEXT: Same list of authors with
             *           same or different years, possibly with
             *           uniqueLetters could be done.
             *           But with different list of authors?
             *           "(X, Y et al 2000) (X, Y et al 2001)"
             *           will depend on authors not shown here.
             *
             */
            List<String> keys =
                    parseRefMarkNameToUniqueCitationKeys(names.get(pivot));
            keys.addAll(parseRefMarkNameToUniqueCitationKeys(names.get(pivot + 1)));

            documentConnection.removeReferenceMark(names.get(pivot));
            documentConnection.removeReferenceMark(names.get(pivot + 1));

            List<BibEntry> entries = lookupEntriesInDatabasesSkipMissing(keys, databases);
            entries.sort(new FieldComparator(StandardField.YEAR));

            String keyString =
                entries.stream()
                .map(c -> c.getCitationKey().orElse(""))
                .collect(Collectors.joining(","));

            // Insert reference mark:
            String newName = getUniqueReferenceMarkName(documentConnection,
                    keyString,
                    OOBibBase.AUTHORYEAR_PAR
            );

            insertReferenceMark(
                documentConnection,
                newName,
                "tmp",
                textCursor,
                true, // withText
                style);
            names.set(pivot + 1, newName); // <- put in the next-to-be-processed position
            madeModifications = true;

            pivot++;
        } // while

        if (madeModifications) {
            updateSortedReferenceMarks();
            refreshCiteMarkers(databases, style);
        }
    }

    /**
     * GUI action.
     * Do the opposite of combineCiteMarkers.
     * Combined markers are split, with a space inserted between.
     */
    public void unCombineCiteMarkers(List<BibDatabase> databases, OOBibStyle style)
            throws IOException,
            WrappedTargetException,
            NoSuchElementException,
            IllegalArgumentException,
            UndefinedCharacterFormatException,
            UnknownPropertyException,
            PropertyVetoException,
            CreationException,
            BibEntryNotFoundException,
            NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        List<String> names =
                getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        int pivot = 0;
        boolean madeModifications = false;
        XNameAccess nameAccess = documentConnection.getReferenceMarks();

        while (pivot < (names.size())) {
            XTextRange range1 =
                    unoQI(XTextContent.class,
                            nameAccess.getByName(names.get(pivot)))
                            .getAnchor();

            XTextCursor textCursor =
                    range1.getText().createTextCursorByRange(range1);

            // If we are supposed to set character format for
            // citations, test this before making any changes. This
            // way we can throw an exception before any reference
            // marks are removed, preventing damage to the user's
            // document:
            if (style.isFormatCitations()) {
                testFormatCitations(textCursor, style);
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
                String newName = getUniqueReferenceMarkName(documentConnection,
                        key,
                        OOBibBase.AUTHORYEAR_PAR
                );

                insertReferenceMark(
                        documentConnection,
                        newName,
                        "tmp",
                        textCursor,
                        /* TODO: withText should be itcType != OOBibBase.INVISIBLE_CIT */
                        true,
                        style);
                textCursor.collapseToEnd();
                if (i != last) {
                    textCursor.setString(" ");
                    textCursor.collapseToEnd();
                }
                i++;
            }
            madeModifications = true;

            pivot++;
        }
        if (madeModifications) {
            updateSortedReferenceMarks();
            refreshCiteMarkers(databases, style);
        }
    }

    /**
     * Used from GUI.
     */
    public BibDatabase generateDatabase(List<BibDatabase> databases)
            throws NoSuchElementException,
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

                            // TODO: broken logic here: we just created resultDatabase,
                            // and added nothing yet. Question: why do we use
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
        }

        resultDatabase.insertEntries(entriesToInsert);
        return resultDatabase;
    }

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
}
