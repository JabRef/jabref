package org.jabref.model.groups;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.search.SearchMatcher;
import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NonNull;

/**
 * Base class for all groups.
 */
public abstract class AbstractGroup implements SearchMatcher {

    /**
     * The group's name.
     */
    protected final StringProperty name = new SimpleStringProperty();
    /**
     * The hierarchical context of the group.
     */
    protected final GroupHierarchyType context;
    // group color stored as a String (e.g., hex like "#RRGGBB" or any CSS-compatible representation)
    protected Optional<String> color = Optional.empty();
    protected boolean isExpanded = true;
    protected Optional<String> description = Optional.empty();
    protected Optional<String> iconName = Optional.empty();

    protected AbstractGroup(String name, @NonNull GroupHierarchyType context) {
        this.name.setValue(name);
        this.context = context;
    }

    @Override
    public String toString() {
        return "AbstractGroup{" +
                "name='" + name.getValue() + '\'' +
                ", context=" + context +
                ", color=" + color +
                ", isExpanded=" + isExpanded +
                ", description=" + description +
                ", iconName=" + iconName +
                '}';
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if ((other == null) || (getClass() != other.getClass())) {
            return false;
        }
        AbstractGroup that = (AbstractGroup) other;
        return Objects.equals(this.name.getValue(), that.name.getValue()) && Objects.equals(this.description, that.description)
                && Objects.equals(this.context, that.context);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.getValue(), description, context);
    }

    public Optional<String> getColor() {
        return color;
    }

    /**
     * Sets the group's color string. Pass null or blank to clear.
     */
    public void setColor(String colorString) {
        if (StringUtil.isBlank(colorString)) {
            this.color = Optional.empty();
        } else {
            this.color = Optional.of(colorString);
        }
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (StringUtil.isBlank(description)) {
            this.description = Optional.empty();
        } else {
            this.description = Optional.of(description);
        }
    }

    public Optional<String> getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        if (StringUtil.isBlank(iconName)) {
            this.iconName = Optional.empty();
        } else {
            this.iconName = Optional.of(iconName);
        }
    }

    /**
     * Returns the way this group relates to its sub- or supergroup.
     */
    public GroupHierarchyType getHierarchicalContext() {
        return context;
    }

    /**
     * Returns this group's name, e.g. for display in a list/tree.
     */
    public final String getName() {
        return name.getValue();
    }

    public StringProperty nameProperty() {
        return name;
    }

    /**
     * @return true if this group contains the specified entry, false otherwise.
     */
    public abstract boolean contains(BibEntry entry);

    @Override
    public boolean isMatch(BibEntry entry) {
        return contains(entry);
    }

    /**
     * @return true if this group contains any of the specified entries, false otherwise.
     */
    public boolean containsAny(List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            if (contains(entry)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return true if this group contains all of the specified entries, false otherwise.
     */
    public boolean containsAll(List<BibEntry> entries) {
        for (BibEntry entry : entries) {
            if (!contains(entry)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true if this group is dynamic, i.e. uses a search definition or
     * equiv. that might match new entries, or false if this group contains a
     * fixed set of entries and thus will never match a new entry that was not
     * explicitly added to it.
     */
    public abstract boolean isDynamic();

    /**
     * @return A deep copy of this object.
     */
    public abstract AbstractGroup deepCopy();
}
