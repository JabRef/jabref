package org.jabref.gui.groups;

import java.util.Collections;
import java.util.List;

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
import org.jabref.gui.maintable.MainTableDataModel;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.event.FieldChangedEvent;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.search.matchers.MatcherSet;
import org.jabref.model.search.matchers.MatcherSets;
import org.jabref.preferences.JabRefPreferences;

import com.google.common.eventbus.Subscribe;

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
        super(manager, IconTheme.JabRefIcon.TOGGLE_GROUPS.getIcon(), Localization.lang("Groups"));

        Globals.stateManager.activeGroupProperty()
                .addListener((observable, oldValue, newValue) -> updateShownEntriesAccordingToSelectedGroups(newValue));

        // register the panel the current active context
        Globals.stateManager.activeDatabaseProperty()
                .addListener((observable, oldValue, newValue) -> {
            newValue.ifPresent(databaseContext ->
                    databaseContext.getDatabase().registerListener(this));
            oldValue.ifPresent(databaseContext ->
                    databaseContext.getDatabase().unregisterListener(this));
        });

        toggleAction = new ToggleAction(Localization.menuTitle("Toggle groups interface"),
                Localization.lang("Toggle groups interface"),
                Globals.getKeyPrefs().getKey(KeyBinding.TOGGLE_GROUPS_INTERFACE),
                IconTheme.JabRefIcon.TOGGLE_GROUPS);

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

    @Subscribe
    public synchronized void listen(FieldChangedEvent event) {
        if (FieldName.GROUPS.equals(event.getFieldName())) {
            updateShownEntriesAccordingToSelectedGroups(Globals.stateManager.activeGroupProperty());
        }
    }

    private void updateShownEntriesAccordingToSelectedGroups(List<GroupTreeNode> selectedGroups) {
        if ((selectedGroups == null) || selectedGroups.isEmpty()) {
            // No selected group, show all entries
            selectedGroups = Collections.singletonList(new GroupTreeNode(DefaultGroupsFactory.getAllEntriesGroup()));
        }

        final MatcherSet searchRules = MatcherSets.build(
                Globals.prefs.getBoolean(JabRefPreferences.GROUP_INTERSECT_SELECTIONS) ? MatcherSets.MatcherType.AND : MatcherSets.MatcherType.OR);

        for (GroupTreeNode node : selectedGroups) {
            searchRules.addRule(node.getSearchMatcher());
        }

        GroupingWorker worker = new GroupingWorker(frame, panel);
        worker.run(searchRules);
        worker.update();
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
        if (panel != null) { // panel may be null if no file is open any more
            panel.getMainTable().getTableModel().updateGroupingState(MainTableDataModel.DisplayOption.DISABLED);
        }
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
