/// Language Server Protocol implementation for `.bib` files (diagnostics from the
/// integrity checker, based on lsp4j). Started by the `jabls-cli` module or embedded in the GUI.
///
/// Entry points: [org.jabref.languageserver.LspLauncher],
/// [org.jabref.languageserver.BibtexTextDocumentService].
///
/// @see <a href="https://devdocs.jabref.org/architecture-and-components.html">Architecture and components</a>
module org.jabref.jabls {
    exports org.jabref.languageserver;
    opens org.jabref.languageserver to org.eclipse.lsp4j, org.eclipse.lsp4j.jsonrpc, com.google.gson;
    exports org.jabref.languageserver.controller;
    exports org.jabref.languageserver.util;

    requires transitive org.jabref.jablib;

    requires tools.jackson.core;
    requires tools.jackson.databind;

    requires com.google.common;
    requires transitive com.google.gson;

    requires org.slf4j;

    requires transitive org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires transitive org.jspecify;

}
