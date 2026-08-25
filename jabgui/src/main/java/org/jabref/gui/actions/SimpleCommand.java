package org.jabref.gui.actions;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;

import org.jabref.gui.util.BindingsHelper;

import de.saxsys.mvvmfx.utils.commands.CommandBase;

/// A simple command that does not track progress of the action.
public abstract class SimpleCommand extends CommandBase {

    /// Shared, because it never changes: a command that reports no progress reports the same
    /// nothing as every other one, and a read-only property that is never set can be bound from
    /// anywhere without the bindings interfering.
    private static final ReadOnlyDoubleProperty NO_PROGRESS = new ReadOnlyDoubleWrapper(0).getReadOnlyProperty();

    protected ReadOnlyStringWrapper statusMessage = new ReadOnlyStringWrapper("");

    public ReadOnlyStringProperty statusMessageProperty() {
        return statusMessage.getReadOnlyProperty();
    }

    @Override
    public double getProgress() {
        return NO_PROGRESS.get();
    }

    /// A constant zero rather than `null`.
    ///
    /// [CommandBase] promises a property here, and a `null` breaks the callers that trust it —
    /// binding a progress bar to a command is a one-liner that would throw instead. "No progress
    /// to report" is a value, not an absence, and every one of the 100-odd subclasses means it.
    @Override
    public ReadOnlyDoubleProperty progressProperty() {
        return NO_PROGRESS;
    }

    public void setExecutable(boolean executable) {
        this.executable.bind(BindingsHelper.constantOf(executable));
    }
}
