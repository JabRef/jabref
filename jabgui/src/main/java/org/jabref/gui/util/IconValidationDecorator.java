package org.jabref.gui.util;

import java.util.Collection;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.gui.icon.IconTheme;

import org.controlsfx.control.decoration.Decoration;
import org.controlsfx.control.decoration.GraphicDecoration;
import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationMessage;
import org.controlsfx.validation.decoration.GraphicValidationDecoration;

/**
 * This class is similar to {@link GraphicValidationDecoration} but with a different style and font-based icon.
 */
public class IconValidationDecorator extends GraphicValidationDecoration {

    private final Pos position;

    /**
     * Creates a new IconValidationDecorator with default position CENTER_LEFT.
     * This position is chosen to better align with text content regardless of case.
     */
    public IconValidationDecorator() {
        this(Pos.CENTER_LEFT);
    }

    public IconValidationDecorator(Pos position) {
        this.position = position;
    }

    @Override
    public Node createDecorationNode(ValidationMessage message) {
        Node graphic = Severity.ERROR == message.getSeverity()
                       ? IconTheme.JabRefIcons.ERROR.getGraphicNode()
                       : IconTheme.JabRefIcons.WARNING.getGraphicNode();
        graphic.getStyleClass().add(Severity.ERROR == message.getSeverity() ? "error-icon" : "warning-icon");
        Label label = new Label();
        label.setGraphic(graphic);
        label.setTooltip(createTooltip(message));
        label.setAlignment(position);
        // Prevent label from stretching
        label.setMaxHeight(Control.USE_PREF_SIZE);
        return label;
    }

    @Override
    protected Tooltip createTooltip(ValidationMessage message) {
        Tooltip tooltip = new Tooltip(message.getText());
        tooltip.getStyleClass().add(Severity.ERROR == message.getSeverity() ? "tooltip-error" : "tooltip-warning");
        return tooltip;
    }

    @Override
    protected Collection<Decoration> createValidationDecorations(ValidationMessage message) {
        return List.of(new GraphicDecoration(createDecorationNode(message), position));
    }
}
