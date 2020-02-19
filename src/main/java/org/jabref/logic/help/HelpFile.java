package org.jabref.logic.help;

/**
 * This enum globally defines all help pages with the name of the markdown file in the help repository at Github.
 *
 * @see <a href="https://github.com/JabRef/user-documentation">user-documentation@github</a>
 */
public enum HelpFile {
    // empty string denotes that it refers to the TOC/index
    CONTENTS(""), // this is always the index
    ENTRY_EDITOR("general/entryeditor"),
    STRING_EDITOR("setup/stringeditor"),
    GROUPS("finding-sorting-and-cleaning-entries/groups#groups-structure-creating-and-removing-groups"),
    SPECIAL_FIELDS("fields/specialfields"),
    BIBTEX_KEY_PATTERN("setup/bibtexkeypatterns"),
    OWNER("fields/owner"),
    TIMESTAMP("fields/timestamp"),
    CUSTOM_EXPORTS_NAME_FORMATTER("import-export/customexports#using-custom-name-formatters"),
    GENERAL_FIELDS("setup/generalfields"),
    REMOTE("general/remote"),
    REGEX_SEARCH("fields/filelinks#RegularExpressionSearch"),
    PREVIEW("setup/preview"),
    AUTOSAVE("general/autosave"),
    //The help page covers both OO and LO.
    OPENOFFICE_LIBREOFFICE("import-export/openofficeintegration"),
    FETCHER_ACM("import-using-online-bibliographic-database/acmportal"),
    FETCHER_ADS("import-using-online-bibliographic-database/ads"),
    FETCHER_BIBSONOMY_SCRAPER(""),
    FETCHER_CITESEERX("import-using-online-bibliographic-database/citeseer"),
    FETCHER_DBLP("import-using-online-bibliographic-database/dblp"),
    FETCHER_DIVA("import-using-publication-identifiers/divatobibtex"),
    FETCHER_DOAJ("import-using-online-bibliographic-database/doaj"),
    FETCHER_DOI("import-using-publication-identifiers/doitobibtex"),
    FETCHER_GOOGLE_SCHOLAR("import-using-online-bibliographic-database/googlescholar"),
    FETCHER_GVK("import-using-online-bibliographic-database/gvk"),
    FETCHER_IEEEXPLORE("import-using-online-bibliographic-database/ieeexplore"),
    FETCHER_INSPIRE("import-using-online-bibliographic-database/inspire"),
    FETCHER_ISBN("import-using-publication-identifiers/isbntobibtex"),
    FETCHER_MEDLINE("import-using-online-bibliographic-database/medline"),
    FETCHER_OAI2_ARXIV("import-using-online-bibliographic-database/arxiv"),
    FETCHER_RFC("import-using-publication-identifiers/rfctobibtex"),
    FETCHER_SPRINGER("import-using-online-bibliographic-database/springer"),
    FETCHER_TITLE("import-using-publication-identifiers/titletobibtex"),
    FETCHER_SCIENCEDIRECT(""),
    DATABASE_PROPERTIES("setup/databaseproperties"),
    FIND_DUPLICATES("finding-sorting-and-cleaning-entries/findduplicates"),
    SQL_DATABASE_MIGRATION("collaborative-work/sqldatabasemigration");

    private final String pageName;

    /**
     * Sets the URL path part of the help page.
     *
     * @param pageName the URL path part of the help page
     */
    HelpFile(String pageName) {
        this.pageName = pageName;
    }

    /**
     * Returns the URL path part of the help page.
     *
     * @return the URL path part of the help page
     */
    public String getPageName() {
        return pageName;
    }
}
