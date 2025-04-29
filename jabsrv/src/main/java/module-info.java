module org.jabref.jabsrv {
    exports org.jabref.http.server;
    opens org.jabref.http.server
            to org.glassfish.hk2.utilities,
            org.glassfish.hk2.locator;

    requires org.jabref.jablib;

    requires org.slf4j;
    requires jul.to.slf4j;
    requires org.apache.logging.log4j.to.slf4j;
    requires org.tinylog.api;
    requires org.tinylog.api.slf4j;
    requires org.tinylog.impl;

    requires com.google.common;
    requires com.google.gson;

    requires org.glassfish.hk2.api;

    requires jakarta.annotation;
    requires jakarta.inject;

    requires afterburner.fx;
    provides com.airhacks.afterburner.views.ResourceLocator
            with org.jabref.http.JabRefResourceLocator;

    // needs to be loaded here as it's otherwise not found at runtime; XJC related maybe
    // requires org.glassfish.jaxb.runtime;

    requires org.glassfish.grizzly;
    requires jakarta.ws.rs;

    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires jbibtex;
    requires citeproc.java;

    requires transitive org.jspecify;
}
