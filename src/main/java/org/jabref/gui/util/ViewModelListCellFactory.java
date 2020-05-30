package org.jabref.gui.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
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
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.Subscription;
import de.saxsys.mvvmfx.utils.validation.ValidationStatus;

/**
 * Constructs a {@link ListCell} based on the view model of the row and a bunch of specified converter methods.
 *
 * @param <T> cell value
 */
public class ViewModelListCellFactory<T> implements Callback<ListView<T>, ListCell<T>> {

    private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

    private Callback<T, String> toText;
    private Callback<T, Node> toGraphic;
    private Callback<T, Tooltip> toTooltip;
    private BiConsumer<T, ? super MouseEvent> toOnMouseClickedEvent;
    private Callback<T, String> toStyleClass;
    private Callback<T, ContextMenu> toContextMenu;
    private BiConsumer<T, ? super MouseEvent> toOnDragDetected;
    private BiConsumer<T, ? super DragEvent> toOnDragDropped;
    private BiConsumer<T, ? super DragEvent> toOnDragEntered;
    private BiConsumer<T, ? super DragEvent> toOnDragExited;
    private BiConsumer<T, ? super DragEvent> toOnDragOver;
    private final Map<PseudoClass, Callback<T, ObservableValue<Boolean>>> pseudoClasses = new HashMap<>();
    private Callback<T, ValidationStatus> validationStatusProperty;

    public ViewModelListCellFactory<T> withText(Callback<T, String> toText) {
        this.toText = toText;
        return this;
    }

    public ViewModelListCellFactory<T> withGraphic(Callback<T, Node> toGraphic) {
        this.toGraphic = toGraphic;
        return this;
    }

    public ViewModelListCellFactory<T> withIcon(Callback<T, JabRefIcon> toIcon) {
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

    public ViewModelListCellFactory<T> withStringTooltip(Callback<T, String> toStringTooltip) {
        this.toTooltip = viewModel -> {
            String tooltipText = toStringTooltip.call(viewModel);
            if (StringUtil.isNotBlank(tooltipText)) {
                return new Tooltip(tooltipText);
            }
            return null;
        };
        return this;
    }

    public ViewModelListCellFactory<T> withTooltip(Callback<T, Tooltip> toTooltip) {
        this.toTooltip = toTooltip;
        return this;
    }

    public ViewModelListCellFactory<T> withContextMenu(Callback<T, ContextMenu> toContextMenu) {
        this.toContextMenu = toContextMenu;
        return this;
    }

    public ViewModelListCellFactory<T> withStyleClass(Callback<T, String> toStyleClass) {
        this.toStyleClass = toStyleClass;
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
    public ListCell<T> call(ListView<T> param) {

        return new ListCell<>() {

            final List<Subscription> subscriptions = new ArrayList<>();

            @Override
            protected void updateItem(T item, boolean empty) {
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
                    if (toOnMouseClickedEvent != null) {
                        setOnMouseClicked(event -> toOnMouseClickedEvent.accept(viewModel, event));
                    }
                    if (toStyleClass != null) {
                        getStyleClass().setAll(toStyleClass.call(viewModel));
                    }
                    if (toTooltip != null) {
                        setTooltip(toTooltip.call(viewModel));
                    }
                    if (toContextMenu != null) {
                        setContextMenu(toContextMenu.call(viewModel));
                    }
                    if (toOnDragDetected != null) {
                        setOnDragDetected(event -> toOnDragDetected.accept(viewModel, event));
                    }
                    if (toOnDragDropped != null) {
                        setOnDragDropped(event -> toOnDragDropped.accept(viewModel, event));
                    }
                    if (toOnDragEntered != null) {
                        setOnDragEntered(event -> toOnDragEntered.accept(viewModel, event));
                    }
                    if (toOnDragExited != null) {
                        setOnDragExited(event -> toOnDragExited.accept(viewModel, event));
                    }
                    if (toOnDragOver != null) {
                        setOnDragOver(event -> toOnDragOver.accept(viewModel, event));
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
    }
}
