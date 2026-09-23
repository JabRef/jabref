package org.jabref.gui.slr;

import java.util.Objects;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import org.jspecify.annotations.NullMarked;

/// View representation of [org.jabref.model.study.StudyCatalog]
@NullMarked
public class StudyCatalogItem {
    private final StringProperty name;
    private final BooleanProperty enabled;
    private final StringProperty reason;
    private final StringProperty nativeQuery;

    public StudyCatalogItem(String name, boolean enabled) {
        this(name, enabled, "");
    }

    public StudyCatalogItem(String name, boolean enabled, String reason) {
        this(name, enabled, reason, "");
    }

    public StudyCatalogItem(String name, boolean enabled, String reason, String nativeQuery) {
        this.name = new SimpleStringProperty(name);
        this.enabled = new SimpleBooleanProperty(enabled);
        this.reason = new SimpleStringProperty(reason);
        this.nativeQuery = new SimpleStringProperty(nativeQuery);
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

    public String getNativeQuery() {
        return nativeQuery.getValue();
    }

    public void setNativeQuery(String nativeQuery) {
        this.nativeQuery.setValue(nativeQuery);
    }

    public StringProperty nativeQueryProperty() {
        return nativeQuery;
    }

    @Override
    public String toString() {
        return "StudyCatalogItem{" +
                "name=" + name.get() +
                ", enabled=" + enabled.get() +
                ", reason=" + reason.get() +
                ", nativeQuery=" + nativeQuery.get() +
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
                Objects.equals(getReason(), that.getReason()) &&
                Objects.equals(getNativeQuery(), that.getNativeQuery());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), isEnabled(), getReason(), getNativeQuery());
    }
}
