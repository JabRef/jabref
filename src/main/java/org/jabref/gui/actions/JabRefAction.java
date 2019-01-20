package org.jabref.gui.actions;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBindingRepository;

import de.saxsys.mvvmfx.utils.commands.Command;

/**
 * Wrapper around one of our actions from {@link Action} to convert them to controlsfx {@link org.controlsfx.control.action.Action}.
 */
class JabRefAction extends org.controlsfx.control.action.Action {


    public JabRefAction(Action action, KeyBindingRepository keyBindingRepository) {
        super(action.getText());
        action.getIcon()
              .ifPresent(icon -> setGraphic(icon.getGraphicNode()));
        action.getKeyBinding()
              .ifPresent(keyBinding -> setAccelerator(keyBindingRepository.getKeyCombination(keyBinding)));

        setLongText(action.getDescription());

    }

    public JabRefAction(Action action, Command command, KeyBindingRepository keyBindingRepository) {
        this(action, keyBindingRepository);

        setEventHandler(event -> {
            command.execute();
            trackExecute();
        });

        disabledProperty().bind(command.executableProperty().not());
    }

    private void trackExecute() {
        Globals.getTelemetryClient()
               .ifPresent(telemetryClient -> telemetryClient.trackEvent(getText()));
    }
}
