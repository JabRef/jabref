/*  Copyright (C) 2003-2012 JabRef contributors.
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
package net.sf.jabref.gui;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import net.sf.jabref.AuthorList;
import net.sf.jabref.BasePanel;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefPreferences;
import net.sf.jabref.SearchRuleSet;
import net.sf.jabref.Util;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.SpecialFieldValue;
import net.sf.jabref.specialfields.SpecialFieldsUtils;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.matchers.Matcher;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

/**
 * Class defining the contents and column headers of the main table.
 */
public class MainTableFormat implements TableFormat<BibtexEntry> {

    // Character separating field names that are to be used in sequence as
    // fallbacks for a single column (e.g. "author/editor" to use editor where
    // author is not set):
    public static final String COL_DEFINITION_FIELD_SEPARATOR = "/";

    public static final String ICON_COLUMN_PREFIX = "iconcol:";

    // Values to gather iconImages for those columns
    // These values are also used to put a heading into the table; see getColumnName(int)
    public static final String[]
    PDF = {"pdf", "ps"},
    URL_FIRST = {"url", "doi"},
    DOI_FIRST = {"doi", "url"},
    CITESEER = {"citeseerurl"},
    ARXIV = {"eprint"},
    RANKING = {SpecialFieldsUtils.FIELDNAME_RANKING},
    PRIORITY = {SpecialFieldsUtils.FIELDNAME_PRIORITY},
    RELEVANCE = {SpecialFieldsUtils.FIELDNAME_RELEVANCE},
    QUALITY = {SpecialFieldsUtils.FIELDNAME_QUALITY},
    PRINTED = {SpecialFieldsUtils.FIELDNAME_PRINTED},
    READ = {SpecialFieldsUtils.FIELDNAME_READ},
    FILE = {GUIGlobals.FILE_FIELD};

    BasePanel panel;

    private String[][] columns; // Contains the current column names.
    public int padleft = -1; // padleft indicates how many columns (starting from left) are
    // special columns (number column or icon column).
    private HashMap<Integer, String[]> iconCols = new HashMap<Integer, String[]>();
    int[][] nameCols = null;
    boolean namesAsIs, abbr_names, namesNatbib, namesFf, namesLf, namesLastOnly, showShort;

    public MainTableFormat(BasePanel panel) {
        this.panel = panel;
    }

    public int getColumnCount() {
        return padleft + columns.length;
    }

    /**
     * @return the string that should be put in the column header
     */
    public String getColumnName(int col) {
        if (col == 0) {
            return GUIGlobals.NUMBER_COL;
        } else if (getIconTypeForColumn(col) != null) {
            if (JabRef.jrf.prefs().getBoolean(JabRefPreferences.SHOWONELETTERHEADINGFORICONCOLUMNS)) {
                return getIconTypeForColumn(col)[0].substring(0,1).toUpperCase();
            } else {
            	return null;
            }
        }
        else // try to find an alternative fieldname (for display)
        {
            String[] fld = columns[col - padleft];
            StringBuilder sb = new StringBuilder();
            for (int i=0; i<fld.length; i++) {
                if (i > 0)
                    sb.append('/');
                String disName = BibtexFields.getFieldDisplayName(fld[i]);
                if (disName != null)
                    sb.append(disName);
                else
                    sb.append(Util.nCase(fld[i]));
            }
            return sb.toString();
          /*String disName = BibtexFields.getFieldDisplayName(columns[col - padleft]) ;
          if ( disName != null)
          {
            return disName ;
          } */
        }
        //return Util.nCase(columns[col - padleft]);
    }

    /**
     * Get the column title, or a string identifying the column if it is an icon
     * column without a title.
     * @param col The column number
     * @return the String identifying the column
     */
    public String getColumnType(int col) {
        String name = getColumnName(col);
        if (name != null)
            return name;
        String[] icon = getIconTypeForColumn(col);
        if ((icon != null) && (icon.length > 0)) {
            return ICON_COLUMN_PREFIX+icon[0];
        }
        return null;
    }

    /**
     * This method returns a string array indicating the types of icons to be displayed in the given column.
     * It returns null if the column is not an icon column, and thereby also serves to identify icon
     * columns.
     */
    public String[] getIconTypeForColumn(int col) {
        Object o = iconCols.get(new Integer(col));
        if (o != null)
            return (String[]) o;
        else
            return null;
    }

    /**
     * Finds the column index for the given column name.
     * @param colName The column name
     * @return The column index if any, or -1 if no column has that name.
     */
    public int getColumnIndex(String colName) {
        for (int i=0; i<columns.length; i++) {
            // TODO: is the following line correct with [0] ?
            if (columns[i][0].equalsIgnoreCase(colName))
                return i+padleft;
        }
        return -1;
    }

    /**
     * Checks, if the Column (int col) is a Ranking-Column
     * @param col Column Number
     * @return Is Ranking-Column or not?
     */
    public boolean isRankingColumn(int col) {
        if (iconCols.get(col) != null) {
            if (iconCols.get(col)[0].equals(RANKING[0])){
                return true;
            }
        }
        return false;
    }

    private Object modifyIconForMultipleLinks(JLabel label) {
        Icon icon = label.getIcon();
        BufferedImage bufImg = new BufferedImage(
            icon.getIconWidth(),
            icon.getIconHeight(),
            BufferedImage.TYPE_INT_ARGB);
        Graphics g = bufImg.createGraphics();
        // paint the Icon to the BufferedImage.
        icon.paintIcon(null, g, 0,0);
        // add the letter "m" in the bottom right corner
        g.setColor(Color.BLACK);
        g.setFont(new java.awt.Font("Serif", java.awt.Font.PLAIN, 12));
        g.drawString("m",bufImg.getWidth() - g.getFontMetrics().stringWidth("m"),bufImg.getHeight());
        g.dispose();
        return new JLabel(new ImageIcon(bufImg));
    }
    
    public Object getColumnValue(BibtexEntry be, int col) {
        Object o = null;
        String[] iconType = getIconTypeForColumn(col); // If non-null, indicates an icon column's type.

        if (col == 0) {
            o = "#";// + (row + 1);
        }

        else if (iconType != null) {
            int hasField = -1;

            int[] fieldCount = hasField(be,iconType);
            hasField=fieldCount[0];

            if (hasField < 0)
                return null;

            // Ok, so we are going to display an icon. Find out which one, and return it:
            if (iconType[hasField].equals(GUIGlobals.FILE_FIELD)) {
                o = FileListTableModel.getFirstLabel(be.getField(GUIGlobals.FILE_FIELD));

            if(fieldCount[1]>1) {
                o = modifyIconForMultipleLinks((JLabel)o);
            }

            // Handle priority column special
            // Extra handling because the icon depends on a FieldValue
            } else if (iconType[hasField].equals(PRIORITY[0])) {
                SpecialFieldValue prio = Priority.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_PRIORITY));
                if (prio != null) {
                    // prio might be null if fieldvalue is an invalid value, therefore we check for != null
                    o = prio.createLabel();
                }
            // Handle ranking column special
            // Extra handling because the icon depends on a FieldValue
            } else if (iconType[hasField].equals(RANKING[0])) {
                SpecialFieldValue rank = Rank.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_RANKING));
                if (rank != null) {
                    o = rank.createLabel();
                }
            // Handle read status column special
            // Extra handling because the icon depends on a FieldValue
            } else if (iconType[hasField].equals(READ[0])) {
                SpecialFieldValue status = ReadStatus.getInstance().parse(be.getField(SpecialFieldsUtils.FIELDNAME_READ));
                if (status != null) {
                    o = status.createLabel();
                }
            } else {
                o = GUIGlobals.getTableIcon(iconType[hasField]);

                if(fieldCount[1]>1) {
                    o = modifyIconForMultipleLinks((JLabel)o);
                }
            }
        } else {
            String[] fld = columns[col - padleft];
            // Go through the fields until we find one with content:
            int j = 0;
            for (int i = 0; i < fld.length; i++) {
                if (fld[i].equals(GUIGlobals.TYPE_HEADER))
                    o = be.getType().getName();
                else {
                    o = be.getFieldOrAlias(fld[i]);
                    if (getColumnName(col).equals("Author") && o != null) {
                        o = panel.database().resolveForStrings((String) o);
                    }
                }
                if (o != null) {
                    j = i;
                    break;
                }
            }

            for (int[] nameCol : nameCols) {
                if ((col - padleft == nameCol[0]) && (nameCol[1] == j)) {
                    return formatName(o);
                }
            }


        }

        return o;
    }

    /**
     * Format a name field for the table, according to user preferences.
     * @param o The contents of the name field.
     * @return The formatted name field.
     */
    public Object formatName(Object o) {
        if (o == null) {
            return null;
        }
        if (namesAsIs) return o;
        if (namesNatbib) o = AuthorList.fixAuthor_Natbib((String) o);
        else if (namesLastOnly) o = AuthorList.fixAuthor_lastNameOnlyCommas((String) o, false);
        else if (namesFf) o = AuthorList.fixAuthor_firstNameFirstCommas((String) o, abbr_names, false);
        else if (namesLf) o = AuthorList.fixAuthor_lastNameFirstCommas((String) o, abbr_names, false);
        return o;
    }

    public boolean hasField(BibtexEntry be, String field) {
        // Returns true iff the entry has a nonzero value in its
        // 'search' field.
        return ((be != null) && (be.getFieldOrAlias(field) != null));
    }

    public int[] hasField(BibtexEntry be, String[] field) {
        // If the entry has a nonzero value in any of the
        // 'search' fields, returns the smallest index for which it does. 
        // Otherwise returns -1. When field indicates one or more file types,
        // returns the index of the first present file type.
        if(be==null||field==null||field.length<1) {
            return new int[] {-1,-1};
        }
        int hasField=-1;
        if(!field[0].equals(GUIGlobals.FILE_FIELD)) {
            for (int i = field.length - 1; i >= 0; i--) {
                if (hasField(be, field[i])) {
                    hasField = i;
                }
            }
            return new int[] {hasField,-1};
        }
        else {
            // We use a FileListTableModel to parse the field content:
            Object o = be.getField(GUIGlobals.FILE_FIELD);
            FileListTableModel fileList = new FileListTableModel();
            fileList.setContent((String)o);
            if(field.length==1) {
                if(fileList.getRowCount()==0) {
                    return new int[] {-1,-1};
                }
                else {
                    return new int[] {0,fileList.getRowCount()};
                }
            }
            int lastLinkPosition=-1, countLinks = 0;
            for (int i = 1; i < field.length; i++) {
                // Count the number of links of correct type.
                for (int j=0; j<fileList.getRowCount(); j++) {
                    FileListEntry flEntry = fileList.getEntry(j);
                    if(flEntry.getType().toString().equals(field[i])) {
                        lastLinkPosition=i;
                        countLinks++;
                    }
                }
            }
            return new int[] {lastLinkPosition,countLinks};
        }
    }

    public void updateTableFormat() {

        // Read table columns from prefs:
        String[] colSettings = Globals.prefs.getStringArray("columnNames");
        columns = new String[colSettings.length][];
        for (int i=0; i<colSettings.length; i++) {
            String[] fields = colSettings[i].split(COL_DEFINITION_FIELD_SEPARATOR);
            columns[i] = new String[fields.length];
            System.arraycopy(fields, 0, columns[i], 0, fields.length);
        }

        // Read name format options:
        showShort = Globals.prefs.getBoolean("showShort");        //MK:
        namesNatbib = Globals.prefs.getBoolean("namesNatbib");    //MK:
        namesLastOnly = Globals.prefs.getBoolean("namesLastOnly");
        namesAsIs = Globals.prefs.getBoolean("namesAsIs");
        abbr_names = Globals.prefs.getBoolean("abbrAuthorNames"); //MK:
        namesFf = Globals.prefs.getBoolean("namesFf");
        namesLf = !(namesAsIs || namesFf || namesNatbib || namesLastOnly); // None of the above.

        // Set the icon columns, indicating the number of special columns to the left.
        // We add those that are enabled in preferences.
        iconCols.clear();
        int coln = 1;

        // Add special Icon Columns
        if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SPECIALFIELDSENABLED)) {
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RANKING))
                iconCols.put(coln++, RANKING);
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_RELEVANCE))
                iconCols.put(coln++, RELEVANCE);
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_QUALITY))
                iconCols.put(coln++, QUALITY);
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRIORITY))
                iconCols.put(coln++, PRIORITY);
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_PRINTED))
                iconCols.put(coln++, PRINTED);
            if (Globals.prefs.getBoolean(SpecialFieldsUtils.PREF_SHOWCOLUMN_READ))
                iconCols.put(coln++, READ);
        }

        if (Globals.prefs.getBoolean("fileColumn"))
            iconCols.put(coln++, FILE);
        if (Globals.prefs.getBoolean("pdfColumn"))
            iconCols.put(coln++, PDF);
        if (Globals.prefs.getBoolean("urlColumn")) {
            if(Globals.prefs.getBoolean("preferUrlDoi")) {
                iconCols.put(coln++, DOI_FIRST);
            } else {
                iconCols.put(coln++, URL_FIRST);
            }
           
        }
            
        if (Globals.prefs.getBoolean("arxivColumn"))
            iconCols.put(coln++, ARXIV);

        if (Globals.prefs.getBoolean("extraFileColumns")) {
            String[] desiredColumns = Globals.prefs.getStringArray("listOfFileColumns");
            for (String desiredColumn : desiredColumns) {
                iconCols.put(coln++, new String[]{GUIGlobals.FILE_FIELD, desiredColumn});
            }
        }

        // Add 1 to the number of icon columns to get padleft.
        padleft = 1 + iconCols.size();

        // Set up the int[][] nameCols, to mark which columns should be
        // treated as lists of names. This is to provide a correct presentation
        // of names as efficiently as possible.
        // Each subarray contains the column number (before padding) and the
        // subfield number in case a column has fallback fields.
        Vector<int[]> tmp = new Vector<int[]>(2, 1);
        for (int i = 0; i < columns.length; i++) {
            for (int j = 0; j < columns[i].length; j++) {
                if (columns[i][j].equals("author")
                    || columns[i][j].equals("editor")) {
                    tmp.add(new int[] {i, j});
                }
            }
        }
        nameCols = new int[tmp.size()][];
        for (int i = 0; i < nameCols.length; i++) {
            nameCols[i] = tmp.elementAt(i);
        }
    }

    public boolean isIconColumn(int col) {
        return (getIconTypeForColumn(col) != null);
    }



    static class NoSearchMatcher implements Matcher<BibtexEntry> {
        public boolean matches(BibtexEntry object) {
            return true;
        }
    }

    static class SearchMatcher implements Matcher<BibtexEntry> {
        private SearchRuleSet ruleSet;
        private Hashtable<String, String> searchOptions;

        public SearchMatcher(SearchRuleSet ruleSet, Hashtable<String, String> searchOptions) {
            this.ruleSet = ruleSet;
            this.searchOptions = searchOptions;
        }
        public boolean matches(BibtexEntry entry) {
            int result = ruleSet.applyRule(searchOptions, entry);
            return result > 0;
        }
    }
}
