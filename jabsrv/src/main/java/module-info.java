module org.jabref.jabsrv {
    exports org.jabref.http.server;
    exports org.jabref.http.manager;

    exports org.jabref.http.dto to com.google.gson, org.glassfish.hk2.locator;
    exports org.jabref.http.dto.cayw to com.google.gson;

    opens org.jabref.http.server to org.glassfish.hk2.utilities, org.glassfish.hk2.locator;
    exports org.jabref.http.server.cayw;
    opens org.jabref.http.server.cayw to com.google.gson, org.glassfish.hk2.locator, org.glassfish.hk2.utilities;
    opens org.jabref.http.dto to com.google.gson;

    requires javafx.base;

    // For CAYW feature
    requires transitive javafx.graphics;
    requires transitive javafx.controls;
    requires afterburner.fx;

    // For ServiceLocatorUtilities.createAndPopulateServiceLocator()
    requires org.glassfish.hk2.locator;
    uses org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

    requires org.jabref.jablib;

    requires org.slf4j;

    requires com.google.common;
    requires com.google.gson;

    requires org.glassfish.hk2.api;

    requires jakarta.annotation;
    requires jakarta.inject;

    requires org.glassfish.grizzly;
    requires org.glassfish.grizzly.http;
    requires org.glassfish.grizzly.http.server;
    requires jakarta.validation;
    requires jakarta.ws.rs;

    requires org.glassfish.jersey.common;

    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires org.jbibtex;
    requires de.undercouch.citeproc.java;

    requires transitive org.jspecify;
    requires java.logging;
    requires org.glassfish.jersey.grizzly2.http;
    requires org.glassfish.jersey.server;
}
