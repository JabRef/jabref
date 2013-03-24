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

import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.Property;
import com.sun.star.text.*;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.frame.XDesktop;
import net.sf.jabref.BibtexDatabase;
import net.sf.jabref.BibtexEntry;
import net.sf.jabref.Globals;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.export.layout.Layout;

import javax.swing.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.List;
import java.util.Iterator;

/**
 * Utility methods for processing OO Writer documents.
 */
public class OOUtil {

    
    static Pattern htmlTag = Pattern.compile("</?[a-z]+>");

    static OOPreFormatter postformatter = new OOPreFormatter();

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
            Layout layout, String parStyle, BibtexEntry entry, BibtexDatabase database, String uniquefier)
            throws UndefinedParagraphFormatException, Exception {

        final String UNIQUEFIER_FIELD = "uniq";

        // Backup the value of the uniq field, just in case the entry already has it:
        String oldUniqVal = (String)entry.getField(UNIQUEFIER_FIELD);

        // Set the uniq field with the supplied uniquefier:
        entry.setField(UNIQUEFIER_FIELD, uniquefier);

        // Do the layout for this entry:
        String lText = layout.doLayout(entry, database);

        // Afterwards, reset the old value:
        entry.setField(UNIQUEFIER_FIELD, oldUniqVal);

        // Insert the formatted text:
        insertOOFormattedTextAtCurrentLocation(text, cursor, lText, parStyle);
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
              String lText, String parStyle) throws UndefinedParagraphFormatException, Exception {

        XParagraphCursor parCursor = (XParagraphCursor)UnoRuntime.queryInterface(
            XParagraphCursor.class, cursor);
        XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
            XPropertySet.class, parCursor);

        try {
            props.setPropertyValue("ParaStyleName", parStyle);
        } catch (com.sun.star.lang.IllegalArgumentException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }
        
        // We need to extract formatting. Use a simple regexp search iteration:
        int piv = 0;
        int italic = 0, bold = 0, sup = 0, sub = 0, mono = 0, smallCaps = 0;
        //insertTextAtCurrentLocation(text, cursor, "_",
        //    false, false, false, false, false, false);
        //cursor.goLeft((short)1, true);
        Matcher m = htmlTag.matcher(lText);
        while (m.find()) {
            String ss = lText.substring(piv, m.start());
            if (ss.length() > 0) {
                insertTextAtCurrentLocation(text, cursor, ss, (bold % 2) > 0, (italic % 2) > 0,
                        mono > 0, smallCaps > 0, sup > 0, sub > 0);
            }
            String tag = m.group();
            // Handle tags:
            if (tag.equals("<b>"))
                bold++;
            else if (tag.equals("</b>"))
                bold--;
            else if (tag.equals("<i>") || tag.equals("<em>"))
                italic++;
            else if (tag.equals("</i>") || tag.equals("</em>"))
                italic--;
            else if (tag.equals("</monospace>"))
                mono = 0;
            else if (tag.equals("<monospace>"))
                mono = 1;
            else if (tag.equals("</smallcaps>"))
                smallCaps = 0;
            else if (tag.equals("<smallcaps>"))
                smallCaps = 1;
            else if (tag.equals("</sup>"))
                sup = 0;
            else if (tag.equals("<sup>"))
                sup = 1;
            else if (tag.equals("</sub>"))
                sub = 0;
            else if (tag.equals("<sub>"))
                sub = 1;

            piv = m.end();
            
        }

        if (piv < lText.length())
            insertTextAtCurrentLocation(text, cursor,lText.substring(piv),
                    (bold % 2) > 0, (italic % 2) > 0, mono > 0, smallCaps > 0, sup > 0, sub > 0);



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
        XPropertySet xCursorProps = (XPropertySet) UnoRuntime.queryInterface(
            XPropertySet.class, cursor);
        if (bold)
            xCursorProps.setPropertyValue("CharWeight",
                    new Float(com.sun.star.awt.FontWeight.BOLD));
        else
            xCursorProps.setPropertyValue("CharWeight",
                    new Float(com.sun.star.awt.FontWeight.NORMAL));

        if (italic)
            xCursorProps.setPropertyValue("CharPosture",
                            com.sun.star.awt.FontSlant.ITALIC);
        else
            xCursorProps.setPropertyValue("CharPosture",
                            com.sun.star.awt.FontSlant.NONE);

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
                    (byte)-101);
            xCursorProps.setPropertyValue("CharEscapementHeight",
                    (byte)58);
        }
        else if (superscript) {
            xCursorProps.setPropertyValue("CharEscapement",
                    (byte)101);
            xCursorProps.setPropertyValue("CharEscapementHeight",
                    (byte)58);
        }
        else {
            xCursorProps.setPropertyValue("CharEscapement",
                    (byte)0);
            xCursorProps.setPropertyValue("CharEscapementHeight",
                    (byte)100);
        }

        cursor.collapseToEnd();

    }

    public static void insertTextAtCurrentLocation(XText text, XTextCursor cursor, String string,
                                                   String parStyle) throws Exception {
        text.insertString(cursor, string, true);
        XParagraphCursor parCursor = (XParagraphCursor)UnoRuntime.queryInterface(
            XParagraphCursor.class, cursor);
        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
            XPropertySet.class, parCursor);
        try {
            props.setPropertyValue("ParaStyleName", parStyle);
        } catch (com.sun.star.lang.IllegalArgumentException ex) {
            throw new UndefinedParagraphFormatException(parStyle);
        }
        cursor.collapseToEnd();

    }



    public static Object getProperty(Object o, String property) throws Exception {
        XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
                XPropertySet.class, o);
        return props.getPropertyValue(property);
    }

    public static void listProperties(Object o) throws Exception {
        XPropertySet props = (XPropertySet) UnoRuntime.queryInterface(
                XPropertySet.class, o);
        Property[] pr = props.getPropertySetInfo().getProperties();
        for (int i = 0; i < pr.length; i++) {
            Property property1 = pr[i];
            System.out.println(property1.Name+" : "+props.getPropertyValue(property1.Name));
        }
    }

    public static XTextDocument selectComponent(JFrame parent, XDesktop xDesktop, List<XTextDocument> list) throws Exception {
        String[] values = new String[list.size()];
        int ii=0;
        for (Iterator<XTextDocument> iterator = list.iterator(); iterator.hasNext();) {
            XTextDocument doc = iterator.next();
            values[ii++] = String.valueOf(getProperty(doc.getCurrentController().getFrame(), "Title"));
        }
        JList sel = new JList(values);
        sel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sel.setSelectedIndex(0);
        int ans = JOptionPane.showConfirmDialog(parent, new JScrollPane(sel), Globals.lang("Select document"),
                JOptionPane.OK_CANCEL_OPTION);
        if (ans == JOptionPane.OK_OPTION) {
            return list.get(sel.getSelectedIndex());
        }
        else return null;
    }

    /**
     * Make a cloned BibtexEntry and do the necessary preprocessing for use by the plugin.
     * If the running JabRef version doesn't support post-processing in Layout, this
     * preprocessing includes running the OOPreFormatter formatter for all fields except the
     * BibTeX key.
     * @param entry the original entry
     * @return the cloned and processed entry
     */
    public static BibtexEntry createAdaptedEntry(BibtexEntry entry) {
        if (entry == null)
            return null;
        BibtexEntry e = (BibtexEntry)entry.clone();
        for (String field : e.getAllFields()) {
            if (field.equals(BibtexFields.KEY_FIELD))
                continue;
            String value = e.getField(field);
            // If the running JabRef version doesn't support post-processing in Layout,
            // preprocess fields instead:
            if (!OpenOfficePanel.postLayoutSupported && (value != null))
                e.setField(field, postformatter.format(value));
        }
        return e;
    }
}
