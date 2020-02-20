package org.jabref.gui.actions;

import java.util.Optional;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabaseMode;

public enum StandardActions implements Action {

    COPY_MORE(Localization.lang("Copy") + "..."),
    COPY_TITLE(Localization.lang("Copy title"), KeyBinding.COPY_TITLE),
    COPY_KEY(Localization.lang("Copy BibTeX key"), KeyBinding.COPY_BIBTEX_KEY),
    COPY_CITE_KEY(Localization.lang("Copy \\cite{BibTeX key}"), KeyBinding.COPY_CITE_BIBTEX_KEY),
    COPY_KEY_AND_TITLE(Localization.lang("Copy BibTeX key and title"), KeyBinding.COPY_BIBTEX_KEY_AND_TITLE),
    COPY_KEY_AND_LINK(Localization.lang("Copy BibTeX key and link"), KeyBinding.COPY_BIBTEX_KEY_AND_LINK),
    COPY_CITATION_HTML(Localization.lang("Copy citation") + " (HTML)", KeyBinding.COPY_PREVIEW),
    COPY_CITATION_MORE(Localization.lang("Copy citation") + "..."),
    COPY_CITATION_TEXT("Text"),
    COPY_CITATION_RTF("RTF"),
    COPY_CITATION_ASCII_DOC("AsciiDoc"),
    COPY_CITATION_XSLFO("XSL-FO"),
    COPY_CITATION_PREVIEW(Localization.lang("Copy preview"), KeyBinding.COPY_PREVIEW),
    EXPORT_TO_CLIPBOARD(Localization.lang("Export to clipboard"), IconTheme.JabRefIcons.EXPORT_TO_CLIPBOARD),
    EXPORT_SELECTED_TO_CLIPBOARD(Localization.lang("Export selected entries to clipboard"), IconTheme.JabRefIcons.EXPORT_TO_CLIPBOARD),
    COPY(Localization.lang("Copy"), IconTheme.JabRefIcons.COPY, KeyBinding.COPY),
    PASTE(Localization.lang("Paste"), IconTheme.JabRefIcons.PASTE, KeyBinding.PASTE),
    CUT(Localization.lang("Cut"), IconTheme.JabRefIcons.CUT, KeyBinding.CUT),
    DELETE(Localization.lang("Delete"), IconTheme.JabRefIcons.DELETE_ENTRY),
    DELETE_ENTRY(Localization.lang("Delete Entry"), IconTheme.JabRefIcons.DELETE_ENTRY, KeyBinding.DELETE_ENTRY),
    SEND_AS_EMAIL(Localization.lang("Send as email"), IconTheme.JabRefIcons.EMAIL),
    OPEN_EXTERNAL_FILE(Localization.lang("Open file"), IconTheme.JabRefIcons.FILE, KeyBinding.OPEN_FILE),
    OPEN_URL(Localization.lang("Open URL or DOI"), IconTheme.JabRefIcons.WWW, KeyBinding.OPEN_URL_OR_DOI),
    MERGE_WITH_FETCHED_ENTRY(Localization.lang("Get BibTeX data from %0", "DOI/ISBN/...")),
    ATTACH_FILE(Localization.lang("Attach file"), IconTheme.JabRefIcons.ATTACH_FILE),
    PRIORITY(Localization.lang("Priority"), IconTheme.JabRefIcons.PRIORITY),
    CLEAR_PRIORITY(Localization.lang("Clear priority")),
    PRIORITY_HIGH(Localization.lang("Set priority to high"), IconTheme.JabRefIcons.PRIORITY_HIGH),
    PRIORITY_MEDIUM(Localization.lang("Set priority to medium"), IconTheme.JabRefIcons.PRIORITY_MEDIUM),
    PRIORITY_LOW(Localization.lang("Set priority to low"), IconTheme.JabRefIcons.PRIORITY_LOW),
    QUALITY(Localization.lang("Quality"), IconTheme.JabRefIcons.QUALITY),
    QUALITY_ASSURED(Localization.lang("Toggle quality assured"), IconTheme.JabRefIcons.QUALITY_ASSURED),
    RANKING(Localization.lang("Rank"), IconTheme.JabRefIcons.RANKING),
    CLEAR_RANK(Localization.lang("Clear rank")),
    RANK_1(Localization.lang("Set rank to one"), IconTheme.JabRefIcons.RANK1),
    RANK_2(Localization.lang("Set rank to two"), IconTheme.JabRefIcons.RANK2),
    RANK_3(Localization.lang("Set rank to three"), IconTheme.JabRefIcons.RANK3),
    RANK_4(Localization.lang("Set rank to four"), IconTheme.JabRefIcons.RANK4),
    RANK_5(Localization.lang("Set rank to five"), IconTheme.JabRefIcons.RANK5),
    PRINTED(Localization.lang("Printed"), IconTheme.JabRefIcons.PRINTED),
    TOGGLE_PRINTED(Localization.lang("Toggle print status"), IconTheme.JabRefIcons.PRINTED),
    READ_STATUS(Localization.lang("Read status"), IconTheme.JabRefIcons.READ_STATUS),
    CLEAR_READ_STATUS(Localization.lang("Clear read status")),
    READ(Localization.lang("Set read status to read"), IconTheme.JabRefIcons.READ_STATUS_READ),
    SKIMMED(Localization.lang("Set read status to skimmed"), IconTheme.JabRefIcons.READ_STATUS_SKIMMED),
    RELEVANCE(Localization.lang("Relevance"), IconTheme.JabRefIcons.RELEVANCE),
    RELEVANT(Localization.lang("Toggle relevance"), IconTheme.JabRefIcons.RELEVANCE),
    NEW_LIBRARY(Localization.lang("New library"), IconTheme.JabRefIcons.NEW),
    NEW_LIBRARY_BIBTEX(Localization.lang("New %0 library", BibDatabaseMode.BIBTEX.getFormattedName()), IconTheme.JabRefIcons.NEW),
    NEW_LIBRARY_BIBLATEX(Localization.lang("New %0 library", BibDatabaseMode.BIBLATEX.getFormattedName()), IconTheme.JabRefIcons.NEW),
    OPEN_LIBRARY(Localization.lang("Open library"), IconTheme.JabRefIcons.OPEN, KeyBinding.OPEN_DATABASE),
    IMPORT(Localization.lang("Import"), IconTheme.JabRefIcons.IMPORT),
    EXPORT(Localization.lang("Export"), IconTheme.JabRefIcons.EXPORT, KeyBinding.EXPORT),
    MERGE_DATABASE(Localization.lang("Append library"), Localization.lang("Append contents from a BibTeX library into the currently viewed library")),
    SAVE_LIBRARY(Localization.lang("Save library"), IconTheme.JabRefIcons.SAVE, KeyBinding.SAVE_DATABASE),
    SAVE_LIBRARY_AS(Localization.lang("Save library as..."), KeyBinding.SAVE_DATABASE_AS),
    SAVE_SELECTED_AS_PLAIN_BIBTEX(Localization.lang("Save selected as plain BibTeX...")),
    SAVE_ALL(Localization.lang("Save all"), Localization.lang("Save all open libraries"), IconTheme.JabRefIcons.SAVE_ALL, KeyBinding.SAVE_ALL),
    IMPORT_INTO_NEW_LIBRARY(Localization.lang("Import into new library"), KeyBinding.IMPORT_INTO_NEW_DATABASE),
    IMPORT_INTO_CURRENT_LIBRARY(Localization.lang("Import into current library"), KeyBinding.IMPORT_INTO_CURRENT_DATABASE),
    EXPORT_ALL(Localization.lang("Export all entries")),
    EXPORT_SELECTED(Localization.lang("Export selected entries"), KeyBinding.EXPORT_SELECTED),
    CONNECT_TO_SHARED_DB(Localization.lang("Connect to shared database"), IconTheme.JabRefIcons.CONNECT_DB),
    PULL_CHANGES_FROM_SHARED_DB(Localization.lang("Pull changes from shared database"), KeyBinding.PULL_CHANGES_FROM_SHARED_DATABASE),
    CLOSE_LIBRARY(Localization.lang("Close library"), Localization.lang("Close the current library"), IconTheme.JabRefIcons.CLOSE, KeyBinding.CLOSE_DATABASE),
    QUIT(Localization.lang("Quit"), Localization.lang("Quit JabRef"), IconTheme.JabRefIcons.CLOSE_JABREF, KeyBinding.QUIT_JABREF),
    UNDO(Localization.lang("Undo"), IconTheme.JabRefIcons.UNDO, KeyBinding.UNDO),
    REDO(Localization.lang("Redo"), IconTheme.JabRefIcons.REDO, KeyBinding.REDO),
    REPLACE_ALL(Localization.lang("Find and replace"), KeyBinding.REPLACE_STRING),
    MANAGE_KEYWORDS(Localization.lang("Manage keywords")),
    MASS_SET_FIELDS(Localization.lang("Manage field names & content")),
    TOGGLE_GROUPS(Localization.lang("Groups interface"), IconTheme.JabRefIcons.TOGGLE_GROUPS, KeyBinding.TOGGLE_GROUPS_INTERFACE),
    TOOGLE_OO(Localization.lang("OpenOffice/LibreOffice"), IconTheme.JabRefIcons.FILE_OPENOFFICE, KeyBinding.OPEN_OPEN_OFFICE_LIBRE_OFFICE_CONNECTION),
    TOGGLE_WEB_SEARCH(Localization.lang("Web search"), Localization.lang("Toggle web search interface"), IconTheme.JabRefIcons.WWW, KeyBinding.WEB_SEARCH),

    PARSE_TEX(Localization.lang("Search for Citations in LaTeX Files"), IconTheme.JabRefIcons.LATEX_CITATIONS),
    NEW_SUB_LIBRARY_FROM_AUX(Localization.lang("New sublibrary based on AUX file") + "...", Localization.lang("New BibTeX sublibrary") + Localization.lang("This feature generates a new library based on which entries are needed in an existing LaTeX document."), IconTheme.JabRefIcons.NEW),
    WRITE_XMP(Localization.lang("Write XMP-metadata to PDFs"), Localization.lang("Will write XMP-metadata to the PDFs linked from selected entries."), KeyBinding.WRITE_XMP),
    OPEN_FOLDER(Localization.lang("Open folder"), Localization.lang("Open folder"), KeyBinding.OPEN_FOLDER),
    OPEN_FILE(Localization.lang("Open file"), Localization.lang("Open file"), IconTheme.JabRefIcons.FILE, KeyBinding.OPEN_FILE),
    OPEN_CONSOLE(Localization.lang("Open terminal here"), Localization.lang("Open terminal here"), IconTheme.JabRefIcons.CONSOLE, KeyBinding.OPEN_CONSOLE),
    COPY_LINKED_FILES(Localization.lang("Copy linked files to folder...")),
    COPY_DOI(Localization.lang("Copy DOI url")),
    ABBREVIATE(Localization.lang("Abbreviate journal names")),
    ABBREVIATE_DEFAULT("DEFAULT", Localization.lang("Abbreviate journal names of the selected entries (DEFAULT abbreviation)"), KeyBinding.ABBREVIATE),
    ABBREVIATE_MEDLINE("MEDLINE", Localization.lang("Abbreviate journal names of the selected entries (MEDLINE abbreviation)")),
    ABBREVIATE_SHORTEST_UNIQUE("SHORTEST UNIQUE", Localization.lang("Abbreviate journal names of the selected entries (SHORTEST UNIQUE abbreviation)")),
    UNABBREVIATE(Localization.lang("Unabbreviate journal names"), Localization.lang("Unabbreviate journal names of the selected entries"), KeyBinding.UNABBREVIATE),

    MANAGE_CUSTOM_EXPORTS(Localization.lang("Manage custom exports")),
    MANAGE_CUSTOM_IMPORTS(Localization.lang("Manage custom imports")),
    CUSTOMIZE_ENTRY_TYPES(Localization.lang("Customize entry types")),
    SETUP_GENERAL_FIELDS(Localization.lang("Set up general fields")),
    MANAGE_EXTERNAL_FILETYPES(Localization.lang("Manage external file types")),
    MANAGE_PROTECTED_TERMS(Localization.lang("Manage protected terms")),
    BIBTEX_KEY_PATTERN(Localization.lang("BibTeX key patterns")),
    SHOW_PREFS(Localization.lang("Preferences")),
    MANAGE_JOURNALS(Localization.lang("Manage journal abbreviations")),
    CUSTOMIZE_KEYBINDING(Localization.lang("Customize key bindings"), IconTheme.JabRefIcons.KEY_BINDINGS),
    MANAGE_CONTENT_SELECTORS(Localization.lang("Manage content selectors"), IconTheme.JabRefIcons.PREFERENCES),
    MANAGE_CITE_KEY_PATTERNS(Localization.lang("BibTeX key patterns")),

    EDIT_ENTRY(Localization.lang("Open entry editor"), IconTheme.JabRefIcons.EDIT_ENTRY, KeyBinding.EDIT_ENTRY),
    SHOW_PDF_VIEWER(Localization.lang("Open document viewer"), IconTheme.JabRefIcons.PDF_FILE),
    NEXT_PREVIEW_STYLE(Localization.lang("Next citation style"), KeyBinding.NEXT_PREVIEW_LAYOUT),
    PREVIOUS_PREVIEW_STYLE(Localization.lang("Previous citation style"), KeyBinding.PREVIOUS_PREVIEW_LAYOUT),
    SELECT_ALL(Localization.lang("Select all"), KeyBinding.SELECT_ALL),

    NEW_ENTRY(Localization.lang("New entry"), IconTheme.JabRefIcons.ADD_ENTRY, KeyBinding.NEW_ENTRY),
    NEW_ARTICLE(Localization.lang("New article"), IconTheme.JabRefIcons.ADD_ARTICLE),
    NEW_ENTRY_FROM_PLAIN_TEXT(Localization.lang("New entry from plain text"), IconTheme.JabRefIcons.NEW_ENTRY_FROM_PLAIN_TEXT, KeyBinding.NEW_ENTRY_FROM_PLAIN_TEXT),
    LIBRARY_PROPERTIES(Localization.lang("Library properties")),
    EDIT_PREAMBLE(Localization.lang("Edit preamble")),
    EDIT_STRINGS(Localization.lang("Edit string constants"), IconTheme.JabRefIcons.EDIT_STRINGS, KeyBinding.EDIT_STRINGS),

    FIND_DUPLICATES(Localization.lang("Find duplicates"), IconTheme.JabRefIcons.FIND_DUPLICATES),
    MERGE_ENTRIES(Localization.lang("Merge entries"), IconTheme.JabRefIcons.MERGE_ENTRIES),
    RESOLVE_DUPLICATE_KEYS(Localization.lang("Resolve duplicate BibTeX keys"), Localization.lang("Find and remove duplicate BibTeX keys"), KeyBinding.RESOLVE_DUPLICATE_BIBTEX_KEYS),
    CHECK_INTEGRITY(Localization.lang("Check integrity"), KeyBinding.CHECK_INTEGRITY),
    FIND_UNLINKED_FILES(Localization.lang("Search for unlinked local files"), IconTheme.JabRefIcons.SEARCH, KeyBinding.FIND_UNLINKED_FILES),
    AUTO_LINK_FILES(Localization.lang("Automatically set file links"), IconTheme.JabRefIcons.AUTO_FILE_LINK, KeyBinding.AUTOMATICALLY_LINK_FILES),
    LOOKUP_DOC_IDENTIFIER(Localization.lang("Search document identifier online")),
    LOOKUP_FULLTEXT(Localization.lang("Search full text documents online"), IconTheme.JabRefIcons.FILE_SEARCH, KeyBinding.DOWNLOAD_FULL_TEXT),
    GENERATE_CITE_KEY(Localization.lang("Generate BibTeX key"), IconTheme.JabRefIcons.MAKE_KEY, KeyBinding.AUTOGENERATE_BIBTEX_KEYS),
    GENERATE_CITE_KEYS(Localization.lang("Generate BibTeX keys"), IconTheme.JabRefIcons.MAKE_KEY, KeyBinding.AUTOGENERATE_BIBTEX_KEYS),
    DOWNLOAD_FULL_TEXT(Localization.lang("Search full text documents online"), IconTheme.JabRefIcons.FILE_SEARCH, KeyBinding.DOWNLOAD_FULL_TEXT),
    CLEANUP_ENTRIES(Localization.lang("Cleanup entries"), IconTheme.JabRefIcons.CLEANUP_ENTRIES, KeyBinding.CLEANUP),
    SET_FILE_LINKS(Localization.lang("Automatically set file links"), KeyBinding.AUTOMATICALLY_LINK_FILES),

    HELP(Localization.lang("Online help"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_KEY_PATTERNS(Localization.lang("Help on key patterns"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_REGEX_SEARCH(Localization.lang("Help on regular expression search"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_NAME_FORMATTER(Localization.lang("Help on Name Formatting"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_SPECIAL_FIELDS(Localization.lang("Help on special fields"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    WEB_MENU(Localization.lang("JabRef resources")),
    OPEN_WEBPAGE(Localization.lang("Website"), Localization.lang("Opens JabRef's website")),
    OPEN_FACEBOOK("Facebook", Localization.lang("Opens JabRef's Facebook page"), IconTheme.JabRefIcons.FACEBOOK),
    OPEN_TWITTER("Twitter", Localization.lang("Opens JabRef's Twitter page"), IconTheme.JabRefIcons.TWITTER),
    OPEN_BLOG(Localization.lang("Blog"), Localization.lang("Opens JabRef's blog"), IconTheme.JabRefIcons.BLOG),
    OPEN_DEV_VERSION_LINK(Localization.lang("Development version"), Localization.lang("Opens a link where the current development version can be downloaded")),
    OPEN_CHANGELOG(Localization.lang("View change log"), Localization.lang("See what has been changed in the JabRef versions")),
    FORK_ME(Localization.lang("Fork me on GitHub"), Localization.lang("Opens JabRef's GitHub page"), IconTheme.JabRefIcons.GITHUB),
    DONATE(Localization.lang("Donate to JabRef"), Localization.lang("Donate to JabRef"), IconTheme.JabRefIcons.DONATE),
    OPEN_FORUM(Localization.lang("Online help forum"), Localization.lang("Online help forum"), IconTheme.JabRefIcons.FORUM),
    ERROR_CONSOLE(Localization.lang("View event log"), Localization.lang("Display all error messages")),
    SEARCH_FOR_UPDATES(Localization.lang("Check for updates")),
    ABOUT(Localization.lang("About JabRef"), Localization.lang("About JabRef"));


    private final String text;
    private final String description;
    private final Optional<JabRefIcon> icon;
    private final Optional<KeyBinding> keyBinding;

    StandardActions(String text) {
        this(text, "");
    }

    StandardActions(String text, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.description = "";
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    StandardActions(String text, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.description = "";
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    StandardActions(String text, String description, IconTheme.JabRefIcons icon) {
        this.text = text;
        this.description = description;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.empty();
    }

    StandardActions(String text, String description, IconTheme.JabRefIcons icon, KeyBinding keyBinding) {
        this.text = text;
        this.description = description;
        this.icon = Optional.of(icon);
        this.keyBinding = Optional.of(keyBinding);
    }

    StandardActions(String text, KeyBinding keyBinding) {
        this.text = text;
        this.description = "";
        this.keyBinding = Optional.of(keyBinding);
        this.icon = Optional.empty();
    }

    StandardActions(String text, String description) {
        this.text = text;
        this.description = description;
        this.icon = Optional.empty();
        this.keyBinding = Optional.empty();
    }

    StandardActions(String text, String description, KeyBinding keyBinding) {
        this.text = text;
        this.description = description;
        this.icon = Optional.empty();
        this.keyBinding = Optional.of(keyBinding);
    }

    @Override
    public Optional<JabRefIcon> getIcon() {
        return icon;
    }

    @Override
    public Optional<KeyBinding> getKeyBinding() {
        return keyBinding;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
