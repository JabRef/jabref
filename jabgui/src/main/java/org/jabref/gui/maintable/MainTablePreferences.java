package org.jabref.gui.maintable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class MainTablePreferences {

    
    public static final String DEFAULT_COLUMN_NAMES = "groups;group_icons;files;linked_id;field:citationkey;field:entrytype;field:author/editor;field:title;field:year;field:journal/booktitle;special:ranking;special:readstatus;special:priority";
    public static final String DEFAULT_COLUMN_WIDTHS = "28;40;28;28;100;75;300;470;60;130;50;50;50";

    private final ColumnPreferences columnPreferences;
    private final BooleanProperty resizeColumnsToFit = new SimpleBooleanProperty();
    private final BooleanProperty extraFileColumnsEnabled = new SimpleBooleanProperty();

    // Private Default Constructor
    private MainTablePreferences() {
        this(
            ColumnPreferences.getDefault(),
            false, 
            false
        );
    }

    // Existing Constructor
    public MainTablePreferences(ColumnPreferences columnPreferences,
                                boolean resizeColumnsToFit,
                                boolean extraFileColumnsEnabled) {
        this.columnPreferences = columnPreferences;
        this.resizeColumnsToFit.set(resizeColumnsToFit);
        this.extraFileColumnsEnabled.set(extraFileColumnsEnabled);
    }

    
    public static MainTablePreferences getDefault() {
        return new MainTablePreferences();
    }

    public void setAll(MainTablePreferences other) {
        this.resizeColumnsToFit.set(other.getResizeColumnsToFit());
        this.extraFileColumnsEnabled.set(other.getExtraFileColumnsEnabled());
        
        if (this.columnPreferences != null) {
            this.columnPreferences.setAll(other.getColumnPreferences());
        }
    }

    // --- Getters and Setters ---

    public ColumnPreferences getColumnPreferences() {
        return columnPreferences;
    }

    public boolean getResizeColumnsToFit() {
        return resizeColumnsToFit.get();
    }

    public BooleanProperty resizeColumnsToFitProperty() {
        return resizeColumnsToFit;
    }

    public void setResizeColumnsToFit(boolean resizeColumnsToFit) {
        this.resizeColumnsToFit.set(resizeColumnsToFit);
    }

    public boolean getExtraFileColumnsEnabled() {
        return extraFileColumnsEnabled.get();
    }

    public BooleanProperty extraFileColumnsEnabledProperty() {
        return extraFileColumnsEnabled;
    }

    public void setExtraFileColumnsEnabled(boolean extraFileColumnsEnabled) {
        this.extraFileColumnsEnabled.set(extraFileColumnsEnabled);
    }
}