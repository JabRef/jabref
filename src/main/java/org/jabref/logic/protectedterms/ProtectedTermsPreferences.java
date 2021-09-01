package org.jabref.logic.protectedterms;

import java.util.List;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class ProtectedTermsPreferences {

    private final ObservableList<String> enabledInternalTermLists;
    private final ObservableList<String> enabledExternalTermLists;
    private final ObservableList<String> disabledInternalTermLists;
    private final ObservableList<String> disabledExternalTermLists;

    public ProtectedTermsPreferences(List<String> enabledInternalTermLists,
                                     List<String> enabledExternalTermLists,
                                     List<String> disabledInternalTermLists,
                                     List<String> disabledExternalTermLists) {
        this.enabledInternalTermLists = FXCollections.observableArrayList(enabledInternalTermLists);
        this.disabledInternalTermLists = FXCollections.observableArrayList(disabledInternalTermLists);
        this.enabledExternalTermLists = FXCollections.observableArrayList(enabledExternalTermLists);
        this.disabledExternalTermLists = FXCollections.observableArrayList(disabledExternalTermLists);
    }

    public ObservableList<String> getEnabledInternalTermLists() {
        return FXCollections.unmodifiableObservableList(enabledInternalTermLists);
    }

    public ObservableList<String> getEnabledExternalTermLists() {
        return FXCollections.unmodifiableObservableList(enabledExternalTermLists);
    }

    public ObservableList<String> getDisabledInternalTermLists() {
        return FXCollections.unmodifiableObservableList(disabledInternalTermLists);
    }

    public ObservableList<String> getDisabledExternalTermLists() {
        return FXCollections.unmodifiableObservableList(disabledExternalTermLists);
    }

    public void setEnabledInternalTermLists(List<String> list) {
        enabledInternalTermLists.clear();
        enabledInternalTermLists.addAll(list);
    }

    public void setEnabledExternalTermLists(List<String> list) {
        enabledExternalTermLists.clear();
        enabledExternalTermLists.addAll(list);
    }

    public void setDisabledInternalTermLists(List<String> list) {
        disabledInternalTermLists.clear();
        disabledInternalTermLists.addAll(list);
    }

    public void setDisabledExternalTermLists(List<String> list) {
        disabledExternalTermLists.clear();
        disabledExternalTermLists.addAll(list);
    }
}
