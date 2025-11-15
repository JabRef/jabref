module org.jabref.jabls {
    exports org.jabref.languageserver;
    opens org.jabref.languageserver to org.eclipse.lsp4j, org.eclipse.lsp4j.jsonrpc, com.google.gson;
    exports org.jabref.languageserver.controller;
    exports org.jabref.languageserver.util;

    requires org.jabref.jablib;

    requires com.fasterxml.jackson.databind;

    requires com.google.common;
    requires com.google.gson;

    requires org.slf4j;

    requires org.eclipse.lsp4j;
    requires org.eclipse.lsp4j.jsonrpc;
    requires org.eclipse.lsp4j.websocket;
    requires org.jspecify;

}
