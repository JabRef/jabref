package org.jabref.gui.actions;

import javafx.beans.binding.Bindings;

import org.jabref.Globals;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.model.strings.StringUtil;

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
        this(action, command, keyBindingRepository, null);
    }

    /**
     * especially for the track execute when the action run the same function but from different source.
     * @param source is a string contains the source, for example "button"
     */
    public JabRefAction(Action action, Command command, KeyBindingRepository keyBindingRepository, String source) {
        this(action, keyBindingRepository);

        setEventHandler(event -> {
            command.execute();
            if (StringUtil.isNullOrEmpty(source)) {
                trackExecute(getActionName(action, command));
            } else {
                trackExecute(getActionName(action, command) + "From" + source);
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
            String ans = command.getClass().getSimpleName();
            if (ans.contains("OldDatabaseCommandWrapper")) {
                OldDatabaseCommandWrapper tmp = (OldDatabaseCommandWrapper) command;
                return tmp.getActions().toString();
            } else if (ans.contains("EditAction")) {
                return command.toString();
            } else {
                return command.getClass().getSimpleName();
            }
        }
    }

    private void trackExecute(String actionName) {
        Globals.getTelemetryClient()
               .ifPresent(telemetryClient -> telemetryClient.trackEvent(actionName));
    }
}
