package org.jabref.logic.remote.server;

@FunctionalInterface
public interface MessageHandler {

    void handleCommandLineArguments(String[] message);

}
