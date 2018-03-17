package org.jabref.gui.maintable;

import java.util.HashMap;
import java.util.Map;

import javax.swing.undo.UndoManager;

import javafx.scene.Node;

import org.jabref.gui.IconTheme;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.specialfields.SpecialField;

public class CellFactory {

    private final Map<String, Node> TABLE_ICONS = new HashMap<>();

    public CellFactory(ExternalFileTypes externalFileTypes, UndoManager undoManager) {
        Node label;
        label = IconTheme.JabRefIcons.PDF_FILE.getGraphicNode();
        //label.setToo(Localization.lang("Open") + " PDF");
        TABLE_ICONS.put(FieldName.PDF, label);

        label = IconTheme.JabRefIcons.WWW.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open") + " URL");
        TABLE_ICONS.put(FieldName.URL, label);

        label = IconTheme.JabRefIcons.WWW.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open") + " CiteSeer URL");
        TABLE_ICONS.put("citeseerurl", label);

        label = IconTheme.JabRefIcons.WWW.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open") + " ArXiv URL");
        TABLE_ICONS.put(FieldName.EPRINT, label);

        label = IconTheme.JabRefIcons.DOI.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open") + " DOI " + Localization.lang("web link"));
        TABLE_ICONS.put(FieldName.DOI, label);

        label = IconTheme.JabRefIcons.FILE.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open") + " PS");
        TABLE_ICONS.put(FieldName.PS, label);

        label = IconTheme.JabRefIcons.FOLDER.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open folder"));
        TABLE_ICONS.put(FieldName.FOLDER, label);

        label = IconTheme.JabRefIcons.FILE.getGraphicNode();
        //label.setToolTipText(Localization.lang("Open file"));
        TABLE_ICONS.put(FieldName.FILE, label);

        for (ExternalFileType fileType : externalFileTypes.getExternalFileTypeSelection()) {
            label = fileType.getIcon().getGraphicNode();
            //label.setToolTipText(Localization.lang("Open %0 file", fileType.getName()));
            TABLE_ICONS.put(fileType.getName(), label);
        }

        SpecialFieldViewModel relevanceViewModel = new SpecialFieldViewModel(SpecialField.RELEVANCE, undoManager);
        label = relevanceViewModel.getIcon().getGraphicNode();
        //label.setToolTipText(relevanceViewModel.getLocalization());
        TABLE_ICONS.put(SpecialField.RELEVANCE.getFieldName(), label);

        SpecialFieldViewModel qualityViewModel = new SpecialFieldViewModel(SpecialField.QUALITY, undoManager);
        label = qualityViewModel.getIcon().getGraphicNode();
        //label.setToolTipText(qualityViewModel.getLocalization());
        TABLE_ICONS.put(SpecialField.QUALITY.getFieldName(), label);

        // Ranking item in the menu uses one star
        SpecialFieldViewModel rankViewModel = new SpecialFieldViewModel(SpecialField.RANKING, undoManager);
        label = rankViewModel.getIcon().getGraphicNode();
        //label.setToolTipText(rankViewModel.getLocalization());
        TABLE_ICONS.put(SpecialField.RANKING.getFieldName(), label);

        // Priority icon used for the menu
        SpecialFieldViewModel priorityViewModel = new SpecialFieldViewModel(SpecialField.PRIORITY, undoManager);
        label = priorityViewModel.getIcon().getGraphicNode();
        //label.setToolTipText(priorityViewModel.getLocalization());
        TABLE_ICONS.put(SpecialField.PRIORITY.getFieldName(), label);

        // Read icon used for menu
        SpecialFieldViewModel readViewModel = new SpecialFieldViewModel(SpecialField.READ_STATUS, undoManager);
        label = readViewModel.getIcon().getGraphicNode();
        //label.setToolTipText(readViewModel.getLocalization());
        TABLE_ICONS.put(SpecialField.READ_STATUS.getFieldName(), label);

        // Print icon used for menu
        SpecialFieldViewModel printedViewModel = new SpecialFieldViewModel(SpecialField.PRINTED, undoManager);
        label = printedViewModel.getIcon().getGraphicNode();
        //label.setToolTipText(printedViewModel.getLocalization());
        TABLE_ICONS.put(SpecialField.PRINTED.getFieldName(), label);
    }

    public Node getTableIcon(String fieldType) {
        Node label = TABLE_ICONS.get(fieldType);
        if (label == null) {
            //LOGGER.info("Error: no table icon defined for type '" + fieldType + "'.");
            return null;
        } else {
            return label;
        }
    }
}
