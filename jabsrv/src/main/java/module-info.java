module org.jabref.jabsrv {
    exports org.jabref.http.server;

    exports org.jabref.http.dto to com.google.gson, org.glassfish.hk2.locator;

    opens org.jabref.http.server to org.glassfish.hk2.utilities, org.glassfish.hk2.locator;

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

    requires afterburner.fx;
    provides com.airhacks.afterburner.views.ResourceLocator
            with org.jabref.http.JabRefResourceLocator;

    requires org.glassfish.grizzly;
    requires org.glassfish.grizzly.http;
    requires org.glassfish.grizzly.http.server;
    requires jakarta.validation;
    requires jakarta.ws.rs;

    requires jersey.common;

    requires net.harawata.appdirs;
    requires com.sun.jna;
    requires com.sun.jna.platform;

    requires jbibtex;
    requires citeproc.java;

    requires transitive org.jspecify;
    requires java.logging;
    requires jersey.container.grizzly2.http;
    requires jersey.server;
}
