module org.jabref {
    exports org.jabref;

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
	requires customjfx;
	requires afterburner.fx;
	requires de.jensd.fx.glyphs.commons;
	requires de.jensd.fx.glyphs.materialdesignicons;

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

	// Libre Office
	/*requires ridl;
	requires unoil;
	requires juh;*/
}
