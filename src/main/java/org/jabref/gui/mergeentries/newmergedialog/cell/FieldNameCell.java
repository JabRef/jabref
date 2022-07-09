package org.jabref.gui.mergeentries.newmergedialog.cell;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

import de.saxsys.mvvmfx.utils.commands.Command;

/**
 * A non-editable cell that contains the name of some field
 */
public class FieldNameCell extends AbstractCell {
    public static final String DEFAULT_STYLE_CLASS = "field-name";
    protected final HBox actionLayout = new HBox();
    private final Label label = new Label();

    private final HBox labelBox = new HBox(label);
    private final ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

    public FieldNameCell(String text, int rowIndex) {
        super(text, rowIndex);
        initialize();
    }

    private void initialize() {
        getStyleClass().add(DEFAULT_STYLE_CLASS);
        initializeLabel();
        getChildren().addAll(labelBox, actionLayout);
    }

    private void initializeLabel() {
        label.textProperty().bind(textProperty());
        HBox.setHgrow(labelBox, Priority.ALWAYS);
    }

    protected void setAction(String actionName, IconTheme.JabRefIcons icon, Command command) {
        actionLayout.getChildren().clear();
        Node iconNode = icon.getGraphicNode();
        Button actionButton = factory.createIconButton(() -> Localization.lang(actionName), command);
        actionButton.setGraphic(iconNode);
        actionButton.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        actionButton.setMaxHeight(Double.MAX_VALUE);

        actionLayout.getChildren().add(actionButton);
    }
}
