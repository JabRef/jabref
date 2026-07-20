package org.jabref.gui.preferences.forms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.logic.help.HelpFile;

import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;

/// Fluent, eager builder that assembles a preference tab's node tree and wires all bindings.
///
/// It replaces the FXML + controller pair: each `add*` call creates a control, binds it to a
/// view-model property and appends it to the current container. Post-configuration methods
/// ({@link #disableWhen}, {@link #validate}, {@link #help}) apply to the most recently added
/// control, so calls read top-to-bottom like the form itself.
///
/// Consecutive labelled fields share an aligned two-column {@link GridPane}; any full-width
/// element (checkbox, section header, custom node) flushes that grid. Validation decoration is
/// collected and initialised once on the FX thread in {@link #build()}.
public class PreferencesFormBuilder {

    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final VBox root = new VBox(10.0);
    private final Deque<Pane> containers = new ArrayDeque<>();
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final List<Runnable> validationInits = new ArrayList<>();

    private GridPane currentGrid;
    private int gridRow;

    /// The toggle group that radios are added to between beginRadioGroup/endRadioGroup.
    private ToggleGroup currentToggleGroup;

    /// The control that trailing configuration methods act on.
    private Node lastControl;
    /// The row (if any) that {@link #help} appends to.
    private HBox lastRow;

    public PreferencesFormBuilder(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.visualizer.setDecoration(new IconValidationDecorator());
        this.containers.push(root);
    }

    // region headers & static content

    public PreferencesFormBuilder title(String text) {
        return styledLabel(text, "titleHeader");
    }

    public PreferencesFormBuilder section(String text) {
        return styledLabel(text, "sectionHeader");
    }

    public PreferencesFormBuilder sectionWithHelp(String text, HelpFile helpFile) {
        flushGrid();
        Label header = new Label(text);
        header.getStyleClass().add("sectionHeader");
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(header, Priority.ALWAYS);
        HBox row = new HBox(header, helpButton(StandardActions.HELP, helpFile));
        row.setAlignment(Pos.BASELINE_CENTER);
        container().getChildren().add(row);
        lastRow = row;
        lastControl = header;
        return this;
    }

    public PreferencesFormBuilder info(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("italic");
        label.setPadding(new Insets(0, 0, 0, 20));
        addNode(label);
        return this;
    }

    private PreferencesFormBuilder styledLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        addNode(label);
        return this;
    }

    // endregion

    // region controls

    public PreferencesFormBuilder checkbox(String text, Property<Boolean> value) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.setMaxWidth(Double.MAX_VALUE);
        checkBox.selectedProperty().bindBidirectional(value);
        HBox row = new HBox(10.0, checkBox);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        lastControl = checkBox;
        lastRow = row;
        return this;
    }

    /// A checkbox with an inline value field that is enabled only while the box is ticked
    /// (the recurring "Enable ... on port [....]" pattern). Trailing {@link #validate} targets
    /// the value field.
    public PreferencesFormBuilder checkWithField(String text, Property<Boolean> enabled, StringProperty fieldValue) {
        CheckBox checkBox = new CheckBox(text);
        checkBox.selectedProperty().bindBidirectional(enabled);
        TextField field = new TextField();
        field.setMaxWidth(100.0);
        field.textProperty().bindBidirectional(fieldValue);
        field.disableProperty().bind(checkBox.selectedProperty().not());
        HBox row = new HBox(10.0, checkBox, field);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        lastControl = field;
        lastRow = row;
        return this;
    }

    public PreferencesFormBuilder stringField(String label, StringProperty value) {
        TextField field = new TextField();
        field.setMaxWidth(Double.MAX_VALUE);
        field.textProperty().bindBidirectional(value);
        addField(label, field);
        return this;
    }

    public PreferencesFormBuilder browseField(String label, StringProperty value, Runnable onBrowse) {
        BrowseFileEditor.Result result = BrowseFileEditor.create(value, onBrowse);
        if (label == null) {
            addNode(result.row());
        } else {
            addField(label, result.row());
        }
        lastControl = result.field();
        lastRow = result.row();
        return this;
    }

    public PreferencesFormBuilder button(String text, JabRefIcon icon, Runnable action) {
        javafx.scene.control.Button button = new javafx.scene.control.Button(text);
        if (icon != null) {
            button.setGraphic(icon.getGraphicNode());
        }
        button.setOnAction(_ -> action.run());
        addNode(button);
        return this;
    }

    public PreferencesFormBuilder hyperlink(String text, Runnable action) {
        Hyperlink link = new Hyperlink(text);
        link.setOnAction(_ -> action.run());
        addNode(link);
        return this;
    }

    public <X> PreferencesFormBuilder combo(String label,
                                            ObservableValue<? extends ObservableList<X>> items,
                                            Property<X> value,
                                            Callback<X, String> display) {
        ComboBox<X> combo = new ComboBox<>();
        new ViewModelListCellFactory<X>().withText(display).install(combo);
        combo.itemsProperty().bind(items);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.valueProperty().bindBidirectional(value);
        addField(label, combo);
        return this;
    }

    public <X> PreferencesFormBuilder searchableCombo(String label,
                                                      ObservableValue<? extends ObservableList<X>> items,
                                                      Property<X> value,
                                                      Callback<X, String> display) {
        SearchableComboBox<X> combo = new SearchableComboBox<>();
        new ViewModelListCellFactory<X>().withText(display).install(combo);
        combo.itemsProperty().bind(items);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.valueProperty().bindBidirectional(value);
        addField(label, combo);
        return this;
    }

    /// String combo box (optionally editable, with a prompt). The identity display is used.
    public PreferencesFormBuilder stringCombo(String label,
                                              ObservableValue<? extends ObservableList<String>> items,
                                              Property<String> value,
                                              boolean editable,
                                              String prompt) {
        ComboBox<String> combo = new ComboBox<>();
        combo.itemsProperty().bind(items);
        combo.valueProperty().bindBidirectional(value);
        combo.setEditable(editable);
        combo.setMaxWidth(Double.MAX_VALUE);
        if (prompt != null) {
            combo.setPromptText(prompt);
        }
        addField(label, combo);
        return this;
    }

    // endregion

    // region radio groups

    /// Opens a mutually-exclusive radio group. Radios added until {@link #endRadioGroup()} share one
    /// {@link ToggleGroup}; each stays bound to its own boolean property (matching the existing VMs).
    public PreferencesFormBuilder beginRadioGroup() {
        currentToggleGroup = new ToggleGroup();
        return this;
    }

    public PreferencesFormBuilder endRadioGroup() {
        currentToggleGroup = null;
        return this;
    }

    public PreferencesFormBuilder radio(String text, Property<Boolean> selected) {
        addNode(radioRow(text, selected, null));
        return this;
    }

    /// A radio with a bound text field that is enabled only while the radio is selected.
    public PreferencesFormBuilder radioWithField(String text, Property<Boolean> selected, StringProperty fieldValue) {
        RadioButton radio = newRadio(text, selected);
        TextField field = new TextField();
        field.textProperty().bindBidirectional(fieldValue);
        field.disableProperty().bind(radio.selectedProperty().not());
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox row = new HBox(10.0, radio, field);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        lastControl = field;
        lastRow = row;
        return this;
    }

    /// A radio with an inline browse field (e.g. "Main file directory"). Trailing {@link #validate}
    /// and {@link #disableWhen} target the path field (the browse button follows its disabled state).
    public PreferencesFormBuilder radioWithBrowse(String text, Property<Boolean> selected, StringProperty pathValue, Runnable onBrowse) {
        RadioButton radio = newRadio(text, selected);
        BrowseFileEditor.Result browse = BrowseFileEditor.create(pathValue, onBrowse);
        HBox.setHgrow(browse.row(), Priority.ALWAYS);
        HBox row = new HBox(10.0, radio, browse.row());
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        lastControl = browse.field();
        lastRow = row;
        return this;
    }

    private HBox radioRow(String text, Property<Boolean> selected, TextField trailing) {
        RadioButton radio = newRadio(text, selected);
        HBox row = new HBox(10.0, radio);
        row.setAlignment(Pos.CENTER_LEFT);
        if (trailing != null) {
            row.getChildren().add(trailing);
        }
        lastControl = radio;
        lastRow = row;
        return row;
    }

    private RadioButton newRadio(String text, Property<Boolean> selected) {
        RadioButton radio = new RadioButton(text);
        radio.setToggleGroup(currentToggleGroup);
        radio.selectedProperty().bindBidirectional(selected);
        return radio;
    }

    // endregion

    // region escape hatches

    /// Adds a bespoke labelled control; trailing configuration methods target it.
    public PreferencesFormBuilder field(String label, Node control) {
        addField(label, control);
        return this;
    }

    /// Adds a fully custom node spanning the form width (the `.custom(Node)` hatch).
    public PreferencesFormBuilder custom(Node node) {
        addNode(node);
        return this;
    }

    /// Runs arbitrary configuration on the last added control (e.g. capturing a reference).
    public PreferencesFormBuilder configure(Consumer<Node> consumer) {
        consumer.accept(lastControl);
        return this;
    }

    // endregion

    // region grouping & post-configuration

    /// Starts a sub-group whose visibility (and layout participation) follows `visibleWhen`.
    /// Controls added until {@link #endGroup()} are placed inside it.
    public PreferencesFormBuilder beginGroup(ObservableValue<? extends Boolean> visibleWhen) {
        flushGrid();
        VBox group = new VBox(10.0);
        group.visibleProperty().bind(visibleWhen);
        group.managedProperty().bind(visibleWhen);
        container().getChildren().add(group);
        containers.push(group);
        return this;
    }

    public PreferencesFormBuilder endGroup() {
        flushGrid();
        if (containers.size() > 1) {
            containers.pop();
        }
        return this;
    }

    public PreferencesFormBuilder disableWhen(ObservableValue<? extends Boolean> condition) {
        lastControl.disableProperty().bind(condition);
        return this;
    }

    /// Statically disables the last control (for platform-capability checks, not reactive state).
    public PreferencesFormBuilder disabled(boolean value) {
        lastControl.setDisable(value);
        return this;
    }

    public PreferencesFormBuilder tooltip(String text) {
        if (lastControl instanceof Control control) {
            control.setTooltip(new Tooltip(text));
        }
        return this;
    }

    public PreferencesFormBuilder validate(ValidationStatus status) {
        if (lastControl instanceof Control control) {
            validationInits.add(() -> visualizer.initVisualization(status, control));
        }
        return this;
    }

    /// Appends a help icon button to the current row (checkbox, checkWithField or section row).
    public PreferencesFormBuilder help(HelpFile helpFile) {
        return help(StandardActions.HELP, helpFile);
    }

    public PreferencesFormBuilder help(StandardActions action, HelpFile helpFile) {
        if (lastRow != null) {
            lastRow.getChildren().add(helpButton(action, helpFile));
        }
        return this;
    }

    // endregion

    public VBox build() {
        flushGrid();
        Platform.runLater(() -> validationInits.forEach(Runnable::run));
        return root;
    }

    // region internals

    private Pane container() {
        return containers.peek();
    }

    private void addNode(Node node) {
        flushGrid();
        container().getChildren().add(node);
        lastControl = node;
        lastRow = null;
    }

    private void addField(String label, Node control) {
        GridPane grid = ensureGrid();
        if (label != null) {
            grid.add(new Label(label), 0, gridRow);
        }
        grid.add(control, 1, gridRow);
        GridPane.setHgrow(control, Priority.ALWAYS);
        gridRow++;
        lastControl = control;
        lastRow = (control instanceof HBox hbox) ? hbox : null;
    }

    private GridPane ensureGrid() {
        if (currentGrid == null) {
            currentGrid = new GridPane();
            currentGrid.setHgap(10.0);
            currentGrid.setVgap(10.0);
            ColumnConstraints labelColumn = new ColumnConstraints();
            labelColumn.setMinWidth(120.0);
            labelColumn.setHalignment(HPos.LEFT);
            ColumnConstraints controlColumn = new ColumnConstraints();
            controlColumn.setHgrow(Priority.ALWAYS);
            currentGrid.getColumnConstraints().addAll(labelColumn, controlColumn);
            gridRow = 0;
            container().getChildren().add(currentGrid);
        }
        return currentGrid;
    }

    private void flushGrid() {
        currentGrid = null;
    }

    private javafx.scene.control.Button helpButton(StandardActions action, HelpFile helpFile) {
        javafx.scene.control.Button button = new javafx.scene.control.Button();
        button.setPrefWidth(20.0);
        new ActionFactory().configureIconButton(
                action,
                new HelpAction(helpFile, dialogService, preferences.getExternalApplicationsPreferences()),
                button);
        return button;
    }

    // endregion
}
