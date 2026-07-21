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
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
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
import org.jabref.gui.preferences.SearchableElement;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.help.HelpFile;

import com.dlsc.gemsfx.TagsField;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;

/// Fluent, eager builder that assembles a preference tab's node tree and wires all bindings. It
/// replaces the FXML + controller pair: each call creates a control, binds it to a view-model
/// property and appends it to the current container.
///
/// One rule governs the shape of a form: **the chain only ever adds elements; anything that
/// configures an element happens inside that element's lambda.** Nothing on this class configures
/// anything, so no call can land on the wrong node — there is no "current element" to get wrong:
///
/// ```java
/// form().section(Localization.lang("HTTP Server"), httpServer -> httpServer
///               .checkWithField(Localization.lang("Enable HTTP Server on port"),
///                       viewModel.enableHttpServerProperty(), viewModel.httpPortProperty(),
///                       port -> port.validate(viewModel.httpPortValidationStatus()))
///               .group(expert -> expert
///                       .stringField(Localization.lang("API base URL"), viewModel.apiBaseUrlProperty()),
///                   expertGroup -> expertGroup.disableWhen(viewModel.disableExpertSettingsProperty())))
///       .build();
/// ```
///
/// The handle a lambda receives states what it is: a {@link RowElement} carries a help button
/// because it sits in a row of its own, an {@link InputElement} does not; a {@link NodeElement}
/// offers neither tooltip nor validation because it is not a {@link Control}. Asking for the wrong
/// one does not compile.
///
/// Consecutive labelled fields share an aligned two-column {@link GridPane}; any full-width element
/// flushes that grid — use {@link #fields} to make a shared block explicit. Validation decoration is
/// collected and applied once on the FX thread in {@link #build()}.
public class PreferencesFormBuilder {

    /// Gap between elements, both inside a region and between regions.
    private static final double GAP = 10.0;
    /// Width of the inline value field of {@link #checkWithField} (sized for a port number; use
    /// {@link ControlElementBase#grow()} for longer values).
    private static final double SHORT_FIELD_WIDTH = 100.0;
    /// Minimum width of the caption column of the shared field grid.
    private static final double LABEL_COLUMN_MIN_WIDTH = 120.0;

    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final VBox root = new VBox(GAP);
    private final Deque<Pane> containers = new ArrayDeque<>();
    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();
    private final List<Runnable> validationInits = new ArrayList<>();

    /// Every visible text handed to the builder, paired with the node it captions. The
    /// preferences search matches against these and highlights the node.
    private final List<SearchableElement> searchableElements = new ArrayList<>();

    /// Disable bindings the builder installed itself (a value field following its checkbox, ...).
    /// {@link ElementBase#disableWhen} combines with these instead of silently replacing them.
    private final Map<Node, ObservableValue<? extends Boolean>> ownedDisableBindings = new IdentityHashMap<>();

    private GridPane currentGrid;
    private int gridRow;

    /// Depth of nested {@link #fields} blocks; inside one, only labelled fields may be added.
    private int fieldsDepth;

    private boolean built;

    /// The toggle group radios join inside {@link #radioGroup}.
    private ToggleGroup currentToggleGroup;

    public PreferencesFormBuilder(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.visualizer.setDecoration(new IconValidationDecorator());
        this.containers.push(root);
    }

    // region headers & static content

    /// A titled section. Its contents go in the lambda, so the grouping is visible in the source and
    /// the section can be configured or moved as a unit — rather than a header label followed by
    /// loose siblings that only look related because of their order.
    public PreferencesFormBuilder section(String title, Consumer<PreferencesFormBuilder> content) {
        return section(title, content, noConfig());
    }

    public PreferencesFormBuilder section(String title,
                                          Consumer<PreferencesFormBuilder> content,
                                          Consumer<FormRegion<VBox>> config) {
        return section(title, null, content, config);
    }

    public PreferencesFormBuilder sectionWithHelp(String title, HelpFile helpFile, Consumer<PreferencesFormBuilder> content) {
        return sectionWithHelp(title, helpFile, content, noConfig());
    }

    public PreferencesFormBuilder sectionWithHelp(String title,
                                                  HelpFile helpFile,
                                                  Consumer<PreferencesFormBuilder> content,
                                                  Consumer<FormRegion<VBox>> config) {
        return section(title, helpButton(StandardActions.HELP, helpFile), content, config);
    }

    /// Section whose help button points at a documentation URL rather than a {@link HelpFile}.
    public PreferencesFormBuilder sectionWithHelp(String title, String helpUrl, Consumer<PreferencesFormBuilder> content) {
        return sectionWithHelp(title, helpUrl, content, noConfig());
    }

    public PreferencesFormBuilder sectionWithHelp(String title,
                                                  String helpUrl,
                                                  Consumer<PreferencesFormBuilder> content,
                                                  Consumer<FormRegion<VBox>> config) {
        return section(title, new HelpButton(helpUrl), content, config);
    }

    /// The funnel all section variants go through: a "sectionHeader"-styled label, joined by the
    /// help button if there is one, followed by the section's own region.
    private PreferencesFormBuilder section(String title,
                                           Button help,
                                           Consumer<PreferencesFormBuilder> content,
                                           Consumer<FormRegion<VBox>> config) {
        Label header = new Label(title);
        searchable(title, header);
        header.getStyleClass().add("sectionHeader");
        if (help == null) {
            addNode(header);
        } else {
            header.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(header, Priority.ALWAYS);
            HBox row = new HBox(header, help);
            row.setAlignment(Pos.BASELINE_CENTER);
            addNode(row);
        }
        return region(new VBox(GAP), content, config);
    }

    /// A plain, unstyled caption line (for text that introduces the following controls).
    public PreferencesFormBuilder label(String text) {
        Label label = new Label(text);
        searchable(text, label);
        addNode(label);
        return this;
    }

    public PreferencesFormBuilder info(String text) {
        Label label = new Label(text);
        searchable(text, label);
        label.getStyleClass().add("italic");
        label.setPadding(new Insets(0, 0, 0, 20));
        addNode(label);
        return this;
    }

    // endregion

    // region controls

    public PreferencesFormBuilder checkbox(String text, Property<Boolean> value) {
        return checkbox(text, value, noConfig());
    }

    public PreferencesFormBuilder checkbox(String text, Property<Boolean> value, Consumer<RowElement<CheckBox>> config) {
        CheckBox checkBox = new CheckBox(text);
        searchable(text, checkBox);
        checkBox.setMaxWidth(Double.MAX_VALUE);
        // Consent and explanation labels run long; wrapping is never wrong for a short one.
        checkBox.setWrapText(true);
        checkBox.selectedProperty().bindBidirectional(value);
        HBox row = new HBox(GAP, checkBox);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new RowElement<>(this, checkBox, row), config);
    }

    /// A checkbox with an inline value field that is enabled only while the box is ticked (the
    /// recurring "Enable ... on port [....]" pattern). The configured element is the **value field**;
    /// its disable binding to the checkbox is preserved even if you add one of your own.
    public PreferencesFormBuilder checkWithField(String text, Property<Boolean> enabled, StringProperty fieldValue) {
        return checkWithField(text, enabled, fieldValue, noConfig());
    }

    public PreferencesFormBuilder checkWithField(String text,
                                                 Property<Boolean> enabled,
                                                 StringProperty fieldValue,
                                                 Consumer<RowElement<TextField>> config) {
        CheckBox checkBox = new CheckBox(text);
        searchable(text, checkBox);
        checkBox.selectedProperty().bindBidirectional(enabled);
        TextField field = new TextField();
        field.setMaxWidth(SHORT_FIELD_WIDTH);
        field.textProperty().bindBidirectional(fieldValue);
        ownDisable(field, checkBox.selectedProperty().not());
        HBox row = new HBox(GAP, checkBox, field);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new RowElement<>(this, field, row), config);
    }

    public PreferencesFormBuilder stringField(String label, StringProperty value) {
        return stringField(label, value, noConfig());
    }

    public PreferencesFormBuilder stringField(String label, StringProperty value, Consumer<InputElement<TextField>> config) {
        TextField field = new TextField();
        field.setMaxWidth(Double.MAX_VALUE);
        field.textProperty().bindBidirectional(value);
        addField(label, field);
        return configured(new InputElement<>(this, field), config);
    }

    /// A path field with a browse button. The configured element is the **text field**; the browse
    /// button follows its disabled state.
    public PreferencesFormBuilder browseField(String label, StringProperty value, Runnable onBrowse) {
        return browseField(label, value, onBrowse, noConfig());
    }

    public PreferencesFormBuilder browseField(String label,
                                              StringProperty value,
                                              Runnable onBrowse,
                                              Consumer<RowElement<TextField>> config) {
        BrowseFileEditor.Result result = BrowseFileEditor.create(value, onBrowse);
        if (label == null) {
            addNode(result.row());
        } else {
            addField(label, result.row());
        }
        return configured(new RowElement<>(this, result.field(), result.row()), config);
    }

    public PreferencesFormBuilder button(String text, JabRefIcon icon, Runnable action) {
        return button(text, icon, action, noConfig());
    }

    public PreferencesFormBuilder button(String text, JabRefIcon icon, Runnable action, Consumer<InputElement<Button>> config) {
        Button button = new Button(text);
        searchable(text, button);
        if (icon != null) {
            button.setGraphic(icon.getGraphicNode());
        }
        button.setOnAction(_ -> action.run());
        addNode(button);
        return configured(new InputElement<>(this, button), config);
    }

    public PreferencesFormBuilder hyperlink(String text, Runnable action) {
        return hyperlink(text, action, noConfig());
    }

    public PreferencesFormBuilder hyperlink(String text, Runnable action, Consumer<InputElement<Hyperlink>> config) {
        Hyperlink link = new Hyperlink(text);
        searchable(text, link);
        link.setOnAction(_ -> action.run());
        addNode(link);
        return configured(new InputElement<>(this, link), config);
    }

    /// Combo box whose items are bound to a (potentially changing) list property.
    public <X> PreferencesFormBuilder combo(String label,
                                            ObservableValue<? extends ObservableList<X>> items,
                                            Property<X> value,
                                            Callback<X, String> display) {
        return combo(label, items, value, display, noConfig());
    }

    public <X> PreferencesFormBuilder combo(String label,
                                            ObservableValue<? extends ObservableList<X>> items,
                                            Property<X> value,
                                            Callback<X, String> display,
                                            Consumer<InputElement<ComboBox<X>>> config) {
        ComboBox<X> combo = new ComboBox<>();
        combo.itemsProperty().bind(items);
        return addCombo(label, combo, value, display, config);
    }

    /// Combo box over a fixed item list (set directly, not bound to a property).
    public <X> PreferencesFormBuilder comboItems(String label,
                                                 ObservableList<X> items,
                                                 Property<X> value,
                                                 Callback<X, String> display) {
        return comboItems(label, items, value, display, noConfig());
    }

    public <X> PreferencesFormBuilder comboItems(String label,
                                                 ObservableList<X> items,
                                                 Property<X> value,
                                                 Callback<X, String> display,
                                                 Consumer<InputElement<ComboBox<X>>> config) {
        ComboBox<X> combo = new ComboBox<>();
        combo.setItems(items);
        return addCombo(label, combo, value, display, config);
    }

    public <X> PreferencesFormBuilder searchableCombo(String label,
                                                      ObservableValue<? extends ObservableList<X>> items,
                                                      Property<X> value,
                                                      Callback<X, String> display) {
        return searchableCombo(label, items, value, display, noConfig());
    }

    public <X> PreferencesFormBuilder searchableCombo(String label,
                                                      ObservableValue<? extends ObservableList<X>> items,
                                                      Property<X> value,
                                                      Callback<X, String> display,
                                                      Consumer<InputElement<ComboBox<X>>> config) {
        SearchableComboBox<X> combo = new SearchableComboBox<>();
        combo.itemsProperty().bind(items);
        return addCombo(label, combo, value, display, config);
    }

    /// Shared wiring for every combo variant. {@link SearchableComboBox} extends {@link ComboBox},
    /// so all flavours funnel through here; items are set/bound by the caller beforehand.
    private <X> PreferencesFormBuilder addCombo(String label,
                                                ComboBox<X> combo,
                                                Property<X> value,
                                                Callback<X, String> display,
                                                Consumer<InputElement<ComboBox<X>>> config) {
        new ViewModelListCellFactory<X>().withText(display).install(combo);
        combo.setMaxWidth(Double.MAX_VALUE);
        combo.valueProperty().bindBidirectional(value);
        addField(label, combo);
        return configured(new InputElement<>(this, combo), config);
    }

    /// A pre-built, pre-bound {@link TagsField} (see {@link TagsFieldEditor}).
    public <X> PreferencesFormBuilder tagsField(String label, TagsField<X> tagsField) {
        return tagsField(label, tagsField, noConfig());
    }

    public <X> PreferencesFormBuilder tagsField(String label,
                                                TagsField<X> tagsField,
                                                Consumer<InputElement<TagsField<X>>> config) {
        HBox.setHgrow(tagsField, Priority.ALWAYS);
        addField(label, tagsField);
        return configured(new InputElement<>(this, tagsField), config);
    }

    // endregion

    // region radio groups

    /// A mutually-exclusive radio group: radios added inside share one {@link ToggleGroup}, while
    /// each stays bound to its own boolean property (matching the existing view models). Unlike a
    /// {@link #group}, this adds no container — the radios stay in the surrounding layout, so there
    /// is nothing to configure afterwards. Wrap it in a {@link #group} to style the block.
    public PreferencesFormBuilder radioGroup(Consumer<PreferencesFormBuilder> content) {
        ToggleGroup enclosing = currentToggleGroup;
        currentToggleGroup = new ToggleGroup();
        content.accept(this);
        currentToggleGroup = enclosing;
        return this;
    }

    public PreferencesFormBuilder radio(String text, Property<Boolean> selected) {
        return radio(text, selected, noConfig());
    }

    public PreferencesFormBuilder radio(String text, Property<Boolean> selected, Consumer<RowElement<RadioButton>> config) {
        RadioButton radio = newRadio(text, selected);
        HBox row = new HBox(GAP, radio);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new RowElement<>(this, radio, row), config);
    }

    /// A radio with a bound text field that is enabled only while the radio is selected. The
    /// configured element is the **text field**.
    public PreferencesFormBuilder radioWithField(String text, Property<Boolean> selected, StringProperty fieldValue) {
        return radioWithField(text, selected, fieldValue, noConfig());
    }

    public PreferencesFormBuilder radioWithField(String text,
                                                 Property<Boolean> selected,
                                                 StringProperty fieldValue,
                                                 Consumer<RowElement<TextField>> config) {
        RadioButton radio = newRadio(text, selected);
        TextField field = new TextField();
        field.textProperty().bindBidirectional(fieldValue);
        ownDisable(field, radio.selectedProperty().not());
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox row = new HBox(GAP, radio, field);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new RowElement<>(this, field, row), config);
    }

    /// A radio with an inline browse field (e.g. "Main file directory"). The configured element is
    /// the **path field**; the browse button follows its disabled state.
    public PreferencesFormBuilder radioWithBrowse(String text,
                                                  Property<Boolean> selected,
                                                  StringProperty pathValue,
                                                  Runnable onBrowse) {
        return radioWithBrowse(text, selected, pathValue, onBrowse, noConfig());
    }

    public PreferencesFormBuilder radioWithBrowse(String text,
                                                  Property<Boolean> selected,
                                                  StringProperty pathValue,
                                                  Runnable onBrowse,
                                                  Consumer<RowElement<TextField>> config) {
        RadioButton radio = newRadio(text, selected);
        BrowseFileEditor.Result browse = BrowseFileEditor.create(pathValue, onBrowse);
        HBox.setHgrow(browse.row(), Priority.ALWAYS);
        HBox row = new HBox(GAP, radio, browse.row());
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new RowElement<>(this, browse.field(), row), config);
    }

    private RadioButton newRadio(String text, Property<Boolean> selected) {
        RadioButton radio = new RadioButton(text);
        searchable(text, radio);
        radio.setToggleGroup(currentToggleGroup);
        radio.selectedProperty().bindBidirectional(selected);
        return radio;
    }

    // endregion

    // region escape hatches

    /// Adds a bespoke labelled control.
    public <T extends Control> PreferencesFormBuilder field(String label, T control) {
        return field(label, control, noConfig());
    }

    public <T extends Control> PreferencesFormBuilder field(String label, T control, Consumer<InputElement<T>> config) {
        addField(label, control);
        return configured(new InputElement<>(this, control), config);
    }

    /// Adds a bespoke labelled node that is not a {@link Control} — a hand-assembled row, a table.
    public <T extends Node> PreferencesFormBuilder customField(String label, T node) {
        return customField(label, node, noConfig());
    }

    public <T extends Node> PreferencesFormBuilder customField(String label, T node, Consumer<NodeElement<T>> config) {
        addField(label, node);
        return configured(new NodeElement<>(this, node), config);
    }

    /// Adds a fully custom node spanning the form width (the `.custom(Node)` hatch).
    public <T extends Node> PreferencesFormBuilder custom(T node) {
        return custom(node, noConfig());
    }

    public <T extends Node> PreferencesFormBuilder custom(T node, Consumer<NodeElement<T>> config) {
        addNode(node);
        return configured(new NodeElement<>(this, node), config);
    }

    /// Registers a control the builder did not create — one inside a {@link #custom} region — with
    /// the same visualizer, so a tab needs no second {@link ControlsFxVisualizer} of its own.
    public PreferencesFormBuilder validate(ValidationStatus status, Control control) {
        validationInits.add(() -> visualizer.initVisualization(status, control));
        return this;
    }

    // endregion

    // region regions

    public PreferencesFormBuilder group(Consumer<PreferencesFormBuilder> content) {
        return group(content, noConfig());
    }

    public PreferencesFormBuilder group(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<VBox>> config) {
        return region(new VBox(GAP), content, config);
    }

    /// A side-by-side region: every element inside becomes an equally growing column. Usually filled
    /// with {@link #group} blocks, one per column.
    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content) {
        return columns(content, noConfig());
    }

    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<HBox>> config) {
        return region(new HBox(GAP), content, config);
    }

    /// A wrapping region: elements flow left to right and wrap onto the next line as the dialog
    /// narrows.
    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content) {
        return flow(content, noConfig());
    }

    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<FlowPane>> config) {
        return region(new FlowPane(), content, config);
    }

    /// An explicitly aligned block: every labelled field inside shares one {@link GridPane}, so
    /// their captions line up. Outside such a block consecutive fields still coalesce, but any
    /// full-width element silently splits them into separate grids with independent column widths —
    /// inside `fields(...)` that cannot happen, because adding one is an error.
    public PreferencesFormBuilder fields(Consumer<PreferencesFormBuilder> content) {
        return fields(content, noConfig());
    }

    public PreferencesFormBuilder fields(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<GridPane>> config) {
        flushGrid();
        GridPane grid = ensureGrid();
        fieldsDepth++;
        content.accept(this);
        fieldsDepth--;
        flushGrid();
        config.accept(new FormRegion<>(grid));
        return this;
    }

    /// Everything added inside becomes a sub-region of the form. Nesting is the lambda's, so a
    /// region cannot be left unclosed, and the second lambda configures the region as a whole —
    /// `group(content, g -> g.disableWhen(off))` disables all of its contents.
    private <T extends Pane> PreferencesFormBuilder region(T region,
                                                           Consumer<PreferencesFormBuilder> content,
                                                           Consumer<FormRegion<T>> config) {
        flushGrid();
        addToContainer(region);
        containers.push(region);

        content.accept(this);

        flushGrid();
        containers.pop();
        config.accept(new FormRegion<>(region));
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

    /// The visible texts of this form with the nodes they caption; see {@link SearchableElement}.
    public List<SearchableElement> getSearchableElements() {
        return List.copyOf(searchableElements);
    }

    // region internals

    private void searchable(String text, Node node) {
        searchableElements.add(new SearchableElement(text, node));
    }

    private <E> PreferencesFormBuilder configured(E element, Consumer<E> config) {
        config.accept(element);
        return this;
    }

    /// The empty configuration every no-config overload delegates with.
    private static <E> Consumer<E> noConfig() {
        return _ -> {
        };
    }

    /// Records a disable binding the builder owns, so that a later {@link ElementBase#disableWhen}
    /// extends it rather than replacing it.
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
    }

    /// Appends to the open container. Inside a {@link #columns} region every child is made an
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
            searchable(label, control);
            grid.add(new Label(label), 0, gridRow);
            grid.add(control, 1, gridRow);
        }
        GridPane.setHgrow(control, Priority.ALWAYS);
        gridRow++;
    }

    private GridPane ensureGrid() {
        if (currentGrid == null) {
            currentGrid = new GridPane();
            currentGrid.setHgap(GAP);
            currentGrid.setVgap(GAP);
            ColumnConstraints labelColumn = new ColumnConstraints();
            labelColumn.setMinWidth(LABEL_COLUMN_MIN_WIDTH);
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

    private Button helpButton(StandardActions action, HelpFile helpFile) {
        Button button = new Button();
        button.setPrefWidth(20.0);
        new ActionFactory().configureIconButton(
                action,
                new HelpAction(helpFile, dialogService, preferences.getExternalApplicationsPreferences()),
                button);
        return button;
    }

    // endregion

    /// A region the builder just closed, handed to that region's configuration lambda. Configuring a
    /// region is deliberately the same shape as configuring a control, so that no configuration
    /// method exists on the builder itself — a call aimed at the wrong thing cannot compile.
    public static final class FormRegion<T extends Pane> {

        private final T region;

        private FormRegion(T region) {
            this.region = region;
        }

        public T node() {
            return region;
        }

        public FormRegion<T> configure(Consumer<T> consumer) {
            consumer.accept(region);
            return this;
        }

        /// Disables the whole region while `condition` holds; disable propagates to every descendant,
        /// so its contents need no binding of their own.
        public FormRegion<T> disableWhen(ObservableValue<? extends Boolean> condition) {
            region.disableProperty().bind(condition);
            return this;
        }

        /// Binds the region's visibility, and its participation in layout, to `condition`.
        public FormRegion<T> visibleWhen(ObservableValue<? extends Boolean> condition) {
            region.visibleProperty().bind(condition);
            region.managedProperty().bind(condition);
            return this;
        }

        public FormRegion<T> styleClass(String... styleClasses) {
            region.getStyleClass().addAll(styleClasses);
            return this;
        }

        /// Overrides the gap between the region's elements (default {@value #GAP}).
        public FormRegion<T> spacing(double value) {
            switch (region) {
                case VBox box ->
                        box.setSpacing(value);
                case HBox box ->
                        box.setSpacing(value);
                case FlowPane pane -> {
                    pane.setHgap(value);
                    pane.setVgap(value);
                }
                case GridPane grid -> {
                    grid.setHgap(value);
                    grid.setVgap(value);
                }
                default ->
                        throw new IllegalStateException(
                                "spacing() does not apply to a " + region.getClass().getSimpleName() + " region");
            }
            return this;
        }
    }

    /// Base of the element handles: what can be done to any node the builder just added. Subclasses
    /// add what is only meaningful for a narrower kind of node, so that — unlike a single handle
    /// generic over `Node` — asking for a tooltip on a table, or for a help button on a control that
    /// sits in no row, is a compile error rather than an exception.
    ///
    /// `S` is the concrete handle type, so that a base method still returns the subclass and the
    /// order of a configuration chain does not matter.
    public abstract static sealed class ElementBase<S extends ElementBase<S, N>, N extends Node>
            permits NodeElement, ControlElementBase {

        final PreferencesFormBuilder form;
        final N node;

        ElementBase(PreferencesFormBuilder form, N node) {
            this.form = form;
            this.node = node;
        }

        /// The single unchecked cast of the handle hierarchy: `S` is always the concrete class of
        /// `this`, so base methods can return the subclass and chain order does not matter.
        @SuppressWarnings("unchecked")
        final S self() {
            return (S) this;
        }

        /// The node itself, e.g. to keep a reference for later wiring.
        public N node() {
            return node;
        }

        public S configure(Consumer<N> consumer) {
            consumer.accept(node);
            return self();
        }

        /// Disables the node while `condition` holds. Where the builder installed a disable binding
        /// of its own — the value field of {@link #checkWithField}, the path field of
        /// {@link #radioWithBrowse} — the two are combined, so the built-in coupling survives.
        public S disableWhen(ObservableValue<? extends Boolean> condition) {
            ObservableValue<? extends Boolean> owned = form.ownedDisableBindings.get(node);
            ObservableValue<? extends Boolean> effective = owned == null ? condition : either(owned, condition);
            node.disableProperty().unbind();
            node.disableProperty().bind(effective);
            if (owned != null) {
                form.ownedDisableBindings.put(node, effective);
            }
            return self();
        }

        public S visibleWhen(ObservableValue<? extends Boolean> condition) {
            node.visibleProperty().bind(condition);
            node.managedProperty().bind(condition);
            return self();
        }

        /// Statically disables the node (for platform-capability checks, not reactive state).
        public S disabled(boolean value) {
            node.setDisable(value);
            return self();
        }

        public S styleClass(String... styleClasses) {
            node.getStyleClass().addAll(styleClasses);
            return self();
        }
    }

    /// A node that is not a {@link Control}: a hand-assembled row, a table, a custom region.
    public static final class NodeElement<N extends Node> extends ElementBase<NodeElement<N>, N> {

        NodeElement(PreferencesFormBuilder form, N node) {
            super(form, node);
        }
    }

    /// A {@link Control}, so it can carry a tooltip and validation decoration, and — being a
    /// {@link Region} — can be told to take the remaining width.
    public abstract static sealed class ControlElementBase<S extends ControlElementBase<S, N>, N extends Control>
            extends ElementBase<S, N> permits InputElement, RowElement {

        ControlElementBase(PreferencesFormBuilder form, N control) {
            super(form, control);
        }

        public S tooltip(String text) {
            node.setTooltip(new Tooltip(text));
            return self();
        }

        /// Decorates the control with `status`, applied once on the FX thread in {@link #build()}.
        public S validate(ValidationStatus status) {
            form.validate(status, node);
            return self();
        }

        /// Lets the control take all remaining horizontal space in its row. Use where a builder
        /// default is too narrow, e.g. the value field of {@link #checkWithField} when it holds a
        /// name rather than a port number.
        public S grow() {
            node.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(node, Priority.ALWAYS);
            return self();
        }
    }

    /// A control the builder placed in a labelled grid row or a bare column.
    public static final class InputElement<N extends Control> extends ControlElementBase<InputElement<N>, N> {

        InputElement(PreferencesFormBuilder form, N control) {
            super(form, control);
        }
    }

    /// A control the builder placed in a row of its own, which is therefore the only kind that can
    /// take a trailing help button.
    public static final class RowElement<N extends Control> extends ControlElementBase<RowElement<N>, N> {

        private final HBox row;

        RowElement(PreferencesFormBuilder form, N control, HBox row) {
            super(form, control);
            this.row = row;
        }

        public RowElement<N> help(HelpFile helpFile) {
            return help(StandardActions.HELP, helpFile);
        }

        public RowElement<N> help(StandardActions action, HelpFile helpFile) {
            row.getChildren().add(form.helpButton(action, helpFile));
            return this;
        }

        /// Appends a help icon button linking to a documentation URL.
        public RowElement<N> help(String helpUrl) {
            row.getChildren().add(new HelpButton(helpUrl));
            return this;
        }
    }
}
