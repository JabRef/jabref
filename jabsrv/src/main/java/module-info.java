module org.jabref.jabsrv {

    exports org.jabref.http;
    exports org.jabref.http.dto to com.google.gson, org.glassfish.hk2.locator;
    exports org.jabref.http.dto.cayw to com.google.gson;
    exports org.jabref.http.server;
    exports org.jabref.http.server.cayw to org.glassfish.jersey.core.server;
    exports org.jabref.http.server.manager;
    exports org.jabref.http.server.resources;
    exports org.jabref.http.server.resources.callback to org.glassfish.hk2.locator, org.glassfish.jersey.core.server;
    exports org.jabref.http.server.resources.command to org.glassfish.jersey.core.server;
    exports org.jabref.http.server.services;

    opens org.jabref.http.dto to com.google.gson;
    opens org.jabref.http.server to org.glassfish.hk2.utilities, org.glassfish.hk2.locator;
    opens org.jabref.http.server.cayw to com.google.gson, org.glassfish.hk2.locator, org.glassfish.hk2.utilities;
    opens org.jabref.http.server.resources.callback to org.glassfish.hk2.utilities;
    opens org.jabref.http.server.resources to org.glassfish.hk2.locator, org.glassfish.hk2.utilities;
    opens org.jabref.http.server.resources.command to com.google.gson, org.glassfish.hk2.locator, org.glassfish.hk2.utilities, tools.jackson.databind;

    requires transitive javafx.base;

    // For CAYW feature
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires afterburner.fx;
    requires java.desktop;

    // For ServiceLocatorUtilities.createAndPopulateServiceLocator()
    requires /*runtime*/ org.glassfish.hk2.locator;

    requires transitive org.jabref.jablib;

    requires transitive org.slf4j;

    requires com.google.common;
    requires transitive com.google.gson;

    requires transitive org.glassfish.hk2.api;

    requires static jakarta.annotation;
    requires transitive jakarta.inject;

    // Injection framework
    requires /*runtime*/ org.glassfish.jersey.inject.hk2;

    requires org.glassfish.grizzly;
    requires transitive org.glassfish.grizzly.http.server;
    requires transitive jakarta.ws.rs;

    requires org.glassfish.jersey.core.server;
    requires org.glassfish.jersey.container.grizzly2.http;

    requires net.harawata.appdirs;

    requires transitive org.jspecify;
    requires java.logging;
    requires tools.jackson.core;
    requires tools.jackson.databind;
    requires transitive com.fasterxml.jackson.annotation;
}
