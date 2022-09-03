package org.jabref.gui.slr;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jabref.model.study.StudyDatabase;

/**
 * View representation of {@link StudyDatabase}
 */
public class StudyDatabaseItem {
    private final StringProperty name;
    private final BooleanProperty enabled;

    public StudyDatabaseItem(String name, boolean enabled) {
        this.name = new SimpleStringProperty(Objects.requireNonNull(name));
        this.enabled = new SimpleBooleanProperty(enabled);
    }

    public String getName() {
        return name.getValue();
    }

    public void setName(String name) {
        this.name.setValue(name);
    }

    public StringProperty nameProperty() {
        return name;
    }

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.setValue(enabled);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
    }

    @Override
    public String toString() {
        return "StudyDatabaseItem{" +
                "name=" + name.get() +
                ", enabled=" + enabled.get() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        StudyDatabaseItem that = (StudyDatabaseItem) o;
        return Objects.equals(getName(), that.getName()) && Objects.equals(isEnabled(), that.isEnabled());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), isEnabled());
    }
}
