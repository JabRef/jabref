package org.jabref.gui.groups;

import javafx.scene.Node;
import javafx.scene.layout.Priority;

import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * The groups side pane.
 */
public class GroupSidePane extends SidePaneComponent {

    private final JabRefPreferences preferences;

    public GroupSidePane(SidePaneManager manager, JabRefPreferences preferences) {
        super(manager, IconTheme.JabRefIcons.TOGGLE_GROUPS, Localization.lang("Groups"));
        this.preferences = preferences;
    }

    @Override
    public void afterOpening() {
        preferences.putBoolean(JabRefPreferences.GROUP_SIDEPANE_VISIBLE, Boolean.TRUE);
    }

    @Override
    public Priority getResizePolicy() {
        return Priority.ALWAYS;
    }

    @Override
    public void beforeClosing() {
        preferences.putBoolean(JabRefPreferences.GROUP_SIDEPANE_VISIBLE, Boolean.FALSE);
    }

    @Override
    public Action getToggleAction() {
        return StandardActions.TOGGLE_GROUPS;
    }

    @Override
    protected Node createContentPane() {
        return ViewLoader.view(GroupTreeView.class)
                         .load()
                         .getView();
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.GROUPS;
    }
}
