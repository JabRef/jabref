package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.util.OptionalUtil;

/**
 * @implNote Enter the extensions in lowercase without a dot! The dot is added implicitly.
 */
public enum StandardFileType implements FileType {

    BIBTEXML("bibx", "xml"),
    ENDNOTE("ref", "enw"),
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
    HTML("html", "htm"),
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
    ZIP("zip"),
    CSS("css");

    private final List<String> extensions;

    StandardFileType(String... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    public static FileType fromExtensions(String... extensions) {
        var exts = Arrays.asList(extensions);

        return OptionalUtil.orElse(Arrays.stream(StandardFileType.values())
                                         .filter(field -> field.getExtensions().stream().anyMatch(elem -> exts.contains(elem)))
                                         .findAny(),
                new UnknownFileType(extensions));
    }
}
