package net.sf.jabref.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;

import net.sf.jabref.Globals;
import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.external.ExternalFileTypes;
import net.sf.jabref.gui.keyboard.EmacsKeyBindings;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.SpecialFields;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Static variables for graphics files and keyboard shortcuts.
 */
public class GUIGlobals {

    private static final Log LOGGER = LogFactory.getLog(GUIGlobals.class);

    public static final String UNTITLED_TITLE = Localization.lang("untitled");
    public static Font currentFont;

    private static final Map<String, JLabel> TABLE_ICONS = new HashMap<>(); // Contains table icon mappings. Set up

    //	Colors.
    public static final Color ENTRY_EDITOR_LABEL_COLOR = new Color(100, 100, 150); // Empty field, blue.
    static final Color ACTIVE_TABBED_COLOR = ENTRY_EDITOR_LABEL_COLOR.darker(); // active Database (JTabbedPane)
    static final Color INACTIVE_TABBED_COLOR = Color.black; // inactive Database
    public static Color editorTextColor;
    public static Color validFieldBackgroundColor;
    public static Color activeBackgroundColor;
    public static Color invalidFieldBackgroundColor;
    public static final Color NULL_FIELD_COLOR = new Color(75, 130, 95); // Valid field, green.
    public static final Color ACTIVE_EDITOR_COLOR = new Color(230, 230, 255);

    public static final int WIDTH_ICON_COL = 26;
    public static final int WIDTH_ICON_COL_RANKING = 80; // Width of Ranking Icon Column

    public static final int MAX_BACK_HISTORY_SIZE = 10; // The maximum number of "Back" operations stored.

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

        label = new JLabel(Relevance.getInstance().getRepresentingIcon());
        label.setToolTipText(Relevance.getInstance().getToolTip());
        GUIGlobals.TABLE_ICONS.put(SpecialFields.FIELDNAME_RELEVANCE, label);

        label = new JLabel(Quality.getInstance().getRepresentingIcon());
        label.setToolTipText(Quality.getInstance().getToolTip());
        GUIGlobals.TABLE_ICONS.put(SpecialFields.FIELDNAME_QUALITY, label);

        // Ranking item in the menu uses one star
        label = new JLabel(Rank.getInstance().getRepresentingIcon());
        label.setToolTipText(Rank.getInstance().getToolTip());
        GUIGlobals.TABLE_ICONS.put(SpecialFields.FIELDNAME_RANKING, label);

        // Priority icon used for the menu
        label = new JLabel(Priority.getInstance().getRepresentingIcon());
        label.setToolTipText(Priority.getInstance().getToolTip());
        GUIGlobals.TABLE_ICONS.put(SpecialFields.FIELDNAME_PRIORITY, label);

        // Read icon used for menu
        label = new JLabel(ReadStatus.getInstance().getRepresentingIcon());
        label.setToolTipText(ReadStatus.getInstance().getToolTip());
        GUIGlobals.TABLE_ICONS.put(SpecialFields.FIELDNAME_READ, label);

        // Print icon used for menu
        label = new JLabel(Printed.getInstance().getRepresentingIcon());
        label.setToolTipText(Printed.getInstance().getToolTip());
        GUIGlobals.TABLE_ICONS.put(SpecialFields.FIELDNAME_PRINTED, label);

        if (Globals.prefs.getBoolean(JabRefPreferences.EDITOR_EMACS_KEYBINDINGS)) {
            EmacsKeyBindings.load();
        }

        // Set up entry editor colors, first time:
        GUIGlobals.updateEntryEditorColors();

        GUIGlobals.currentFont = new Font(Globals.prefs.get(JabRefPreferences.FONT_FAMILY),
                Globals.prefs.getInt(JabRefPreferences.FONT_STYLE), Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));

    }

}
