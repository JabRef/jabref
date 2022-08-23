package org.jabref.gui.mergeentries.newmergedialog.cell.sidebuttons;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;

import com.tobiasdiez.easybind.EasyBind;

public class WarningButton extends Button {
    private final StringProperty warningMessage = new SimpleStringProperty();
    private final ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());

    public WarningButton(String warningMessage) {
        setWarningMessage(warningMessage);
        configureButton();
        EasyBind.subscribe(warningMessageProperty(), newWarningMessage -> {
            configureButton();
        });
    }

    private void configureButton() {
        setMaxHeight(Double.MAX_VALUE);
        setFocusTraversable(false);
        Action mergeAction = new Action.Builder(getWarningMessage()).setIcon(IconTheme.JabRefIcons.WARNING);

        actionFactory.configureIconButton(mergeAction, new SimpleCommand() {
            @Override
            public void execute() {
            }
        }, this);
    }

    private void setWarningMessage(String warningMessage) {
        warningMessageProperty().set(warningMessage);
    }

    public StringProperty warningMessageProperty() {
        return warningMessage;
    }

    public String getWarningMessage() {
        return warningMessage.get();
    }
}
