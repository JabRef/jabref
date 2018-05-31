package org.jabref.logic.util;

import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.jabref.logic.l10n.Localization;

/**
 *
 * @implNote Enter the extensions without a dot! The dot is added implicitly.
 */
public enum BasicFileType implements FileType {
    BIBTEXML(Localization.lang("%0 file", "BibTeXML"), "bibx", "xml"),
    ENDNOTE(Localization.lang("%0 file", "EndNote/Refer"), "ref", "enw"),
    FREECITE(Localization.lang("%0 file", "FreeCite"), "txt", "xml"),
    ISI(Localization.lang("%0 file", "ISI"), "isi", "txt"),
    MEDLINE(Localization.lang("%0 file", "Medline"), "nbib", "xml"),
    MEDLINE_PLAIN(Localization.lang("%0 file", "MedlinePlain"), "nbib", "txt"),
    PUBMED(Localization.lang("%0 file", "PubMed"), "fcgi"),
    SILVER_PLATTER(Localization.lang("%0 file", "SilverPlatter"), "dat", "txt"),

    AUX(Localization.lang("%0 file", "AUX"), "aux"),
    BIBTEX_DB(String.format("%1s %2s", "BibTex", Localization.lang("Library")), "bib"),
    CITATION_STYLE(Localization.lang("%0 file", "CSL"), "csl"),
    CLASS(Localization.lang("%0 file", "CLASS"), "class"),
    CSV(Localization.lang("%0 file", "CSV"), "csv"),
    HTML(Localization.lang("%0 file", "HTML"), "html"),
    JAR(Localization.lang("%0 file", "JAR"), "jar"),
    JSTYLE(Localization.lang("Style file"), "jstyle"),
    LAYOUT(Localization.lang("Custom layout file"), "layout"),
    ODS(Localization.lang("%0 file", Localization.lang("OpenDocument spreadsheet")), "ods"),
    PDF(Localization.lang("%0 file"), "pdf"),
    RIS(Localization.lang("%0 file", "RIS"), "ris"),
    TERMS(Localization.lang("Protected terms file"), "terms"),
    TXT(Localization.lang("%0 file", Localization.lang("Plain text")), "txt"),
    RDF(Localization.lang("%0 file", "RDF"), "rdf"),
    RTF(Localization.lang("%0 file", "RTF"), "rtf"),
    SXC(Localization.lang("%0 file", "OpenOffice/LibreOffice Calc"), "sxc"),
    XML(Localization.lang("%0 file", "XML"), "xml"),
    XMP(Localization.lang("%0 file", "XMP"), "xmp"),
    ZIP(Localization.lang("%0 file", "ZIP"), "zip"),

    DEFAULT(Localization.lang("%0 file", "DEFAULT"), "default");

    private final List<String> extensions;
    private String description;

    BasicFileType(String description, String... extensions) {
        this.description = description;
        this.extensions = Arrays.asList(extensions);
    }

    @Override
    public List<String> getExtensions() {
        return getExtensions(extensions);
    }

    @Override
    public String getDescription() {
        return getDescription(description, extensions);
    }

    @Override
    public String getFirstExtensionWithDot() {
        return getFirstExtensionWithDot(extensions);
    }

    @Override
    public List<String> getExtensionsWithDot() {
        return getExtensionsWithDot(extensions);
    }

    public static FileType addnewFileType(String description, String... extensionsToAdd) {
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

            @Override
            public String getDescription() {
                return BasicFileType.getDescription(description, extensions);
            }
        };
        return fileType;

    }

    private static String getDescription(String description, List<String> extensions) {
        StringJoiner sj = new StringJoiner(", ", description + " (", ")");
        for (String ext : extensions) {
            sj.add("*." + ext);
        }
        return sj.toString();
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
