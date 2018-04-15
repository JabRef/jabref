package org.jabref.gui.groups;

import org.jabref.gui.icon.IconTheme.JabRefIcons;

public enum GroupViewMode {

    INTERSECTION(JabRefIcons.GROUP_INTERSECTION),
    UNION(JabRefIcons.GROUP_UNION);

    private JabRefIcons icon;

    GroupViewMode(JabRefIcons icon) {
        this.icon = icon;
    }

    GroupViewMode() {
        //empty, but needed for valueOf Method
    }

    public JabRefIcons getIcon() {
        return icon;
    }
}
