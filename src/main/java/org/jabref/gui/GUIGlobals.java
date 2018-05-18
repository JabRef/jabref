package org.jabref.gui;

import java.awt.Color;
import java.awt.Font;

import org.jabref.Globals;
import org.jabref.gui.keyboard.EmacsKeyBindings;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Static variables for graphics files and keyboard shortcuts.
 */
public class GUIGlobals {
    public static Color editorTextColor;
    public static Color validFieldBackgroundColor;
    public static Color activeBackgroundColor;
    public static Color invalidFieldBackgroundColor;
    public static Font currentFont;
    public static final Color NULL_FIELD_COLOR = new Color(75, 130, 95); // Valid field, green.

    public static final Color ACTIVE_EDITOR_COLOR = new Color(230, 230, 255);
    public static final int WIDTH_ICON_COL = JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL) + 12; // add some additional space to improve appearance

    public static final int WIDTH_ICON_COL_RANKING = 5 * JabRefPreferences.getInstance().getInt(JabRefPreferences.ICON_SIZE_SMALL); // Width of Ranking Icon Column

    public static final String UNTITLED_TITLE = Localization.lang("untitled");

    //	Colors.
    public static final Color ENTRY_EDITOR_LABEL_COLOR = new Color(100, 100, 150); // Empty field, blue.

    private static final Logger LOGGER = LoggerFactory.getLogger(GUIGlobals.class);

    private GUIGlobals() {
    }

    public static void updateEntryEditorColors() {
        GUIGlobals.activeBackgroundColor = JabRefPreferences.getInstance().getColor(JabRefPreferences.ACTIVE_FIELD_EDITOR_BACKGROUND_COLOR);
        GUIGlobals.validFieldBackgroundColor = JabRefPreferences.getInstance().getColor(JabRefPreferences.VALID_FIELD_BACKGROUND_COLOR);
        GUIGlobals.invalidFieldBackgroundColor = JabRefPreferences.getInstance().getColor(JabRefPreferences.INVALID_FIELD_BACKGROUND_COLOR);
        GUIGlobals.editorTextColor = JabRefPreferences.getInstance().getColor(JabRefPreferences.FIELD_EDITOR_TEXT_COLOR);
    }

    /**
     * Perform initializations that are only used in graphical mode. This is to prevent
     * the "Xlib: connection to ":0.0" refused by server" error when access to the X server
     * on Un*x is unavailable.
     */
    public static void init() {
        if (Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS)) {
            EmacsKeyBindings.load();
        }

        // Set up entry editor colors, first time:
        GUIGlobals.updateEntryEditorColors();

        GUIGlobals.currentFont = new Font(Globals.prefs.get(JabRefPreferences.FONT_FAMILY),
                Globals.prefs.getInt(JabRefPreferences.FONT_STYLE), Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));
    }

    public static void setFont(int size) {
        currentFont = new Font(currentFont.getFamily(), currentFont.getStyle(), size);
        // update preferences
        Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, size);
    }

}
