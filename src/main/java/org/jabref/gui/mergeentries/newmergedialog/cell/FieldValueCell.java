package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;

import static org.fxmisc.wellbehaved.event.EventPattern.anyOf;
import static org.fxmisc.wellbehaved.event.EventPattern.eventType;
import static org.fxmisc.wellbehaved.event.EventPattern.mousePressed;

/**
 * A non-editable and selectable field cell that contains the value of some field
 */
public class FieldValueCell extends AbstractCell implements Toggle {
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();
    private final BooleanProperty selected = new SimpleBooleanProperty();

    private final StyleClassedTextArea label = new StyleClassedTextArea();

    private final VirtualizedScrollPane<StyleClassedTextArea> scrollPane = new VirtualizedScrollPane<>(label);

    public FieldValueCell(String text, BackgroundTone backgroundTone) {
        super(text, backgroundTone);
    }

    public FieldValueCell(String text) {
        super(text);
    }

    private void initialize() {
        initializeLabel();
        initializeSelectionBox();
    }

    private void initializeLabel() {
        label.setEditable(false);
        label.setBackground(Background.fill(Color.TRANSPARENT));
        label.appendText(textProperty().get());
        label.setAutoHeight(true);
        label.setWrapText(true);

        preventTextSelectionViaMouseEvents();
    }

    private void initializeSelectionBox() {
    }

    private void initializeScrollPane() {
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void preventTextSelectionViaMouseEvents() {
        InputMap<Event> preventSelection = InputMap.consume(
                anyOf(eventType(MouseEvent.MOUSE_DRAGGED),
                      eventType(MouseEvent.DRAG_DETECTED),
                      eventType(MouseEvent.MOUSE_ENTERED),
                      mousePressed().unless(e -> e.getClickCount() == 1)
                )
        );
        Nodes.addInputMap(label, preventSelection);
    }

    @Override
    public ToggleGroup getToggleGroup() {
        return toggleGroupProperty().get();
    }

    @Override
    public void setToggleGroup(ToggleGroup toggleGroup) {
        toggleGroupProperty().set(toggleGroup);
    }

    @Override
    public ObjectProperty<ToggleGroup> toggleGroupProperty() {
        return toggleGroup;
    }

    @Override
    public boolean isSelected() {
        return selectedProperty().get();
    }

    @Override
    public void setSelected(boolean selected) {
        selectedProperty().set(selected);
    }

    @Override
    public BooleanProperty selectedProperty() {
        return selected;
    }
}
