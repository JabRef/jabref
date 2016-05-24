package net.sf.jabref.logic.remote.server;

@FunctionalInterface
public interface MessageHandler {

    void handleMessage(String message);

}
