/// `jabkit`, the command-line interface to JabRef: picocli commands for converting,
/// checking, fetching and searching without the GUI.
///
/// Entry point: [org.jabref.toolkit.JabKitLauncher]; commands live in
/// `org.jabref.toolkit.commands`.
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/cli.html">CLI code howto</a>
module org.jabref.jabkit {
    requires org.jabref.jablib;

    requires info.picocli;
    opens org.jabref.toolkit.commands;
    opens org.jabref.toolkit.converter;
    opens org.jabref.toolkit;

    requires transitive org.jspecify;
    requires java.prefs;

    requires javafx.base;
    requires afterburner.fx;

    requires org.slf4j;
    requires jul.to.slf4j;
    requires /*runtime*/ org.apache.logging.log4j.to.slf4j;
    requires org.tinylog.api;
    requires /*runtime*/ org.tinylog.api.slf4j;
    requires /*runtime*/ org.tinylog.impl;

    requires java.xml;

    // region: other libraries (alphabetically)
    // endregion
}
