package org.jabref.gui.groups;

import javafx.scene.Node;
import javafx.scene.control.Tooltip;

import org.jabref.gui.icon.IconTheme.JabRefIcons;
import org.jabref.logic.l10n.Localization;

public class GroupModeViewModel {

    private final GroupViewMode mode;

    public GroupModeViewModel(GroupViewMode mode) {
        this.mode = mode;
    }

    public Node getUnionIntersectionGraphic() {
        if (mode == GroupViewMode.UNION) {
            return JabRefIcons.GROUP_UNION.getGraphicNode();
        } else if (mode == GroupViewMode.INTERSECTION) {
            return JabRefIcons.GROUP_INTERSECTION.getGraphicNode();
        }

        // As there is no concept like an empty node/icon, we return simply the other icon
        return JabRefIcons.GROUP_INTERSECTION.getGraphicNode();
    }

    public Tooltip getUnionIntersectionTooltip() {
        if (mode == GroupViewMode.UNION) {
            return new Tooltip(Localization.lang("Toggle intersection"));
        } else if (mode == GroupViewMode.INTERSECTION) {
            return new Tooltip(Localization.lang("Toggle union"));
        }
        return new Tooltip();
    }
}
