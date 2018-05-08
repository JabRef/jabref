open module org.jabref {
    exports org.jabref;

    exports org.jabref.gui;
    exports org.jabref.gui.logging;
    exports org.jabref.gui.maintable;
    exports org.jabref.gui.specialfields;

    exports org.jabref.model.database;

    exports org.jabref.logic;
    exports org.jabref.logic.citationstyle;
    exports org.jabref.logic.search;

//    opens org.jabref.gui to com.google.common;
//    opens org.jabref.model.database to com.google.common;
//    opens org.jabref.logic.citationstyle to com.google.common;

    // Swing
	requires java.desktop;

	// SQL
	requires java.sql;
	requires pgjdbc.ng;

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
	requires spin;
    requires easybind;
    requires de.jensd.fx.glyphs.commons;
    requires controlsfx;

    // Libre Office
	/*requires ridl;
	requires unoil;
	requires juh;*/
}
