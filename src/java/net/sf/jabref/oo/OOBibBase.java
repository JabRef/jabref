/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.oo;

import com.sun.star.awt.Point;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.PropertyValue;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.container.*;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.*;
import com.sun.star.lang.*;
import com.sun.star.lang.Locale;
import com.sun.star.text.*;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import net.sf.jabref.*;
import net.sf.jabref.export.layout.Layout;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for manipulating the Bibliography of the currently start document in OpenOffice.
 */
public class OOBibBase {

    final static String BIB_SECTION_NAME = "JR_bib";
    final static String BIB_SECTION_END_NAME = "JR_bib_end";
    final static String BIB_CITATION = "JR_cite";
    public final Pattern citePattern = Pattern.compile(BIB_CITATION+"\\d*_(\\d*)_(.*)");

    final static int
        AUTHORYEAR_PAR = 1,
        AUTHORYEAR_INTEXT = 2,
        INVISIBLE_CIT = 3;

    final static String DEFAULT_CONNECTION_STRING = "uno:socket,host=localhost,port=2002;urp;StarOffice.ServiceManager";
    final String[] BIB_TYPES = new String[] { "ARTICLE", "BOOK", "BOOKLET", "CONFERENCE",
            "INBOOK", "INCOLLECTION", "INPROCEEDINGS", "JOURNAL", "MANUAL", "MASTERTHESIS",
            "MISC", "PHDTHESIS", "PROCEEDINGS", "TECHREPORT", "UNPUBLISHED", "EMAIL", "WWW",
            "CUSTOM1", "CUSTOM2", "CUSTOM3", "CUSTOM4", "CUSTOM5" };

    
    private XMultiServiceFactory mxDocFactory = null;
    private XTextDocument mxDoc = null;
    private XText text = null;
    private XDesktop xDesktop = null;
    XTextViewCursorSupplier xViewCursorSupplier = null;
    XComponent xCurrentComponent = null;
    XComponentLoader xComponentLoader = null;
    XPropertyContainer userProperties = null;
    XPropertySet propertySet = null;

    private boolean atEnd;
    private AlphanumericComparator entryComparator = new AlphanumericComparator();
    private YearComparator yearComparator = new YearComparator();

    private HashMap<String,String> uniquefiers = new HashMap<String, String>();

    private String[] sortedReferenceMarks = null;

    public OOBibBase(String pathToOO, boolean atEnd) throws Exception {
        this.atEnd = atEnd;
        xDesktop = simpleBootstrap(pathToOO);//getDesktop();
        selectDocument();
    }

    public boolean isConnectedToDocument() {
        return xCurrentComponent != null;
    }

    public String getCurrentDocumentTitle() {
        if (mxDoc != null) {
            try {
                return String.valueOf(OOUtil.getProperty
                        (mxDoc.getCurrentController().getFrame(), "Title"));
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        else
            return null;
    }

    public void selectDocument() throws Exception {
        List<XTextDocument> ls = getTextDocuments();
        XTextDocument selected = null;
        if (ls.size() == 0) {
            // No text documents found.
            throw new Exception("No Writer documents found");
        }
        else if (ls.size() > 1) {
            selected = OOUtil.selectComponent(null, xDesktop, ls);
        }
        else
            selected = ls.get(0);

        if (selected == null) {
            return;
        }
        xCurrentComponent = (XComponent) UnoRuntime.queryInterface(
                XComponent.class, selected);
        mxDoc = selected;

        com.sun.star.text.XDocumentIndexesSupplier indexesSupp = (com.sun.star.text.XDocumentIndexesSupplier) UnoRuntime.queryInterface(
                 com.sun.star.text.XDocumentIndexesSupplier.class, xCurrentComponent);

        XModel xModel = (XModel) UnoRuntime.queryInterface(XModel.class, xCurrentComponent);
        XController xController = xModel.getCurrentController();
        xViewCursorSupplier =
            (com.sun.star.text.XTextViewCursorSupplier) UnoRuntime.queryInterface(
                    com.sun.star.text.XTextViewCursorSupplier.class, xController);

        // get a reference to the body text of the document
        text = mxDoc.getText();

        // Access the text document's multi service factory:
        mxDocFactory = (XMultiServiceFactory) UnoRuntime.queryInterface(XMultiServiceFactory.class, mxDoc);


        XDocumentPropertiesSupplier supp =
                UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, mxDoc);
        userProperties = supp.getDocumentProperties().getUserDefinedProperties();
        propertySet = UnoRuntime.queryInterface(XPropertySet.class, userProperties);

    }

    public XDesktop simpleBootstrap(String pathToExecutable) throws Exception {

        ClassLoader loader = ClassLoader.getSystemClassLoader();
        if (loader instanceof URLClassLoader) {
            URLClassLoader cl = (URLClassLoader) loader;
            Class sysclass = URLClassLoader.class;
            try {

                 Method method = sysclass.getDeclaredMethod("addURL", new Class[]{URL.class});
                 method.setAccessible(true);
                 method.invoke(cl, new Object[]{new File(pathToExecutable).toURI().toURL()});
             } catch (Throwable t) {
                 t.printStackTrace();
                 throw new IOException("Error, could not add URL to system classloader");
             }
         } else {
             System.out.println("Error occured, URLClassLoader expected but " +
                     loader.getClass() + " received. Could not continue.");
         }

         //Get the office component context:
         XComponentContext xContext = Bootstrap.bootstrap();
         //Get the office service manager:
         XMultiComponentFactory xServiceManager = xContext.getServiceManager();
         //Create the desktop, which is the root frame of the
         //hierarchy of frames that contain viewable components:
         Object desktop = xServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
        XDesktop xD = (XDesktop) UnoRuntime.queryInterface(XDesktop.class, desktop);

        xComponentLoader = (XComponentLoader)UnoRuntime.queryInterface(
             XComponentLoader.class, desktop);

        return xD;

     }

     public List<XTextDocument> getTextDocuments() throws Exception {
         List<XTextDocument> res = new ArrayList<XTextDocument>();
         XEnumerationAccess enumA = xDesktop.getComponents();
         XEnumeration e = enumA.createEnumeration();

         // TODO: http://api.openoffice.org/docs/DevelopersGuide/OfficeDev/OfficeDev.xhtml#1_1_3_2_1_2_Frame_Hierarchies

         while (e.hasMoreElements()) {
             Object o = e.nextElement();
             XComponent comp = (XComponent) UnoRuntime.queryInterface(XComponent.class, o);
             XTextDocument doc = (XTextDocument) UnoRuntime.queryInterface(
                    XTextDocument.class, comp);
             if (doc != null) {
                res.add(doc);
             }
         }
         return res;
     }

    public void testCustomProperties() throws Exception {
        XDocumentPropertiesSupplier supp = (XDocumentPropertiesSupplier)UnoRuntime.queryInterface(
                XDocumentPropertiesSupplier.class, mxDoc);
        XPropertyContainer cont = supp.getDocumentProperties().getUserDefinedProperties();
        XPropertySet set = (XPropertySet)UnoRuntime.queryInterface(XPropertySet.class, cont);
        try {
            cont.addProperty("JR_cite_1_Danielsen1987", (short)0, new Any(Type.STRING, "Brum"));
        } catch (Exception ex) {
            System.out.println("property already exists");
        }
        System.out.println(set.getPropertyValue("Test").toString());

    }

    public void setCustomProperty(String property, String value) throws Exception {
        if (propertySet.getPropertySetInfo().hasPropertyByName(property))
            userProperties.removeProperty(property);
        if (value != null)
            userProperties.addProperty(property, com.sun.star.beans.PropertyAttribute.REMOVEABLE,
                   new Any(Type.STRING, value));
    }
    
    public String getCustomProperty(String property) throws Exception {
        if (propertySet.getPropertySetInfo().hasPropertyByName(property))
            return propertySet.getPropertyValue(property).toString();
        else return null;

    }

     public void updateSortedReferenceMarks() throws Exception {
         XReferenceMarksSupplier supplier = (XReferenceMarksSupplier) UnoRuntime.queryInterface(
                XReferenceMarksSupplier.class, xCurrentComponent);
        XNameAccess nameAccess = supplier.getReferenceMarks();
        String[] names;
        sortedReferenceMarks = getSortedReferenceMarks(nameAccess);
     }
     
    /**
     * This method inserts a cite marker in the text for the given BibtexEntry,
     * and may refresh the bibliography.
     * @param entries The entries to cite.
     * @param database The database the entry belongs to.
     * @param style The bibliography style we are using.
     * @param inParenthesis Indicates whether it is an in-text citation or a citation in parenthesis.
     *   This is not relevant if numbered citations are used.
     * @param withText Indicates whether this should be a normal citation (true) or an empty
     *   (invisible) citation (false).
     * @param sync Indicates whether the reference list should be refreshed.
     * @throws Exception
     */
    public void insertEntry(BibtexEntry[] entries, BibtexDatabase database,
                            List<BibtexDatabase> allBases, OOBibStyle style,
                            boolean inParenthesis, boolean withText, String pageInfo,
                            boolean sync) throws Exception {

        try {

            XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();

            if (entries.length > 1) {
                if (style.getBooleanCitProperty("MultiCiteChronological"))
                    Arrays.sort(entries, yearComparator);
                else
                    Arrays.sort(entries, entryComparator);
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < entries.length; i++) {
                BibtexEntry entry = entries[i];
                if (i > 0)
                    sb.append(",");
                sb.append(entry.getCiteKey());
            }
            String keyString = sb.toString();
            // Insert bookmark:
            String bName = getUniqueReferenceMarkName(keyString,
                    withText ? (inParenthesis ? AUTHORYEAR_PAR : AUTHORYEAR_INTEXT) : INVISIBLE_CIT);
            //XTextContent content = insertBookMark(bName, xViewCursor);

            // If we should store metadata for page info, do that now:
            if (pageInfo != null) {
                System.out.println("Storing page info: "+pageInfo);
                setCustomProperty(bName, pageInfo);
            }


            String citeText = style.isNumberEntries() ? "-" : style.getCitationMarker(entries, database, inParenthesis, null, null);

            //System.out.println(text+" / "+xViewCursor.getText());
            xViewCursor.getText().insertString(xViewCursor, " ", false);
            if (style.isFormatCitations()) {
                XPropertySet xCursorProps = (XPropertySet) UnoRuntime.queryInterface(
                                    XPropertySet.class, xViewCursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue("CharStyleName", charStyle);
                } catch (Throwable ex) {
                    // Setting the character format failed, so we throw an exception that
                    // will result in an error message for the user. Before that,
                    // delete the space we inserted:
                    xViewCursor.goLeft((short)1,true);
                    xViewCursor.setString("");
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }
            xViewCursor.goLeft((short)1,false);
            insertReferenceMark(bName, citeText, xViewCursor, withText, style);
            //xViewCursor.collapseToEnd();

            xViewCursor.collapseToEnd();
            xViewCursor.goRight((short)1,false);

            XTextRange position = xViewCursor.getEnd();

            if (sync) {
                // To account for numbering and for uniqiefiers, we must refresh the cite markers:
                updateSortedReferenceMarks();
                refreshCiteMarkers(allBases, style);

                // Insert it at the current position:
                rebuildBibTextSection(allBases, style);
            }

            // Go back to the relevant position:
            try {
                xViewCursor.gotoRange(position, false);
            } catch (Exception ex) {
                System.out.println("Catch");
                ex.printStackTrace();
            }
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO jar file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    /**
     * Refresh all cite markers in the document.
     * @param databases The databases to get entries from.
     * @param style The bibliography style to use.
     * @return A list of those referenced BibTeX keys that could not be resolved.
     * @throws Exception
     */
    public List<String> refreshCiteMarkers(List<BibtexDatabase> databases, OOBibStyle style) throws
            Exception {
        try {
            return refreshCiteMarkersInternal(databases, style);
        } catch (DisposedException ex) {
            // We need to catch this one here because the OpenOfficePanel class is
            // loaded before connection, and therefore cannot directly reference
            // or catch a DisposedException (which is in a OO jar file).
            throw new ConnectionLostException(ex.getMessage());
        }
    }

    public XNameAccess getReferenceMarks() {
        XReferenceMarksSupplier supplier = (XReferenceMarksSupplier) UnoRuntime.queryInterface(
                XReferenceMarksSupplier.class, xCurrentComponent);
        return supplier.getReferenceMarks();
    }

    public String[] getJabRefReferenceMarks(XNameAccess nameAccess) {
        String[] names = nameAccess.getElementNames();
        // Remove all reference marks that don't look like JabRef citations:
        ArrayList<String> tmp = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            if (citePattern.matcher(names[i]).find())
                tmp.add(names[i]);
        }
        names = tmp.toArray(new String[tmp.size()]);
        return names;
    }

    private List<String> refreshCiteMarkersInternal(List<BibtexDatabase> databases, OOBibStyle style) throws
            Exception {

        List<String> cited = findCitedKeys();
        HashMap<String,BibtexDatabase> linkSourceBase = new HashMap<String, BibtexDatabase>();
        Map<BibtexEntry,BibtexDatabase> entries = findCitedEntries(databases, cited, linkSourceBase);

        XNameAccess nameAccess = getReferenceMarks();

        String[] names;
        if (style.isSortByPosition()) {
            // We need to sort the reference marks according to their order of appearance:
            /*if (sortedReferenceMarks == null)
                updateSortedReferenceMarks();*/
            names = sortedReferenceMarks;
        }
        else if (style.isNumberEntries()) {
            // We need to sort the reference marks according to the sorting of the bibliographic
            // entries:
            SortedMap<BibtexEntry,BibtexDatabase> newMap =
                    new TreeMap<BibtexEntry, BibtexDatabase>(entryComparator);
            for (BibtexEntry entry : entries.keySet())
                newMap.put(entry, entries.get(entry));
            entries = newMap;
            // Rebuild the list of cited keys according to the sort order:
            cited.clear();
            for (Iterator<BibtexEntry> iterator = entries.keySet().iterator(); iterator.hasNext();) {
                BibtexEntry entry = iterator.next();
                cited.add(entry.getCiteKey());
            }
            names = nameAccess.getElementNames();
        }
        else {
            /*if (sortedReferenceMarks == null)
                updateSortedReferenceMarks();*/
            names = sortedReferenceMarks;            
        }

        // Remove all reference marks that don't look like JabRef citations:
        ArrayList<String> tmp = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            if (citePattern.matcher(names[i]).find())
                tmp.add(names[i]);
        }
        names = tmp.toArray(new String[tmp.size()]);

        HashMap<String,Integer> numbers = new HashMap<String, Integer>();
        //HashMap<S
        int lastNum = 0;
        // First compute citation markers for all citations:
        String[] citMarkers = new String[names.length];
        String[][] normCitMarkers = new String[names.length][];
        String[][] bibtexKeys = new String[names.length][];

        int minGroupingCount = style.getIntCitProperty("MinimumGroupingCount");

        int[] types = new int[names.length];
        for (int i = 0; i < names.length; i++) {
            Matcher m = citePattern.matcher(names[i]);
            if (m.find()) {
                String typeStr = m.group(1);
                int type = Integer.parseInt(typeStr);
                types[i] = type; // Remember the type in case we need to uniqiefy.
                String[] keys = m.group(2).split(",");
                bibtexKeys[i] = keys;
                BibtexEntry[] cEntries = new BibtexEntry[keys.length];
                for (int j = 0; j < cEntries.length; j++) {
                    BibtexDatabase database = linkSourceBase.get(keys[j]);
                    cEntries[j] = null;
                    if (database != null)
                        cEntries[j] = OOUtil.createAdaptedEntry(database.getEntryByKey(keys[j]));
                    if (cEntries[j] == null) {
                        System.out.println("Bibtex key not found : '"+keys[j]+"'");
                        System.out.println("Problem with reference mark: '"+names[i]+"'");
                        cEntries[j] = new UndefinedBibtexEntry(keys[j]);
                        //throw new BibtexEntryNotFoundException(keys[j], "");
                    }
                }

                String[] normCitMarker = new String[keys.length];
                String citationMarker;
                if (style.isBibtexKeyCiteMarkers()) {
                    StringBuilder sb = new StringBuilder();
                    normCitMarkers[i] = new String[keys.length];
                    for (int j=0; j<keys.length; j++) {
                        normCitMarkers[i][j] = cEntries[j].getCiteKey();
                        sb.append(cEntries[j].getCiteKey());
                        if (j < keys.length-1)
                            sb.append(",");
                    }
                    citationMarker = sb.toString();
                }
                else if (style.isNumberEntries()) {
                    if (style.isSortByPosition()) {
                        // We have sorted the citation markers according to their order of appearance,
                        // so we simply count up for each marker referring to a new entry:
                        int[] num = new int[keys.length];
                        for (int j=0; j<keys.length; j++) {
                            if (cEntries[j] instanceof UndefinedBibtexEntry) {
                                num[j] = -1;
                            } else {
                                num[j] = lastNum + 1;
                                if (numbers.containsKey(keys[j]))
                                    num[j] = numbers.get(keys[j]);
                                else {
                                    numbers.put(keys[j], num[j]);
                                    lastNum = num[j];
                                }
                            }
                        }
                        citationMarker = style.getNumCitationMarker(num, minGroupingCount, false);
                        for (int j=0; j<keys.length; j++)
                            normCitMarker[j] = style.getNumCitationMarker(new int[] {num[j]},
                                    minGroupingCount, false);
                    }
                    else {
                        // We need to find the number of the cited entry in the bibliography,
                        // and use that number for the cite marker:
                        int[] num = findCitedEntryIndex(names[i], cited);

                        if (num != null)
                            citationMarker = style.getNumCitationMarker(num, minGroupingCount, false);
                        else
                            throw new BibtexEntryNotFoundException(names[i], Globals.lang("Could not resolve BibTeX entry for citation marker '%0'.", names[i]));

                        for (int j=0; j<keys.length; j++)
                            normCitMarker[j] = style.getNumCitationMarker(new int[] {num[j]},
                                    minGroupingCount, false);
                    }
                }
                else {

                    if (cEntries.length > 1) {
                        if (style.getBooleanCitProperty("MultiCiteChronological"))
                            Arrays.sort(cEntries, yearComparator);
                        else
                            Arrays.sort(cEntries, entryComparator);
                        // Update key list to match the new sorting:
                        for (int j = 0; j < cEntries.length; j++) {
                            bibtexKeys[i][j] = cEntries[j].getCiteKey();
                        }
                    }
                    /*System.out.println(style.getBooleanCitProperty("MultiCiteChronological"));
                    for (int j = 0; j < cEntries.length; j++) {
                        BibtexEntry cEntry = cEntries[j];
                        System.out.println(cEntry.getCiteKey());
                    } */

                    citationMarker = style.getCitationMarker(cEntries, entries.get(cEntries), type == AUTHORYEAR_PAR, null, null);
                    // We need "normalized" (in parenthesis) markers for uniqueness checking purposes:
                    for (int j=0; j<cEntries.length; j++)
                        normCitMarker[j] = style.getCitationMarker(cEntries[j], entries.get(cEntries), true, null, -1);
                }
                citMarkers[i] = citationMarker;
                normCitMarkers[i] = normCitMarker;

            }


        }

        uniquefiers.clear();
        if (!style.isBibtexKeyCiteMarkers() && !style.isNumberEntries()) {
            // See if there are duplicate citations marks referring to different entries. If so, we need to
            // use uniquefiers:
            HashMap<String,List<String>> refKeys = new HashMap<String, List<String>>();
            HashMap<String,List<Integer>> refNums = new HashMap<String, List<Integer>>();
            for (int i = 0; i < citMarkers.length; i++) {
                String[] markers = normCitMarkers[i]; // compare normalized markers, since the actual markers can be different
                for (int j=0; j<markers.length; j++) {
                    String marker = markers[j];
                    if (!refKeys.containsKey(marker)) {
                        List<String> l = new ArrayList<String>(1);
                        l.add(bibtexKeys[i][j]);
                        refKeys.put(marker, l);
                        List<Integer> l2 = new ArrayList<Integer>(1);
                        l2.add(i);
                        refNums.put(marker, l2);
                    }
                    else {
                        // Ok, we have seen this exact marker before.
                        if (!refKeys.get(marker).contains(bibtexKeys[i][j])) {
                            // ... but not for this entry.
                            refKeys.get(marker).add(bibtexKeys[i][j]);
                            refNums.get(marker).add(i);
                        }
                    }
                }
            }
            // Go through the collected lists and see where we need to uniquefy:
            for (String marker : refKeys.keySet()) {
                List<String> keys = refKeys.get(marker);
                if (keys.size() > 1) {
                    // This marker appears for more than one unique entry:
                    int uniq = 'a';
                    for (String key : keys) {
                        // Update the map of uniquefiers for the benefit of both the following generation of new
                        // citation markers, and for the method that builds the bibliography:
                        uniquefiers.put(key, String.valueOf((char)uniq));
                        uniq++;
                    }
                }
            }

            // Finally, go through all citation markers, and update those referring to entries in our current list:
            int maxAuthorsFirst = style.getIntCitProperty("MaxAuthorsFirst");
            HashSet<String> seenBefore = new HashSet<String>();
            for (int j = 0; j < bibtexKeys.length; j++) {
                boolean needsChange = false;
                int[] firstLimAuthors = new int[bibtexKeys[j].length];
                String[] uniquif = new String[bibtexKeys[j].length];
                BibtexEntry[] cEntries = new BibtexEntry[bibtexKeys[j].length];
                for (int k=0; k<bibtexKeys[j].length; k++) {
                    firstLimAuthors[k] = -1;
                    if (maxAuthorsFirst > 0) {
                        if (!seenBefore.contains(bibtexKeys[j][k])) {
                            firstLimAuthors[k] = maxAuthorsFirst;
                        }
                        seenBefore.add(bibtexKeys[j][k]);
                    }
                    String uniq = uniquefiers.get(bibtexKeys[j][k]);
                    if ((uniq != null) && (uniq.length() >= 0)) {
                        needsChange = true;
                        BibtexDatabase database = linkSourceBase.get(bibtexKeys[j][k]);
                        if (database != null)
                            cEntries[k] = OOUtil.createAdaptedEntry(database.getEntryByKey(bibtexKeys[j][k]));
                        uniquif[k] = uniq;
                    }
                    else if (firstLimAuthors[k] > 0) {
                        needsChange = true;
                        BibtexDatabase database = linkSourceBase.get(bibtexKeys[j][k]);
                        if (database != null)
                            cEntries[k] = OOUtil.createAdaptedEntry(database.getEntryByKey(bibtexKeys[j][k]));
                        uniquif[k] = "";
                    }
                    else {
                        BibtexDatabase database = linkSourceBase.get(bibtexKeys[j][k]);
                        if (database != null)
                            cEntries[k] = OOUtil.createAdaptedEntry(database.getEntryByKey(bibtexKeys[j][k]));
                        uniquif[k] = "";
                    }
                }
                if (needsChange) {
                    citMarkers[j] = style.getCitationMarker(cEntries, entries.get(cEntries), types[j] == AUTHORYEAR_PAR,
                        uniquif, firstLimAuthors);
                }
            }
        }

        // Refresh all reference marks with the citation markers we computed:
        boolean hadBibSection = getBookmarkRange(BIB_SECTION_NAME) != null;
        // Check if we are supposed to set a character format for citations:
        boolean mustTestCharFormat = style.isFormatCitations();
        for (int i = 0; i < names.length; i++) {
            Object o = nameAccess.getByName(names[i]);
            XTextContent bm = (XTextContent) UnoRuntime.queryInterface
                    (XTextContent.class, o);

            XTextCursor cursor = bm.getAnchor().getText().createTextCursorByRange(bm.getAnchor());

            if (mustTestCharFormat) {
                // If we are supposed to set character format for citations, must run a test before we
                // delete old citation markers. Otherwise, if the specified character format doesn't
                // exist, we end up deleting the markers before the process crashes due to a the missing
                // format, with catastrophic consequences for the user.
                mustTestCharFormat = false; // need to do this only once
                XPropertySet xCursorProps = (XPropertySet) UnoRuntime.queryInterface(
                                    XPropertySet.class, cursor);
                String charStyle = style.getCitationCharacterFormat();
                try {
                    xCursorProps.setPropertyValue("CharStyleName", charStyle);
                } catch (Throwable ex) {
                    throw new UndefinedCharacterFormatException(charStyle);
                }
            }


            text.removeTextContent(bm);

            insertReferenceMark(names[i], citMarkers[i], cursor, types[i] != INVISIBLE_CIT, style);
            if (hadBibSection && (getBookmarkRange(BIB_SECTION_NAME) == null)) {
                // We have overwritten the marker for the start of the reference list.
                // We need to add it again.
                cursor.collapseToEnd();
                OOUtil.insertParagraphBreak(text, cursor);
                insertBookMark(BIB_SECTION_NAME, cursor);
                /* The following is for resetting the paragraph format, but should probably
                   not be done.
                   
                XParagraphCursor parCursor =
                    (XParagraphCursor)UnoRuntime.queryInterface(
                    java.lang.Class.forName("com.sun.star.text.XParagraphCursor"), cursor);
                parCursor.gotoPreviousParagraph(false);
                parCursor.gotoStartOfParagraph(false);
                parCursor.gotoEndOfParagraph(true);
                XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
                    XPropertySet.class, parCursor);

                try {
                    props.setPropertyValue("ParaStyleName", "Default");
                } catch (com.sun.star.lang.IllegalArgumentException ex) {
                    throw new UndefinedParagraphFormatException("Default");
                }
                */

            }
        }

        ArrayList<String> unresolvedKeys = new ArrayList<String>();
        for (BibtexEntry entry : entries.keySet()) {
            if (entry instanceof UndefinedBibtexEntry) {
                String key = ((UndefinedBibtexEntry)entry).getKey();
                if (!unresolvedKeys.contains(key))
                    unresolvedKeys.add(key);
            }
        }
        return unresolvedKeys;
    }

    public String[] getSortedReferenceMarks(final XNameAccess nameAccess) throws Exception {
        /*
        PropertyValue[] props = new PropertyValue[2];

        props[0] = new PropertyValue();
        props[0].Name = "Model";

        props[0].Value = mxDoc.getCurrentController().getModel();
        props[1] = new PropertyValue();
        props[1].Name = "Hidden";
        props[1].Value = true;

        // argument xModel wins over URL.
        System.out.println("her");
        XComponent comp = xComponentLoader.loadComponentFromURL("private:factory/swriter",
                           "_blank", 0, props);
        System.out.println("her2");

        XTextDocument newDoc = (XTextDocument)UnoRuntime.queryInterface(
                XTextDocument.class, comp);
        System.out.println("newDoc = "+newDoc);

        // Controller of the hidden frame
        XController xController = newDoc.getCurrentController();

        XFrame xFrame = xController.getFrame();
        XWindow xContainerWindow = xFrame.getContainerWindow();
        XWindow xComponentWindow = xFrame.getComponentWindow();

        xContainerWindow.setVisible(true);
        xComponentWindow.setFocus();
        xContainerWindow.setVisible(false);
        */
        XTextViewCursorSupplier css = (XTextViewCursorSupplier)UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, mxDoc.getCurrentController());

        XTextViewCursor tvc = css.getViewCursor();
        XTextRange initialPos = tvc.getStart();
        String[] names = nameAccess.getElementNames();
        Point[] positions = new Point[names.length];
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            XTextContent tc = (XTextContent) UnoRuntime.queryInterface
                    (XTextContent.class, nameAccess.getByName(name));
            XTextRange r = tc.getAnchor();
            // Check if we are inside a footnote:
            if (UnoRuntime.queryInterface(XFootnote.class, r.getText()) != null) {
                // Find the linking footnote marker:
                XFootnote footer = (XFootnote)UnoRuntime.queryInterface(XFootnote.class, r.getText());
                // The footnote's anchor gives the correct position in the text:
                r = footer.getAnchor();
            }

            positions[i] = findPosition(tvc, r);
        }
        TreeSet<ComparableMark> set = new TreeSet<ComparableMark>();
        for (int i = 0; i < positions.length; i++) {
            set.add(new ComparableMark(names[i], positions[i]));
        }
        int i=0;
        for (Iterator<ComparableMark> iterator = set.iterator(); iterator.hasNext();) {
            ComparableMark mark = iterator.next();
            //System.out.println(mark.getPosition().X+" -- "+mark.getPosition().Y+" : "+mark.getName());
            names[i++] = mark.getName();
        }
        tvc.gotoRange(initialPos, false);
        //xFrame.dispose();

        return names;
        
        /*final XTextRangeCompare compare = (XTextRangeCompare) UnoRuntime.queryInterface
                (XTextRangeCompare.class, text);
        Arrays.sort(names, new Comparator<String>() {
            public int compare(String o1, String o2) {
                try {
                    XTextRange r1 = ((XTextContent) UnoRuntime.queryInterface
                            (XTextContent.class, nameAccess.getByName(o1))).getAnchor();
                    XTextRange r2 = ((XTextContent) UnoRuntime.queryInterface
                            (XTextContent.class, nameAccess.getByName(o2))).getAnchor();

                    try {
                        return compare.compareRegionStarts(r2, r1);
                    } catch (com.sun.star.lang.IllegalArgumentException ex) {
                        // problem comparing reference marks in different areas (text, table, etc.)
                        return 0;
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    return 0;
                }
            }
        });
        return names;*/
    }

    public void rebuildBibTextSection(List<BibtexDatabase> databases, OOBibStyle style)
            throws Exception {
        List<String> cited = findCitedKeys();
        HashMap<String,BibtexDatabase> linkSourceBase = new HashMap<String, BibtexDatabase>();
        Map<BibtexEntry,BibtexDatabase> entries = findCitedEntries
                (databases, cited, linkSourceBase);
        
        String[] names = sortedReferenceMarks;
        
        if (style.isSortByPosition()) {
            // We need to sort the entries according to their order of appearance:
           entries = getSortedEntriesFromSortedRefMarks(names, entries, linkSourceBase);
        }
        else {
            SortedMap<BibtexEntry,BibtexDatabase> newMap =
                    new TreeMap<BibtexEntry, BibtexDatabase>(entryComparator);
            for (BibtexEntry entry : entries.keySet())
                newMap.put(entry, entries.get(entry));
            entries = newMap;
        }
        clearBibTextSectionContent2();
        populateBibTextSection(entries, style);
    }



    public String getUniqueReferenceMarkName(String bibtexKey, int type) {
        XReferenceMarksSupplier supplier = (XReferenceMarksSupplier) UnoRuntime.queryInterface(
                XReferenceMarksSupplier.class, xCurrentComponent);
        XNameAccess xNamedRefMarks = supplier.getReferenceMarks();
        int i=0;
        String name = BIB_CITATION+"_"+type+"_"+bibtexKey;
        while (xNamedRefMarks.hasByName(name)) {
            name = BIB_CITATION+i+"_"+type+"_"+bibtexKey;
            i++;
        }
        return name;
    }

    public LinkedHashMap<BibtexEntry,BibtexDatabase> findCitedEntries
            (List<BibtexDatabase> databases, List<String> keys,
             HashMap<String,BibtexDatabase> linkSourceBase) {
        LinkedHashMap<BibtexEntry,BibtexDatabase> entries = new LinkedHashMap<BibtexEntry, BibtexDatabase>();
        for (String key : keys) {
            boolean found = false;
            bases: for (BibtexDatabase database : databases) {
                BibtexEntry entry = database.getEntryByKey(key);
                if (entry != null) {
                    entries.put(OOUtil.createAdaptedEntry(entry), database);
                    linkSourceBase.put(key, database);
                    found = true;
                    break bases;
                }
            }

            if (!found)
                entries.put(new UndefinedBibtexEntry(key), null);
        }
        return entries;
    }

    public List<String> findCitedKeys() throws com.sun.star.container.NoSuchElementException, WrappedTargetException {

        XReferenceMarksSupplier supplier = (XReferenceMarksSupplier) UnoRuntime.queryInterface(
                XReferenceMarksSupplier.class, xCurrentComponent);
        XNameAccess xNamedMarks = supplier.getReferenceMarks();
        String[] names = xNamedMarks.getElementNames();
        ArrayList<String> keys = new ArrayList<String>();
        for (int i = 0; i < names.length; i++) {
            Object bookmark = xNamedMarks.getByName(names[i]);
            XTextContent xTextContent = (XTextContent) UnoRuntime.queryInterface(
                    XTextContent.class, bookmark);

            String name = names[i];
            List<String> newKeys = parseRefMarkName(name);
            for (String key : newKeys)
                if (!keys.contains(key))
                    keys.add(key);
        }

        return keys;
    }

    public LinkedHashMap<BibtexEntry,BibtexDatabase> getSortedEntriesFromSortedRefMarks
            (String[] names,
            Map<BibtexEntry,BibtexDatabase> entries,
            HashMap<String,BibtexDatabase> linkSourceBase)
            throws BibtexEntryNotFoundException {
        
        LinkedHashMap<BibtexEntry,BibtexDatabase> newList = new LinkedHashMap<BibtexEntry,BibtexDatabase>();
        HashMap<BibtexEntry,BibtexEntry> adaptedEntries = new HashMap<BibtexEntry,BibtexEntry>();
        for (int i = 0; i < names.length; i++) {
            Matcher m = citePattern.matcher(names[i]);
            if (m.find()) {
                String[] keys = m.group(2).split(",");
                for (int j = 0; j < keys.length; j++) {
                    BibtexDatabase database = linkSourceBase.get(keys[j]);
                    BibtexEntry origEntry = null;
                    if (database != null) origEntry = database.getEntryByKey(keys[j]);
                    if (origEntry == null) {
                        System.out.println("Bibtex key not found : '"+keys[j]+"'");
                        System.out.println("Problem with reference mark: '"+names[i]+"'");
                        newList.put(new UndefinedBibtexEntry(keys[j]), null);
                        //throw new BibtexEntryNotFoundException(keys[j], "");
                    } else {
                        BibtexEntry entry = adaptedEntries.get(origEntry);
                        if (entry == null) {
                            entry = OOUtil.createAdaptedEntry(origEntry);
                            adaptedEntries.put(origEntry, entry);
                        }
                        if (!newList.containsKey(entry)) {
                            newList.put(entry, database);
                        }
                    }
                }
            }
        }

        return newList;
    }
    
    public Point findPosition(XTextViewCursor cursor, XTextRange range) {
        cursor.gotoRange(range, false);
        return cursor.getPosition();
    }

    /**
     * Extract the list of bibtex keys from a reference mark name.
     * @param name The reference mark name.
     * @return The list of bibtex keys encoded in the name.
     */
    public List<String> parseRefMarkName(String name) {
        ArrayList<String> keys = new ArrayList<String>();
        Matcher m = citePattern.matcher(name);
        if (m.find()) {
            String[] keystring = m.group(2).split(",");
            for (int j = 0; j < keystring.length; j++) {
                if (!keys.contains(keystring[j]))
                    keys.add(keystring[j]);
            }
        }
        return keys;
    }

    /**
     * Resolve the bibtex key from a citation reference marker name, and look up
     * the index of the key in a list of keys.
     * @param citRefName The name of the ReferenceMark representing the citation.
     * @param keys A List of bibtex keys representing the entries in the bibliography.
     * @return the indices of the cited keys, -1 if a key is not found. Returns null if the ref name
     *   could not be resolved as a citation.
     */
    public int[] findCitedEntryIndex(String citRefName, List<String> keys) {
        Matcher m = citePattern.matcher(citRefName);
        if (m.find()) {
            String[] keyStrings = m.group(2).split(",");
            int[] res = new int[keyStrings.length];
            for (int i=0; i<keyStrings.length; i++) {
                int ind = keys.indexOf(keyStrings[i]);
                res[i] = ind != -1 ? 1+ind : -1;
            }
            return res;
        }
        else
            return null;
    }

    public String getCitationContext(XNameAccess nameAccess, String refMarkName,
                                     int charBefore, int charAfter,
                                     boolean htmlMarkup) throws Exception {
        Object o = nameAccess.getByName(refMarkName);
        XTextContent bm = (XTextContent) UnoRuntime.queryInterface
                (XTextContent.class, o);

        XTextCursor cursor = bm.getAnchor().getText().createTextCursorByRange(bm.getAnchor());
        String citPart = cursor.getString();
        int flex = 8;
        for (int i=0; i<charBefore; i++) {
            try {
                cursor.goLeft((short)1, true);
                if ((i >= charBefore-flex) && Character.isWhitespace(cursor.getString().charAt(0)))
                    break;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        int length = cursor.getString().length();
        int added = length - citPart.length();
        cursor.collapseToStart();
        for (int i=0; i<charAfter+length; i++) {
            try {
                cursor.goRight((short)1, true);
                if (i >= charAfter+length-flex) {
                    String strNow = cursor.getString();
                    if (Character.isWhitespace(strNow.charAt(strNow.length()-1)))
                        break;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        String result = cursor.getString();
        if (htmlMarkup) {
            result = result.substring(0, added)+"<b>"+citPart+
                    "</b>"+result.substring(length);
        }
        return result.trim();
    }

    public void insertFullReferenceAtCursor(XTextCursor cursor, Map<BibtexEntry,BibtexDatabase> entries,
                                            OOBibStyle style, String parFormat)
            throws UndefinedParagraphFormatException, Exception {
        // If we don't have numbered entries, we need to sort the entries before adding them:
        if (!style.isSortByPosition()) {
            Map<BibtexEntry,BibtexDatabase> newMap = new TreeMap<BibtexEntry,BibtexDatabase>(entryComparator);
            for (BibtexEntry entry : entries.keySet())
                newMap.put(entry, entries.get(entry));
            entries = newMap;
        }
        int number = 1;
        for (BibtexEntry entry : entries.keySet()) {
            if (entry instanceof UndefinedBibtexEntry)
                continue;
            OOUtil.insertParagraphBreak(text, cursor);
            if (style.isNumberEntries()) {
                int minGroupingCount = style.getIntCitProperty("MinimumGroupingCount");
                OOUtil.insertTextAtCurrentLocation(text, cursor,
                        style.getNumCitationMarker(new int[] {number++}, minGroupingCount, true),
                        false, false, false, false, false, false);
            }
            Layout layout = style.getReferenceFormat(entry.getType().getName());
            try {
                layout.setPostFormatter(OOUtil.postformatter);
            } catch (NoSuchMethodError ex) {
                
            }
            OOUtil.insertFullReferenceAtCurrentLocation(text, cursor, layout, parFormat, entry,
                    entries.get(entry), uniquefiers.get(entry.getCiteKey()));
        }
        
    }

    public void insertFullReferenceAtViewCursor(Map<BibtexEntry,BibtexDatabase> entries,
                                                OOBibStyle style, String parFormat) throws Exception {
        XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
        insertFullReferenceAtCursor(xViewCursor, entries, style, parFormat);
    }


    public void insertMarkedUpTextAtViewCursor(String lText, String parFormat) throws Exception {
        XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
        XTextCursor cursor = text.createTextCursorByRange(xViewCursor.getEnd());
        OOUtil.insertOOFormattedTextAtCurrentLocation(text,cursor, lText, parFormat);
        
    }

    /**
     * This method creates and inserts an XTextSection named as determined by the
     * string BIB_SECTION_NAME.
     * @param end true to indicate that the section should be put at the end of the document,
     *  false to indicate that it should be put at the cursor position.
     * @return true if the bibliography already existed, false otherwise..
     * @throws Exception
     */
    public boolean createBibTextSection(boolean end) throws Exception {
        // Check if there already is a bookmarked section:
        XBookmarksSupplier bSupp = (XBookmarksSupplier) UnoRuntime.queryInterface(
                 XBookmarksSupplier.class, mxDoc);
        if (bSupp.getBookmarks().hasByName(BIB_SECTION_NAME)) {
            System.out.println("Found existing JabRef bookmark");
            return true;
        }
        XTextCursor mxDocCursor = text.createTextCursor();
        if (end)
            mxDocCursor.gotoEnd(false);
        OOUtil.insertParagraphBreak(text, mxDocCursor);
        insertBookMark(BIB_SECTION_NAME, mxDocCursor);
        return false;
    }

    public void createBibTextSection2(boolean end) throws Exception {

        XTextCursor mxDocCursor = text.createTextCursor();
        if (end)
            mxDocCursor.gotoEnd(false);
        OOUtil.insertParagraphBreak(text, mxDocCursor);
        // Create a new TextSection from the document factory and access it's XNamed interface
        XNamed xChildNamed = UnoRuntime.queryInterface(
          XNamed.class, mxDocFactory.createInstance("com.sun.star.text.TextSection"));
        // Set the new sections name to 'Child_Section'
        xChildNamed.setName(BIB_SECTION_NAME);
        // Access the Child_Section's XTextContent interface and insert it into the document
         XTextContent xChildSection = (XTextContent) UnoRuntime.queryInterface(
             XTextContent.class, xChildNamed);
         text.insertTextContent (mxDocCursor, xChildSection, false);
        
    }

    public void clearBibTextSectionContent2() throws Exception {

        // Check if the section exists:
        boolean exists = false;
        XTextSectionsSupplier supp = UnoRuntime.queryInterface(
                XTextSectionsSupplier.class, mxDoc);
        try {
            XTextSection section = (XTextSection)((Any)supp.getTextSections().getByName(BIB_SECTION_NAME)).getObject();
            // Clear it:
            XTextCursor cursor = text.createTextCursorByRange(section.getAnchor());
            cursor.gotoRange(section.getAnchor(), false);
            cursor.setString("");

        } catch (NoSuchElementException ex) {
            createBibTextSection2(atEnd);
        }
    }

    public void clearBibTextSectionContent() throws Exception {
        // Get a range comparator:
        XTextRangeCompare compare = (XTextRangeCompare) UnoRuntime.queryInterface
                (XTextRangeCompare.class, text);
        // Find the bookmarks for the bibliography:
        XTextRange range = getBookmarkRange(BIB_SECTION_NAME);
        if (range == null) {
            createBibTextSection(atEnd);
        }


        XTextRange rangeEnd = getBookmarkRange(BIB_SECTION_END_NAME);
        if (rangeEnd == null) {
            // No end bookmark. This means that there is no bibliography.
            return;
        }
        // Get a paragraph cursor at the start of the bibliography:
        //System.out.println("text="+text+" range="+range);
        XTextCursor mxDocCursor = text.createTextCursorByRange(range.getEnd());
        mxDocCursor.goRight((short)1, true);
        boolean couldExpand = true;
        while (couldExpand && (compare.compareRegionEnds(mxDocCursor, rangeEnd) > 0)) {
            couldExpand = mxDocCursor.goRight((short)1, true);
        }
        // Finally, clear the bibliography:
        mxDocCursor.setString("");
        mxDocCursor.collapseToStart();
        removeBookMark(BIB_SECTION_END_NAME);
        // If we lost the start bookmark, recreate it:
        if (getBookmarkRange(BIB_SECTION_NAME) == null)
            insertBookMark(BIB_SECTION_NAME, mxDocCursor);
    }

    public void populateBibTextSection(Map<BibtexEntry,BibtexDatabase> entries,
                                       OOBibStyle style)
            throws UndefinedParagraphFormatException, Exception {
        XTextSectionsSupplier supp = UnoRuntime.queryInterface(XTextSectionsSupplier.class, mxDoc);
        XTextSection section = (XTextSection)((Any)supp.getTextSections().getByName(BIB_SECTION_NAME))
                    .getObject();
        XTextCursor cursor = text.createTextCursorByRange(section.getAnchor());
        OOUtil.insertTextAtCurrentLocation(text, cursor, (String)style.getProperty("Title"),
                (String)style.getProperty("ReferenceHeaderParagraphFormat"));
        insertFullReferenceAtCursor(cursor, entries, style,
                (String)style.getProperty("ReferenceParagraphFormat"));
        insertBookMark(BIB_SECTION_END_NAME, cursor);
    }

    public XTextContent insertBookMark(String name, XTextCursor position) throws Exception {
        Object bookmark = mxDocFactory.createInstance("com.sun.star.text.Bookmark");
        // name the bookmark
        XNamed xNamed = (XNamed) UnoRuntime.queryInterface(
                XNamed.class, bookmark);
        xNamed.setName(name);
        // get XTextContent interface
        XTextContent xTextContent = (XTextContent) UnoRuntime.queryInterface(
                XTextContent.class, bookmark);
        // insert bookmark at the end of the document
        // instead of mxDocText.getEnd you could use a text cursor's XTextRange interface or any XTextRange
        text.insertTextContent(position, xTextContent, true);
        position.collapseToEnd();
        return xTextContent;
    }

    public void insertReferenceMark(String name, String citText, XTextCursor position, boolean withText,
                               OOBibStyle style)
            throws Exception {

        // Check if there is "page info" stored for this citation. If so, insert it into 
        // the citation text before inserting the citation:
        String pageInfo = getCustomProperty(name);
        if (pageInfo != null) {
            citText = style.insertPageInfo(citText, pageInfo);
        }

        Object bookmark = mxDocFactory.createInstance("com.sun.star.text.ReferenceMark");
        // Name the reference
        XNamed xNamed = UnoRuntime.queryInterface(XNamed.class, bookmark);
        xNamed.setName(name);
        // get XTextContent interface
        if (true) {

            XTextContent xTextContent = UnoRuntime.queryInterface(XTextContent.class, bookmark);
            if (withText) {
                position.setString(citText);
                XPropertySet xCursorProps = UnoRuntime.queryInterface(XPropertySet.class, position);

                // Set language to [None]:
                xCursorProps.setPropertyValue("CharLocale", new Locale("zxx", "", ""));
                if (style.isFormatCitations()) {
                    String charStyle = style.getCitationCharacterFormat();
                    try {
                        xCursorProps.setPropertyValue("CharStyleName", charStyle);
                    } catch (Throwable ex) {
                        throw new UndefinedCharacterFormatException(charStyle);
                    }
                }
            }
            else
                position.setString("");

            position.getText().insertTextContent(position, xTextContent, true);

            // Check if we should italicize the "et al." string in citations:
            boolean italicize = style.getBooleanCitProperty("ItalicEtAl");
            if (italicize) {
                String etAlString = style.getStringCitProperty("EtAlString");
                int index = citText.indexOf(etAlString);
                if (index >= 0) {
                    italicizeOrBold(position, true, index, index+etAlString.length());
                }
            }

        }
        position.collapseToEnd();

    }

    private void italicizeOrBold(XTextCursor position, boolean italicize,
                                 int start, int end) throws Exception {
        XTextRange rng = position.getStart();
        XTextCursor cursor = position.getText().createTextCursorByRange(rng);
        cursor.goRight((short)start, false);
        cursor.goRight((short)(end-start), true);
        XPropertySet xcp = UnoRuntime.queryInterface(XPropertySet.class, cursor);
        if (italicize)
            xcp.setPropertyValue("CharPosture", com.sun.star.awt.FontSlant.ITALIC);
        else
            xcp.setPropertyValue("CharWeight", com.sun.star.awt.FontWeight.BOLD);
    }

    public void testFootnote() throws Exception {
        XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
        insertFootnote("jabbes", "Cite text", xViewCursor);
    }

    public void insertFootnote(String name, String citText, XTextCursor position) throws Exception {
        XFootnote xFootnote = (XFootnote) UnoRuntime.queryInterface( XFootnote.class,
            mxDocFactory.createInstance("com.sun.star.text.Footnote"));
        xFootnote.setLabel("");
        XPropertySet props = (XPropertySet)UnoRuntime.queryInterface(XPropertySet.class, xFootnote);
        props.setPropertyValue("ReferenceId", name); // doesn't work: short data type
        System.out.println(props.getPropertyValue("ReferenceId"));
        XTextContent xContent = (XTextContent)UnoRuntime.queryInterface(
            XTextContent.class, xFootnote);
        text.insertTextContent (position, xContent, false);
        XSimpleText xSimple = (XSimpleText)UnoRuntime.queryInterface(XSimpleText.class, xFootnote);
        XTextRange xRange = (XTextRange)UnoRuntime.queryInterface(XTextRange.class, xSimple.createTextCursor());
        xSimple.insertString (xRange, citText, false);
    }

    public void removeBookMark(String name) throws Exception {
        XBookmarksSupplier xBookmarksSupplier = (XBookmarksSupplier) UnoRuntime.queryInterface(
                XBookmarksSupplier.class, xCurrentComponent);
        if (xBookmarksSupplier.getBookmarks().hasByName(name)) {
            Object o = xBookmarksSupplier.getBookmarks().getByName(name);
            XTextContent bm = (XTextContent) UnoRuntime.queryInterface(
                XTextContent.class, o);
            text.removeTextContent(bm);
        }
    }

    public void removeReferenceMark(String name) throws Exception {
        XReferenceMarksSupplier xSupplier = (XReferenceMarksSupplier) UnoRuntime.queryInterface(
                XReferenceMarksSupplier.class, xCurrentComponent);
        if (xSupplier.getReferenceMarks().hasByName(name)) {
            Object o = xSupplier.getReferenceMarks().getByName(name);
            XTextContent bm = (XTextContent) UnoRuntime.queryInterface(
                XTextContent.class, o);
            text.removeTextContent(bm);
        }
    }

    /**
     * Get the XTextRange corresponding to the named bookmark.
     * @param name The name of the bookmark to find.
     * @return The XTextRange for the bookmark.
     * @throws Exception
     */
    public XTextRange getBookmarkRange(String name) throws Exception {
        // query XBookmarksSupplier from document model and get bookmarks collection
        XBookmarksSupplier xBookmarksSupplier = (XBookmarksSupplier) UnoRuntime.queryInterface(
                XBookmarksSupplier.class, xCurrentComponent);
        XNameAccess xNamedBookmarks = xBookmarksSupplier.getBookmarks();

        // retrieve bookmark by name
        //System.out.println("Name="+name+" : "+xNamedBookmarks.hasByName(name));
        if (!xNamedBookmarks.hasByName(name))
            return null;
        Object foundBookmark = xNamedBookmarks.getByName(name);
        XTextContent xFoundBookmark = (XTextContent) UnoRuntime.queryInterface(
                XTextContent.class, foundBookmark);
        return xFoundBookmark.getAnchor();
    }

    public void printBookmarkNames() throws Exception {
        XBookmarksSupplier xBookmarksSupplier = (XBookmarksSupplier) UnoRuntime.queryInterface(
                XBookmarksSupplier.class, xCurrentComponent);
        XNameAccess xNamedBookmarks = xBookmarksSupplier.getBookmarks();
        String[] names = xNamedBookmarks.getElementNames();
        for (int i = 0; i < names.length; i++) {
            System.out.println(i+". "+names[i]);
        }
    }


    /**
     * Focus the active OO document.
     */
    public void setFocus() {
        xDesktop.getCurrentFrame().getContainerWindow().setFocus();
    }


    public void combineCiteMarkers(List<BibtexDatabase> databases, OOBibStyle style) throws Exception {
        XReferenceMarksSupplier supplier = (XReferenceMarksSupplier) UnoRuntime.queryInterface(
                XReferenceMarksSupplier.class, xCurrentComponent);
        XNameAccess nameAccess = supplier.getReferenceMarks();
        // TODO: doesn't work for citations in footnotes/tables
        String[] names = getSortedReferenceMarks(nameAccess);


        final XTextRangeCompare compare = (XTextRangeCompare) UnoRuntime.queryInterface
                (XTextRangeCompare.class, text);

        int piv = 0;
        boolean madeModifications = false;
        while (piv < names.length-1) {
            XTextRange r1 = ((XTextContent) UnoRuntime.queryInterface
                            (XTextContent.class, nameAccess.getByName(names[piv]))).getAnchor().getEnd();
            XTextRange r2 = ((XTextContent) UnoRuntime.queryInterface
                            (XTextContent.class,
                                    nameAccess.getByName(names[piv+1]))).getAnchor().getStart();
            if (r1.getText() != r2.getText()) {
                piv++;
                continue;
            }
            XTextCursor mxDocCursor = r1.getText().createTextCursorByRange(r1);
            mxDocCursor.goRight((short)1, true);
            boolean couldExpand = true;
            while (couldExpand && (compare.compareRegionEnds(mxDocCursor, r2) > 0)) {
                couldExpand = mxDocCursor.goRight((short)1, true);
            }
            String text = mxDocCursor.getString();
            // Check if the string contains no line breaks and only whitespace:
            if ((text.indexOf('\n') == -1) && (text.trim().length() == 0)) {

                // If we are supposed to set character format for citations, test this before
                // making any changes. This way we can throw an exception before any reference
                // marks are removed, preventing damage to the user's document:
                if (style.isFormatCitations()) {
                    XPropertySet xCursorProps = (XPropertySet) UnoRuntime.queryInterface(
                                        XPropertySet.class, mxDocCursor);
                    String charStyle = style.getCitationCharacterFormat();
                    try {
                        xCursorProps.setPropertyValue("CharStyleName", charStyle);
                    } catch (Throwable ex) {
                        // Setting the character format failed, so we throw an exception that
                        // will result in an error message for the user:
                        throw new UndefinedCharacterFormatException(charStyle);
                    }
                }

                List<String> keys = parseRefMarkName(names[piv]);
                keys.addAll(parseRefMarkName(names[piv+1]));
                removeReferenceMark(names[piv]);
                removeReferenceMark(names[piv+1]);
                ArrayList<BibtexEntry> entries = new ArrayList<BibtexEntry>();
                for (String key : keys) {
                    bases: for (BibtexDatabase database : databases) {
                        BibtexEntry entry = database.getEntryByKey(key);
                        if (entry != null) {
                            entries.add(OOUtil.createAdaptedEntry(entry));
                            break bases;
                        }
                    }
                }
                Collections.sort(entries, new FieldComparator("year"));
                StringBuilder sb = new StringBuilder();
                int i=0;
                for (BibtexEntry entry : entries) {
                    if (i > 0)
                        sb.append(",");
                    sb.append(entry.getCiteKey());
                    i++;
                }
                String keyString = sb.toString();
                boolean inParenthesis = true;
                // Insert bookmark:
                String bName = getUniqueReferenceMarkName(keyString,
                        inParenthesis ? AUTHORYEAR_PAR : AUTHORYEAR_INTEXT);
                insertReferenceMark(bName, "tmp", mxDocCursor, true, style);
                names[piv+1] = bName;
                madeModifications = true;
            }
            piv++;
        }
        if (madeModifications) {
            updateSortedReferenceMarks();
            refreshCiteMarkers(databases, style);
        }


    }

    public void testFrameHandling() throws Exception {

        XController oldController = mxDoc.getCurrentController();
        PropertyValue[] props = new PropertyValue[2];

        props[0] = new PropertyValue();
        props[0].Name = "Model";

        props[0].Value = mxDoc.getCurrentController().getModel();
        props[1] = new PropertyValue();
        props[1].Name = "Hidden";
        props[1].Value = true;

        // argument xModel wins over URL.
        System.out.println("her");
        XComponent comp = xComponentLoader.loadComponentFromURL("private:factory/swriter",
                           "_blank", 0, props);
        System.out.println("her2");

        XTextDocument newDoc = (XTextDocument)UnoRuntime.queryInterface(
                XTextDocument.class, comp);
        System.out.println("newDoc = "+newDoc);

        // Controller of the hidden frame
        XController xController = newDoc.getCurrentController();

        XFrame xFrame = xController.getFrame();
        XWindow xContainerWindow = xFrame.getContainerWindow();
        XWindow xComponentWindow = xFrame.getComponentWindow();

        //xContainerWindow.setVisible(true);
        //xComponentWindow.setFocus();
        //xContainerWindow.setVisible(false);
        xFrame.dispose();
    }
}
