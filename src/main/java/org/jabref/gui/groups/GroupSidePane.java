package org.jabref.gui.groups;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.preferences.JabRefPreferences;

/**
 * The groups side pane.
 * This class is just a Swing wrapper around the JavaFX implementation {@link GroupTreeView}.
 */
public class GroupSidePane extends SidePaneComponent {

    protected final JabRefFrame frame;
    private final ToggleAction toggleAction;

    /**
     * The first element for each group defines which field to use for the quicksearch. The next two define the name and
     * regexp for the group.
     */
    public GroupSidePane(JabRefFrame frame, SidePaneManager manager) {
        super(manager, IconTheme.JabRefIcons.TOGGLE_GROUPS.getIcon(), Localization.lang("Groups"));

        toggleAction = new ToggleAction(Localization.menuTitle("Toggle groups interface"),
                Localization.lang("Toggle groups interface"),
                Globals.getKeyPrefs().getKey(KeyBinding.TOGGLE_GROUPS_INTERFACE),
                IconTheme.JabRefIcons.TOGGLE_GROUPS);

        this.frame = frame;

        JFXPanel groupsPane = CustomJFXPanel.create();

        add(groupsPane);
        // Execute on JavaFX Application Thread
        Platform.runLater(() -> {
            StackPane root = new StackPane();
            root.getChildren().addAll(new GroupTreeView().getView());
            Scene scene = new Scene(root);
            groupsPane.setScene(scene);
        });
    }

    @Override
    public void componentOpening() {
        Globals.prefs.putBoolean(JabRefPreferences.GROUP_SIDEPANE_VISIBLE, Boolean.TRUE);
    }

    @Override
    public int getRescalingWeight() {
        return 1;
    }

    @Override
    public void componentClosing() {
        getToggleAction().setSelected(false);
        Globals.prefs.putBoolean(JabRefPreferences.GROUP_SIDEPANE_VISIBLE, Boolean.FALSE);
    }

    @Override
    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
        if (panel == null) { // hide groups
            frame.getSidePaneManager().hide(GroupSidePane.class);
            return;
        }

        synchronized (getTreeLock()) {
            validateTree();
        }
    }

    @Override
    public void grabFocus() {

    }

    @Override
    public ToggleAction getToggleAction() {
        return toggleAction;
    }
}
