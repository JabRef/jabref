package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.fxmisc.wellbehaved.event.InputMap;
import org.fxmisc.wellbehaved.event.Nodes;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

import static org.fxmisc.wellbehaved.event.EventPattern.anyOf;
import static org.fxmisc.wellbehaved.event.EventPattern.eventType;
import static org.fxmisc.wellbehaved.event.EventPattern.mousePressed;

/**
 * A readonly, selectable field cell that contains the value of some field
 */
public class FieldValueCell extends AbstractCell implements Toggle {
    public static final String DEFAULT_STYLE_CLASS = "field-value";
    public static final String SELECTION_BOX_STYLE_CLASS = "selection-box";

    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();
    private final StyleClassedTextArea label = new StyleClassedTextArea();

    private final VirtualizedScrollPane<StyleClassedTextArea> scrollPane = new VirtualizedScrollPane<>(label);
    HBox labelBox = new HBox(scrollPane);
    private final BooleanProperty selected = new BooleanPropertyBase() {
        @Override
        public Object getBean() {
            return FieldValueCell.class;
        }

        @Override
        public String getName() {
            return "selected";
        }

        @Override
        protected void invalidated() {
            pseudoClassStateChanged(SELECTED_PSEUDO_CLASS, get());

            ToggleGroup group = getToggleGroup();
            group.selectToggle(FieldValueCell.this);
        }
    };
    private final HBox selectionBox = new HBox();
    private final VBox checkmarkLayout = new VBox();

    public FieldValueCell(String text, BackgroundTone backgroundTone) {
        super(text, backgroundTone);
        initialize();
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        initializeScrollPane();
        initializeLabel();
        initializeSelectionBox();
        textProperty().addListener(invalidated -> setUserData(getText()));
        setOnMouseClicked(e -> {
            if (!isDisabled()) {
                setSelected(true);
            }
        });

        selectionBox.getChildren().addAll(labelBox, checkmarkLayout);
        getChildren().setAll(selectionBox);
    }

    private void initializeLabel() {
        label.setEditable(false);
        label.setBackground(Background.fill(Color.TRANSPARENT));
        label.appendText(textProperty().get());
        label.setAutoHeight(true);
        label.setWrapText(true);
        label.setStyle("-fx-cursor: hand");

        // Workarounds
        preventTextSelectionViaMouseEvents();

        label.prefHeightProperty().bind(label.totalHeightEstimateProperty().orElseConst(-1d));

        // Fix text area consuming scroll events before they rich the outer scrollable
        label.addEventFilter(ScrollEvent.SCROLL, e -> {
            e.consume();
            FieldValueCell.this.fireEvent(e.copyFor(e.getSource(), FieldValueCell.this));
        });
    }

    private void initializeSelectionBox() {
        selectionBox.getStyleClass().add(SELECTION_BOX_STYLE_CLASS);
        HBox.setHgrow(selectionBox, Priority.ALWAYS);

        HBox.setHgrow(labelBox, Priority.ALWAYS);
        labelBox.setPadding(new Insets(8));
        labelBox.setCursor(Cursor.HAND);

        FontIcon copyIcon = FontIcon.of(MaterialDesignC.CONTENT_COPY);
        Button copyButton = new Button();
        copyButton.setGraphic(copyIcon);
        copyButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        copyButton.setStyle("-fx-border-width: 0");

        copyIcon.getStyleClass().add("checkmark-icon");
        checkmarkLayout.getChildren().setAll(copyButton);
        checkmarkLayout.setAlignment(Pos.TOP_CENTER);
        checkmarkLayout.setPrefWidth(28);

        copyButton.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(copyButton, Priority.ALWAYS);
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

    @Override
    public void setUserData(Object value) {
        super.setText((String) value);
    }

    @Override
    public Object getUserData() {
        return super.getText();
    }
}
