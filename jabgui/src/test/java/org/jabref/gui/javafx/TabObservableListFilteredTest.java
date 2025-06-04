package org.jabref.gui.javafx;

import java.util.List;

import javafx.collections.transformation.FilteredList;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

import com.sun.javafx.scene.control.TabObservableList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@ExtendWith(ApplicationExtension.class)
public class TabObservableListFilteredTest {

    @Test
    public void filteredListDetectsPermutation() {
        Tab lib1 = new Tab("lib1");
        Tab lib2 = new Tab("lib2");
        Tab other = new Tab("other");

        TabPane tabPane = new TabPane(lib1, other, lib2);
        TabObservableList<Tab> base = (TabObservableList) tabPane.getTabs();

        FilteredList<Tab> filtered = new FilteredList<>(base, tab -> tab.getText().startsWith("lib"));

        assertEquals(List.of(lib1, lib2), filtered);

        base.reorder(lib1, other);

        assertNotEquals(other, filtered.getFirst()); // This should not fail

        assertEquals(List.of(lib2, lib1), filtered);
    }
}
