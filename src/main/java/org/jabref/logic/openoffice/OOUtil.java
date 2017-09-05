package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.layout.Layout;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.ControlCharacter;
import com.sun.star.text.XParagraphCursor;
import com.sun.star.text.XText;
import com.sun.star.text.XTextCursor;
import com.sun.star.uno.UnoRuntime;

/**
 * Utility methods for processing OO Writer documents.
 */
public class OOUtil {

    private static final String CHAR_STRIKEOUT = "CharStrikeout";
    private static final String CHAR_UNDERLINE = "CharUnderline";
    private static final String PARA_STYLE_NAME = "ParaStyleName";
    private static final String CHAR_CASE_MAP = "CharCaseMap";
    private static final String CHAR_POSTURE = "CharPosture";
    private static final String CHAR_WEIGHT = "CharWeight";
    private static final String CHAR_ESCAPEMENT_HEIGHT = "CharEscapementHeight";
    private static final String CHAR_ESCAPEMENT = "CharEscapement";

    public enum Formatting {
        BOLD,
        ITALIC,
        SMALLCAPS,
        SUPERSCRIPT,
        SUBSCRIPT,
        UNDERLINE,
        STRIKEOUT,
        MONOSPACE
    }

    private static final Pattern HTML_TAG = Pattern.compile("</?[a-z]+>");

    private static final String UNIQUEFIER_FIELD = "uniq";

    private OOUtil() {
        // Just to hide the public constructor
    }

    /**
     * Insert a reference, formatted using a Layout, at the position of a given cursor.
     * @param text The text to insert in.
     * @param cursor The cursor giving the insert location.
     * @param layout The Layout to format the reference with.
     * @param parStyle The name of the paragraph style to use.
     * @param entry The entry to insert.
     * @param database The database the entry belongs to.
     * @param uniquefier Uniqiefier letter, if any, to append to the entry's year.
     */
    public static void insertFullReferenceAtCurrentLocation(XText text, XTextCursor cursor,
            Layout layout, String parStyle, BibEntry entry, BibDatabase database, String uniquefier)
                    throws UndefinedParagraphFormatException, UnknownPropertyException, PropertyVetoException,
                    WrappedTargetException, IllegalArgumentException {

        // Backup the value of the uniq field, just in case the entry already has it:
        Optional<String> oldUniqVal = entry.getField(UNIQUEFIER_FIELD);

        // Set the uniq field with the supplied uniquefier:
        if (uniquefier == null) {
            entry.clearField(UNIQUEFIER_FIELD);
        } else {
            entry.setField(UNIQUEFIER_FIELD, uniquefier);
        }

        // Do the layout for this entry:
        String formattedText = layout.doLayout(entry, database);

        // Afterwards, reset the old value:
        if (oldUniqVal.isPresent()) {
            entry.setField(UNIQUEFIER_FIELD, oldUniqVal.get());
        } else {
            entry.clearField(UNIQUEFIER_FIELD);
        }

        // Insert the formatted text:
        OOUtil.insertOOFormattedTextAtCurrentLocation(text, cursor, formattedText, parStyle);
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

        List<Formatting> formatting = new ArrayList<>();
        // We need to extract formatting. Use a simple regexp search iteration:
        int piv = 0;
        Matcher m = OOUtil.HTML_TAG.matcher(lText);
        while (m.find()) {
            String currentSubstring = lText.substring(piv, m.start());
            if (!currentSubstring.isEmpty()) {
                OOUtil.insertTextAtCurrentLocation(text, cursor, currentSubstring, formatting);
            }
            String tag = m.group();
            // Handle tags:
            if ("<b>".equals(tag)) {
                formatting.add(Formatting.BOLD);
            } else if ("</b>".equals(tag)) {
                formatting.remove(Formatting.BOLD);
            } else if ("<i>".equals(tag) || "<em>".equals(tag)) {
                formatting.add(Formatting.ITALIC);
            } else if ("</i>".equals(tag) || "</em>".equals(tag)) {
                formatting.remove(Formatting.ITALIC);
            } else if ("<tt>".equals(tag)) {
                formatting.add(Formatting.MONOSPACE);
            } else if ("</tt>".equals(tag)) {
                formatting.remove(Formatting.MONOSPACE);
            } else if ("<smallcaps>".equals(tag)) {
                formatting.add(Formatting.SMALLCAPS);
            } else if ("</smallcaps>".equals(tag)) {
                formatting.remove(Formatting.SMALLCAPS);
            } else if ("<sup>".equals(tag)) {
                formatting.add(Formatting.SUPERSCRIPT);
            } else if ("</sup>".equals(tag)) {
                formatting.remove(Formatting.SUPERSCRIPT);
            } else if ("<sub>".equals(tag)) {
                formatting.add(Formatting.SUBSCRIPT);
            } else if ("</sub>".equals(tag)) {
                formatting.remove(Formatting.SUBSCRIPT);
            } else if ("<u>".equals(tag)) {
                formatting.add(Formatting.UNDERLINE);
            } else if ("</u>".equals(tag)) {
                formatting.remove(Formatting.UNDERLINE);
            } else if ("<s>".equals(tag)) {
                formatting.add(Formatting.STRIKEOUT);
            } else if ("</s>".equals(tag)) {
                formatting.remove(Formatting.STRIKEOUT);
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

    public static void insertTextAtCurrentLocation(XText text, XTextCursor cursor, String string,
            List<Formatting> formatting)
                    throws UnknownPropertyException, PropertyVetoException, WrappedTargetException,
                    IllegalArgumentException {
        text.insertString(cursor, string, true);
        // Access the property set of the cursor, and set the currently selected text
        // (which is the string we just inserted) to be bold
        XPropertySet xCursorProps = UnoRuntime.queryInterface(
                XPropertySet.class, cursor);
        if (formatting.contains(Formatting.BOLD)) {
            xCursorProps.setPropertyValue(CHAR_WEIGHT,
                    com.sun.star.awt.FontWeight.BOLD);
        } else {
            xCursorProps.setPropertyValue(CHAR_WEIGHT,
                    com.sun.star.awt.FontWeight.NORMAL);
        }

        if (formatting.contains(Formatting.ITALIC)) {
            xCursorProps.setPropertyValue(CHAR_POSTURE,
                    com.sun.star.awt.FontSlant.ITALIC);
        } else {
            xCursorProps.setPropertyValue(CHAR_POSTURE,
                    com.sun.star.awt.FontSlant.NONE);
        }

        if (formatting.contains(Formatting.SMALLCAPS)) {
            xCursorProps.setPropertyValue(CHAR_CASE_MAP,
                    com.sun.star.style.CaseMap.SMALLCAPS);
        }        else {
            xCursorProps.setPropertyValue(CHAR_CASE_MAP,
                    com.sun.star.style.CaseMap.NONE);
        }

        // TODO: the <monospace> tag doesn't work
        /*
        if (formatting.contains(Formatting.MONOSPACE)) {
            xCursorProps.setPropertyValue("CharFontPitch",
                            com.sun.star.awt.FontPitch.FIXED);
        }
        else {
            xCursorProps.setPropertyValue("CharFontPitch",
                            com.sun.star.awt.FontPitch.VARIABLE);
        } */
        if (formatting.contains(Formatting.SUBSCRIPT)) {
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT,
                    (byte) -101);
            xCursorProps.setPropertyValue(CHAR_ESCAPEMENT_HEIGHT,
                    (byte) 58);
        } else if (formatting.contains(Formatting.SUPERSCRIPT)) {
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

        if (formatting.contains(Formatting.UNDERLINE)) {
            xCursorProps.setPropertyValue(CHAR_UNDERLINE, com.sun.star.awt.FontUnderline.SINGLE);
        } else {
            xCursorProps.setPropertyValue(CHAR_UNDERLINE, com.sun.star.awt.FontUnderline.NONE);
        }

        if (formatting.contains(Formatting.STRIKEOUT)) {
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
}
