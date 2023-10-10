package org.jabref.gui.groups;

import javafx.collections.ObservableSet;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;

import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.logic.l10n.Localization;

public class GroupModeViewModel {

    private final ObservableSet<GroupViewMode> mode;

    public GroupModeViewModel(ObservableSet<GroupViewMode> mode) {
        this.mode = mode;
    }

    public Node getUnionIntersectionGraphic() {
        if (mode.contains(GroupViewMode.INTERSECTION)) {
            return JabRefIcons.GROUP_INTERSECTION.getGraphicNode();
        } else {
            return JabRefIcons.GROUP_UNION.getGraphicNode();
        }
    }

    public Tooltip getUnionIntersectionTooltip() {
        if (mode.contains(GroupViewMode.INTERSECTION)) {
            return new Tooltip(Localization.lang("Toggle intersection"));
        } else {
            return new Tooltip(Localization.lang("Toggle union"));
        }
    }
}
