package org.jabref.gui.mergeentries.newmergedialog.cell.sidebuttons;

import java.util.Optional;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

import org.jabref.gui.Globals;
import org.jabref.gui.actions.Action;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;

import com.tobiasdiez.easybind.EasyBind;

public class InfoButton extends Button {
    private final StringProperty infoMessage = new SimpleStringProperty();
    private final ActionFactory actionFactory = new ActionFactory(Globals.getKeyPrefs());

    private final Action mergeAction = new Action() {
        @Override
        public Optional<JabRefIcon> getIcon() {
            return Optional.of(IconTheme.JabRefIcons.INTEGRITY_INFO);
        }

        @Override
        public String getText() {
            return infoMessage.get();
        }
    };

    public InfoButton(String infoMessage) {
        this.infoMessage.setValue(infoMessage);
        configureButton();
        EasyBind.subscribe(this.infoMessage, newWarningMessage -> configureButton());
    }

    private void configureButton() {
        setMaxHeight(Double.MAX_VALUE);
        setFocusTraversable(false);

        actionFactory.configureIconButton(mergeAction, new SimpleCommand() {
            @Override
            public void execute() {
                // The info button is not meant to be clickable that's why this is empty
            }
        }, this);
    }
}
