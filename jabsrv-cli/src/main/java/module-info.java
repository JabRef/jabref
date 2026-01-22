module org.jabref.jabsrv.cli {
    opens org.jabref.http.server.cli to info.picocli;

    requires transitive org.jabref.jablib;
    requires transitive org.jabref.jabsrv;

    requires org.slf4j;
    requires jul.to.slf4j;

    requires com.google.common;
    requires com.google.gson;

    // requires org.glassfish.hk2.api;

    requires jakarta.annotation;
    requires jakarta.inject;

    requires afterburner.fx;
    provides com.airhacks.afterburner.views.ResourceLocator
            with org.jabref.http.cli.JabRefResourceLocator;
    requires javafx.base;

    requires org.glassfish.grizzly;
    requires org.glassfish.grizzly.http;
    requires org.glassfish.grizzly.http.server;
    requires jakarta.validation;
    requires jakarta.ws.rs;

    requires org.glassfish.jersey.core.common;
    requires org.glassfish.jersey.container.grizzly2.http;
    requires org.glassfish.jersey.core.server;
    requires info.picocli;

    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires jbibtex;
    requires citeproc.java;

    requires transitive org.jspecify;
    requires java.logging;

}
