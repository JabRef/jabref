package org.jabref.gui.tooltip;

import org.jabref.gui.maintable.BibEntryTableViewModel;
import org.jabref.gui.maintable.MainTableColumnModel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(ApplicationExtension.class)
public class ToolTipDataModelTest {
    private static String testQualifier = "author";
    private static String testName = "field:author";
    private static MainTableColumnModel.Type testType = MainTableColumnModel.Type.NORMALFIELD;

    @Test
    public void testNullCallBackReturnsNoTooltip() {

//        MainTableColumnModel model = new MainTableColumnModel(MainTableColumnModel.Type.NORMALFIELD);
//        FieldColumn testColumnModel = new FieldColumn(model);
//        Map<ObservableValue<String>, String> map = new HashMap();
//        map.put(new ReadOnlyObjectWrapper<>("FieldTEXT"), "FieldDTOOLTIP");
//        TableColumn column = new TableColumn<>("FieldTEXT");
//
//        testColumnModel.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper(cellData.getValue().getBibPreview(testColumnModel.getFieldValue(cellData.getValue()))));
//        ValueTableCellFactory<BibEntryTableViewModel, Map<ObservableValue<String>, String>> factory = new ValueTableCellFactory<>();
//        factory.withTooltip(testColumnModel.createTooltip(map));
//        Map<ObservableValue<String>, String> map = new HashMap();
//        ValueTableCellFactory<BibEntryTableViewModel, Map<ObservableValue<String>, String>> factory = new ValueTableCellFactory<>();
//        factory.withTooltip();
//        factory.install(tableView);
//        assertEquals(null, tableView.getTooltip());
    }

    @Test
    public void testThatToolTipIsAlwaysTurnedOffByDefault() {
        assertFalse(BibEntryTableViewModel.showTooltipProperty().get());
    }
}

