package org.jabref.gui;

import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import org.jabref.Globals;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * Static variables for graphics files and keyboard shortcuts.
 */
public class GUIGlobals {

    public static Color editorTextColor;
    public static Color validFieldBackgroundColor;
    public static Color activeBackgroundColor;
    public static Color invalidFieldBackgroundColor;
    public static Font currentFont;

    public static CustomLocalDragboard localDragboard = new CustomLocalDragboard();

    public static final String UNTITLED_TITLE = Localization.lang("untitled");

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
        // Set up entry editor colors, first time:
        GUIGlobals.updateEntryEditorColors();

        IconTheme.loadFonts();
        GUIGlobals.currentFont = new Font(Globals.prefs.getFontFamily(), Globals.prefs.getDouble(JabRefPreferences.FONT_SIZE));
    }

    public static void setFont(double size) {
        currentFont = new Font(currentFont.getFamily(), size);
        // update preferences
        Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, size);
    }
}
