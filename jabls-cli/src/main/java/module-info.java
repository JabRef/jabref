module org.jabref.jabls.cli {
    opens org.jabref.languageserver.cli to info.picocli;

    requires transitive org.jabref.jablib;
    requires transitive org.jabref.jabls;

    requires afterburner.fx;

    requires org.slf4j;
    requires jul.to.slf4j;

    requires info.picocli;
}
