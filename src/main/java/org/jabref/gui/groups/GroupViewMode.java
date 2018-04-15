package org.jabref.gui.groups;

import org.jabref.gui.icon.IconTheme.JabRefIcons;

public enum GroupViewMode {

    INTERSECTION(JabRefIcons.WWW),
    UNION(JabRefIcons.TWITTER);

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
