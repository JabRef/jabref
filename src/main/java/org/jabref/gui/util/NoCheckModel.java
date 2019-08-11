package org.jabref.gui.util;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.controlsfx.control.IndexedCheckModel;

public class NoCheckModel<T> implements IndexedCheckModel<T> {
    @Override
    public int getItemCount() { return 0; }

    @Override
    public ObservableList<T> getCheckedItems() { return FXCollections.emptyObservableList(); }

    @Override
    public void checkAll() {
    }

    @Override
    public void clearCheck(T item) {
    }

    @Override
    public void clearChecks() {
    }

    @Override
    public boolean isEmpty() { return true; }

    @Override
    public boolean isChecked(T item) { return false; }

    @Override
    public void check(T item) {
    }

    @Override
    public void toggleCheckState(T item) {
    }

    @Override
    public T getItem(int index) { return null; }

    @Override
    public int getItemIndex(T item) { return 0; }

    @Override
    public ObservableList<Integer> getCheckedIndices() { return FXCollections.emptyObservableList(); }

    @Override
    public void checkIndices(int... indices) {
    }

    @Override
    public void clearCheck(int index) {
    }

    @Override
    public boolean isChecked(int index) { return false; }

    @Override
    public void check(int index) {
    }

    @Override
    public void toggleCheckState(int index) {
    }
}
