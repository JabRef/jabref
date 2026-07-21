package org.jabref.gui.preferences.forms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.property.ListProperty;
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
import javafx.scene.layout.Region;
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
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.help.HelpFile;

import com.dlsc.gemsfx.TagsField;
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
        return sectionWithHelp(text, helpButton(StandardActions.HELP, helpFile));
    }

    /// Section header with a help button pointing at a documentation URL rather than a {@link HelpFile}.
    public PreferencesFormBuilder sectionWithHelp(String text, String helpUrl) {
        return sectionWithHelp(text, new HelpButton(helpUrl));
    }

    private PreferencesFormBuilder sectionWithHelp(String text, javafx.scene.control.Button help) {
        flushGrid();
        Label header = new Label(text);
        header.getStyleClass().add("sectionHeader");
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(header, Priority.ALWAYS);
        HBox row = new HBox(header, help);
        row.setAlignment(Pos.BASELINE_CENTER);
        addToContainer(row);
        lastRow = row;
        lastControl = header;
        return this;
    }

    /// A plain, unstyled caption line (for text that introduces the following controls).
    public PreferencesFormBuilder label(String text) {
        Label label = new Label(text);
        addNode(label);
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

    /// Combo box whose items are bound to a (potentially changing) list property.
    public <X> PreferencesFormBuilder combo(String label,
                                            ObservableValue<? extends ObservableList<X>> items,
                                            Property<X> value,
                                            Callback<X, String> display) {
        ComboBox<X> combo = new ComboBox<>();
        combo.itemsProperty().bind(items);
        return addCombo(label, combo, value, display, false, null);
    }

    /// Combo box over a fixed item list (set directly, not bound to a property).
    public <X> PreferencesFormBuilder comboItems(String label,
                                                 ObservableList<X> items,
                                                 Property<X> value,
                                                 Callback<X, String> display) {
        ComboBox<X> combo = new ComboBox<>();
        combo.setItems(items);
        return addCombo(label, combo, value, display, false, null);
    }

    public <X> PreferencesFormBuilder searchableCombo(String label,
                                                      ObservableValue<? extends ObservableList<X>> items,
                                                      Property<X> value,
                                                      Callback<X, String> display) {
        SearchableComboBox<X> combo = new SearchableComboBox<>();
        combo.itemsProperty().bind(items);
        return addCombo(label, combo, value, display, false, null);
    }

    /// String combo box (optionally editable, with a prompt). No display callback (identity toString).
    public PreferencesFormBuilder stringCombo(String label,
                                              ObservableValue<? extends ObservableList<String>> items,
                                              Property<String> value,
                                              boolean editable,
                                              String prompt) {
        ComboBox<String> combo = new ComboBox<>();
        combo.itemsProperty().bind(items);
        return addCombo(label, combo, value, null, editable, prompt);
    }

    /// Shared wiring for every combo variant. {@link SearchableComboBox} extends {@link ComboBox},
    /// so all flavours funnel through here; items are set/bound by the caller beforehand.
    private <X> PreferencesFormBuilder addCombo(String label,
                                                ComboBox<X> combo,
                                                Property<X> value,
                                                Callback<X, String> display,
                                                boolean editable,
                                                String prompt) {
        if (display != null) {
            new ViewModelListCellFactory<X>().withText(display).install(combo);
        }
        combo.setEditable(editable);
        combo.setMaxWidth(Double.MAX_VALUE);
        if (prompt != null) {
            combo.setPromptText(prompt);
        }
        combo.valueProperty().bindBidirectional(value);
        addField(label, combo);
        return this;
    }

    /// A pre-built {@link TagsField} (see {@link TagsFieldEditor}) bound to a list property.
    public <T> PreferencesFormBuilder tagsField(String label, TagsField<T> tagsField, ListProperty<T> value) {
        tagsField.tagsProperty().bindBidirectional(value);
        HBox.setHgrow(tagsField, Priority.ALWAYS);
        addField(label, tagsField);
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

    /// Opens a sub-group: controls added until {@link #endGroup()} are placed inside it. The group
    /// becomes the current subject, so the universal {@link #visibleWhen} / {@link #disableWhen}
    /// (called before adding children) bind the whole group — e.g.
    /// `beginGroup().visibleWhen(expertOn)` or `beginGroup().disableWhen(aiOff)` (disable propagates
    /// to every descendant). Both are combinable.
    public PreferencesFormBuilder beginGroup() {
        flushGrid();
        VBox group = new VBox(10.0);
        addToContainer(group);
        containers.push(group);
        lastControl = group;
        lastRow = null;
        return this;
    }

    /// Opens a side-by-side region: every element added until {@link #endColumns()} becomes an
    /// equally growing column. Usually filled with {@link #beginGroup()} blocks, one per column.
    public PreferencesFormBuilder beginColumns() {
        flushGrid();
        HBox columns = new HBox(10.0);
        addToContainer(columns);
        containers.push(columns);
        lastControl = columns;
        lastRow = null;
        return this;
    }

    public PreferencesFormBuilder endColumns() {
        return endGroup();
    }

    public PreferencesFormBuilder endGroup() {
        flushGrid();
        if (containers.size() > 1) {
            containers.pop();
        }
        return this;
    }

    /// Overrides the spacing of the last group or column region (default 10).
    public PreferencesFormBuilder spacing(double value) {
        if (lastControl instanceof VBox box) {
            box.setSpacing(value);
        } else if (lastControl instanceof HBox box) {
            box.setSpacing(value);
        }
        return this;
    }

    public PreferencesFormBuilder disableWhen(ObservableValue<? extends Boolean> condition) {
        lastControl.disableProperty().bind(condition);
        return this;
    }

    /// Binds the last subject's visibility (and layout participation) to `condition`. Works on any
    /// control or on a group opened with {@link #beginGroup()}.
    public PreferencesFormBuilder visibleWhen(ObservableValue<? extends Boolean> condition) {
        lastControl.visibleProperty().bind(condition);
        lastControl.managedProperty().bind(condition);
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

    /// Appends a help icon button linking to a documentation URL.
    public PreferencesFormBuilder help(String helpUrl) {
        if (lastRow != null) {
            lastRow.getChildren().add(new HelpButton(helpUrl));
        }
        return this;
    }

    /// Adds CSS style classes to the last subject (control or group), e.g. `"prefIndent"`.
    public PreferencesFormBuilder styleClass(String... styleClasses) {
        lastControl.getStyleClass().addAll(styleClasses);
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
        addToContainer(node);
        lastControl = node;
        lastRow = null;
    }

    /// Appends to the open container. Inside a {@link #beginColumns()} region every child is made an
    /// equally growing column, so callers never repeat the hgrow boilerplate.
    private void addToContainer(Node node) {
        Pane container = container();
        container.getChildren().add(node);
        if (container instanceof HBox) {
            HBox.setHgrow(node, Priority.ALWAYS);
            if (node instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
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
            addToContainer(currentGrid);
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
