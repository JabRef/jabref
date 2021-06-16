package org.jabref.logic.help;

/**
 * This enum globally defines all help pages with the name of the markdown file in the help repository at Github.
 *
 * @see <a href="https://github.com/JabRef/user-documentation">user-documentation@github</a>
 */
public enum HelpFile {
    // empty string denotes that it refers to the TOC/index
    CONTENTS(""), // this is always the index
    ENTRY_EDITOR("advanced/entryeditor"),
    STRING_EDITOR("setup/stringeditor"),
    GROUPS("finding-sorting-and-cleaning-entries/groups#groups-structure-creating-and-removing-groups"),
    SPECIAL_FIELDS("finding-sorting-and-cleaning-entries/specialfields"),
    CITATION_KEY_PATTERN("setup/citationkeypatterns"),
    OWNER("advanced/entryeditor/owner"),
    TIMESTAMP("advanced/entryeditor/timestamp"),
    CUSTOM_EXPORTS_NAME_FORMATTER("collaborative-work/export/customexports#using-custom-name-formatters"),
    GENERAL_FIELDS("setup/generalfields"),
    REMOTE("advanced/remote"),
    REGEX_SEARCH("finding-sorting-and-cleaning-entries/filelinks#using-regular-expression-search-for-auto-linking"),
    PREVIEW("setup/preview"),
    AUTOSAVE("advanced/autosave"),
    // The help page covers both OO and LO.
    OPENOFFICE_LIBREOFFICE("cite/openofficeintegration"),
    FETCHER_ACM("collect/import-using-online-bibliographic-database#acmportal"),
    FETCHER_ADS("collect/import-using-online-bibliographic-database#ads"),
    FETCHER_BIBSONOMY_SCRAPER(""),
    FETCHER_CITESEERX("collect/import-using-online-bibliographic-database#citeseer"),
    FETCHER_DBLP("collect/import-using-online-bibliographic-database#dblp"),
    FETCHER_DIVA("collect/add-entry-using-an-id"),
    FETCHER_DOAJ("collect/import-using-online-bibliographic-database#doaj"),
    FETCHER_DOI("collect/add-entry-using-an-id"),
    FETCHER_GOOGLE_SCHOLAR("collect/import-using-online-bibliographic-database#googlescholar"),
    FETCHER_GVK("collect/import-using-online-bibliographic-database#gvk"),
    FETCHER_IEEEXPLORE("collect/import-using-online-bibliographic-database#ieeexplore"),
    FETCHER_INSPIRE("collect/import-using-online-bibliographic-database#inspire"),
    FETCHER_ISBN("collect/add-entry-using-an-id"),
    FETCHER_MEDLINE("collect/import-using-online-bibliographic-database#medline"),
    FETCHER_OAI2_ARXIV("collect/import-using-online-bibliographic-database#arxiv"),
    FETCHER_RFC("collect/add-entry-using-an-id"),
    FETCHER_SPRINGER("collect/import-using-online-bibliographic-database#springer"),
    FETCHER_TITLE("collect/add-entry-using-an-id"),
    FETCHER_SCIENCEDIRECT(""),
    DATABASE_PROPERTIES("setup/databaseproperties"),
    FIND_DUPLICATES("finding-sorting-and-cleaning-entries/findduplicates"),
    SQL_DATABASE_MIGRATION("collaborative-work/sqldatabase/sqldatabasemigration");

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
