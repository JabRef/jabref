package org.jabref.gui.actions;

import java.util.Optional;

import javafx.beans.property.ReadOnlyDoubleProperty;

import org.jabref.gui.JabRefFrame;
import org.jabref.gui.StateManager;

import de.saxsys.mvvmfx.utils.commands.CommandBase;
import org.fxmisc.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command that is only executable if a database is open.
 * Deprecated use instead
 * @see org.jabref.gui.actions.SimpleCommand
 */
@Deprecated
public class OldDatabaseCommandWrapper extends CommandBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OldDatabaseCommandWrapper.class);

    private final Actions command;
    private final JabRefFrame frame;

    public OldDatabaseCommandWrapper(Actions command, JabRefFrame frame, StateManager stateManager) {
        this.command = command;
        this.frame = frame;

        this.executable.bind(
                EasyBind.map(stateManager.activeDatabaseProperty(), Optional::isPresent));
    }

    @Override
    public void execute() {
        if (frame.getTabbedPane().getTabs().size() > 0) {
            try {
                frame.getCurrentBasePanel().runCommand(command);
            } catch (Throwable ex) {
                LOGGER.error("Problem with executing command: " + command, ex);
            }
        } else {
            LOGGER.info("Action '" + command + "' must be disabled when no database is open.");
        }
    }

    @Override
    public double getProgress() {
        return 0;
    }

    @Override
    public String toString() {
        return this.command.toString();
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return null;
    }
}
