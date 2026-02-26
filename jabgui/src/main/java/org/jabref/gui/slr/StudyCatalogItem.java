package org.jabref.gui.slr;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.NonNull;

/// View representation of {@link org.jabref.model.study.StudyCatalog}
public class StudyCatalogItem {
    private final StringProperty name;
    private final BooleanProperty enabled;
    private final StringProperty reason;

    public StudyCatalogItem(@NonNull String name, boolean enabled) {
        this(name, enabled, "");
    }

    public StudyCatalogItem(@NonNull String name, boolean enabled, String reason) {
        this.name = new SimpleStringProperty(name);
        this.enabled = new SimpleBooleanProperty(enabled);
        this.reason = new SimpleStringProperty(reason != null ? reason : "");
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

    public String getReason() {
        return reason.getValue();
    }

    public void setReason(String reason) {
        this.reason.setValue(reason);
    }

    public StringProperty reasonProperty() {
        return reason;
    }

    @Override
    public String toString() {
        return "StudyCatalogItem{" +
                "name=" + name.get() +
                ", enabled=" + enabled.get() +
                ", reason=" + reason.get() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StudyCatalogItem that = (StudyCatalogItem) o;
        return Objects.equals(getName(), that.getName()) &&
                Objects.equals(isEnabled(), that.isEnabled()) &&
                Objects.equals(getReason(), that.getReason());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), isEnabled(), getReason());
    }
}
