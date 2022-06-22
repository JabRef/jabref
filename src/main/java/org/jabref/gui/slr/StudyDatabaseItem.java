package org.jabref.gui.slr;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class StudyDatabaseItem {
    private final StringProperty name;
    private final BooleanProperty enabled;

    public StudyDatabaseItem(String name, boolean enabled) {
        this.name = new SimpleStringProperty(name);
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StudyDatabaseItem that = (StudyDatabaseItem) o;

        if (isEnabled() != that.isEnabled()) {
            return false;
        }
        return getName() != null ? getName().equals(that.getName()) : that.getName() == null;
    }

    @Override
    public int hashCode() {
        int result = getName() != null ? getName().hashCode() : 0;
        result = 31 * result + (isEnabled() ? 1 : 0);
        return result;
    }
}
