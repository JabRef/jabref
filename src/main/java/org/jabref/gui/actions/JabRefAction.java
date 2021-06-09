package org.jabref.gui.actions;

import java.util.Map;

import javafx.beans.binding.Bindings;

import org.jabref.gui.Globals;
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
              .ifPresent(keyBinding -> keyBindingRepository.getKeyCombination(keyBinding).ifPresent(combination -> setAccelerator(combination)));

        setLongText(action.getDescription());
    }

    public JabRefAction(Action action, Command command, KeyBindingRepository keyBindingRepository) {
        this(action, command, keyBindingRepository, null);
    }

    /**
     * especially for the track execute when the action run the same function but from different source.
     *
     * @param source is a string contains the source, for example "button"
     */
    public JabRefAction(Action action, Command command, KeyBindingRepository keyBindingRepository, Sources source) {
        this(action, keyBindingRepository);

        setEventHandler(event -> {
            command.execute();
            if (source == null) {
                trackExecute(getActionName(action, command));
            } else {
                trackUserActionSource(getActionName(action, command), source);
            }
        });

        disabledProperty().bind(command.executableProperty().not());

        if (command instanceof SimpleCommand) {
            SimpleCommand ourCommand = (SimpleCommand) command;
            longTextProperty().bind(Bindings.concat(action.getDescription(), ourCommand.statusMessageProperty()));
        }
    }

    private String getActionName(Action action, Command command) {
        if (command.getClass().isAnonymousClass()) {
            return action.getText();
        } else {
            String commandName = command.getClass().getSimpleName();
            if (commandName.contains("EditAction")
                    || commandName.contains("CopyMoreAction")
                    || commandName.contains("CopyCitationAction")
                    || commandName.contains("PreviewSwitchAction")
                    || commandName.contains("SaveAction")) {
                return command.toString();
            } else {
                return commandName;
            }
        }
    }

    private void trackExecute(String actionName) {
        Globals.getTelemetryClient()
               .ifPresent(telemetryClient -> telemetryClient.trackEvent(actionName));
    }

    private void trackUserActionSource(String actionName, Sources source) {
        Globals.getTelemetryClient().ifPresent(telemetryClient -> telemetryClient.trackEvent(
                actionName,
                Map.of("Source", source.toString()),
                Map.of()));
    }
}
