package net.sf.jabref;

/**
 * BaseAction is used to define actions that are called from the
 * base frame through runCommand(). runCommand() finds the
 * appropriate BaseAction object, and runs its action() method.
 */
public abstract class BaseAction {//implements Runnable {
    public abstract void action() throws Throwable;
}
