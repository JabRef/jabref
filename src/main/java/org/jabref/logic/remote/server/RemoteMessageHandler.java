package org.jabref.logic.remote.server;

@FunctionalInterface
public interface RemoteMessageHandler {

    void handleCommandLineArguments(String[] message);
}
