package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.DragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.util.Callback;

import org.jabref.gui.icon.JabRefIcon;
import org.jabref.logic.util.strings.StringUtil;

import com.tobiasdiez.easybind.Subscription;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/// Constructs a [ListCell] based on the view model of the row and a bunch of specified converter methods.
///
/// @param <T> cell value
@NullMarked
public class ViewModelListCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {

    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    private @Nullable Callback<T, String> toText;
    private @Nullable Callback<T, @Nullable Node> toGraphic;
    private Callback<ListCell<T>, Callback<T, @Nullable Tooltip>> tooltipFactory = _ -> _ -> null;
    private @Nullable BiConsumer<T, ? super MouseEvent> toOnMouseClickedEvent;
    private @Nullable Supplier<List<String>> styleClassSupplier;
    private @Nullable Callback<T, @Nullable ContextMenu> toContextMenu;
    private @Nullable BiConsumer<T, ? super MouseEvent> toOnDragDetected;
    private @Nullable BiConsumer<T, ? super DragEvent> toOnDragDropped;
    private @Nullable BiConsumer<T, ? super DragEvent> toOnDragEntered;
    private @Nullable BiConsumer<T, ? super DragEvent> toOnDragExited;
    private @Nullable BiConsumer<T, ? super DragEvent> toOnDragOver;
    private @Nullable BiConsumer<T, ? super DragEvent> toOnDragDone;
    private final Map<PseudoClass, Callback<T, ObservableValue<Boolean>>> pseudoClasses = new HashMap<>();
    private @Nullable Callback<T, ValidationStatus> validationStatusProperty;

    public ViewModelListCellFactory<T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ViewModelListCellFactory<T> withGraphic(Callback<T, @Nullable Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ViewModelListCellFactory<T> withIcon(Callback<T, @Nullable JabRefIcon> toIcon) {
        this.toGraphic = viewModel -> {
            JabRefIcon icon = toIcon.call(viewModel);
            if (icon != null) {
                return icon.getGraphicNode();
            }
            return null;
        };
        return this;
    }

    public ViewModelListCellFactory<T> withIcon(Callback<T, JabRefIcon> toIcon, Callback<T, Color> toColor) {
        this.toGraphic = viewModel -> toIcon.call(viewModel).withColor(toColor.call(viewModel)).getGraphicNode();
        return this;
    }

    public ViewModelListCellFactory<T> withStringTooltip(Callback<T, @Nullable String> toStringTooltip) {
        return withTooltip(viewModel -> {
            String tooltipText = toStringTooltip.call(viewModel);
            if (StringUtil.isNotBlank(tooltipText)) {
                return new Tooltip(tooltipText);
            }
            return null;
        });
    }

    public ViewModelListCellFactory<T> withTooltip(Callback<T, @Nullable Tooltip> toTooltip) {
        this.tooltipFactory = _ -> toTooltip;
        return this;
    }

    /// Creates one tooltip per cell and binds its text to the current item's observable value.
    ///
    /// The callback is called when the cell receives an item. It must return a property or a lazy
    /// observable that releases its listeners when the cell changes items or becomes empty.
    ///
    /// @param toTooltipText provides observable tooltip text for an item
    /// @return this factory
    public ViewModelListCellFactory<T> withTooltipText(Callback<T, ObservableValue<String>> toTooltipText) {
        this.tooltipFactory = cell -> {
            Tooltip tooltip = new Tooltip();
            tooltip.textProperty().bind(cell.itemProperty().flatMap(toTooltipText::call).when(cell.emptyProperty().not()));
            return _ -> tooltip;
        };
        return this;
    }

    public ViewModelListCellFactory<T> withContextMenu(Callback<T, @Nullable ContextMenu> toContextMenu) {
        this.toContextMenu = toContextMenu;
        return this;
    }

    /// Supplies styleClasses that are set on the cell right after creation, only once.
    ///
    /// @param styleClassSupplier the supplier that is called only once on the cell
    /// @return the factory
    public ViewModelListCellFactory<T> withStyleClasses(Supplier<List<String>> styleClassSupplier) {
        this.styleClassSupplier = styleClassSupplier;
        return this;
    }

    public ViewModelListCellFactory<T> withOnMouseClickedEvent(BiConsumer<T, ? super MouseEvent> toOnMouseClickedEvent) {
        this.toOnMouseClickedEvent = toOnMouseClickedEvent;
        return this;
    }

    public ViewModelListCellFactory<T> setOnDragDetected(BiConsumer<T, ? super MouseEvent> toOnDragDetected) {
        this.toOnDragDetected = toOnDragDetected;
        return this;
    }

    public ViewModelListCellFactory<T> setOnDragDropped(BiConsumer<T, ? super DragEvent> toOnDragDropped) {
        this.toOnDragDropped = toOnDragDropped;
        return this;
    }

    public ViewModelListCellFactory<T> setOnDragEntered(BiConsumer<T, ? super DragEvent> toOnDragEntered) {
        this.toOnDragEntered = toOnDragEntered;
        return this;
    }

    public ViewModelListCellFactory<T> setOnDragExited(BiConsumer<T, ? super DragEvent> toOnDragExited) {
        this.toOnDragExited = toOnDragExited;
        return this;
    }

    public ViewModelListCellFactory<T> setOnDragOver(BiConsumer<T, ? super DragEvent> toOnDragOver) {
        this.toOnDragOver = toOnDragOver;
        return this;
    }

    public ViewModelListCellFactory<T> setOnDragDone(BiConsumer<T, DragEvent> toOnDragDone) {
        this.toOnDragDone = toOnDragDone;
        return this;
    }

    public ViewModelListCellFactory<T> withPseudoClass(PseudoClass pseudoClass, Callback<T, ObservableValue<Boolean>> toCondition) {
        this.pseudoClasses.putIfAbsent(pseudoClass, toCondition);
        return this;
    }

    public ViewModelListCellFactory<T> withValidation(Callback<T, ValidationStatus> validationStatusProperty) {
        this.validationStatusProperty = validationStatusProperty;
        return this;
    }

    public void install(ComboBox<T> comboBox) {
        comboBox.setButtonCell(this.call(null));
        comboBox.setCellFactory(this);
    }

    public void install(ListView<T> listView) {
        listView.setCellFactory(this);
    }

    @Override
    public ListCell<T> call(@Nullable ListView<T> param) {
        ListCell<T> listCell = new ListCell<>() {
            final List<Subscription> subscriptions = new ArrayList<>();
            private final Callback<T, @Nullable Tooltip> toTooltip = tooltipFactory.call(this);

            @Override
            protected void updateItem(@Nullable T item, boolean empty) {
                super.updateItem(item, empty);

                // Remove previous subscriptions
                subscriptions.forEach(Subscription::unsubscribe);
                subscriptions.clear();

                T viewModel = getItem();
                if (empty || (viewModel == null)) {
                    setText(null);
                    setGraphic(null);
                    setOnMouseClicked(null);
                    setTooltip(null);
                    pseudoClassStateChanged(INVALID_PSEUDO_CLASS, false);
                } else {
                    if (toText != null) {
                        setText(toText.call(viewModel));
                    }
                    if (toGraphic != null) {
                        setGraphic(toGraphic.call(viewModel));
                    }
                    BiConsumer<T, ? super MouseEvent> onMouseClickedEvent = toOnMouseClickedEvent;
                    if (onMouseClickedEvent != null) {
                        setOnMouseClicked(event -> onMouseClickedEvent.accept(viewModel, event));
                    }
                    setTooltip(toTooltip.call(viewModel));
                    if (toContextMenu != null) {
                        setContextMenu(toContextMenu.call(viewModel));
                    }
                    BiConsumer<T, ? super MouseEvent> onDragDetected = toOnDragDetected;
                    if (onDragDetected != null) {
                        setOnDragDetected(event -> onDragDetected.accept(viewModel, event));
                    }
                    BiConsumer<T, ? super DragEvent> onDragDropped = toOnDragDropped;
                    if (onDragDropped != null) {
                        setOnDragDropped(event -> {
                            // For reordering we need to accept the event so that the local handlers get it
                            onDragDropped.accept(viewModel, event);
                            Node parent = getParent();
                            if (parent != null) {
                                // The parent is the box for dropping from external files; that should be used as target (and not this cell)
                                // We need to send a copy of the event before consuming the original one
                                DragEvent forwarded = event.copyFor(parent, parent);
                                Event.fireEvent(parent, forwarded);
                            }
                            event.consume();
                        });
                    }
                    BiConsumer<T, ? super DragEvent> onDragEntered = toOnDragEntered;
                    if (onDragEntered != null) {
                        setOnDragEntered(event -> onDragEntered.accept(viewModel, event));
                    }
                    BiConsumer<T, ? super DragEvent> onDragExited = toOnDragExited;
                    if (onDragExited != null) {
                        setOnDragExited(event -> onDragExited.accept(viewModel, event));
                    }
                    BiConsumer<T, ? super DragEvent> onDragOver = toOnDragOver;
                    if (onDragOver != null) {
                        setOnDragOver(event -> {
                            // For reordering we need to accept the event so that the local handlers get it
                            onDragOver.accept(viewModel, event);
                            Node parent = getParent();
                            if (parent != null) {
                                DragEvent forwarded = event.copyFor(parent, parent);
                                Event.fireEvent(parent, forwarded);
                            }
                            event.consume();
                        });
                    }
                    BiConsumer<T, ? super DragEvent> onDragDone = toOnDragDone;
                    if (onDragDone != null) {
                        setOnDragDone(event -> onDragDone.accept(viewModel, event));
                    }
                    for (Map.Entry<PseudoClass, Callback<T, ObservableValue<Boolean>>> pseudoClassWithCondition : pseudoClasses.entrySet()) {
                        ObservableValue<Boolean> condition = pseudoClassWithCondition.getValue().call(viewModel);
                        subscriptions.add(BindingsHelper.includePseudoClassWhen(
                                this,
                                pseudoClassWithCondition.getKey(),
                                condition));
                    }
                    if (validationStatusProperty != null) {
                        validationStatusProperty.call(viewModel)
                                                .getHighestMessage()
                                                .ifPresent(message -> setTooltip(new Tooltip(message.getMessage())));

                        subscriptions.add(BindingsHelper.includePseudoClassWhen(
                                this,
                                INVALID_PSEUDO_CLASS,
                                validationStatusProperty.call(viewModel).validProperty().not()));
                    }
                }
            }
        };
        if (styleClassSupplier != null) {
            listCell.getStyleClass().setAll(styleClassSupplier.get());
        }
        return listCell;
    }
}
