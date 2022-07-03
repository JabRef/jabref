package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
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

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.mergeentries.newmergedialog.CopyFieldValueCommand;
import org.jabref.logic.l10n.Localization;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.StyleClassedTextArea;
import org.kordamp.ikonli.javafx.FontIcon;
import org.kordamp.ikonli.materialdesign2.MaterialDesignC;

/**
 * A readonly, selectable field cell that contains the value of some field
 */
public class FieldValueCell extends AbstractCell implements Toggle {
    public static final String DEFAULT_STYLE_CLASS = "field-value";
    public static final String SELECTION_BOX_STYLE_CLASS = "selection-box";

    private static final PseudoClass SELECTED_PSEUDO_CLASS = PseudoClass.getPseudoClass("selected");
    private final ObjectProperty<ToggleGroup> toggleGroup = new SimpleObjectProperty<>();
    private final StyleClassedTextArea label = new StyleClassedTextArea();

    private final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

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

            getToggleGroup().selectToggle(FieldValueCell.this);
        }
    };
    private final HBox selectionBox = new HBox();
    private final HBox actionsContainer = new HBox();

    public FieldValueCell(String text, int rowIndex) {
        super(text, rowIndex);
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

        selectionBox.getChildren().addAll(labelBox, actionsContainer);
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

        // Fix text area consuming scroll events before they reach the outer scrollable
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

        actionsContainer.getChildren().setAll(createCopyButton());
        actionsContainer.setAlignment(Pos.TOP_CENTER);
        actionsContainer.setPrefWidth(28);
    }

    private Button createCopyButton() {
        FontIcon copyIcon = FontIcon.of(MaterialDesignC.CONTENT_COPY);
        copyIcon.getStyleClass().add("copy-icon");

        Button copyButton = factory.createIconButton(() -> Localization.lang("Copy"), new CopyFieldValueCommand(Globals.prefs, getText()));
        copyButton.setGraphic(copyIcon);
        copyButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        copyButton.setMaxHeight(Double.MAX_VALUE);

        return copyButton;
    }

    private void initializeScrollPane() {
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
    }

    private void preventTextSelectionViaMouseEvents() {
        label.addEventFilter(MouseEvent.ANY, e -> {
            if (e.getEventType() == MouseEvent.MOUSE_DRAGGED ||
                    e.getEventType() == MouseEvent.DRAG_DETECTED ||
                    e.getEventType() == MouseEvent.MOUSE_ENTERED) {
                e.consume();
            } else if (e.getEventType() == MouseEvent.MOUSE_PRESSED) {
                if (e.getClickCount() > 1) {
                    e.consume();
                }
            }
        });
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

    public StyleClassedTextArea getStyleClassedLabel() {
        return label;
    }
}
