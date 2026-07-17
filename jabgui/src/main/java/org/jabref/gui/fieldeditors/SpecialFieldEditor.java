package org.jabref.gui.fieldeditors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.swing.undo.UndoManager;

import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.specialfields.SpecialFieldValueViewModel;
import org.jabref.gui.specialfields.SpecialFieldViewModel;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.SpecialFieldValue;

import com.tobiasdiez.easybind.EasyBind;
import org.controlsfx.control.Rating;

/// Editor offering the same icon-based selection for [SpecialField]s as the main table's
/// special field columns ([org.jabref.gui.maintable.columns.SpecialFieldColumn]):
/// a star [Rating] for the ranking, a single toggle for the one-value fields
/// (printed, quality, relevance), and one toggle per value for priority and read status.
// [impl->req~entry-editor.special-field-editors~1]
public class SpecialFieldEditor extends HBox implements FieldEditorFX {

    private final SpecialField specialField;
    private final SpecialFieldViewModel viewModel;

    private BibEntry entry;
    // BibEntry field bindings are held weakly, so keep a reference for the editor's lifetime
    private ObservableValue<Optional<String>> fieldValue;
    // Distinguishes programmatic control updates (driven by the field binding) from user input
    private boolean updatingControls;

    private Rating ratingControl;
    private ToggleButton toggleControl;
    private ToggleGroup toggleGroup;

    public SpecialFieldEditor(SpecialField specialField, CliPreferences preferences, UndoManager undoManager) {
        this.specialField = specialField;
        this.viewModel = new SpecialFieldViewModel(specialField, preferences, undoManager);

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(2);

        if (specialField == SpecialField.RANKING) {
            getChildren().add(createRatingControl());
        } else if (specialField.isSingleValueField()) {
            getChildren().add(createToggleControl());
        } else {
            getChildren().addAll(createToggleGroupControls());
        }
    }

    private Rating createRatingControl() {
        ratingControl = new Rating();
        ratingControl.setRating(0);
        ratingControl.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if ((event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2)) {
                ratingControl.setRating(0);
                event.consume();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        EasyBind.subscribe(ratingControl.ratingProperty(), rating -> {
            if (!updatingControls && (entry != null)) {
                viewModel.setSpecialFieldValue(entry, SpecialFieldValue.getRating(rating.intValue()));
            }
        });
        return ratingControl;
    }

    private ToggleButton createToggleControl() {
        SpecialFieldValueViewModel value = new SpecialFieldValueViewModel(specialField.getValues().getFirst());
        toggleControl = new ToggleButton();
        toggleControl.setGraphic(viewModel.getIcon().getGraphicNode());
        toggleControl.getStyleClass().add("icon-button");
        toggleControl.setTooltip(new Tooltip(value.getToolTipText()));
        EasyBind.subscribe(toggleControl.selectedProperty(), selected -> {
            if (!updatingControls && (entry != null)) {
                // Setting the single value again clears it, so select and deselect are the same call
                viewModel.toggle(entry);
            }
        });
        return toggleControl;
    }

    private List<ToggleButton> createToggleGroupControls() {
        toggleGroup = new ToggleGroup();
        List<ToggleButton> buttons = new ArrayList<>();
        for (SpecialFieldValueViewModel value : viewModel.getValues()) {
            if (value.getValue().getFieldValue().isEmpty()) {
                // CLEAR_* pseudo-values are covered by deselecting the active toggle
                continue;
            }
            ToggleButton button = new ToggleButton();
            button.setGraphic(value.getIcon().map(JabRefIcon::getGraphicNode).orElse(null));
            button.getStyleClass().add("icon-button");
            button.setTooltip(new Tooltip(value.getToolTipText()));
            button.setUserData(value.getValue());
            button.setToggleGroup(toggleGroup);
            buttons.add(button);
        }
        EasyBind.subscribe(toggleGroup.selectedToggleProperty(), toggle -> {
            if (!updatingControls && (entry != null)) {
                if (toggle == null) {
                    viewModel.setSpecialFieldValue(entry, clearValue());
                } else {
                    viewModel.setSpecialFieldValue(entry, (SpecialFieldValue) toggle.getUserData());
                }
            }
        });
        return buttons;
    }

    private SpecialFieldValue clearValue() {
        return specialField.getValues().stream()
                           .filter(value -> value.getFieldValue().isEmpty())
                           .findFirst()
                           .orElseThrow();
    }

    @Override
    public void bindToEntry(BibEntry entry) {
        this.entry = entry;
        fieldValue = entry.getFieldBinding(specialField);
        EasyBind.subscribe(fieldValue, value -> {
            updatingControls = true;
            try {
                applyValue(value.flatMap(specialField::parseValue));
            } finally {
                updatingControls = false;
            }
        });
    }

    private void applyValue(Optional<SpecialFieldValue> value) {
        if (specialField == SpecialField.RANKING) {
            ratingControl.setRating(value.map(SpecialFieldValue::toRating).orElse(0));
        } else if (specialField.isSingleValueField()) {
            toggleControl.setSelected(value.isPresent());
        } else {
            toggleGroup.selectToggle(toggleGroup.getToggles().stream()
                                                .filter(toggle -> value.map(toggle.getUserData()::equals).orElse(false))
                                                .findFirst()
                                                .orElse(null));
        }
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
