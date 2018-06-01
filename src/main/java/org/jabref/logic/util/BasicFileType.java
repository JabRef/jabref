package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;

/**
 *
 * @implNote Enter the extensions without a dot! The dot is added implicitly.
 */
public enum BasicFileType implements FileType {
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
    XML("xml"),
    XMP("xmp"),
    ZIP("zip"),

    DEFAULT(Localization.lang("%0 file", "DEFAULT"), "default");

    private final List<String> extensions;

    BasicFileType(String... extensions) {
        this.extensions = Arrays.asList(extensions);
    }

    @Override
    public List<String> getExtensions() {
        return getExtensions(extensions);
    }

    @Override
    public String getFirstExtensionWithDot() {
        return getFirstExtensionWithDot(extensions);
    }

    @Override
    public List<String> getExtensionsWithDot() {
        return getExtensionsWithDot(extensions);
    }

    public static FileType addnewFileType(String... extensionsToAdd) {
        List<String> extensions = Arrays.asList(extensionsToAdd);
        FileType fileType = new FileType() {

            @Override
            public String getFirstExtensionWithDot() {
                return BasicFileType.getFirstExtensionWithDot(extensions);
            }

            @Override
            public List<String> getExtensionsWithDot() {
                return BasicFileType.getExtensionsWithDot(extensions);
            }

            @Override
            public List<String> getExtensions() {
                return BasicFileType.getExtensions(extensions);
            }
        };
        return fileType;
    }

    private static List<String> getExtensionsWithDot(List<String> extensions) {
        return extensions.stream().map(extension -> "." + extension).collect(Collectors.toList());
    }

    private static String getFirstExtensionWithDot(List<String> extensions) {
        return "." + extensions.get(0).trim();
    }

    private static List<String> getExtensions(List<String> extensions) {
        return extensions;
    }

}
