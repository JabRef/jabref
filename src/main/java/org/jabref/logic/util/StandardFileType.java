package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;

import org.jabref.model.util.OptionalUtil;

/**
 * @implNote Enter the extensions in lowercase without a dot! The dot is added implicitly.
 */
public enum StandardFileType implements FileType {

    ANY_FILE("Any", "*"),

    AUX("Aux file", "aux"),
    BIBTEX_DB("Bibtex library", "bib"),
    BST("BibTeX-Style file", "bst"),
    CER("SSL Certificate", "cer"),
    CFF("CFF", "cff"),
    CITATION_STYLE("Citation Style", "csl"),
    CITAVI("Citavi", "ctv6bak", "ctv5bak"),
    CLASS("Class file", "class"),
    CSS("CSS Styleshet", "css"),
    CSV("CSV", "csv"),
    ENDNOTE("Endnote", "ref", "enw"),
    HTML("HTML", "html", "htm"),
    ISI("Isi", "isi", "txt"),
    JAR("JAR", "jar"),
    JAVA_KEYSTORE("Java Keystore", "jks"),
    JSON("JSON", "json"),
    JSTYLE("LibreOffice layout style", "jstyle"),
    LAYOUT("Custom Exporter format", "layout"),
    MARKDOWN("Markdown", "md"),
    MEDLINE("Medline", "nbib", "xml"),
    MEDLINE_PLAIN("Medline Plain", "nbib", "txt"),
    ODS("OpenOffice Calc", "ods"),
    PDF("PDF", "pdf"),
    PUBMED("Pubmed", "fcgi"),
    RDF("RDF", "rdf"),
    RIS("RIS", "ris"),
    RTF("RTF", "rtf"),
    SXC("Open Office Calc 1.x", "sxc"),
    TERMS("Protected terms", "terms"),
    TEX("LaTeX", "tex"),
    TXT("Plain Text", "txt"),
    XML("XML", "xml"),
    XMP("XMP", "xmp"),
    YAML("YAML Markup", "yaml"),
    ZIP("Zip Archive", "zip");

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
                                         .filter(field -> field.getExtensions().stream().anyMatch(exts::contains))
                                         .findAny(),
                new UnknownFileType(extensions));
    }
}
