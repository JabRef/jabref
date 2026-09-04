package org.jabref.gui.maintable;

import java.util.HashMap;
import java.util.Map;

import javafx.scene.Node;

import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;

public class CellFactory {

    private final Map<Field, JabRefIcon> TABLE_ICONS = new HashMap<>();

    public CellFactory(GuiPreferences preferences) {
        JabRefIcon icon;
        icon = IconTheme.JabRefIcons.PDF_FILE;
        // icon.setToo(Localization.lang("Open") + " PDF");
        TABLE_ICONS.put(StandardField.PDF, icon);

        icon = IconTheme.JabRefIcons.WWW;
        // icon.setToolTipText(Localization.lang("Open") + " URL");
        TABLE_ICONS.put(StandardField.URL, icon);

        icon = IconTheme.JabRefIcons.WWW;
        // icon.setToolTipText(Localization.lang("Open") + " ArXivFetcher URL");
        TABLE_ICONS.put(StandardField.EPRINT, icon);

        icon = IconTheme.JabRefIcons.DOI;
        // icon.setToolTipText(Localization.lang("Open") + " DOI " + Localization.lang("web link"));
        TABLE_ICONS.put(StandardField.DOI, icon);

        icon = IconTheme.JabRefIcons.FILE;
        // icon.setToolTipText(Localization.lang("Open") + " PS");
        TABLE_ICONS.put(StandardField.PS, icon);

        icon = IconTheme.JabRefIcons.FOLDER;
        // icon.setToolTipText(Localization.lang("Open folder"));
        TABLE_ICONS.put(StandardField.FOLDER, icon);

        icon = IconTheme.JabRefIcons.FILE;
        // icon.setToolTipText(Localization.lang("Open file"));
        TABLE_ICONS.put(StandardField.FILE, icon);

        for (ExternalFileType fileType : preferences.getExternalApplicationsPreferences().getExternalFileTypes()) {
            icon = fileType.getIcon();
            // icon.setToolTipText(Localization.lang("Open %0 file", fileType.getName()));
            TABLE_ICONS.put(fileType.getField(), icon);
        }

        icon = SpecialFieldViewModel.getAction(SpecialField.RELEVANCE).getIcon().orElseThrow();
        TABLE_ICONS.put(SpecialField.RELEVANCE, icon);

        icon = SpecialFieldViewModel.getAction(SpecialField.QUALITY).getIcon().orElseThrow();
        TABLE_ICONS.put(SpecialField.QUALITY, icon);

        // Ranking item in the menu uses one star
        icon = SpecialFieldViewModel.getAction(SpecialField.RANKING).getIcon().orElseThrow();
        TABLE_ICONS.put(SpecialField.RANKING, icon);

        // Priority icon used for the menu
        icon = SpecialFieldViewModel.getAction(SpecialField.PRIORITY).getIcon().orElseThrow();
        TABLE_ICONS.put(SpecialField.PRIORITY, icon);

        // Read icon used for menu
        icon = SpecialFieldViewModel.getAction(SpecialField.READ_STATUS).getIcon().orElseThrow();
        TABLE_ICONS.put(SpecialField.READ_STATUS, icon);

        // Print icon used for menu
        icon = SpecialFieldViewModel.getAction(SpecialField.PRINTED).getIcon().orElseThrow();
        TABLE_ICONS.put(SpecialField.PRINTED, icon);
    }

    public Node getTableIcon(Field field) {
        JabRefIcon icon = TABLE_ICONS.get(field);
        if (icon == null) {
            // LOGGER.info("Error: no table icon defined for type '" + field + "'.");
            return null;
        } else {
            // node should be generated for each call, as nodes can be added to the scene graph only once
            return icon.getGraphicNode();
        }
    }
}
