package net.sf.jabref.logic.util;

import java.util.StringJoiner;

import net.sf.jabref.logic.l10n.Localization;

/**
 * This enum contains all kind of file extensions for open and save dialogs.
 *
 * Important: Enter the extension without a dot! The dot is added implicitly.
 */
public enum FileExtensions {
    //important: No dot before the extension!
    BIBTEX_DB(String.format("%1s %2s", "BibTex", Localization.lang("Database")), "bib"),
    BIBTEXML(Localization.lang("%0 file", "BibTeXML"), "bibx", "xml"),
    BILBIOSCAPE(Localization.lang("%0 file", "Biblioscape"), "txt"),
    COPAC(Localization.lang("%0 file", "Copac"), "txt"),
    CITATION_STYLE(Localization.lang("%0 file", "CSL"), "csl"),
    ENDNOTE(Localization.lang("%0 file", "Endnote/Refer"), "ref", "enw"),
    FREECITE(Localization.lang("%0 file", "FreeCite"), "txt", "xml"),
    INSPEC(Localization.lang("%0 file", "INSPEC"), "txt"),
    ISI(Localization.lang("%0 file", "ISI"), "isi", "txt"),
    MEDLINE(Localization.lang("%0 file", "Medline"), "nbib", "xml"),
    MEDLINE_PLAIN(Localization.lang("%0 file", "MedlinePlain"), "nbib", "txt"),
    MODS(Localization.lang("%0 file", "MODS"), "xml"),
    MSBIB(Localization.lang("%0 file", "MSBib"), "xml"),
    OVID(Localization.lang("%0 file", "Ovid"), "txt"),
    PDF_CONTENT(Localization.lang("%0 file", "PDF content "), "pdf"),
    PUBMED(Localization.lang("%0 file", "PubMed"), "fcgi"),
    REPEC("REPEC New Economic Papers (NEP)", "txt"),
    RIS(Localization.lang("%0 file", "RIS"), "ris"),
    SILVER_PLATTER(Localization.lang("%0 file", "SilverPlatter"), "dat", "txt"),
    XMP(Localization.lang("XMP-annotated PDF"), "pdf"),

    AUX(Localization.lang("%0 file", "AUX"), "aux"),
    JSTYLE(Localization.lang("Style file"), "jstyle"),
    LAYOUT(Localization.lang("Custom layout file"), "layout"),
    TERMS(Localization.lang("Protected terms file"), "terms"),
    TXT(Localization.lang("%0 file", Localization.lang("Plain text")), "txt"),
    CLASS(Localization.lang("%0 file", "CLASS"), "class"),
    JAR(Localization.lang("%0 file", "JAR"), "jar"),
    XML(Localization.lang("%0 file", "XML"), "xml"),
    ZIP(Localization.lang("%0 file", "ZIP"), "zip");

    private final String[] extension;
    private final String description;

    private FileExtensions(String description, String... extension) {
        this.extension = extension;
        this.description = description;
    }

    //Array because a) is varags and b) gets passed as varags parameter to FileExtensionNameFilter
    public String[] getExtensions() {
        return extension;
    }

    public String getDescription() {
        StringJoiner sj = new StringJoiner(", ", description + " (", ")");
        for (String ext : extension) {
            sj.add("*." + ext);
        }
        return sj.toString();
    }

    public String getFirstExtensionWithDot() {
        return "." + extension[0].trim();
    }
}
