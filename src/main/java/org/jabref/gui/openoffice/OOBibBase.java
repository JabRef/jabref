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
     * Document-connection related variables.
     */
    private static class DocumentConnection {
        /** https://wiki.openoffice.org/wiki/Documentation/BASIC_Guide/
         *  Structure_of_Text_Documents#Character_Properties
         *  "CharStyleName" is an OpenOffice Property name.
         */
        private static final String CHAR_STYLE_NAME = "CharStyleName";
        private static final Logger LOGGER =
            LoggerFactory.getLogger(OOBibBase.DocumentConnection.class);


        public XTextDocument mxDoc;
        public XComponent xCurrentComponent;
        public XMultiServiceFactory mxDocFactory;
        public XText xText;
        public XTextViewCursorSupplier xViewCursorSupplier;
        public XPropertyContainer userProperties;
        public XPropertySet propertySet;

        DocumentConnection(
            XTextDocument mxDoc
            ) {
            this.mxDoc = mxDoc;
            this.xCurrentComponent = unoQI(XComponent.class, mxDoc);
            this.mxDocFactory = unoQI(XMultiServiceFactory.class, mxDoc);
            // unoQI(XDocumentIndexesSupplier.class, component);

            // get a reference to the body text of the document
            this.xText = mxDoc.getText();

            XModel mo = unoQI(XModel.class, this.xCurrentComponent);
            XController co = mo.getCurrentController();
            this.xViewCursorSupplier = unoQI(XTextViewCursorSupplier.class, co);

            XDocumentPropertiesSupplier supp =
                unoQI(XDocumentPropertiesSupplier.class, mxDoc);
            this.userProperties =
                supp.getDocumentProperties().getUserDefinedProperties();

            this.propertySet = unoQI(XPropertySet.class, userProperties);
        }

        /**
         *  @return True if we cannot reach the current document.
         */
        public boolean
        documentConnectionMissing() {

            boolean missing = false;
            // These are set by DocumentConnection constructor.
            if (null == this.mxDoc
                || null == this.xCurrentComponent
                || null == this.mxDocFactory
                || null == this.xText
                || null == this.xViewCursorSupplier
                || null == this.userProperties
                || null == this.propertySet) {
                missing = true;
            }

            // Attempt to check document is really available
            if (!missing) {
                try {
                    getReferenceMarks();
                } catch (NoDocumentException ex) {
                    missing = true;
                }
            }

            if (missing) {
                // release it
                this.mxDoc = null;
                this.xCurrentComponent = null;
                this.mxDocFactory = null;
                this.xText = null;
                this.xViewCursorSupplier = null;
                this.userProperties = null;
                this.propertySet = null;
            }
            return missing;
        }

        /**
         *  Get the title of the connected document.
         */
        public Optional<String>
        getDocumentTitle() {
            return OOBibBase.getDocumentTitle(this.mxDoc);
        }

        List<String>
        getCustomPropertyNames() {
            assert (this.propertySet != null);

            XPropertySetInfo psi = (this.propertySet
                                    .getPropertySetInfo());

            List<String> names = new ArrayList<>();
            for (Property p : psi.getProperties()) {
                names.add(p.Name);
            }
            return names;
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
         * we have a working connection.
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
                throw new NoDocumentException("getReferenceMarks failed with" + ex);
            }
        }

        /**
         * Provides access to bookmarks by name.
         */
        private XNameAccess
        getBookmarks() {

            XBookmarksSupplier supplier =
                unoQI(
                    XBookmarksSupplier.class,
                    this.xCurrentComponent
                    );
            return supplier.getBookmarks();
        }

        /**
         *  @return An XNameAccess to find sections by name.
         */
        private XNameAccess
        getTextSections()
            throws
            IllegalArgumentException {

            XTextSectionsSupplier supplier =
                unoQI(
                    XTextSectionsSupplier.class,
                    this.mxDoc
                    );
            return supplier.getTextSections();
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
         * @return null if name not found, or if the result does not
         *         support the XTextContent interface.
         */
        static XTextContent
        nameAccessGetTextContentByNameOrNull(XNameAccess nameAccess, String name)
            throws WrappedTargetException {

            if (!nameAccess.hasByName(name)) {
                return null;
            }
            try {
                Object referenceMark = nameAccess.getByName(name);
                return unoQI(XTextContent.class, referenceMark);
            } catch (NoSuchElementException ex) {
                LOGGER.warn(String.format(
                                "nameAccessGetTextContentByNameOrNull got NoSuchElementException"
                                + " for '%s'", name));
                return null;
            }
        }

        /**
         * Create a text cursor for a textContent.
         *
         * @return null if mark is null, otherwise cursor.
         *
         */
        static XTextCursor
        getTextCursorOfTextContent(XTextContent mark) {
            if (mark == null) {
                return null;
            }
            XTextRange markAnchor = mark.getAnchor();
            return
                markAnchor.getText()
                .createTextCursorByRange(markAnchor);
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
                XTextContent mark =
                        nameAccessGetTextContentByNameOrNull(xReferenceMarks, name);
                if (mark == null) {
                    return;
                }
                this.xText.removeTextContent(mark);
            }
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
         * Get the XTextRange corresponding to the named bookmark.
         *
         * @param name The name of the bookmark to find.
         * @return The XTextRange for the bookmark, or null.
         */
        public XTextRange
        getBookmarkRangeOrNull(String name)
            throws
            WrappedTargetException {

            XNameAccess nameAccess = this.getBookmarks();
            XTextContent textContent =
                nameAccessGetTextContentByNameOrNull(nameAccess, name);
            if (textContent == null) {
                return null;
            }
            return textContent.getAnchor();
        }

        public XTextRange
        getReferenceMarkRangeOrNull(String name)
            throws
            NoDocumentException,
            WrappedTargetException {

            XNameAccess nameAccess = this.getReferenceMarks();
            XTextContent textContent =
                nameAccessGetTextContentByNameOrNull(nameAccess, name);
            if (textContent == null) {
                return null;
            }
            return textContent.getAnchor();
        }

        /**
         * Insert a new instance of a service at the provided cursor
         * position.
         *
         * @param service For example
         *                 "com.sun.star.text.ReferenceMark",
         *                 "com.sun.star.text.Bookmark" or
         *                 "com.sun.star.text.TextSection".
         *
         *                 Passed to this.mxDocFactory.createInstance(service)
         *                 The result is expected to support the
         *                 XNamed and XTextContent interfaces.
         *
         * @param name     For the ReferenceMark, Bookmark, TextSection.
         *                 If the name is already in use, LibreOffice
         *                 may change the name.
         *
         * @param range   Marks the location or range for
         *                the thing to be inserted.
         *
         * @param absorb ReferenceMark, Bookmark and TextSection can
         *               incorporate a text range. If absorb is true,
         *               the text in the range becomes part of the thing.
         *               If absorb is false,  the thing is
         *               inserted at the end of the range.
         *
         * @return The XNamed interface, in case we need to check the actual name.
         *
         */
        private XNamed
        insertNamedTextContent(
            String service,
            String name,
            XTextRange range,
            boolean absorb
            )
            throws
            CreationException {

            Object xObject;
            try {
                xObject =
                    this.mxDocFactory
                    .createInstance(service);
            } catch (Exception e) {
                throw new CreationException(e.getMessage());
            }

            XNamed xNamed = unoQI(XNamed.class, xObject);
            xNamed.setName(name);

            // get XTextContent interface
            XTextContent xTextContent = unoQI(XTextContent.class, xObject);
            range.getText().insertTextContent(range, xTextContent, absorb);
            return xNamed;
        }

        /**
         * Insert a new reference mark at the provided cursor
         * position.
         *
         * The text in the cursor range will be the text with gray
         * background.
         *
         * @param name     For the reference mark.
         * @param range    Cursor marking the location or range for
         *                 the reference mark.
         */
        public XNamed
        insertReferenceMark(
            String name,
            XTextRange range,
            boolean absorb
            )
            throws
            CreationException {
            return
                insertNamedTextContent(
                    "com.sun.star.text.ReferenceMark",
                    name,
                    range,
                    absorb // was true
                    );
        }

        /**
         * Insert a bookmark with the given name at the cursor provided,
         * or with another name if the one we asked for is already in use.
         *
         * @param name     For the bookmark.
         * @param range    Cursor marking the location or range for
         *                 the bookmark.
         * @param absorb   Shall we incorporate range?
         *
         */
        public XNamed
        insertBookmark(
            String name,
            XTextRange range,
            boolean absorb)
            throws
            IllegalArgumentException,
            CreationException {

            return
                insertNamedTextContent(
                    "com.sun.star.text.Bookmark",
                    name,
                    range,
                    absorb // was true
                    );
        }

        /**
         *  Create a text section with the provided name and insert it at
         *  the provided cursor.
         *
         *  @param name  The desired name for the section.
         *  @param range The location to insert at.
         *
         */
        private XNamed
        insertTextSection(
            String name,
            XTextRange range,
            boolean absorb)
            throws
            IllegalArgumentException,
            CreationException {

            return
                insertNamedTextContent(
                    "com.sun.star.text.TextSection",
                    name,
                    range,
                    absorb // was false
                    );
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

        /**
         *  If original is in a footnote, return a range containing
         *  the corresponding footnote marker.
         *
         *  Returns null if not in a footnote.
         */
        static XTextRange
        getFootnoteMarkRangeOrNull(XTextRange original) {
            // If we are inside a footnote:
            if (unoQI(XFootnote.class, original.getText()) != null) {
                // Find the linking footnote marker:
                XFootnote footer = unoQI(XFootnote.class, original.getText());
                // The footnote's anchor gives the correct position in the text:
                return footer.getAnchor();
            }
            return null;
        }

        /**
         *  Apply a character style to a range of text selected by a
         *  cursor.
         *
         * @param position  The range to apply to.
         * @param charStyle Name of the character style as known by Openoffice.
         */
        private static void
        setCharStyle(
            XTextCursor position,
            String charStyle
            )
            throws UndefinedCharacterFormatException {

            XPropertySet xCursorProps = unoQI(XPropertySet.class, position);

            try {
                xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
            } catch (UnknownPropertyException
                     | PropertyVetoException
                     | IllegalArgumentException
                     | WrappedTargetException ex) {
                throw new UndefinedCharacterFormatException(charStyle);
                // Setting the character format failed, so we throw an exception that
                // will result in an error message for the user:
            }
        }

        /**
         * Apply direct character format "Italic" to a range of text.
         *
         * Ref: https://www.openoffice.org/api/docs/common/ref/com/sun/star/style/CharacterProperties.html
         */
        private static void
        setCharFormatItalic(XTextRange textRange)
            throws
            UnknownPropertyException,
            PropertyVetoException,
            WrappedTargetException {
            XPropertySet xcp = unoQI(XPropertySet.class, textRange);
            xcp.setPropertyValue("CharPosture", com.sun.star.awt.FontSlant.ITALIC);
        }

        /*
         * Apply direct character format "Bold" to a range of text.
         */
        /* unused:
        private static void
        setCharFormatBold(XTextRange textRange)
            throws
            UnknownPropertyException,
            PropertyVetoException,
            WrappedTargetException {
            XPropertySet xcp = unoQI(XPropertySet.class, textRange);
            xcp.setPropertyValue("CharWeight", com.sun.star.awt.FontWeight.BOLD);
        }
        */

        /**
         *  Set language to [None]
         *
         *  Note: "zxx" is an https://en.wikipedia.org/wiki/ISO_639 code for
         *        "No linguistic information at all"
         */
        private static void
        setCharLocaleNone(XTextRange textRange)
            throws
            UnknownPropertyException,
            PropertyVetoException,
            WrappedTargetException {
            XPropertySet xcp = unoQI(XPropertySet.class, textRange);
            xcp.setPropertyValue("CharLocale", new Locale("zxx", "", ""));
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
     *
     *  Depends on: style, citations and their order.
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

        class RangeForOverlapCheck {
            final static int REFERENCE_MARK_KIND = 0;
            final static int FOOTNOTE_MARK_KIND = 1;

            XTextRange range;
            int i;
            int kind;
            String description;

            RangeForOverlapCheck(XTextRange range, int i, int kind, String description) {
                this.range = range;
                this.i = i;
                this.kind = kind;
                this.description = description;
            }

            String format() {
                return description;
                //    String[] prefixes = { "", "FootnoteMark for " } ;
                //    return prefixes[kind] + names[ this.i ];
            }

        } // class X

        /**
         * Assumes a.getText() == b.getText(), and both belong to documentConnection.xText
         */
        public int
        compareRegionStarts(RangeForOverlapCheck a,
                            RangeForOverlapCheck b) {
            //
            // XTextRange cannot be compared, only == or != is available.
            //
            // XTextRangeCompare: compares the positions of two TextRanges within a Text.
            // Only TextRange instances within the same Text can be compared.
            // final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
            //                                         documentConnection.xText);

            XTextRange ra = a.range;
            XTextRange rb = b.range;
            if (ra.getText() != rb.getText()) {
                throw new RuntimeException(
                    String.format(
                        "OOBibBase.CitationGroups.compareRegionStarts:"
                        + " incomparable regions: %s %s",
                        a.format(),
                        b.format())
                    );
            }

            /*
             * documentConnection.xText cannot compare two ranges in
             * the same footnote. We must use XTextRangeCompare interface
             * of the ranges themselves.
             */
            final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                    ra.getText());

            try {
                return (-1) * compare.compareRegionStarts(ra, rb);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(
                    String.format(
                        "OOBibBase.CitationGroups.compareRegionStarts:"
                        + " caught IllegalArgumentException: %s %s",
                        a.format(),
                        b.format()
                        )
                    );
            }
        }

        public int
        compareRegionEndToStart(RangeForOverlapCheck a,
                                RangeForOverlapCheck b) {

            XTextRange ra = a.range.getEnd();
            XTextRange rb = b.range.getStart();
            final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                    ra.getText());
            return (-1) * compare.compareRegionStarts(ra, rb);
        }

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
            List<XTextRange> seen = new ArrayList<>();
            final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                    documentConnection.xText);

            List<RangeForOverlapCheck> xs = new ArrayList<>();
            for (int i = 0; i < names.length; i++) {
                XTextRange r = this.getReferenceMarkRangeOrNull(documentConnection, i);
                XTextRange footnoteMarkRange =
                    DocumentConnection.getFootnoteMarkRangeOrNull(r);

                if (footnoteMarkRange != null) {
                    // Problem: quadratic complexity. Each new footnoteMarkRange
                    // is compared to all we have seen before.
                    boolean seenContains = false;
                    for (XTextRange s : seen) {
                        if (s.getText() == footnoteMarkRange.getText() &&
                            compare.compareRegionStarts(s, footnoteMarkRange) == 0 &&
                            compare.compareRegionEnds(s, footnoteMarkRange) == 0) {
                            seenContains = true;
                            break;
                        }
                    }
                    if (!seenContains) {
                    seen.add(footnoteMarkRange);
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

        private Map<XText, List<RangeForOverlapCheck>>
        partitionByGetText(List<RangeForOverlapCheck> xs) {
            Map<XText, List<RangeForOverlapCheck>> xxs = new HashMap<>();
            for (RangeForOverlapCheck x : xs) {
                XTextRange xr = x.range;
                XText t = xr.getText();
                if (xxs.containsKey(t)) {
                    xxs.get(t).add(x);
                } else {
                    xxs.put(t, new ArrayList<>(List.of(x)));
                }
            }
            return xxs;
        }

        private List<RangeForOverlapCheck>
        sortPartitionByRegionStart(List<RangeForOverlapCheck> xs) {
            return
                xs.stream()
                .sorted(this::compareRegionStarts)
                .collect(Collectors.toList());
        }

        private void
        checkSortedPartitionForOverlap(boolean requireSeparation,
                                       List<RangeForOverlapCheck> oxs)
            throws JabRefException {
            for (int i = 0; (i + 1) < oxs.size(); i++) {
                RangeForOverlapCheck a = oxs.get(i);
                RangeForOverlapCheck b = oxs.get(i + 1);
                int cmp = compareRegionEndToStart(a, b);
                if (cmp > 0) {
                    // found overlap
                    throw new JabRefException(
                        "Range overlap found",
                        Localization.lang(
                            "Ranges of '%0' and '%1' overlap", a.format(), b.format())
                        );
                }
                if (requireSeparation && cmp == 0) {
                    throw new JabRefException(
                        "Ranges with no gap found",
                        Localization.lang(
                            "Ranges of '%0' and '%1' are not separated",
                                a.format(), b.format())
                        );
                }
            }
        }

        public void
        checkRangeOverlaps(DocumentConnection documentConnection,
                           boolean requireSeparation)
            throws
            NoDocumentException,
            WrappedTargetException,
            JabRefException {

            List<RangeForOverlapCheck> xs = citationRanges(documentConnection);
            xs.addAll(footnoteMarkRanges(documentConnection));

            // We can only compare ranges with equal .getText(),
            // so partition the list.
            Map<XText, List<RangeForOverlapCheck>> xxs = partitionByGetText(xs);
            // Sort xs by x.getText() and x.getStart()
            // Then, within each getText() value, we need x[i].getEnd() <= x[i+1].getStart()
            // final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
            //                                        documentConnection.xText);
            for (List<RangeForOverlapCheck> partition : xxs.values()) {
                List<RangeForOverlapCheck> oxs =
                    sortPartitionByRegionStart(partition);
                System.out.println("partition");
                for (RangeForOverlapCheck r : oxs) {
                    System.out.println("  " + r.format());
                }
                checkSortedPartitionForOverlap(requireSeparation, oxs);
            }
        }
    } // class citationGroups

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
     * Does not chaneg presentation.
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
     *  Refresh list of JabRef reference marks (sorts by position).
     *
     *  Probably should be called at the start of actions from the GUI,
     *  that rely on jabRefReferenceMarkNamesSortedByPosition to be up-to-date.
     */
    public void
    updateSortedReferenceMarks()
        throws
        WrappedTargetException,
        NoSuchElementException,
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        this.jabRefReferenceMarkNamesSortedByPosition =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);
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
    void updateUniqueLetters(
        String[][] bibtexKeys,
        String[][] normCitMarkers,
        final Map<String, String> uniqueLetters
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

        uniqueLetters.clear();

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
    produceCitationMarkersForNormalStyle(
        List<String> referenceMarkNames,
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

        updateUniqueLetters(bibtexKeys, normCitMarkers, uniqueLetters);

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

        return citMarkers;
    }

    /* ***********************************
     *
     *  modifies both storage and presentation
     *
     * ***********************************/

    /**
     *
     *  Insert a reference mark: creates and fills it.
     *
     * @param documentConnection Connection to a document.
     *
     * @param name Name of the reference mark to be created and also
     *             the name of the custom property holding the pageInfo part.
     */
    private void
    insertReferenceMark(
        DocumentConnection documentConnection,
        String name,
        String citationText,
        XTextCursor position,
        boolean withText,
        OOBibStyle style
        )
        throws
        UnknownPropertyException,
        WrappedTargetException,
        PropertyVetoException,
        IllegalArgumentException,
        UndefinedCharacterFormatException,
        CreationException {

        // Last minute editing: If there is "page info" for this
        // citation mark, inject it into the citation marker before
        // inserting.

        String citText;
        String pageInfo =
            getPageInfoForReferenceMarkName(documentConnection, name);
        citText =
            pageInfo.isEmpty()
            ? citationText
            : style.insertPageInfo(citationText, pageInfo);

        /*
         * We had a problem here, position.setString() not inserting the text.
         * The solution seems to be: create a new cursor (c2), and use that.
         */
        XTextCursor c2 = position.getText().createTextCursorByRange(position);
        if (withText) {
            c2.setString(citText);
            DocumentConnection.setCharLocaleNone(c2);
            if (style.isFormatCitations()) {
                String charStyle = style.getCitationCharacterFormat();
                DocumentConnection.setCharStyle(c2, charStyle);
            }
        } else {
            c2.setString("");
        }

        documentConnection.insertReferenceMark(name, c2, true);

        // Last minute editing: find "et al." (OOBibStyle.ET_AL_STRING) and
        //                      format it as italic.

        // Check if we should italicize the "et al." string in citations:
        boolean italicize = style.getBooleanCitProperty(OOBibStyle.ITALIC_ET_AL);
        if (italicize) {
            String etAlString = style.getStringCitProperty(OOBibStyle.ET_AL_STRING);
            int index = citText.indexOf(etAlString);
            if (index >= 0) {
                italicizeRangeFromPosition(c2, index, index + etAlString.length());
            }
        }

        position.collapseToEnd();
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
    private void
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
     * Called from: OpenOfficePanel.pushEntries, a GUI action for
     * "Cite", "Cite in-text", "Cite special" and "Insert empty
     * citation".
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
     *
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
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        try {
            XTextCursor cursor;
            // Get the cursor positioned by the user.
            try {
                cursor = documentConnection.getViewCursor();
            } catch (RuntimeException ex){
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
            // Generate unique bookmark-name
            int itcType = citationTypeFromOptions(withText, inParenthesis);
            String newName =
                getUniqueReferenceMarkName(
                    documentConnection,
                    keyString,
                    itcType);

            // If we should store metadata for page info, do that now:
            if (pageInfo != null) {
                LOGGER.info("Storing page info: " + pageInfo);
                documentConnection.setCustomProperty(newName, pageInfo);
            }
            // else: branch ???
            // Note: if (pageInfo is null), we might inadvertently
            // pick up a pageInfo from an earlier citation. The user
            // may have removed the citation, thus the reference mark,
            // but pageInfo stored separately stays there.

            // insert space: we will write our citation before this space.
            cursor
                .getText()
                .insertString(cursor, " ", false);

            // format the space inserted
            if (style.isFormatCitations()) {
                String charStyle = style.getCitationCharacterFormat();
                try {
                    DocumentConnection.setCharStyle(cursor, charStyle);
                } catch (UndefinedCharacterFormatException ex) {
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
                    style);
             // } // end of scope for databaseMap, citeText

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
        }
    }

    /* **************************************************
     *
     *  modifies both storage and presentation, but should only affect presentation
     *
     * **************************************************/

    /**
     * Refresh all citation markers in the document.
     *
     * GUI: called as part of "Export cited"
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

            XTextContent mark =
                DocumentConnection.nameAccessGetTextContentByNameOrNull(nameAccess, name);
            if (null == mark) {
                LOGGER.warn(String.format(
                               "OOBibBase.applyNewCitationMarkers:"
                               + " lost reference mark '%s'",
                               name
                               ));
                continue;
            }

            XTextCursor cursor =
                DocumentConnection.getTextCursorOfTextContent(mark);

            if (mustTestCharFormat) {
                mustTestCharFormat = false; // need to do this only once
                String charStyle = style.getCitationCharacterFormat();
                DocumentConnection.setCharStyle(cursor, charStyle);
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
                && (documentConnection.getBookmarkRangeOrNull(OOBibBase.BIB_SECTION_NAME) == null)) {
                // We have overwritten the marker for the start of the reference list.
                // We need to add it again.
                cursor.collapseToEnd();
                OOUtil.insertParagraphBreak(documentConnection.xText, cursor);
                documentConnection.insertBookmark(OOBibBase.BIB_SECTION_NAME, cursor, true);
                cursor.collapseToEnd();
            }
        }
    }

    /**
     * Refresh citation markers according to `style`.
     *
     * - Requires an up-to-date jabRefReferenceMarkNamesSortedByPosition
     *
     * @param documentConnection Connection.
     * @param databases For look up by citation key. Must have at least one.
     * @param style     Style.
     * @param uniqueLetters Will be cleared and potentially filled with new values.
     */
    private List<String>
    refreshCiteMarkersInternal(
        DocumentConnection documentConnection,
        List<BibDatabase> databases,
        OOBibStyle style,
        final Map<String, String> uniqueLetters
        )
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
                    itcTypes,
                    fce.entries,
                    uniqueLetters,
                    style);
        }

        // Refresh all reference marks with the citation markers we computed:
        applyNewCitationMarkers(
            documentConnection,
            referenceMarkNames,
            citMarkers,
            itcTypes,
            style);

        return unresolvedKeysFromEntries(fce.entries);
    }

    /* **************************************************
     *
     *     Bibliography: needs uniqueLetters or numbers
     *
     * **************************************************/

    /**
     * Rebuilds the bibliography.
     *
     * @param databases  Must have at least one.
     *
     *  Note: assumes fresh `jabRefReferenceMarkNamesSortedByPosition`
     *  if `style.isSortByPosition()`
     */
    public void
    rebuildBibTextSection(
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
            this.xUniqueLetters);
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
     * Insert a paragraph break and creates a text section for the bibliography.
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
        NoDocumentException {

        DocumentConnection documentConnection = this.getDocumentConnectionOrThrow();

        // The testing for whitespace-only between (pivot) and (pivot+1) assumes that
        // names are in textual order: textually consecutive pairs
        // must appear as neighbours (and in textual order).
        // We have a bit of a clash here: names is sorted by visual position,
        // but we are testing if they are textually neighbours.
        // In a two-column layout
        //  | a | c |
        //  | b | d |
        // abcd is the textual order, but the visual order is acbd.
        // So we will not find out that a and b are only separated by white space.
        List<String> names =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        // XTextRangeCompare: compares the positions of two TextRanges within a Text.
        // Only TextRange instances within the same Text can be compared.
        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                documentConnection.xText);

        int pivot = 0;
        boolean madeModifications = false;
        boolean setCharStyleTested = false;
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
                .getStart(); // end of range2 is the start of (pivot + 1)

            if (range1.getText() != range2.getText()) {
                /* pivot and (pivot+1) belong to different Text instances.
                 * Maybe to different footnotes?
                 * Cannot combine across boundaries, skip.
                 */
                pivot++;
                continue;
            }

            // Start from end of text for pivot.
            XTextCursor textCursor =
                range1.getText().createTextCursorByRange(range1);

            // Select next character (if possible), and more, as long as we can and
            // do not reach start of (pivot+1), which we now know to be
            // in the same Text instance.

            // If there is no space between the two reference marks,
            // the next line moves INTO the next. And probably will
            // cover a non-whitespace character, inhibiting the merge.
            // Empirically: does not merge. Probably a bug.
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
            // Q: we may have zero characters selected. Is this a valid test
            //    in this case?
            if (style.isFormatCitations() && !setCharStyleTested) {
                String charStyle = style.getCitationCharacterFormat();
                DocumentConnection.setCharStyle(textCursor, charStyle);
                setCharStyleTested = true;
            }

            /*
             * This only gets the keys: itcType is discarded.
             *
             * AUTHORYEAR_PAR:   "(X and Y 2000)"
             * AUTHORYEAR_INTEXT: "X and Y (2000)"
             * INVISIBLE_CIT: ""
             *
             *  We probably only want to collect citations with
             *  AUTHORYEAR_PAR itcType.
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

            // Note: silently drops duplicate keys.
            //       What if they have different pageInfo fields?

            //  combineCiteMarkers: merging for same citation keys,
            //       but different pageInfo looses information.
            List<String> keys =
                parseRefMarkNameToUniqueCitationKeys(names.get(pivot));
            keys.addAll(parseRefMarkNameToUniqueCitationKeys(names.get(pivot + 1)));

            documentConnection.removeReferenceMark(names.get(pivot));
            documentConnection.removeReferenceMark(names.get(pivot + 1));

            // Note: citation keys not found are silently left out from the
            //       combined reference mark name. Loosing information.
            List<BibEntry> entries = lookupEntriesInDatabasesSkipMissing(keys, databases);
            entries.sort(new FieldComparator(StandardField.YEAR));

            String keyString =
                entries.stream()
                .map(c -> c.getCitationKey().orElse(""))
                .collect(Collectors.joining(","));

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
        NoDocumentException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        List<String> names =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        int pivot = 0;
        boolean madeModifications = false;
        boolean setCharStyleTested = false;
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
            if (style.isFormatCitations() && !setCharStyleTested) {
                String charStyle = style.getCitationCharacterFormat();
                DocumentConnection.setCharStyle(textCursor, charStyle);
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

                insertReferenceMark(
                        documentConnection,
                        newName,
                        "tmp",
                        textCursor,
                        /* withText should be itcType != OOBibBase.INVISIBLE_CIT */
                        true,
                        style);
                textCursor.collapseToEnd();
                if (i != last) {
                    // space between citation markers: what style?
                    // DocumentConnection.setCharStyle(textCursor, "Standard");
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

    /**
     * GUI action, refreshes citation markers and bibliography.
     *
     * @param databases Must have at least one.
     * @param style Style.
     * @return List of unresolved citation keys.
     *
     * Note: calls updateSortedReferenceMarks();
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
        JabRefException {

        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        boolean requireSeparation = false; // may loose citation without requireSeparation=true
        CitationGroups cg = new CitationGroups(documentConnection);
        cg.checkRangeOverlaps(this.xDocumentConnection, requireSeparation);

        updateSortedReferenceMarks();
        List<String> unresolvedKeys = refreshCiteMarkers(databases, style);
        rebuildBibTextSection(databases, style);
        return unresolvedKeys;
    }

} // end of OOBibBase
