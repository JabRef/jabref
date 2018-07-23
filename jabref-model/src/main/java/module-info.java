open module org.jabref.model {

    exports org.jabref.architecture;
    exports org.jabref.model;
    exports org.jabref.model.auxparser;
    exports org.jabref.model.bibtexkeypattern;
    exports org.jabref.model.cleanup;
    exports org.jabref.model.database;
    exports org.jabref.model.database.event;
    exports org.jabref.model.database.shared;
    exports org.jabref.model.entry;
    exports org.jabref.model.entry.event;
    exports org.jabref.model.entry.identifier;
    exports org.jabref.model.entry.specialfields;
    exports org.jabref.model.groups;
    exports org.jabref.model.groups.event;
    exports org.jabref.model.metadata;
    exports org.jabref.model.metadata.event;
    exports org.jabref.model.pdf;
    exports org.jabref.model.search;
    exports org.jabref.model.search.matchers;
    exports org.jabref.model.search.rules;
    exports org.jabref.model.strings;
    exports org.jabref.model.util;
    exports org.jabref.search;

    requires javafx.base;
    requires javafx.graphics;

    requires java.sql;

    requires com.google.common;
    requires org.slf4j;

}
