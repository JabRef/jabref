package org.jabref.gui.groups;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.Dragboard;
import javafx.scene.paint.Color;

import org.jabref.gui.DragAndDropDataFormats;
import org.jabref.gui.IconTheme;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.FieldChange;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.GroupEntryChanger;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.strings.StringUtil;

import com.google.common.eventbus.Subscribe;
import org.fxmisc.easybind.EasyBind;

public class GroupNodeViewModel {

    private final String displayName;
    private final boolean isRoot;
    private final ObservableList<GroupNodeViewModel> children;
    private final BibDatabaseContext databaseContext;
    private final StateManager stateManager;
    private final GroupTreeNode groupNode;
    private final SimpleIntegerProperty hits;
    private final SimpleBooleanProperty hasChildren;
    private final SimpleBooleanProperty expandedProperty = new SimpleBooleanProperty();
    private final BooleanBinding anySelectedEntriesMatched;
    private final BooleanBinding allSelectedEntriesMatched;
    private final TaskExecutor taskExecutor;

    public GroupNodeViewModel(BibDatabaseContext databaseContext, StateManager stateManager, TaskExecutor taskExecutor, GroupTreeNode groupNode) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.taskExecutor = Objects.requireNonNull(taskExecutor);
        this.stateManager = Objects.requireNonNull(stateManager);
        this.groupNode = Objects.requireNonNull(groupNode);

        LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();
        displayName = formatter.format(groupNode.getName());
        isRoot = groupNode.isRoot();
        if (groupNode.getGroup() instanceof AutomaticGroup) {
            AutomaticGroup automaticGroup = (AutomaticGroup) groupNode.getGroup();

            // TODO: Update on changes to entry list (however: there is no flatMap and filter as observable TransformationLists)
            children = databaseContext.getDatabase()
                    .getEntries().stream()
                    .flatMap(stream -> createSubgroups(automaticGroup, stream))
                    .filter(distinctByKey(group -> group.getGroupNode().getName()))
                    .sorted((group1, group2) -> group1.getDisplayName().compareToIgnoreCase(group2.getDisplayName()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
        } else {
            children = EasyBind.map(groupNode.getChildren(),
                    child -> new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, child));
        }
        hasChildren = new SimpleBooleanProperty();
        hasChildren.bind(Bindings.isNotEmpty(children));
        hits = new SimpleIntegerProperty(0);
        calculateNumberOfMatches();
        expandedProperty.set(groupNode.getGroup().isExpanded());
        expandedProperty.addListener((observable, oldValue, newValue) -> groupNode.getGroup().setExpanded(newValue));

        // Register listener
        databaseContext.getDatabase().registerListener(this);

        ObservableList<Boolean> selectedEntriesMatchStatus = EasyBind.map(stateManager.getSelectedEntries(), groupNode::matches);
        anySelectedEntriesMatched = BindingsHelper.any(selectedEntriesMatchStatus, matched -> matched);
        allSelectedEntriesMatched = BindingsHelper.all(selectedEntriesMatchStatus, matched -> matched);
    }

    public GroupNodeViewModel(BibDatabaseContext databaseContext, StateManager stateManager, TaskExecutor taskExecutor, AbstractGroup group) {
        this(databaseContext, stateManager, taskExecutor, new GroupTreeNode(group));
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    static GroupNodeViewModel getAllEntriesGroup(BibDatabaseContext newDatabase, StateManager stateManager, TaskExecutor taskExecutor) {
        return new GroupNodeViewModel(newDatabase, stateManager, taskExecutor, DefaultGroupsFactory.getAllEntriesGroup());
    }

    private Stream<GroupNodeViewModel> createSubgroups(AutomaticGroup automaticGroup, BibEntry entry) {
        return automaticGroup.createSubgroups(entry).stream()
                .map(child -> new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, child));
    }

    public List<FieldChange> addEntriesToGroup(List<BibEntry> entries) {
        // TODO: warn if assignment has undesired side effects (modifies a field != keywords)
        //if (!WarnAssignmentSideEffects.warnAssignmentSideEffects(group, groupSelector.frame))
        //{
        //    return; // user aborted operation
        //}

        return groupNode.addEntriesToGroup(entries);

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

    public SimpleIntegerProperty getHits() {
        return hits;
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

        if (!groupNode.equals(that.groupNode)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "GroupNodeViewModel{" +
                "displayName='" + displayName + '\'' +
                ", isRoot=" + isRoot +
                ", iconCode='" + getIconCode() + '\'' +
                ", children=" + children +
                ", databaseContext=" + databaseContext +
                ", groupNode=" + groupNode +
                ", hits=" + hits +
                '}';
    }

    @Override
    public int hashCode() {
        return groupNode.hashCode();
    }

    public String getIconCode() {
        return groupNode.getGroup().getIconCode().orElse(IconTheme.JabRefIcon.DEFAULT_GROUP_ICON.getCode());
    }

    public ObservableList<GroupNodeViewModel> getChildren() {
        return children;
    }

    public GroupTreeNode getGroupNode() {
        return groupNode;
    }

    /**
     * Gets invoked if an entry in the current database changes.
     */
    @Subscribe
    public void listen(@SuppressWarnings("unused") EntryEvent entryEvent) {
        calculateNumberOfMatches();
    }

    private void calculateNumberOfMatches() {
        // We calculate the new hit value
        // We could be more intelligent and try to figure out the new number of hits based on the entry change
        // for example, a previously matched entry gets removed -> hits = hits - 1
        BackgroundTask
                .wrap(() -> groupNode.calculateNumberOfMatches(databaseContext.getDatabase()))
                .onSuccess(hits::setValue)
                .executeWith(taskExecutor);
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
        return groupNode.getGroup().getColor().orElse(IconTheme.getDefaultColor());
    }

    public String getPath() {
        return groupNode.getPath();
    }

    public Optional<GroupNodeViewModel> getChildByPath(String pathToSource) {
        return groupNode.getChildByPath(pathToSource).map(child -> new GroupNodeViewModel(databaseContext, stateManager, taskExecutor, child));
    }

    /**
     * Decides if the content stored in the given {@link Dragboard} can be droped on the given target row.
     * Currently, the following sources are allowed:
     *  - another group (will be added as subgroup on drop)
     *  - entries if the group implements {@link GroupEntryChanger} (will be assigned to group on drop)
     */
    public boolean acceptableDrop(Dragboard dragboard) {
        // TODO: we should also check isNodeDescendant
        boolean canDropOtherGroup = dragboard.hasContent(DragAndDropDataFormats.GROUP);
        boolean canDropEntries = dragboard.hasContent(DragAndDropDataFormats.ENTRIES)
                && groupNode.getGroup() instanceof GroupEntryChanger;
        return canDropOtherGroup || canDropEntries;
    }

    public void moveTo(GroupNodeViewModel target) {
        // TODO: Add undo and display message
        //MoveGroupChange undo = new MoveGroupChange(((GroupTreeNodeViewModel)source.getParent()).getNode(),
        //        source.getNode().getPositionInParent(), target.getNode(), target.getChildCount());

        getGroupNode().moveTo(target.getGroupNode());
        //panel.getUndoManager().addEdit(new UndoableMoveGroup(this.groupsRoot, moveChange));
        //panel.markBaseChanged();
        //frame.output(Localization.lang("Moved group \"%0\".", node.getNode().getGroup().getName()));

    }
}
