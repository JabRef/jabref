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
