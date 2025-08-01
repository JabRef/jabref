module org.jabref.jabls {
    exports org.jabref.languageserver;
    opens org.jabref.languageserver to lsp4j, lsp4j.jsonrpc;

    requires org.jabref.jablib;

    requires org.slf4j;

    requires lsp4j;
    requires lsp4j.jsonrpc;
    requires lsp4j.websocket;
    requires lsp4j.websocket.jakarta;
}
