package org.jabref.logic.tele.server;

@FunctionalInterface
public interface TeleMessageHandler {

    void handleCommandLineArguments(String[] message);
}
