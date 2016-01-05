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
package net.sf.jabref.openoffice;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.exporter.layout.Layout;

import com.sun.star.beans.XPropertySet;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import net.sf.jabref.logic.l10n.Localization;

/**
 * Utility methods for processing OO Writer documents.
 */
class OOUtil {

    private static final Pattern HTML_TAG = Pattern.compile("</?[a-z]+>");

    static final OOPreFormatter POSTFORMATTER = new OOPreFormatter();

    private static final String UNIQUEFIER_FIELD = "uniq";



    /**
     * Insert a reference, formatted using a Layout, at the position of a given cursor.
     * @param text The text to insert in.
     * @param cursor The cursor giving the insert location.
     * @param layout The Layout to format the reference with.
     * @param parStyle The name of the paragraph style to use.
     * @param entry The entry to insert.
     * @param database The database the entry belongs to.
     * @param uniquefier Uniqiefier letter, if any, to append to the entry's year.
     * @throws Exception
     */
    public static void insertFullReferenceAtCurrentLocation(XText text, XTextCursor cursor,
            Layout layout, String parStyle, BibEntry entry, BibDatabase database, String uniquefier)
            throws Exception {

        // Backup the value of the uniq field, just in case the entry already has it:
        String oldUniqVal = entry.getField(UNIQUEFIER_FIELD);


        // Set the uniq field with the supplied uniquefier:
        if (uniquefier == null) {
            entry.clearField(UNIQUEFIER_FIELD);
        } else {
            entry.setField(UNIQUEFIER_FIELD, uniquefier);
        }

        // Do the layout for this entry:
        String lText = layout.doLayout(entry, database);

        // Afterwards, reset the old value:
        if (oldUniqVal == null) {
            entry.clearField(UNIQUEFIER_FIELD);
        } else {
            entry.setField(UNIQUEFIER_FIELD, oldUniqVal);
        }

        // Insert the formatted text:
        OOUtil.insertOOFormattedTextAtCurrentLocation(text, cursor, lText, parStyle);
    }

    /**
     * Insert a text with formatting indicated by HTML-like tags, into a text at
     * the position given by a cursor.
     * @param text The text to insert in.
     * @param cursor The cursor giving the insert location.
     * @param lText The marked-up text to insert.
     * @param parStyle The name of the paragraph style to use.
     * @throws Exception
     */
    public static void insertOOFormattedTextAtCurrentLocation(XText text, XTextCursor cursor,
            String lText, String parStyle) throws Exception {

        XParagraphCursor parCursor = UnoRuntime.queryInterface(
                XParagraphCursor.class, cursor);
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, parCursor);

        try {
            props.setPropertyValue("ParaStyleName", parStyle);
        } catch (com.sun.star.lang.IllegalArgumentException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }

        // We need to extract formatting. Use a simple regexp search iteration:
        int piv = 0;
        int italic = 0;
        int bold = 0;
        int sup = 0;
        int sub = 0;
        int mono = 0;
        int smallCaps = 0;
        //insertTextAtCurrentLocation(text, cursor, "_",
        //    false, false, false, false, false, false);
        //cursor.goLeft((short)1, true);
        Matcher m = OOUtil.HTML_TAG.matcher(lText);
        while (m.find()) {
            String ss = lText.substring(piv, m.start());
            if (!ss.isEmpty()) {
                OOUtil.insertTextAtCurrentLocation(text, cursor, ss, (bold % 2) > 0, (italic % 2) > 0,
                        mono > 0, smallCaps > 0, sup > 0, sub > 0);
            }
            String tag = m.group();
            // Handle tags:
            if ("<b>".equals(tag)) {
                bold++;
            } else if ("</b>".equals(tag)) {
                bold--;
            } else if ("<i>".equals(tag) || "<em>".equals(tag)) {
                italic++;
            } else if ("</i>".equals(tag) || "</em>".equals(tag)) {
                italic--;
            } else if ("</monospace>".equals(tag)) {
                mono = 0;
            } else if ("<monospace>".equals(tag)) {
                mono = 1;
            } else if ("</smallcaps>".equals(tag)) {
                smallCaps = 0;
            } else if ("<smallcaps>".equals(tag)) {
                smallCaps = 1;
            } else if ("</sup>".equals(tag)) {
                sup = 0;
            } else if ("<sup>".equals(tag)) {
                sup = 1;
            } else if ("</sub>".equals(tag)) {
                sub = 0;
            } else if ("<sub>".equals(tag)) {
                sub = 1;
            }

            piv = m.end();

        }

        if (piv < lText.length()) {
            OOUtil.insertTextAtCurrentLocation(text, cursor, lText.substring(piv),
                    (bold % 2) > 0, (italic % 2) > 0, mono > 0, smallCaps > 0, sup > 0, sub > 0);
        }

        cursor.collapseToEnd();
    }

    public static void insertParagraphBreak(XText text, XTextCursor cursor) throws Exception {
        text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, true);
        cursor.collapseToEnd();
    }

    public static void insertTextAtCurrentLocation(XText text, XTextCursor cursor, String string,
            boolean bold, boolean italic, boolean monospace, boolean smallCaps, boolean superscript,
            boolean subscript) throws Exception {
        text.insertString(cursor, string, true);
        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet xCursorProps = UnoRuntime.queryInterface(
                XPropertySet.class, cursor);
        if (bold) {
            xCursorProps.setPropertyValue("CharWeight",
                    com.sun.star.awt.FontWeight.BOLD);
        } else {
            xCursorProps.setPropertyValue("CharWeight",
                    com.sun.star.awt.FontWeight.NORMAL);
        }

        if (italic) {
            xCursorProps.setPropertyValue("CharPosture",
                    com.sun.star.awt.FontSlant.ITALIC);
        } else {
            xCursorProps.setPropertyValue("CharPosture",
                    com.sun.star.awt.FontSlant.NONE);
        }

        if (smallCaps) {
            xCursorProps.setPropertyValue("CharCaseMap",
                    com.sun.star.style.CaseMap.SMALLCAPS);
        }
        else {
            xCursorProps.setPropertyValue("CharCaseMap",
                    com.sun.star.style.CaseMap.NONE);
        }

        // TODO: the <monospace> tag doesn't work
        /*
        if (monospace) {
            xCursorProps.setPropertyValue("CharFontPitch",
                            com.sun.star.awt.FontPitch.FIXED);
        }
        else {
            xCursorProps.setPropertyValue("CharFontPitch",
                            com.sun.star.awt.FontPitch.VARIABLE);
        } */
        if (subscript) {
            xCursorProps.setPropertyValue("CharEscapement",
                    (byte) -101);
            xCursorProps.setPropertyValue("CharEscapementHeight",
                    (byte) 58);
        }
        else if (superscript) {
            xCursorProps.setPropertyValue("CharEscapement",
                    (byte) 101);
            xCursorProps.setPropertyValue("CharEscapementHeight",
                    (byte) 58);
        }
        else {
            xCursorProps.setPropertyValue("CharEscapement",
                    (byte) 0);
            xCursorProps.setPropertyValue("CharEscapementHeight",
                    (byte) 100);
        }

        cursor.collapseToEnd();

    }

    public static void insertTextAtCurrentLocation(XText text, XTextCursor cursor, String string,
            String parStyle) throws Exception {
        text.insertString(cursor, string, true);
        XParagraphCursor parCursor = UnoRuntime.queryInterface(
                XParagraphCursor.class, cursor);
        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, parCursor);
        try {
            props.setPropertyValue("ParaStyleName", parStyle);
        } catch (com.sun.star.lang.IllegalArgumentException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }
        cursor.collapseToEnd();

    }

    public static Object getProperty(Object o, String property) throws Exception {
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, o);
        return props.getPropertyValue(property);
    }

    public static XTextDocument selectComponent(List<XTextDocument> list) throws Exception {
        String[] values = new String[list.size()];
        int ii = 0;
        for (XTextDocument doc : list) {
            values[ii] = String.valueOf(OOUtil.getProperty(doc.getCurrentController().getFrame(), "Title"));
            ii++;
        }
        JList<String> sel = new JList<>(values);
        sel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sel.setSelectedIndex(0);
        int ans = JOptionPane.showConfirmDialog(null, new JScrollPane(sel), Localization.lang("Select document"),
                JOptionPane.OK_CANCEL_OPTION);
        if (ans == JOptionPane.OK_OPTION) {
            return list.get(sel.getSelectedIndex());
        } else {
            return null;
        }
    }

    /**
     * Make a cloned BibEntry and do the necessary preprocessing for use by the plugin.
     * If the running JabRef version doesn't support post-processing in Layout, this
     * preprocessing includes running the OOPreFormatter formatter for all fields except the
     * BibTeX key.
     * @param entry the original entry
     * @return the cloned and processed entry
     */
    public static BibEntry createAdaptedEntry(BibEntry entry) {
        if (entry == null) {
            return null;
        }
        BibEntry e = (BibEntry) entry.clone();
        for (String field : e.getFieldNames()) {
            if (field.equals(BibEntry.KEY_FIELD)) {
                continue;
            }
            String value = e.getField(field);
            // If the running JabRef version doesn't support post-processing in Layout,
            // preprocess fields instead:
            if (!OpenOfficePanel.postLayoutSupported && (value != null)) {
                e.setField(field, OOUtil.POSTFORMATTER.format(value));
            }
        }
        return e;
    }
}
