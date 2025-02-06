package org.jabref.gui.groups;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.binding.IntegerBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.input.Dragboard;
import javafx.scene.paint.Color;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.StateManager;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.CustomLocalDragboard;
import org.jabref.gui.util.DroppingMouseLocation;
import org.jabref.gui.util.UiTaskExecutor;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.logic.util.BackgroundTask;
import org.jabref.logic.util.TaskExecutor;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.AutomaticKeywordGroup;
import org.jabref.model.groups.AutomaticPersonsGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.groups.KeywordGroup;
import org.jabref.model.groups.LastNameGroup;
import org.jabref.model.groups.RegexKeywordGroup;
import org.jabref.model.groups.SearchGroup;
import org.jabref.model.groups.TexGroup;
import org.jabref.model.search.event.IndexAddedOrUpdatedEvent;
import org.jabref.model.search.event.IndexClosedEvent;
import org.jabref.model.search.event.IndexRemovedEvent;
import org.jabref.model.search.event.IndexStartedEvent;
import org.jabref.model.strings.StringUtil;

import com.google.common.eventbus.Subscribe;
import com.tobiasdiez.easybind.EasyBind;
import com.tobiasdiez.easybind.EasyObservableList;
import io.github.adr.linked.ADR;

public class GroupNodeViewModel {

    private final String displayName;
    private final boolean isRoot;
    private final ObservableList<GroupNodeViewModel> children;
    private final BibDatabaseContext databaseContext;
    private final StateManager stateManager;
    private final GroupTreeNode groupNode;
    @ADR(38)
    private final ObservableSet<String> matchedEntries = FXCollections.observableSet();
    private final SimpleBooleanProperty hasChildren;
    private final SimpleBooleanProperty expandedProperty = new SimpleBooleanProperty();
    private final BooleanBinding anySelectedEntriesMatched;
    private final BooleanBinding allSelectedEntriesMatched;
    private final TaskExecutor taskExecutor;
    private final CustomLocalDragboard localDragBoard;
    private final GuiPreferences preferences;
    @SuppressWarnings("FieldCanBeLocal")
    private final ObservableList<BibEntry> entriesList;
    @SuppressWarnings("FieldCanBeLocal")
    private final InvalidationListener onInvalidatedGroup = listener -> refreshGroup();

    public GroupNodeViewModel(BibDatabaseContext databaseContext, StateManager stateManager, TaskExecutor taskExecutor, GroupTreeNode groupNode, CustomLocalDragboard localDragBoard, GuiPreferences preferences) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.stateManager = Objects.requireNonNull(stateManager);
        this.groupNode = Objects.requireNonNull(groupNode);
        this.localDragBoard = Objects.requireNonNull(localDragBoard);
        this.preferences = preferences;

        displayName = new LatexToUnicodeFormatter().format(groupNode.getName());
        isRoot = groupNode.isRoot();
        if (groupNode.getGroup() instanceof AutomaticGroup automaticGroup) {
            children = automaticGroup.createSubgroups(this.databaseContext.getDatabase().getEntries())
                                     .stream()
                                     .map(this::toViewModel)
                                     .sorted((group1, group2) -> group1.getDisplayName().compareToIgnoreCase(group2.getDisplayName()))
                                     .collect(Collectors.toCollection(FXCollections::observableArrayList));
        } else {
            children = EasyBind.mapBacked(groupNode.getChildren(), this::toViewModel);
        }
        if (groupNode.getGroup() instanceof TexGroup) {
            databaseContext.getMetaData().groupsBinding().addListener(new WeakInvalidationListener(onInvalidatedGroup));
        } else if (groupNode.getGroup() instanceof SearchGroup searchGroup) {
            stateManager.getIndexManager(databaseContext).ifPresent(indexManager -> {
                searchGroup.setMatchedEntries(indexManager.search(searchGroup.getSearchQuery()).getMatchedEntries());
                refreshGroup();
                databaseContext.getMetaData().groupsBinding().invalidate();
            });
        }

        hasChildren = new SimpleBooleanProperty();
        hasChildren.bind(Bindings.isNotEmpty(children));
        EasyBind.subscribe(preferences.getGroupsPreferences().displayGroupCountProperty(), shouldDisplay -> updateMatchedEntries());
        expandedProperty.set(groupNode.getGroup().isExpanded());
        expandedProperty.addListener((observable, oldValue, newValue) -> groupNode.getGroup().setExpanded(newValue));

        // Register listener
        // The wrapper created by the FXCollections will set a weak listener on the wrapped list. This weak listener gets garbage collected. Hence, we need to maintain a reference to this list.
        entriesList = databaseContext.getDatabase().getEntries();
        entriesList.addListener(this::onDatabaseChanged);

        EasyObservableList<Boolean> selectedEntriesMatchStatus = EasyBind.map(stateManager.getSelectedEntries(), groupNode::matches);
        anySelectedEntriesMatched = selectedEntriesMatchStatus.anyMatch(matched -> matched);
        // 'all' returns 'true' for empty streams, so this has to be checked explicitly
        allSelectedEntriesMatched = selectedEntriesMatchStatus.isEmptyBinding().not().and(selectedEntriesMatchStatus.allMatch(matched -> matched));

        this.databaseContext.getDatabase().registerListener(new SearchIndexListener());
    }

    public GroupNodeViewModel(BibDatabaseContext databaseContext, StateManager stateManager, TaskExecutor taskExecutor, AbstractGroup group, CustomLocalDragboard localDragboard, GuiPreferences preferences) {
        this(databaseContext, stateManager, taskExecutor, new GroupTreeNode(group), localDragboard, preferences);
    }

    static GroupNodeViewModel getAllEntriesGroup(BibDatabaseContext newDatabase, StateManager stateManager, TaskExecutor taskExecutor, CustomLocalDragboard localDragBoard, GuiPreferences preferences) {
        return new GroupNodeViewModel(newDatabase, stateManager, taskExecutor, DefaultGroupsFactory.getAllEntriesGroup(), localDragBoard, preferences);
    }

    private GroupNodeViewModel toViewModel(GroupTreeNode child) {
        return new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, child, localDragBoard, preferences);
    }

    public List<FieldChange> addEntriesToGroup(List<BibEntry> entries) {
        // TODO: warn if assignment has undesired side effects (modifies a field != keywords)
        // if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(group, groupSelector.frame))
        // {
        //    return; // user aborted operation
        // }

        var changes = groupNode.addEntriesToGroup(entries);

        // Update appearance of group
        anySelectedEntriesMatched.invalidate();
        allSelectedEntriesMatched.invalidate();

        return changes;
        // TODO: Store undo
        // if (!undo.isEmpty()) {
        // groupSelector.concludeAssignment(UndoableChangeEntriesOfGroup.getUndoableEdit(target, undo), target.getNode(), assignedEntries);
    }

    public SimpleBooleanProperty expandedProperty() {
        return expandedProperty;
    }

    public BooleanBinding anySelectedEntriesMatchedProperty() {
        return anySelectedEntriesMatched;
    }

    public BooleanBinding allSelectedEntriesMatchedProperty() {
        return allSelectedEntriesMatched;
    }

    public SimpleBooleanProperty hasChildrenProperty() {
        return hasChildren;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String getDescription() {
        return groupNode.getGroup().getDescription().orElse("");
    }

    public IntegerBinding getHits() {
        return Bindings.size(matchedEntries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }

        GroupNodeViewModel that = (GroupNodeViewModel) o;

        return groupNode.equals(that.groupNode);
    }

    @Override
    public String toString() {
        return "GroupNodeViewModel{" +
                "displayName='" + displayName + '\'' +
                ", isRoot=" + isRoot +
                ", icon='" + getIcon() + '\'' +
                ", children=" + children +
                ", databaseContext=" + databaseContext +
                ", groupNode=" + groupNode +
                ", matchedEntries=" + matchedEntries +
                '}';
    }

    @Override
    public int hashCode() {
        return groupNode.hashCode();
    }

    public JabRefIcon getIcon() {
        Optional<String> iconName = groupNode.getGroup().getIconName();
        return iconName.flatMap(this::parseIcon)
                       .orElseGet(this::createDefaultIcon);
    }

    private JabRefIcon createDefaultIcon() {
        Color color = groupNode.getGroup().getColor().orElse(IconTheme.getDefaultGroupColor());
        return IconTheme.JabRefIcons.DEFAULT_GROUP_ICON_COLORED.withColor(color);
    }

    private Optional<JabRefIcon> parseIcon(String iconCode) {
        return IconTheme.findIcon(iconCode, getColor());
    }

    public ObservableList<GroupNodeViewModel> getChildren() {
        return children;
    }

    public GroupTreeNode getGroupNode() {
        return groupNode;
    }

    /**
     * Gets invoked if an entry in the current database changes.
     *
     * @implNote Search groups are updated in {@link SearchIndexListener}.
     */
    private void onDatabaseChanged(ListChangeListener.Change<? extends BibEntry> change) {
        if (groupNode.getGroup() instanceof SearchGroup) {
            return;
        }
        while (change.next()) {
            if (change.wasPermutated()) {
                // Nothing to do, as permutation doesn't change matched entries
            } else if (change.wasUpdated()) {
                for (BibEntry changedEntry : change.getList().subList(change.getFrom(), change.getTo())) {
                    if (groupNode.matches(changedEntry)) {
                        // ADR-0038
                        matchedEntries.add(changedEntry.getId());
                    } else {
                        // ADR-0038
                        matchedEntries.remove(changedEntry.getId());
                    }
                }
            } else {
                for (BibEntry removedEntry : change.getRemoved()) {
                    // ADR-0038
                    matchedEntries.remove(removedEntry.getId());
                }
                for (BibEntry addedEntry : change.getAddedSubList()) {
                    if (groupNode.matches(addedEntry)) {
                        // ADR-0038
                        matchedEntries.add(addedEntry.getId());
                    }
                }
            }
        }
    }

    private void refreshGroup() {
        UiTaskExecutor.runInJavaFXThread(() -> {
            updateMatchedEntries(); // Update the entries matched by the group
            // "Re-add" to the selected groups if it were selected, this refreshes the entries the user views
            ObservableList<GroupTreeNode> selectedGroups = this.stateManager.getSelectedGroups(this.databaseContext);
            if (selectedGroups.remove(this.groupNode)) {
                selectedGroups.add(this.groupNode);
            }
        });
    }

    private void updateMatchedEntries() {
        // We calculate the new hit value
        // We could be more intelligent and try to figure out the new number of hits based on the entry change
        // for example, a previously matched entry gets removed -> hits = hits - 1
        if (preferences.getGroupsPreferences().shouldDisplayGroupCount()) {
            BackgroundTask
                    .wrap(() -> groupNode.findMatches(databaseContext.getDatabase()))
                    .onSuccess(entries -> {
                        matchedEntries.clear();
                        // ADR-0038
                        entries.forEach(entry -> matchedEntries.add(entry.getId()));
                    })
                    .executeWith(taskExecutor);
        }
    }

    public GroupTreeNode addSubgroup(AbstractGroup subgroup) {
        return groupNode.addSubgroup(subgroup);
    }

    void toggleExpansion() {
        expandedProperty().set(!expandedProperty().get());
    }

    boolean isMatchedBy(String searchString) {
        return StringUtil.isBlank(searchString) || StringUtil.containsIgnoreCase(getDisplayName(), searchString);
    }

    public Color getColor() {
        return groupNode.getGroup().getColor().orElse(IconTheme.getDefaultGroupColor());
    }

    public String getPath() {
        return groupNode.getPath();
    }

    public Optional<GroupNodeViewModel> getChildByPath(String pathToSource) {
        return groupNode.getChildByPath(pathToSource).map(this::toViewModel);
    }

    /**
     * Decides if the content stored in the given {@link Dragboard} can be dropped on the given target row. Currently, the following sources are allowed:
     * <ul>
     *     <li>another group (will be added as subgroup on drop)</li>
     *     <li>entries if the group implements {@link GroupEntryChanger} (will be assigned to group on drop)</li>
     * </ul>
     */
    public boolean acceptableDrop(Dragboard dragboard) {
        // TODO: we should also check isNodeDescendant
        boolean canDropOtherGroup = dragboard.hasContent(DragAndDropDataFormats.GROUP);
        boolean canDropEntries = localDragBoard.hasBibEntries() && (groupNode.getGroup() instanceof GroupEntryChanger);
        return canDropOtherGroup || canDropEntries;
    }

    public void moveTo(GroupNodeViewModel target) {
        // TODO: Add undo and display message
        // MoveGroupChange undo = new MoveGroupChange(((GroupTreeNodeViewModel)source.getParent()).getNode(),
        //        source.getNode().getPositionInParent(), target.getNode(), target.getChildCount());

        getGroupNode().moveTo(target.getGroupNode());
        // panel.getUndoManager().addEdit(new UndoableMoveGroup(this.groupsRoot, moveChange));
        // panel.markBaseChanged();
        // frame.output(Localization.lang("Moved group \"%0\".", node.getNode().getGroup().getName()));
    }

    public void moveTo(GroupTreeNode target, int targetIndex) {
        getGroupNode().moveTo(target, targetIndex);
    }

    public Optional<GroupTreeNode> getParent() {
        return groupNode.getParent();
    }

    public void draggedOn(GroupNodeViewModel target, DroppingMouseLocation mouseLocation) {
        // No action, if the target is the same as the source
        if (this.equals(target)) {
            return;
        }

        Optional<GroupTreeNode> targetParent = target.getParent();
        if (targetParent.isPresent()) {
            int targetIndex = target.getPositionInParent();
            // In case we want to move an item in the same parent
            // and the item is moved down, we need to adjust the target index
            if (targetParent.equals(getParent())) {
                int sourceIndex = this.getPositionInParent();
                if (sourceIndex < targetIndex) {
                    targetIndex--;
                }
            }

            // Different actions depending on where the user releases the drop in the target row
            // Bottom + top -> insert source row before / after this row
            // Center -> add as child
            switch (mouseLocation) {
                case BOTTOM -> this.moveTo(targetParent.get(), targetIndex + 1);
                case CENTER -> this.moveTo(target);
                case TOP -> this.moveTo(targetParent.get(), targetIndex);
            }
        } else {
            // No parent = root -> just add
            this.moveTo(target);
        }
    }

    private int getPositionInParent() {
        return groupNode.getPositionInParent();
    }

    public boolean hasSubgroups() {
        return !getChildren().isEmpty();
    }

    public boolean canAddEntriesIn() {
        AbstractGroup group = groupNode.getGroup();
        if (group instanceof AllEntriesGroup) {
            return false;
        } else if (group instanceof ExplicitGroup) {
            return true;
        } else if (group instanceof LastNameGroup || group instanceof RegexKeywordGroup) {
            return groupNode.getParent()
                            .map(GroupTreeNode::getGroup)
                            .map(groupParent -> groupParent instanceof AutomaticKeywordGroup || groupParent instanceof AutomaticPersonsGroup)
                            .orElse(false);
        } else if (group instanceof KeywordGroup) {
            // also covers WordKeywordGroup
            return true;
        } else if (group instanceof SearchGroup) {
            return false;
        } else if (group instanceof AutomaticKeywordGroup) {
            return false;
        } else if (group instanceof AutomaticPersonsGroup) {
            return false;
        } else if (group instanceof TexGroup) {
            return false;
        } else {
            throw new UnsupportedOperationException("canAddEntriesIn method not yet implemented in group: " + group.getClass().getName());
        }
    }

    public boolean canBeDragged() {
        AbstractGroup group = groupNode.getGroup();
        return switch (group) {
            case AllEntriesGroup _ -> false;
            case ExplicitGroup _, SearchGroup _, AutomaticKeywordGroup _, AutomaticPersonsGroup _, TexGroup _ -> true;
            case KeywordGroup _ ->
                // KeywordGroup is parent of LastNameGroup, RegexKeywordGroup and WordKeywordGroup
                    groupNode.getParent()
                            .map(GroupTreeNode::getGroup)
                            .map(groupParent ->
                                    !(groupParent instanceof AutomaticKeywordGroup || groupParent instanceof AutomaticPersonsGroup))
                            .orElse(false);

            case null -> throw new IllegalArgumentException("Group cannot be null");
            default -> throw new UnsupportedOperationException("canBeDragged method not yet implemented in group: " + group.getClass().getName());
        };
    }

    public boolean canAddGroupsIn() {
        AbstractGroup group = groupNode.getGroup();
        return switch (group) {
            case AllEntriesGroup _, ExplicitGroup _, SearchGroup _, TexGroup _ -> true;
            case KeywordGroup _ ->
                // KeywordGroup is parent of LastNameGroup, RegexKeywordGroup and WordKeywordGroup
                    groupNode.getParent()
                            .map(GroupTreeNode::getGroup)
                            .map(groupParent -> !(groupParent instanceof AutomaticKeywordGroup || groupParent instanceof AutomaticPersonsGroup))
                            .orElse(false);
            case AutomaticKeywordGroup _, AutomaticPersonsGroup _ -> false;
            case null -> throw new IllegalArgumentException("Group cannot be null");
            default -> throw new UnsupportedOperationException("canAddGroupsIn method not yet implemented in group: " + group.getClass().getName());
        };
    }

    public boolean canRemove() {
        AbstractGroup group = groupNode.getGroup();
        return switch (group) {
            case AllEntriesGroup _ -> false;
            case ExplicitGroup _, SearchGroup _, AutomaticKeywordGroup _, AutomaticPersonsGroup _, TexGroup _ -> true;
            case KeywordGroup _ ->
                // KeywordGroup is parent of LastNameGroup, RegexKeywordGroup and WordKeywordGroup
                    groupNode.getParent()
                            .map(GroupTreeNode::getGroup)
                            .map(groupParent -> !(groupParent instanceof AutomaticKeywordGroup || groupParent instanceof AutomaticPersonsGroup))
                            .orElse(false);
            case null -> throw new IllegalArgumentException("Group cannot be null");
            default -> throw new UnsupportedOperationException("canRemove method not yet implemented in group: " + group.getClass().getName());
        };
    }

    public boolean isEditable() {
        AbstractGroup group = groupNode.getGroup();
        return switch (group) {
            case AllEntriesGroup _ -> false;
            case ExplicitGroup _, SearchGroup _, AutomaticKeywordGroup _, AutomaticPersonsGroup _, TexGroup _ -> true;
            case KeywordGroup _ ->
                // KeywordGroup is parent of LastNameGroup, RegexKeywordGroup and WordKeywordGroup
                    groupNode.getParent()
                            .map(GroupTreeNode::getGroup)
                            .map(groupParent -> !(groupParent instanceof AutomaticKeywordGroup || groupParent instanceof AutomaticPersonsGroup))
                            .orElse(false);

            case null -> throw new IllegalArgumentException("Group cannot be null");
            default -> throw new UnsupportedOperationException("isEditable method not yet implemented in group: " + group.getClass().getName());
        };
    }

    class SearchIndexListener {
        @Subscribe
        public void listen(IndexStartedEvent event) {
            if (groupNode.getGroup() instanceof SearchGroup searchGroup) {
                stateManager.getIndexManager(databaseContext).ifPresent(indexManager -> {
                    searchGroup.setMatchedEntries(indexManager.search(searchGroup.getSearchQuery()).getMatchedEntries());
                    refreshGroup();
                    databaseContext.getMetaData().groupsBinding().invalidate();
                });
            }
        }

        @Subscribe
        public void listen(IndexAddedOrUpdatedEvent event) {
            if (groupNode.getGroup() instanceof SearchGroup searchGroup) {
                stateManager.getIndexManager(databaseContext).ifPresent(indexManager -> {
                    BackgroundTask.wrap(() -> {
                        for (BibEntry entry : event.entries()) {
                            searchGroup.updateMatches(entry, indexManager.isEntryMatched(entry, searchGroup.getSearchQuery()));
                        }
                    }).onFinished(() -> {
                        for (BibEntry entry : event.entries()) {
                            if (groupNode.matches(entry)) {
                                matchedEntries.add(entry.getId());
                            } else {
                                matchedEntries.remove(entry.getId());
                            }
                        }
                    }).executeWith(taskExecutor);
                });
            }
        }

        @Subscribe
        public void listen(IndexRemovedEvent event) {
            if (groupNode.getGroup() instanceof SearchGroup searchGroup) {
                for (BibEntry entry : event.entries()) {
                    searchGroup.updateMatches(entry, false);
                    matchedEntries.remove(entry.getId());
                }
            }
        }

        @Subscribe
        public void listen(IndexClosedEvent event) {
            if (groupNode.getGroup() instanceof SearchGroup group) {
                databaseContext.getDatabase().unregisterListener(this);
            }
        }
    }
}
