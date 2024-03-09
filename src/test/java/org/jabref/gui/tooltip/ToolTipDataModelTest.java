package org.jabref.gui.tooltip;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.TableColumn;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.columns.FieldColumn;
import org.jabref.gui.util.ValueTableCellFactory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(ApplicationExtension.class)
public class ToolTipDataModelTest {

    @Test
    public void testNullCallBackReturnsNoToolTipForCell() {
        ValueTableCellFactory<BibEntryTableViewModel, Map<ObservableValue<String>, String>> factory = new ValueTableCellFactory<>();
        TableColumn<BibEntryTableViewModel, Map<ObservableValue<String>, String>> column = new TableColumn<>();
        factory.withTooltip((Function<Map<ObservableValue<String>, String>, String>) null);
        factory.install(column);
        assertEquals(null, column.getTableView());
    }

    @Test
    public void testExtractFieldValueWithEmptyValuesMap() {
        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
        FieldColumn testColumnModel = new FieldColumn(model);
        Map<ObservableValue<String>, String> values = new HashMap<>();
        String fieldText = testColumnModel.extractFieldValue(values);
        assertEquals("", fieldText);
    }

    @Test
    public void testExtractFieldValueWithSingleValuePair() {
        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
        FieldColumn testColumnModel = new FieldColumn(model);
        ObservableValue<String> observableValue = new SimpleStringProperty("Field");
        Map<ObservableValue<String>, String> values = new HashMap<>();
        values.put(observableValue, "Value");

        String fieldText = testColumnModel.extractFieldValue(values);

        assertEquals("Field", fieldText);
    }

    @Test
    public void testExtractFieldValueWithMultipleValuePairs() {
        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
        FieldColumn testColumnModel = new FieldColumn(model);
        ObservableValue<String> observableValue1 = new SimpleStringProperty("Field1");
        ObservableValue<String> observableValue2 = new SimpleStringProperty("Field2");
        Map<ObservableValue<String>, String> values = new HashMap<>();
        values.put(observableValue1, "Value1");
        values.put(observableValue2, "Value2");

        String fieldText = testColumnModel.extractFieldValue(values);

        assertTrue(fieldText.equals("Field1") || fieldText.equals("Field2"));
    }

    @Test
    public void testCreateTooltipWithEmptyValuesMap() {
        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
        FieldColumn testColumnModel = new FieldColumn(model);
        Map<ObservableValue<String>, String> values = new HashMap<>();

        String tooltip = testColumnModel.createTooltip(values);

        assertEquals("", tooltip);
    }

    @Test
    public void testCreateTooltipWithSingleValuePair() {
        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
        FieldColumn testColumnModel = new FieldColumn(model);
        ObservableValue<String> observableValue = new SimpleStringProperty("Field");
        Map<ObservableValue<String>, String> values = new HashMap<>();
        values.put(observableValue, "Preview");

        String tooltip = testColumnModel.createTooltip(values);

        assertEquals("Field\n\nPreview", tooltip);
    }

    @Test
    public void testCreateTooltipWithMultipleValuePairs() {
        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
        FieldColumn testColumnModel = new FieldColumn(model);
        ObservableValue<String> observableValue1 = new SimpleStringProperty("Field1");
        ObservableValue<String> observableValue2 = new SimpleStringProperty("Field2");
        Map<ObservableValue<String>, String> values = new HashMap<>();
        values.put(observableValue1, "Preview1");
        values.put(observableValue2, "Preview2");

        String tooltip = testColumnModel.createTooltip(values);

        String expected1 = "Field1\n\nPreview2\n\nPreview1";
        String expected2 = "Field2\n\nPreview1\n\nPreview2";

        assertTrue(tooltip.equals(expected1) || tooltip.equals(expected2));
    }

    @Test
    public void testThatToolTipForEntireEntryIsAlwaysTurnedOffByDefault() {
        assertFalse(BibEntryTableViewModel.showTooltipProperty().get());
    }
}

