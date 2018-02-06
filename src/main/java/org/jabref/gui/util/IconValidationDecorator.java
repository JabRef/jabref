package org.jabref.gui.util;

import java.util.Arrays;
import java.util.Collection;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;

import org.jabref.gui.IconTheme;

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

    public IconValidationDecorator() {
        this(Pos.BOTTOM_LEFT);
    }

    public IconValidationDecorator(Pos position) {
        this.position = position;
    }

    protected Node createErrorNode() {
        return IconTheme.JabRefIcon.ERROR.getGraphicNode();
    }

    protected Node createWarningNode() {
        return IconTheme.JabRefIcon.WARNING.getGraphicNode();
    }

    public Node createDecorationNode(ValidationMessage message) {
        Node graphic = Severity.ERROR == message.getSeverity() ? createErrorNode() : createWarningNode();
        graphic.getStyleClass().add(Severity.ERROR == message.getSeverity() ? "error-icon" : "warning-icon");
        Label label = new Label();
        label.setGraphic(graphic);
        label.setTooltip(createTooltip(message));
        label.setAlignment(Pos.CENTER);
        return label;
    }

    protected Tooltip createTooltip(ValidationMessage message) {
        Tooltip tooltip = new Tooltip(message.getText());
        tooltip.getStyleClass().add(Severity.ERROR == message.getSeverity() ? "tooltip-error" : "tooltip-warning");
        return tooltip;
    }

    @Override
    protected Collection<Decoration> createValidationDecorations(ValidationMessage message) {
        return Arrays.asList(new GraphicDecoration(createDecorationNode(message), position));
    }
}
