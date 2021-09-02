package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.util.OptionalUtil;

/**
 * @implNote Enter the extensions in lowercase without a dot! The dot is added implicitly.
 */
public enum StandardFileType implements FileType {

    BIBTEXML("BibTeXML", "bibx", "xml"),
    ENDNOTE("Endnote", "ref", "enw"),
    ISI("Isi", "isi", "txt"),
    MEDLINE("Medline", "nbib", "xml"),
    MEDLINE_PLAIN("Medline Plain", "nbib", "txt"),
    PUBMED("Pubmed", "fcgi"),
    SILVER_PLATTER("SilverPlatter", "dat", "txt"),
    AUX("Aux file", "aux"),
    BIBTEX_DB("Bibtex library", "bib"),
    CITATION_STYLE("Citation Style", "csl"),
    CLASS("Class file", "class"),
    CSV("CSV", "csv"),
    HTML("HTML", "html", "htm"),
    JAR("JAR", "jar"),
    JAVA_KEYSTORE("Java Keystore", "jks"),
    JSTYLE("LibreOffice layout style", "jstyle"),
    LAYOUT("Custom Exporter format", "layout"),
    ODS("OpenOffice Calc", "ods"),
    PDF("PDF", "pdf"),
    RIS("RIS", "ris"),
    TERMS("Protected terms", "terms"),
    TXT("Plain Text", "txt"),
    RDF("RDF", "rdf"),
    RTF("RTF", "rtf"),
    SXC("Open Office Calc 1.x", "sxc"),
    TEX("LaTeX", "tex"),
    XML("XML", "xml"),
    JSON("JSON", "json"),
    XMP("XMP", "xmp"),
    ZIP("Zip Archive", "zip"),
    CSS("CSS Styleshet", "css"),
    YAML("YAML Markup", "yaml"),
    CFF("CFF", "cff"),
    ANY_FILE("Any", "*");

    private final List<String> extensions;
    private final String name;

    StandardFileType(String name, String... extensions) {
        this.extensions = Arrays.asList(extensions);
        this.name = name;
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public static FileType fromExtensions(String... extensions) {
        var exts = Arrays.asList(extensions);

        return OptionalUtil.orElse(Arrays.stream(StandardFileType.values())
                                         .filter(field -> field.getExtensions().stream().anyMatch(elem -> exts.contains(elem)))
                                         .findAny(),
                                   new UnknownFileType(extensions));
    }
}
