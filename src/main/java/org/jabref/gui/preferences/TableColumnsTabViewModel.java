package org.jabref.gui.preferences;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.preferences.JabRefPreferences;

public class TableColumnsTabViewModel implements PreferenceTabViewModel {

    private final ListProperty<TableColumnsNodeViewModel> columnsNamesProperty = new SimpleListProperty<>(FXCollections.observableArrayList());
    private final ObservableList<TableColumnsNodeViewModel> checkedColumns = FXCollections.observableArrayList();

    private final SimpleBooleanProperty specialFieldsEnabledProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldRankingProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldQualityProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldPriorityProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldRelevanceProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldPrintedProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldReadStatusProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldsSyncKeyWordsProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty specialFieldsWriteProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty identifierFieldsEnabledProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty urlFieldEnabledProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty preferUrlProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty arxivFieldProperty = new SimpleBooleanProperty();

    private final SimpleBooleanProperty fileFieldProperty = new SimpleBooleanProperty();
    private final SimpleBooleanProperty extraFileFieldsEnabledProperty = new SimpleBooleanProperty();

    private final DialogService dialogService;
    private final JabRefPreferences preferences;
    private final JabRefFrame frame;

    public TableColumnsTabViewModel(DialogService dialogService, JabRefPreferences preferences, JabRefFrame frame) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.frame = frame;

        setValues();

        specialFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> setValues());
        identifierFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> setValues());
        extraFileFieldsEnabledProperty.addListener((observable, oldValue, newValue) -> setValues());
    }

    @Override
    public void setValues() {
        List<String> prefColNames = this.preferences.getStringList(JabRefPreferences.COLUMN_NAMES);
        List<String> prefColWidths = this.preferences.getStringList(JabRefPreferences.COLUMN_WIDTHS);
        this.columnsNamesProperty.getValue().clear();
        this.checkedColumns.clear();
        for (int i = 0; i < prefColNames.size(); i++) {
            TableColumnsNodeViewModel column = new TableColumnsNodeViewModel(
                    FieldFactory.parseField(prefColNames.get(i)),
                    Double.parseDouble(prefColWidths.get(i)));
            this.columnsNamesProperty.getValue().add(column);
            this.checkedColumns.add(column);
        }

        Set<TableColumnsNodeViewModel> fields = new HashSet<>();

        // Internal Fields

        fields.add(new TableColumnsNodeViewModel(InternalField.OWNER));
        fields.add(new TableColumnsNodeViewModel(InternalField.TIMESTAMP));
        fields.add(new TableColumnsNodeViewModel(InternalField.GROUPS));
        fields.add(new TableColumnsNodeViewModel(InternalField.KEY_FIELD));
        fields.add(new TableColumnsNodeViewModel(InternalField.TYPE_HEADER));

        // Special Fields

        specialFieldsEnabledProperty.setValue(preferences.getBoolean(JabRefPreferences.SPECIALFIELDSENABLED));
        specialFieldsSyncKeyWordsProperty.setValue(preferences.getBoolean(JabRefPreferences.AUTOSYNCSPECIALFIELDSTOKEYWORDS));
        specialFieldsWriteProperty.setValue(preferences.getBoolean(JabRefPreferences.SERIALIZESPECIALFIELDS));

        specialFieldRankingProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_RANKING));
        specialFieldQualityProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_QUALITY));
        specialFieldPriorityProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_PRIORITY));
        specialFieldRelevanceProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_RELEVANCE));
        specialFieldPrintedProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_PRINTED));
        specialFieldReadStatusProperty.setValue(preferences.getBoolean(JabRefPreferences.SHOWCOLUMN_READ));
        specialFieldsSyncKeyWordsProperty.setValue(preferences.getBoolean(JabRefPreferences.AUTOSYNCSPECIALFIELDSTOKEYWORDS));
        specialFieldsWriteProperty.setValue(preferences.getBoolean(JabRefPreferences.SERIALIZESPECIALFIELDS));

        if (specialFieldsEnabledProperty.getValue()) {
            EnumSet.allOf(SpecialField.class).forEach(specialField -> {
                TableColumnsNodeViewModel column = new TableColumnsNodeViewModel(specialField);
                fields.add(column);
                if (specialField == SpecialField.RANKING && specialFieldRankingProperty.getValue()) {
                    checkedColumns.add(column);
                } else if (specialField == SpecialField.QUALITY && specialFieldQualityProperty.getValue()) {
                    checkedColumns.add(column);
                } else if (specialField == SpecialField.PRIORITY && specialFieldPriorityProperty.getValue()) {
                    checkedColumns.add(column);
                } else if (specialField == SpecialField.RELEVANCE && specialFieldRelevanceProperty.getValue()) {
                    checkedColumns.add(column);
                } else if (specialField == SpecialField.PRINTED && specialFieldPrintedProperty.getValue()) {
                    checkedColumns.add(column);
                } else if (specialField == SpecialField.READ_STATUS && specialFieldReadStatusProperty.getValue()) {
                    checkedColumns.add(column);
                }
            });

            checkedColumns.addAll(
                    specialFieldRankingProperty.getValue() ? new TableColumnsNodeViewModel(SpecialField.RANKING) : null,
                    specialFieldQualityProperty.getValue() ? new TableColumnsNodeViewModel(SpecialField.QUALITY) : null,
                    specialFieldPriorityProperty.getValue() ? new TableColumnsNodeViewModel(SpecialField.PRIORITY) : null,
                    specialFieldRelevanceProperty.getValue() ? new TableColumnsNodeViewModel(SpecialField.RELEVANCE) : null,
                    specialFieldPrintedProperty.getValue() ? new TableColumnsNodeViewModel(SpecialField.PRINTED) : null,
                    specialFieldReadStatusProperty.getValue() ? new TableColumnsNodeViewModel(SpecialField.READ_STATUS) : null
            );
        }

        // Identifier Fields

        identifierFieldsEnabledProperty.setValue(true);
        urlFieldEnabledProperty.setValue(preferences.getBoolean(JabRefPreferences.URL_COLUMN));
        preferUrlProperty.setValue(!preferences.getBoolean(JabRefPreferences.PREFER_URL_DOI));
        arxivFieldProperty.setValue(preferences.getBoolean(JabRefPreferences.ARXIV_COLUMN));
        // ToDo
        fields.add(new TableColumnsNodeViewModel(new UnknownField("url")));

        // File Fields

        fileFieldProperty.setValue(preferences.getBoolean(JabRefPreferences.FILE_COLUMN));
        extraFileFieldsEnabledProperty.setValue(preferences.getBoolean(JabRefPreferences.EXTRA_FILE_COLUMNS));
        // ToDo

        EnumSet.allOf(StandardField.class).forEach(TableColumnsNodeViewModel::new);

        insertColumns(fields);
    }

    private void insertColumns(Set<TableColumnsNodeViewModel> fields) {
        fields.stream()
                .filter(field -> columnsNamesProperty.getValue().filtered(item -> item.getName().equals(field.getName())).isEmpty())
                .forEach(columnsNamesProperty::add);
    }

    @Override
    public void storeSettings() {

    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    public ListProperty<TableColumnsNodeViewModel> columnsNamesProperty() { return this.columnsNamesProperty; }

    public ObservableList<TableColumnsNodeViewModel> getCheckedColumns() { return new ReadOnlyListWrapper<>(checkedColumns); }

    public SimpleBooleanProperty specialFieldsEnabledProperty() { return this.specialFieldsEnabledProperty; }

    public SimpleBooleanProperty identifierFieldsEnabledProperty() { return this.identifierFieldsEnabledProperty; }

    public SimpleBooleanProperty extraFieldsEnabledProperty() { return this.extraFileFieldsEnabledProperty; }
}
