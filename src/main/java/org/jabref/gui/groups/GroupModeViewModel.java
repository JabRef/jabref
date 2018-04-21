package org.jabref.gui.groups;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

import org.jabref.logic.l10n.Localization;

public class GroupModeViewModel {

    public static Tooltip getUnionIntersectionTooltip(GroupViewMode mode) {
        if (mode == GroupViewMode.UNION) {
            return new Tooltip(Localization.lang("Toogle intersection"));
        }
        if (mode == GroupViewMode.INTERSECTION) {
            return new Tooltip(Localization.lang("Toogle union"));
        }
        return new Tooltip();

    }

    public static Node getUnionIntersectionGraphic(GroupViewMode mode) {
        return mode.getIcon().getGraphicNode();
    }
}