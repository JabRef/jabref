package org.jabref.gui.openoffice;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.OOPreFormatter;
import org.jabref.logic.openoffice.OOUtil;
import org.jabref.logic.openoffice.UndefinedBibtexEntry;
import org.jabref.logic.openoffice.UndefinedParagraphFormatException;
import org.jabref.logic.openoffice.CitationEntry;

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
import com.sun.star.frame.XModel;
import com.sun.star.frame.XFrame;
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

    private static final String BIB_SECTION_NAME     = "JR_bib";
    private static final String BIB_SECTION_END_NAME = "JR_bib_end";

    private static final String BIB_CITATION = "JR_cite";
    private static final Pattern CITE_PATTERN =
        Pattern.compile(BIB_CITATION + "(\\d*)_(\\d*)_(.*)");

    private static final String CHAR_STYLE_NAME = "CharStyleName";

    /* Types of in-text citation. (itcType)
     * Their numeric values are used in reference mark names.
     */
    private static final int AUTHORYEAR_PAR    = 1;
    private static final int AUTHORYEAR_INTEXT = 2;
    private static final int INVISIBLE_CIT     = 3;

    private static final Logger LOGGER =
        LoggerFactory.getLogger(OOBibBase.class);

    /* variables  */
    private final DialogService dialogService;
    private final XDesktop      xDesktop;
    private final boolean       atEnd;
    private final Comparator<BibEntry> entryComparator;
    private final Comparator<BibEntry> yearAuthorTitleComparator;

    /* document-connection related */
    private class DocumentConnection {
        public XMultiServiceFactory    mxDocFactory;
        public XTextDocument           mxDoc;
        public XText                   xText;
        public XTextViewCursorSupplier xViewCursorSupplier;
        public XComponent              xCurrentComponent;
        public XPropertySet            propertySet;
        public XPropertyContainer      userProperties;
        DocumentConnection(XMultiServiceFactory    mxDocFactory,
                           XTextDocument           mxDoc,
                           XText                   xText,
                           XTextViewCursorSupplier xViewCursorSupplier,
                           XComponent              xCurrentComponent,
                           XPropertySet            propertySet,
                           XPropertyContainer      userProperties
                           ) {
            this.mxDocFactory = mxDocFactory ;
            this.mxDoc = mxDoc ;
            this.xText = xText ;
            this.xViewCursorSupplier = xViewCursorSupplier ;
            this.xCurrentComponent = xCurrentComponent ;
            this.propertySet = propertySet ;
            this.userProperties = userProperties ;
        }

        public Optional<String> getDocumentTitle() {
            return OOBibBase.getDocumentTitle( this.mxDoc );
        }

        private Optional<String> getCustomProperty(String property)
            throws UnknownPropertyException,
                   WrappedTargetException
        {
            assert (this.propertySet != null);

            XPropertySetInfo psi =
                this.propertySet
                .getPropertySetInfo();

            if (psi.hasPropertyByName(property)) {
                String v =
                    this.propertySet
                    .getPropertyValue(property)
                    .toString();
                return Optional.ofNullable(v);
            }
            return Optional.empty();
        }

        private void setCustomProperty(String property, String value)
            throws UnknownPropertyException,
                   NotRemoveableException,
                   PropertyExistException,
                   IllegalTypeException,
                   IllegalArgumentException
        {
            XPropertySetInfo psi =
                this.propertySet
                .getPropertySetInfo();
            if (psi.hasPropertyByName(property)) {
                this.userProperties.removeProperty(property);
            }
            if (value != null) {
                this.userProperties
                    .addProperty(property,
                                 com.sun.star.beans.PropertyAttribute.REMOVEABLE,
                                 new Any(Type.STRING, value)
                                 );
            }
        }

        /**
         *
         * @throws NoDocumentException
         *
         */
        private XNameAccess getReferenceMarks()
            throws NoDocumentException
        {
            XReferenceMarksSupplier supplier =
                unoQI(XReferenceMarksSupplier.class,
                      this.xCurrentComponent);
            try {
                XNameAccess res = supplier.getReferenceMarks();
                return res;
            } catch ( Exception ex ){
                LOGGER.warn( "getReferenceMarks caught: ", ex );
                throw new NoDocumentException("getReferenceMarks failed");
            }
        }

        public boolean checkDocumentConnection(){
            boolean res = true;
            // These are set by selectDocument:
            if (null == this.xCurrentComponent   ){ res = false; }
            if (null == this.mxDoc               ){ res = false; }
            if (null == this.xViewCursorSupplier ){ res = false; }
            if (null == this.xText               ){ res = false; }
            if (null == this.mxDocFactory        ){ res = false; }
            if (null == this.userProperties      ){ res = false; }
            if (null == this.propertySet         ){ res = false; }
            //
            if ( ! res ){
                return false;
            }
            // Attempt to check document is really available
            // TODO
            try {
                getReferenceMarks();
            } catch (NoDocumentException ex ) {
                return false;
            }
            return true;
        }

        List<String> getReferenceMarknames()
            throws NoDocumentException
        {
            XNameAccess nameAccess = getReferenceMarks();
            String[] names = nameAccess.getElementNames();
            if (names == null) {
                return new ArrayList<>();
            }
            return  Arrays.asList( names );
        }

        public String getCitationContext(String refMarkName,
                                         int charBefore,
                                         int charAfter,
                                         boolean htmlMarkup)
            throws NoSuchElementException,
                   WrappedTargetException,
                   NoDocumentException
        {
            XNameAccess nameAccess = getReferenceMarks();
            Object referenceMark = nameAccess.getByName(refMarkName);
            XTextContent bookmark = unoQI(XTextContent.class, referenceMark);

            XTextCursor cursor =
                bookmark.getAnchor() // the text range to which the content is attached.
                .getText()
                .createTextCursorByRange(bookmark.getAnchor());

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

        // Get the cursor positioned by the user.
        public XTextViewCursor getViewCursor(){
            return this.xViewCursorSupplier.getViewCursor();
        }

    } // end DocumentConnection

    private DocumentConnection xDocumentConnection;
    /*
     *  xUniquefiers : maps bibtexkeys to letters ("a", "b")
     */
    private final Map<String, String> xUniquefiers = new HashMap<>();

    private List<String> jabRefReferenceMarkNamesSortedByPosition;

    /** unoQI : short for UnoRuntime.queryInterface
     *
     * Returns: a reference to the requested UNO interface type if
     * available, otherwise null
     */
    private static <T> T unoQI(Class<T> zInterface,
                               Object object)
    {
        return UnoRuntime.queryInterface( zInterface, object );
    }

    /*
     * Shall we keep calls I suspect to be useless?
     */
    private final boolean run_useless_parts = true;

    /*
     * Constructor
     */

    private XDesktop simpleBootstrap(Path loPath)
        throws CreationException,
               BootstrapException
    {
        // Get the office component context:
        XComponentContext      context = org.jabref.gui.openoffice.Bootstrap.bootstrap(loPath);
        XMultiComponentFactory sem     = context.getServiceManager();

        // Create the desktop, which is the root frame of the
        // hierarchy of frames that contain viewable components:
        Object desktop;
        try {
            desktop = sem.createInstanceWithContext("com.sun.star.frame.Desktop", context);
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        XDesktop result = unoQI(XDesktop.class, desktop);

        // TODO: useless call?
        if ( run_useless_parts ){
            unoQI(XComponentLoader.class, desktop);
        }

        return result;
    }

    public OOBibBase(Path loPath,
                     boolean atEnd,
                     DialogService dialogService
                     ) throws IllegalAccessException,
                              InvocationTargetException,
                              BootstrapException,
                              CreationException,
                              IOException,
                              ClassNotFoundException
    {
        this.dialogService = dialogService;
        {
            FieldComparator a = new FieldComparator(StandardField.AUTHOR);
            FieldComparator y = new FieldComparator(StandardField.YEAR);
            FieldComparator t = new FieldComparator(StandardField.TITLE);

            {
                List<Comparator<BibEntry>> ayt = new ArrayList<>(3);
                ayt.add(a);
                ayt.add(y);
                ayt.add(t);
                this.entryComparator= new FieldComparatorStack<>(ayt);
            }
            {
                List<Comparator<BibEntry>> yat = new ArrayList<>(3);
                yat.add(y);
                yat.add(a);
                yat.add(t);
                this.yearAuthorTitleComparator = new FieldComparatorStack<>(yat);
            }
        }
        this.atEnd    = atEnd;
        this.xDesktop = simpleBootstrap(loPath);
    }


    /*
     *  section: selectDocument()
     */

    private static List<XTextDocument> getTextDocuments( XDesktop desktop )
        throws NoSuchElementException,
               WrappedTargetException
    {
        List<XTextDocument> result = new ArrayList<>();

        XEnumerationAccess  enumAccess = desktop.getComponents();
        XEnumeration        compEnum   = enumAccess.createEnumeration();

        while (compEnum.hasMoreElements()) {
            Object       next = compEnum.nextElement();
            XComponent   comp = unoQI(XComponent.class   , next);
            XTextDocument doc = unoQI(XTextDocument.class, comp);
            if (doc != null) {
                result.add(doc);
            }
        }
        return result;
    }

    private static Optional<String> getDocumentTitle(XTextDocument doc) {
        if (doc == null) {
            return Optional.empty();
        }

        try {
            XFrame frame           = doc.getCurrentController().getFrame();
            Object frame_title_obj = OOUtil.getProperty( frame , "Title");
            String frame_title_str = String.valueOf(frame_title_obj);
            return  Optional.of(frame_title_str);
        } catch (UnknownPropertyException | WrappedTargetException e) {
            LOGGER.warn("Could not get document title", e);
            return Optional.empty();
        }
    }

    private static XTextDocument selectDocumentDialog(List<XTextDocument> list,
                                                      DialogService dialogService)
    {
        class DocumentTitleViewModel {

            private final XTextDocument xTextDocument;
            private final String        description;

            public DocumentTitleViewModel(XTextDocument xTextDocument) {
                this.xTextDocument = xTextDocument;
                this.description   = OOBibBase.getDocumentTitle(xTextDocument).orElse("");
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
            .map( DocumentTitleViewModel::new )
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
            .map( DocumentTitleViewModel::getXtextDocument )
            .orElse(null);
    }

    /** Choose a document to work with.
     *
     *  inititalized fields:
     *     - this.xCurrentComponent
     *     - this.mxDoc
     *     - this.xViewCursorSupplier
     *     - this.xText
     *     - this.mxDocFactory
     *     - this.userProperties
     *     - this.propertySet
     *
     */
    public void selectDocument()
        throws NoDocumentException,
               NoSuchElementException,
               WrappedTargetException
    {
        XTextDocument mxDoc;
        {
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
                    OOBibBase.selectDocumentDialog(textDocumentList,
                                                   this.dialogService);
            }

            if (selected == null) {
                return;
            }
            mxDoc = selected;
        }
        XComponent component = unoQI(XComponent.class, mxDoc);


        // TODO: what is the point of the next line? Does it have a side effect?
        if ( run_useless_parts ){
            unoQI(XDocumentIndexesSupplier.class, component);
        }

        XTextViewCursorSupplier viewCursorSupplier;
        {
            XModel      mo = unoQI(XModel.class, component);
            XController co = mo.getCurrentController();
            viewCursorSupplier = unoQI(XTextViewCursorSupplier.class, co);
        }

        // get a reference to the body text of the document
        XText text = mxDoc.getText();

        // Access the text document's multi service factory:
        XMultiServiceFactory mxDocFactory = unoQI(XMultiServiceFactory.class, mxDoc);

        XPropertyContainer userProperties;
        {
            XDocumentPropertiesSupplier supp =
                unoQI(XDocumentPropertiesSupplier.class, mxDoc);
            userProperties = supp.getDocumentProperties().getUserDefinedProperties();
        }

        XPropertySet propertySet = unoQI(XPropertySet.class, userProperties);

        this.xDocumentConnection = new DocumentConnection(
                                                         mxDocFactory,
                                                         mxDoc,
                                                         text,
                                                         viewCursorSupplier,
                                                         component,
                                                         propertySet,
                                                         userProperties
                                                         );

        // TODO: maybe we should install an event handler for document
        // close: addCloseListener
        //
        // https://www.openoffice.org/api/docs/common/ref/com/sun/star/
        //         util/XCloseBroadcaster.html#addCloseListener
    }

    /*
     * TODO: GUI should be notified
     */
    private void forgetDocument(){
        this.xDocumentConnection   = null ;
    }

    public boolean isConnectedToDocument() {
        return this.xDocumentConnection != null;
    }

    public boolean checkDocumentConnection(){
        if (this.xDocumentConnection == null){
            return false;
        }
        boolean res = this.xDocumentConnection.checkDocumentConnection();
        if ( ! res ){
            forgetDocument();
        }
        return res;
    }

    private DocumentConnection getDocumentConnectionOrThrow()
        throws NoDocumentException
    {
        if ( ! checkDocumentConnection() ){
            throw new NoDocumentException("Not connected to document");
        }
        return this.xDocumentConnection;
    }

    /*
     *  Getters useful after selectDocument()
     */

    public Optional<String> getCurrentDocumentTitle() {
        if ( ! checkDocumentConnection() ){
            return Optional.empty();
        } else {
            return  this.xDocumentConnection.getDocumentTitle();
        }
    }



    /*
     * === insertEntry
     */

    private void sortBibEntryListForMulticite( List<BibEntry>    entries,
                                               OOBibStyle        style )
    {
        if (entries.size() <= 1){
            return;
        }
        if (style.getBooleanCitProperty( OOBibStyle.MULTI_CITE_CHRONOLOGICAL )) {
            entries.sort(this.yearAuthorTitleComparator);
        } else {
            entries.sort(this.entryComparator);
        }
    }
    private void sortBibEntryArrayForMulticite( BibEntry[] entries,
                                                OOBibStyle style )
    {
        if (entries.length <= 1) {
            return;
        }
        if (style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL)) {
            Arrays.sort(entries, yearAuthorTitleComparator);
        } else {
            Arrays.sort(entries, entryComparator);
        }
    }

    private static int citationTypeFromOptions( boolean withText, boolean inParenthesis ) {
        if ( !withText ){
            return OOBibBase.INVISIBLE_CIT ;
        }
        return ( inParenthesis
                 ? OOBibBase.AUTHORYEAR_PAR
                 : OOBibBase.AUTHORYEAR_INTEXT );
    }


    private static boolean isJabRefReferenceMarkName( String name ){
        return (CITE_PATTERN.matcher(name).find());
    }
    private static List<String> filterIsJabRefReferenceMarkName( List<String> names ) {
        return ( names
                 .stream()
                 .filter( OOBibBase::isJabRefReferenceMarkName )
                 .collect(Collectors.toList())
                 );
    }

    /*
     * called from getCitationEntries(...)
     */
    private List<String> getJabRefReferenceMarkNames(DocumentConnection documentConnection)
        throws NoDocumentException
    {
        return filterIsJabRefReferenceMarkName( documentConnection.getReferenceMarknames() );
    }

    /**
     *
     */
    public List<CitationEntry> getCitationEntries()
        throws NoSuchElementException,
               UnknownPropertyException,
               WrappedTargetException,
               NoDocumentException
    {
        return this.getCitationEntriesImpl( this.getDocumentConnectionOrThrow() );
    }

    private List<CitationEntry> getCitationEntriesImpl( DocumentConnection documentConnection )
        throws NoSuchElementException,
               UnknownPropertyException,
               WrappedTargetException,
               NoDocumentException
    {
        List<String> names = this.getJabRefReferenceMarkNames(documentConnection);

        List<CitationEntry> citations = new ArrayList(names.size());
        for (String name : names) {
            CitationEntry entry =
                new CitationEntry(name,
                                  documentConnection.getCitationContext(name, 30, 30, true),
                                  documentConnection.getCustomProperty(name)
                                  );
            citations.add(entry);
        }
        return citations;
    }

    /**
     * Apply editable parts of citationEntries to the document.
     *
     * - Currently the only editable part is pageInfo.
     *
     * Since the only call to applyCitationEntries() only changes
     * pageInfo w.r.t those returned by getCitationEntries(), we can
     * do with the following restrictions:
     *
     * - Missing pageInfo means no action.
     *
     * - Missing CitationEntry means no action (no attempt to
     *   remove citation from the text).
     *
     * - Reference to citation not present in the text evokes
     *   no error, and setCustomProperty() is called.
     *
     */
    public void applyCitationEntries( List<CitationEntry> citationEntries )
        throws UnknownPropertyException,
               NotRemoveableException,
               PropertyExistException,
               IllegalTypeException,
               IllegalArgumentException,
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        // Leave exceptions to the caller.
        //
        // Note: not catching exceptions here means nothing is applied
        //       after the first problematic entry. We might catch and
        //       collect messages here. 
        //
        // try {
        for (CitationEntry entry : citationEntries) {
            Optional<String> pageInfo = entry.getPageInfo();
            if (pageInfo.isPresent()) {
                documentConnection.setCustomProperty( entry.getRefMarkName(), pageInfo.get() );
            } else {
                // TODO: if pageInfo is not present, or is empty:
                // maybe we should remove it from the document.
            }
        }
        // } catch (UnknownPropertyException
        //        | NotRemoveableException
        //        | PropertyExistException
        //        | IllegalTypeException
        //        | IllegalArgumentException ex)
        // {
        //   LOGGER.warn("Problem modifying citation", ex);
        //   dialogService.showErrorDialogAndWait(
        //         Localization.lang("Problem modifying citation"), ex);
        //  }
    }

    /**
     *
     * The first occurrence of bibtexKey gets no serial number, the
     * second gets 0, the third 1 ...
     *
     * Or the first unused in this series, after removals.
     *
     */
    private String getUniqueReferenceMarkName(DocumentConnection documentConnection,
                                              String bibtexKey,
                                              int itcType)
        throws NoDocumentException
    {
        XNameAccess xNamedRefMarks = xDocumentConnection.getReferenceMarks();
        int i = 0;
        String name = BIB_CITATION + '_' + itcType + '_' + bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = BIB_CITATION + i + '_' + itcType + '_' + bibtexKey;
            i++;
        }
        return name;
    }

    /**
     *   This is what we get back from parsing a refMarkName
     *
     */
    private static class ParsedRefMark {
        public String i ; // "", "0", "1" ...
        public int itcType ; // in-text-citation type
        public List<String> citedKeys;
        ParsedRefMark( String i, int itcType, List<String> citedKeys ){
            this.i         = i;
            this.itcType   = itcType;
            this.citedKeys = citedKeys;
        }
    }

    /**
     * Parse a refMarkName.
     *
     */
    private static Optional<ParsedRefMark> parseRefMarkName( String refMarkName ){
        Matcher citeMatcher = CITE_PATTERN.matcher(refMarkName);
        if (!citeMatcher.find()) {
            return Optional.empty();
        }
        List<String> keys = Arrays.asList( citeMatcher.group(3).split(",") );
        String i = citeMatcher.group(1);
        int itcType = Integer.parseInt( citeMatcher.group(2) );
        return( Optional.of( new OOBibBase.ParsedRefMark( i, itcType, keys ) ) );
    }

    /**
     * This method inserts a cite marker in the text (at the cursor) for the given
     * BibEntry, and may refresh the bibliography.
     *
     * @param entries       The entries to cite.
     * @param database      The database the entry belongs to.
     * @param style         The bibliography style we are using.
     * @param inParenthesis Indicates whether it is an in-text citation
     *                      or a citation in parenthesis.
     *                      This is not relevant if numbered citations are used.
     * @param withText      Indicates whether this should be a normal citation (true)
     *                      or an empty (invisible) citation (false).
     * @param sync          Indicates whether the reference list should be refreshed.
     *
     * @throws IllegalTypeException
     * @throws PropertyExistException
     * @throws NotRemoveableException
     * @throws UnknownPropertyException
     * @throws UndefinedCharacterFormatException
     * @throws NoSuchElementException
     * @throws WrappedTargetException
     * @throws IOException
     * @throws PropertyVetoException
     * @throws CreationException
     * @throws BibEntryNotFoundException
     * @throws UndefinedParagraphFormatException
     *
     * TODO: https://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XUndoManager.html
     * Group changes into a single Undo context.
     *
     */
    public void insertEntry(List<BibEntry>    entries,
                            BibDatabase       database,
                            List<BibDatabase> allBases,
                            OOBibStyle        style,
                            boolean           inParenthesis,
                            boolean           withText,
                            String            pageInfo,
                            boolean           sync
                            )
        throws IllegalArgumentException,
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
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        try {
            // Get the cursor positioned by the user.
            XTextCursor cursor = documentConnection.getViewCursor();

            sortBibEntryListForMulticite( entries, style );

            String keyString =
                String.join(",",
                            entries.stream()
                            .map( entry -> entry.getCitationKey().orElse("") )
                            .collect( Collectors.toList() )
                            );
            // Generate unique bookmark-name
            int    citationType = citationTypeFromOptions( withText, inParenthesis );
            String bName        = getUniqueReferenceMarkName( documentConnection,
                                                              keyString,
                                                              citationType );

            // If we should store metadata for page info, do that now:
            if (pageInfo != null) {
                LOGGER.info("Storing page info: " + pageInfo);
                documentConnection.setCustomProperty(bName, pageInfo);
            }

            // insert space
            cursor
                .getText()
                .insertString(cursor, " ", false);

            // format the space inserted
            if ( style.isFormatCitations() ) {
                XPropertySet xCursorProps = unoQI(XPropertySet.class, cursor);
                String       charStyle    = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch ( UnknownPropertyException
                        | PropertyVetoException
                        | IllegalArgumentException
                        | WrappedTargetException ex
                        )
                    {
                        // Setting the character format failed, so we
                        // throw an exception that will result in an
                        // error message for the user.
                        //
                        // Before that, delete the space we inserted:
                        cursor.goLeft((short) 1, true);
                        cursor.setString("");
                        throw new UndefinedCharacterFormatException(charStyle);
                    }
            }

            // go back to before the space
            cursor.goLeft((short) 1, false);

            // Insert bookmark and text
            {
                // Create a BibEntry to BibDatabase map (to make
                // style.getCitationMarker happy?)
                Map<BibEntry, BibDatabase> databaseMap = new HashMap<>();
                for (BibEntry entry : entries) {
                    databaseMap.put(entry, database);
                }
                // the text we insert?
                String citeText =
                    style.isNumberEntries()
                    ? "-" // A dash only. Presumably we expect a refresh later.
                    : style.getCitationMarker(entries,
                                              databaseMap,
                                              inParenthesis,
                                              null, // uniquefiers
                                              null  // unlimAuthors
                                              );
                insertReferenceMark(documentConnection, bName, citeText, cursor, withText, style);
            }
            //
            // Move to the right of the space and remember this
            // position: we will come back here in the end.
            //
            cursor.collapseToEnd();
            cursor.goRight((short) 1, false);
            XTextRange position = cursor.getEnd();

            if (sync) {
                // To account for numbering and for uniqiefiers, we
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
                 */
                // Go back to the relevant position:
                try {
                    cursor.gotoRange(position, false);
                } catch ( com.sun.star.uno.RuntimeException ex ){
                    LOGGER.warn("OOBibBase.insertEntry:"
                                +" Could not go back to end of in-text citation", ex);
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
     *         In case of duplicated citation keys, only the first occurrence.
     *         Otherwise their order is preserved.
     *
     *         If name does not match CITE_PATTERN, an empty List<String> is returned.
     */
    private List<String> parseRefMarkNameToUniqueCitationKeys(String name) {
        Optional< ParsedRefMark > op = parseRefMarkName( name );
        if ( op.isPresent() ){
            return op.get().citedKeys.stream().distinct().collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     *  Extract citation keys from names of referenceMarks in the document.
     *
     *  Each citation key is listed only once, in the order of first appearance.
     *
     *   doc.referenceMarks.names.map(parse).flatten.unique
     *
     */
    private List<String> findCitedKeys( DocumentConnection documentConnection )
        throws NoSuchElementException,
               WrappedTargetException,
               NoDocumentException
    {

        List<String> names = getJabRefReferenceMarkNames( documentConnection );

        {
            // assert it supports XTextContent
            XNameAccess xNamedMarks = documentConnection.getReferenceMarks();
            for (String name1 : names) {
                Object bookmark = xNamedMarks.getByName(name1);
                assert (null != unoQI(XTextContent.class, bookmark));
            }
        }

        List<String> keys = new ArrayList<>();
        for (String name1 : names) {
            List<String> newKeys = parseRefMarkNameToUniqueCitationKeys( name1 );
            for (String key : newKeys) {
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }


    /**
     * @return LinkedHashMap, from BibEntry to BibDatabase
     *         Side effect: add citedKeys to citeKeyToBibEntry, using UndefinedBibtexEntry if not found.
     *
     *  If a citedKey is not found, BibEntry is new UndefinedBibtexEntry(citedKey), BibDatabase is null.
     *  If citedKey is found, then
     *          BibEntry is what we found, BibDatabase is the database we found it in.
     *
     *  So:
     *  - result has an entry for each citedKey, in the same order as in citedKeys
     *  - citedKey in the entry is the same as the original citedKey
     *
     */
    private Map<BibEntry, BibDatabase> findCitedEntries(List<BibDatabase> databases,
                                                        List<String> citedKeys,
                                                        Map<String, BibEntry> citeKeyToBibEntry
                                                        )
    {
        Map<BibEntry, BibDatabase> entries = new LinkedHashMap<>();
        for (String citedKey : citedKeys) {
            boolean found = false;
            for (BibDatabase database : databases) {
                Optional<BibEntry> entry = database.getEntryByCitationKey(citedKey);
                if (entry.isPresent()) {
                    entries.put(entry.get(), database);
                    citeKeyToBibEntry.put( citedKey, entry.get() );
                    found = true;
                    break;
                }
            }

            if (!found) {
                BibEntry x = new UndefinedBibtexEntry(citedKey);
                entries.put(x, null);
                citeKeyToBibEntry.put( citedKey, x );
            }
        }
        return entries;
    }

    /**
     * Refresh all cite markers in the document.
     *
     * @param databases The databases to get entries from.
     * @param style     The bibliography style to use.
     * @return A list of those referenced citation keys that could not be resolved.
     * @throws UndefinedCharacterFormatException
     * @throws NoSuchElementException
     * @throws IllegalArgumentException
     * @throws WrappedTargetException
     * @throws BibEntryNotFoundException
     * @throws CreationException
     * @throws IOException
     * @throws PropertyVetoException
     * @throws UnknownPropertyException
     */
    public List<String> refreshCiteMarkers(List<BibDatabase> databases,
                                           OOBibStyle style)
        throws WrappedTargetException,
               IllegalArgumentException,
               NoSuchElementException,
               UndefinedCharacterFormatException,
               UnknownPropertyException,
               PropertyVetoException,
               IOException,
               CreationException,
               BibEntryNotFoundException,
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        try {
            return refreshCiteMarkersInternal(documentConnection,
                                              databases,
                                              style,
                                              this.xUniquefiers);
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO JAR file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    private static BibEntry[]
        mapCiteKeysToBibEntryArray( String[] keys, // citeKeys
                                    Map<String, BibEntry> citeKeyToBibEntry,
                                    String referenceMarkName,   // for reporting
                                    boolean undefinedToNull     // for undefined entries insert NULL
                                    )
        throws BibEntryNotFoundException
    {
        BibEntry[] cEntries = new BibEntry[keys.length];

        // fill cEntries
        for (int j = 0; j < keys.length; j++) {
            String kj = keys[j];
            BibEntry tmpEntry = citeKeyToBibEntry.get( kj );
            if ( tmpEntry == null ){
                LOGGER.info("Citation key not found: '" + kj + '\'');
                LOGGER.info("Problem with reference mark: '" + referenceMarkName + '\'');
                String msg = Localization.lang("Could not resolve BibTeX entry"
                                               +" for citation marker '%0'.",
                                               referenceMarkName
                                               );
                throw new BibEntryNotFoundException(referenceMarkName, msg);
            } else {
                if ( undefinedToNull && tmpEntry instanceof UndefinedBibtexEntry ){
                    tmpEntry = null;
                }
                cEntries[j] = tmpEntry;
            }

        } // for j
        return cEntries;
    }

    private static String rcmCitationMarkerForIsCitationKeyCiteMarkers( BibEntry[] cEntries,
                                                                        OOBibStyle style )
    {
        assert( style.isCitationKeyCiteMarkers() );

        String citationMarker =
            Arrays.stream(cEntries)
            .map( (c) -> c.getCitationKey().orElse("") )
            .collect(Collectors.joining(","));

        return citationMarker;
    }

    private static String[]
        rcmCitationMarkersForIsCitationKeyCiteMarkers( List<String> referenceMarkNames,
                                                       String[][] bibtexKeys,
                                                       Map<String, BibEntry>  citeKeyToBibEntry,
                                                       OOBibStyle style )
        throws BibEntryNotFoundException
    {
        assert( style.isCitationKeyCiteMarkers() );
        final int nRefMarks = referenceMarkNames.size();
        assert( nRefMarks == bibtexKeys.length );

        String[]   citMarkers     = new String[nRefMarks];
        for (int i = 0; i < referenceMarkNames.size(); i++) {
            final String namei = referenceMarkNames.get(i);

            BibEntry[] cEntries =
                mapCiteKeysToBibEntryArray( bibtexKeys[i], citeKeyToBibEntry, namei, false );
            assert (cEntries.length == bibtexKeys[i].length) ;

            citMarkers[i] = rcmCitationMarkerForIsCitationKeyCiteMarkers( cEntries, style );
        }
        return citMarkers;
    }

    /**
     *  Number source for (1-based) numbering of citations.
     *
     *
     */
    private static class CitationNumberingState {
        public Map<String, Integer> numbers;
        public int lastNum;
        CitationNumberingState(){
            // For numbered citation style. Map( citedKey, number )
            Map<String, Integer> numbers = new HashMap<>();
            int lastNum = 0;
        }
        /**
         *  The first call returns 1.
         */
        public int getOrAllocateNumber( String key ){
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
     *  Get number for a BibEntry. (-1) for UndefinedBibtexEntry
     *
     *  BibEntry.getCitationKey() must not be Optional.empty().
     *
     */
    private static int numberPossiblyUndefinedBibEntry( BibEntry ce,
                                                        CitationNumberingState cns )
    {
        if (ce instanceof UndefinedBibtexEntry){
            return (-1);
        }

        String key = ce.getCitationKey()
            .orElseThrow(IllegalArgumentException::new);

        return cns.getOrAllocateNumber( key );

    }
    /**
     *  Number citations.
     *
     *  @param cEntries  BibEntries to number. Numbering follows this order.
     *  @param cns INOUT Tracks keys already seen and their numbers.
     *                   OUT: Updated, the entries in cEntries are seen.
     *
     *  @return An int for each cEntry. (-1) for UndefinedBibtexEntry
     */
    private static List<Integer> numberPossiblyUndefinedBibEntres(  BibEntry[] cEntries,
                                                                    CitationNumberingState cns )
    {

        if ( false ){
            List<Integer> nums = new ArrayList<>(cEntries.length);
            for (int j = 0; j < cEntries.length; j++) {
                BibEntry cej = cEntries[j];
                String   kj  = cej.getCitationKey().get();
                int num =
                    (cej instanceof UndefinedBibtexEntry)
                    ? (-1)
                    : cns.getOrAllocateNumber(kj)
                    ;
                nums.add(j,num);
            }
            return nums;
        } else {
            // alt
            List<Integer> nums =
                Arrays.stream( cEntries )
                .map( ce -> numberPossiblyUndefinedBibEntry(ce, cns) )
                .collect( Collectors.toList() )
                ;
            return nums;
        }
    }

    private static String []
        rcmCitationMarkersForIsNumberEntriesIsSortByPosition( List<String> referenceMarkNames,
                                                              String[][] bibtexKeys,
                                                              Map<String, BibEntry>  citeKeyToBibEntry,
                                                              OOBibStyle style )
        throws BibEntryNotFoundException
    {
        assert (style.isNumberEntries());
        assert (style.isSortByPosition());

        final int nRefMarks = referenceMarkNames.size();
        assert( nRefMarks == bibtexKeys.length );
        String[]   citMarkers     = new String[nRefMarks];

        // // For numbered citation style. Map( citedKey, number )
        CitationNumberingState cns = new CitationNumberingState();

        final int minGroupingCount =
            style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);

        for (int i = 0; i < referenceMarkNames.size(); i++) {
            final String namei = referenceMarkNames.get(i);

            BibEntry[] cEntries =
                            mapCiteKeysToBibEntryArray( bibtexKeys[i], citeKeyToBibEntry, namei, false );
            assert (cEntries.length == bibtexKeys[i].length) ;

            // Assumption:
            //
            // We have sorted the citation markers according to their
            // order of appearance, so we simply count up for each marker
            // referring to a new entry:
            //
            // nums: Numbers for cEntries, (-1) for none.
            //       Passed to style.getNumCitationMarker()
            //
            //
            // fill nums while adjusting lastNum and filling numbers
            //
            List<Integer> num ;
            num = numberPossiblyUndefinedBibEntres( cEntries, cns );
            citMarkers[i] = style.getNumCitationMarker(num, minGroupingCount, false);
        } // for
        return citMarkers;
    }

    private String[]
        rcmCitationMarkersForIsNumberEntriesNotSortByPosition( List<String> referenceMarkNames,
                                                               String[][] bibtexKeys,
                                                               Map<BibEntry, BibDatabase> entries,
                                                               OOBibStyle style  )
    {
        assert( style.isNumberEntries() );
        assert( ! style.isSortByPosition() );

        final int nRefMarks = referenceMarkNames.size();
        assert( nRefMarks == bibtexKeys.length );
        String[]   citMarkers     = new String[nRefMarks];

        // An exception: numbered entries that are NOT sorted by position
        // exceptional_refmarkorder, entries and cited are sorted
        //if (style.isNumberEntries() && ! style.isSortByPosition()) {
        //
        // sort entries to order in bibliography
        Map<BibEntry, BibDatabase> sortedEntries = sortEntriesByComparator( entries, entryComparator );
        // adjust order of cited to match
        List<String> sortedCited = new ArrayList( entries.size() );
        sortedCited.clear();
        for (BibEntry entry : sortedEntries.keySet()) {
            sortedCited.add(entry.getCitationKey().orElse(null));
        }
        //}

        final int minGroupingCount =
            style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);
        for (int i = 0; i < referenceMarkNames.size(); i++) {
            final String namei = referenceMarkNames.get(i);

            //
            // BibEntry[] cEntries =
            //    mapCiteKeysToBibEntryArray( bibtexKeys[i], citeKeyToBibEntry, namei, false );
            // assert (cEntries.length == bibtexKeys[i].length) ;
            List<Integer> num ;
            num = findCitedEntryIndices( Arrays.asList(bibtexKeys[i]) , sortedCited );
            citMarkers[i] = style.getNumCitationMarker(num, minGroupingCount, false);
        } // for
        return citMarkers;
    }

    /**
     * Resolve the citation key from a citation reference marker name,
     * and look up the index of the key in a list of keys.
     *
     * @param keysCitedHere    The citation keys needing indices.
     * @param orderedCiteKeys  A List of citation keys representing the entries in the bibliography.
     * @return the (1-based) indices of the cited keys, -1 if a key is not found.
     *         Returns Collections.emptyList() if the ref name could not be resolved as a citation.
     */
    private static List<Integer> findCitedEntryIndices(List<String>  keysCitedHere,
                                                       List<String> orderedCiteKeys)
    {
        List<Integer> result        = new ArrayList<>(keysCitedHere.size());
        for (String key : keysCitedHere) {
            int ind = orderedCiteKeys.indexOf(key);
            result.add(ind == -1 ? -1 : 1 + ind);
        }
        return result;
    }

    /**
     *  Visit each reference mark in referenceMarkNames,
     *  remove its text content,
     *  call insertReferenceMark.
     *
     *  After each insertReferenceMark call check if we lost the
     *  OOBibBase.BIB_SECTION_NAME bookmark and recrate it if we did.
     *
     * @param referenceMarkNames      Reference mark names
     * @param citMarkers Corresponding text for each reference mark,
     *                   that replaces the old text.
     * @param types      itcType codes for each reference mark.
     * @param style
     */
    private void rcmApplyNewCitationMarkers(DocumentConnection documentConnection,
                                            List<String> referenceMarkNames,
                                            String[]     citMarkers,
                                            int[]        types,
                                            OOBibStyle   style      )
        throws NoDocumentException,
               NoSuchElementException,
               UndefinedCharacterFormatException,
               UnknownPropertyException,
               CreationException,
               WrappedTargetException,
               PropertyVetoException
    {
        final int nRefMarks  = referenceMarkNames.size();
        assert( citMarkers.length == nRefMarks );
        assert( types.length      == nRefMarks );

        XNameAccess xReferenceMarks = documentConnection.getReferenceMarks();
        final boolean hadBibSection =
            (getBookmarkRange(documentConnection, OOBibBase.BIB_SECTION_NAME) != null);

        // If we are supposed to set character format for citations,
        // must run a test before we delete old citation
        // markers. Otherwise, if the specified character format
        // doesn't exist, we end up deleting the markers before the
        // process crashes due to a the missing format, with
        // catastrophic consequences for the user.
        boolean mustTestCharFormat = style.isFormatCitations();

        for (int i = 0; i < nRefMarks; i++) {
            Object referenceMark = xReferenceMarks.getByName(referenceMarkNames.get(i));
            XTextContent bookmark = unoQI(XTextContent.class, referenceMark);

            XTextCursor cursor =
                bookmark
                .getAnchor()
                .getText()
                .createTextCursorByRange(bookmark.getAnchor());

            if (mustTestCharFormat) {
                mustTestCharFormat = false; // need to do this only once
                XPropertySet xCursorProps = unoQI(XPropertySet.class, cursor);
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

            documentConnection.xText.removeTextContent(bookmark);

            insertReferenceMark(documentConnection,
                                referenceMarkNames.get(i),
                                citMarkers[i],
                                cursor,
                                types[i] != OOBibBase.INVISIBLE_CIT,
                                style
                                );

            if (hadBibSection && (getBookmarkRange(documentConnection,
                                                   OOBibBase.BIB_SECTION_NAME) == null)) {
                // We have overwritten the marker for the start of the reference list.
                // We need to add it again.
                cursor.collapseToEnd();
                OOUtil.insertParagraphBreak(documentConnection.xText, cursor);
                insertBookMark(documentConnection, OOBibBase.BIB_SECTION_NAME, cursor);
            }
        }
    }

        private static void parseRefMarkNamesToArrays( List<String> referenceMarkNames, int[] types, String[][] bibtexKeys ) {
            final int nRefMarks  = referenceMarkNames.size();
            assert( types.length == nRefMarks );
            assert( bibtexKeys.length == nRefMarks );
            for (int i = 0; i < nRefMarks; i++) {
                final String namei = referenceMarkNames.get(i);
                Optional<ParsedRefMark> op = parseRefMarkName( namei );
                if ( !op.isPresent() ) {
                    assert( false );
                    continue;
                }
                ParsedRefMark ov = op.get();
                types[i]      = ov.itcType; // Remember the itcType in case we need to uniquefy.
                bibtexKeys[i] = ov.citedKeys.stream().toArray(String[]::new);
            }
        }

        private static List<String> unresolvedKeysFromEntries( Map<BibEntry, BibDatabase> entries ){
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

    private List<String> refreshCiteMarkersInternal(DocumentConnection documentConnection,
                                                    List<BibDatabase> databases,
                                                    OOBibStyle style,
                                                    final Map<String, String> uniquefiers
                                                    )
        throws WrappedTargetException,
               IllegalArgumentException,
               NoSuchElementException,
               UndefinedCharacterFormatException,
               UnknownPropertyException,
               PropertyVetoException,
               CreationException,
               BibEntryNotFoundException,
               NoDocumentException
    {

        // Normally we sort the reference marks according to their
        // order of appearance:
        List<String> referenceMarkNames = jabRefReferenceMarkNamesSortedByPosition;
        //
        // Compute citation markers for all citations:
        final int nRefMarks  = referenceMarkNames.size();
        int[]      types      = new int[nRefMarks];
        String[][] bibtexKeys = new String[nRefMarks][];
        //
        // fill:
        //    types[i]      = ov.itcType
        //    bibtexKeys[i] = ov.citedKeys.toArray()
        parseRefMarkNamesToArrays( referenceMarkNames, types, bibtexKeys );
        //
        // An exception: numbered entries that are NOT sorted by position
        // I think in this case we do not care, since numbering comes from
        // order in cited
        //
        //        if ( false ){
        //            if (style.isNumberEntries() && ! style.isSortByPosition()) {
        //                XNameAccess xReferenceMarks = documentConnection.getReferenceMarks();
        //                // isNumberEntries && !isSortByPosition
        //                referenceMarkNames = Arrays.asList(xReferenceMarks.getElementNames());
        //                // Remove all reference marks that don't look like JabRef citations:
        //                referenceMarkNames = filterIsJabRefReferenceMarkName( referenceMarkNames );
        //            }
        //        }
        //


        // keys cited in the text
        List<String>               cited = findCitedKeys( documentConnection );
        Map<String, BibEntry>      citeKeyToBibEntry   = new HashMap<>();
        Map<BibEntry, BibDatabase> entries = findCitedEntries(databases, cited, citeKeyToBibEntry);
        // entries are now in same order as cited
        //
        //
        String[]   citMarkers  = new String[nRefMarks];
        // fill:
        //    citMarkers[i] = what goes in the text


        // fill citMarkers
        if (style.isCitationKeyCiteMarkers()) {
           citMarkers = rcmCitationMarkersForIsCitationKeyCiteMarkers( referenceMarkNames, bibtexKeys, citeKeyToBibEntry, style );
           uniquefiers.clear();
        } else if (style.isNumberEntries()) {

            if (style.isSortByPosition()) {
                citMarkers = rcmCitationMarkersForIsNumberEntriesIsSortByPosition(referenceMarkNames, bibtexKeys, citeKeyToBibEntry, style);
            } else {
                citMarkers = rcmCitationMarkersForIsNumberEntriesNotSortByPosition(referenceMarkNames, bibtexKeys, entries, style  );
            }
            uniquefiers.clear();

        } else {

            assert( !style.isCitationKeyCiteMarkers() );
            assert( !style.isNumberEntries() );
            // Citations in (Au1, Au2 2000) form

            //    normCitMarkers[i][j] = for unification
            String[][] normCitMarkers = new String[nRefMarks][];

            for (int i = 0; i < referenceMarkNames.size(); i++) {
                final String namei = referenceMarkNames.get(i);

                BibEntry[] cEntries =
                    mapCiteKeysToBibEntryArray( bibtexKeys[i], citeKeyToBibEntry, namei, false );
                assert (cEntries.length == bibtexKeys[i].length) ;

                // sort itcBlock
                sortBibEntryArrayForMulticite( cEntries, style );

                // Update key list to match the new sorting:
                for (int j = 0; j < cEntries.length; j++) {
                    bibtexKeys[i][j] = cEntries[j].getCitationKey().orElse(null);
                }

                citMarkers[i] = style.getCitationMarker( Arrays.asList(cEntries), // entries
                                                         entries, // database
                                                         types[i] == OOBibBase.AUTHORYEAR_PAR,
                                                         null, // uniquefiers
                                                         null  // unlimAuthors
                                                         );

                // We need "normalized" (in parenthesis) markers
                // for uniqueness checking purposes:
                //
                // normCitMarker[ cEntries.length ] null if missing
                String[] normCitMarker = new String[cEntries.length];
                for (int j = 0; j < cEntries.length; j++) {
                    List<BibEntry> cej = Collections.singletonList(cEntries[j]);
                    normCitMarker[j] =
                        style.getCitationMarker( cej,      // entries
                                                 entries,  // database
                                                 true,     // inParenthesis
                                                 null,     // uniquefiers
                                                 new int[] {-1} // unlimAuthors
                                                 );
                }
                normCitMarkers[i] = normCitMarker;
            }
            uniquefiers.clear();

            // The following block
            // changes: citMarkers[i], uniquefiers
            // uses: nRefMarks, normCitMarkers, bibtexKeys,
            //       style (style.getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST))
            //       citeKeyToBibEntry, entries, types
            //
            if (!style.isCitationKeyCiteMarkers() && !style.isNumberEntries()) {
                // Only for normal citations. Numbered citations and
                // citeKeys are already unique.

                // See if there are duplicate citations marks referring to
                // different entries. If so, we need to use uniquefiers:

                // refKeys: normCitMarker to list of bibtexkeys sharing it.
                //          The entries in the lists are ordered as in
                //          normCitMarkers[i][j]
                Map<String, List<String>>  refKeys = new HashMap<>();

                for (int i = 0; i < nRefMarks; i++) {
                    // Compare normalized markers, since the actual
                    // markers can be different.
                    String[] markers = normCitMarkers[i];
                    for (int j = 0; j < markers.length; j++) {
                        String marker     = markers[j];
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
                // uniquefy:
                for (Map.Entry<String, List<String>> stringListEntry : refKeys.entrySet()) {
                    List<String> clashingKeys = stringListEntry.getValue();
                    if (clashingKeys.size() > 1) {
                        // This marker appears for more than one unique entry:
                        int uniq = 'a';
                        for (String key : clashingKeys) {
                            // Update the map of uniquefiers for the
                            // benefit of both the following generation of
                            // new citation markers, and for the method
                            // that builds the bibliography:
                            uniquefiers.put(key, String.valueOf((char) uniq));
                            uniq++;
                        }
                    }
                }

                // Finally, go through all citation markers, and update
                // those referring to entries in our current list:
                final int maxAuthorsFirst = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST);
                Set<String> seenBefore = new HashSet<>();
                for (int i = 0; i < nRefMarks; i++) {
                    final String namei = referenceMarkNames.get(i);
                    final int  nCitedEntries = bibtexKeys[i].length;
                    boolean    needsChange     = false;
                    int[]      firstLimAuthors = new int[nCitedEntries];
                    String[]   uniquif         = new String[nCitedEntries];
                    BibEntry[] cEntries = mapCiteKeysToBibEntryArray( bibtexKeys[i],
                                                                      citeKeyToBibEntry,
                                                                      namei,
                                                                      true);

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

                        {
                            String uniq = uniquefiers.get(currentKey);
                            if (uniq == null) {
                                uniquif[j] = "";
                            } else {
                                uniquif[j] = uniq;
                                needsChange = true;
                            }
                        }

                        if (firstLimAuthors[j] > 0) {
                            needsChange = true;
                        }


                    } // for j

                    if (needsChange) {
                        citMarkers[i] =
                            style.getCitationMarker( Arrays.asList(cEntries),
                                                     entries,
                                                     types[i] == OOBibBase.AUTHORYEAR_PAR,
                                                     uniquif,
                                                     firstLimAuthors
                                                     );
                    }
                } // for i
            } // if normalStyle
        }


        // Refresh all reference marks with the citation markers we computed:
        rcmApplyNewCitationMarkers(documentConnection, referenceMarkNames, citMarkers, types, style );

        return unresolvedKeysFromEntries( entries );
    }



    // Position as in a document on the screen.
    // Probably to get the correct order with
    // referenceMarks in footnotes
    private static Point findPositionOfTextRange(XTextViewCursor cursor, XTextRange range) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
        // the cursor's coordinates relative to the top left position
        // of the first page of the document.
    }

    /**
     *
     */
    private List<String>
        getJabRefReferenceMarkNamesSortedByPosition(DocumentConnection documentConnection)
        throws WrappedTargetException,
               NoSuchElementException,
               NoDocumentException
    {

        List<String> names = getJabRefReferenceMarkNames(documentConnection);


        // find coordinates
        List<Point> positions = new ArrayList<>(names.size());
        {
            XNameAccess nameAccess = documentConnection.getReferenceMarks();
            XTextViewCursor viewCursor = documentConnection.getViewCursor();
            // initialPos: to be restored before return
            XTextRange initialPos = viewCursor.getStart();
            for (String name : names) {

                XTextContent textContent =
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
        }

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
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();
        this.jabRefReferenceMarkNamesSortedByPosition =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);
    }

    /**
     * GUI action
     *
     * @return unresolvedKeys
     */
    public List<String>  updateDocumentActionHelper(List<BibDatabase> databases,
                                                    OOBibStyle style)
        throws NoSuchElementException,
               WrappedTargetException,
               IllegalArgumentException,
               CreationException,
               PropertyVetoException,
               UnknownPropertyException,
               UndefinedParagraphFormatException,
               NoDocumentException,
               UndefinedCharacterFormatException,
               BibEntryNotFoundException,
               IOException
    {
        updateSortedReferenceMarks();
        List<String> unresolvedKeys = refreshCiteMarkers(databases, style);
        rebuildBibTextSection(databases, style);
        return unresolvedKeys;
    }


    public void rebuildBibTextSection(List<BibDatabase> databases,
                                      OOBibStyle style)
        throws NoSuchElementException,
               WrappedTargetException,
               IllegalArgumentException,
               CreationException,
               PropertyVetoException,
               UnknownPropertyException,
               UndefinedParagraphFormatException,
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        List<String>               cited             = findCitedKeys(documentConnection);
        Map<String, BibEntry>      citeKeyToBibEntry = new HashMap<>();
        Map<BibEntry, BibDatabase> entries =
            // Although entries are redefined without use, this also
            // updates citeKeyToBibEntry
            findCitedEntries(databases, cited, citeKeyToBibEntry);

        if (style.isSortByPosition()) {
            // We need to sort the entries according to their order of appearance:
            entries = sortEntriesByRefMarkNames(
                 jabRefReferenceMarkNamesSortedByPosition,
                 citeKeyToBibEntry,
                 entries
                 );
        } else {
            entries = sortEntriesByComparator( entries, entryComparator );
        }
        clearBibTextSectionContent2(documentConnection);
        populateBibTextSection(documentConnection, entries, style, this.xUniquefiers);
    }

    SortedMap<BibEntry, BibDatabase>
        sortEntriesByComparator( Map<BibEntry, BibDatabase> entries,
                                 Comparator<BibEntry> entryComparator )
    {
        SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
        for (Map.Entry<BibEntry, BibDatabase> kv : entries.entrySet()) {
            newMap.put(kv.getKey(),
                       kv.getValue());
        }
        return newMap;
    }

    /**
     * @param referenceMarkNames
     * @param citeKeyToBibEntry Helps to find the entries
     *
     * @return LinkedHashMap from BibEntry to BibDatabase with
     * iteration order as first appearance in referenceMarkNames.
     */
    private Map<BibEntry, BibDatabase>
        sortEntriesByRefMarkNames(List<String> referenceMarkNames,
                                  Map<String, BibEntry> citeKeyToBibEntry,
                                  Map<BibEntry, BibDatabase> entries
                                  )
    {

        // LinkedHashMap: iteration order is insertion-order, not
        // affected if a key is re-inserted.
        Map<BibEntry, BibDatabase> newList = new LinkedHashMap<>();

        for (String name : referenceMarkNames) {
            Optional<ParsedRefMark> op = parseRefMarkName( name );
            if ( ! op.isPresent() ){ continue; }

            List<String> keys = op.get().citedKeys;
            // no need to look in the database again
            for (String key : keys) {
                BibEntry origEntry    = citeKeyToBibEntry.get(key);
                if (origEntry != null) {
                    if (!newList.containsKey(origEntry)) {
                        BibDatabase database = entries.get( origEntry );
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
     *  Only called from populateBibTextSection (and that from rebuildBibTextSection)
     */
    private void insertFullReferenceAtCursor(DocumentConnection documentConnection,
                                             XTextCursor cursor,
                                             Map<BibEntry, BibDatabase> entries,
                                             OOBibStyle style,
                                             String parFormat,
                                             final Map<String, String> uniquefiers)
            throws UndefinedParagraphFormatException,
                   IllegalArgumentException,
                   UnknownPropertyException,
                   PropertyVetoException,
                   WrappedTargetException
    {

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
                String marker = style.getNumCitationMarker(Collections.singletonList(number++),
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
            OOUtil.insertFullReferenceAtCurrentLocation
                (documentConnection.xText,
                 cursor,
                 layout,
                 parFormat,
                 entry.getKey(),
                 entry.getValue(),
                 uniquefiers.get(entry.getKey().getCitationKey().orElse(null))
                 );
        }
    }

    private void createBibTextSection2(DocumentConnection documentConnection,
                                       boolean end)
            throws IllegalArgumentException,
                   CreationException
    {

        XTextCursor mxDocCursor = documentConnection.xText.createTextCursor();
        if (end) {
            mxDocCursor.gotoEnd(false);
        }
        OOUtil.insertParagraphBreak(documentConnection.xText, mxDocCursor);
        // Create a new TextSection from the document factory and access it's XNamed interface
        XNamed xChildNamed;
        try {
            xChildNamed = unoQI(XNamed.class,
                                (documentConnection.mxDocFactory
                                 .createInstance("com.sun.star.text.TextSection"))
                                );
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // Set the new sections name to 'Child_Section'
        xChildNamed.setName(OOBibBase.BIB_SECTION_NAME);
        // Access the Child_Section's XTextContent interface and insert it into the document
        XTextContent xChildSection = unoQI(XTextContent.class, xChildNamed);
        documentConnection.xText.insertTextContent(mxDocCursor, xChildSection, false);
    }

    private void clearBibTextSectionContent2(DocumentConnection documentConnection)
        throws NoSuchElementException,
               WrappedTargetException,
               IllegalArgumentException,
               CreationException
    {

        // Check if the section exists:
        XTextSectionsSupplier supplier =
            unoQI(XTextSectionsSupplier.class, documentConnection.mxDoc);

        com.sun.star.container.XNameAccess ts = supplier.getTextSections();

        if (ts.hasByName(OOBibBase.BIB_SECTION_NAME)) {
            try {
                Any a = ((Any) ts.getByName(OOBibBase.BIB_SECTION_NAME));
                XTextSection section = (XTextSection) a.getObject();
                // Clear it:
                XTextCursor cursor =
                    documentConnection.xText.createTextCursorByRange(section.getAnchor());
                cursor.gotoRange(section.getAnchor(), false);
                cursor.setString("");
                return;
            } catch ( NoSuchElementException ex ) {
                // NoSuchElementException: is thrown by child access
                // methods of collections, if the addressed child does
                // not exist.
                //
                // We got this exception from ts.getByName() despite the ts.hasByName() check
                // just above.
                // Try to create.
                LOGGER.warn( "Could not get section '"+ OOBibBase.BIB_SECTION_NAME + "'", ex );
                createBibTextSection2(documentConnection, atEnd);
            }
        } else {
            createBibTextSection2(documentConnection, atEnd);
        }
    }

    /**
     * Only called from: rebuildBibTextSection
     */
    private void populateBibTextSection(DocumentConnection documentConnection,
                                        Map<BibEntry, BibDatabase> entries,
                                        OOBibStyle style,
                                        final Map<String, String> uniquefiers)
        throws NoSuchElementException,
               WrappedTargetException,
               PropertyVetoException,
               UnknownPropertyException,
               UndefinedParagraphFormatException,
               IllegalArgumentException,
               CreationException
    {
        XTextSectionsSupplier supplier =
            unoQI(XTextSectionsSupplier.class,
                  documentConnection.mxDoc);

        XTextSection section =
            ( (XTextSection)
              ((Any) supplier
               .getTextSections()
               .getByName(OOBibBase.BIB_SECTION_NAME)
               )
              .getObject()
              );

        XTextCursor cursor =
            documentConnection.xText
            .createTextCursorByRange(section.getAnchor());

        OOUtil.insertTextAtCurrentLocation
            (documentConnection.xText,
             cursor,
             (String) style.getProperty(OOBibStyle.TITLE),
             (String) style.getProperty(OOBibStyle.REFERENCE_HEADER_PARAGRAPH_FORMAT)
             );

        {
            String refParaFormat =
                (String) style.getProperty(OOBibStyle.REFERENCE_PARAGRAPH_FORMAT);
            insertFullReferenceAtCursor(documentConnection,
                                        cursor,
                                        entries,
                                        style,
                                        refParaFormat,
                                        uniquefiers
                                        );
        }
        insertBookMark(documentConnection, OOBibBase.BIB_SECTION_END_NAME, cursor);
    }

    private XTextContent insertBookMark(DocumentConnection documentConnection,
                                        String name,
                                        XTextCursor position)
        throws IllegalArgumentException,
               CreationException
    {
        Object bookmark;
        try {
            bookmark = ( documentConnection.mxDocFactory
                         .createInstance("com.sun.star.text.Bookmark") );
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }

        // name the bookmark
        XNamed xNamed = unoQI(XNamed.class, bookmark);
        xNamed.setName(name);

        // get XTextContent interface
        XTextContent xTextContent = unoQI(XTextContent.class, bookmark);

        // insert bookmark at position
        documentConnection.xText.insertTextContent(position, xTextContent, true);
        position.collapseToEnd();
        return xTextContent;
    }

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

        // Check if there is "page info" stored for this citation. If so, insert it into
        // the citation text before inserting the citation:
        Optional<String> pageInfo = documentConnection.getCustomProperty(name);
        String citText;
        if ((pageInfo.isPresent()) && !pageInfo.get().isEmpty()) {
            citText = style.insertPageInfo(citationText, pageInfo.get());
        } else {
            citText = citationText;
        }

        Object bookmark;
        try {
            bookmark = documentConnection.mxDocFactory.createInstance("com.sun.star.text.ReferenceMark");
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
                } catch (UnknownPropertyException | PropertyVetoException | IllegalArgumentException |
                        WrappedTargetException ex) {
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }
        } else {
            position.setString("");
        }

        // get XTextContent interface
        XTextContent xTextContent = unoQI(XTextContent.class, bookmark);

        position.getText().insertTextContent(position, xTextContent, true);

        // Check if we should italicize the "et al." string in citations:
        boolean italicize = style.getBooleanCitProperty(OOBibStyle.ITALIC_ET_AL);
        if (italicize) {
            String etAlString = style.getStringCitProperty(OOBibStyle.ET_AL_STRING);
            int index = citText.indexOf(etAlString);
            if (index >= 0) {
                italicizeOrBold(position, true, index, index + etAlString.length());
            }
        }

        position.collapseToEnd();
    }

    private void removeReferenceMark(DocumentConnection documentConnection, String name)
        throws NoSuchElementException,
               WrappedTargetException,
               NoDocumentException
    {
        XNameAccess xReferenceMarks = documentConnection.getReferenceMarks();
        if (xReferenceMarks.hasByName(name)) {
            Object referenceMark = xReferenceMarks.getByName(name);
            XTextContent bookmark = unoQI(XTextContent.class, referenceMark);
            documentConnection.xText.removeTextContent(bookmark);
        }
    }

    /**
     * Get the XTextRange corresponding to the named bookmark.
     *
     * @param name The name of the bookmark to find.
     * @return The XTextRange for the bookmark.
     * @throws WrappedTargetException
     * @throws NoSuchElementException
     */
    private XTextRange getBookmarkRange(DocumentConnection documentConnection,
                                        String name)
        throws NoSuchElementException,
               WrappedTargetException
    {
        XNameAccess xNamedBookmarks = getBookmarks(documentConnection);

        // retrieve bookmark by name
        if (!xNamedBookmarks.hasByName(name)) {
            return null;
        }
        Object foundBookmark = xNamedBookmarks.getByName(name);
        XTextContent xFoundBookmark = unoQI(XTextContent.class, foundBookmark);
        return xFoundBookmark.getAnchor();
    }

    private XNameAccess getBookmarks(DocumentConnection documentConnection) {
        // query XBookmarksSupplier from document model
        // and get bookmarks collection
        XBookmarksSupplier xBookmarksSupplier =
            unoQI(XBookmarksSupplier.class,
                  documentConnection.xCurrentComponent
                  );
        XNameAccess xNamedBookmarks =
            xBookmarksSupplier.getBookmarks();
        return xNamedBookmarks;
    }

    private void italicizeOrBold(XTextCursor position, boolean italicize, int start, int end)
            throws UnknownPropertyException,
                   PropertyVetoException,
                   IllegalArgumentException,
                   WrappedTargetException
    {
        XTextRange range = position.getStart();
        XTextCursor cursor = position.getText().createTextCursorByRange(range);
        cursor.goRight((short) start, false);
        cursor.goRight((short) (end - start), true);
        XPropertySet xcp = unoQI(XPropertySet.class, cursor);
        if (italicize) {
            xcp.setPropertyValue("CharPosture", com.sun.star.awt.FontSlant.ITALIC);
        } else {
            xcp.setPropertyValue("CharWeight", com.sun.star.awt.FontWeight.BOLD);
        }
    }

    /**
     *  GUI action
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
                   NoDocumentException
    {
        DocumentConnection documentConnection = this.xDocumentConnection;

        // TODO: doesn't work for citations in footnotes/tables
        List<String> names =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                documentConnection.xText);

        int piv = 0;
        boolean madeModifications = false;
        XNameAccess nameAccess = documentConnection.getReferenceMarks();
        while (piv < (names.size() - 1)) {
            XTextRange range1 = unoQI(XTextContent.class, nameAccess.getByName(names.get(piv)))
                                          .getAnchor().getEnd();
            XTextRange range2 = unoQI(XTextContent.class, nameAccess.getByName(names.get(piv + 1)))
                                          .getAnchor().getStart();
            if (range1.getText() != range2.getText()) {
                piv++;
                continue;
            }
            XTextCursor mxDocCursor = range1.getText().createTextCursorByRange(range1);
            mxDocCursor.goRight((short) 1, true);
            boolean couldExpand = true;
            while (couldExpand && (compare.compareRegionEnds(mxDocCursor, range2) > 0)) {
                couldExpand = mxDocCursor.goRight((short) 1, true);
            }
            String cursorText = mxDocCursor.getString();
            // Check if the string contains no line breaks and only whitespace:
            if ((cursorText.indexOf('\n') == -1) && cursorText.trim().isEmpty()) {

                // If we are supposed to set character format for citations, test this before
                // making any changes. This way we can throw an exception before any reference
                // marks are removed, preventing damage to the user's document:
                if (style.isFormatCitations()) {
                    XPropertySet xCursorProps = unoQI(XPropertySet.class, mxDocCursor);
                    String charStyle = style.getCitationCharacterFormat();
                    try {
                        xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                    } catch (UnknownPropertyException | PropertyVetoException | IllegalArgumentException |
                            WrappedTargetException ex) {
                        // Setting the character format failed, so we throw an exception that
                        // will result in an error message for the user:
                        throw new UndefinedCharacterFormatException(charStyle);
                    }
                }

                List<String> keys = parseRefMarkNameToUniqueCitationKeys(names.get(piv));
                keys.addAll(parseRefMarkNameToUniqueCitationKeys(names.get(piv + 1)));
                removeReferenceMark(documentConnection, names.get(piv));
                removeReferenceMark(documentConnection, names.get(piv + 1));
                List<BibEntry> entries = new ArrayList<>();
                for (String key : keys) {
                    for (BibDatabase database : databases) {
                        Optional<BibEntry> entry = database.getEntryByCitationKey(key);
                        if (entry.isPresent()) {
                            entries.add(entry.get());
                            break;
                        }
                    }
                }
                Collections.sort(entries, new FieldComparator(StandardField.YEAR));
                String keyString = String.join(",", entries.stream().map(entry -> entry.getCitationKey().orElse(""))
                                                           .collect(Collectors.toList()));
                // Insert bookmark:
                String bName = getUniqueReferenceMarkName(documentConnection,
                                                          keyString,
                                                          OOBibBase.AUTHORYEAR_PAR
                                                          );
                insertReferenceMark(documentConnection, bName, "tmp", mxDocCursor, true, style);
                names.set(piv + 1, bName);
                madeModifications = true;
            }
            piv++;
        }
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
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        // TODO: doesn't work for citations in footnotes/tables
        List<String> names =
            getJabRefReferenceMarkNamesSortedByPosition(documentConnection);

        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class,
                                                documentConnection.xText);

        int piv = 0;
        boolean madeModifications = false;
        XNameAccess nameAccess = documentConnection.getReferenceMarks();
        while (piv < (names.size())) {
            XTextRange range1 = unoQI(XTextContent.class, nameAccess.getByName(names.get(piv)))
                .getAnchor();

            XTextCursor mxDocCursor = range1.getText().createTextCursorByRange(range1);
            //
            // If we are supposed to set character format for citations, test this before
            // making any changes. This way we can throw an exception before any reference
            // marks are removed, preventing damage to the user's document:
            if (style.isFormatCitations()) {
                XPropertySet xCursorProps = unoQI(XPropertySet.class, mxDocCursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch (UnknownPropertyException | PropertyVetoException | IllegalArgumentException |
                         WrappedTargetException ex) {
                    // Setting the character format failed, so we throw an exception that
                    // will result in an error message for the user:
                        throw new UndefinedCharacterFormatException(charStyle);
                }
            }

            List<String> keys = parseRefMarkNameToUniqueCitationKeys(names.get(piv));
            if (keys.size() > 1) {
                removeReferenceMark(documentConnection, names.get(piv));
                //
                // Insert bookmark for each key
                int last = keys.size() - 1;
                int i = 0;
                for (String key : keys) {
                    String bName = getUniqueReferenceMarkName(documentConnection,
                                                              key,
                                                              OOBibBase.AUTHORYEAR_PAR
                                                              );
                    insertReferenceMark(documentConnection, bName, "tmp", mxDocCursor, true, style);
                    mxDocCursor.collapseToEnd();
                    if (i != last) {
                        mxDocCursor.setString(" ");
                        mxDocCursor.collapseToEnd();
                    }
                    i++;
                }
                madeModifications = true;
            }
            piv++;
        }
        if (madeModifications) {
            updateSortedReferenceMarks();
            refreshCiteMarkers(databases, style);
        }
    }

    /**
     *  Used from GUI.
     */
    public BibDatabase generateDatabase(List<BibDatabase> databases)
        throws NoSuchElementException,
               WrappedTargetException,
               NoDocumentException
    {
        DocumentConnection documentConnection = getDocumentConnectionOrThrow();

        BibDatabase resultDatabase = new BibDatabase();
        List<String> cited = findCitedKeys(documentConnection);
        List<BibEntry> entriesToInsert = new ArrayList<>();

        // For each cited key
        for (String key : cited) {
            // Loop through the available databases
            for (BibDatabase loopDatabase : databases) {
                Optional<BibEntry> entry = loopDatabase.getEntryByCitationKey(key);
                // If entry found
                if (entry.isPresent()) {
                    BibEntry clonedEntry = (BibEntry) entry.get().clone();
                    // Insert a copy of the entry
                    entriesToInsert.add(clonedEntry);
                    // Check if the cloned entry has a crossref field
                    clonedEntry.getField(StandardField.CROSSREF).ifPresent(crossref -> {
                        // If the crossref entry is not already in the database
                        if (!resultDatabase.getEntryByCitationKey(crossref).isPresent()) {
                            // Add it if it is in the current library
                            loopDatabase.getEntryByCitationKey(crossref).ifPresent(entriesToInsert::add);
                        }
                    });

                    // Be happy with the first found BibEntry and move on to next key
                    break;
                }
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
                return (this.position.X == other.position.X) && (this.position.Y == other.position.Y)
                        && Objects.equals(this.name, other.name);
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
