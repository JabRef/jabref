package org.jabref.gui.actions;

import de.saxsys.mvvmfx.utils.commands.CommandBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.jabref.gui.util.BindingsHelper;

/**
 * A simple command that does not track progress of the action.
 */
public abstract class SimpleCommand extends CommandBase {

    protected ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("");

    public String getStatusMessage() {
        return statusMessage.get();
    }

    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
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
