package org.jabref.gui.commonfxcontrols;

import java.util.EnumMap;
import java.util.Map;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.MapProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.MapChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;

import org.jabref.gui.util.BindingsHelper;
import org.jabref.logic.cleanup.BooktitleCleanups;
import org.jabref.logic.util.LocationDetector;
import org.jabref.model.cleanup.BooktitleCleanupAction;
import org.jabref.model.cleanup.BooktitleCleanupField;

import com.airhacks.afterburner.views.ViewLoader;

/**
 * A JavaFX panel component for configuring book title cleanup operations.
 *
 * <p>This panel provides a user interface for managing cleanup actions that extract and move
 * embedded fields from booktitles, including months, years, page ranges, and locations. Users can
 * configure how each field's cleanup should be handled through radio button groups.</p>
 *
 * <p>The radio buttons are dynamically generated for each cleanup field based on the
 * {@link BooktitleCleanupField} enum and {@link BooktitleCleanupAction} enum.
 *
 * <p>Available actions for each metadata type:</p>
 * <ul>
 *   <li><b>Remove Only:</b> Extract and remove the metadata from the book title</li>
 *   <li><b>Replace:</b> Extract, remove from title, and populate the corresponding field</li>
 *   <li><b>Replace If Empty:</b> Extract and populate only if the target field is empty</li>
 *   <li><b>Skip:</b> Do not perform any cleanup for this metadata type</li>
 * </ul>
 *
 * For details on how the cleanups are executed, see {@link BooktitleCleanups}
 */
public class BooktitleCleanupPanel extends VBox {

    public static final double ROW_MIN_HEIGHT = 15.0;
    public static final double ROW_PREF_HEIGHT = 20.0;

    private final Map<BooktitleCleanupField, ToggleGroup> toggleGroups = new EnumMap<>(BooktitleCleanupField.class);

    @FXML private CheckBox cleanupsEnabled;
    @FXML private GridPane cleanupGrid;

    private BooktitleCleanupPanelViewModel viewModel;

    public BooktitleCleanupPanel() {
        ViewLoader.view(this)
                  .root(this)
                  .load();
    }

    @FXML
    private void initialize() {
        this.viewModel = new BooktitleCleanupPanelViewModel(LocationDetector.getInstance());

        generateCleanupRows();
        setupBindings();
    }

    /**
     * Dynamically generates radio button rows for each cleanup field type.
     *
     * <p>For each {@link BooktitleCleanupField}, this method:</p>
     * <ul>
     *   <li>Creates a row constraint in the grid</li>
     *   <li>Adds a label for the field name</li>
     *   <li>Gets the toggle group from the view model</li>
     *   <li>Creates radio buttons for each {@link BooktitleCleanupAction}</li>
     *   <li>Selects the default action</li>
     * </ul>
     */
    private void generateCleanupRows() {
        int rowIndex = 1;
        int labelIndex = 0;

        for (BooktitleCleanupField field : BooktitleCleanupField.values()) {
            RowConstraints rowConstraint = new RowConstraints();
            rowConstraint.setMinHeight(ROW_MIN_HEIGHT);
            rowConstraint.setPrefHeight(ROW_PREF_HEIGHT);
            cleanupGrid.getRowConstraints().add(rowConstraint);

            Label fieldLabel = new Label(field.getDisplayName());
            fieldLabel.getStyleClass().add("field-label");
            cleanupGrid.add(fieldLabel, labelIndex, rowIndex);

            ToggleGroup toggleGroup = new ToggleGroup();
            toggleGroups.put(field, toggleGroup);

            int columnIndex = 1;
            for (BooktitleCleanupAction cleanupAction : BooktitleCleanupAction.values()) {
                RadioButton radioButton = createRadioButton(cleanupAction, toggleGroup);

                if (cleanupAction == field.getDefaultAction()) {
                    radioButton.setSelected(true);
                }

                cleanupGrid.add(radioButton, columnIndex, rowIndex);
                columnIndex++;
            }

            rowIndex++;
        }
    }

    private RadioButton createRadioButton(BooktitleCleanupAction cleanupAction, ToggleGroup toggleGroup) {
        RadioButton radioButton = new RadioButton();
        radioButton.setToggleGroup(toggleGroup);
        radioButton.setUserData(cleanupAction);
        return radioButton;
    }

    private void setupBindings() {
        BindingsHelper.bindBidirectional(
                (ObservableValue<Boolean>) cleanupsEnabled.selectedProperty(),
                viewModel.cleanupsDisableProperty(),
                disabled -> cleanupsEnabled.selectedProperty().setValue(!disabled),
                selected -> viewModel.cleanupsDisableProperty().setValue(!selected));

        // setup bidirectional binding between each toggleGroup and its corresponding field-to-cleanup-action map in the view model
        toggleGroups.forEach((field, toggleGroup) -> {
            toggleGroup.selectedToggleProperty().addListener((_, _, selectedToggle) -> {
                if (selectedToggle != null) {
                    BooktitleCleanupAction action = (BooktitleCleanupAction) selectedToggle.getUserData();
                    viewModel.setSelectedAction(field, action);
                }
            });
            viewModel.selectedActionsProperty().addListener((MapChangeListener<BooktitleCleanupField, BooktitleCleanupAction>) change -> {
                if (change.wasAdded() && change.getKey() == field) {
                    BooktitleCleanupAction newAction = change.getValueAdded();
                    for (Toggle toggle : toggleGroup.getToggles()) {
                        if (toggle.getUserData() == newAction) {
                            toggleGroup.selectToggle(toggle);
                            break;
                        }
                    }
                }
            });
        });
    }

    public BooktitleCleanups createCleanupAction() {
        return viewModel.createCleanup();
    }

    public BooleanProperty cleanupsDisableProperty() {
        return viewModel.cleanupsDisableProperty();
    }

    public MapProperty<BooktitleCleanupField, BooktitleCleanupAction> selectedActionsProperty() {
        return viewModel.selectedActionsProperty();
    }
}
