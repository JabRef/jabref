/*  Copyright (C) 2003-2015 JabRef contributors.
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
package net.sf.jabref.openoffice;

import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.exporter.layout.Layout;
import net.sf.jabref.exporter.layout.LayoutFormatter;
import net.sf.jabref.exporter.layout.LayoutHelper;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This class embodies a bibliography formatting for OpenOffice, which is composed
 * of the following elements:
 * <p>
 * 1) Each OO bib entry type must have a formatting. A formatting is an array of elements, each
 * of which is either a piece of constant text, an entry field value, or a tab. Each element has
 * a character format associated with it.
 * <p>
 * 2) Many field values (e.g. author) need to be formatted before input to OpenOffice. The style
 * has the responsibility of formatting all field values. Formatting is handled by 0-n
 * JabRef LayoutFormatter classes.
 * <p>
 * 3) If the entries are not numbered, a citation marker must be produced for each entry. This
 * operation is performed for each JabRef BibEntry.
 */
class OOBibStyle implements Comparable<OOBibStyle> {

    public static final String UNDEFINED_CITATION_MARKER = "??";
    private String name;
    private final SortedSet<String> journals = new TreeSet<>();

    // Formatter to be run on fields before they are used as part of citation marker:
    private final LayoutFormatter fieldFormatter = new OOPreFormatter();

    private Layout defaultBibLayout;

    // reference layout mapped from entry type number:
    private final Map<String, Layout> bibLayout = new HashMap<>();

    private final Map<String, Object> properties = new HashMap<>();
    private final Map<String, Object> citProperties = new HashMap<>();

    private static final Pattern numPattern = Pattern.compile("-?\\d+");

    private boolean valid;

    private static final int NONE = 0;
    private static final int LAYOUT = 1;
    private static final int PROPERTIES = 2;
    private static final int CITATION = 3;
    private static final int NAME = 4;
    private static final int JOURNALS = 5;
    private static final String LAYOUT_MRK = "LAYOUT";
    private static final String PROPERTIES_MARK = "PROPERTIES";
    private static final String CITATION_MARK = "CITATION";
    private static final String NAME_MARK = "NAME";
    private static final String JOURNALS_MARK = "JOURNALS";
    private static final String DEFAULT_MARK = "default";
    private File styleFile;
    private static long styleFileModificationTime = Long.MIN_VALUE;

    //private Pattern quoted = Pattern.compile("\".*^\\\\\"");
    private static final Pattern quoted = Pattern.compile("\".*\"");

    private static final Log LOGGER = LogFactory.getLog(OOBibStyle.class);


    public OOBibStyle(File styleFile) throws IOException {
        setDefaultProperties();
        try (Reader in = new FileReader(styleFile)) {
            initialize(in);
        }
        this.styleFile = styleFile;
        OOBibStyle.styleFileModificationTime = styleFile.lastModified();
    }

    public OOBibStyle(Reader in) throws IOException {
        setDefaultProperties();
        initialize(in);
    }

    private void setDefaultProperties() {
        // Set default property values:
        properties.put("Title", "Bibliography");
        properties.put("SortAlgorithm", "alphanumeric");
        properties.put("IsSortByPosition", Boolean.FALSE);
        properties.put("IsNumberEntries", Boolean.FALSE);
        properties.put("BracketBefore", "[");
        properties.put("BracketAfter", "]");
        properties.put("ReferenceParagraphFormat", "Default");
        properties.put("ReferenceHeaderParagraphFormat", "Heading 1");

        // Set default properties for the citation marker:
        citProperties.put("AuthorField", "author/editor");
        citProperties.put("YearField", "year");
        citProperties.put("MaxAuthors", 3);
        citProperties.put("MaxAuthorsFirst", -1);
        citProperties.put("AuthorSeparator", ", ");
        citProperties.put("AuthorLastSeparator", " & ");
        citProperties.put("AuthorLastSeparatorInText", null);
        citProperties.put("EtAlString", " et al.");
        citProperties.put("YearSeparator", ", ");
        citProperties.put("InTextYearSeparator", " ");
        citProperties.put("BracketBefore", "(");
        citProperties.put("BracketAfter", ")");
        citProperties.put("CitationSeparator", "; ");
        citProperties.put("PageInfoSeparator", "; ");
        citProperties.put("GroupedNumbersSeparator", "-");
        citProperties.put("MinimumGroupingCount", 3);
        citProperties.put("FormatCitations", Boolean.FALSE);
        citProperties.put("CitationCharacterFormat", "Default");
        citProperties.put("ItalicCitations", Boolean.FALSE);
        citProperties.put("BoldCitations", Boolean.FALSE);
        citProperties.put("SuperscriptCitations", Boolean.FALSE);
        citProperties.put("SubscriptCitations", Boolean.FALSE);
        citProperties.put("MultiCiteChronological", Boolean.TRUE);
        citProperties.put("BibTeXKeyCitations", Boolean.FALSE);
        citProperties.put("ItalicEtAl", Boolean.FALSE);
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return styleFile;
    }

    public Set<String> getJournals() {
        return Collections.unmodifiableSet(journals);
    }

    private void initialize(Reader in) throws IOException {
        name = null;
        readFormatFile(in);
    }

    /**
     * If this style was initialized from a file on disk, reload the style
     * if the file has been modified since it was read.
     *
     * @throws Exception
     */
    public void ensureUpToDate() throws IOException {
        if (!isUpToDate()) {
            reload();
        }
    }

    /**
     * If this style was initialized from a file on disk, reload the style
     * information.
     *
     * @throws Exception
     */
    private void reload() throws IOException {
        if (styleFile != null) {
            OOBibStyle.styleFileModificationTime = styleFile.lastModified();
            try (FileReader fr = new FileReader(styleFile)) {
                initialize(fr);
            }
        }
    }

    /**
     * If this style was initialized from a file on disk, check whether the file
     * is unmodified since initialization.
     *
     * @return true if the file has not been modified, false otherwise.
     */
    private boolean isUpToDate() {
        if (styleFile == null) {
            return true;
        } else {
            return styleFile.lastModified() == OOBibStyle.styleFileModificationTime;
        }
    }

    private void readFormatFile(Reader in) throws IOException {

        // First read all the contents of the file:
        StringBuffer sb = new StringBuffer();
        int c;
        while ((c = in.read()) != -1) {
            sb.append((char) c);
        }
        // Break into separate lines:
        String[] lines = sb.toString().split("\n");
        int mode = OOBibStyle.NONE;

        for (String line1 : lines) {
            String line = line1;
            if (!line.isEmpty() && (line.charAt(line.length() - 1) == '\r')) {
                line = line.substring(0, line.length() - 1);
            }
            // Check for empty line or comment:
            if (line.trim().isEmpty() || (line.charAt(0) == '#')) {
                continue;
            }
            // Check if we should change mode:
            if (line.equals(OOBibStyle.NAME_MARK)) {
                mode = OOBibStyle.NAME;
                continue;
            } else if (line.equals(OOBibStyle.LAYOUT_MRK)) {
                mode = OOBibStyle.LAYOUT;
                continue;
            } else if (line.equals(OOBibStyle.PROPERTIES_MARK)) {
                mode = OOBibStyle.PROPERTIES;
                continue;
            } else if (line.equals(OOBibStyle.CITATION_MARK)) {
                mode = OOBibStyle.CITATION;
                continue;
            } else if (line.equals(OOBibStyle.JOURNALS_MARK)) {
                mode = OOBibStyle.JOURNALS;
                continue;
            }

            switch (mode) {
            case NAME:
                if (!line.trim().isEmpty()) {
                    name = line.trim();
                }
                break;
            case LAYOUT:
                handleStructureLine(line);
                break;
            case PROPERTIES:
                handlePropertiesLine(line, properties);
                break;
            case CITATION:
                handlePropertiesLine(line, citProperties);
                break;
            case JOURNALS:
                handleJournalsLine(line);
            }

        }

        // Set validity boolean based on whether we found anything interesting
        // in the file:
        if (mode != OOBibStyle.NONE) {
            valid = true;
        }

    }

    /**
     * After initalizing this style from a file, this method can be used to check
     * whether the file appeared to be a proper style file.
     *
     * @return true if the file could be parsed as a style file, false otherwise.
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Parse a line providing bibliography structure information for an entry type.
     *
     * @param line The string containing the structure description.
     * @throws IOException
     */
    private void handleStructureLine(String line) {
        int index = line.indexOf('=');
        if ((index > 0) && (index < (line.length() - 1))) {
            String formatString = line.substring(index + 1);
            boolean setDefault = line.substring(0, index).equals(OOBibStyle.DEFAULT_MARK);
            String type = line.substring(0, index);
            try {
                Layout layout = new LayoutHelper(new StringReader(formatString)).
                        getLayoutFromText(Globals.FORMATTER_PACKAGE);
                if (setDefault) {
                    defaultBibLayout = layout;
                } else {
                    bibLayout.put(type.toLowerCase(), layout);
                }

            } catch (IOException ex) {
                LOGGER.warn("Cannot parse bibliography structure", ex);
            }
        }
    }

    /**
     * Parse a line providing a property name and value.
     *
     * @param line The line containing the formatter names.
     * @throws IOException
     */
    private void handlePropertiesLine(String line, Map<String, Object> map) {
        int index = line.indexOf('=');
        if ((index > 0) && (index <= (line.length() - 1))) {
            String propertyName = line.substring(0, index).trim();
            String value = line.substring(index + 1);
            if ((value.trim().length() > 2) && quoted.matcher(value.trim()).matches()) {
                value = value.trim().substring(1, value.trim().length() - 1);
            }
            Object toSet = value;
            if (numPattern.matcher(value).matches()) {
                toSet = Integer.parseInt(value);
            } else if ("true".equals(value.toLowerCase().trim())) {
                toSet = Boolean.TRUE;
            } else if ("false".equals(value.toLowerCase().trim())) {
                toSet = Boolean.FALSE;
            }
            map.put(propertyName, toSet);
        }
    }

    /**
     * Parse a line providing a journal name for which this style is valid.
     *
     * @param line
     * @throws IOException
     */
    private void handleJournalsLine(String line) {
        if (!line.trim().isEmpty()) {
            journals.add(line.trim());
        }
    }

    public Layout getReferenceFormat(String type) {
        Layout l = bibLayout.get(type.toLowerCase());
        if (l == null) {
            return defaultBibLayout;
        } else {
            return l;
        }
    }

    /**
     * Get the array of elements composing the reference for a given entry type.
     * @param bibType The OO type number.
     * @return The format definition.

    public PropertyValue[][] getReferenceFormat(short bibType) {
    Object o = bibLayout.get(new Short(bibType));
    if (o != null)
    return (PropertyValue[][])o;
    else
    return defaultBibLayout;
    }*/

    /**
     * Format a number-based citation marker for the given number.
     *
     * @param number The citation numbers.
     * @return The text for the citation.
     */
    public String getNumCitationMarker(List<Integer> number, int minGroupingCount, boolean inList) {
        String bracketBefore = (String) citProperties.get("BracketBefore");
        if (inList && (citProperties.get("BracketBeforeInList") != null)) {
            bracketBefore = (String) citProperties.get("BracketBeforeInList");
        }
        String bracketAfter = (String) citProperties.get("BracketAfter");
        if (inList && (citProperties.get("BracketAfterInList") != null)) {
            bracketAfter = (String) citProperties.get("BracketAfterInList");
        }
        // Sort the numbers:
        List<Integer> lNum = new ArrayList<>(number);
        Collections.sort(lNum);
        StringBuilder sb = new StringBuilder(bracketBefore);
        int combineFrom = -1;
        int written = 0;
        for (int i = 0; i < lNum.size(); i++) {
            int i1 = lNum.get(i);
            if (combineFrom < 0) {
                // Check if next entry is the next in the ref list:
                if ((i < (lNum.size() - 1)) && (lNum.get(i + 1) == (i1 + 1))) {
                    combineFrom = i1;
                } else {
                    // Add single entry:
                    if (i > 0) {
                        sb.append((String) citProperties.get("CitationSeparator"));
                    }
                    sb.append(lNum.get(i) > 0 ? String.valueOf(lNum.get(i)) : OOBibStyle.UNDEFINED_CITATION_MARKER);
                    written++;
                }
            } else {
                // We are building a list of combined entries.
                // Check if it ends here:
                if ((i == (lNum.size() - 1)) || (lNum.get(i + 1) != (i1 + 1))) {
                    if (written > 0) {
                        sb.append((String) citProperties.get("CitationSeparator"));
                    }
                    if ((minGroupingCount > 0) && (((i1 + 1) - combineFrom) >= minGroupingCount)) {
                        sb.append(combineFrom);
                        sb.append((String) citProperties.get("GroupedNumbersSeparator"));
                        sb.append(i1);
                        written++;
                    } else {
                        // Either we should never group, or there aren't enough
                        // entries in this case to group. Output all:
                        for (int jj = combineFrom; jj <= i1; jj++) {
                            sb.append(jj);
                            if (jj < i1) {
                                sb.append((String) citProperties.get("CitationSeparator"));
                            }
                            written++;
                        }
                    }
                    combineFrom = -1;

                }
                // If it doesn't end here, just keep iterating.
            }

        }
        sb.append(bracketAfter);
        return sb.toString();
    }

    /**
     * Format the marker for the in-text citation according to this bib style.
     *
     * @param cEntries         The JabRef BibEntry providing the data.
     * @param inParenthesis Signals whether a parenthesized citation or an in-text citation is wanted.
     * @param strings    String to add behind the year in case it's needed to separate similar
     *                      entries.
     * @return The formatted citation.
     */
    public String getCitationMarker(BibEntry[] cEntries, BibDatabase database, boolean inParenthesis, String[] strings,
            int[] is) {
        return getCitationMarker(Arrays.asList(cEntries), database, inParenthesis, strings, is);
    }

    /**
     * Format the marker for the in-text citation according to this bib style. Uniquefier letters are added as
     * provided by the uniquefiers argument. If successive entries within the citation are uniquefied from each other,
     * this method will perform a grouping of these entries.
     *
     * @param entries       The array of JabRef BibEntry providing the data.
     * @param inParenthesis Signals whether a parenthesized citation or an in-text citation is wanted.
     * @param uniquefiers   Strings to add behind the year for each entry in case it's needed to separate similar
     *                      entries.
     * @param unlimAuthors  Boolean for each entry. If true, we should not use "et al" formatting regardless
     *                      of the number of authors. Can be null to indicate that no entries should have unlimited names.
     * @return The formatted citation.
     */
    public String getCitationMarker(List<BibEntry> entries, BibDatabase database, boolean inParenthesis,
                                    String[] uniquefiers, int[] unlimAuthors) {
        // Look for groups of uniquefied entries that should be combined in the output.
        // E.g. (Olsen, 2005a, b) should be output instead of (Olsen, 2005a; Olsen, 2005b).
        int piv = -1;
        String tmpMarker = null;
        if (uniquefiers != null) {
            for (int i = 0; i < uniquefiers.length; i++) {

                if ((uniquefiers[i] != null) && !uniquefiers[i].isEmpty()) {
                    String authorField = (String) citProperties.get("AuthorField");
                    int maxAuthors = (Integer) citProperties.get("MaxAuthors");
                    if (piv == -1) {
                        piv = i;
                        tmpMarker = getAuthorYearParenthesisMarker(Arrays.asList(entries.get(i)), database,
                                authorField,
                                (String) citProperties.get("YearField"), maxAuthors,
                                (String) citProperties.get("AuthorSeparator"),
                                (String) citProperties.get("AuthorLastSeparator"),
                                (String) citProperties.get("EtAlString"), (String) citProperties.get("YearSeparator"),
                                (String) citProperties.get("BracketBefore"), (String) citProperties.get("BracketAfter"),
                                (String) citProperties.get("CitationSeparator"), null, unlimAuthors);
                        //System.out.println("piv="+piv+" tmpMarker='"+tmpMarker+"'");
                    } else {
                        // See if this entry can go into a group with the previous one:
                        String thisMarker = getAuthorYearParenthesisMarker(Arrays.asList(entries.get(i)), database,
                                authorField, (String) citProperties.get("YearField"), maxAuthors,
                                (String) citProperties.get("AuthorSeparator"),
                                (String) citProperties.get("AuthorLastSeparator"),
                                (String) citProperties.get("EtAlString"), (String) citProperties.get("YearSeparator"),
                                (String) citProperties.get("BracketBefore"), (String) citProperties.get("BracketAfter"),
                                (String) citProperties.get("CitationSeparator"), null, unlimAuthors);

                        String author = getCitationMarkerField(entries.get(i), database, authorField);
                        AuthorList al = AuthorList.getAuthorList(author);
                        //System.out.println("i="+i+" thisMarker='"+thisMarker+"'");
                        int prevALim = unlimAuthors[i - 1]; // i always at least 1 here
                        if (!thisMarker.equals(tmpMarker)
                                || ((al.size() > maxAuthors) && (unlimAuthors[i] != prevALim))) {
                            // No match. Update piv to exclude the previous entry. But first check if the
                            // previous entry was part of a group:
                            if ((piv > -1) && (i > (piv + 1))) {
                                // Do the grouping:
                                group(entries, uniquefiers, piv, i - 1,
                                        (String) citProperties.get("UniquefierSeparator"));
                            }
                            tmpMarker = thisMarker;
                            piv = i;
                        }
                    }
                } else {
                    // This entry has no uniquefier.
                    // Check if we just passed a group of more than one entry with uniquefier:
                    if ((piv > -1) && (i > (piv + 1))) {
                        // Do the grouping:
                        group(entries, uniquefiers, piv, i - 1, (String) citProperties.get("UniquefierSeparator"));
                    }

                    piv = -1;
                }

            }
            // Finished with the loop. See if the last entries form a group:
            if (piv >= 0) {
                // Do the grouping:
                group(entries, uniquefiers, piv, uniquefiers.length - 1,
                        (String) citProperties.get("UniquefierSeparator"));
            }
        }

        if (inParenthesis) {
            return getAuthorYearParenthesisMarker(entries, database,
                    (String) citProperties.get("AuthorField"),
                    (String) citProperties.get("YearField"),
                    (Integer) citProperties.get("MaxAuthors"),
                    (String) citProperties.get("AuthorSeparator"),
                    (String) citProperties.get("AuthorLastSeparator"),
                    (String) citProperties.get("EtAlString"),
                    (String) citProperties.get("YearSeparator"),
                    (String) citProperties.get("BracketBefore"),
                    (String) citProperties.get("BracketAfter"),
                    (String) citProperties.get("CitationSeparator"),
                    uniquefiers, unlimAuthors);
        } else {
            String authorLastSeparator = (String) citProperties.get("AuthorLastSeparator");
            String alsInText = (String) citProperties.get("AuthorLastSeparatorInText");
            if (alsInText != null) {
                authorLastSeparator = alsInText;
            }
            return getAuthorYearInTextMarker(entries, database,
                    (String) citProperties.get("AuthorField"),
                    (String) citProperties.get("YearField"),
                    (Integer) citProperties.get("MaxAuthors"),
                    (String) citProperties.get("AuthorSeparator"),
                    authorLastSeparator,
                    (String) citProperties.get("EtAlString"),
                    (String) citProperties.get("InTextYearSeparator"),
                    (String) citProperties.get("BracketBefore"),
                    (String) citProperties.get("BracketAfter"),
                    (String) citProperties.get("CitationSeparator"),
 uniquefiers, unlimAuthors);
        }
    }

    /**
     * Modify entry and uniqiefier arrays to facilitate a grouped presentation of uniqiefied entries.
     *
     * @param entries     The entry array.
     * @param uniquefiers The uniquefier array.
     * @param from        The first index to group (inclusive)
     * @param to          The last index to group (inclusive)
     * @param separator   The separator for the uniquefier letters.
     */
    private void group(List<BibEntry> entries, String[] uniquefiers, int from, int to, String separator) {
        StringBuilder sb = new StringBuilder(uniquefiers[from]);
        for (int i = from + 1; i <= to; i++) {
            sb.append(separator);
            sb.append(uniquefiers[i]);
            entries.set(i, null);
        }
        uniquefiers[from] = sb.toString();
    }

    /**
     * This method produces (Author, year) style citation strings in many different forms.
     *
     * @param entries           The array of BibEntry to get fields from.
     * @param authorField       The bibtex field providing author names, e.g. "author" or "editor".
     * @param yearField         The bibtex field providing the year, e.g. "year".
     * @param maxA              The maximum number of authors to write out in full without using etal. Set to
     *                          -1 to always write out all authors.
     * @param authorSep         The String to add between author names except the last two, e.g. ", ".
     * @param andString         The String to add between the two last author names, e.g. " & ".
     * @param etAlString        The String to represent authors that are not mentioned, e.g. " et al."
     * @param yearSep           The String to separate authors from year, e.g. "; ".
     * @param startBrace        The opening parenthesis.
     * @param endBrace          The closing parenthesis.
     * @param citationSeparator The String to separate citations from each other.
     * @param uniquifiers       Optional parameter to separate similar citations. Elements can be null if not needed.
     * @return The formatted citation.
     */
    private String getAuthorYearParenthesisMarker(List<BibEntry> entries, BibDatabase database,
                                                  String authorField, String yearField,
                                                  int maxA, String authorSep,
                                                  String andString, String etAlString, String yearSep,
                                                  String startBrace, String endBrace, String citationSeparator,
                                                  String[] uniquifiers, int[] unlimAuthors) {

        StringBuffer sb = new StringBuffer(startBrace);
        for (int j = 0; j < entries.size(); j++) {

            int unlimA = unlimAuthors == null ? -1 : unlimAuthors[j];
            int maxAuthors = unlimA > 0 ? unlimA : maxA;

            BibEntry entry = entries.get(j);

            // Check if this entry has been nulled due to grouping with the previous entry(ies):
            if (entry == null) {
                continue;
            }

            if (j > 0) {
                sb.append(citationSeparator);
            }

            String author = getCitationMarkerField(entry, database, authorField);

            if (author != null) {
                AuthorList al = AuthorList.getAuthorList(author);
                if (!al.isEmpty()) {
                    sb.append(getAuthorLastName(al, 0));
                }
                if ((al.size() > 1) && ((al.size() <= maxAuthors) || (maxAuthors < 0))) {
                    int i = 1;
                    while (i < (al.size() - 1)) {
                        sb.append(authorSep);
                        sb.append(getAuthorLastName(al, i));
                        i++;
                    }
                    sb.append(andString);
                    sb.append(getAuthorLastName(al, al.size() - 1));
                } else if (al.size() > maxAuthors) {
                    sb.append(etAlString);
                }
                sb.append(yearSep);
            }
            String year = getCitationMarkerField(entry, database, yearField);
            if (year != null) {
                sb.append(year);
            }
            if ((uniquifiers != null) && (uniquifiers[j] != null)) {
                sb.append(uniquifiers[j]);
            }
        }
        sb.append(endBrace);
        return sb.toString();

    }

    /**
     * This method produces "Author (year)" style citation strings in many different forms.
     *
     * @param entries     The array of BibEntry to get fields from.
     * @param authorField The bibtex field providing author names, e.g. "author" or "editor".
     * @param yearField   The bibtex field providing the year, e.g. "year".
     * @param maxA        The maximum number of authors to write out in full without using etal. Set to
     *                    -1 to always write out all authors.
     * @param authorSep   The String to add between author names except the last two, e.g. ", ".
     * @param andString   The String to add between the two last author names, e.g. " & ".
     * @param etAlString  The String to represent authors that are not mentioned, e.g. " et al."
     * @param yearSep     The String to separate authors from year, e.g. "; ".
     * @param startBrace  The opening parenthesis.
     * @param endBrace    The closing parenthesis.
     * @param uniquefiers Optional parameters to separate similar citations. Can be null if not needed.
     * @return The formatted citation.
     */
    private String getAuthorYearInTextMarker(List<BibEntry> entries, BibDatabase database, String authorField,
                                             String yearField, int maxA, String authorSep,
                                             String andString, String etAlString, String yearSep,
                                             String startBrace, String endBrace, String citationSeparator,
                                             String[] uniquefiers, int[] unlimAuthors) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < entries.size(); i++) {

            int unlimA = unlimAuthors == null ? -1 : unlimAuthors[i];
            int maxAuthors = unlimA > 0 ? unlimA : maxA;

            // Check if this entry has been nulled due to grouping with the previous entry(ies):
            if (entries.get(i) == null) {
                continue;
            }

            if (i > 0) {
                sb.append(citationSeparator);
            }
            String author = getCitationMarkerField(entries.get(i), database, authorField);
            if (author != null) {
                AuthorList al = AuthorList.getAuthorList(author);
                if (!al.isEmpty()) {
                    sb.append(getAuthorLastName(al, 0));
                }
                if ((al.size() > 1) && ((al.size() <= maxAuthors) || (maxAuthors < 0))) {
                    int j = 1;
                    while (j < (al.size() - 1)) {
                        sb.append(authorSep);
                        sb.append(getAuthorLastName(al, j));
                        j++;
                    }
                    sb.append(andString);
                    sb.append(getAuthorLastName(al, al.size() - 1));
                } else if (al.size() > maxAuthors) {
                    sb.append(etAlString);
                }
                sb.append(yearSep);
            }
            sb.append(startBrace);
            String year = getCitationMarkerField(entries.get(i), database, yearField);
            if (year != null) {
                sb.append(year);
            }
            if ((uniquefiers != null) && (uniquefiers[i] != null)) {
                sb.append(uniquefiers[i]);
            }
            sb.append(endBrace);
        }
        return sb.toString();

    }

    /**
     * This method looks up a field for en entry in a database. Any number of backup fields can be used
     * if the primary field is empty.
     *
     * @param entry    The entry.
     * @param database The database the entry belongs to.
     * @param field    The field, or succession of fields, to look up. If backup fields are needed, separate
     *                 field names by /. E.g. to use "author" with "editor" as backup, specify "author/editor".
     * @return The resolved field content, or an empty string if the field(s) were empty.
     */
    private String getCitationMarkerField(BibEntry entry, BibDatabase database, String field) {
        String[] fields = field.split("/");
        for (String s : fields) {
            String content = BibDatabase.getResolvedField(s, entry, database);
            if ((content != null) && !content.trim().isEmpty()) {
                if (fieldFormatter != null) {
                    content = fieldFormatter.format(content);
                }
                return content;
            }
        }
        // No luck? Return an empty string:
        return "";
    }

    /**
     * Look up the nth author and return the proper last name for citation markers.
     *
     * @param al     The author list.
     * @param number The number of the author to return.
     * @return The author name, or an empty String if inapplicable.
     */
    private String getAuthorLastName(AuthorList al, int number) {
        StringBuilder sb = new StringBuilder();

        if (al.size() > number) {
            AuthorList.Author a = al.getAuthor(number);
            if ((a.getVon() != null) && !a.getVon().isEmpty()) {
                String von = a.getVon();
                sb.append(von);
                /*sb.append(von.substring(0, 1).toUpperCase());
                if (von.length() > 1)
                    sb.append(von.substring(1));*/
                sb.append(' ');
            }
            sb.append(a.getLast());
        }

        return sb.toString();
    }

    /**
     * Take a finished citation and insert a string at the end (but inside the end bracket)
     * separated by "PageInfoSeparator"
     *
     * @param citation
     * @param pageInfo
     * @return
     */
    public String insertPageInfo(String citation, String pageInfo) {
        String bracketAfter = getStringCitProperty("BracketAfter");
        if (citation.endsWith(bracketAfter)) {
            String first = citation.substring(0, citation.length() - bracketAfter.length());
            return first + getStringCitProperty("PageInfoSeparator") + pageInfo + bracketAfter;
        } else {
            return citation + getStringCitProperty("PageInfoSeparator") + pageInfo;
        }
    }

    /**
     * Convenience method for checking the property for whether we use number citations or
     * author-year citations.
     *
     * @return true if we use numbered citations, false otherwise.
     */
    public boolean isNumberEntries() {
        return (Boolean) getProperty("IsNumberEntries");
    }

    /**
     * Convenience method for checking the property for whether we sort the bibliography
     * according to their order of appearance in the text.
     *
     * @return true to sort by appearance, false to sort alphabetically.
     */
    public boolean isSortByPosition() {
        return (Boolean) getProperty("IsSortByPosition");
    }

    /**
     * Convenience method for checking whether citation markers should be italicised.
     * Will only be relevant if isFormatCitations() returns true.
     *
     * @return true to indicate that citations should be in italics.
     */
    public boolean isItalicCitations() {
        return (Boolean) citProperties.get("ItalicCitations");
    }

    /**
     * Convenience method for checking whether citation markers should be bold.
     * Will only be relevant if isFormatCitations() returns true.
     *
     * @return true to indicate that citations should be in bold.
     */
    public boolean isBoldCitations() {
        return (Boolean) citProperties.get("BoldCitations");
    }

    /**
     * Convenience method for checking whether citation markers formatted
     * according to the results of the isItalicCitations() and
     * isBoldCitations() methods.
     *
     * @return true to indicate that citations should be in italics.
     */
    public boolean isFormatCitations() {
        return (Boolean) citProperties.get("FormatCitations");
    }

    public boolean isBibtexKeyCiteMarkers() {
        return (Boolean) citProperties.get("BibTeXKeyCitations");
    }

    /**
     * Get boolean property.
     *
     * @param key The property key
     * @return the value
     */
    public boolean getBooleanCitProperty(String key) {
        return (Boolean) citProperties.get(key);
    }

    public int getIntCitProperty(String key) {
        return (Integer) citProperties.get(key);
    }

    public String getStringCitProperty(String key) {
        return (String) citProperties.get(key);
    }

    public String getCitationCharacterFormat() {
        return (String) citProperties.get("CitationCharacterFormat");
    }

    /**
     * Get a style property.
     *
     * @param propName The property name.
     * @return The property value, or null if it doesn't exist.
     */
    public Object getProperty(String propName) {
        return properties.get(propName);
    }

    @Override
    public int compareTo(OOBibStyle other) {
        return getName().compareTo(other.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof OOBibStyle) {
            return styleFile.equals(((OOBibStyle) o).styleFile);
        }
        return false;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return super.hashCode();
    }

}
