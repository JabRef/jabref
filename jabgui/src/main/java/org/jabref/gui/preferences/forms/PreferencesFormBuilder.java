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
import javafx.scene.control.ToggleButton;
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
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.gui.preferences.SearchableElement;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

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
/// The handle a lambda receives states what it is: an {@link InputElement} is a {@link Control},
/// so it can carry a tooltip, validation and attachments — a help button, a browse button, an
/// inline value field appended right after it; a {@link NodeElement} offers none of that because
/// it is not a {@link Control}. Asking for the wrong one does not compile.
///
/// Consecutive labelled fields share an aligned two-column {@link GridPane}; any full-width element
/// flushes that grid — use {@link #fields} to make a shared block explicit. Validation decoration is
/// collected and applied once on the FX thread in {@link #build()}.
public class PreferencesFormBuilder {

    /// Gap between elements, both inside a region and between regions.
    private static final double GAP = 10.0;
    /// Width of the inline value field of {@link #checkWithField} (sized for a port number; use
    /// {@link InputElement#grow()} for longer values).
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

    /// The configuration lambda addresses the section as a whole — including its header, which is
    /// where a {@link SectionRegion#help help button} attaches.
    public PreferencesFormBuilder section(String title,
                                          Consumer<PreferencesFormBuilder> content,
                                          Consumer<SectionRegion> config) {
        Label header = new Label(title);
        searchable(title, header);
        header.getStyleClass().add("sectionHeader");
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(header, Priority.ALWAYS);
        HBox headerRow = new HBox(header);
        headerRow.setAlignment(Pos.BASELINE_CENTER);
        addNode(headerRow);

        return configured(new SectionRegion(this, region(new VBox(GAP), content), header), config);
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

    public PreferencesFormBuilder checkbox(String text, Property<Boolean> value, Consumer<InputElement<CheckBox>> config) {
        CheckBox checkBox = new CheckBox(text);
        searchable(text, checkBox);
        checkBox.setMaxWidth(Double.MAX_VALUE);
        // Consent and explanation labels run long; wrapping is never wrong for a short one.
        checkBox.setWrapText(true);
        checkBox.selectedProperty().bindBidirectional(value);
        HBox row = new HBox(GAP, checkBox);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new InputElement<>(this, checkBox), config);
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
                                                 Consumer<InputElement<TextField>> config) {
        return checkbox(text, enabled, box -> box.attachField(fieldValue, field -> {
            field.node().setMaxWidth(SHORT_FIELD_WIDTH);
            config.accept(field);
        }));
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

    public PreferencesFormBuilder radio(String text, Property<Boolean> selected, Consumer<InputElement<RadioButton>> config) {
        RadioButton radio = newRadio(text, selected);
        HBox row = new HBox(GAP, radio);
        row.setAlignment(Pos.CENTER_LEFT);
        addNode(row);
        return configured(new InputElement<>(this, radio), config);
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
        return configured(new FormRegion<>(this, region(new VBox(GAP), content)), config);
    }

    /// A side-by-side region: every element inside becomes an equally growing column. Usually filled
    /// with {@link #group} blocks, one per column.
    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content) {
        return columns(content, noConfig());
    }

    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<HBox>> config) {
        return configured(new FormRegion<>(this, region(new HBox(GAP), content)), config);
    }

    /// A wrapping region: elements flow left to right and wrap onto the next line as the dialog
    /// narrows.
    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content) {
        return flow(content, noConfig());
    }

    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<FlowPane>> config) {
        return configured(new FormRegion<>(this, region(new FlowPane(), content)), config);
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
        return configured(new FormRegion<>(this, grid), config);
    }

    /// Everything added inside becomes a sub-region of the form. Nesting is the lambda's, so a
    /// region cannot be left unclosed; the caller wraps the returned pane in the handle its
    /// configuration lambda expects — `group(content, g -> g.disableWhen(off))` disables all of
    /// its contents.
    private <T extends Pane> T region(T region, Consumer<PreferencesFormBuilder> content) {
        flushGrid();
        addToContainer(region);
        containers.push(region);

        content.accept(this);

        flushGrid();
        containers.pop();
        return region;
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

    /// Places `attachment` directly after `primary`: appended to the primary's row if it sits in
    /// an {@link HBox}, otherwise by wrapping the primary's grid cell into a row. Wrapping keeps
    /// the cell's position and span, and the primary keeps growing while the attachment does not.
    private void attachTo(Node primary, Node attachment) {
        switch (primary.getParent()) {
            case HBox row ->
                    row.getChildren().add(attachment);
            case GridPane grid -> {
                Integer columnIndex = GridPane.getColumnIndex(primary);
                Integer rowIndex = GridPane.getRowIndex(primary);
                Integer columnSpan = GridPane.getColumnSpan(primary);
                grid.getChildren().remove(primary);
                HBox wrapper = new HBox(GAP, primary, attachment);
                wrapper.setAlignment(Pos.CENTER_LEFT);
                HBox.setHgrow(primary, Priority.ALWAYS);
                GridPane.setHgrow(wrapper, Priority.ALWAYS);
                grid.add(wrapper,
                        columnIndex == null ? 0 : columnIndex,
                        rowIndex == null ? 0 : rowIndex,
                        columnSpan == null ? 1 : columnSpan,
                        1);
            }
            case null ->
                    throw new IllegalStateException("the control has not been placed yet; attach from within its config lambda");
            default ->
                    throw new IllegalStateException("cannot attach to a control sitting in a "
                            + primary.getParent().getClass().getSimpleName());
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

    /// Base of the region handles, handed to a region's configuration lambda once the builder has
    /// closed it. Configuring a region is deliberately the same shape as configuring an element,
    /// so that no configuration method exists on the builder itself — a call aimed at the wrong
    /// thing cannot compile. As with {@link ElementBase}, `S` is the concrete handle type, so a
    /// base method still returns the subclass and the order of a chain does not matter.
    public abstract static sealed class RegionBase<S extends RegionBase<S, T>, T extends Pane>
            permits FormRegion, SectionRegion {

        final PreferencesFormBuilder form;
        final T region;

        RegionBase(PreferencesFormBuilder form, T region) {
            this.form = form;
            this.region = region;
        }

        @SuppressWarnings("unchecked")
        final S self() {
            return (S) this;
        }

        public T node() {
            return region;
        }

        public S configure(Consumer<T> consumer) {
            consumer.accept(region);
            return self();
        }

        /// Disables the whole region while `condition` holds; disable propagates to every descendant,
        /// so its contents need no binding of their own.
        public S disableWhen(ObservableValue<? extends Boolean> condition) {
            region.disableProperty().bind(condition);
            return self();
        }

        /// Binds the region's visibility, and its participation in layout, to `condition`.
        public S visibleWhen(ObservableValue<? extends Boolean> condition) {
            region.visibleProperty().bind(condition);
            region.managedProperty().bind(condition);
            return self();
        }

        public S styleClass(String... styleClasses) {
            region.getStyleClass().addAll(styleClasses);
            return self();
        }

        /// Overrides the gap between the region's elements (default {@value PreferencesFormBuilder#GAP}).
        public S spacing(double value) {
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
            return self();
        }
    }

    /// A plain region: a {@link #group}, {@link #columns}, {@link #flow} or {@link #fields} block,
    /// which has contents but no heading of its own.
    public static final class FormRegion<T extends Pane> extends RegionBase<FormRegion<T>, T> {

        FormRegion(PreferencesFormBuilder form, T region) {
            super(form, region);
        }
    }

    /// The region of a {@link #section}: unlike a plain region it has a header, which is therefore
    /// the only kind of region that can take a help button.
    public static final class SectionRegion extends RegionBase<SectionRegion, VBox> {

        private final Label header;

        SectionRegion(PreferencesFormBuilder form, VBox region, Label header) {
            super(form, region);
            this.header = header;
        }

        /// Attaches a help icon button to the section header, right-aligned in its row.
        public SectionRegion help(HelpFile helpFile) {
            return help(StandardActions.HELP, helpFile);
        }

        public SectionRegion help(StandardActions action, HelpFile helpFile) {
            form.attachTo(header, form.helpButton(action, helpFile));
            return this;
        }

        /// Attaches a help icon button linking to a documentation URL.
        public SectionRegion help(String helpUrl) {
            form.attachTo(header, new HelpButton(helpUrl));
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
            permits NodeElement, InputElement {

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
        /// of its own — an {@link InputElement#attachField attached field} following its toggle —
        /// the two are combined, so the built-in coupling survives.
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

    /// A {@link Control} the builder placed. Being a control, it can carry a tooltip and
    /// validation decoration, be told to take the remaining width — and take **attachments**:
    /// nodes appended right after it (a help button, a browse button, an inline value field) that
    /// stay coupled to it instead of floating free in the layout.
    public static final class InputElement<N extends Control> extends ElementBase<InputElement<N>, N> {

        InputElement(PreferencesFormBuilder form, N control) {
            super(form, control);
        }

        public InputElement<N> tooltip(String text) {
            node.setTooltip(new Tooltip(text));
            return this;
        }

        /// Decorates the control with `status`, applied once on the FX thread in {@link #build()}.
        public InputElement<N> validate(ValidationStatus status) {
            form.validate(status, node);
            return this;
        }

        /// Lets the control take all remaining horizontal space in its row. Use where a builder
        /// default is too narrow, e.g. the value field of {@link #checkWithField} when it holds a
        /// name rather than a port number.
        public InputElement<N> grow() {
            node.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(node, Priority.ALWAYS);
            return this;
        }

        // region attachments

        /// Attaches an arbitrary node right after this control; it follows the control's disabled
        /// state. The config lambda addresses the attachment.
        public <A extends Node> InputElement<N> attach(A attachment) {
            return attach(attachment, noConfig());
        }

        public <A extends Node> InputElement<N> attach(A attachment, Consumer<NodeElement<A>> config) {
            attachment.disableProperty().bind(node.disableProperty());
            form.attachTo(node, attachment);
            config.accept(new NodeElement<>(form, attachment));
            return this;
        }

        /// Attaches a text field bound to `value`. On a checkbox or radio the field is enabled
        /// only while the toggle is selected (the recurring "option with inline value" pattern);
        /// on any other control it follows the control's disabled state. The config lambda
        /// addresses the new field.
        public InputElement<N> attachField(StringProperty value) {
            return attachField(value, noConfig());
        }

        public InputElement<N> attachField(StringProperty value, Consumer<InputElement<TextField>> config) {
            TextField field = new TextField();
            field.textProperty().bindBidirectional(value);
            field.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(field, Priority.ALWAYS);
            switch (node) {
                case CheckBox box ->
                        form.ownDisable(field, box.selectedProperty().not());
                case ToggleButton toggle ->
                        form.ownDisable(field, toggle.selectedProperty().not());
                default ->
                        field.disableProperty().bind(node.disableProperty());
            }
            form.attachTo(node, field);
            config.accept(new InputElement<>(form, field));
            return this;
        }

        /// Attaches a "browse" icon button that follows this control's disabled state.
        public InputElement<N> browse(Runnable onBrowse) {
            Button browseButton = new Button();
            browseButton.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
            browseButton.getStyleClass().addAll("icon-button", "narrow");
            browseButton.setPrefSize(20.0, 20.0);
            browseButton.setTooltip(new Tooltip(Localization.lang("Browse")));
            browseButton.disableProperty().bind(node.disableProperty());
            browseButton.setOnAction(_ -> onBrowse.run());
            form.attachTo(node, browseButton);
            return this;
        }

        /// Attaches a help icon button. Help stays clickable even while the control is disabled.
        public InputElement<N> help(HelpFile helpFile) {
            return help(StandardActions.HELP, helpFile);
        }

        public InputElement<N> help(StandardActions action, HelpFile helpFile) {
            form.attachTo(node, form.helpButton(action, helpFile));
            return this;
        }

        /// Attaches a help icon button linking to a documentation URL.
        public InputElement<N> help(String helpUrl) {
            form.attachTo(node, new HelpButton(helpUrl));
            return this;
        }

        // endregion
    }
}
