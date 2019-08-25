package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;

/**
 *
 * @implNote Enter the extensions without a dot! The dot is added implicitly.
 */
public enum StandardFileType implements FileType {
    BIBTEXML("bibx", "xml"),
    ENDNOTE("ref", "enw"),
    FREECITE("txt", "xml"),
    ISI("isi", "txt"),
    MEDLINE("nbib", "xml"),
    MEDLINE_PLAIN("nbib", "txt"),
    PUBMED("fcgi"),
    SILVER_PLATTER("dat", "txt"),

    AUX("aux"),
    BIBTEX_DB("bib"),
    CITATION_STYLE("csl"),
    CLASS("class"),
    CSV("csv"),
    HTML("html"),
    JAR("jar"),
    JAVA_KEYSTORE("jks"),
    JSTYLE("jstyle"),
    LAYOUT("layout"),
    ODS("ods"),
    PDF("pdf"),
    RIS("ris"),
    TERMS("terms"),
    TXT("txt"),
    RDF("rdf"),
    RTF("rtf"),
    SXC("sxc"),
    TEX("tex"),
    XML("xml"),
    JSON("json"),
    XMP("xmp"),
    ZIP("zip");

    private final List<String> extensions;

    StandardFileType(String... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    public static FileType newFileType(String... extensions) {
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i].contains(".")) {
                extensions[i] = extensions[i].substring(extensions[i].indexOf('.') + 1);
            }
        }
        return () -> Arrays.asList(extensions);
    }
}
