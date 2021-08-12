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
import org.jabref.logic.openoffice.OOUtil;
import org.jabref.logic.openoffice.UndefinedBibtexEntry;
import org.jabref.logic.openoffice.UndefinedParagraphFormatException;
import org.jabref.logic.openoffice.style.OOBibStyle;
import org.jabref.logic.openoffice.style.OOPreFormatter;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.openoffice.uno.CreationException;
import org.jabref.model.openoffice.uno.NoDocumentException;

import com.sun.star.awt.Point;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
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
 * Class for manipulating the Bibliography of the currently start document in OpenOffice.
 */
@AllowedToUseAwt("Requires AWT for italics and bold")
class OOBibBase {

    private static final OOPreFormatter POSTFORMATTER = new OOPreFormatter();

    private static final String BIB_SECTION_NAME = "JR_bib";
    private static final String BIB_SECTION_END_NAME = "JR_bib_end";
    private static final String BIB_CITATION = "JR_cite";
    private static final Pattern CITE_PATTERN = Pattern.compile(OOBibBase.BIB_CITATION + "\\d*_(\\d*)_(.*)");

    private static final String CHAR_STYLE_NAME = "CharStyleName";

    private static final int AUTHORYEAR_PAR = 1;
    private static final int AUTHORYEAR_INTEXT = 2;
    private static final int INVISIBLE_CIT = 3;

    private static final Logger LOGGER = LoggerFactory.getLogger(OOBibBase.class);
    private XMultiServiceFactory mxDocFactory;
    private XTextDocument mxDoc;
    private XText text;
    private final XDesktop xDesktop;
    private XTextViewCursorSupplier xViewCursorSupplier;
    private XComponent xCurrentComponent;
    private XPropertySet propertySet;

    private XPropertyContainer userProperties;
    private final boolean atEnd;
    private final Comparator<BibEntry> entryComparator;
    private final Comparator<BibEntry> yearAuthorTitleComparator;
    private final FieldComparator authComp = new FieldComparator(StandardField.AUTHOR);
    private final FieldComparator yearComp = new FieldComparator(StandardField.YEAR);

    private final FieldComparator titleComp = new FieldComparator(StandardField.TITLE);
    private final List<Comparator<BibEntry>> authorYearTitleList = new ArrayList<>(3);

    private final List<Comparator<BibEntry>> yearAuthorTitleList = new ArrayList<>(3);
    private final Map<String, String> uniquefiers = new HashMap<>();
    private List<String> sortedReferenceMarks;

    private final DialogService dialogService;

    public OOBibBase(Path loPath, boolean atEnd, DialogService dialogService) throws IllegalAccessException, InvocationTargetException, BootstrapException, CreationException, IOException, ClassNotFoundException {

        this.dialogService = dialogService;

        authorYearTitleList.add(authComp);
        authorYearTitleList.add(yearComp);
        authorYearTitleList.add(titleComp);

        yearAuthorTitleList.add(yearComp);
        yearAuthorTitleList.add(authComp);
        yearAuthorTitleList.add(titleComp);

        entryComparator = new FieldComparatorStack<>(authorYearTitleList);
        yearAuthorTitleComparator = new FieldComparatorStack<>(yearAuthorTitleList);

        this.atEnd = atEnd;

        xDesktop = simpleBootstrap(loPath);
    }

    public boolean isConnectedToDocument() {
        return xCurrentComponent != null;
    }

    public XTextDocument selectComponent(List<XTextDocument> list) {
        List<DocumentTitleViewModel> viewModel = list.stream().map(DocumentTitleViewModel::new).collect(Collectors.toList());
        // this whole method is part of a background task when auto-detecting instances, so we need to show dialog in FX thread
        Optional<DocumentTitleViewModel> selectedDocument = dialogService.showChoiceDialogAndWait(Localization.lang("Select document"), Localization.lang("Found documents:"), Localization.lang("Use selected document"), viewModel);
        return selectedDocument.map(DocumentTitleViewModel::getXtextDocument).orElse(null);
    }

    public Optional<String> getCurrentDocumentTitle() {
        return getDocumentTitle(mxDoc);
    }

    private Optional<String> getDocumentTitle(XTextDocument doc) {
        if (doc == null) {
            return Optional.empty();
        } else {
            try {
                return Optional.of(String.valueOf(OOUtil.getProperty(doc.getCurrentController().getFrame(), "Title")));
            } catch (UnknownPropertyException | WrappedTargetException e) {
                LOGGER.warn("Could not get document title", e);
                return Optional.empty();
            }
        }
    }

    public void selectDocument() throws NoDocumentException, NoSuchElementException, WrappedTargetException {
        List<XTextDocument> textDocumentList = getTextDocuments();
        XTextDocument selected;
        if (textDocumentList.isEmpty()) {
            // No text documents found.
            throw new NoDocumentException("No Writer documents found");
        } else if (textDocumentList.size() == 1) {
            // Get the only one
            selected = textDocumentList.get(0);
        } else {
            // Bring up a dialog
            selected = selectComponent(textDocumentList);
        }

        if (selected == null) {
            return;
        }
        xCurrentComponent = UnoRuntime.queryInterface(XComponent.class, selected);
        mxDoc = selected;

        UnoRuntime.queryInterface(XDocumentIndexesSupplier.class, xCurrentComponent);

        XModel xModel = UnoRuntime.queryInterface(XModel.class, xCurrentComponent);
        XController xController = xModel.getCurrentController();
        xViewCursorSupplier = UnoRuntime.queryInterface(XTextViewCursorSupplier.class, xController);

        // get a reference to the body text of the document
        text = mxDoc.getText();

        // Access the text document's multi service factory:
        mxDocFactory = UnoRuntime.queryInterface(XMultiServiceFactory.class, mxDoc);

        XDocumentPropertiesSupplier supp = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, mxDoc);
        userProperties = supp.getDocumentProperties().getUserDefinedProperties();
        propertySet = UnoRuntime.queryInterface(XPropertySet.class, userProperties);
    }

    private List<XTextDocument> getTextDocuments() throws NoSuchElementException, WrappedTargetException {
        List<XTextDocument> result = new ArrayList<>();
        XEnumerationAccess enumAccess = xDesktop.getComponents();
        XEnumeration componentEnumeration = enumAccess.createEnumeration();

        // TODO: http://api.openoffice.org/docs/DevelopersGuide/OfficeDev/OfficeDev.xhtml#1_1_3_2_1_2_Frame_Hierarchies

        while (componentEnumeration.hasMoreElements()) {
            Object nextElement = componentEnumeration.nextElement();
            XComponent component = UnoRuntime.queryInterface(XComponent.class, nextElement);
            XTextDocument document = UnoRuntime.queryInterface(XTextDocument.class, component);
            if (document != null) {
                result.add(document);
            }
        }
        return result;
    }

    private XDesktop simpleBootstrap(Path loPath)
            throws CreationException, BootstrapException {

        // Get the office component context:
        XComponentContext xContext = org.jabref.gui.openoffice.Bootstrap.bootstrap(loPath);
        // Get the office service manager:
        XMultiComponentFactory xServiceManager = xContext.getServiceManager();
        // Create the desktop, which is the root frame of the
        // hierarchy of frames that contain viewable components:
        Object desktop;
        try {
            desktop = xServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        XDesktop resultDesktop = UnoRuntime.queryInterface(XDesktop.class, desktop);

        UnoRuntime.queryInterface(XComponentLoader.class, desktop);

        return resultDesktop;
    }

    public Optional<String> getCustomProperty(String property) throws UnknownPropertyException, WrappedTargetException {
        if (propertySet.getPropertySetInfo().hasPropertyByName(property)) {
            return Optional.ofNullable(propertySet.getPropertyValue(property).toString());
        }
        return Optional.empty();
    }

    public void updateSortedReferenceMarks() throws WrappedTargetException, NoSuchElementException {
        sortedReferenceMarks = getSortedReferenceMarks(getReferenceMarks());
    }

    public void setCustomProperty(String property, String value) throws UnknownPropertyException,
            NotRemoveableException, PropertyExistException, IllegalTypeException, IllegalArgumentException {
        if (propertySet.getPropertySetInfo().hasPropertyByName(property)) {
            userProperties.removeProperty(property);
        }
        if (value != null) {
            userProperties.addProperty(property, com.sun.star.beans.PropertyAttribute.REMOVEABLE,
                    new Any(Type.STRING, value));
        }
    }

    /**
     * This method inserts a cite marker in the text for the given BibEntry, and may refresh the bibliography.
     *
     * @param entries       The entries to cite.
     * @param database      The database the entry belongs to.
     * @param style         The bibliography style we are using.
     * @param inParenthesis Indicates whether it is an in-text citation or a citation in parenthesis. This is not relevant if numbered citations are used.
     * @param withText      Indicates whether this should be a normal citation (true) or an empty (invisible) citation (false).
     * @param sync          Indicates whether the reference list should be refreshed.
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
     */
    public void insertEntry(List<BibEntry> entries, BibDatabase database,
                            List<BibDatabase> allBases, OOBibStyle style,
                            boolean inParenthesis, boolean withText, String pageInfo, boolean sync)
            throws IllegalArgumentException,
            UnknownPropertyException, NotRemoveableException, PropertyExistException, IllegalTypeException,
            UndefinedCharacterFormatException, WrappedTargetException, NoSuchElementException, PropertyVetoException,
            IOException, CreationException, BibEntryNotFoundException, UndefinedParagraphFormatException {

        try {

            XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();

            if (entries.size() > 1) {
                if (style.getBooleanCitProperty(OOBibStyle.MULTI_CITE_CHRONOLOGICAL)) {
                    entries.sort(yearAuthorTitleComparator);
                } else {
                    entries.sort(entryComparator);
                }
            }

            String keyString = String.join(",",
                    entries.stream().map(entry -> entry.getCitationKey().orElse("")).collect(Collectors.toList()));
            // Insert bookmark:
            String bName = getUniqueReferenceMarkName(keyString,
                    withText ? inParenthesis ? OOBibBase.AUTHORYEAR_PAR : OOBibBase.AUTHORYEAR_INTEXT : OOBibBase.INVISIBLE_CIT);

            // If we should store metadata for page info, do that now:
            if (pageInfo != null) {
                LOGGER.info("Storing page info: " + pageInfo);
                setCustomProperty(bName, pageInfo);
            }

            xViewCursor.getText().insertString(xViewCursor, " ", false);
            if (style.isFormatCitations()) {
                XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, xViewCursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch (UnknownPropertyException | PropertyVetoException | IllegalArgumentException |
                        WrappedTargetException ex) {
                    // Setting the character format failed, so we throw an exception that
                    // will result in an error message for the user. Before that,
                    // delete the space we inserted:
                    xViewCursor.goLeft((short) 1, true);
                    xViewCursor.setString("");
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }
            xViewCursor.goLeft((short) 1, false);
            Map<BibEntry, BibDatabase> databaseMap = new HashMap<>();
            for (BibEntry entry : entries) {
                databaseMap.put(entry, database);
            }
            String citeText = style.isNumberEntries() ? "-" : style.getCitationMarker(entries, databaseMap,
                    inParenthesis, null, null);
            insertReferenceMark(bName, citeText, xViewCursor, withText, style);

            xViewCursor.collapseToEnd();
            xViewCursor.goRight((short) 1, false);

            XTextRange position = xViewCursor.getEnd();

            if (sync) {
                // To account for numbering and for uniqiefiers, we must refresh the cite markers:
                updateSortedReferenceMarks();
                refreshCiteMarkers(allBases, style);

                // Insert it at the current position:
                rebuildBibTextSection(allBases, style);
            }

            // Go back to the relevant position:
            xViewCursor.gotoRange(position, false);
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO JAR file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    public List<String> getJabRefReferenceMarks(XNameAccess nameAccess) {
        String[] names = nameAccess.getElementNames();
        // Remove all reference marks that don't look like JabRef citations:
        List<String> result = new ArrayList<>();
        if (names != null) {
            for (String name : names) {
                if (CITE_PATTERN.matcher(name).find()) {
                    result.add(name);
                }
            }
        }
        return result;
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
    public List<String> refreshCiteMarkers(List<BibDatabase> databases, OOBibStyle style)
            throws WrappedTargetException, IllegalArgumentException, NoSuchElementException,
            UndefinedCharacterFormatException, UnknownPropertyException, PropertyVetoException, IOException,
            CreationException, BibEntryNotFoundException {
        try {
            return refreshCiteMarkersInternal(databases, style);
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO JAR file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    private List<String> refreshCiteMarkersInternal(List<BibDatabase> databases, OOBibStyle style)
            throws WrappedTargetException, IllegalArgumentException, NoSuchElementException,
            UndefinedCharacterFormatException, UnknownPropertyException, PropertyVetoException,
            CreationException, BibEntryNotFoundException {

        List<String> cited = findCitedKeys();
        Map<String, BibDatabase> linkSourceBase = new HashMap<>();
        Map<BibEntry, BibDatabase> entries = findCitedEntries(databases, cited, linkSourceBase);

        XNameAccess xReferenceMarks = getReferenceMarks();

        List<String> names;
        if (style.isSortByPosition()) {
            // We need to sort the reference marks according to their order of appearance:
            names = sortedReferenceMarks;
        } else if (style.isNumberEntries()) {
            // We need to sort the reference marks according to the sorting of the bibliographic
            // entries:
            SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
            for (Map.Entry<BibEntry, BibDatabase> bibtexEntryBibtexDatabaseEntry : entries.entrySet()) {
                newMap.put(bibtexEntryBibtexDatabaseEntry.getKey(), bibtexEntryBibtexDatabaseEntry.getValue());
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
        List<String> tmp = new ArrayList<>();
        for (String name : names) {
            if (CITE_PATTERN.matcher(name).find()) {
                tmp.add(name);
            }
        }
        names = tmp;

        Map<String, Integer> numbers = new HashMap<>();
        int lastNum = 0;
        // First compute citation markers for all citations:
        String[] citMarkers = new String[names.size()];
        String[][] normCitMarkers = new String[names.size()][];
        String[][] bibtexKeys = new String[names.size()][];

        int minGroupingCount = style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);

        int[] types = new int[names.size()];
        for (int i = 0; i < names.size(); i++) {
            Matcher citeMatcher = CITE_PATTERN.matcher(names.get(i));
            if (citeMatcher.find()) {
                String typeStr = citeMatcher.group(1);
                int type = Integer.parseInt(typeStr);
                types[i] = type; // Remember the type in case we need to uniquefy.
                String[] keys = citeMatcher.group(2).split(",");
                bibtexKeys[i] = keys;
                BibEntry[] cEntries = new BibEntry[keys.length];
                for (int j = 0; j < cEntries.length; j++) {
                    BibDatabase database = linkSourceBase.get(keys[j]);
                    Optional<BibEntry> tmpEntry = Optional.empty();
                    if (database != null) {
                        tmpEntry = database.getEntryByCitationKey(keys[j]);
                    }
                    if (tmpEntry.isPresent()) {
                        cEntries[j] = tmpEntry.get();
                    } else {
                        LOGGER.info("Citation key not found: '" + keys[j] + '\'');
                        LOGGER.info("Problem with reference mark: '" + names.get(i) + '\'');
                        throw new BibEntryNotFoundException(names.get(i), Localization
                                .lang("Could not resolve BibTeX entry for citation marker '%0'.", names.get(i)));
                    }
                }

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
            }
        }

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
            XTextContent bookmark = UnoRuntime.queryInterface(XTextContent.class, referenceMark);

            XTextCursor cursor = bookmark.getAnchor().getText().createTextCursorByRange(bookmark.getAnchor());

            if (mustTestCharFormat) {
                // If we are supposed to set character format for citations, must run a test before we
                // delete old citation markers. Otherwise, if the specified character format doesn't
                // exist, we end up deleting the markers before the process crashes due to a the missing
                // format, with catastrophic consequences for the user.
                mustTestCharFormat = false; // need to do this only once
                XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, cursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue(CHAR_STYLE_NAME, charStyle);
                } catch (UnknownPropertyException | PropertyVetoException | IllegalArgumentException |
                        WrappedTargetException ex) {
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }

            text.removeTextContent(bookmark);

            insertReferenceMark(names.get(i), citMarkers[i], cursor, types[i] != OOBibBase.INVISIBLE_CIT, style);
            if (hadBibSection && (getBookmarkRange(OOBibBase.BIB_SECTION_NAME) == null)) {
                // We have overwritten the marker for the start of the reference list.
                // We need to add it again.
                cursor.collapseToEnd();
                OOUtil.insertParagraphBreak(text, cursor);
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
            throws WrappedTargetException, NoSuchElementException {
        XTextViewCursorSupplier cursorSupplier = UnoRuntime.queryInterface(XTextViewCursorSupplier.class,
                mxDoc.getCurrentController());

        XTextViewCursor viewCursor = cursorSupplier.getViewCursor();
        XTextRange initialPos = viewCursor.getStart();
        List<String> names = Arrays.asList(nameAccess.getElementNames());
        List<Point> positions = new ArrayList<>(names.size());
        for (String name : names) {
            XTextContent textContent = UnoRuntime.queryInterface(XTextContent.class, nameAccess.getByName(name));
            XTextRange range = textContent.getAnchor();
            // Check if we are inside a footnote:
            if (UnoRuntime.queryInterface(XFootnote.class, range.getText()) != null) {
                // Find the linking footnote marker:
                XFootnote footer = UnoRuntime.queryInterface(XFootnote.class, range.getText());
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

    public XNameAccess getReferenceMarks() {
        XReferenceMarksSupplier supplier = UnoRuntime.queryInterface(XReferenceMarksSupplier.class, xCurrentComponent);
        return supplier.getReferenceMarks();
    }

    private String getUniqueReferenceMarkName(String bibtexKey, int type) {
        XNameAccess xNamedRefMarks = getReferenceMarks();
        int i = 0;
        String name = OOBibBase.BIB_CITATION + '_' + type + '_' + bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = OOBibBase.BIB_CITATION + i + '_' + type + '_' + bibtexKey;
            i++;
        }
        return name;
    }

    public void rebuildBibTextSection(List<BibDatabase> databases, OOBibStyle style)
            throws NoSuchElementException, WrappedTargetException, IllegalArgumentException,
            CreationException, PropertyVetoException, UnknownPropertyException, UndefinedParagraphFormatException {
        List<String> cited = findCitedKeys();
        Map<String, BibDatabase> linkSourceBase = new HashMap<>();
        Map<BibEntry, BibDatabase> entries = findCitedEntries(databases, cited, linkSourceBase); // Although entries are redefined without use, this also updates linkSourceBase

        List<String> names = sortedReferenceMarks;

        if (style.isSortByPosition()) {
            // We need to sort the entries according to their order of appearance:
            entries = getSortedEntriesFromSortedRefMarks(names, linkSourceBase);
        } else {
            SortedMap<BibEntry, BibDatabase> newMap = new TreeMap<>(entryComparator);
            for (Map.Entry<BibEntry, BibDatabase> bibtexEntryBibtexDatabaseEntry : findCitedEntries(databases, cited,
                    linkSourceBase).entrySet()) {
                newMap.put(bibtexEntryBibtexDatabaseEntry.getKey(), bibtexEntryBibtexDatabaseEntry.getValue());
            }
            entries = newMap;
        }
        clearBibTextSectionContent2();
        populateBibTextSection(entries, style);
    }

    private List<String> findCitedKeys() throws NoSuchElementException, WrappedTargetException {
        XNameAccess xNamedMarks = getReferenceMarks();
        String[] names = xNamedMarks.getElementNames();
        List<String> keys = new ArrayList<>();
        for (String name1 : names) {
            Object bookmark = xNamedMarks.getByName(name1);
            UnoRuntime.queryInterface(XTextContent.class, bookmark);

            List<String> newKeys = parseRefMarkName(name1);
            for (String key : newKeys) {
                if (!keys.contains(key)) {
                    keys.add(key);
                }
            }
        }

        return keys;
    }

    private Map<BibEntry, BibDatabase> findCitedEntries(List<BibDatabase> databases, List<String> keys,
                                                        Map<String, BibDatabase> linkSourceBase) {
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

    private Point findPosition(XTextViewCursor cursor, XTextRange range) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
    }

    /**
     * Extract the list of citation keys from a reference mark name.
     *
     * @param name The reference mark name.
     * @return The list of citation keys encoded in the name.
     */
    public List<String> parseRefMarkName(String name) {
        List<String> keys = new ArrayList<>();
        Matcher citeMatcher = CITE_PATTERN.matcher(name);
        if (citeMatcher.find()) {
            String[] keystring = citeMatcher.group(2).split(",");
            for (String aKeystring : keystring) {
                if (!keys.contains(aKeystring)) {
                    keys.add(aKeystring);
                }
            }
        }
        return keys;
    }

    /**
     * Resolve the citation key from a citation reference marker name, and look up the index of the key in a list of keys.
     *
     * @param citRefName The name of the ReferenceMark representing the citation.
     * @param keys       A List of citation keys representing the entries in the bibliography.
     * @return the indices of the cited keys, -1 if a key is not found. Returns null if the ref name could not be resolved as a citation.
     */
    private List<Integer> findCitedEntryIndex(String citRefName, List<String> keys) {
        Matcher citeMatcher = CITE_PATTERN.matcher(citRefName);
        if (citeMatcher.find()) {
            List<String> keyStrings = Arrays.asList(citeMatcher.group(2).split(","));
            List<Integer> result = new ArrayList<>(keyStrings.size());
            for (String key : keyStrings) {
                int ind = keys.indexOf(key);
                result.add(ind == -1 ? -1 : 1 + ind);
            }
            return result;
        } else {
            return Collections.emptyList();
        }
    }

    private Map<BibEntry, BibDatabase> getSortedEntriesFromSortedRefMarks(List<String> names,
                                                                          Map<String, BibDatabase> linkSourceBase) {

        Map<BibEntry, BibDatabase> newList = new LinkedHashMap<>();
        for (String name : names) {
            Matcher citeMatcher = CITE_PATTERN.matcher(name);
            if (citeMatcher.find()) {
                String[] keys = citeMatcher.group(2).split(",");
                for (String key : keys) {
                    BibDatabase database = linkSourceBase.get(key);
                    Optional<BibEntry> origEntry = Optional.empty();
                    if (database != null) {
                        origEntry = database.getEntryByCitationKey(key);
                    }
                    if (origEntry.isPresent()) {
                        if (!newList.containsKey(origEntry.get())) {
                            newList.put(origEntry.get(), database);
                        }
                    } else {
                        LOGGER.info("Citation key not found: '" + key + "'");
                        LOGGER.info("Problem with reference mark: '" + name + "'");
                        newList.put(new UndefinedBibtexEntry(key), null);
                    }
                }
            }
        }

        return newList;
    }

    public String getCitationContext(XNameAccess nameAccess, String refMarkName, int charBefore, int charAfter,
                                     boolean htmlMarkup)
            throws NoSuchElementException, WrappedTargetException {
        Object referenceMark = nameAccess.getByName(refMarkName);
        XTextContent bookmark = UnoRuntime.queryInterface(XTextContent.class, referenceMark);

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
            OOUtil.insertParagraphBreak(text, cursor);
            if (style.isNumberEntries()) {
                int minGroupingCount = style.getIntCitProperty(OOBibStyle.MINIMUM_GROUPING_COUNT);
                OOUtil.insertTextAtCurrentLocation(text, cursor,
                        style.getNumCitationMarker(Collections.singletonList(number++), minGroupingCount, true), Collections.emptyList());
            }
            Layout layout = style.getReferenceFormat(entry.getKey().getType());
            layout.setPostFormatter(POSTFORMATTER);
            OOUtil.insertFullReferenceAtCurrentLocation(text, cursor, layout, parFormat, entry.getKey(),
                    entry.getValue(), uniquefiers.get(entry.getKey().getCitationKey().orElse(null)));
        }
    }

    private void createBibTextSection2(boolean end)
            throws IllegalArgumentException, CreationException {

        XTextCursor mxDocCursor = text.createTextCursor();
        if (end) {
            mxDocCursor.gotoEnd(false);
        }
        OOUtil.insertParagraphBreak(text, mxDocCursor);
        // Create a new TextSection from the document factory and access it's XNamed interface
        XNamed xChildNamed;
        try {
            xChildNamed = UnoRuntime.queryInterface(XNamed.class,
                    mxDocFactory.createInstance("com.sun.star.text.TextSection"));
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // Set the new sections name to 'Child_Section'
        xChildNamed.setName(OOBibBase.BIB_SECTION_NAME);
        // Access the Child_Section's XTextContent interface and insert it into the document
        XTextContent xChildSection = UnoRuntime.queryInterface(XTextContent.class, xChildNamed);
        text.insertTextContent(mxDocCursor, xChildSection, false);
    }

    private void clearBibTextSectionContent2()
            throws NoSuchElementException, WrappedTargetException, IllegalArgumentException, CreationException {

        // Check if the section exists:
        XTextSectionsSupplier supplier = UnoRuntime.queryInterface(XTextSectionsSupplier.class, mxDoc);
        if (supplier.getTextSections().hasByName(OOBibBase.BIB_SECTION_NAME)) {
            XTextSection section = (XTextSection) ((Any) supplier.getTextSections().getByName(OOBibBase.BIB_SECTION_NAME))
                    .getObject();
            // Clear it:
            XTextCursor cursor = text.createTextCursorByRange(section.getAnchor());
            cursor.gotoRange(section.getAnchor(), false);
            cursor.setString("");
        } else {
            createBibTextSection2(atEnd);
        }
    }

    private void populateBibTextSection(Map<BibEntry, BibDatabase> entries, OOBibStyle style)
            throws NoSuchElementException, WrappedTargetException, PropertyVetoException,
            UnknownPropertyException, UndefinedParagraphFormatException, IllegalArgumentException, CreationException {
        XTextSectionsSupplier supplier = UnoRuntime.queryInterface(XTextSectionsSupplier.class, mxDoc);
        XTextSection section = (XTextSection) ((Any) supplier.getTextSections().getByName(OOBibBase.BIB_SECTION_NAME))
                .getObject();
        XTextCursor cursor = text.createTextCursorByRange(section.getAnchor());
        OOUtil.insertTextAtCurrentLocation(text, cursor, (String) style.getProperty(OOBibStyle.TITLE),
                (String) style.getProperty(OOBibStyle.REFERENCE_HEADER_PARAGRAPH_FORMAT));
        insertFullReferenceAtCursor(cursor, entries, style,
                (String) style.getProperty(OOBibStyle.REFERENCE_PARAGRAPH_FORMAT));
        insertBookMark(OOBibBase.BIB_SECTION_END_NAME, cursor);
    }

    private XTextContent insertBookMark(String name, XTextCursor position)
            throws IllegalArgumentException, CreationException {
        Object bookmark;
        try {
            bookmark = mxDocFactory.createInstance("com.sun.star.text.Bookmark");
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // name the bookmark
        XNamed xNamed = UnoRuntime.queryInterface(XNamed.class, bookmark);
        xNamed.setName(name);
        // get XTextContent interface
        XTextContent xTextContent = UnoRuntime.queryInterface(XTextContent.class, bookmark);
        // insert bookmark at the end of the document
        // instead of mxDocText.getEnd you could use a text cursor's XTextRange interface or any XTextRange
        text.insertTextContent(position, xTextContent, true);
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
            bookmark = mxDocFactory.createInstance("com.sun.star.text.ReferenceMark");
        } catch (Exception e) {
            throw new CreationException(e.getMessage());
        }
        // Name the reference
        XNamed xNamed = UnoRuntime.queryInterface(XNamed.class, bookmark);
        xNamed.setName(name);

        if (withText) {
            position.setString(citText);
            XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, position);

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
        XTextContent xTextContent = UnoRuntime.queryInterface(XTextContent.class, bookmark);

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
            XTextContent bookmark = UnoRuntime.queryInterface(XTextContent.class, referenceMark);
            text.removeTextContent(bookmark);
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
        XTextContent xFoundBookmark = UnoRuntime.queryInterface(XTextContent.class, foundBookmark);
        return xFoundBookmark.getAnchor();
    }

    private XNameAccess getBookmarks() {
        // query XBookmarksSupplier from document model and get bookmarks collection
        XBookmarksSupplier xBookmarksSupplier = UnoRuntime.queryInterface(XBookmarksSupplier.class, xCurrentComponent);
        XNameAccess xNamedBookmarks = xBookmarksSupplier.getBookmarks();
        return xNamedBookmarks;
    }

    private void italicizeOrBold(XTextCursor position, boolean italicize, int start, int end)
            throws UnknownPropertyException, PropertyVetoException, IllegalArgumentException, WrappedTargetException {
        XTextRange range = position.getStart();
        XTextCursor cursor = position.getText().createTextCursorByRange(range);
        cursor.goRight((short) start, false);
        cursor.goRight((short) (end - start), true);
        XPropertySet xcp = UnoRuntime.queryInterface(XPropertySet.class, cursor);
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

        final XTextRangeCompare compare = UnoRuntime.queryInterface(XTextRangeCompare.class, text);

        int piv = 0;
        boolean madeModifications = false;
        while (piv < (names.size() - 1)) {
            XTextRange range1 = UnoRuntime.queryInterface(XTextContent.class, nameAccess.getByName(names.get(piv)))
                                          .getAnchor().getEnd();
            XTextRange range2 = UnoRuntime.queryInterface(XTextContent.class, nameAccess.getByName(names.get(piv + 1)))
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
                    XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, mxDocCursor);
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

                List<String> keys = parseRefMarkName(names.get(piv));
                keys.addAll(parseRefMarkName(names.get(piv + 1)));
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
        List<String> names = getSortedReferenceMarks(nameAccess);

        final XTextRangeCompare compare = UnoRuntime.queryInterface(XTextRangeCompare.class, text);

        int pivot = 0;
        boolean madeModifications = false;
        while (pivot < (names.size())) {
            XTextRange range1 = UnoRuntime.queryInterface(XTextContent.class, nameAccess.getByName(names.get(pivot)))
                .getAnchor();

            XTextCursor textCursor = range1.getText().createTextCursorByRange(range1);

            // If we are supposed to set character format for citations, test this before
            // making any changes. This way we can throw an exception before any reference
            // marks are removed, preventing damage to the user's document:
            if (style.isFormatCitations()) {
                XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, textCursor);
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

            List<String> keys = parseRefMarkName(names.get(pivot));
            if (keys.size() > 1) {
                removeReferenceMark(names.get(pivot));

                // Insert bookmark for each key
                int last = keys.size() - 1;
                int i = 0;
                for (String key : keys) {
                    String newName = getUniqueReferenceMarkName(key, OOBibBase.AUTHORYEAR_PAR);
                    insertReferenceMark(newName, "tmp", textCursor, true, style);
                    textCursor.collapseToEnd();
                    if (i != last) {
                        textCursor.setString(" ");
                        textCursor.collapseToEnd();
                    }
                    i++;
                }
                madeModifications = true;
            }
            pivot++;
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

    private class DocumentTitleViewModel {

        private final XTextDocument xTextDocument;
        private final String description;

        public DocumentTitleViewModel(XTextDocument xTextDocument) {
            this.xTextDocument = xTextDocument;
            this.description = getDocumentTitle(xTextDocument).orElse("");
        }

        public XTextDocument getXtextDocument() {
            return xTextDocument;
        }

        @Override
        public String toString() {
            return description;
        }
    }
}
