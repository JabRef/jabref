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
import java.util.BitSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.Layout;

/**
 * Utility methods for processing OO Writer documents.
 */
class OOUtil {

    private static final String CHAR_STRIKEOUT = "CharStrikeout";
    private static final String CHAR_UNDERLINE = "CharUnderline";
    private static final String PARA_STYLE_NAME = "ParaStyleName";
    private static final String CHAR_CASE_MAP = "CharCaseMap";
    private static final String CHAR_POSTURE = "CharPosture";
    private static final String CHAR_WEIGHT = "CharWeight";
    private static final String CHAR_ESCAPEMENT_HEIGHT = "CharEscapementHeight";
    private static final String CHAR_ESCAPEMENT = "CharEscapement";

    public static final int TOTAL_FORMAT_COUNT = 8;

    private static final int FORMAT_BOLD = 0;
    private static final int FORMAT_ITALIC = 1;
    private static final int FORMAT_SMALLCAPS = 2;
    private static final int FORMAT_SUPERSCRIPT = 3;
    private static final int FORMAT_SUBSCRIPT = 4;
    private static final int FORMAT_UNDERLINE = 5;
    private static final int FORMAT_STRIKEOUT = 6;
    private static final int FORMAT_MONOSPACE = 7;

    private static final Pattern HTML_TAG = Pattern.compile("</?[a-z]+>");

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
                    throws UndefinedParagraphFormatException, UnknownPropertyException, PropertyVetoException,
                    WrappedTargetException, IllegalArgumentException {

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
     * @throws WrappedTargetException
     * @throws PropertyVetoException
     * @throws UnknownPropertyException
     * @throws IllegalArgumentException
     */
    public static void insertOOFormattedTextAtCurrentLocation(XText text, XTextCursor cursor, String lText,
            String parStyle) throws UndefinedParagraphFormatException, UnknownPropertyException, PropertyVetoException,
                    WrappedTargetException, IllegalArgumentException {

        XParagraphCursor parCursor = UnoRuntime.queryInterface(
                XParagraphCursor.class, cursor);
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, parCursor);

        try {
            props.setPropertyValue(PARA_STYLE_NAME, parStyle);
        } catch (IllegalArgumentException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }

        BitSet formatting = new BitSet(TOTAL_FORMAT_COUNT);
        // We need to extract formatting. Use a simple regexp search iteration:
        int piv = 0;
        Matcher m = OOUtil.HTML_TAG.matcher(lText);
        while (m.find()) {
            String ss = lText.substring(piv, m.start());
            if (!ss.isEmpty()) {
                OOUtil.insertTextAtCurrentLocation(text, cursor, ss, formatting);
            }
            String tag = m.group();
            // Handle tags:
            if ("<b>".equals(tag)) {
                formatting.set(FORMAT_BOLD);
            } else if ("</b>".equals(tag)) {
                formatting.clear(FORMAT_BOLD);
            } else if ("<i>".equals(tag) || "<em>".equals(tag)) {
                formatting.set(FORMAT_ITALIC);
            } else if ("</i>".equals(tag) || "</em>".equals(tag)) {
                formatting.clear(FORMAT_ITALIC);
            } else if ("<tt>".equals(tag)) {
                formatting.set(FORMAT_MONOSPACE);
            } else if ("</tt>".equals(tag)) {
                formatting.clear(FORMAT_MONOSPACE);
            } else if ("<smallcaps>".equals(tag)) {
                formatting.set(FORMAT_SMALLCAPS);
            } else if ("</smallcaps>".equals(tag)) {
                formatting.clear(FORMAT_SMALLCAPS);
            } else if ("<sup>".equals(tag)) {
                formatting.set(FORMAT_SUPERSCRIPT);
            } else if ("</sup>".equals(tag)) {
                formatting.clear(FORMAT_SUPERSCRIPT);
            } else if ("<sub>".equals(tag)) {
                formatting.set(FORMAT_SUBSCRIPT);
            } else if ("</sub>".equals(tag)) {
                formatting.clear(FORMAT_SUBSCRIPT);
            } else if ("<u>".equals(tag)) {
                formatting.set(FORMAT_UNDERLINE);
            } else if ("</u>".equals(tag)) {
                formatting.clear(FORMAT_UNDERLINE);
            } else if ("<s>".equals(tag)) {
                formatting.set(FORMAT_STRIKEOUT);
            } else if ("</s>".equals(tag)) {
                formatting.clear(FORMAT_STRIKEOUT);
            }

            piv = m.end();

        }

        if (piv < lText.length()) {
            OOUtil.insertTextAtCurrentLocation(text, cursor, lText.substring(piv), formatting);
        }

        cursor.collapseToEnd();
    }

    public static void insertParagraphBreak(XText text, XTextCursor cursor) throws IllegalArgumentException {
        text.insertControlCharacter(cursor, ControlCharacter.PARAGRAPH_BREAK, true);
        cursor.collapseToEnd();
    }

    public static void insertTextAtCurrentLocation(XText text, XTextCursor cursor, String string, BitSet formatting)
                    throws UnknownPropertyException, PropertyVetoException, WrappedTargetException,
                    IllegalArgumentException {
        text.insertString(cursor, string, true);
        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet xCursorProps = UnoRuntime.queryInterface(
                XPropertySet.class, cursor);
        if (formatting.get(FORMAT_BOLD)) {
            xCursorProps.setPropertyValue(CHAR_WEIGHT,
                    com.sun.star.awt.FontWeight.BOLD);
        } else {
            xCursorProps.setPropertyValue(CHAR_WEIGHT,
                    com.sun.star.awt.FontWeight.NORMAL);
        }

        if (formatting.get(FORMAT_ITALIC)) {
            xCursorProps.setPropertyValue(CHAR_POSTURE,
                    com.sun.star.awt.FontSlant.ITALIC);
        } else {
            xCursorProps.setPropertyValue(CHAR_POSTURE,
                    com.sun.star.awt.FontSlant.NONE);
        }

        if (formatting.get(FORMAT_SMALLCAPS)) {
            xCursorProps.setPropertyValue(CHAR_CASE_MAP,
                    com.sun.star.style.CaseMap.SMALLCAPS);
        }        else {
            xCursorProps.setPropertyValue(CHAR_CASE_MAP,
                    com.sun.star.style.CaseMap.NONE);
        }

        // TODO: the <monospace> tag doesn't work
        /*
        if (formatting.get(FORMAT_MONOSPACE)) {
            xCursorProps.setPropertyValue("CharFontPitch",
                            com.sun.star.awt.FontPitch.FIXED);
        }
        else {
            xCursorProps.setPropertyValue("CharFontPitch",
                            com.sun.star.awt.FontPitch.VARIABLE);
        } */
        if (formatting.get(FORMAT_SUBSCRIPT)) {
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT,
                    (byte) -101);
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT_HEIGHT,
                    (byte) 58);
        } else if (formatting.get(FORMAT_SUPERSCRIPT)) {
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT,
                    (byte) 101);
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT_HEIGHT,
                    (byte) 58);
        } else {
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT,
                    (byte) 0);
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT_HEIGHT,
                    (byte) 100);
        }

        if (formatting.get(FORMAT_UNDERLINE)) {
            xCursorProps.setPropertyValue(CHAR_UNDERLINE, com.sun.star.awt.FontUnderline.SINGLE);
        } else {
            xCursorProps.setPropertyValue(CHAR_UNDERLINE, com.sun.star.awt.FontUnderline.NONE);
        }

        if (formatting.get(FORMAT_STRIKEOUT)) {
            xCursorProps.setPropertyValue(CHAR_STRIKEOUT, com.sun.star.awt.FontStrikeout.SINGLE);
        } else {
            xCursorProps.setPropertyValue(CHAR_STRIKEOUT, com.sun.star.awt.FontStrikeout.NONE);
        }
        cursor.collapseToEnd();

    }

    public static void insertTextAtCurrentLocation(XText text, XTextCursor cursor, String string, String parStyle)
            throws WrappedTargetException, PropertyVetoException, UnknownPropertyException,
            UndefinedParagraphFormatException {
        text.insertString(cursor, string, true);
        XParagraphCursor parCursor = UnoRuntime.queryInterface(
                XParagraphCursor.class, cursor);
        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, parCursor);
        try {
            props.setPropertyValue(PARA_STYLE_NAME, parStyle);
        } catch (IllegalArgumentException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }
        cursor.collapseToEnd();

    }

    public static Object getProperty(Object o, String property)
            throws UnknownPropertyException, WrappedTargetException {
        XPropertySet props = UnoRuntime.queryInterface(
                XPropertySet.class, o);
        return props.getPropertyValue(property);
    }

    public static XTextDocument selectComponent(List<XTextDocument> list)
            throws UnknownPropertyException, WrappedTargetException, IndexOutOfBoundsException {
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
}
