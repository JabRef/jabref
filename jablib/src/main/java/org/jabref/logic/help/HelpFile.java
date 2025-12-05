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
    FETCHER_ACM("collect/import-using-online-bibliographic-database#acm-portal"),
    FETCHER_SCIENCE_DIRECT("ScienceDirectHelp", "collect/import-using-online-bibliographic-database"),
    FETCHER_BIBSONOMY_SCRAPER(""),
    FETCHER_BIODIVERSITY_HERITAGE_LIBRARY("collect/import-using-online-bibliographic-database#bibliotheksverbund-bayern-bvb"),
    FETCHER_CITESEERX("collect/import-using-online-bibliographic-database#citeseerx"),
    FETCHER_COLLECTION_OF_COMPUTER_SCIENCE_BIBLIOGRAPHIES("collect/import-using-online-bibliographic-database#collection-of-computer-science-bibliographies-ccsb"),
    FETCHER_CROSSREF("collect/import-using-online-bibliographic-database#crossref-unpaywalll"),
    FETCHER_DBLP("collect/import-using-online-bibliographic-database#dblp"),
    FETCHER_DOAB("collect/import-using-online-bibliographic-database#doab"),
    FETCHER_DOAJ("collect/import-using-online-bibliographic-database#doaj"),
    FETCHER_DIVA("collect/add-entry-using-an-id"),
    FETCHER_DOI("collect/add-entry-using-an-id"),
    FETCHER_GOOGLE_SCHOLAR("collect/import-using-online-bibliographic-database#googlescholar"),
    FETCHER_GVK("collect/import-using-online-bibliographic-database#gvk"),
    FETCHER_IEEEXPLORE("collect/import-using-online-bibliographic-database#ieeexplore"),
    FETCHER_INSPIRE("collect/import-using-online-bibliographic-database#inspire"),
    FETCHER_ISBN("collect/add-entry-using-an-id"),
    FETCHER_MEDLINE("collect/import-using-online-bibliographic-database#medline-pubmed"),
    FETCHER_OAI2_ARXIV("collect/import-using-online-bibliographic-database#arxiv"),
    FETCHER_RFC("collect/add-entry-using-an-id"),
    FETCHER_SPRINGER("collect/import-using-online-bibliographic-database#springer"),
    FETCHER_ZBMATH("collect/import-using-online-bibliographic-database#zbmath-open"),
    FETCHER_TITLE("collect/add-entry-using-an-id"),
    //  FETCHER_SCIENCEDIRECT(""),
    DATABASE_PROPERTIES("setup/databaseproperties"),
    FIND_DUPLICATES("finding-sorting-and-cleaning-entries/findduplicates"),
    SQL_DATABASE_MIGRATION("collaborative-work/sqldatabase/sqldatabasemigration"),
    PUSH_TO_APPLICATION("cite/pushtoapplications"),
    AI_GENERAL_SETTINGS("ai/preferences"),
    AI_EXPERT_SETTINGS("ai/preferences#ai-expert-settings"),
    AI_TEMPLATES("ai/preferences#templates");

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
