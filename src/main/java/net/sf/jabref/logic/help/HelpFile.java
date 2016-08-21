package net.sf.jabref.logic.help;

/**
 * This enum globally defines all help pages with the name of the markdown file in the help repository at Github.
 *
 * @see <a href=https://github.com/JabRef/help.jabref.org>help.jabref.org@github</a>
 */
public enum HelpFile {
    COMMAND_LINE(""),
    //Empty because it refers to the TOC/index
    CONTENTS(""),
    ENTRY_EDITOR("EntryEditorHelp"),
    STRING_EDITOR("StringEditorHelp"),
    SEARCH("SearchHelp"),
    GROUP("GroupsHelp"),
    CONTENT_SELECTOR("ContentSelectorHelp"),
    SPECIAL_FIELDS("SpecialFieldsHelp"),
    BIBTEX_KEY_PATTERN("BibtexKeyPatterns"),
    OWNER("OwnerHelp"),
    TIMESTAMP("TimeStampHelp"),
    CUSTOM_EXPORTS("CustomExports"),
    CUSTOM_EXPORTS_NAME_FORMATTER("CustomExports#NameFormatter"),
    CUSTOM_IMPORTS("CustomImports"),
    GENERAL_FIELDS("GeneralFields"),
    IMPORT_INSPECTION("ImportInspectionDialog"),
    REMOTE("RemoteHelp"),
    JOURNAL_ABBREV("JournalAbbreviations"),
    REGEX_SEARCH("ExternalFiles#RegularExpressionSearch"),
    PREVIEW("PreviewHelp"),
    AUTOSAVE("Autosave"),
    //The help page covers both OO and LO.
    OPENOFFICE_LIBREOFFICE("OpenOfficeIntegration"),
    FETCHER_ACM("ACMPortalHelp"),
    FETCHER_ADS("ADSHelp"),
    FETCHER_CITESEERX("CiteSeerHelp"),
    FETCHER_DBLP("DBLPHelp"),
    FETCHER_DIVA_TO_BIBTEX("DiVAtoBibTeXHelp"),
    FETCHER_DOAJ("DOAJHelp"),
    FETCHER_DOI_TO_BIBTEX("DOItoBibTeXHelp"),
    FETCHER_GOOGLE_SCHOLAR("GoogleScholarHelp"),
    FETCHER_GVK("GVKHelp"),
    FETCHER_IEEEXPLORE("IEEEXploreHelp"),
    FETCHER_INSPIRE("INSPIRE"),
    FETCHER_ISBN_TO_BIBTEX("ISBNtoBibTeXHelp"),
    FETCHER_MEDLINE("MedlineHelp"),
    FETCHER_OAI2_ARXIV("arXivHelp"),
    FETCHER_SPRINGER("SpringerHelp"),
    FETCHER_SCIENCEDIRECT(""),
    FETCHER_BIBSONOMY_SCRAPER(""),
    DATABASE_PROPERTIES("DatabaseProperties"),
    FIND_DUPLICATES("FindDuplicates");

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
