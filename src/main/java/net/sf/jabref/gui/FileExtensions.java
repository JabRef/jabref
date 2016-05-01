package net.sf.jabref.gui;

public enum FileExtensions {

    //importannt: No dot before the extension!
    BIBTEX_DB(
            "BibTeX Database", "bib"),
    BIBTEXML(
            "BibTexMl, bibx"),
    AUX(
            "Aux file", "aux"),
    SILVER_PLATTERN(
            "SilverPlatter", "dat"),
    PUBMED(
            "Pubmed file", "fcgi"),
    RIS(
            "Ris file ", "ris"),
    ENDNOTE(
            "Endnote/Refer file", "ref"),

    JSTYLE(
            "Style file", "jstyle"),
    LAYOUT(
            "Custom Layout File", "layout"),
    XML(
            "XML file", "xml"),
    TERMS(
            "Protected TERMS file", "terms"),
    TXT(
            "Plain text file", "txt"),
    CLASS(
            "Class file", "class"),
    JAR(
            "Jar file", "jar"),
    ZIP(
            "Zip file", "zip");


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
