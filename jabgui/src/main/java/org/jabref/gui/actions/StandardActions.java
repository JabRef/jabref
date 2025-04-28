package org.jabref.gui.actions;

import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.icon.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.logic.l10n.Localization;

public enum StandardActions implements Action {

    COPY_TO(Localization.lang("Copy to")),
    COPY_MORE(Localization.lang("Copy") + "..."),
    COPY_TITLE(Localization.lang("Copy title"), KeyBinding.COPY_TITLE),
    COPY_KEY(Localization.lang("Copy citation key"), KeyBinding.COPY_CITATION_KEY),
    COPY_CITE_KEY(Localization.lang("Copy citation key with configured cite command"), KeyBinding.COPY_CITE_CITATION_KEY),
    COPY_KEY_AND_TITLE(Localization.lang("Copy citation key and title"), KeyBinding.COPY_CITATION_KEY_AND_TITLE),
    COPY_KEY_AND_LINK(Localization.lang("Copy citation key and link"), KeyBinding.COPY_CITATION_KEY_AND_LINK),
    COPY_CITATION_HTML(Localization.lang("Copy citation (html)"), KeyBinding.COPY_PREVIEW),
    COPY_CITATION_TEXT(Localization.lang("Copy citation (text)")),
    COPY_CITATION_PREVIEW(Localization.lang("Copy preview"), KeyBinding.COPY_PREVIEW),
    EXPORT_TO_CLIPBOARD(Localization.lang("Export to clipboard"), IconTheme.JabRefIcons.EXPORT_TO_CLIPBOARD),
    EXPORT_SELECTED_TO_CLIPBOARD(Localization.lang("Export selected entries to clipboard"), IconTheme.JabRefIcons.EXPORT_TO_CLIPBOARD),
    COPY(Localization.lang("Copy"), IconTheme.JabRefIcons.COPY, KeyBinding.COPY),
    PASTE(Localization.lang("Paste"), IconTheme.JabRefIcons.PASTE, KeyBinding.PASTE),
    CUT(Localization.lang("Cut"), IconTheme.JabRefIcons.CUT, KeyBinding.CUT),
    DELETE(Localization.lang("Delete"), IconTheme.JabRefIcons.DELETE_ENTRY),
    DELETE_ENTRY(Localization.lang("Delete entry"), IconTheme.JabRefIcons.DELETE_ENTRY, KeyBinding.DELETE_ENTRY),
    SEND(Localization.lang("Send"), IconTheme.JabRefIcons.EMAIL),
    SEND_AS_EMAIL(Localization.lang("As Email")),
    SEND_TO_KINDLE(Localization.lang("To Kindle")),
    REBUILD_FULLTEXT_SEARCH_INDEX(Localization.lang("Rebuild fulltext search index"), IconTheme.JabRefIcons.FILE),
    REDOWNLOAD_MISSING_FILES(Localization.lang("Redownload missing files"), IconTheme.JabRefIcons.DOWNLOAD),
    OPEN_EXTERNAL_FILE(Localization.lang("Open file"), IconTheme.JabRefIcons.FILE, KeyBinding.OPEN_FILE),
    EXTRACT_FILE_REFERENCES_ONLINE(Localization.lang("Extract references from file (online)"), IconTheme.JabRefIcons.FILE_STAR),
    EXTRACT_FILE_REFERENCES_OFFLINE(Localization.lang("Extract references from file (offline)"), IconTheme.JabRefIcons.FILE_STAR),
    OPEN_URL(Localization.lang("Open URL or DOI"), IconTheme.JabRefIcons.WWW, KeyBinding.OPEN_URL_OR_DOI),
    SEARCH_SHORTSCIENCE(Localization.lang("Search ShortScience")),
    MERGE_WITH_FETCHED_ENTRY(Localization.lang("Get bibliographic data from %0", "DOI/ISBN/..."), KeyBinding.MERGE_WITH_FETCHED_ENTRY),
    ATTACH_FILE(Localization.lang("Attach file"), IconTheme.JabRefIcons.ATTACH_FILE),
    ATTACH_FILE_FROM_URL(Localization.lang("Attach file from URL"), IconTheme.JabRefIcons.DOWNLOAD_FILE),
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
    CLEAR_READ_STATUS(Localization.lang("Clear read status"), KeyBinding.CLEAR_READ_STATUS),
    READ(Localization.lang("Set read status to read"), IconTheme.JabRefIcons.READ_STATUS_READ, KeyBinding.READ),
    SKIMMED(Localization.lang("Set read status to skimmed"), IconTheme.JabRefIcons.READ_STATUS_SKIMMED, KeyBinding.SKIMMED),
    RELEVANCE(Localization.lang("Relevance"), IconTheme.JabRefIcons.RELEVANCE),
    RELEVANT(Localization.lang("Toggle relevance"), IconTheme.JabRefIcons.RELEVANCE),
    NEW_LIBRARY(Localization.lang("New library"), IconTheme.JabRefIcons.NEW),
    OPEN_LIBRARY(Localization.lang("Open library"), IconTheme.JabRefIcons.OPEN, KeyBinding.OPEN_DATABASE),
    IMPORT(Localization.lang("Import"), IconTheme.JabRefIcons.IMPORT),
    EXPORT(Localization.lang("Export"), IconTheme.JabRefIcons.EXPORT, KeyBinding.EXPORT),
    SAVE_LIBRARY(Localization.lang("Save library"), IconTheme.JabRefIcons.SAVE, KeyBinding.SAVE_DATABASE),
    SAVE_LIBRARY_AS(Localization.lang("Save library as..."), KeyBinding.SAVE_DATABASE_AS),
    SAVE_SELECTED_AS_PLAIN_BIBTEX(Localization.lang("Save selected as plain BibTeX...")),
    SAVE_ALL(Localization.lang("Save all"), Localization.lang("Save all open libraries"), IconTheme.JabRefIcons.SAVE_ALL, KeyBinding.SAVE_ALL),
    IMPORT_INTO_NEW_LIBRARY(Localization.lang("Import into new library"), KeyBinding.IMPORT_INTO_NEW_DATABASE),
    IMPORT_INTO_CURRENT_LIBRARY(Localization.lang("Import into current library"), KeyBinding.IMPORT_INTO_CURRENT_DATABASE),
    EXPORT_ALL(Localization.lang("Export all entries")),
    REMOTE_DB(Localization.lang("Shared database"), IconTheme.JabRefIcons.REMOTE_DATABASE),
    EXPORT_SELECTED(Localization.lang("Export selected entries"), KeyBinding.EXPORT_SELECTED),
    CONNECT_TO_SHARED_DB(Localization.lang("Connect to shared database"), IconTheme.JabRefIcons.CONNECT_DB),
    PULL_CHANGES_FROM_SHARED_DB(Localization.lang("Pull changes from shared database"), KeyBinding.PULL_CHANGES_FROM_SHARED_DATABASE),
    CLOSE_LIBRARY(Localization.lang("Close library"), Localization.lang("Close the current library"), IconTheme.JabRefIcons.CLOSE, KeyBinding.CLOSE_DATABASE),
    CLOSE_OTHER_LIBRARIES(Localization.lang("Close others"), Localization.lang("Close other libraries"), IconTheme.JabRefIcons.CLOSE),
    CLOSE_ALL_LIBRARIES(Localization.lang("Close all"), Localization.lang("Close all libraries"), IconTheme.JabRefIcons.CLOSE),
    QUIT(Localization.lang("Quit"), Localization.lang("Quit JabRef"), IconTheme.JabRefIcons.CLOSE_JABREF, KeyBinding.QUIT_JABREF),
    UNDO(Localization.lang("Undo"), IconTheme.JabRefIcons.UNDO, KeyBinding.UNDO),
    REDO(Localization.lang("Redo"), IconTheme.JabRefIcons.REDO, KeyBinding.REDO),
    REPLACE_ALL(Localization.lang("Find and replace"), KeyBinding.REPLACE_STRING),
    MANAGE_KEYWORDS(Localization.lang("Manage keywords")),
    MASS_SET_FIELDS(Localization.lang("Manage field names & content")),

    AUTOMATIC_FIELD_EDITOR(Localization.lang("Automatic field editor")),
    TOGGLE_GROUPS(Localization.lang("Groups"), IconTheme.JabRefIcons.TOGGLE_GROUPS, KeyBinding.TOGGLE_GROUPS_INTERFACE),
    TOGGLE_OO(Localization.lang("OpenOffice/LibreOffice"), IconTheme.JabRefIcons.FILE_OPENOFFICE, KeyBinding.OPEN_OPEN_OFFICE_LIBRE_OFFICE_CONNECTION),
    TOGGLE_WEB_SEARCH(Localization.lang("Web search"), Localization.lang("Toggle web search interface"), IconTheme.JabRefIcons.WWW, KeyBinding.WEB_SEARCH),

    PARSE_LATEX(Localization.lang("Search for citations in LaTeX files..."), IconTheme.JabRefIcons.LATEX_CITATIONS),
    NEW_SUB_LIBRARY_FROM_AUX(Localization.lang("New sublibrary based on AUX file") + "...", Localization.lang("New BibTeX sublibrary") + Localization.lang("This feature generates a new library based on which entries are needed in an existing LaTeX document."), IconTheme.JabRefIcons.NEW),
    NEW_LIBRARY_FROM_PDF_ONLINE(Localization.lang("New library based on references in PDF file... (online)"), Localization.lang("This feature generates a new library based on the list of references in a PDF file. Thereby, it uses Grobid's functionality."), IconTheme.JabRefIcons.NEW),
    NEW_LIBRARY_FROM_PDF_OFFLINE(Localization.lang("New library based on references in PDF file... (offline)"), Localization.lang("This feature generates a new library based on the list of references in a PDF file. Thereby, it uses JabRef's built-in functionality."), IconTheme.JabRefIcons.NEW),
    WRITE_METADATA_TO_PDF(Localization.lang("Write metadata to PDF files"), Localization.lang("Will write metadata to the PDFs linked from selected entries."), KeyBinding.WRITE_METADATA_TO_PDF),

    START_NEW_STUDY(Localization.lang("Start new systematic literature review")),
    UPDATE_SEARCH_RESULTS_OF_STUDY(Localization.lang("Update study search results")),
    EDIT_EXISTING_STUDY(Localization.lang("Manage study definition")),

    OPEN_DATABASE_FOLDER(Localization.lang("Reveal in file explorer")),
    OPEN_FOLDER(Localization.lang("Open folder"), Localization.lang("Open folder"), IconTheme.JabRefIcons.FOLDER, KeyBinding.OPEN_FOLDER),
    OPEN_FILE(Localization.lang("Open file"), Localization.lang("Open file"), IconTheme.JabRefIcons.FILE, KeyBinding.OPEN_FILE),
    OPEN_CONSOLE(Localization.lang("Open terminal here"), Localization.lang("Open terminal here"), IconTheme.JabRefIcons.CONSOLE, KeyBinding.OPEN_CONSOLE),
    COPY_LINKED_FILES(Localization.lang("Copy linked files to folder...")),
    COPY_DOI(Localization.lang("Copy DOI")),
    COPY_DOI_URL(Localization.lang("Copy DOI url")),
    ABBREVIATE(Localization.lang("Abbreviate journal names")),
    ABBREVIATE_DEFAULT(Localization.lang("default"), Localization.lang("Abbreviate journal names of the selected entries (DEFAULT abbreviation)"), KeyBinding.ABBREVIATE),
    ABBREVIATE_DOTLESS(Localization.lang("dotless"), Localization.lang("Abbreviate journal names of the selected entries (DOTLESS abbreviation)")),
    ABBREVIATE_SHORTEST_UNIQUE(Localization.lang("shortest unique"), Localization.lang("Abbreviate journal names of the selected entries (SHORTEST UNIQUE abbreviation)")),
    ABBREVIATE_LTWA(Localization.lang("LTWA"), Localization.lang("Abbreviate journal names of the selected entries (LTWA)")),
    UNABBREVIATE(Localization.lang("Unabbreviate journal names"), Localization.lang("Unabbreviate journal names of the selected entries"), KeyBinding.UNABBREVIATE),

    MANAGE_CUSTOM_EXPORTS(Localization.lang("Manage custom exports")),
    MANAGE_CUSTOM_IMPORTS(Localization.lang("Manage custom imports")),
    CUSTOMIZE_ENTRY_TYPES(Localization.lang("Customize entry types")),
    SETUP_GENERAL_FIELDS(Localization.lang("Set up general fields")),
    MANAGE_PROTECTED_TERMS(Localization.lang("Manage protected terms")),
    CITATION_KEY_PATTERN(Localization.lang("Citation key patterns")),
    SHOW_PREFS(Localization.lang("Preferences"), IconTheme.JabRefIcons.PREFERENCES, KeyBinding.SHOW_PREFS),
    MANAGE_JOURNALS(Localization.lang("Manage journal abbreviations")),
    CUSTOMIZE_KEYBINDING(Localization.lang("Customize keyboard shortcuts"), IconTheme.JabRefIcons.KEY_BINDINGS),
    EDIT_ENTRY(Localization.lang("Open entry editor"), IconTheme.JabRefIcons.EDIT_ENTRY, KeyBinding.OPEN_CLOSE_ENTRY_EDITOR),
    SHOW_PDF_VIEWER(Localization.lang("Open document viewer"), IconTheme.JabRefIcons.PDF_FILE),
    NEXT_PREVIEW_STYLE(Localization.lang("Next preview style"), KeyBinding.NEXT_PREVIEW_LAYOUT),
    PREVIOUS_PREVIEW_STYLE(Localization.lang("Previous preview style"), KeyBinding.PREVIOUS_PREVIEW_LAYOUT),
    SELECT_ALL(Localization.lang("Select all"), KeyBinding.SELECT_ALL),
    UNSELECT_ALL(Localization.lang("Unselect all")),

    EXPAND_ALL(Localization.lang("Expand all")),
    COLLAPSE_ALL(Localization.lang("Collapse all")),

    NEW_INSTANT_ENTRY(Localization.lang("Add empty entry"), IconTheme.JabRefIcons.ADD_ARTICLE),
    NEW_ENTRY_UNIFIED(Localization.lang("Create new entry"), IconTheme.JabRefIcons.ADD_ENTRY, KeyBinding.NEW_ENTRY),
    NEW_ENTRY_PLAINTEXT(Localization.lang("Interpret citations"), IconTheme.JabRefIcons.NEW_ENTRY_PLAINTEXT, KeyBinding.NEW_ENTRY_PLAINTEXT),

    LIBRARY_PROPERTIES(Localization.lang("Library properties")),
    FIND_DUPLICATES(Localization.lang("Find duplicates"), IconTheme.JabRefIcons.FIND_DUPLICATES),
    MERGE_ENTRIES(Localization.lang("Merge entries"), IconTheme.JabRefIcons.MERGE_ENTRIES, KeyBinding.MERGE_ENTRIES),
    RESOLVE_DUPLICATE_KEYS(Localization.lang("Resolve duplicate citation keys"), Localization.lang("Find and remove duplicate citation keys"), KeyBinding.RESOLVE_DUPLICATE_CITATION_KEYS),
    CHECK_INTEGRITY(Localization.lang("Check integrity"), KeyBinding.CHECK_INTEGRITY),
    CHECK_CONSISTENCY(Localization.lang("Check consistency"), KeyBinding.CHECK_CONSISTENCY),
    FIND_UNLINKED_FILES(Localization.lang("Search for unlinked local files"), IconTheme.JabRefIcons.SEARCH, KeyBinding.FIND_UNLINKED_FILES),
    AUTO_LINK_FILES(Localization.lang("Automatically set file links"), IconTheme.JabRefIcons.AUTO_FILE_LINK, KeyBinding.AUTOMATICALLY_LINK_FILES),
    LOOKUP_DOC_IDENTIFIER(Localization.lang("Search document identifier online"), KeyBinding.LOOKUP_DOC_IDENTIFIER),
    LOOKUP_FULLTEXT(Localization.lang("Search full text documents online"), IconTheme.JabRefIcons.FILE_SEARCH, KeyBinding.DOWNLOAD_FULL_TEXT),
    GENERATE_CITE_KEY(Localization.lang("Generate citation key"), IconTheme.JabRefIcons.MAKE_KEY, KeyBinding.AUTOGENERATE_CITATION_KEYS),
    GENERATE_CITE_KEYS(Localization.lang("Generate citation keys"), IconTheme.JabRefIcons.MAKE_KEY, KeyBinding.AUTOGENERATE_CITATION_KEYS),
    DOWNLOAD_FULL_TEXT(Localization.lang("Search full text documents online"), IconTheme.JabRefIcons.FILE_SEARCH, KeyBinding.DOWNLOAD_FULL_TEXT),
    CLEANUP_ENTRIES(Localization.lang("Clean up entries"), IconTheme.JabRefIcons.CLEANUP_ENTRIES, KeyBinding.CLEANUP),
    SET_FILE_LINKS(Localization.lang("Automatically set file links"), KeyBinding.AUTOMATICALLY_LINK_FILES),

    EDIT_FILE_LINK(Localization.lang("Edit"), IconTheme.JabRefIcons.EDIT, KeyBinding.OPEN_CLOSE_ENTRY_EDITOR),
    DOWNLOAD_FILE(Localization.lang("Download file"), IconTheme.JabRefIcons.DOWNLOAD_FILE),
    REDOWNLOAD_FILE(Localization.lang("Redownload file"), IconTheme.JabRefIcons.DOWNLOAD_FILE),
    RENAME_FILE_TO_PATTERN(Localization.lang("Rename file to defined pattern"), IconTheme.JabRefIcons.AUTO_RENAME),
    RENAME_FILE_TO_NAME(Localization.lang("Rename files to configured filename format pattern"), IconTheme.JabRefIcons.RENAME, KeyBinding.REPLACE_STRING),
    MOVE_FILE_TO_FOLDER(Localization.lang("Move file to file directory"), IconTheme.JabRefIcons.MOVE_TO_FOLDER),
    MOVE_FILE_TO_FOLDER_AND_RENAME(Localization.lang("Move file to file directory and rename file")),
    COPY_FILE_TO_FOLDER(Localization.lang("Copy linked file to folder..."), IconTheme.JabRefIcons.COPY_TO_FOLDER, KeyBinding.COPY),
    REMOVE_LINK(Localization.lang("Remove link"), IconTheme.JabRefIcons.REMOVE_LINK),
    REMOVE_LINKS(Localization.lang("Remove links"), IconTheme.JabRefIcons.REMOVE_LINK),
    DELETE_FILE(Localization.lang("Permanently delete local file"), IconTheme.JabRefIcons.DELETE_FILE, KeyBinding.DELETE_ENTRY),

    HELP(Localization.lang("Online help"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_GROUPS(Localization.lang("Open Help page"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_KEY_PATTERNS(Localization.lang("Help on key patterns"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_REGEX_SEARCH(Localization.lang("Help on regular expression search"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_NAME_FORMATTER(Localization.lang("Help on Name Formatting"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_SPECIAL_FIELDS(Localization.lang("Help on special fields"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    HELP_PUSH_TO_APPLICATION(Localization.lang("Help on external applications"), IconTheme.JabRefIcons.HELP, KeyBinding.HELP),
    WEB_MENU(Localization.lang("JabRef resources")),
    OPEN_WEBPAGE(Localization.lang("Website"), Localization.lang("Opens JabRef's website"), IconTheme.JabRefIcons.HOME),
    OPEN_FACEBOOK("Facebook", Localization.lang("Opens JabRef's Facebook page"), IconTheme.JabRefIcons.FACEBOOK),
    OPEN_LINKEDIN("LinkedIn", Localization.lang("Opens JabRef's LinkedIn page"), IconTheme.JabRefIcons.LINKEDIN),
    OPEN_MASTODON("Mastodon", Localization.lang("Opens JabRef's Mastodon page"), IconTheme.JabRefIcons.MASTODON),
    OPEN_BLOG(Localization.lang("Blog"), Localization.lang("Opens JabRef's blog"), IconTheme.JabRefIcons.BLOG),
    OPEN_DEV_VERSION_LINK(Localization.lang("Development version"), Localization.lang("Opens a link where the current development version can be downloaded")),
    OPEN_CHANGELOG(Localization.lang("View change log"), Localization.lang("See what has been changed in the JabRef versions")),
    OPEN_GITHUB("GitHub", Localization.lang("Opens JabRef's GitHub page"), IconTheme.JabRefIcons.GITHUB),
    DONATE(Localization.lang("Donate to JabRef"), Localization.lang("Donate to JabRef"), IconTheme.JabRefIcons.DONATE),
    OPEN_FORUM(Localization.lang("Community forum"), Localization.lang("Community forum"), IconTheme.JabRefIcons.FORUM),
    ERROR_CONSOLE(Localization.lang("View event log"), Localization.lang("Display all error messages")),
    SEARCH_FOR_UPDATES(Localization.lang("Check for updates")),
    ABOUT(Localization.lang("About JabRef"), Localization.lang("About JabRef")),
    OPEN_WELCOME_TAB(Localization.lang("Open welcome tab")),

    EDIT_LIST(Localization.lang("Edit"), IconTheme.JabRefIcons.EDIT),
    VIEW_LIST(Localization.lang("View"), IconTheme.JabRefIcons.FILE),
    REMOVE_LIST(Localization.lang("Remove"), IconTheme.JabRefIcons.REMOVE),
    RELOAD_LIST(Localization.lang("Reload"), IconTheme.JabRefIcons.REFRESH),

    GROUP_REMOVE(Localization.lang("Remove group")),
    GROUP_REMOVE_KEEP_SUBGROUPS(Localization.lang("Keep subgroups")),
    GROUP_REMOVE_WITH_SUBGROUPS(Localization.lang("Also remove subgroups")),
    GROUP_CHAT(Localization.lang("Chat with group")),
    GROUP_EDIT(Localization.lang("Edit group")),
    GROUP_GENERATE_SUMMARIES(Localization.lang("Generate summaries for entries in the group")),
    GROUP_GENERATE_EMBEDDINGS(Localization.lang("Generate embeddings for linked files in the group")),
    GROUP_SUBGROUP_ADD(Localization.lang("Add subgroup")),
    GROUP_SUBGROUP_REMOVE(Localization.lang("Remove subgroups")),
    GROUP_SUBGROUP_SORT(Localization.lang("Sort subgroups A-Z")),
    GROUP_SUBGROUP_SORT_REVERSE(Localization.lang("Sort subgroups Z-A")),
    GROUP_SUBGROUP_SORT_ENTRIES(Localization.lang("Sort subgroups by # of entries (Descending)")),
    GROUP_SUBGROUP_SORT_ENTRIES_REVERSE(Localization.lang("Sort subgroups by # of entries (Ascending)")),
    GROUP_ENTRIES_ADD(Localization.lang("Add selected entries to this group")),
    GROUP_SUBGROUP_RENAME(Localization.lang("Rename subgroup"), KeyBinding.GROUP_SUBGROUP_RENAME),
    GROUP_ENTRIES_REMOVE(Localization.lang("Remove selected entries from this group")),

    CLEAR_EMBEDDINGS_CACHE(Localization.lang("Clear embeddings cache"));

    private String text;
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

    public Action withText(String text) {
        this.text = Objects.requireNonNull(text);
        return this;
    }
}
