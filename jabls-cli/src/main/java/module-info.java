/// Standalone launcher for the language server in `org.jabref.jabls`.
///
/// Entry point: [org.jabref.languageserver.cli.ServerCli].
///
/// @see <a href="https://devdocs.jabref.org/architecture-and-components.html">Architecture and components</a>
module org.jabref.jabls.cli {
    opens org.jabref.languageserver.cli to info.picocli;

    requires org.jabref.jablib;
    requires org.jabref.jabls;

    requires afterburner.fx;

    requires org.slf4j;
    requires jul.to.slf4j;
    requires /*runtime*/ org.tinylog.api;
    requires /*runtime*/ org.tinylog.impl;
    requires /*runtime*/ org.apache.logging.log4j.to.slf4j;
    requires /*runtime*/ org.tinylog.api.slf4j;

    requires info.picocli;
}
