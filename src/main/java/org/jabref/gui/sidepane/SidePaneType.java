package org.jabref.gui.sidepane;

import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.l10n.Localization;

/**
 * Definition of all possible components in the side pane.
 */
public enum SidePaneType {
    OPEN_OFFICE("OpenOffice/LibreOffice", IconTheme.JabRefIcons.FILE_OPENOFFICE, StandardActions.TOOGLE_OO),
    WEB_SEARCH(Localization.lang("Web search"), IconTheme.JabRefIcons.WWW, StandardActions.TOGGLE_WEB_SEARCH),
    GROUPS(Localization.lang("Groups"), IconTheme.JabRefIcons.TOGGLE_GROUPS, StandardActions.TOGGLE_GROUPS);

    private final String title;
    private final JabRefIcon icon;
    private final Action toggleAction;

    SidePaneType(String title, JabRefIcon icon, Action toggleAction) {
        this.title = title;
        this.icon = icon;
        this.toggleAction = toggleAction;
    }

    public String getTitle() {
        return title;
    }

    public JabRefIcon getIcon() {
        return icon;
    }

    public Action getToggleAction() {
        return toggleAction;
    }
}
