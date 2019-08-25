package org.jabref.gui.actions;

import javafx.beans.property.ReadOnlyDoubleProperty;

import org.jabref.JabRefGUI;
import org.jabref.gui.util.BindingsHelper;

import de.saxsys.mvvmfx.utils.commands.CommandBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This wraps the old Swing commands so that they fit into the new infrastructure.
 * In the long term, this class should be removed.
 */
@Deprecated
public class OldCommandWrapperForActiveDatabase extends CommandBase {

    private static final Logger LOGGER = LoggerFactory.getLogger(OldCommandWrapperForActiveDatabase.class);

    private final Actions command;

    public OldCommandWrapperForActiveDatabase(Actions command) {
        this.command = command;
    }

    @Override
    public void execute() {
        try {
            JabRefGUI.getMainFrame().getCurrentBasePanel().runCommand(command);
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
}
