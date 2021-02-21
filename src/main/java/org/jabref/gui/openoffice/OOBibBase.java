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
	Pattern.compile(BIB_CITATION + "\\d*_(\\d*)_(.*)");

    // Another pattern, to also recover the "i" part
    private static final Pattern CITE_PATTERN2 =
	Pattern.compile(BIB_CITATION + "(\\d*)_(\\d*)_(.*)");

    private static final String CHAR_STYLE_NAME = "CharStyleName";

    /* Types of in-text citation.
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

    /* document-related */
    private XMultiServiceFactory    mxDocFactory;
    private XTextDocument           mxDoc;
    private XText                   xtext;
    private XTextViewCursorSupplier xViewCursorSupplier;
    private XComponent              xCurrentComponent;
    private XPropertySet            propertySet;
    private XPropertyContainer      userProperties;


    private final Map<String, String> uniquefiers = new HashMap<>();

    private List<String> sortedReferenceMarks;

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

    public boolean isConnectedToDocument() {
        return this.xCurrentComponent != null;
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

        // TODO: http://api.openoffice.org/docs/DevelopersGuide/OfficeDev/OfficeDev.xhtml#1_1_3_2_1_2_Frame_Hierarchies

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
     *     - this.xtext
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
	    this.xCurrentComponent = unoQI(XComponent.class, selected);
	    this.mxDoc = selected;
	}


	// TODO: what is the point of the next line? Does it have a side effect?
	if ( run_useless_parts ){
	    unoQI(XDocumentIndexesSupplier.class, xCurrentComponent);
	}

	{
	    XModel      mo = unoQI(XModel.class, this.xCurrentComponent);
	    XController co = mo.getCurrentController();
	    this.xViewCursorSupplier = unoQI(XTextViewCursorSupplier.class, co);
	}

        // get a reference to the body text of the document
        this.xtext = this.mxDoc.getText();

        // Access the text document's multi service factory:
        this.mxDocFactory = unoQI(XMultiServiceFactory.class, this.mxDoc);

	{
	    XDocumentPropertiesSupplier supp =
		unoQI(XDocumentPropertiesSupplier.class, this.mxDoc);
	    this.userProperties = supp.getDocumentProperties().getUserDefinedProperties();
	}
        this.propertySet = unoQI(XPropertySet.class, this.userProperties);
    }

    /*
     *  Getters useful after selectDocument()
     */

    public Optional<String> getCurrentDocumentTitle() {
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

    /*
     * === insertEntry
     */

    private void sortBibEntryList( List<BibEntry>    entries,
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

    private static int citationTypeFromOptions( boolean withText, boolean inParenthesis ){
	if ( !withText ){
	    return OOBibBase.INVISIBLE_CIT ;
	}
	return ( inParenthesis
		 ? OOBibBase.AUTHORYEAR_PAR
		 : OOBibBase.AUTHORYEAR_INTEXT );
    }

    /*
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
	    return Optional.of(res);
	} catch ( Exception ex ){
	    LOGGER.warn( "getReferenceMarks caught: ", ex );
	    throw NoDocumentException("getReferenceMarks failed");
	}
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
    private List<String> getJabRefReferenceMarkNames(XNameAccess nameAccess) {
        String[] names = nameAccess.getElementNames();
        // Remove all reference marks that don't look like JabRef citations:
	/*
	 * List<String> result = new ArrayList<>();
	 * if (names != null) {
	 *     for (String name : names) {
         *       if (CITE_PATTERN.matcher(name).find()) {
         *           result.add(name);
         *       }
         *   }
	 * }
	 * return result;
	 */
	if (names == null) {
	    return new ArrayList<>();
	}
	return filterIsJabRefReferenceMarkName( Arrays.asList( names ) );
    }

    /**
     *
     */
    public List<CitationEntry> getCitationEntries()
	throws NoSuchElementException,
	       UnknownPropertyException,
	       WrappedTargetException
    {
        XNameAccess nameAccess = this.getReferenceMarks();
        List<String> names = this.getJabRefReferenceMarkNames(nameAccess);
	List<CitationEntry> citations = new ArrayList(names.size());
        for (String name : names) {
            CitationEntry entry =
		new CitationEntry(name,
				  this.getCitationContext(nameAccess, name, 30, 30, true),
				  this.getCustomProperty(name)
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
	       IllegalArgumentException
    {
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
		this.setCustomProperty( entry.getRefMarkName(), pageInfo.get() );
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

    /*
     * The first occurrence of bibtexKey gets no serial number, the
     * second gets 0, the third 1 ...
     *
     * Or the first unused in this series, after removals.
     *
     */
    private String getUniqueReferenceMarkName(String bibtexKey, int type) {
        XNameAccess xNamedRefMarks = getReferenceMarks();
        int i = 0;
        String name = BIB_CITATION + '_' + type + '_' + bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = BIB_CITATION + i + '_' + type + '_' + bibtexKey;
            i++;
        }
        return name;
    }

    private class ParsedRefMark {
	public String i ; // "", "0", "1" ...
	public int type ;
	public List<String> citedKeys;
	ParsedRefMark( String i, int type, List<String> citedKeys ){
	    this.i=i;
	    this.type = type;
	    this.citedKeys = citedKeys;
	}
    }
    private Optional<ParsedRefMark> parseRefMarkName( String name ){
        Matcher citeMatcher = CITE_PATTERN2.matcher(name);
        if (!citeMatcher.find()) {
	    return Optional.empty();
	}
	List<String> keys = Arrays.asList( citeMatcher.group(3).split(",") );
	String i = citeMatcher.group(1);
	int type = Integer.parseInt( citeMatcher.group(2) );
	return( Optional.of( new ParsedRefMark( i, type, keys ) ) );
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
	       UndefinedParagraphFormatException
    {
        try {
	    XTextCursor cursor;
	    {
		// Get the cursor positioned by the user.
		XTextViewCursor xViewCursor =
		    this.xViewCursorSupplier
		    .getViewCursor();
		if ( true ){
		    cursor = xViewCursor;
		} else {
		    //
		    // An XTextCursor is sufficient for the rest.
		    //
		    // https://wiki.openoffice.org/wiki/Documentation/DevGuide/Text/\
		    //      Example:_Visible_Cursor_Position
		    //
		    // We create a model cursor at the current view cursor
		    // position with the following steps: we get the Text
		    // service from the TextViewCursor, the cursor is an
		    // XTextRange and has therefore a method getText()
		    //
		    XText xDocumentText = xViewCursor.getText();
		    // the text creates a model cursor from the viewcursor
		    XTextCursor xModelCursor =
			xDocumentText.createTextCursorByRange(xViewCursor.getStart());
		    // use the xModelCursor
		    cursor = xModelCursor;
		}
	    }

	    sortBibEntryList( entries, style );

            String keyString =
		String.join(",",
			    entries.stream()
			    .map( entry -> entry.getCitationKey().orElse("") )
			    .collect( Collectors.toList() )
			    );
	    // Generate unique bookmark-name
	    int    citationType = citationTypeFromOptions( withText, inParenthesis );
            String bName        = getUniqueReferenceMarkName( keyString, citationType );

            // If we should store metadata for page info, do that now:
            if (pageInfo != null) {
                LOGGER.info("Storing page info: " + pageInfo);
                setCustomProperty(bName, pageInfo);
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
					      null,
					      null
					      );
		insertReferenceMark(bName, citeText, cursor, withText, style);
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
    private List<String> findCitedKeys()
	throws NoSuchElementException,
	       WrappedTargetException
    {
        XNameAccess xNamedMarks = getReferenceMarks();
        String[] names          = xNamedMarks.getElementNames();

        List<String> keys = new ArrayList<>();
        for (String name1 : names) {
            Object bookmark = xNamedMarks.getByName(name1);
            assert (null != unoQI(XTextContent.class, bookmark));

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
     * @return LinkedHashMap, from BibEntry to BibDatabase
     *
     *  If a key is not found, BibEntry is new UndefinedBibtexEntry(key), BibDatabase is null.
     *  If key is found, then
     *          BibEntry is what we found, BibDatabase is the database we found it in.
     *          linkSourceBase.put(key, database); is called.
     *
     *  So:
     *  - result has an entry for each key, in the same order
     *  - key in the entry is the same as the original key
     *  - on return linkSourceBase has an entry for the keys we did find
     */
    private Map<BibEntry, BibDatabase> findCitedEntries(List<BibDatabase> databases,
							List<String> keys,
                                                        Map<String, BibDatabase> linkSourceBase)
    {
        Map<BibEntry, BibDatabase> entries = new LinkedHashMap<>();
        for (String key : keys) {
            boolean found = false;
            for (BibDatabase database : databases) {
                Optional<BibEntry> entry = database.getEntryByCitationKey(key);
                if (entry.isPresent()) {
                    entries.put(entry.get(), database);
                    linkSourceBase.put(key, database);
                    found = true;
                    break;
                }
            }

            if (!found) {
                entries.put(new UndefinedBibtexEntry(key), null);
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
	       BibEntryNotFoundException
    {
        try {
            return refreshCiteMarkersInternal(databases, style);
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO JAR file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    private static Optional<BibEntry> linkSourceBaseCiteKeyToBibEntry( Map<String, BibDatabase> linkSourceBase,
								       String citeKey )
    {
	BibDatabase database = linkSourceBase.get(citeKey);
	Optional<BibEntry> res = ( (database == null)
				   ? Optional.empty()
				   : database.getEntryByCitationKey(citeKey)
				   );
	return res;
    }

    private static BibEntry[] linkSourceBaseGetBibEntriesOfCiteKeys( Map<String, BibDatabase> linkSourceBase,
								     String[] keys, // citeKeys
								     String namei   // refMarkName
								     )
	throws BibEntryNotFoundException
    {
	BibEntry[] cEntries = new BibEntry[keys.length];
	// fill cEntries
	for (int j = 0; j < keys.length; j++) {
	    String kj = keys[j];
	    Optional<BibEntry> tmpEntry =
		linkSourceBaseCiteKeyToBibEntry( linkSourceBase, kj );
	    if (tmpEntry.isPresent()) {
		cEntries[j] = tmpEntry.get();
	    } else {
		LOGGER.info("Citation key not found: '" + kj + '\'');
		LOGGER.info("Problem with reference mark: '" + namei + '\'');
		String msg = Localization.lang("Could not resolve BibTeX entry"
					       +" for citation marker '%0'.",
					       namei
					       );
		throw new BibEntryNotFoundException(namei, msg);
	    }
	} // for j
	return cEntries;
    }

    private List<String> refreshCiteMarkersInternal(List<BibDatabase> databases,
						    OOBibStyle style)
	throws WrappedTargetException,
	       IllegalArgumentException,
	       NoSuchElementException,
	       UndefinedCharacterFormatException,
	       UnknownPropertyException,
	       PropertyVetoException,
	       CreationException,
	       BibEntryNotFoundException
    {
        List<String> cited = findCitedKeys();
        Map<String, BibDatabase>   linkSourceBase = new HashMap<>();
        Map<BibEntry, BibDatabase> entries =
	    findCitedEntries(databases, cited, linkSourceBase);

        XNameAccess xReferenceMarks = getReferenceMarks();

        List<String> names;
        if (style.isSortByPosition()) {
            // We need to sort the reference marks according to their
            // order of appearance:
            names = sortedReferenceMarks;
        } else if (style.isNumberEntries()) {
            // We need to sort the reference marks according to the
            // sorting of the bibliographic entries:
            SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
            for (Map.Entry<BibEntry, BibDatabase> ee : entries.entrySet()) {
                newMap.put(ee.getKey(), ee.getValue());
            }
            entries = newMap;
            // Rebuild the list of cited keys according to the sort order:
            cited.clear();
            for (BibEntry entry : entries.keySet()) {
                cited.add(entry.getCitationKey().orElse(null));
            }
            names = Arrays.asList(xReferenceMarks.getElementNames());
        } else {
            names = sortedReferenceMarks;
        }

        // Remove all reference marks that don't look like JabRef citations:
	names = filterIsJabRefReferenceMarkName( names );

        Map<String, Integer> numbers = new HashMap<>();
        int lastNum = 0;
        // First compute citation markers for all citations:
        String[]   citMarkers     = new String[names.size()];
        String[][] normCitMarkers = new String[names.size()][];
        String[][] bibtexKeys     = new String[names.size()][];

        final int minGroupingCount =
	    style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);

        int[] types = new int[names.size()];
        for (int i = 0; i < names.size(); i++) {
	    final String namei = names.get(i);
	    Optional<ParsedRefMark> op = parseRefMarkName( namei );
            if ( op.isPresent() ) {
		ParsedRefMark ov = op.get();
		int type = ov.type;
                types[i] = type; // Remember the type in case we need to uniquefy.
                String[] keys = ov.citedKeys.stream().toArray(String[]::new);
                bibtexKeys[i] = keys;
		//
		/*
                BibEntry[] cEntries = new BibEntry[keys.length];
		// fill cEntries
                for (int j = 0; j < keys.length; j++) {
		    String kj = keys[j];
		    Optional<BibEntry> tmpEntry =
			linkSourceBaseCiteKeyToBibEntry( linkSourceBase, kj );
                    if (tmpEntry.isPresent()) {
                        cEntries[j] = tmpEntry.get();
                    } else {
                        LOGGER.info("Citation key not found: '" + kj + '\'');
                        LOGGER.info("Problem with reference mark: '" + namei + '\'');
			String msg = Localization.lang("Could not resolve BibTeX entry"
						       +" for citation marker '%0'.",
						       namei
						       );
                        throw new BibEntryNotFoundException(namei, msg);
                    }
                } // for j
		*/
		BibEntry[] cEntries = linkSourceBaseGetBibEntriesOfCiteKeys( linkSourceBase, keys, namei );
                String[] normCitMarker = new String[keys.length];
                String citationMarker;
                if (style.isCitationKeyCiteMarkers()) {
                    StringBuilder sb = new StringBuilder();
                    normCitMarkers[i] = new String[keys.length];
                    for (int j = 0; j < keys.length; j++) {
                        normCitMarkers[i][j] = cEntries[j].getCitationKey().orElse(null);
                        sb.append(cEntries[j].getCitationKey().orElse(""));
                        if (j < (keys.length - 1)) {
                            sb.append(',');
                        }
                    }
                    citationMarker = sb.toString();
                } else if (style.isNumberEntries()) {
                    if (style.isSortByPosition()) {
                        // We have sorted the citation markers according to their order of appearance,
                        // so we simply count up for each marker referring to a new entry:
                        List<Integer> num = new ArrayList<>(keys.length);
                        for (int j = 0; j < keys.length; j++) {
                            if (cEntries[j] instanceof UndefinedBibtexEntry) {
                                num.add(j, -1);
                            } else {
                                num.add(j, lastNum + 1);
                                if (numbers.containsKey(keys[j])) {
                                    num.set(j, numbers.get(keys[j]));
                                } else {
                                    numbers.put(keys[j], num.get(j));
                                    lastNum = num.get(j);
                                }
                            }
                        }
                        citationMarker = style.getNumCitationMarker(num, minGroupingCount, false);
                        for (int j = 0; j < keys.length; j++) {
                            normCitMarker[j] = style.getNumCitationMarker(Collections.singletonList(num.get(j)),
                                    minGroupingCount, false);
                        }
                    } else {
                        // We need to find the number of the cited entry in the bibliography,
                        // and use that number for the cite marker:
                        List<Integer> num = findCitedEntryIndex(names.get(i), cited);

                        if (num.isEmpty()) {
                            throw new BibEntryNotFoundException(names.get(i), Localization
                                    .lang("Could not resolve BibTeX entry for citation marker '%0'.", names.get(i)));
                        } else {
                            citationMarker = style.getNumCitationMarker(num, minGroupingCount, false);
                        }

                        for (int j = 0; j < keys.length; j++) {
                            List<Integer> list = new ArrayList<>(1);
                            list.add(num.get(j));
                            normCitMarker[j] = style.getNumCitationMarker(list, minGroupingCount, false);
                        }
                    }
                } else {

                    if (cEntries.length > 1) {
                        if (style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL)) {
                            Arrays.sort(cEntries, yearAuthorTitleComparator);
                        } else {
                            Arrays.sort(cEntries, entryComparator);
                        }
                        // Update key list to match the new sorting:
                        for (int j = 0; j < cEntries.length; j++) {
                            bibtexKeys[i][j] = cEntries[j].getCitationKey().orElse(null);
                        }
                    }

                    citationMarker = style.getCitationMarker(Arrays.asList(cEntries), entries,
                            type == OOBibBase.AUTHORYEAR_PAR, null, null);
                    // We need "normalized" (in parenthesis) markers for uniqueness checking purposes:
                    for (int j = 0; j < cEntries.length; j++) {
                        normCitMarker[j] = style.getCitationMarker(Collections.singletonList(cEntries[j]), entries,
                                true, null, new int[] {-1});
                    }
                }
                citMarkers[i] = citationMarker;
                normCitMarkers[i] = normCitMarker;
            } // if (citeMatcher.find())
        } // for i

        uniquefiers.clear();
        if (!style.isCitationKeyCiteMarkers() && !style.isNumberEntries()) {
            // See if there are duplicate citations marks referring to different entries. If so, we need to
            // use uniquefiers:
            Map<String, List<String>> refKeys = new HashMap<>();
            Map<String, List<Integer>> refNums = new HashMap<>();
            for (int i = 0; i < citMarkers.length; i++) {
                String[] markers = normCitMarkers[i]; // compare normalized markers, since the actual markers can be different
                for (int j = 0; j < markers.length; j++) {
                    String marker = markers[j];
                    String currentKey = bibtexKeys[i][j];
                    if (refKeys.containsKey(marker)) {
                        // Ok, we have seen this exact marker before.
                        if (!refKeys.get(marker).contains(currentKey)) {
                            // ... but not for this entry.
                            refKeys.get(marker).add(currentKey);
                            refNums.get(marker).add(i);
                        }
                    } else {
                        List<String> l = new ArrayList<>(1);
                        l.add(currentKey);
                        refKeys.put(marker, l);
                        List<Integer> l2 = new ArrayList<>(1);
                        l2.add(i);
                        refNums.put(marker, l2);
                    }
                }
            }
            // Go through the collected lists and see where we need to uniquefy:
            for (Map.Entry<String, List<String>> stringListEntry : refKeys.entrySet()) {
                List<String> keys = stringListEntry.getValue();
                if (keys.size() > 1) {
                    // This marker appears for more than one unique entry:
                    int uniq = 'a';
                    for (String key : keys) {
                        // Update the map of uniquefiers for the benefit of both the following generation of new
                        // citation markers, and for the method that builds the bibliography:
                        uniquefiers.put(key, String.valueOf((char) uniq));
                        uniq++;
                    }
                }
            }

            // Finally, go through all citation markers, and update those referring to entries in our current list:
            int maxAuthorsFirst = style.getIntCitProperty(OOBibStyle.MAX_AUTHORS_FIRST);
            Set<String> seenBefore = new HashSet<>();
            for (int j = 0; j < bibtexKeys.length; j++) {
                boolean needsChange = false;
                int[] firstLimAuthors = new int[bibtexKeys[j].length];
                String[] uniquif = new String[bibtexKeys[j].length];
                BibEntry[] cEntries = new BibEntry[bibtexKeys[j].length];
                for (int k = 0; k < bibtexKeys[j].length; k++) {
                    String currentKey = bibtexKeys[j][k];
                    firstLimAuthors[k] = -1;
                    if (maxAuthorsFirst > 0) {
                        if (!seenBefore.contains(currentKey)) {
                            firstLimAuthors[k] = maxAuthorsFirst;
                        }
                        seenBefore.add(currentKey);
                    }
                    String uniq = uniquefiers.get(currentKey);
                    Optional<BibEntry> tmpEntry = Optional.empty();
                    if (uniq == null) {
                        if (firstLimAuthors[k] > 0) {
                            needsChange = true;
                            BibDatabase database = linkSourceBase.get(currentKey);
                            if (database != null) {
                                tmpEntry = database.getEntryByCitationKey(currentKey);
                            }
                        } else {
                            BibDatabase database = linkSourceBase.get(currentKey);
                            if (database != null) {
                                tmpEntry = database.getEntryByCitationKey(currentKey);
                            }
                        }
                        uniquif[k] = "";
                    } else {
                        needsChange = true;
                        BibDatabase database = linkSourceBase.get(currentKey);
                        if (database != null) {
                            tmpEntry = database.getEntryByCitationKey(currentKey);
                        }
                        uniquif[k] = uniq;
                    }
                    if (tmpEntry.isPresent()) {
                        cEntries[k] = tmpEntry.get();
                    }
                }
                if (needsChange) {
                    citMarkers[j] = style.getCitationMarker(Arrays.asList(cEntries), entries,
                            types[j] == OOBibBase.AUTHORYEAR_PAR, uniquif, firstLimAuthors);
                }
            }
        }

        // Refresh all reference marks with the citation markers we computed:
        boolean hadBibSection = getBookmarkRange(OOBibBase.BIB_SECTION_NAME) != null;
        // Check if we are supposed to set a character format for citations:
        boolean mustTestCharFormat = style.isFormatCitations();
        for (int i = 0; i < names.size(); i++) {
            Object referenceMark = xReferenceMarks.getByName(names.get(i));
            XTextContent bookmark = unoQI(XTextContent.class, referenceMark);

            XTextCursor cursor =
		bookmark
		.getAnchor()
		.getText()
		.createTextCursorByRange(bookmark.getAnchor());

            if (mustTestCharFormat) {
                // If we are supposed to set character format for citations, must run a test before we
                // delete old citation markers. Otherwise, if the specified character format doesn't
                // exist, we end up deleting the markers before the process crashes due to a the missing
                // format, with catastrophic consequences for the user.
                mustTestCharFormat = false; // need to do this only once
                XPropertySet xCursorProps = unoQI(XPropertySet.class, cursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch (UnknownPropertyException | PropertyVetoException | IllegalArgumentException |
                        WrappedTargetException ex) {
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }

            this.xtext.removeTextContent(bookmark);

            insertReferenceMark(names.get(i), citMarkers[i], cursor, types[i] != OOBibBase.INVISIBLE_CIT, style);
            if (hadBibSection && (getBookmarkRange(OOBibBase.BIB_SECTION_NAME) == null)) {
                // We have overwritten the marker for the start of the reference list.
                // We need to add it again.
                cursor.collapseToEnd();
                OOUtil.insertParagraphBreak(this.xtext, cursor);
                insertBookMark(OOBibBase.BIB_SECTION_NAME, cursor);
            }
        }

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

    private List<String> getSortedReferenceMarks(final XNameAccess nameAccess)
            throws WrappedTargetException,
		   NoSuchElementException
    {
        XTextViewCursorSupplier cursorSupplier =
	    unoQI(XTextViewCursorSupplier.class,
		  this.mxDoc.getCurrentController());

        XTextViewCursor viewCursor = cursorSupplier.getViewCursor();
        XTextRange initialPos = viewCursor.getStart();
        List<String> names = Arrays.asList(nameAccess.getElementNames());
        List<Point> positions = new ArrayList<>(names.size());
        for (String name : names) {
            XTextContent textContent =
		unoQI(XTextContent.class, nameAccess.getByName(name));
            XTextRange range = textContent.getAnchor();
            // Check if we are inside a footnote:
            if (unoQI(XFootnote.class, range.getText()) != null) {
                // Find the linking footnote marker:
                XFootnote footer = unoQI(XFootnote.class, range.getText());
                // The footnote's anchor gives the correct position in the text:
                range = footer.getAnchor();
            }

            positions.add(findPosition(viewCursor, range));
        }
        Set<ComparableMark> set = new TreeSet<>();
        for (int i = 0; i < positions.size(); i++) {
            set.add(new ComparableMark(names.get(i), positions.get(i)));
        }

        List<String> result = new ArrayList<>(set.size());
        for (ComparableMark mark : set) {
            result.add(mark.getName());
        }
        viewCursor.gotoRange(initialPos, false);

        return result;
    }

    public void updateSortedReferenceMarks()
	throws WrappedTargetException,
	       NoSuchElementException
    {
        this.sortedReferenceMarks = getSortedReferenceMarks(getReferenceMarks());
    }


    public void rebuildBibTextSection(List<BibDatabase> databases,
				      OOBibStyle style)
	throws NoSuchElementException,
	       WrappedTargetException,
	       IllegalArgumentException,
	       CreationException,
	       PropertyVetoException,
	       UnknownPropertyException,
	       UndefinedParagraphFormatException
    {
        List<String>               cited          = findCitedKeys();
        Map<String, BibDatabase>   linkSourceBase = new HashMap<>();
        Map<BibEntry, BibDatabase> entries =
	    // Although entries are redefined without use, this also
	    // updates linkSourceBase
	    findCitedEntries(databases, cited, linkSourceBase);

        if (style.isSortByPosition()) {
            // We need to sort the entries according to their order of appearance:
	    List<String> names = sortedReferenceMarks;
            entries = getSortedEntriesFromSortedRefMarks(names, linkSourceBase);
        } else {
	    // Find them again? Why?
	    Map<BibEntry, BibDatabase> entries2 =
		findCitedEntries(databases, cited, linkSourceBase);

            SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
            for (Map.Entry<BibEntry, BibDatabase> kv : entries2.entrySet()) {
		newMap.put(kv.getKey(),
			   kv.getValue());
	    }
            entries = newMap;
        }
        clearBibTextSectionContent2();
        populateBibTextSection(entries, style);
    }


    private Point findPosition(XTextViewCursor cursor, XTextRange range) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
    }


    /**
     * Resolve the citation key from a citation reference marker name,
     * and look up the index of the key in a list of keys.
     *
     * @param citRefName The name of the ReferenceMark representing the citation.
     * @param keys       A List of citation keys representing the entries in the bibliography.
     * @return the (1-based) indices of the cited keys, -1 if a key is not found.
     *         Returns Collections.emptyList() if the ref name could not be resolved as a citation.
     */
    private List<Integer> findCitedEntryIndex(String citRefName, List<String> keys) {
	/*
	 * Matcher citeMatcher = CITE_PATTERN.matcher(citRefName);
	 * if (citeMatcher.find()) {
	 *     List<String> keyStrings = Arrays.asList(citeMatcher.group(2).split(","));
	 *     List<Integer> result = new ArrayList<>(keyStrings.size());
	 *     for (String key : keyStrings) {
	 *         int ind = keys.indexOf(key);
	 *         result.add(ind == -1 ? -1 : 1 + ind);
	 *     }
	 *     return result;
	 * } else {
	 *     return Collections.emptyList();
	 * }
	 */
	Optional< ParsedRefMark > op = parseRefMarkName( citRefName );
	if ( !op.isPresent() ){
	    return Collections.emptyList();
	}
	List<String> keyStrings = op.get().citedKeys;
	List<Integer> result = new ArrayList<>(keyStrings.size());
	for (String key : keyStrings) {
	    int ind = keys.indexOf(key);
	    result.add(ind == -1 ? -1 : 1 + ind);
	}
	return result;
    }

    private Map<BibEntry, BibDatabase> getSortedEntriesFromSortedRefMarks(List<String> names,
                                                                          Map<String, BibDatabase> linkSourceBase) {

        Map<BibEntry, BibDatabase> newList = new LinkedHashMap<>();
        for (String name : names) {
	    /*
	     * Matcher citeMatcher = CITE_PATTERN.matcher(name);
	     *  if (citeMatcher.find()) {
             *    String[] keys = citeMatcher.group(2).split(",");
	     */
	    Optional<ParsedRefMark> op = parseRefMarkName( name );
	    if ( ! op.isPresent() ){ continue; }
	    List<String> keys = op.get().citedKeys;
	    for (String key : keys) {
		BibDatabase        database  = linkSourceBase.get(key);
		Optional<BibEntry> origEntry = Optional.empty();
		if (database != null) {
		    origEntry = database.getEntryByCitationKey(key);
		}
		if (origEntry.isPresent()) {
		    BibEntry oe = origEntry.get();
		    if (!newList.containsKey(oe)) {
			newList.put(oe, database);
		    }
		} else {
		    LOGGER.info("Citation key not found: '" + key + "'");
		    LOGGER.info("Problem with reference mark: '" + name + "'");
		    newList.put(new UndefinedBibtexEntry(key), null);
		}
	    }
	    /*  } */
        }
        return newList;
    }

    public String getCitationContext(XNameAccess nameAccess, String refMarkName, int charBefore, int charAfter,
                                     boolean htmlMarkup)
            throws NoSuchElementException, WrappedTargetException {
        Object referenceMark = nameAccess.getByName(refMarkName);
        XTextContent bookmark = unoQI(XTextContent.class, referenceMark);

        XTextCursor cursor = bookmark.getAnchor().getText().createTextCursorByRange(bookmark.getAnchor());
        String citPart = cursor.getString();
        int flex = 8;
        for (int i = 0; i < charBefore; i++) {
            try {
                cursor.goLeft((short) 1, true);
                if ((i >= (charBefore - flex)) && Character.isWhitespace(cursor.getString().charAt(0))) {
                    break;
                }
            } catch (IndexOutOfBoundsException ex) {
                LOGGER.warn("Problem going left", ex);
            }
        }
        int length = cursor.getString().length();
        int added = length - citPart.length();
        cursor.collapseToStart();
        for (int i = 0; i < (charAfter + length); i++) {
            try {
                cursor.goRight((short) 1, true);
                if (i >= ((charAfter + length) - flex)) {
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
            result = result.substring(0, added) + "<b>" + citPart + "</b>" + result.substring(length);
        }
        return result.trim();
    }

    private void insertFullReferenceAtCursor(XTextCursor cursor, Map<BibEntry, BibDatabase> entries, OOBibStyle style,
                                             String parFormat)
            throws UndefinedParagraphFormatException, IllegalArgumentException,
            UnknownPropertyException, PropertyVetoException, WrappedTargetException {
        Map<BibEntry, BibDatabase> correctEntries;
        // If we don't have numbered entries, we need to sort the entries before adding them:
        if (style.isSortByPosition()) {
            // Use the received map directly
            correctEntries = entries;
        } else {
            // Sort map
            Map<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
            newMap.putAll(entries);
            correctEntries = newMap;
        }
        int number = 1;
        for (Map.Entry<BibEntry, BibDatabase> entry : correctEntries.entrySet()) {
            if (entry.getKey() instanceof UndefinedBibtexEntry) {
                continue;
            }
            OOUtil.insertParagraphBreak(this.xtext, cursor);
            if (style.isNumberEntries()) {
                int minGroupingCount = style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);
                OOUtil.insertTextAtCurrentLocation(this.xtext, cursor,
                        style.getNumCitationMarker(Collections.singletonList(number++), minGroupingCount, true), Collections.emptyList());
            }
            Layout layout = style.getReferenceFormat(entry.getKey().getType());
            layout.setPostFormatter(POSTFORMATTER);
            OOUtil.insertFullReferenceAtCurrentLocation(this.xtext, cursor, layout, parFormat, entry.getKey(),
                    entry.getValue(), uniquefiers.get(entry.getKey().getCitationKey().orElse(null)));
        }
    }

    private void createBibTextSection2(boolean end)
            throws IllegalArgumentException, CreationException {

        XTextCursor mxDocCursor = this.xtext.createTextCursor();
        if (end) {
            mxDocCursor.gotoEnd(false);
        }
        OOUtil.insertParagraphBreak(this.xtext, mxDocCursor);
        // Create a new TextSection from the document factory and access it's XNamed interface
        XNamed xChildNamed;
        try {
            xChildNamed = unoQI(XNamed.class,
                    this.mxDocFactory.createInstance("com.sun.star.text.TextSection"));
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // Set the new sections name to 'Child_Section'
        xChildNamed.setName(OOBibBase.BIB_SECTION_NAME);
        // Access the Child_Section's XTextContent interface and insert it into the document
        XTextContent xChildSection = unoQI(XTextContent.class, xChildNamed);
        this.xtext.insertTextContent(mxDocCursor, xChildSection, false);
    }

    private void clearBibTextSectionContent2()
            throws NoSuchElementException, WrappedTargetException, IllegalArgumentException, CreationException {

        // Check if the section exists:
        XTextSectionsSupplier supplier = unoQI(XTextSectionsSupplier.class, this.mxDoc);
	com.sun.star.container.XNameAccess ts = supplier.getTextSections();
        if (ts.hasByName(OOBibBase.BIB_SECTION_NAME)) {
	    try {
		Any a = ((Any) ts.getByName(OOBibBase.BIB_SECTION_NAME));
		XTextSection section = (XTextSection) a.getObject();
		// Clear it:
		XTextCursor cursor = this.xtext.createTextCursorByRange(section.getAnchor());
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
		createBibTextSection2(atEnd);
	    }
        } else {
            createBibTextSection2(atEnd);
        }
    }

    private void populateBibTextSection(Map<BibEntry, BibDatabase> entries, OOBibStyle style)
            throws NoSuchElementException, WrappedTargetException, PropertyVetoException,
            UnknownPropertyException, UndefinedParagraphFormatException, IllegalArgumentException, CreationException {
        XTextSectionsSupplier supplier = unoQI(XTextSectionsSupplier.class, this.mxDoc);
        XTextSection section = (XTextSection) ((Any) supplier.getTextSections().getByName(OOBibBase.BIB_SECTION_NAME))
                .getObject();
        XTextCursor cursor = this.xtext.createTextCursorByRange(section.getAnchor());
        OOUtil.insertTextAtCurrentLocation(this.xtext, cursor, (String) style.getProperty(OOBibStyle.TITLE),
                (String) style.getProperty(OOBibStyle.REFERENCE_HEADER_PARAGRAPH_FORMAT));
        insertFullReferenceAtCursor(cursor, entries, style,
                (String) style.getProperty(OOBibStyle.REFERENCE_PARAGRAPH_FORMAT));
        insertBookMark(OOBibBase.BIB_SECTION_END_NAME, cursor);
    }

    private XTextContent insertBookMark(String name, XTextCursor position)
            throws IllegalArgumentException, CreationException {
        Object bookmark;
        try {
            bookmark = this.mxDocFactory.createInstance("com.sun.star.text.Bookmark");
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // name the bookmark
        XNamed xNamed = unoQI(XNamed.class, bookmark);
        xNamed.setName(name);
        // get XTextContent interface
        XTextContent xTextContent = unoQI(XTextContent.class, bookmark);
        // insert bookmark at the end of the document
        // instead of mxDocText.getEnd you could use a text cursor's XTextRange interface or any XTextRange
        this.xtext.insertTextContent(position, xTextContent, true);
        position.collapseToEnd();
        return xTextContent;
    }

    private void insertReferenceMark(String name, String citationText, XTextCursor position, boolean withText,
                                     OOBibStyle style)
            throws UnknownPropertyException, WrappedTargetException,
            PropertyVetoException, IllegalArgumentException, UndefinedCharacterFormatException, CreationException {

        // Check if there is "page info" stored for this citation. If so, insert it into
        // the citation text before inserting the citation:
        Optional<String> pageInfo = getCustomProperty(name);
        String citText;
        if ((pageInfo.isPresent()) && !pageInfo.get().isEmpty()) {
            citText = style.insertPageInfo(citationText, pageInfo.get());
        } else {
            citText = citationText;
        }

        Object bookmark;
        try {
            bookmark = this.mxDocFactory.createInstance("com.sun.star.text.ReferenceMark");
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

    private void removeReferenceMark(String name) throws NoSuchElementException, WrappedTargetException {
        XNameAccess xReferenceMarks = getReferenceMarks();
        if (xReferenceMarks.hasByName(name)) {
            Object referenceMark = xReferenceMarks.getByName(name);
            XTextContent bookmark = unoQI(XTextContent.class, referenceMark);
            this.xtext.removeTextContent(bookmark);
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
    private XTextRange getBookmarkRange(String name) throws NoSuchElementException, WrappedTargetException {
        XNameAccess xNamedBookmarks = getBookmarks();

        // retrieve bookmark by name
        if (!xNamedBookmarks.hasByName(name)) {
            return null;
        }
        Object foundBookmark = xNamedBookmarks.getByName(name);
        XTextContent xFoundBookmark = unoQI(XTextContent.class, foundBookmark);
        return xFoundBookmark.getAnchor();
    }

    private XNameAccess getBookmarks() {
        // query XBookmarksSupplier from document model and get bookmarks collection
        XBookmarksSupplier xBookmarksSupplier = unoQI(XBookmarksSupplier.class, xCurrentComponent);
        XNameAccess xNamedBookmarks = xBookmarksSupplier.getBookmarks();
        return xNamedBookmarks;
    }

    private void italicizeOrBold(XTextCursor position, boolean italicize, int start, int end)
            throws UnknownPropertyException, PropertyVetoException, IllegalArgumentException, WrappedTargetException {
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

    public void combineCiteMarkers(List<BibDatabase> databases, OOBibStyle style)
            throws IOException, WrappedTargetException, NoSuchElementException, IllegalArgumentException,
            UndefinedCharacterFormatException, UnknownPropertyException, PropertyVetoException, CreationException,
            BibEntryNotFoundException {
        XNameAccess nameAccess = getReferenceMarks();
        // TODO: doesn't work for citations in footnotes/tables
        List<String> names = getSortedReferenceMarks(nameAccess);

        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class, this.xtext);

        int piv = 0;
        boolean madeModifications = false;
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
                removeReferenceMark(names.get(piv));
                removeReferenceMark(names.get(piv + 1));
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
                String bName = getUniqueReferenceMarkName(keyString, OOBibBase.AUTHORYEAR_PAR);
                insertReferenceMark(bName, "tmp", mxDocCursor, true, style);
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
     * Do the opposite of combineCiteMarkers.
     * Combined markers are split, with a space inserted between.
     */
    public void unCombineCiteMarkers(List<BibDatabase> databases, OOBibStyle style)
            throws IOException, WrappedTargetException, NoSuchElementException, IllegalArgumentException,
            UndefinedCharacterFormatException, UnknownPropertyException, PropertyVetoException, CreationException,
            BibEntryNotFoundException {
        XNameAccess nameAccess = getReferenceMarks();
        // TODO: doesn't work for citations in footnotes/tables
        List<String> names = getSortedReferenceMarks(nameAccess);

        final XTextRangeCompare compare = unoQI(XTextRangeCompare.class, this.xtext);

        int piv = 0;
        boolean madeModifications = false;
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
                removeReferenceMark(names.get(piv));
                //
                // Insert bookmark for each key
                int last = keys.size() - 1;
                int i = 0;
                for (String key : keys) {
                    String bName = getUniqueReferenceMarkName(key, OOBibBase.AUTHORYEAR_PAR);
                    insertReferenceMark(bName, "tmp", mxDocCursor, true, style);
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

    public BibDatabase generateDatabase(List<BibDatabase> databases)
            throws NoSuchElementException, WrappedTargetException {
        BibDatabase resultDatabase = new BibDatabase();
        List<String> cited = findCitedKeys();
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
