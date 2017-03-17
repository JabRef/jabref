package org.jabref.logic.help;

/**
 * This enum globally defines all help pages with the name of the markdown file in the help repository at Github.
 *
 * @see <a href=https://github.com/JabRef/help.jabref.org>help.jabref.org@github</a>
 */
public enum HelpFile {
    COMMAND_LINE(""),
    //Empty because it refers to the TOC/index
    CONTENTS(""),
    ENTRY_EDITOR("EntryEditor"),
    STRING_EDITOR("StringEditor"),
    SEARCH("Search"),
    GROUP("Groups"),
    CONTENT_SELECTOR("ContentSelector"),
    SPECIAL_FIELDS("SpecialFields"),
    BIBTEX_KEY_PATTERN("BibtexKeyPatterns"),
    OWNER("Owner"),
    TIMESTAMP("TimeStamp"),
    CUSTOM_EXPORTS("CustomExports"),
    CUSTOM_EXPORTS_NAME_FORMATTER("CustomExports#NameFormatter"),
    CUSTOM_IMPORTS("CustomImports"),
    GENERAL_FIELDS("GeneralFields"),
    IMPORT_INSPECTION("ImportInspectionDialog"),
    REMOTE("Remote"),
    JOURNAL_ABBREV("JournalAbbreviations"),
    REGEX_SEARCH("ExternalFiles#RegularExpressionSearch"),
    PREVIEW("Preview"),
    AUTOSAVE("Autosave"),
    //The help page covers both OO and LO.
    OPENOFFICE_LIBREOFFICE("OpenOfficeIntegration"),
    FETCHER_ACM("ACMPortal"),
    FETCHER_ADS("ADS"),
    FETCHER_BIBSONOMY_SCRAPER(""),
    FETCHER_CITESEERX("CiteSeer"),
    FETCHER_DBLP("DBLP"),
    FETCHER_DIVA("DiVAtoBibTeX"),
    FETCHER_DOAJ("DOAJ"),
    FETCHER_DOI("DOItoBibTeX"),
    FETCHER_GOOGLE_SCHOLAR("GoogleScholar"),
    FETCHER_GVK("GVK"),
    FETCHER_IEEEXPLORE("IEEEXplore"),
    FETCHER_INSPIRE("INSPIRE"),
    FETCHER_ISBN("ISBNtoBibTeX"),
    FETCHER_MEDLINE("Medline"),
    FETCHER_OAI2_ARXIV("arXiv"),
    FETCHER_SPRINGER("Springer"),
    FETCHER_TITLE("TitleToBibTeX"),
    FETCHER_SCIENCEDIRECT(""),
    DATABASE_PROPERTIES("DatabaseProperties"),
    FIND_DUPLICATES("FindDuplicates"),
    SQL_DATABASE_MIGRATION("SQLDatabaseMigration"),
    SQL_DATABASE("SQLDatabase");

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
