package org.jabref.gui.util;

import java.util.Optional;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;

import org.jabref.gui.testutils.JavaFxTest;

import de.saxsys.mvvmfx.utils.validation.ValidationMessage;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@NullMarked
class ViewModelListCellFactoryTest extends JavaFxTest {

    @Test
    void defaultAndCallbackTooltipPathsHandleMissingText() {
        interact(() -> {
            ListView<String> listView = new ListView<>();
            listView.getItems().setAll("file.pdf", "", "unknown");

            ListCell<String> defaultCell = new ViewModelListCellFactory<String>().call(listView);
            defaultCell.updateListView(listView);
            defaultCell.updateIndex(0);
            assertNull(defaultCell.getTooltip());

            ListCell<String> stringCell = new ViewModelListCellFactory<String>()
                    .withStringTooltip(item -> "unknown".equals(item) ? null : item)
                    .call(listView);
            stringCell.updateListView(listView);
            stringCell.updateIndex(0);
            assertEquals("file.pdf", stringCell.getTooltip().getText());
            stringCell.updateIndex(1);
            assertNull(stringCell.getTooltip());
            stringCell.updateIndex(2);
            assertNull(stringCell.getTooltip());

            ListCell<String> customCell = new ViewModelListCellFactory<String>()
                    .withStringTooltip(item -> item)
                    .withTooltip(item -> item.isEmpty() ? null : new Tooltip("Custom " + item))
                    .call(listView);
            customCell.updateListView(listView);
            customCell.updateIndex(0);
            assertEquals("Custom file.pdf", customCell.getTooltip().getText());
            customCell.updateIndex(1);
            assertNull(customCell.getTooltip());
        });
    }

    @Test
    void tooltipTracksLinkAndDescriptionChanges() {
        interact(() -> {
            StringProperty link = spy(new SimpleStringProperty("original.pdf"));
            StringProperty description = spy(new SimpleStringProperty("Attachment"));
            ListView<StringProperty> listView = new ListView<>();
            listView.getItems().add(link);
            ListCell<StringProperty> cell = new ViewModelListCellFactory<StringProperty>()
                    .withTooltipText(item -> item.flatMap(path -> description.map(text -> text + " (" + path + ")")))
                    .call(listView);
            cell.updateListView(listView);
            cell.updateIndex(0);

            assertEquals("Attachment (original.pdf)", cell.getTooltip().getText());

            link.set("renamed.pdf");
            assertEquals("Attachment (renamed.pdf)", cell.getTooltip().getText());

            link.set("directory/renamed.pdf");
            assertEquals("Attachment (directory/renamed.pdf)", cell.getTooltip().getText());

            description.set("Updated attachment");
            assertEquals("Updated attachment (directory/renamed.pdf)", cell.getTooltip().getText());

            cell.updateIndex(-1);
            assertAllListenersRemoved(link);
            assertAllListenersRemoved(description);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void detachesTooltipListenersWhenCellIsClearedOrReused(int nextIndex) {
        interact(() -> {
            StringProperty first = spy(new SimpleStringProperty("first.pdf"));
            StringProperty second = new SimpleStringProperty("second.pdf");
            ListView<StringProperty> listView = new ListView<>();
            listView.getItems().setAll(first, second);
            ListCell<StringProperty> cell = new ViewModelListCellFactory<StringProperty>()
                    .withTooltipText(item -> item)
                    .call(listView);
            cell.updateListView(listView);
            cell.updateIndex(0);
            Tooltip oldTooltip = cell.getTooltip();
            assertEquals("first.pdf", oldTooltip.getText());

            ArgumentCaptor<InvalidationListener> listener = ArgumentCaptor.forClass(InvalidationListener.class);
            verify(first).addListener(listener.capture());

            cell.updateIndex(nextIndex);

            verify(first).removeListener(listener.getValue());
            first.set("stale.pdf");
            if (nextIndex == -1) {
                assertNull(cell.getTooltip());
            } else {
                assertSame(oldTooltip, cell.getTooltip());
                assertEquals("second.pdf", cell.getTooltip().getText());
                second.set("updated.pdf");
                assertEquals("updated.pdf", cell.getTooltip().getText());
            }

            cell.updateIndex(-1);
            verify(first).removeListener(listener.getValue());

            cell.updateIndex(0);
            assertSame(oldTooltip, cell.getTooltip());
            assertEquals("stale.pdf", cell.getTooltip().getText());

            cell.updateIndex(-1);
            assertAllListenersRemoved(first);
        });
    }

    @Test
    void validationTooltipOverridesBoundTextWithoutRetainingOldItemListeners() {
        interact(() -> {
            StringProperty link = spy(new SimpleStringProperty("file.pdf"));
            ValidationStatus validationStatus = mock(ValidationStatus.class);
            when(validationStatus.getHighestMessage()).thenReturn(Optional.of(ValidationMessage.warning("Missing file")));
            when(validationStatus.validProperty()).thenReturn(new ReadOnlyBooleanWrapper(false).getReadOnlyProperty());

            ListView<StringProperty> listView = new ListView<>();
            listView.getItems().add(link);
            ListCell<StringProperty> cell = new ViewModelListCellFactory<StringProperty>()
                    .withTooltipText(item -> item)
                    .withValidation(_ -> validationStatus)
                    .call(listView);
            cell.updateListView(listView);
            cell.updateIndex(0);
            assertEquals("Missing file", cell.getTooltip().getText());

            cell.updateIndex(-1);
            assertNull(cell.getTooltip());
            assertAllListenersRemoved(link);
        });
    }

    private static void assertAllListenersRemoved(StringProperty property) {
        ArgumentCaptor<InvalidationListener> added = ArgumentCaptor.forClass(InvalidationListener.class);
        ArgumentCaptor<InvalidationListener> removed = ArgumentCaptor.forClass(InvalidationListener.class);
        verify(property, atLeastOnce()).addListener(added.capture());
        verify(property, atLeastOnce()).removeListener(removed.capture());
        assertEquals(added.getAllValues(), removed.getAllValues());
    }
}
