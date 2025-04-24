package org.jabref.gui.actions;

import javafx.beans.binding.Bindings;

import org.jabref.gui.keyboard.KeyBindingRepository;

import de.saxsys.mvvmfx.utils.commands.Command;

/**
 * Wrapper around one of our actions from {@link Action} to convert them to controlsfx {@link org.controlsfx.control.action.Action}.
 */
class JabRefAction extends org.controlsfx.control.action.Action {

    public JabRefAction(Action action, KeyBindingRepository keyBindingRepository) {
        super(action.getText());
        action.getIcon().ifPresent(icon -> setGraphic(icon.getGraphicNode()));
        action.getKeyBinding().flatMap(keyBindingRepository::getKeyCombination).ifPresent(this::setAccelerator);

        setLongText(action.getDescription());
    }

    public JabRefAction(Action action, Command command, KeyBindingRepository keyBindingRepository) {
        this(action, keyBindingRepository);

        setEventHandler(event -> command.execute());

        disabledProperty().bind(command.executableProperty().not());

        if (command instanceof SimpleCommand simpleCommand) {
            longTextProperty().bind(Bindings.concat(action.getDescription(), simpleCommand.statusMessageProperty()));
        }
    }
}
