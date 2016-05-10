/*  Copyright (C) 2003-2016 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref.gui.help;

/**
 * This enum globally defines all help pages with the name of the markdown file in the help repository at github
 * @see <a href=https://github.com/JabRef/help.jabref.org>help.jabref.org@github</a>
 *
 *
 */
public enum HelpFiles {
    COMMAND_LINE(
            ""),

    //Empty because it refers to the TOC/index
    CONTENTS(
            ""),
    ENTRY_EDITOR(
            "EntryEditorHelp"),
    STRING_EDITOR(
            "StringEditorHelp"),
    SEARCH(
            "SearchHelp"),
    GROUP(
            "GroupsHelp"),
    CONTENT_SELECTOR(
            "ContentSelectorHelp"),
    SPECIAL_FIELDS(
            "specialFieldsHelp"),
    LABEL_PATTERN(
            "labelPatternHelp"),
    OWNER(
            "OwnerHelp"),
    TIMESTAMP(
            "TimeStampHelp"),
    CUSTOM_EXPORTS(
            "CustomExports"),
    CUSTOM_EXPORTS_NAME_FORMATTER(
            "CustomExports#NameFormatter"),
    CUSTOM_IMPORTS(
            "CustomImports"),
    GENERAL_FIELDS(
            "GeneralFields"),
    IMPORT_INSPECTION(
            "ImportInspectionDialog"),
    REMOTE(
            "RemoteHelp"),
    JOURNAL_ABBREV(
            "JournalAbbreviations"),
    REGEX_SEARCH(
            "ExternalFiles#RegularExpressionSearch"),
    PREVIEW(
            "PreviewHelp"),
    AUTOSAVE(
            "Autosave"),

    //The help page covers both OO and LO.
    OPENOFFICE_LIBREOFFICE(
            "OpenOfficeIntegration"),

    FETCHER_ACM(
            "ACMPortalHelp"),

    FETCHER_ADS(
            "ADSHelp"),

    FETCHER_CITESEERX(
            "CiteSeerHelp"),

    FETCHER_DBLP(
            "DBLPHelp"),

    FETCHER_DIVA_TO_BIBTEX(
            "DiVAtoBibTeXHelp"),

    FETCHER_DOAJ(
            "DOAJHelp"),

    FETCHER_DOI_TO_BIBTEX(
            "DOItoBibTeXHelp"),

    FETCHER_GOOGLE_SCHOLAR(
            "GoogleScholarHelp"),

    FETCHER_GVK(
            "GVKHelp"),

    FETCHER_IEEEXPLORE(
            "IEEEXploreHelp"),

    FETCHER_INSPIRE(
            "INSPIRE"),

    FETCHER_ISBN_TO_BIBTEX(
            "ISBNtoBibTeXHelp"),
    FETCHER_MEDLINE(
            "MedlineHelp"),

    FETCHER_OAI2_ARXIV(
            "arXivHelp"),

    FETCHER_SPRINGER(
            "SpringerHelp"),

    FETCHER_SCIENCEDIRECT(
            ""),

    FETCHER_BIBSONOMY_SCRAPER(
            "");

    /**
     *
     * @param pageName The filename of the help page
     */
    HelpFiles(String pageName) {
        this.pageName = pageName;
    }


    private final String pageName;


    /**
     * Get the filename of a help page
     * @return The filename of the associated help page
     */
    public String getPageName() {
        return pageName;
    }

}
