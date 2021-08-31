package org.jabref.logic.protectedterms;

import java.util.List;

import javafx.beans.property.ListProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ProtectedTermsPreferences {

    private final ListProperty<String> enabledInternalTermLists;
    private final ListProperty<String> enabledExternalTermLists;
    private final ListProperty<String> disabledInternalTermLists;
    private final ListProperty<String> disabledExternalTermLists;

    public ProtectedTermsPreferences(List<String> enabledInternalTermLists,
                                     List<String> enabledExternalTermLists,
                                     List<String> disabledInternalTermLists,
                                     List<String> disabledExternalTermLists) {
        this.enabledInternalTermLists = new SimpleListProperty<>(FXCollections.observableArrayList(enabledInternalTermLists));
        this.disabledInternalTermLists = new SimpleListProperty<>(FXCollections.observableArrayList(disabledInternalTermLists));
        this.enabledExternalTermLists = new SimpleListProperty<>(FXCollections.observableArrayList(enabledExternalTermLists));
        this.disabledExternalTermLists = new SimpleListProperty<>(FXCollections.observableArrayList(disabledExternalTermLists));
    }

    public ObservableList<String> getEnabledInternalTermLists() {
        return enabledInternalTermLists.get();
    }

    public ListProperty<String> enabledInternalTermListsProperty() {
        return enabledInternalTermLists;
    }

    public void setEnabledInternalTermLists(List<String> enabledInternalTermLists) {
        this.enabledInternalTermLists.set(FXCollections.observableArrayList(enabledInternalTermLists));
    }

    public ObservableList<String> getEnabledExternalTermLists() {
        return enabledExternalTermLists.get();
    }

    public ListProperty<String> enabledExternalTermListsProperty() {
        return enabledExternalTermLists;
    }

    public void setEnabledExternalTermLists(List<String> enabledExternalTermLists) {
        this.enabledExternalTermLists.set(FXCollections.observableArrayList(enabledExternalTermLists));
    }

    public ObservableList<String> getDisabledInternalTermLists() {
        return disabledInternalTermLists.get();
    }

    public ListProperty<String> disabledInternalTermListsProperty() {
        return disabledInternalTermLists;
    }

    public void setDisabledInternalTermLists(List<String> disabledInternalTermLists) {
        this.disabledInternalTermLists.set(FXCollections.observableArrayList(disabledInternalTermLists));
    }

    public ObservableList<String> getDisabledExternalTermLists() {
        return disabledExternalTermLists.get();
    }

    public ListProperty<String> disabledExternalTermListsProperty() {
        return disabledExternalTermLists;
    }

    public void setDisabledExternalTermLists(List<String> disabledExternalTermLists) {
        this.disabledExternalTermLists.set(FXCollections.observableArrayList(disabledExternalTermLists));
    }
}
