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

public class InfoButton extends Button {
    private final StringProperty infoMessage = new SimpleStringProperty();
    private final ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());

    public InfoButton(String infoMessage) {
        setInfoMessage(infoMessage);
        configureButton();
        EasyBind.subscribe(infoMessageProperty(), newWarningMessage -> {
            configureButton();
        });
    }

    private void configureButton() {
        setMaxHeight(Double.MAX_VALUE);
        setFocusTraversable(false);
        Action mergeAction = new Action.Builder(getInfoMessage()).setIcon(IconTheme.JabRefIcons.INTEGRITY_INFO);

        actionFactory.configureIconButton(mergeAction, new SimpleCommand() {
            @Override
            public void execute() {
                // The info button is not meant to be clickable that's why this is empty
            }
        }, this);
    }

    private void setInfoMessage(String infoMessage) {
        infoMessageProperty().set(infoMessage);
    }

    public StringProperty infoMessageProperty() {
        return infoMessage;
    }

    public String getInfoMessage() {
        return infoMessage.get();
    }
}
