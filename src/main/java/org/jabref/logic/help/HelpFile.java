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
    CITATION_KEY_PATTERN("setup/citationkeypatterns"),
    OWNER("fields/owner"),
    TIMESTAMP("fields/timestamp"),
    CUSTOM_EXPORTS_NAME_FORMATTER("import-export/export/customexports#using-custom-name-formatters"),
    GENERAL_FIELDS("setup/generalfields"),
    REMOTE("general/remote"),
    REGEX_SEARCH("fields/filelinks#RegularExpressionSearch"),
    PREVIEW("setup/preview"),
    AUTOSAVE("general/autosave"),
    // The help page covers both OO and LO.
    OPENOFFICE_LIBREOFFICE("import-export/other-integrations/openofficeintegration"),
    FETCHER_ACM("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database/acmportal"),
    FETCHER_ADS("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database/ads"),
    FETCHER_BIBSONOMY_SCRAPER(""),
    FETCHER_CITESEERX("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database#citeseer"),
    FETCHER_DBLP("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database/dblp"),
    FETCHER_DIVA("finding-sorting-and-cleaning-entries/import-using-publication-identifiers/divatobibtex"),
    FETCHER_DOAJ("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database/doaj"),
    FETCHER_DOI("finding-sorting-and-cleaning-entries/import-using-publication-identifiers/doitobibtex"),
    FETCHER_GOOGLE_SCHOLAR("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database#googlescholar"),
    FETCHER_GVK("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database#gvk"),
    FETCHER_IEEEXPLORE("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database#ieeexplore"),
    FETCHER_INSPIRE("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database#inspire"),
    FETCHER_ISBN("finding-sorting-and-cleaning-entries/import-using-publication-identifiers/isbntobibtex"),
    FETCHER_MEDLINE("finding-sorting-and-cleaning-entries/import-using-publication-identifiers/medlinetobibtex"),
    FETCHER_OAI2_ARXIV("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database/arxiv"),
    FETCHER_RFC("finding-sorting-and-cleaning-entries/import-using-publication-identifiers/rfctobibtex"),
    FETCHER_SPRINGER("finding-sorting-and-cleaning-entries/import-using-online-bibliographic-database/springer"),
    FETCHER_TITLE("finding-sorting-and-cleaning-entries/import-using-publication-identifiers"),
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
