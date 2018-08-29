package org.jabref.gui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;

import org.jabref.Globals;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.preferences.JabRefPreferences;
import org.jabref.preferences.PreviewPreferences;

import org.fxmisc.easybind.EasyBind;

public class BasePanelPreferences {
    private MainTablePreferences tablePreferences;
    private AutoCompletePreferences autoCompletePreferences;
    private EntryEditorPreferences entryEditorPreferences;
    private KeyBindingRepository keyBindings;
    private PreviewPreferences previewPreferences;
    private DoubleProperty entryEditorDividerPosition = new SimpleDoubleProperty();

    public BasePanelPreferences(MainTablePreferences tablePreferences, AutoCompletePreferences autoCompletePreferences, EntryEditorPreferences entryEditorPreferences, KeyBindingRepository keyBindings, PreviewPreferences previewPreferences, Double entryEditorDividerPosition) {
        this.tablePreferences = tablePreferences;
        this.autoCompletePreferences = autoCompletePreferences;
        this.entryEditorPreferences = entryEditorPreferences;
        this.keyBindings = keyBindings;
        this.previewPreferences = previewPreferences;
        this.entryEditorDividerPosition.setValue(entryEditorDividerPosition);
    }

    public static BasePanelPreferences from(JabRefPreferences preferences) {
        BasePanelPreferences basePanelPreferences = new BasePanelPreferences(
                preferences.getMainTablePreferences(),
                preferences.getAutoCompletePreferences(),
                preferences.getEntryEditorPreferences(),
                Globals.getKeyPrefs(),
                preferences.getPreviewPreferences(),
                preferences.getDouble(JabRefPreferences.ENTRY_EDITOR_HEIGHT));
        EasyBind.subscribe(basePanelPreferences.entryEditorDividerPosition, value -> preferences.putDouble(JabRefPreferences.ENTRY_EDITOR_HEIGHT, value.doubleValue()));
        return basePanelPreferences;
    }

    public double getEntryEditorDividerPosition() {
        return entryEditorDividerPosition.get();
    }

    public void setEntryEditorDividerPosition(double entryEditorDividerPosition) {
        this.entryEditorDividerPosition.set(entryEditorDividerPosition);
    }

    public DoubleProperty entryEditorDividerPositionProperty() {
        return entryEditorDividerPosition;
    }

    public MainTablePreferences getTablePreferences() {
        return tablePreferences;
    }

    public AutoCompletePreferences getAutoCompletePreferences() {
        return autoCompletePreferences;
    }

    public void setAutoCompletePreferences(AutoCompletePreferences autoCompletePreferences) {
        this.autoCompletePreferences = autoCompletePreferences;
    }

    public EntryEditorPreferences getEntryEditorPreferences() {
        return entryEditorPreferences;
    }

    public KeyBindingRepository getKeyBindings() {
        return keyBindings;
    }

    public PreviewPreferences getPreviewPreferences() {
        return previewPreferences;
    }
}
