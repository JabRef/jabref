package net.sf.jabref.gui;

import net.sf.jabref.logic.l10n.Localization;

/**
 *
 *
 *This enum contains all kind of file extensions for open and save dialogs
 *Important: Enter the extension without a dot!
 *The dot is added implicit
 */
public enum FileExtensions {

    //important: No dot before the extension!
    BIBTEX_DB(
            String.format("%1s %2s", "BibTex", Localization.lang("Database")), "bib"),
    BIBTEXML(
            Localization.lang("%0 file", "BibTeXML"), "bibx"),
    AUX(
            Localization.lang("%0 file", "AUX"), "aux"),
    SILVER_PLATTERN(
            Localization.lang("%0 file", "SilverPlatter"), "dat"),
    PUBMED(
            Localization.lang("%0 file", "PubMed"), "fcgi"),
    RIS(
            Localization.lang("%0 file", "RIS"), "ris"),
    ENDNOTE(
            Localization.lang("%0 file", "Endnote/Refer"), "ref"),
    JSTYLE(
            Localization.lang("Style file"), "jstyle"),
    LAYOUT(
            Localization.lang("Custom layout file"), "layout"),
    XML(
            Localization.lang("%0 file", "XML"), "xml"),
    TERMS(
            Localization.lang("Protected terms file"), "terms"),
    TXT(
            Localization.lang("%0 file", Localization.lang("plain text")), "txt"),
    CLASS(
            Localization.lang("%0 file", "CLASS"), "class"),
    JAR(
            Localization.lang("%0 file", "JAR"), "jar"),
    ZIP(
            Localization.lang("%0 file", "ZIP"), "zip");

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

        return description;
    }

    public String getFirstExtensionWithDot() {
        return "." + extension[0].trim();
    }
}
