package org.jabref.gui.actions;

/**
 * BaseAction is used to define actions that are called from the
 * base frame through runCommand(). runCommand() finds the
 * appropriate BaseAction object, and runs its action() method.
 *
 * @deprecated use {@link SimpleCommand} instead
 */
@FunctionalInterface
@Deprecated
public interface BaseAction {

    void action() throws Exception;
}
