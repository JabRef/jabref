package org.jabref.gui.preferences.forms;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableView;
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
import org.jabref.gui.theme.StyleClasses;
import org.jabref.gui.util.IconValidationDecorator;
import org.jabref.gui.util.ViewModelListCellFactory;
import org.jabref.gui.util.component.HelpButton;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.strings.StringUtil;

import com.dlsc.gemsfx.TagsField;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import de.saxsys.mvvmfx.utils.validation.visualization.ControlsFxVisualizer;
import org.controlsfx.control.SearchableComboBox;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import static org.jabref.gui.preferences.forms.FormMetrics.GAP;

/// Fluent, eager builder that assembles a preference tab's node tree and wires all bindings. It
/// replaces the FXML + controller pair: each call creates a control, binds it to a view-model
/// property and appends it to the current container.
///
/// One rule governs the shape of a form: **the chain only ever adds elements; anything that
/// configures an element happens inside that element's lambda.** Nothing on this class configures
/// anything, so no call can land on the wrong node — there is no "current element" to get wrong:
///
/// ```java
/// form()
///         // A titled section. Its contents are the chain inside the lambda, so the grouping
///         // is visible in the source; the trailing lambda addresses the section as a whole.
///         .section("HTTP Server", httpServer -> httpServer
///
///                 // A checkbox with a value field beside it, enabled while the box is ticked.
///                 // Convenience sugar for checkbox(text, enabled, box -> box.attachField(...).
///                 // The lambda addresses that field, not the checkbox.
///                 .checkWithField("Enable HTTP Server on port",
///                         viewModel.enableHttpServerProperty(),
///                         viewModel.httpPortProperty(),
///                         port -> port.validate(viewModel.httpPortValidationStatus())
///                                     .help(HelpFile.REMOTE))
///
///                 // A group of related elements, can be disabled as one.
///                 .group(expert -> expert
///                         .stringField("API base URL", viewModel.apiBaseUrlProperty()),
///                         expertGroup -> expertGroup.disableWhen(viewModel.disableExpertSettingsProperty())),
///
///                 // Documentation for the section as a whole: a help button beside its heading.
///                 httpServerSection -> httpServerSection.help("https://docs.jabref.org/advanced/remote"))
///         .section("Another section", section -> section
///                 ...)
///         .build();
/// ```
///
/// The texts are written out above for readability; in a tab they are `Localization.lang(...)`
/// calls.
///
/// The handle a lambda receives states what it is: an [InputElement] is a [Control],
/// so it can carry a tooltip, validation and attachments — a help button, a browse button, an
/// inline value field appended right after it; a [NodeElement] offers none of that because
/// it is not a [Control]. Asking for the wrong one does not compile.
///
/// Documentation is linked with `help(...)`, which appends a help icon button that stays clickable
/// while the element it belongs to is disabled. It sits on the two handles that own a row of their
/// own: [SectionRegion#help] puts it beside the section heading, [InputElement#help] beside a single
/// control. Both take either a [HelpFile] — a page of the JabRef user documentation — or a URL for
/// anything outside it.
///
/// Consecutive labelled fields share an aligned two-column [GridPane]. Validation decoration is
/// applied per control once that control reaches a scene, which is when ControlsFX can position it.
@NullMarked
public class PreferencesFormBuilder {

    private final DialogService dialogService;
    private final GuiPreferences preferences;

    private final VBox root = new VBox(GAP);
    private final Deque<Pane> containers = new ArrayDeque<>();

    private final ControlsFxVisualizer visualizer = new ControlsFxVisualizer();

    /// Every visible text handed to the builder, paired with the node it captions. The
    /// preferences search matches against these and highlights the node without reflection.
    private final List<SearchableElement> searchableElements = new ArrayList<>();

    /// Element grid spanning multiple input elements to ensure correct alignment. Its next free row
    /// is the grid's own row count, so the builder keeps no row counter of its own.
    @Nullable
    private GridPane currentGrid;

    private boolean built;

    /// The toggle group radios join inside [#radioGroup]. Deliberately *not* scoped to the
    /// container stack: [#group]/[#section]/[#columns] opened inside the lambda
    /// keep enrolling their radios in it too, which is what lets a `radioGroup` be wrapped in
    /// a `group` purely for styling without breaking the mutual exclusion. It is restored only
    /// when the owning [#radioGroup] call returns.
    @Nullable
    private ToggleGroup currentToggleGroup;

    public PreferencesFormBuilder(DialogService dialogService, GuiPreferences preferences) {
        this.dialogService = dialogService;
        this.preferences = preferences;
        this.visualizer.setDecoration(new IconValidationDecorator());
        this.containers.push(root);
    }

    // region static content

    /// A plain, unstyled caption line (for text that introduces the following controls).
    public PreferencesFormBuilder label(String text) {
        return label(text, noConfig());
    }

    public PreferencesFormBuilder label(String text, Consumer<InputElement<Label>> config) {
        Label label = new Label(text);
        searchable(text, label);
        addNode(label);
        return configured(new InputElement<>(this, label), config);
    }

    /// An indented, italic explanatory line below the control it comments on.
    public PreferencesFormBuilder info(String text) {
        return info(text, noConfig());
    }

    public PreferencesFormBuilder info(String text, Consumer<InputElement<Label>> config) {
        return label(text, info -> {
            info.styleClass("italic")
                .configure(label -> label.setPadding(new Insets(0, 0, 0, FormMetrics.INFO_LABEL_INDENT)));
            config.accept(info);
        });
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
        addNode(row(checkBox));
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
            field.node().setMaxWidth(FormMetrics.SHORT_FIELD_WIDTH);
            config.accept(field);
        }));
    }

    public PreferencesFormBuilder stringField(@Nullable String label, StringProperty value) {
        return stringField(label, value, noConfig());
    }

    public PreferencesFormBuilder stringField(@Nullable String label, StringProperty value, Consumer<InputElement<TextField>> config) {
        TextField field = new TextField();
        field.setMaxWidth(Double.MAX_VALUE);
        field.textProperty().bindBidirectional(value);
        addField(label, field);
        return configured(new InputElement<>(this, field), config);
    }

    public PreferencesFormBuilder button(String text, Runnable action) {
        return button(text, action, noConfig());
    }

    public PreferencesFormBuilder button(String text, Runnable action, Consumer<InputElement<Button>> config) {
        Button button = new Button(text);
        searchable(text, button);
        button.setOnAction(_ -> action.run());
        addNode(button);
        return configured(new InputElement<>(this, button), config);
    }

    public PreferencesFormBuilder button(String text, JabRefIcon icon, Runnable action) {
        return button(text, icon, action, noConfig());
    }

    public PreferencesFormBuilder button(String text, JabRefIcon icon, Runnable action, Consumer<InputElement<Button>> config) {
        return button(text, action, button -> {
            button.configure(node -> node.setGraphic(icon.getGraphicNode()));
            config.accept(button);
        });
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
        SearchableComboBox<X> combo = new SearchableComboBox<>(); // ControlsFX SearchableComboBox
        combo.itemsProperty().bind(items);
        return addCombo(label, combo, value, display, config);
    }

    /// Shared wiring for combo variants.
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

    /// A pre-built, pre-bound [TagsField] (see [TagsFieldEditor]).
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

    /// A mutually-exclusive radio group: radios added inside share one [ToggleGroup], while
    /// each stays bound to its own boolean property (matching the existing view models). Unlike a
    /// [#group], this adds no container. The radios stay in the surrounding layout, so there
    /// is nothing to configure. Wrap it in a [#group] to style the block.
    public PreferencesFormBuilder radioGroup(Consumer<PreferencesFormBuilder> content) {
        ToggleGroup enclosing = currentToggleGroup;
        currentToggleGroup = new ToggleGroup();
        try {
            content.accept(this);
        } finally {
            currentToggleGroup = enclosing;
        }
        return this;
    }

    public PreferencesFormBuilder radio(String text, Property<Boolean> selected) {
        return radio(text, selected, noConfig());
    }

    public PreferencesFormBuilder radio(String text, Property<Boolean> selected, Consumer<InputElement<RadioButton>> config) {
        if (currentToggleGroup == null) {
            throw new IllegalStateException("radio() outside of a radioGroup(...); without one the radios would not be mutually exclusive");
        }
        RadioButton radio = new RadioButton(text);
        searchable(text, radio);
        radio.setToggleGroup(currentToggleGroup);
        radio.selectedProperty().bindBidirectional(selected);
        addNode(row(radio));
        return configured(new InputElement<>(this, radio), config);
    }

    // endregion

    // region grid fields

    public <T extends Control> PreferencesFormBuilder field(String label, T control) {
        return field(label, control, noConfig());
    }

    public <T extends Control> PreferencesFormBuilder field(String label, T control, Consumer<InputElement<T>> config) {
        addField(label, control);
        return configured(new InputElement<>(this, control), config);
    }

    private void addField(@Nullable String label, Node control) {
        GridPane grid = ensureGrid();
        int row = grid.getRowCount();
        HBox controlRow = row(control);
        HBox.setHgrow(control, Priority.ALWAYS);
        GridPane.setHgrow(controlRow, Priority.ALWAYS);

        if (label != null) {
            searchable(label, control);
        }
        grid.add(new Label(label), 0, row);
        grid.add(controlRow, 1, row);
    }

    /// A field with the label **above**. No grid alignment allowed.
    public <T extends Control> PreferencesFormBuilder stackedField(String label, T control) {
        return stackedField(label, control, noConfig());
    }

    public <T extends Control> PreferencesFormBuilder stackedField(String label, T control, Consumer<InputElement<T>> config) {
        Label caption = new Label(label);
        caption.setMaxWidth(Double.MAX_VALUE);
        searchable(label, caption);

        control.setMaxWidth(Double.MAX_VALUE);
        HBox controlRow = row(control);
        HBox.setHgrow(control, Priority.ALWAYS);

        addNode(new VBox(GAP, caption, controlRow));
        return configured(new InputElement<>(this, control), config);
    }

    // endregion

    // region lists

    /// An editable list, taking the form's remaining height. Usually followed by a [#buttonRow]
    /// holding the actions that operate on the selection.
    public <X> PreferencesFormBuilder table(TableView<X> table) {
        return table(table, noConfig());
    }

    public <X> PreferencesFormBuilder table(TableView<X> table, Consumer<InputElement<TableView<X>>> config) {
        table.setMaxWidth(Double.MAX_VALUE);
        HBox tableRow = row(table);
        HBox.setHgrow(table, Priority.ALWAYS);
        // A table is the one element that should soak up leftover height rather than stay at its
        // preferred size, so that the form does not leave a gap under a short list.
        VBox.setVgrow(tableRow, Priority.ALWAYS);
        addNode(tableRow);
        return configured(new InputElement<>(this, table), config);
    }

    /// The row of actions under a [#table], right-aligned as the table's actions always are.
    /// The buttons come ready-made, since what they do is the tab's business; see
    /// [org.jabref.gui.util.ControlHelper#labelledIconButton].
    public PreferencesFormBuilder buttonRow(Button... buttons) {
        HBox buttonRow = new HBox(GAP, buttons);
        buttonRow.setAlignment(Pos.BASELINE_RIGHT);
        addNode(buttonRow);
        return this;
    }

    // endregion

    // region escape hatches

    /// Adds a fully custom node spanning the form. Ensure that [#field(String,Control)] with
    /// [attachments][InputElement#attach] is not the better fit for new elements (keeps grid alignment).
    public <T extends Node> PreferencesFormBuilder custom(T node) {
        return custom(node, noConfig());
    }

    public <T extends Node> PreferencesFormBuilder custom(T node, Consumer<NodeElement<T>> config) {
        addNode(node);
        // The escape hatch is the one place where the builder did not place the text itself, so it
        // reads it back off the node instead. Without this, every custom node would be a hole in
        // the preferences search. A node filled only after this call registers nothing and has to
        // use [ElementBase#searchable].
        collectSearchable(node);
        return configured(new NodeElement<>(this, node), config);
    }

    // endregion

    // region regions

    /// A titled section. Its contents go in the lambda, so the grouping is visible in the source.
    public PreferencesFormBuilder section(String title, Consumer<PreferencesFormBuilder> content) {
        return section(title, content, noConfig());
    }

    /// The configuration lambda addresses the section as a whole, including its header (e.g. for help buttons).
    public PreferencesFormBuilder section(String title,
                                          Consumer<PreferencesFormBuilder> content,
                                          Consumer<SectionRegion> config) {
        Label header = new Label(title);
        searchable(title, header);
        header.getStyleClass().addAll(StyleClasses.SECTION_HEADER);
        header.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(header, Priority.ALWAYS);
        HBox headerRow = row(header);
        // Unlike an element row, a heading and the help button beside it align on their baseline.
        headerRow.setAlignment(Pos.BASELINE_CENTER);
        addNode(headerRow);

        return configured(new SectionRegion(this, region(new VBox(GAP), content), header), config);
    }

    public PreferencesFormBuilder group(Consumer<PreferencesFormBuilder> content) {
        return group(content, noConfig());
    }

    public PreferencesFormBuilder group(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<VBox>> config) {
        return configured(new FormRegion<>(region(new VBox(GAP), content)), config);
    }

    /// A side-by-side region: every element inside becomes an equally growing column. Usually filled
    /// with [#group] blocks, one per column.
    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content) {
        return columns(content, noConfig());
    }

    public PreferencesFormBuilder columns(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<HBox>> config) {
        return configured(new FormRegion<>(region(new HBox(GAP), content)), config);
    }

    /// A wrapping region: elements flow left to right and wrap onto the next line as the dialog
    /// narrows.
    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content) {
        return flow(content, noConfig());
    }

    public PreferencesFormBuilder flow(Consumer<PreferencesFormBuilder> content, Consumer<FormRegion<FlowPane>> config) {
        return configured(new FormRegion<>(region(new FlowPane(), content)), config);
    }

    /// Everything added inside becomes a sub-region of the form. Nesting is the lambda's, so a
    /// region cannot be left unclosed; the caller wraps the returned pane in the handle its
    /// configuration lambda expects — `group(content, g -> g.disableWhen(off))` disables all of
    /// its contents.
    private <T extends Pane> T region(T region, Consumer<PreferencesFormBuilder> content) {
        flushGrid();
        addToContainer(region);
        containers.push(region);
        try {
            content.accept(this);
        } finally {
            flushGrid();
            containers.pop();
        }
        return region;
    }

    // endregion

    public VBox build() {
        if (built) {
            throw new IllegalStateException("build() was already called; a form builder assembles one tree");
        }
        built = true;
        flushGrid();
        return root;
    }

    /// The visible texts of this form with the nodes they caption; see [SearchableElement].
    public List<SearchableElement> getSearchableElements() {
        return List.copyOf(searchableElements);
    }

    // region internals

    private void searchable(String text, Node node) {
        searchableElements.add(new SearchableElement(text, node));
    }

    /// Registers the text of every [Labeled] inside `current`, each highlighting the labeled itself.
    /// The descent stops at a [Control], whose own text is its caption and whose insides belong to
    /// its skin.
    private void collectSearchable(Node current) {
        if (current instanceof Labeled labeled && StringUtil.isNotBlank(labeled.getText())) {
            searchable(labeled.getText(), labeled);
        } else if (current instanceof Parent parent && !(current instanceof Control)) {
            parent.getChildrenUnmodifiable().forEach(this::collectSearchable);
        }
    }

    /// Applies a validation decoration once `control` actually sits in a scene — ControlsFX
    /// positions the decoration against the parent, so doing this earlier is a no-op at best.
    private void decorate(ValidationStatus status, Control control) {
        if (control.getScene() != null) {
            visualizer.initVisualization(status, control);
            return;
        }
        control.sceneProperty().addListener(new ChangeListener<>() {
            @Override
            public void changed(ObservableValue<? extends Scene> scene, Scene oldScene, @Nullable Scene newScene) {
                if (newScene != null) {
                    control.sceneProperty().removeListener(this);
                    visualizer.initVisualization(status, control);
                }
            }
        });
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

    /// Logical OR of two conditions, used to combine disable conditions: an element is disabled
    /// while *any* of the conditions installed on it holds (see [ElementBase#disableWhen]).
    ///
    /// Written out rather than using `Bindings.or`, which needs two `ObservableBooleanValue`s;
    /// these are `ObservableValue<? extends Boolean>`, the type view-model properties and
    /// `disableProperty()` actually present, and whose value can be `null`.
    private static ObservableValue<Boolean> either(ObservableValue<? extends Boolean> first,
                                                   ObservableValue<? extends Boolean> second) {
        return Bindings.createBooleanBinding(
                () -> Boolean.TRUE.equals(first.getValue()) || Boolean.TRUE.equals(second.getValue()),
                first, second);
    }

    /// The two-column grid consecutive labeled fields share, created on first use. Cleared by
    /// [#flushGrid] whenever something that is not a field interrupts the run, so the next
    /// field starts a fresh grid rather than aligning across the interruption.
    private GridPane ensureGrid() {
        if (currentGrid == null) {
            currentGrid = new GridPane();
            currentGrid.setHgap(GAP);
            currentGrid.setVgap(GAP);
            ColumnConstraints labelColumn = new ColumnConstraints();
            labelColumn.setMinWidth(FormMetrics.LABEL_COLUMN_MIN_WIDTH);
            labelColumn.setHalignment(HPos.LEFT);
            ColumnConstraints controlColumn = new ColumnConstraints();
            controlColumn.setHgrow(Priority.ALWAYS);
            currentGrid.getColumnConstraints().addAll(labelColumn, controlColumn);
            addToContainer(currentGrid);
        }
        return currentGrid;
    }

    private void flushGrid() {
        currentGrid = null;
    }

    /// The row an element lives in, and the guarantee [#attachTo] reads back out: an element
    /// placed through one of these can always take attachments, because there is an [HBox]
    /// to append them to. Placements that skip this (see [#attachTo]) cannot.
    private static HBox row(Node content) {
        HBox row = new HBox(GAP, content);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    /// The open container: `root` until a [#region] pushes one, and never empty, since the
    /// stack is seeded in the constructor and every push is popped in a `finally`.
    private Pane container() {
        return containers.element();
    }

    private void addNode(Node node) {
        flushGrid();
        addToContainer(node);
    }

    /// Appends to the open container. Inside a [#columns] region every child becomes a column
    /// of equal width, so callers never repeat the hgrow boilerplate: with no preferred width of
    /// its own, a column claims nothing up front and the row's whole width is shared out evenly —
    /// column widths therefore do not depend on how long the captions inside them happen to be.
    private void addToContainer(Node node) {
        if (built) {
            throw new IllegalStateException("build() was already called; the form is finished and cannot take more elements");
        }
        Pane container = container();
        container.getChildren().add(node);
        if (container instanceof HBox) {
            HBox.setHgrow(node, Priority.ALWAYS);
            if (node instanceof Region region) {
                region.setPrefWidth(0);
                region.setMaxWidth(Double.MAX_VALUE);
            }
        }
    }

    /// Places `attachment` directly after `primary` in its row: the same append
    /// [#addToContainer] does for top-level nodes, only targeting the primary's row rather
    /// than the open container.
    ///
    /// This works because the placements that can take attachments wrap eagerly in an [HBox]
    /// row of their own — [#field]/[#stringField]/the combos/[#tagsField] (via
    /// [#addField]), [#checkbox], [#radio], [#stackedField] and a
    /// [#section] header. [#label], [#info], [#button] and
    /// [#hyperlink] do *not*: they go straight into the surrounding container, so attaching
    /// to one is unsupported and reports itself here rather than failing as a cast.
    private void attachTo(Node primary, Node attachment) {
        if (primary.getParent() instanceof HBox row) {
            row.getChildren().add(attachment);
            // An attachment the caller brought is as opaque to the builder as a custom node, so its
            // text is read back the same way. The builder's own attachments carry none.
            collectSearchable(attachment);
            return;
        }
        throw new IllegalStateException(primary.getParent() == null
                                        ? "the control has not been placed yet; attach from within its config lambda"
                                        : "cannot attach to a control sitting in a " + primary.getParent().getClass().getSimpleName()
                                                + "; only elements the builder wraps in a row of their own take attachments");
    }

    private Button helpButton(StandardActions action, HelpFile helpFile) {
        Button button = new Button();
        button.setPrefWidth(FormMetrics.ICON_BUTTON_SIZE);
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
    /// thing cannot compile. As with [ElementBase], `S` is the concrete handle type, so a
    /// base method still returns the subclass and the order of a chain does not matter.
    public abstract static sealed class RegionBase<S extends RegionBase<S, T>, T extends Pane>
            permits FormRegion, SectionRegion {

        final T region;

        /// Every disable condition installed on this region so far, OR-combined: the region is
        /// disabled while *any* of them holds; see [ElementBase#combinedDisable] for the same
        /// rule on element handles.
        @Nullable
        private ObservableValue<? extends Boolean> combinedDisable;

        RegionBase(T region) {
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

        /// Disables the whole region while `condition` holds, combined with whatever already
        /// disables it; disable propagates to every descendant, so its contents need no binding
        /// of their own.
        public S disableWhen(ObservableValue<? extends Boolean> condition) {
            combinedDisable = combinedDisable == null ? condition : either(combinedDisable, condition);
            region.disableProperty().unbind();
            region.disableProperty().bind(combinedDisable);
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

        /// Overrides the gap between the region's elements (default {@value FormMetrics#GAP}).
        /// The three cases are the three panes a region is ever made of; see [#region].
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
                default ->
                        throw new IllegalStateException(
                                "spacing() does not apply to a " + region.getClass().getSimpleName() + " region");
            }
            return self();
        }
    }

    /// A plain region: a [#group], [#columns] or [#flow] block, which has contents
    /// but no heading of its own.
    public static final class FormRegion<T extends Pane> extends RegionBase<FormRegion<T>, T> {

        FormRegion(T region) {
            super(region);
        }
    }

    /// The region of a [#section]: unlike a plain region it has a header, which is therefore
    /// the only kind of region that can take a help button.
    public static final class SectionRegion extends RegionBase<SectionRegion, VBox> {

        /// The only region handle that needs the builder back: its help buttons are attached to
        /// the header, which the builder alone knows how to place.
        private final PreferencesFormBuilder form;
        private final Label header;

        SectionRegion(PreferencesFormBuilder form, VBox region, Label header) {
            super(region);
            this.form = form;
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

        /// Every disable condition installed on this element so far, OR-combined — the element is
        /// disabled while *any* of them holds — whether it
        /// came from the builder itself (the value field of an
        /// [attachField][InputElement#attachField], following its toggle) or from a caller's
        /// [#disableWhen]. There is no distinction between "the builder's" and "the caller's"
        /// binding: each call just adds another condition to the combination, so nothing is ever
        /// silently replaced.
        @Nullable
        private ObservableValue<? extends Boolean> combinedDisable;

        ElementBase(PreferencesFormBuilder form, N node) {
            this.form = form;
            this.node = node;
        }

        /// Adds `condition` to the combination of things that disable this element; see
        /// [#combinedDisable].
        final void addDisableCondition(ObservableValue<? extends Boolean> condition) {
            combinedDisable = combinedDisable == null ? condition : either(combinedDisable, condition);
            node.disableProperty().unbind();
            node.disableProperty().bind(combinedDisable);
        }

        /// Couples this element's disable state to `primary`'s. Every attachment that should track
        /// its primary's disabled state goes through this.
        final void followDisable(Node primary) {
            addDisableCondition(primary.disableProperty());
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

        /// Disables the node while `condition` holds, combined with whatever already disables it —
        /// e.g. an [attached field][InputElement#attachField] following its toggle.
        public S disableWhen(ObservableValue<? extends Boolean> condition) {
            addDisableCondition(condition);
            return self();
        }

        public S visibleWhen(ObservableValue<? extends Boolean> condition) {
            node.visibleProperty().bind(condition);
            node.managedProperty().bind(condition);
            return self();
        }

        /// Statically disables the node (for platform-capability checks, not reactive state). Use
        /// [#disableWhen] for anything that can change while the dialog is open.
        public S disable() {
            node.setDisable(true);
            return self();
        }

        public S styleClass(String... styleClasses) {
            node.getStyleClass().addAll(styleClasses);
            return self();
        }

        /// Makes the preferences search find this element under `texts`, highlighting the node.
        /// Only needed where the text is in no [Labeled] the builder can see: a synonym, a text
        /// painted by a control itself, or a [#custom] node filled after it was added — everything
        /// the builder placed, and every labeled inside a custom node, is registered already.
        public S searchable(String... texts) {
            for (String text : texts) {
                form.searchable(text, node);
            }
            return self();
        }
    }

    /// A node that is not a [Control]: a hand-assembled row, a table, a custom region.
    public static final class NodeElement<N extends Node> extends ElementBase<NodeElement<N>, N> {

        NodeElement(PreferencesFormBuilder form, N node) {
            super(form, node);
        }

        /// Decorates a control the builder did not create — one the [#custom] node brought
        /// with it — so that a tab needs no [ControlsFxVisualizer] of its own. The control is
        /// named explicitly because this handle addresses the custom node, not its insides.
        public NodeElement<N> validate(ValidationStatus status, Control control) {
            form.decorate(status, control);
            return this;
        }
    }

    /// A [Control] the builder placed. Being a control, it can carry a tooltip and
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

        /// Decorates the control with `status`, applied once the control reaches a scene.
        public InputElement<N> validate(ValidationStatus status) {
            form.decorate(status, node);
            return this;
        }

        /// Lets the control take all remaining horizontal space in its row. Use where a builder
        /// default is too narrow, e.g. the value field of [#checkWithField] when it holds a
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
            NodeElement<A> element = new NodeElement<>(form, attachment);
            element.followDisable(node);
            form.attachTo(node, attachment);
            config.accept(element);
            return this;
        }

        /// Attaches a text field bound to `value`. Like every attachment it follows the control's
        /// disabled state; on a checkbox or radio it is additionally disabled while the toggle is
        /// unselected (the recurring "option with inline value" pattern). The config lambda
        /// addresses the new field.
        public InputElement<N> attachField(StringProperty value) {
            return attachField(value, noConfig());
        }

        public InputElement<N> attachField(StringProperty value, Consumer<InputElement<TextField>> config) {
            TextField field = new TextField();
            field.setMaxWidth(Double.MAX_VALUE);
            field.textProperty().bindBidirectional(value);
            HBox.setHgrow(field, Priority.ALWAYS);
            InputElement<TextField> element = new InputElement<>(form, field);
            element.followDisable(node);
            if (node instanceof CheckBox box) {
                element.addDisableCondition(box.selectedProperty().not());
            } else if (node instanceof ToggleButton toggle) {
                element.addDisableCondition(toggle.selectedProperty().not());
            }
            form.attachTo(node, field);
            config.accept(element);
            return this;
        }

        /// Attaches a "browse" icon button that follows this control's disabled state.
        public InputElement<N> browse(Runnable onBrowse) {
            Button browseButton = new Button();
            browseButton.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
            browseButton.getStyleClass().addAll(StyleClasses.NARROW_ICON_BUTTON);
            browseButton.setPrefSize(FormMetrics.ICON_BUTTON_SIZE, FormMetrics.ICON_BUTTON_SIZE);
            browseButton.setTooltip(new Tooltip(Localization.lang("Browse")));
            browseButton.setOnAction(_ -> onBrowse.run());
            return attach(browseButton);
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
