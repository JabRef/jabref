package org.jabref.gui.actions;

import javafx.beans.property.ReadOnlyDoubleProperty;

import org.jabref.gui.BasePanel;
import org.jabref.gui.util.BindingsHelper;

import de.saxsys.mvvmfx.utils.commands.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This wraps the old Swing commands so that they fit into the new infrastructure.
 * In the long term, this class should be removed.
 */
@Deprecated
public class OldCommandWrapper extends CommandBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OldCommandWrapper.class);

    private final Actions command;
    private final BasePanel panel;

    public OldCommandWrapper(Actions command, BasePanel panel) {
        this.command = command;
        this.panel = panel;
    }

    @Override
    public void execute() {
        try {
            panel.runCommand(command);
        } catch (Throwable ex) {
            LOGGER.debug("Cannot execute command " + command + ".", ex);
        }
    }

    @Override
    public double getProgress() {
        return 0;
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return null;
    }

    public void setExecutable(boolean executable) {
        this.executable.bind(BindingsHelper.constantOf(executable));
    }

    @Override
    public String toString() {
        return this.command.toString();
    }
}
