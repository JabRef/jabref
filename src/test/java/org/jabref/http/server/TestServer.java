package org.jabref.http.server;

import java.util.concurrent.CountDownLatch;

import org.slf4j.bridge.SLF4JBridgeHandler;

public class TestServer {
    public static void main(final String[] args) throws InterruptedException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
        Server.startServer(new CountDownLatch(1));
        Thread.currentThread().join();
    }
}
