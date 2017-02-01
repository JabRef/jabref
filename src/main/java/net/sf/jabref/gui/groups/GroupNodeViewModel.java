package net.sf.jabref.gui.groups;

import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.ObservableList;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseContext;
import net.sf.jabref.model.entry.event.EntryEvent;
import net.sf.jabref.model.groups.AbstractGroup;
import net.sf.jabref.model.groups.AllEntriesGroup;
import net.sf.jabref.model.groups.GroupTreeNode;

import com.google.common.eventbus.Subscribe;
import org.fxmisc.easybind.EasyBind;

public class GroupNodeViewModel {

    private final String name;
    private final boolean isRoot;
    private final String iconCode;
    private final ObservableList<GroupNodeViewModel> children;
    private final BibDatabaseContext databaseContext;
    private final GroupTreeNode groupNode;
    private final SimpleIntegerProperty hits;
    private final SimpleBooleanProperty hasChildren;

    public GroupNodeViewModel(BibDatabaseContext databaseContext, GroupTreeNode groupNode) {
        this.databaseContext = Objects.requireNonNull(databaseContext);
        this.groupNode = Objects.requireNonNull(groupNode);

        name = groupNode.getName();
        isRoot = groupNode.isRoot();
        iconCode = "";
        children = EasyBind.map(groupNode.getChildren(), child -> new GroupNodeViewModel(databaseContext, child));
        hasChildren = new SimpleBooleanProperty();
        hasChildren.bind(Bindings.isNotEmpty(children));
        hits = new SimpleIntegerProperty(0);
        calculateNumberOfMatches();

        // Register listener
        databaseContext.getDatabase().registerListener(this);
    }


    public GroupNodeViewModel(BibDatabaseContext databaseContext, AbstractGroup group) {
        this(databaseContext, new GroupTreeNode(group));
    }

    static GroupNodeViewModel getAllEntriesGroup(BibDatabaseContext newDatabase) {
        return new GroupNodeViewModel(newDatabase, new AllEntriesGroup(Localization.lang("All entries")));
    }

    public SimpleBooleanProperty hasChildrenProperty() {
        return hasChildren;
    }

    public String getName() {
        return name;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public String getDescription() {
        return "Some group named " + getName();
    }

    public SimpleIntegerProperty getHits() {
        return hits;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GroupNodeViewModel that = (GroupNodeViewModel) o;

        if (isRoot != that.isRoot) return false;
        if (!name.equals(that.name)) return false;
        if (!iconCode.equals(that.iconCode)) return false;
        if (!children.equals(that.children)) return false;
        if (!databaseContext.equals(that.databaseContext)) return false;
        if (!groupNode.equals(that.groupNode)) return false;
        return hits.getValue().equals(that.hits.getValue());
    }

    @Override
    public String toString() {
        return "GroupNodeViewModel{" +
                "name='" + name + '\'' +
                ", isRoot=" + isRoot +
                ", iconCode='" + iconCode + '\'' +
                ", children=" + children +
                ", databaseContext=" + databaseContext +
                ", groupNode=" + groupNode +
                ", hits=" + hits +
                '}';
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (isRoot ? 1 : 0);
        result = 31 * result + iconCode.hashCode();
        result = 31 * result + children.hashCode();
        result = 31 * result + databaseContext.hashCode();
        result = 31 * result + groupNode.hashCode();
        result = 31 * result + hits.hashCode();
        return result;
    }

    public String getIconCode() {
        return iconCode;
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
}
