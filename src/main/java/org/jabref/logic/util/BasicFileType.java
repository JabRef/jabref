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
    BIBTEX_DB(String.format("%1s %2s", "BibTex", Localization.lang("Library")), "bib"),
    BIBTEXML(Localization.lang("%0 file", "BibTeXML"), "bibx", "xml"),
    BIBLIOSCAPE(Localization.lang("%0 file", "Biblioscape"), "txt"),
    COPAC(Localization.lang("%0 file", "Copac"), "txt"),
    CITATION_STYLE(Localization.lang("%0 file", "CSL"), "csl"),
    DIN_1505(Localization.lang("%0 file", "DIN 1505"), "rtf"),
    ENDNOTE(Localization.lang("%0 file", "EndNote/Refer"), "ref", "enw"),
    ENDNOTE_XML(Localization.lang("%0 file", "EndNote XML"), "xml"),
    FREECITE(Localization.lang("%0 file", "FreeCite"), "txt", "xml"),
    HARVARD_RTF(Localization.lang("%0 file", "Harvard"), "rtf"),
    INSPEC(Localization.lang("%0 file", "INSPEC"), "txt"),
    ISI(Localization.lang("%0 file", "ISI"), "isi", "txt"),
    ISO_690_RTF(Localization.lang("%0 file", "ISO 690"), "rtf"),
    ISO_690_TXT(Localization.lang("%0 file", "ISO 690"), "txt"),
    MEDLINE(Localization.lang("%0 file", "Medline"), "nbib", "xml"),
    MEDLINE_PLAIN(Localization.lang("%0 file", "MedlinePlain"), "nbib", "txt"),
    MIS_QUARTERLY(Localization.lang("%0 file", "MIS Quarterly"), "rtf"),
    MODS(Localization.lang("%0 file", "MODS"), "xml"),
    MSBIB(Localization.lang("%0 file", "MS Office 2007 bib"), "xml"),
    OO_LO(Localization.lang("%0 file", "OpenOffice/LibreOffice"), "csv"),
    OVID(Localization.lang("%0 file", "Ovid"), "txt"),
    PDF_CONTENT(Localization.lang("%0 file", "PDF content "), "pdf"),
    PUBMED(Localization.lang("%0 file", "PubMed"), "fcgi"),
    REPEC("REPEC New Economic Papers (NEP)", "txt"),
    RIS(Localization.lang("%0 file", "RIS"), "ris"),
    SILVER_PLATTER(Localization.lang("%0 file", "SilverPlatter"), "dat", "txt"),
    SIMPLE_HTML(Localization.lang("%0 file", Localization.lang("Simple HTML")), "html"),
    PDF_XMP(Localization.lang("XMP-annotated PDF"), "pdf"),
    PLAIN_XMP(Localization.lang("%0 file", "XMP"), "xmp"),

    AUX(Localization.lang("%0 file", "AUX"), "aux"),
    JSTYLE(Localization.lang("Style file"), "jstyle"),
    LAYOUT(Localization.lang("Custom layout file"), "layout"),
    TERMS(Localization.lang("Protected terms file"), "terms"),
    TXT(Localization.lang("%0 file", Localization.lang("Plain text")), "txt"),
    CLASS(Localization.lang("%0 file", "CLASS"), "class"),
    JAR(Localization.lang("%0 file", "JAR"), "jar"),
    XML(Localization.lang("%0 file", "XML"), "xml"),
    ZIP(Localization.lang("%0 file", "ZIP"), "zip"),
    ODS(Localization.lang("%0 file", Localization.lang("OpenDocument spreadsheet")), "ods"),
    SXC(Localization.lang("%0 file", "OpenOffice/LibreOffice Calc"), "sxc"),
    HTML(Localization.lang("%0 file", "HTML"), "html"),
    RTF(Localization.lang("%0 file", "RTF"), "rtf"),
    RDF(Localization.lang("%0 file", "RDF"), "rdf"),
    CSV(Localization.lang("%0 file", "CSV"), "csv"),
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
