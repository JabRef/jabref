package org.jabref.gui.preferences.forms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
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
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
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

    /// Disable bindings the builder installed itself (a value field following its checkbox, ...).
    /// {@link #disableWhen} combines with these instead of silently replacing them.
    private final Map<Node, ObservableValue<? extends Boolean>> ownedDisableBindings = new IdentityHashMap<>();

    private GridPane currentGrid;
    private int gridRow;

    /// Depth of nested {@link #fields} blocks; inside one, only labelled fields may be added.
    private int fieldsDepth;

    private boolean built;

    /// The toggle group radios join inside {@link #radioGroup}.
    private ToggleGroup currentToggleGroup;

    /// The control that trailing configuration methods act on. Null where there is no meaningful
    /// subject (after {@link #radioGroup}, which adds no container of its own), so that misaimed
    /// configuration fails instead of silently landing on the previous element.
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
        ownDisable(field, checkBox.selectedProperty().not());
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

    /// A mutually-exclusive radio group: radios added inside share one {@link ToggleGroup}, while
    /// each stays bound to its own boolean property (matching the existing view models). Unlike a
    /// {@link #group}, this adds no container — the radios stay in the surrounding layout.
    public PreferencesFormBuilder radioGroup(Consumer<PreferencesFormBuilder> content) {
        ToggleGroup enclosing = currentToggleGroup;
        currentToggleGroup = new ToggleGroup();
        content.accept(this);
        currentToggleGroup = enclosing;
        // Deliberately no subject afterwards: a radio group is not a node, so trailing configuration
        // would silently land on its last radio. Wrap it in group(...) to configure the block.
        lastControl = null;
        lastRow = null;
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
        ownDisable(field, radio.selectedProperty().not());
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

    /// Runs arbitrary configuration on the last added element, typed — e.g. to keep a reference to a
    /// control the builder created: `.configure(ComboBox.class, combo -> this.combo = combo)`.
    public <T extends Node> PreferencesFormBuilder configure(Class<T> type, Consumer<T> consumer) {
        consumer.accept(subject("configure", type));
        return this;
    }

    // endregion

    // region grouping & post-configuration

    /// Opens a sub-group: controls added until {@link #endGroup()} are placed inside it. The group
    /// becomes the current subject, so the universal {@link #visibleWhen} / {@link #disableWhen}
    /// (called before adding children) bind the whole group — e.g.
    /// `beginGroup().visibleWhen(expertOn)` or `beginGroup().disableWhen(aiOff)` (disable propagates
    /// to every descendant). Both are combinable.
    public PreferencesFormBuilder group(Consumer<PreferencesFormBuilder> content) {
        return region(new VBox(10.0), content);
    }

    /// A side-by-side region: every element inside becomes an equally growing column. Usually filled
    /// with {@link #group} blocks, one per column.
    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content) {
        return region(new HBox(10.0), content);
    }

    /// A wrapping region: elements flow left to right and wrap onto the next line as the dialog
    /// narrows.
    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content) {
        return region(new FlowPane(), content);
    }

    /// An explicitly aligned block: every labelled field inside shares one {@link GridPane}, so
    /// their captions line up. Outside such a block consecutive fields still coalesce, but any
    /// full-width element silently splits them into separate grids with independent column widths —
    /// inside `fields(...)` that cannot happen, because adding one is an error.
    public PreferencesFormBuilder fields(Consumer<PreferencesFormBuilder> content) {
        flushGrid();
        GridPane grid = ensureGrid();
        fieldsDepth++;
        content.accept(this);
        fieldsDepth--;
        flushGrid();
        lastControl = grid;
        lastRow = null;
        return this;
    }

    /// Everything added inside becomes a sub-region of the form. Nesting is the lambda's, so a
    /// region cannot be left unclosed, and trailing configuration after the call applies to the
    /// region as a whole — `group(g -> ...).disableWhen(off)` disables all of its contents.
    private PreferencesFormBuilder region(Pane region, Consumer<PreferencesFormBuilder> content) {
        flushGrid();
        addToContainer(region);
        containers.push(region);
        lastControl = region;
        lastRow = null;

        content.accept(this);

        flushGrid();
        containers.pop();
        // The region itself, not its last child, is the subject of what follows.
        lastControl = region;
        lastRow = null;
        return this;
    }

    /// Overrides the spacing of the last region (default 10). On a {@link #flow} region this sets
    /// both gaps.
    public PreferencesFormBuilder spacing(double value) {
        switch (subject("spacing", Pane.class)) {
            case VBox box ->
                    box.setSpacing(value);
            case HBox box ->
                    box.setSpacing(value);
            case FlowPane pane -> {
                pane.setHgap(value);
                pane.setVgap(value);
            }
            default ->
                    throw new IllegalStateException(
                            "spacing() applies to a group, columns or flow region, not to a "
                                    + lastControl.getClass().getSimpleName());
        }
        return this;
    }

    /// Disables the last subject while `condition` holds. Where the builder already installed a
    /// disable binding of its own — the value field of {@link #checkWithField}, the path field of
    /// {@link #radioWithBrowse} — the two are combined, so the built-in coupling survives.
    public PreferencesFormBuilder disableWhen(ObservableValue<? extends Boolean> condition) {
        Node target = subject("disableWhen");
        ObservableValue<? extends Boolean> owned = ownedDisableBindings.get(target);
        ObservableValue<? extends Boolean> effective = owned == null ? condition : either(owned, condition);
        target.disableProperty().unbind();
        target.disableProperty().bind(effective);
        if (owned != null) {
            ownedDisableBindings.put(target, effective);
        }
        return this;
    }

    /// Binds the last subject's visibility (and layout participation) to `condition`. Works on any
    /// control or on a region opened with {@link #group}, {@link #columns} or {@link #flow}.
    public PreferencesFormBuilder visibleWhen(ObservableValue<? extends Boolean> condition) {
        Node target = subject("visibleWhen");
        target.visibleProperty().bind(condition);
        target.managedProperty().bind(condition);
        return this;
    }

    /// Statically disables the last subject (for platform-capability checks, not reactive state).
    public PreferencesFormBuilder disabled(boolean value) {
        subject("disabled").setDisable(value);
        return this;
    }

    public PreferencesFormBuilder tooltip(String text) {
        subject("tooltip", Control.class).setTooltip(new Tooltip(text));
        return this;
    }

    /// Decorates the last subject with `status`. Decoration is applied once, on the FX thread, in
    /// {@link #build()}.
    public PreferencesFormBuilder validate(ValidationStatus status) {
        return validate(status, subject("validate", Control.class));
    }

    /// Registers a control the builder did not create — a control inside a {@link #custom} region —
    /// with the same visualizer, so a tab needs no second {@link ControlsFxVisualizer} of its own.
    public PreferencesFormBuilder validate(ValidationStatus status, Control control) {
        validationInits.add(() -> visualizer.initVisualization(status, control));
        return this;
    }

    /// Appends a help icon button to the current row (checkbox, checkWithField or section row).
    public PreferencesFormBuilder help(HelpFile helpFile) {
        return help(StandardActions.HELP, helpFile);
    }

    public PreferencesFormBuilder help(StandardActions action, HelpFile helpFile) {
        row("help").getChildren().add(helpButton(action, helpFile));
        return this;
    }

    /// Appends a help icon button linking to a documentation URL.
    public PreferencesFormBuilder help(String helpUrl) {
        row("help").getChildren().add(new HelpButton(helpUrl));
        return this;
    }

    /// Lets the last subject take all remaining horizontal space in its row. Use where a builder
    /// default is too narrow, e.g. the value field of {@link #checkWithField} when it holds a name
    /// rather than a port number.
    public PreferencesFormBuilder grow() {
        Region target = subject("grow", Region.class);
        target.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(target, Priority.ALWAYS);
        return this;
    }

    /// Wraps the last subject's text over several lines instead of truncating it.
    public PreferencesFormBuilder wrapText() {
        subject("wrapText", Labeled.class).setWrapText(true);
        return this;
    }

    /// Adds CSS style classes to the last subject (control or region), e.g. `"prefIndent"`.
    public PreferencesFormBuilder styleClass(String... styleClasses) {
        subject("styleClass").getStyleClass().addAll(styleClasses);
        return this;
    }

    // endregion

    public VBox build() {
        if (built) {
            throw new IllegalStateException("build() was already called; a form builder assembles one tree");
        }
        if (containers.size() != 1) {
            throw new IllegalStateException("a region is still open — " + (containers.size() - 1) + " unclosed");
        }
        built = true;
        flushGrid();
        Platform.runLater(() -> validationInits.forEach(Runnable::run));
        return root;
    }

    // region internals

    /// The element trailing configuration applies to. Every accessor fails loudly rather than
    /// no-opping: a misaimed `.validate(...)` or `.help(...)` is otherwise invisible until someone
    /// notices the decoration or the button is missing from the running dialog.
    private Node subject(String method) {
        if (lastControl == null) {
            throw new IllegalStateException(method + "() has no element to apply to. radioGroup() adds no "
                    + "container of its own — wrap it in group(...) to configure the block as a whole");
        }
        return lastControl;
    }

    private <T> T subject(String method, Class<T> required) {
        Node target = subject(method);
        if (!required.isInstance(target)) {
            throw new IllegalStateException(method + "() applies to a " + required.getSimpleName()
                    + ", but the current element is a " + target.getClass().getSimpleName());
        }
        return required.cast(target);
    }

    private HBox yrow(String method) {
        if (lastRow == null) {
            throw new IllegalStateException(method + "() appends to the current row, but "
                    + (lastControl == null ? "there is no current element" : lastControl.getClass().getSimpleName()
                                                                             + " does not sit in one"));
        }
        return lastRow;
    }

    /// Records a disable binding the builder owns, so that a later {@link #disableWhen} extends it
    /// rather than replacing it.
    private void ownDisable(Node node, ObservableValue<? extends Boolean> condition) {
        node.disableProperty().bind(condition);
        ownedDisableBindings.put(node, condition);
    }

    private static ObservableValue<Boolean> either(ObservableValue<? extends Boolean> first,
                                                   ObservableValue<? extends Boolean> second) {
        return Bindings.createBooleanBinding(
                () -> Boolean.TRUE.equals(first.getValue()) || Boolean.TRUE.equals(second.getValue()),
                first, second);
    }

    private Pane container() {
        return containers.peek();
    }

    private void addNode(Node node) {
        if (fieldsDepth > 0) {
            throw new IllegalStateException("only labelled fields may be added inside fields(...); "
                    + node.getClass().getSimpleName() + " would break the shared column alignment");
        }
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
        if (label == null) {
            // No caption: span both columns rather than leaving an empty label column.
            grid.add(control, 0, gridRow, 2, 1);
        } else {
            grid.add(new Label(label), 0, gridRow);
            grid.add(control, 1, gridRow);
        }
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
