open module org.jabref {
    exports org.jabref;

    requires org.jabref.model;
    requires org.jabref.logic;

    // Swing
	requires java.desktop;

	// SQL
	requires java.sql;
	requires postgresql;

	// JavaFX
	requires javafx.graphics;
	requires javafx.swing;
	requires javafx.controls;
	requires javafx.web;
	requires javafx.fxml;
	requires afterburner.fx;
//	requires de.jensd.fx.glyphs.commons;
//	requires de.jensd.fx.glyphs.materialdesignicons;

    provides com.airhacks.afterburner.views.ResourceLocator
        with org.jabref.gui.util.JabRefResourceLocator;

    provides com.airhacks.afterburner.injection.PresenterFactory
        with org.jabref.gui.DefaultInjector;

	// Logging
	requires org.slf4j;
	requires org.apache.logging.log4j;

	// Preferences and XML
	requires java.prefs;
	requires java.xml.bind; // Deprecated!
    requires jdk.xml.dom;

    // Annotations (@PostConstruct)
    requires java.annotation;

    // Microsoft application insights
    requires applicationinsights.core;

    requires glazedlists.java15;
	requires jgoodies.forms;
	requires commons.logging;
	requires com.google.common;
    requires easybind;
    requires de.jensd.fx.glyphs.commons;
    requires controlsfx;
    requires mvvmfx;

    // Libre Office
	/*requires ridl;
	requires unoil;
	requires juh;*/
}
