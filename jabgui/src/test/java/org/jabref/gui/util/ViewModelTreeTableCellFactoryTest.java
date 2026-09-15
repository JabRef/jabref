package org.jabref.gui.util;

import java.lang.reflect.Method;

import javafx.scene.control.Cell;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;

import org.jabref.gui.testutils.JavaFxExtension;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(JavaFxExtension.class)
@NullMarked
class ViewModelTreeTableCellFactoryTest {

    private void updateItem(TreeTableCell<String, String> cell, @Nullable String item, boolean empty) throws Exception {
        Method updateItemMethod = Cell.class.getDeclaredMethod("updateItem", Object.class, boolean.class);
        updateItemMethod.setAccessible(true);
        updateItemMethod.invoke(cell, item, empty);
    }

    @Test
    void tooltipSetWhenDescriptionPresent() throws Exception {
        ViewModelTreeTableCellFactory<String> factory = new ViewModelTreeTableCellFactory<String>()
                .withTooltip(vm -> "A description");

        TreeTableCell<String, String> cell = factory.call(new TreeTableColumn<>());
        updateItem(cell, "GroupWithDescription", false);

        assertNotNull(cell.getTooltip());
        assertEquals("A description", cell.getTooltip().getText());
    }

    @Test
    void tooltipClearedWhenDescriptionBlank() throws Exception {
        ViewModelTreeTableCellFactory<String> factory = new ViewModelTreeTableCellFactory<String>()
                .withTooltip(vm -> {
                    if ("HasDescription".equals(vm)) {
                        return "A description";
                    }
                    return "";
                });

        TreeTableCell<String, String> cell = factory.call(new TreeTableColumn<>());

        // First: cell shows a group with a description
        updateItem(cell, "HasDescription", false);
        assertNotNull(cell.getTooltip());
        assertEquals("A description", cell.getTooltip().getText());

        // Then: cell is recycled to show a group without a description
        updateItem(cell, "NoDescription", false);
        assertNull(cell.getTooltip());
    }

    @Test
    void tooltipClearedWhenCellBecomesEmpty() throws Exception {
        ViewModelTreeTableCellFactory<String> factory = new ViewModelTreeTableCellFactory<String>()
                .withTooltip(vm -> "A description");

        TreeTableCell<String, String> cell = factory.call(new TreeTableColumn<>());

        // First: cell shows a group with a description
        updateItem(cell, "GroupWithDescription", false);
        assertNotNull(cell.getTooltip());

        // Then: cell is recycled and becomes empty
        updateItem(cell, null, true);
        assertNull(cell.getTooltip());
    }

    @Test
    void tooltipClearedWhenDescriptionNull() throws Exception {
        ViewModelTreeTableCellFactory<String> factory = new ViewModelTreeTableCellFactory<String>()
                .withTooltip(vm -> {
                    if ("HasDescription".equals(vm)) {
                        return "A description";
                    }
                    return null;
                });

        TreeTableCell<String, String> cell = factory.call(new TreeTableColumn<>());

        // First: cell shows a group with a description
        updateItem(cell, "HasDescription", false);
        assertNotNull(cell.getTooltip());

        // Then: cell is recycled to show a group that returns null
        updateItem(cell, "NullDescription", false);
        assertNull(cell.getTooltip());
    }
}
