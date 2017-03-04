package org.jabref.gui.groups;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

import org.jabref.gui.IconTheme;
import org.jabref.gui.StateManager;
import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.logic.layout.format.LatexToUnicodeFormatter;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.event.EntryEvent;
import org.jabref.model.groups.AbstractGroup;
import org.jabref.model.groups.AutomaticGroup;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.strings.StringUtil;

import com.google.common.eventbus.Subscribe;
import org.fxmisc.easybind.EasyBind;

public class GroupNodeViewModel {

    private final String displayName;
    private final boolean isRoot;
    private final ObservableList<GroupNodeViewModel> children;
    private final BibDatabaseContext databaseContext;
    private final GroupTreeNode groupNode;
    private final SimpleIntegerProperty hits;
    private final SimpleBooleanProperty hasChildren;
    private final SimpleBooleanProperty expandedProperty = new SimpleBooleanProperty();
    private final BooleanBinding anySelectedEntriesMatched;
    private final BooleanBinding allSelectedEntriesMatched;
    public GroupNodeViewModel(BibDatabaseContext databaseContext, StateManager stateManager, GroupTreeNode groupNode) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.groupNode = Objects.requireNonNull(groupNode);

        LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();
        displayName = formatter.format(groupNode.getName());
        isRoot = groupNode.isRoot();
        if (groupNode.getGroup() instanceof AutomaticGroup) {
            AutomaticGroup automaticGroup = (AutomaticGroup) groupNode.getGroup();

            // TODO: Update on changes to entry list (however: there is no flatMap and filter as observable TransformationLists)
            children = databaseContext.getDatabase()
                    .getEntries().stream()
                    .flatMap(stream -> createSubgroups(databaseContext, stateManager, automaticGroup, stream))
                    .filter(distinctByKey(group -> group.getGroupNode().getName()))
                    .sorted((group1, group2) -> group1.getDisplayName().compareToIgnoreCase(group2.getDisplayName()))
                    .collect(Collectors.toCollection(FXCollections::observableArrayList));
        } else {
            children = EasyBind.map(groupNode.getChildren(),
                    child -> new GroupNodeViewModel(databaseContext, stateManager, child));
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

    public GroupNodeViewModel(BibDatabaseContext databaseContext, StateManager stateManager, AbstractGroup group) {
        this(databaseContext, stateManager, new GroupTreeNode(group));
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    static GroupNodeViewModel getAllEntriesGroup(BibDatabaseContext newDatabase, StateManager stateManager) {
        return new GroupNodeViewModel(newDatabase, stateManager, DefaultGroupsFactory.getAllEntriesGroup());
    }

    private Stream<GroupNodeViewModel> createSubgroups(BibDatabaseContext databaseContext, StateManager stateManager, AutomaticGroup automaticGroup, BibEntry entry) {
        return automaticGroup.createSubgroups(entry).stream()
                .map(child -> new GroupNodeViewModel(databaseContext, stateManager, child));
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupNodeViewModel that = (GroupNodeViewModel) o;

        if (!groupNode.equals(that.groupNode)) return false;
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
        new Thread(()-> {
            int newHits = groupNode.calculateNumberOfMatches(databaseContext.getDatabase());
            Platform.runLater(() -> hits.setValue(newHits));
        }).start();
    }

    public GroupTreeNode addSubgroup(AbstractGroup subgroup) {
        return groupNode.addSubgroup(subgroup);
    }

    void toggleExpansion() {
        expandedProperty().set(!expandedProperty().get());
    }

    boolean isMatchedBy(String searchString) {
        return StringUtil.isBlank(searchString) || getDisplayName().contains(searchString);
    }

    public Color getColor() {
        return groupNode.getGroup().getColor().orElse(IconTheme.getDefaultColor());
    }
}
