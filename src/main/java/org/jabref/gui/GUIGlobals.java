package org.jabref.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Toolkit;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;

import org.jabref.Globals;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.keyboard.EmacsKeyBindings;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;
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
    public static final int MAX_BACK_HISTORY_SIZE = 10; // The maximum number of "Back" operations stored.

    //	Colors.
    public static final Color ENTRY_EDITOR_LABEL_COLOR = new Color(100, 100, 150); // Empty field, blue.

    static final Color INACTIVE_TABBED_COLOR = Color.black; // inactive Database
    private static final Logger LOGGER = LoggerFactory.getLogger(GUIGlobals.class);
    private static final Map<String, JLabel> TABLE_ICONS = new HashMap<>(); // Contains table icon mappings. Set up
    static final Color ACTIVE_TABBED_COLOR = ENTRY_EDITOR_LABEL_COLOR.darker(); // active Database (JTabbedPane)

    private GUIGlobals() {
    }

    public static JLabel getTableIcon(String fieldType) {
        JLabel label = GUIGlobals.TABLE_ICONS.get(fieldType);
        if (label == null) {
            LOGGER.info("Error: no table icon defined for type '" + fieldType + "'.");
            return null;
        } else {
            return label;
        }
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
        JLabel label;
        label = new JLabel(IconTheme.JabRefIcon.PDF_FILE.getSmallIcon());
        label.setToolTipText(Localization.lang("Open") + " PDF");
        GUIGlobals.TABLE_ICONS.put(FieldName.PDF, label);

        label = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());
        label.setToolTipText(Localization.lang("Open") + " URL");
        GUIGlobals.TABLE_ICONS.put(FieldName.URL, label);

        label = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());
        label.setToolTipText(Localization.lang("Open") + " CiteSeer URL");
        GUIGlobals.TABLE_ICONS.put("citeseerurl", label);

        label = new JLabel(IconTheme.JabRefIcon.WWW.getSmallIcon());
        label.setToolTipText(Localization.lang("Open") + " ArXiv URL");
        GUIGlobals.TABLE_ICONS.put(FieldName.EPRINT, label);

        label = new JLabel(IconTheme.JabRefIcon.DOI.getSmallIcon());
        label.setToolTipText(Localization.lang("Open") + " DOI " + Localization.lang("web link"));
        GUIGlobals.TABLE_ICONS.put(FieldName.DOI, label);

        label = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
        label.setToolTipText(Localization.lang("Open") + " PS");
        GUIGlobals.TABLE_ICONS.put(FieldName.PS, label);

        label = new JLabel(IconTheme.JabRefIcon.FOLDER.getSmallIcon());
        label.setToolTipText(Localization.lang("Open folder"));
        GUIGlobals.TABLE_ICONS.put(FieldName.FOLDER, label);

        label = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
        label.setToolTipText(Localization.lang("Open file"));
        GUIGlobals.TABLE_ICONS.put(FieldName.FILE, label);

        for (ExternalFileType fileType : ExternalFileTypes.getInstance().getExternalFileTypeSelection()) {
            label = new JLabel(fileType.getIcon());
            label.setToolTipText(Localization.lang("Open %0 file", fileType.getName()));
            GUIGlobals.TABLE_ICONS.put(fileType.getName(), label);
        }

        SpecialFieldViewModel relevanceViewModel = new SpecialFieldViewModel(SpecialField.RELEVANCE);
        label = new JLabel(relevanceViewModel.getRepresentingIcon());
        label.setToolTipText(relevanceViewModel.getLocalization());
        GUIGlobals.TABLE_ICONS.put(SpecialField.RELEVANCE.getFieldName(), label);

        SpecialFieldViewModel qualityViewModel = new SpecialFieldViewModel(SpecialField.QUALITY);
        label = new JLabel(qualityViewModel.getRepresentingIcon());
        label.setToolTipText(qualityViewModel.getLocalization());
        GUIGlobals.TABLE_ICONS.put(SpecialField.QUALITY.getFieldName(), label);

        // Ranking item in the menu uses one star
        SpecialFieldViewModel rankViewModel = new SpecialFieldViewModel(SpecialField.RANKING);
        label = new JLabel(rankViewModel.getRepresentingIcon());
        label.setToolTipText(rankViewModel.getLocalization());
        GUIGlobals.TABLE_ICONS.put(SpecialField.RANKING.getFieldName(), label);

        // Priority icon used for the menu
        SpecialFieldViewModel priorityViewModel = new SpecialFieldViewModel(SpecialField.PRIORITY);
        label = new JLabel(priorityViewModel.getRepresentingIcon());
        label.setToolTipText(priorityViewModel.getLocalization());
        GUIGlobals.TABLE_ICONS.put(SpecialField.PRIORITY.getFieldName(), label);

        // Read icon used for menu
        SpecialFieldViewModel readViewModel = new SpecialFieldViewModel(SpecialField.READ_STATUS);
        label = new JLabel(readViewModel.getRepresentingIcon());
        label.setToolTipText(readViewModel.getLocalization());
        GUIGlobals.TABLE_ICONS.put(SpecialField.READ_STATUS.getFieldName(), label);

        // Print icon used for menu
        SpecialFieldViewModel printedViewModel = new SpecialFieldViewModel(SpecialField.PRINTED);
        label = new JLabel(printedViewModel.getRepresentingIcon());
        label.setToolTipText(printedViewModel.getLocalization());
        GUIGlobals.TABLE_ICONS.put(SpecialField.PRINTED.getFieldName(), label);

        if (Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS)) {
            EmacsKeyBindings.load();
        }

        // Set up entry editor colors, first time:
        GUIGlobals.updateEntryEditorColors();

        GUIGlobals.currentFont = new Font(Globals.prefs.get(JabRefPreferences.FONT_FAMILY),
                Globals.prefs.getInt(JabRefPreferences.FONT_STYLE), Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));

        // Set WM_CLASS using reflection for certain Un*x window managers
        if (!OS.WINDOWS && !OS.OS_X) {
            try {
                Toolkit xToolkit = Toolkit.getDefaultToolkit();
                java.lang.reflect.Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(xToolkit, "org-jabref-JabRefMain");
            } catch (Exception e) {
                // ignore any error since this code only works for certain toolkits
            }
        }

    }

    public static void setFont(int size) {
        currentFont = new Font(currentFont.getFamily(), currentFont.getStyle(), size);
        // update preferences
        Globals.prefs.putInt(JabRefPreferences.FONT_SIZE, size);
    }

}
