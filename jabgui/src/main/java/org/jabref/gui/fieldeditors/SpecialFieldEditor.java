package org.jabref.gui.fieldeditors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Editor offering the same icon-based selection for [SpecialField]s as the main table's
/// special field columns ([org.jabref.gui.maintable.columns.SpecialFieldColumn]):
/// a star [Rating] for the ranking, a single toggle for the one-value fields
/// (printed, quality, relevance), and one toggle per value for priority and read status.
// [impl->req~entry-editor.special-field-editors~1]
@NullMarked
public class SpecialFieldEditor extends HBox implements FieldEditorFX {

    private final SpecialField specialField;
    private final SpecialFieldViewModel viewModel;
    /// Pushes a stored field value into whichever control this editor built, so the varying
    /// control types don't have to be kept as separate (mostly-null) fields.
    private final Consumer<Optional<SpecialFieldValue>> valueApplier;

    /// Null until [#bindToEntry] runs; the control listeners fire once on construction (before
    /// binding) and again for every value pushed into a control, so they must tolerate no entry.
    private @Nullable BibEntry entry;
    /// Retained only to keep the field binding alive: [BibEntry] holds its bindings weakly.
    private @Nullable ObservableValue<Optional<String>> fieldValue;
    /// Distinguishes programmatic control updates (driven by the field binding) from user input.
    private boolean updatingControls;

    public SpecialFieldEditor(SpecialField specialField, CliPreferences preferences, UndoManager undoManager) {
        this.specialField = specialField;
        this.viewModel = new SpecialFieldViewModel(specialField, preferences, undoManager);

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(2);

        if (specialField == SpecialField.RANKING) {
            Rating rating = createRatingControl();
            getChildren().add(rating);
            valueApplier = value -> rating.setRating(value.map(SpecialFieldValue::toRating).orElse(0));
        } else if (specialField.isSingleValueField()) {
            ToggleButton toggle = createToggleControl();
            getChildren().add(toggle);
            valueApplier = value -> toggle.setSelected(value.isPresent());
        } else {
            ToggleGroup group = new ToggleGroup();
            getChildren().addAll(createToggleGroupControls(group));
            valueApplier = value -> group.getToggles().stream()
                                         .filter(toggle -> value.map(toggle.getUserData()::equals).orElse(false))
                                         .findFirst()
                                         .ifPresentOrElse(group::selectToggle, () -> group.selectToggle(null));
        }
    }

    private Rating createRatingControl() {
        Rating rating = new Rating();
        rating.setRating(0);
        rating.addEventFilter(MouseEvent.MOUSE_CLICKED, event -> {
            if ((event.getButton() == MouseButton.PRIMARY) && (event.getClickCount() == 2)) {
                rating.setRating(0);
                event.consume();
            } else if (event.getButton() == MouseButton.SECONDARY) {
                event.consume();
            }
        });
        EasyBind.subscribe(rating.ratingProperty(), value -> {
            BibEntry boundEntry = entry;
            if (!updatingControls && (boundEntry != null)) {
                viewModel.setSpecialFieldValue(boundEntry, SpecialFieldValue.getRating(value.intValue()));
            }
        });
        return rating;
    }

    private ToggleButton createToggleControl() {
        SpecialFieldValueViewModel value = new SpecialFieldValueViewModel(specialField.getValues().getFirst());
        ToggleButton toggle = new ToggleButton();
        toggle.setGraphic(viewModel.getIcon().getGraphicNode());
        toggle.getStyleClass().add("icon-button");
        toggle.setTooltip(new Tooltip(value.getToolTipText()));
        EasyBind.subscribe(toggle.selectedProperty(), selected -> {
            BibEntry boundEntry = entry;
            if (!updatingControls && (boundEntry != null)) {
                // Setting the single value again clears it, so select and deselect are the same call
                viewModel.toggle(boundEntry);
            }
        });
        return toggle;
    }

    private List<ToggleButton> createToggleGroupControls(ToggleGroup group) {
        List<ToggleButton> buttons = new ArrayList<>();
        for (SpecialFieldValueViewModel value : viewModel.getValues()) {
            if (value.getValue().getFieldValue().isEmpty()) {
                // CLEAR_* pseudo-values are covered by deselecting the active toggle
                continue;
            }
            ToggleButton button = new ToggleButton();
            value.getIcon().map(JabRefIcon::getGraphicNode).ifPresent(button::setGraphic);
            button.getStyleClass().add("icon-button");
            button.setTooltip(new Tooltip(value.getToolTipText()));
            button.setUserData(value.getValue());
            button.setToggleGroup(group);
            buttons.add(button);
        }
        EasyBind.subscribe(group.selectedToggleProperty(), toggle -> {
            BibEntry boundEntry = entry;
            if (!updatingControls && (boundEntry != null)) {
                if (toggle == null) {
                    viewModel.setSpecialFieldValue(boundEntry, clearValue());
                } else {
                    viewModel.setSpecialFieldValue(boundEntry, (SpecialFieldValue) toggle.getUserData());
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
                valueApplier.accept(value.flatMap(specialField::parseValue));
            } finally {
                updatingControls = false;
            }
        });
    }

    @Override
    public Parent getNode() {
        return this;
    }
}
