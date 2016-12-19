package net.sf.jabref.gui.groups;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import net.sf.jabref.gui.IconTheme;

public class GroupNodeViewModel {

    private final String name;
    private final boolean isRoot;
    private ObservableValue<Integer> hits;
    private String iconCode;
    private boolean isLeaf;
    private ObservableList<GroupNodeViewModel> children = FXCollections.observableArrayList();;

    public GroupNodeViewModel(String name, boolean isRoot, int hits, boolean isLeaf) {
        this(name, isRoot, hits, IconTheme.JabRefIcon.QUALITY, isLeaf);
    }
    public GroupNodeViewModel(String name, boolean isRoot, int hits, IconTheme.JabRefIcon icon, boolean isLeaf) {
        this(name, isRoot, hits, icon.getCode(), isLeaf);
    }
    private GroupNodeViewModel(String name, boolean isRoot, int hits, String iconCode, boolean isLeaf) {
        this.name = name;
        this.isRoot = isRoot;
        this.iconCode = iconCode;
        this.isLeaf = isLeaf;
        this.hits = new SimpleObjectProperty<>(hits);
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

    public ObservableValue<Integer> getHits() {
        return hits;
    }

    public String getIconCode() {
        return iconCode;
    }

    public boolean isLeaf() {
        return isLeaf;
    }

    public ObservableList<GroupNodeViewModel> getChildren() {
        return children;
    }
}
