package org.jabref.gui.actions;

/**
 * BaseAction is used to define actions that are called from the
 * base frame through runCommand(). runCommand() finds the
 * appropriate BaseAction object, and runs its action() method.
 */
@FunctionalInterface
public interface BaseAction {

    void action() throws Exception;
}
