package org.jabref.gui.preferences;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.scene.control.TableColumn;

import org.jabref.gui.CoreGuiPreferences;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.edit.CopyToPreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.externalfiles.UnlinkedFilesDialogPreferences;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.frame.SidePanePreferences;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.keyboard.KeyBindingRepository;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.maintable.MainTablePreferences;
import org.jabref.gui.maintable.NameDisplayPreferences;
import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.push.PushToApplicationPreferences;
import org.jabref.gui.push.PushToApplications;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.theme.Theme;
import org.jabref.logic.bst.BstPreviewLayout;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.AutoCompleteFirstNameMode;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.util.StandardFileType;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;
import org.jabref.model.strings.StringUtil;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefGuiPreferences extends JabRefCliPreferences implements GuiPreferences {

    // Public because needed for pref migration
    public static final String AUTOCOMPLETER_COMPLETE_FIELDS = "autoCompleteFields";
    public static final String MAIN_FONT_SIZE = "mainFontSize";

    // region Preview - public for pref migrations
    public static final String PREVIEW_STYLE = "previewStyle";
    public static final String CYCLE_PREVIEW_POS = "cyclePreviewPos";
    public static final String CYCLE_PREVIEW = "cyclePreview";
    public static final String PREVIEW_AS_TAB = "previewAsTab";
    public static final String PREVIEW_IN_ENTRY_TABLE_TOOLTIP = "previewInEntryTableTooltip";
    public static final String PREVIEW_BST_LAYOUT_PATHS = "previewBstLayoutPaths";
    // endregion

    // region column names
    // public because of migration
    // Variable names have changed to ensure backward compatibility with pre 5.0 releases of JabRef
    // Pre 5.1: columnNames, columnWidths, columnSortTypes, columnSortOrder
    public static final String COLUMN_NAMES = "mainTableColumnNames";
    public static final String COLUMN_WIDTHS = "mainTableColumnWidths";
    public static final String COLUMN_SORT_TYPES = "mainTableColumnSortTypes";
    public static final String COLUMN_SORT_ORDER = "mainTableColumnSortOrder";
    // endregion

    // region keybindings - public because needed for pref migration
    public static final String BIND_NAMES = "bindNames";
    public static final String BINDINGS = "bindings";
    // endregion

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefGuiPreferences.class);

    // region core GUI preferences
    private static final String MAIN_WINDOW_POS_X = "mainWindowPosX";
    private static final String MAIN_WINDOW_POS_Y = "mainWindowPosY";
    private static final String MAIN_WINDOW_WIDTH = "mainWindowSizeX";
    private static final String MAIN_WINDOW_HEIGHT = "mainWindowSizeY";
    private static final String WINDOW_MAXIMISED = "windowMaximised";
    private static final String SIDE_PANE_WIDTH = "sidePaneWidthFX";
    private static final String SIDE_PANE_COMPONENT_PREFERRED_POSITIONS = "sidePaneComponentPreferredPositions";
    private static final String SIDE_PANE_COMPONENT_NAMES = "sidePaneComponentNames";
    // endregion

    // region main table, main table columns, save columns
    private static final String AUTO_RESIZE_MODE = "autoResizeMode";
    private static final String EXTRA_FILE_COLUMNS = "extraFileColumns";

    private static final String SEARCH_DIALOG_COLUMN_WIDTHS = "searchTableColumnWidths";
    private static final String SEARCH_DIALOG_COLUMN_SORT_TYPES = "searchDialogColumnSortTypes";
    private static final String SEARCH_DIALOG_COLUMN_SORT_ORDER = "searchDalogColumnSortOrder";
    // endregion

    // region Push to application preferences
    private static final String PUSH_TO_APPLICATION = "pushToApplication";
    private static final String PUSH_EMACS_PATH = "emacsPath";
    private static final String PUSH_EMACS_ADDITIONAL_PARAMETERS = "emacsParameters";
    private static final String PUSH_LYXPIPE = "lyxpipe";
    private static final String PUSH_TEXSTUDIO_PATH = "TeXstudioPath";
    private static final String PUSH_TEXWORKS_PATH = "TeXworksPath";
    private static final String PUSH_WINEDT_PATH = "winEdtPath";
    private static final String PUSH_TEXMAKER_PATH = "texmakerPath";
    private static final String PUSH_VIM_SERVER = "vimServer";
    private static final String PUSH_VIM = "vim";
    private static final String PUSH_SUBLIME_TEXT_PATH = "sublimeTextPath";
    private static final String PUSH_VSCODE_PATH = "VScodePath";
    // endregion

    // region NameDisplayPreferences
    private static final String NAMES_LAST_ONLY = "namesLastOnly";
    private static final String ABBR_AUTHOR_NAMES = "abbrAuthorNames";
    private static final String NAMES_NATBIB = "namesNatbib";
    private static final String NAMES_FIRST_LAST = "namesFf";
    private static final String NAMES_AS_IS = "namesAsIs";
    // endregion

    // region ExternalApplicationsPreferences
    private static final String EXTERNAL_FILE_TYPES = "externalFileTypes";
    private static final String CONSOLE_COMMAND = "consoleCommand";
    private static final String USE_DEFAULT_CONSOLE_APPLICATION = "useDefaultConsoleApplication";
    private static final String USE_DEFAULT_FILE_BROWSER_APPLICATION = "userDefaultFileBrowserApplication";
    private static final String CITE_COMMAND = "citeCommand";
    private static final String EMAIL_SUBJECT = "emailSubject";
    private static final String KINDLE_EMAIL = "kindleEmail";
    private static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    private static final String FILE_BROWSER_COMMAND = "fileBrowserCommand";
    // endregion

    // region workspace
    private static final String THEME = "fxTheme";
    private static final String THEME_SYNC_OS = "themeSyncOs";
    private static final String OPEN_LAST_EDITED = "openLastEdited";
    private static final String OVERRIDE_DEFAULT_FONT_SIZE = "overrideDefaultFontSize";
    private static final String SHOW_ADVANCED_HINTS = "showAdvancedHints";
    private static final String CONFIRM_DELETE = "confirmDelete";
    private static final String CONFIRM_HIDE_TAB_BAR = "confirmHideTabBar";
    // endregion

    private static final String ENTRY_EDITOR_HEIGHT = "entryEditorHeightFX";

    /**
     * Holds the horizontal divider position of the preview view when it is shown inside the entry editor
     */
    private static final String ENTRY_EDITOR_PREVIEW_DIVIDER_POS = "entryEditorPreviewDividerPos";

    private static final String JOURNAL_POPUP = "journalPopup";

    // region Auto completion
    private static final String AUTO_COMPLETE = "autoComplete";
    private static final String AUTOCOMPLETER_FIRSTNAME_MODE = "autoCompFirstNameMode";
    private static final String AUTOCOMPLETER_LAST_FIRST = "autoCompLF";
    private static final String AUTOCOMPLETER_FIRST_LAST = "autoCompFF";
    // endregion

    // region SidePanePreferences
    private static final String SELECTED_FETCHER_INDEX = "selectedFetcherIndex";
    private static final String WEB_SEARCH_VISIBLE = "webSearchVisible";
    private static final String OO_SHOW_PANEL = "showOOPanel";
    private static final String GROUP_SIDEPANE_VISIBLE = "groupSidepaneVisible";
    // endregion

    // region GroupsPreferences
    private static final String AUTO_ASSIGN_GROUP = "autoAssignGroup";
    private static final String DISPLAY_GROUP_COUNT = "displayGroupCount";
    private static final String GROUP_VIEW_INTERSECTION = "groupIntersection";
    private static final String GROUP_VIEW_FILTER = "groupFilter";
    private static final String GROUP_VIEW_INVERT = "groupInvert";
    private static final String DEFAULT_HIERARCHICAL_CONTEXT = "defaultHierarchicalContext";
    // endregion

    // region specialFieldsPreferences
    private static final String SPECIALFIELDSENABLED = "specialFieldsEnabled";
    // endregion

    private static final String ID_ENTRY_GENERATOR = "idEntryGenerator";
    private static final String SELECTED_SLR_CATALOGS = "selectedSlrCatalogs";
    private static final String UNLINKED_FILES_SELECTED_EXTENSION = "unlinkedFilesSelectedExtension";
    private static final String UNLINKED_FILES_SELECTED_DATE_RANGE = "unlinkedFilesSelectedDateRange";
    private static final String UNLINKED_FILES_SELECTED_SORT = "unlinkedFilesSelectedSort";

    private static final String INCLUDE_CROSS_REFERENCES = "includeCrossReferences";
    private static final String ASK_FOR_INCLUDING_CROSS_REFERENCES = "askForIncludingCrossReferences";

    private static JabRefGuiPreferences singleton;

    private EntryEditorPreferences entryEditorPreferences;
    private MergeDialogPreferences mergeDialogPreferences;
    private AutoCompletePreferences autoCompletePreferences;
    private CoreGuiPreferences coreGuiPreferences;
    private WorkspacePreferences workspacePreferences;
    private UnlinkedFilesDialogPreferences unlinkedFilesDialogPreferences;
    private ExternalApplicationsPreferences externalApplicationsPreferences;
    private SidePanePreferences sidePanePreferences;
    private GroupsPreferences groupsPreferences;
    private SpecialFieldsPreferences specialFieldsPreferences;
    private PreviewPreferences previewPreferences;
    private PushToApplicationPreferences pushToApplicationPreferences;
    private NameDisplayPreferences nameDisplayPreferences;
    private MainTablePreferences mainTablePreferences;
    private ColumnPreferences mainTableColumnPreferences;
    private ColumnPreferences searchDialogColumnPreferences;
    private KeyBindingRepository keyBindingRepository;
    private CopyToPreferences copyToPreferences;

    private JabRefGuiPreferences() {
        super();

        defaults.put(JOURNAL_POPUP, EntryEditorPreferences.JournalPopupEnabled.FIRST_START.toString());

        defaults.put(ENTRY_EDITOR_HEIGHT, 0.65);
        defaults.put(ENTRY_EDITOR_PREVIEW_DIVIDER_POS, 0.5);

        // region mergeDialogPreferences
        defaults.put(MERGE_ENTRIES_DIFF_MODE, DiffMode.WORD.name());
        defaults.put(MERGE_ENTRIES_SHOULD_SHOW_DIFF, Boolean.TRUE);
        defaults.put(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, Boolean.TRUE);
        defaults.put(MERGE_ENTRIES_HIGHLIGHT_WORDS, Boolean.TRUE);
        defaults.put(MERGE_SHOW_ONLY_CHANGED_FIELDS, Boolean.FALSE);
        defaults.put(MERGE_APPLY_TO_ALL_ENTRIES, Boolean.FALSE);
        defaults.put(DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES, DuplicateResolverDialog.DuplicateResolverResult.BREAK.name());
        // endregion

        // region autoCompletePreferences
        defaults.put(AUTO_COMPLETE, Boolean.FALSE);
        defaults.put(AUTOCOMPLETER_FIRSTNAME_MODE, AutoCompleteFirstNameMode.BOTH.name());
        defaults.put(AUTOCOMPLETER_FIRST_LAST, Boolean.FALSE); // "Autocomplete names in 'Firstname Lastname' format only"
        defaults.put(AUTOCOMPLETER_LAST_FIRST, Boolean.FALSE); // "Autocomplete names in 'Lastname, Firstname' format only"
        defaults.put(AUTOCOMPLETER_COMPLETE_FIELDS, "author;editor;title;journal;publisher;keywords;crossref;related;entryset");
        // endregion

        // region workspace
        defaults.put(MAIN_FONT_SIZE, 9);
        defaults.put(OVERRIDE_DEFAULT_FONT_SIZE, false);
        defaults.put(OPEN_LAST_EDITED, Boolean.TRUE);
        defaults.put(THEME, Theme.BASE_CSS);
        defaults.put(THEME_SYNC_OS, Boolean.FALSE);
        defaults.put(CONFIRM_DELETE, Boolean.TRUE);
        defaults.put(CONFIRM_HIDE_TAB_BAR, Boolean.TRUE);
        defaults.put(SHOW_ADVANCED_HINTS, Boolean.TRUE);
        // endregion

        // region unlinkedFilesDialogPreferences
        defaults.put(UNLINKED_FILES_SELECTED_EXTENSION, StandardFileType.ANY_FILE.getName());
        defaults.put(UNLINKED_FILES_SELECTED_DATE_RANGE, DateRange.ALL_TIME.name());
        defaults.put(UNLINKED_FILES_SELECTED_SORT, ExternalFileSorter.DEFAULT.name());
        // endregion

        // region ExternalApplicationsPreferences
        defaults.put(EXTERNAL_FILE_TYPES, "");
        defaults.put(CITE_COMMAND, "\\cite{key1,key2}");
        defaults.put(EMAIL_SUBJECT, Localization.lang("References"));
        defaults.put(KINDLE_EMAIL, "");

        if (OS.WINDOWS) {
            defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.TRUE);
        } else {
            defaults.put(OPEN_FOLDERS_OF_ATTACHED_FILES, Boolean.FALSE);
        }

        defaults.put(USE_DEFAULT_CONSOLE_APPLICATION, Boolean.TRUE);
        defaults.put(USE_DEFAULT_FILE_BROWSER_APPLICATION, Boolean.TRUE);
        if (OS.WINDOWS) {
            defaults.put(CONSOLE_COMMAND, "C:\\Program Files\\ConEmu\\ConEmu64.exe /single /dir \"%DIR\"");
            defaults.put(FILE_BROWSER_COMMAND, "explorer.exe /select, \"%DIR\"");
        } else {
            defaults.put(CONSOLE_COMMAND, "");
            defaults.put(FILE_BROWSER_COMMAND, "");
        }
        // endregion

        // region SidePanePreferences
        defaults.put(WEB_SEARCH_VISIBLE, Boolean.TRUE);
        defaults.put(SELECTED_FETCHER_INDEX, 0);
        defaults.put(GROUP_SIDEPANE_VISIBLE, Boolean.TRUE);
        defaults.put(OO_SHOW_PANEL, Boolean.FALSE);
        // endregion

        // region GroupsPreferences
        defaults.put(AUTO_ASSIGN_GROUP, Boolean.TRUE);
        defaults.put(DISPLAY_GROUP_COUNT, Boolean.TRUE);
        defaults.put(GROUP_VIEW_INTERSECTION, Boolean.TRUE);
        defaults.put(GROUP_VIEW_FILTER, Boolean.TRUE);
        defaults.put(GROUP_VIEW_INVERT, Boolean.FALSE);
        defaults.put(DEFAULT_HIERARCHICAL_CONTEXT, GroupHierarchyType.INDEPENDENT.name());
        // endregion

        defaults.put(SPECIALFIELDSENABLED, Boolean.TRUE);

        // region PreviewStyle
        defaults.put(CYCLE_PREVIEW, "Preview;" + CitationStyle.DEFAULT);
        defaults.put(CYCLE_PREVIEW_POS, 0);
        defaults.put(PREVIEW_AS_TAB, Boolean.FALSE);
        defaults.put(PREVIEW_IN_ENTRY_TABLE_TOOLTIP, Boolean.FALSE);
        defaults.put(PREVIEW_STYLE,
                "<font face=\"sans-serif\">" +
                        "<b>\\bibtextype</b><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>\\end{citationkey}__NEWLINE__" +
                        "\\begin{author}<BR><BR>\\format[Authors(LastFirst, FullName,Sep= / ,LastSep= / ),HTMLChars]{\\author}\\end{author}__NEWLINE__" +
                        "\\begin{editor & !author}<BR><BR>\\format[Authors(LastFirst,FullName,Sep= / ,LastSep= / ),HTMLChars]{\\editor} (\\format[IfPlural(Eds.,Ed.)]{\\editor})\\end{editor & !author}__NEWLINE__" +
                        "\\begin{title}<BR><b>\\format[HTMLChars]{\\title}</b> \\end{title}__NEWLINE__" +
                        "<BR>\\begin{date}\\date\\end{date}\\begin{edition}, \\edition. edition\\end{edition}__NEWLINE__" +
                        "\\begin{editor & author}<BR><BR>\\format[Authors(LastFirst,FullName,Sep= / ,LastSep= / ),HTMLChars]{\\editor} (\\format[IfPlural(Eds.,Ed.)]{\\editor})\\end{editor & author}__NEWLINE__" +
                        "\\begin{booktitle}<BR><i>\\format[HTMLChars]{\\booktitle}</i>\\end{booktitle}__NEWLINE__" +
                        "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}" +
                        "\\begin{editor & !author}<BR>\\end{editor & !author}\\begin{!editor}<BR>\\end{!editor}\\begin{journal}<BR><i>\\format[HTMLChars]{\\journal}</i> \\end{journal} \\begin{volume}, Vol. \\volume\\end{volume}\\begin{series}<BR>\\format[HTMLChars]{\\series}\\end{series}\\begin{number}, No. \\format[HTMLChars]{\\number}\\end{number}__NEWLINE__" +
                        "\\begin{school} \\format[HTMLChars]{\\school}, \\end{school}__NEWLINE__" +
                        "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__" +
                        "\\begin{publisher}<BR>\\format[HTMLChars]{\\publisher}\\end{publisher}\\begin{location}: \\format[HTMLChars]{\\location} \\end{location}__NEWLINE__" +
                        "\\begin{pages}<BR> p. \\format[FormatPagesForHTML]{\\pages}\\end{pages}__NEWLINE__" +
                        "\\begin{abstract}<BR><BR><b>Abstract: </b>\\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__" +
                        "\\begin{owncitation}<BR><BR><b>Own citation: </b>\\format[HTMLChars]{\\owncitation} \\end{owncitation}__NEWLINE__" +
                        "\\begin{comment}<BR><BR><b>Comment: </b>\\format[Markdown,HTMLChars(keepCurlyBraces)]{\\comment}\\end{comment}__NEWLINE__" +
                        "</font>__NEWLINE__");
        // endregion

        // region NameDisplayPreferences
        defaults.put(NAMES_AS_IS, Boolean.FALSE); // "Show names unchanged"
        defaults.put(NAMES_FIRST_LAST, Boolean.FALSE); // "Show 'Firstname Lastname'"
        defaults.put(NAMES_NATBIB, Boolean.TRUE); // "Natbib style"
        defaults.put(ABBR_AUTHOR_NAMES, Boolean.TRUE); // "Abbreviate names"
        defaults.put(NAMES_LAST_ONLY, Boolean.TRUE); // "Show last names only"
        // endregion

        // region PushToApplicationPreferences
        defaults.put(PUSH_TEXMAKER_PATH, OS.detectProgramPath("texmaker", "Texmaker"));
        defaults.put(PUSH_WINEDT_PATH, OS.detectProgramPath("WinEdt", "WinEdt Team\\WinEdt"));
        defaults.put(PUSH_TO_APPLICATION, "TeXstudio");
        defaults.put(PUSH_TEXSTUDIO_PATH, OS.detectProgramPath("texstudio", "TeXstudio"));
        defaults.put(PUSH_TEXWORKS_PATH, OS.detectProgramPath("texworks", "TeXworks"));
        defaults.put(PUSH_SUBLIME_TEXT_PATH, OS.detectProgramPath("subl", "Sublime"));
        defaults.put(PUSH_LYXPIPE, USER_HOME + File.separator + ".lyx/lyxpipe");
        defaults.put(PUSH_VIM, "vim");
        defaults.put(PUSH_VIM_SERVER, "vim");
        defaults.put(PUSH_EMACS_ADDITIONAL_PARAMETERS, "-n -e");
        defaults.put(PUSH_VSCODE_PATH, OS.detectProgramPath("Code", "Microsoft VS Code"));

        if (OS.OS_X) {
            defaults.put(PUSH_EMACS_PATH, "emacsclient");
        } else if (OS.WINDOWS) {
            defaults.put(PUSH_EMACS_PATH, "emacsclient.exe");
        } else {
            // Linux
            defaults.put(PUSH_EMACS_PATH, "emacsclient");
        }
        // endregion

        // region: Main table, main table column, and search dialog column preferences
        defaults.put(EXTRA_FILE_COLUMNS, Boolean.FALSE);
        defaults.put(COLUMN_NAMES, "groups;group_icons;files;linked_id;field:citationkey;field:entrytype;field:author/editor;field:title;field:year;field:journal/booktitle;special:ranking;special:readstatus;special:priority");
        defaults.put(COLUMN_WIDTHS, "28;40;28;28;100;75;300;470;60;130;50;50;50");

        defaults.put(SIDE_PANE_COMPONENT_NAMES, "");
        defaults.put(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, "");
        defaults.put(SIDE_PANE_WIDTH, 0.15);
        // endregion

        // region core GUI preferences
        // Set DOI to be the default ID entry generator
        defaults.put(ID_ENTRY_GENERATOR, DoiFetcher.NAME);
        defaults.put(MAIN_WINDOW_POS_X, 0);
        defaults.put(MAIN_WINDOW_POS_Y, 0);
        defaults.put(MAIN_WINDOW_WIDTH, 1024);
        defaults.put(MAIN_WINDOW_HEIGHT, 768);
        defaults.put(WINDOW_MAXIMISED, Boolean.TRUE);
        // By default disable "Fit table horizontally on the screen"
        defaults.put(AUTO_RESIZE_MODE, Boolean.FALSE);
        // endregion

        defaults.put(ASK_FOR_INCLUDING_CROSS_REFERENCES, Boolean.TRUE);
        defaults.put(INCLUDE_CROSS_REFERENCES, Boolean.FALSE);
    }

    /**
     * @deprecated Never ever add a call to this method. There should be only one caller.
     *             All other usages should get the preferences passed (or injected).
     *             The JabRef team leaves the <code>@deprecated</code> annotation to have IntelliJ listing this method with a strike-through.
     */
    @Deprecated
    public static JabRefGuiPreferences getInstance() {
        if (JabRefGuiPreferences.singleton == null) {
            JabRefGuiPreferences.singleton = new JabRefGuiPreferences();
        }
        return JabRefGuiPreferences.singleton;
    }

    public CopyToPreferences getCopyToPreferences() {
        if (copyToPreferences != null) {
            return copyToPreferences;
        }
        copyToPreferences = new CopyToPreferences(
                getBoolean(ASK_FOR_INCLUDING_CROSS_REFERENCES),
                getBoolean(INCLUDE_CROSS_REFERENCES)
        );
        return copyToPreferences;
    }

    // region EntryEditorPreferences
    public EntryEditorPreferences getEntryEditorPreferences() {
        if (entryEditorPreferences != null) {
            return entryEditorPreferences;
        }

        entryEditorPreferences = new EntryEditorPreferences(
                getEntryEditorTabs(),
                getDefaultEntryEditorTabs(),
                getBoolean(AUTO_OPEN_FORM),
                getBoolean(SHOW_RECOMMENDATIONS),
                getBoolean(SHOW_AI_SUMMARY),
                getBoolean(SHOW_AI_CHAT),
                getBoolean(SHOW_LATEX_CITATIONS),
                getBoolean(DEFAULT_SHOW_SOURCE),
                getBoolean(VALIDATE_IN_ENTRY_EDITOR),
                getBoolean(ALLOW_INTEGER_EDITION_BIBTEX),
                getDouble(ENTRY_EDITOR_HEIGHT),
                getBoolean(AUTOLINK_FILES_ENABLED),
                EntryEditorPreferences.JournalPopupEnabled.fromString(get(JOURNAL_POPUP)),
                getBoolean(SHOW_SCITE_TAB),
                getBoolean(SHOW_USER_COMMENTS_FIELDS),
                getDouble(ENTRY_EDITOR_PREVIEW_DIVIDER_POS));

        EasyBind.listen(entryEditorPreferences.entryEditorTabs(), (obs, oldValue, newValue) -> storeEntryEditorTabs(newValue));
        // defaultEntryEditorTabs are read-only
        EasyBind.listen(entryEditorPreferences.shouldOpenOnNewEntryProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_OPEN_FORM, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowRecommendationsTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_RECOMMENDATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowAiSummaryTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_AI_SUMMARY, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowAiChatTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_AI_CHAT, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowLatexCitationsTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_LATEX_CITATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.showSourceTabByDefaultProperty(), (obs, oldValue, newValue) -> putBoolean(DEFAULT_SHOW_SOURCE, newValue));
        EasyBind.listen(entryEditorPreferences.enableValidationProperty(), (obs, oldValue, newValue) -> putBoolean(VALIDATE_IN_ENTRY_EDITOR, newValue));
        EasyBind.listen(entryEditorPreferences.allowIntegerEditionBibtexProperty(), (obs, oldValue, newValue) -> putBoolean(ALLOW_INTEGER_EDITION_BIBTEX, newValue));
        EasyBind.listen(entryEditorPreferences.dividerPositionProperty(), (obs, oldValue, newValue) -> putDouble(ENTRY_EDITOR_HEIGHT, newValue.doubleValue()));
        EasyBind.listen(entryEditorPreferences.autoLinkEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(AUTOLINK_FILES_ENABLED, newValue));
        EasyBind.listen(entryEditorPreferences.enableJournalPopupProperty(), (obs, oldValue, newValue) -> put(JOURNAL_POPUP, newValue.toString()));
        EasyBind.listen(entryEditorPreferences.shouldShowLSciteTabProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_SCITE_TAB, newValue));
        EasyBind.listen(entryEditorPreferences.showUserCommentsFieldsProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_USER_COMMENTS_FIELDS, newValue));
        EasyBind.listen(entryEditorPreferences.previewWidthDividerPositionProperty(), (obs, oldValue, newValue) -> putDouble(ENTRY_EDITOR_PREVIEW_DIVIDER_POS, newValue.doubleValue()));
        return entryEditorPreferences;
    }

    /**
     * Get a Map of defined tab names to default tab fields.
     *
     * @return A map of the currently defined tabs in the entry editor from scratch to cache
     */
    private Map<String, Set<Field>> getEntryEditorTabs() {
        Map<String, Set<Field>> tabs = new LinkedHashMap<>();
        List<String> tabNames = getSeries(CUSTOM_TAB_NAME);
        List<String> tabFields = getSeries(CUSTOM_TAB_FIELDS);

        if (tabNames.isEmpty() || (tabNames.size() != tabFields.size())) {
            // Nothing set (or wrong configuration), then we use default values
            tabNames = getSeries(CUSTOM_TAB_NAME + "_def");
            tabFields = getSeries(CUSTOM_TAB_FIELDS + "_def");
        }

        for (int i = 0; i < tabNames.size(); i++) {
            tabs.put(tabNames.get(i), FieldFactory.parseFieldList(tabFields.get(i)));
        }
        return tabs;
    }

    /**
     * Stores the defined tabs and corresponding fields in the preferences.
     *
     * @param customTabs a map of tab names and the corresponding set of fields to be displayed in
     */
    private void storeEntryEditorTabs(Map<String, Set<Field>> customTabs) {
        String[] names = customTabs.keySet().toArray(String[]::new);
        String[] fields = customTabs.values().stream()
                                    .map(set -> set.stream()
                                                   .map(Field::getName)
                                                   .collect(Collectors.joining(STRINGLIST_DELIMITER.toString())))
                                    .toArray(String[]::new);

        for (int i = 0; i < customTabs.size(); i++) {
            put(CUSTOM_TAB_NAME + i, names[i]);
            put(CUSTOM_TAB_FIELDS + i, fields[i]);
        }

        purgeSeries(CUSTOM_TAB_NAME, customTabs.size());
        purgeSeries(CUSTOM_TAB_FIELDS, customTabs.size());

        getEntryEditorTabs();
    }

    private SequencedMap<String, Set<Field>> getDefaultEntryEditorTabs() {
        SequencedMap<String, Set<Field>> customTabsMap = new LinkedHashMap<>();

        int defNumber = 0;
        while (true) {
            // Saved as 'CUSTOMTABNAME_def{number}' and seperated by ';'
            String name = (String) defaults.get(CUSTOM_TAB_NAME + "_def" + defNumber);
            String fields = (String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber);

            if (StringUtil.isNullOrEmpty(name) || StringUtil.isNullOrEmpty(fields)) {
                break;
            }

            customTabsMap.put(name, FieldFactory.parseFieldList((String) defaults.get(CUSTOM_TAB_FIELDS + "_def" + defNumber)));
            defNumber++;
        }
        return customTabsMap;
    }
    // endregion

    @Override
    public MergeDialogPreferences getMergeDialogPreferences() {
        if (mergeDialogPreferences != null) {
            return mergeDialogPreferences;
        }

        mergeDialogPreferences = new MergeDialogPreferences(
                DiffMode.parse(get(MERGE_ENTRIES_DIFF_MODE)),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF),
                getBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS),
                getBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS),
                getBoolean(MERGE_APPLY_TO_ALL_ENTRIES),
                DuplicateResolverDialog.DuplicateResolverResult.parse(get(DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES))
        );

        EasyBind.listen(mergeDialogPreferences.mergeDiffModeProperty(), (obs, oldValue, newValue) -> put(MERGE_ENTRIES_DIFF_MODE, newValue.name()));
        EasyBind.listen(mergeDialogPreferences.mergeShouldShowDiffProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeShouldShowUnifiedDiffProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeHighlightWordsProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeShowChangedFieldOnlyProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeApplyToAllEntriesProperty(), (obs, oldValue, newValue) -> putBoolean(MERGE_APPLY_TO_ALL_ENTRIES, newValue));
        EasyBind.listen(mergeDialogPreferences.allEntriesDuplicateResolverDecisionProperty(), (obs, oldValue, newValue) -> put(DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES, newValue.name()));

        return mergeDialogPreferences;
    }

    @Override
    public AutoCompletePreferences getAutoCompletePreferences() {
        if (autoCompletePreferences != null) {
            return autoCompletePreferences;
        }

        AutoCompletePreferences.NameFormat nameFormat = AutoCompletePreferences.NameFormat.BOTH;
        if (getBoolean(AUTOCOMPLETER_LAST_FIRST)) {
            nameFormat = AutoCompletePreferences.NameFormat.LAST_FIRST;
        } else if (getBoolean(AUTOCOMPLETER_FIRST_LAST)) {
            nameFormat = AutoCompletePreferences.NameFormat.FIRST_LAST;
        }

        autoCompletePreferences = new AutoCompletePreferences(
                getBoolean(AUTO_COMPLETE),
                AutoCompleteFirstNameMode.parse(get(AUTOCOMPLETER_FIRSTNAME_MODE)),
                nameFormat,
                getStringList(AUTOCOMPLETER_COMPLETE_FIELDS).stream().map(FieldFactory::parseField).collect(Collectors.toSet())
        );

        EasyBind.listen(autoCompletePreferences.autoCompleteProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_COMPLETE, newValue));
        EasyBind.listen(autoCompletePreferences.firstNameModeProperty(), (obs, oldValue, newValue) -> put(AUTOCOMPLETER_FIRSTNAME_MODE, newValue.name()));
        autoCompletePreferences.getCompleteFields().addListener((SetChangeListener<Field>) c ->
                putStringList(AUTOCOMPLETER_COMPLETE_FIELDS, autoCompletePreferences.getCompleteFields().stream()
                                                                                    .map(Field::getName)
                                                                                    .collect(Collectors.toList())));
        EasyBind.listen(autoCompletePreferences.nameFormatProperty(), (obs, oldValue, newValue) -> {
            if (autoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.BOTH) {
                putBoolean(AUTOCOMPLETER_LAST_FIRST, false);
                putBoolean(AUTOCOMPLETER_FIRST_LAST, false);
            } else if (autoCompletePreferences.getNameFormat() == AutoCompletePreferences.NameFormat.LAST_FIRST) {
                putBoolean(AUTOCOMPLETER_LAST_FIRST, true);
                putBoolean(AUTOCOMPLETER_FIRST_LAST, false);
            } else {
                putBoolean(AUTOCOMPLETER_LAST_FIRST, false);
                putBoolean(AUTOCOMPLETER_FIRST_LAST, true);
            }
        });

        return autoCompletePreferences;
    }

    // region (core) GUI preferences
    public CoreGuiPreferences getGuiPreferences() {
        if (coreGuiPreferences != null) {
            return coreGuiPreferences;
        }

        coreGuiPreferences = new CoreGuiPreferences(
                getDouble(MAIN_WINDOW_POS_X),
                getDouble(MAIN_WINDOW_POS_Y),
                getDouble(MAIN_WINDOW_WIDTH),
                getDouble(MAIN_WINDOW_HEIGHT),
                getBoolean(WINDOW_MAXIMISED),
                get(ID_ENTRY_GENERATOR),
                getDouble(SIDE_PANE_WIDTH));

        EasyBind.listen(coreGuiPreferences.positionXProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_POS_X, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.positionYProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_POS_Y, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.sizeXProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_WIDTH, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.sizeYProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_HEIGHT, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.windowMaximisedProperty(), (obs, oldValue, newValue) -> putBoolean(WINDOW_MAXIMISED, newValue));
        EasyBind.listen(coreGuiPreferences.sidePaneWidthProperty(), (obs, oldValue, newValue) -> putDouble(SIDE_PANE_WIDTH, newValue.doubleValue()));

        EasyBind.listen(coreGuiPreferences.lastSelectedIdBasedFetcherProperty(), (obs, oldValue, newValue) -> put(ID_ENTRY_GENERATOR, newValue));

        return coreGuiPreferences;
    }

    // endregion

    @Override
    public WorkspacePreferences getWorkspacePreferences() {
        if (workspacePreferences != null) {
            return workspacePreferences;
        }

        workspacePreferences = new WorkspacePreferences(
                getLanguage(),
                getBoolean(OVERRIDE_DEFAULT_FONT_SIZE),
                getInt(MAIN_FONT_SIZE),
                (Integer) defaults.get(MAIN_FONT_SIZE),
                new Theme(get(THEME)),
                getBoolean(THEME_SYNC_OS),
                getBoolean(OPEN_LAST_EDITED),
                getBoolean(SHOW_ADVANCED_HINTS),
                getBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION),
                getBoolean(CONFIRM_DELETE),
                getBoolean(CONFIRM_HIDE_TAB_BAR),
                getStringList(SELECTED_SLR_CATALOGS));

        EasyBind.listen(workspacePreferences.languageProperty(), (obs, oldValue, newValue) -> {
            put(LANGUAGE, newValue.getId());
            if (oldValue != newValue) {
                setLanguageDependentDefaultValues();
                Localization.setLanguage(newValue);
            }
        });

        EasyBind.listen(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), (obs, oldValue, newValue) -> putBoolean(OVERRIDE_DEFAULT_FONT_SIZE, newValue));
        EasyBind.listen(workspacePreferences.mainFontSizeProperty(), (obs, oldValue, newValue) -> putInt(MAIN_FONT_SIZE, newValue));
        EasyBind.listen(workspacePreferences.themeProperty(), (obs, oldValue, newValue) -> put(THEME, newValue.getName()));
        EasyBind.listen(workspacePreferences.themeSyncOsProperty(), (obs, oldValue, newValue) -> putBoolean(THEME_SYNC_OS, newValue));
        EasyBind.listen(workspacePreferences.openLastEditedProperty(), (obs, oldValue, newValue) -> putBoolean(OPEN_LAST_EDITED, newValue));
        EasyBind.listen(workspacePreferences.showAdvancedHintsProperty(), (obs, oldValue, newValue) -> putBoolean(SHOW_ADVANCED_HINTS, newValue));
        EasyBind.listen(workspacePreferences.warnAboutDuplicatesInInspectionProperty(), (obs, oldValue, newValue) -> putBoolean(WARN_ABOUT_DUPLICATES_IN_INSPECTION, newValue));
        EasyBind.listen(workspacePreferences.confirmDeleteProperty(), (obs, oldValue, newValue) -> putBoolean(CONFIRM_DELETE, newValue));
        EasyBind.listen(workspacePreferences.confirmHideTabBarProperty(), (obs, oldValue, newValue) -> putBoolean(CONFIRM_HIDE_TAB_BAR, newValue));
        workspacePreferences.getSelectedSlrCatalogs().addListener((ListChangeListener<String>) change ->
                putStringList(SELECTED_SLR_CATALOGS, workspacePreferences.getSelectedSlrCatalogs()));
        return workspacePreferences;
    }

    @Override
    public UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferences() {
        if (unlinkedFilesDialogPreferences != null) {
            return unlinkedFilesDialogPreferences;
        }

        unlinkedFilesDialogPreferences = new UnlinkedFilesDialogPreferences(
                get(UNLINKED_FILES_SELECTED_EXTENSION),
                DateRange.parse(get(UNLINKED_FILES_SELECTED_DATE_RANGE)),
                ExternalFileSorter.parse(get(UNLINKED_FILES_SELECTED_SORT))
        );

        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedExtensionProperty(), (obs, oldValue, newValue) -> put(UNLINKED_FILES_SELECTED_EXTENSION, newValue));
        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedDateRangeProperty(), (obs, oldValue, newValue) -> put(UNLINKED_FILES_SELECTED_DATE_RANGE, newValue.name()));
        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedSortProperty(), (obs, oldValue, newValue) -> put(UNLINKED_FILES_SELECTED_SORT, newValue.name()));

        return unlinkedFilesDialogPreferences;
    }

    // region SidePanePreferences
    @Override
    public SidePanePreferences getSidePanePreferences() {
        if (sidePanePreferences != null) {
            return sidePanePreferences;
        }

        sidePanePreferences = new SidePanePreferences(
                getVisibleSidePanes(),
                getSidePanePreferredPositions(),
                getInt(SELECTED_FETCHER_INDEX));

        sidePanePreferences.visiblePanes().addListener((InvalidationListener) listener ->
                storeVisibleSidePanes(sidePanePreferences.visiblePanes()));
        sidePanePreferences.getPreferredPositions().addListener((InvalidationListener) listener ->
                storeSidePanePreferredPositions(sidePanePreferences.getPreferredPositions()));
        EasyBind.listen(sidePanePreferences.webSearchFetcherSelectedProperty(), (obs, oldValue, newValue) -> putInt(SELECTED_FETCHER_INDEX, newValue));

        return sidePanePreferences;
    }

    private Set<SidePaneType> getVisibleSidePanes() {
        Set<SidePaneType> visiblePanes = new HashSet<>();
        if (getBoolean(WEB_SEARCH_VISIBLE)) {
            visiblePanes.add(SidePaneType.WEB_SEARCH);
        }
        if (getBoolean(GROUP_SIDEPANE_VISIBLE)) {
            visiblePanes.add(SidePaneType.GROUPS);
        }
        if (getBoolean(OO_SHOW_PANEL)) {
            visiblePanes.add(SidePaneType.OPEN_OFFICE);
        }
        return visiblePanes;
    }

    private void storeVisibleSidePanes(Set<SidePaneType> visiblePanes) {
        putBoolean(WEB_SEARCH_VISIBLE, visiblePanes.contains(SidePaneType.WEB_SEARCH));
        putBoolean(GROUP_SIDEPANE_VISIBLE, visiblePanes.contains(SidePaneType.GROUPS));
        putBoolean(OO_SHOW_PANEL, visiblePanes.contains(SidePaneType.OPEN_OFFICE));
    }

    private Map<SidePaneType, Integer> getSidePanePreferredPositions() {
        Map<SidePaneType, Integer> preferredPositions = new HashMap<>();

        List<String> componentNames = getStringList(SIDE_PANE_COMPONENT_NAMES);
        List<String> componentPositions = getStringList(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS);

        for (int i = 0; i < componentNames.size(); ++i) {
            String name = componentNames.get(i);
            try {
                SidePaneType type = Enum.valueOf(SidePaneType.class, name);
                preferredPositions.put(type, Integer.parseInt(componentPositions.get(i)));
            } catch (NumberFormatException e) {
                LOGGER.debug("Invalid number format for side pane component '{}'", name, e);
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Following component is not a side pane: '{}'", name, e);
            }
        }

        return preferredPositions;
    }

    private void storeSidePanePreferredPositions(Map<SidePaneType, Integer> preferredPositions) {
        // Split the map into a pair of parallel String lists suitable for storage
        List<String> names = preferredPositions.keySet().stream()
                                               .map(Enum::toString)
                                               .collect(Collectors.toList());

        List<String> positions = preferredPositions.values().stream()
                                                   .map(integer -> Integer.toString(integer))
                                                   .collect(Collectors.toList());

        putStringList(SIDE_PANE_COMPONENT_NAMES, names);
        putStringList(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS, positions);
    }
    // endregion

    @Override
    public ExternalApplicationsPreferences getExternalApplicationsPreferences() {
        if (externalApplicationsPreferences != null) {
            return externalApplicationsPreferences;
        }

        externalApplicationsPreferences = new ExternalApplicationsPreferences(
                get(EMAIL_SUBJECT),
                getBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES),
                CitationCommandString.from(get(CITE_COMMAND)),
                CitationCommandString.from((String) defaults.get(CITE_COMMAND)),
                ExternalFileTypes.fromString(get(EXTERNAL_FILE_TYPES)),
                !getBoolean(USE_DEFAULT_CONSOLE_APPLICATION), // mind the !
                get(CONSOLE_COMMAND),
                !getBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION), // mind the !
                get(FILE_BROWSER_COMMAND),
                get(KINDLE_EMAIL));

        EasyBind.listen(externalApplicationsPreferences.eMailSubjectProperty(),
                (obs, oldValue, newValue) -> put(EMAIL_SUBJECT, newValue));
        EasyBind.listen(externalApplicationsPreferences.autoOpenEmailAttachmentsFolderProperty(),
                (obs, oldValue, newValue) -> putBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES, newValue));
        EasyBind.listen(externalApplicationsPreferences.citeCommandProperty(),
                (obs, oldValue, newValue) -> put(CITE_COMMAND, newValue.toString()));
        EasyBind.listen(externalApplicationsPreferences.useCustomTerminalProperty(),
                (obs, oldValue, newValue) -> putBoolean(USE_DEFAULT_CONSOLE_APPLICATION, !newValue)); // mind the !
        externalApplicationsPreferences.getExternalFileTypes().addListener((SetChangeListener<ExternalFileType>) c ->
                put(EXTERNAL_FILE_TYPES, ExternalFileTypes.toStringList(externalApplicationsPreferences.getExternalFileTypes())));
        EasyBind.listen(externalApplicationsPreferences.customTerminalCommandProperty(),
                (obs, oldValue, newValue) -> put(CONSOLE_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.useCustomFileBrowserProperty(),
                (obs, oldValue, newValue) -> putBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION, !newValue)); // mind the !
        EasyBind.listen(externalApplicationsPreferences.customFileBrowserCommandProperty(),
                (obs, oldValue, newValue) -> put(FILE_BROWSER_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.kindleEmailProperty(),
                (obs, oldValue, newValue) -> put(KINDLE_EMAIL, newValue));

        return externalApplicationsPreferences;
    }

    public GroupsPreferences getGroupsPreferences() {
        if (groupsPreferences != null) {
            return groupsPreferences;
        }

        groupsPreferences = new GroupsPreferences(
                getBoolean(GROUP_VIEW_INTERSECTION),
                getBoolean(GROUP_VIEW_FILTER),
                getBoolean(GROUP_VIEW_INVERT),
                getBoolean(AUTO_ASSIGN_GROUP),
                getBoolean(DISPLAY_GROUP_COUNT),
                GroupHierarchyType.valueOf(get(DEFAULT_HIERARCHICAL_CONTEXT))
        );

        groupsPreferences.groupViewModeProperty().addListener((SetChangeListener<GroupViewMode>) change -> {
            putBoolean(GROUP_VIEW_INTERSECTION, groupsPreferences.groupViewModeProperty().contains(GroupViewMode.INTERSECTION));
            putBoolean(GROUP_VIEW_FILTER, groupsPreferences.groupViewModeProperty().contains(GroupViewMode.FILTER));
            putBoolean(GROUP_VIEW_INVERT, groupsPreferences.groupViewModeProperty().contains(GroupViewMode.INVERT));
        });
        EasyBind.listen(groupsPreferences.autoAssignGroupProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_ASSIGN_GROUP, newValue));
        EasyBind.listen(groupsPreferences.displayGroupCountProperty(), (obs, oldValue, newValue) -> putBoolean(DISPLAY_GROUP_COUNT, newValue));
        EasyBind.listen(groupsPreferences.defaultHierarchicalContextProperty(), (obs, oldValue, newValue) -> put(DEFAULT_HIERARCHICAL_CONTEXT, newValue.name()));

        return groupsPreferences;
    }

    public SpecialFieldsPreferences getSpecialFieldsPreferences() {
        if (specialFieldsPreferences != null) {
            return specialFieldsPreferences;
        }

        specialFieldsPreferences = new SpecialFieldsPreferences(getBoolean(SPECIALFIELDSENABLED));

        EasyBind.listen(specialFieldsPreferences.specialFieldsEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(SPECIALFIELDSENABLED, newValue));

        return specialFieldsPreferences;
    }

    // region Preview preferences
    public PreviewPreferences getPreviewPreferences() {
        if (previewPreferences != null) {
            return previewPreferences;
        }

        String style = get(PREVIEW_STYLE);
        List<PreviewLayout> layouts = getPreviewLayouts(style);

        this.previewPreferences = new PreviewPreferences(
                layouts,
                getPreviewCyclePosition(layouts),
                new TextBasedPreviewLayout(
                        style,
                        getLayoutFormatterPreferences(),
                        Injector.instantiateModelOrService(JournalAbbreviationRepository.class)),
                (String) defaults.get(PREVIEW_STYLE),
                getBoolean(PREVIEW_AS_TAB),
                getBoolean(PREVIEW_IN_ENTRY_TABLE_TOOLTIP),
                getStringList(PREVIEW_BST_LAYOUT_PATHS).stream()
                                                       .map(Path::of)
                                                       .collect(Collectors.toList())
        );

        previewPreferences.getLayoutCycle().addListener((InvalidationListener) c -> storePreviewLayouts(previewPreferences.getLayoutCycle()));
        EasyBind.listen(previewPreferences.layoutCyclePositionProperty(), (obs, oldValue, newValue) -> putInt(CYCLE_PREVIEW_POS, newValue));
        EasyBind.listen(previewPreferences.customPreviewLayoutProperty(), (obs, oldValue, newValue) -> put(PREVIEW_STYLE, newValue.getText()));
        EasyBind.listen(previewPreferences.showPreviewAsExtraTabProperty(), (obs, oldValue, newValue) -> putBoolean(PREVIEW_AS_TAB, newValue));
        EasyBind.listen(previewPreferences.showPreviewEntryTableTooltip(), (obs, oldValue, newValue) -> putBoolean(PREVIEW_IN_ENTRY_TABLE_TOOLTIP, newValue));
        previewPreferences.getBstPreviewLayoutPaths().addListener((InvalidationListener) c -> storeBstPaths(previewPreferences.getBstPreviewLayoutPaths()));
        return this.previewPreferences;
    }

    private void storeBstPaths(List<Path> bstPaths) {
        putStringList(PREVIEW_BST_LAYOUT_PATHS, bstPaths.stream().map(Path::toAbsolutePath).map(Path::toString).toList());
    }

    private List<PreviewLayout> getPreviewLayouts(String style) {
        List<String> cycle = getStringList(CYCLE_PREVIEW);

        // For backwards compatibility always add at least the default preview to the cycle
        if (cycle.isEmpty()) {
            cycle.add("Preview");
        }

        return cycle.stream()
                    .map(layout -> {
                        if (CitationStyle.isCitationStyleFile(layout)) {
                            BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
                            return CitationStyle.createCitationStyleFromFile(layout)
                                                .map(file -> (PreviewLayout) new CitationStylePreviewLayout(file, entryTypesManager))
                                                .orElse(null);
                        }
                        if (BstPreviewLayout.isBstStyleFile(layout)) {
                            return getStringList(PREVIEW_BST_LAYOUT_PATHS).stream()
                                                                          .filter(path -> path.endsWith(layout)).map(Path::of)
                                                                          .map(BstPreviewLayout::new)
                                                                          .findFirst()
                                                                          .orElse(null);
                        } else {
                            return new TextBasedPreviewLayout(
                                    style,
                                    getLayoutFormatterPreferences(),
                                    Injector.instantiateModelOrService(JournalAbbreviationRepository.class));
                        }
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }

    private void storePreviewLayouts(ObservableList<PreviewLayout> previewCycle) {
        putStringList(CYCLE_PREVIEW, previewCycle.stream()
                                                 .map(layout -> {
                                                     if (layout instanceof CitationStylePreviewLayout citationStyleLayout) {
                                                         return citationStyleLayout.getFilePath();
                                                     } else {
                                                         return layout.getDisplayName();
                                                     }
                                                 }).collect(Collectors.toList())
        );
    }

    private int getPreviewCyclePosition(List<PreviewLayout> layouts) {
        int storedCyclePos = getInt(CYCLE_PREVIEW_POS);
        if (storedCyclePos < layouts.size()) {
            return storedCyclePos;
        } else {
            return 0; // fallback if stored position is no longer valid
        }
    }
    // endregion

    // region PushToApplicationPreferences

    public PushToApplicationPreferences getPushToApplicationPreferences() {
        if (pushToApplicationPreferences != null) {
            return pushToApplicationPreferences;
        }

        Map<String, String> applicationCommands = new HashMap<>();
        // getEmptyIsDefault is used to ensure that an installation of a tool leads to the new path (instead of leaving the empty one)
        // Reason: empty string is returned by org.jabref.gui.desktop.os.Windows.detectProgramPath if program is not found. That path is stored in the preferences.
        applicationCommands.put(PushToApplications.EMACS, getEmptyIsDefault(PUSH_EMACS_PATH));
        applicationCommands.put(PushToApplications.LYX, getEmptyIsDefault(PUSH_LYXPIPE));
        applicationCommands.put(PushToApplications.TEXMAKER, getEmptyIsDefault(PUSH_TEXMAKER_PATH));
        applicationCommands.put(PushToApplications.TEXSTUDIO, getEmptyIsDefault(PUSH_TEXSTUDIO_PATH));
        applicationCommands.put(PushToApplications.TEXWORKS, getEmptyIsDefault(PUSH_TEXWORKS_PATH));
        applicationCommands.put(PushToApplications.VIM, getEmptyIsDefault(PUSH_VIM));
        applicationCommands.put(PushToApplications.WIN_EDT, getEmptyIsDefault(PUSH_WINEDT_PATH));
        applicationCommands.put(PushToApplications.SUBLIME_TEXT, getEmptyIsDefault(PUSH_SUBLIME_TEXT_PATH));
        applicationCommands.put(PushToApplications.VSCODE, getEmptyIsDefault(PUSH_VSCODE_PATH));

        pushToApplicationPreferences = new PushToApplicationPreferences(
                get(PUSH_TO_APPLICATION),
                applicationCommands,
                get(PUSH_EMACS_ADDITIONAL_PARAMETERS),
                get(PUSH_VIM_SERVER)
        );

        EasyBind.listen(pushToApplicationPreferences.activeApplicationNameProperty(), (obs, oldValue, newValue) -> put(PUSH_TO_APPLICATION, newValue));
        pushToApplicationPreferences.getCommandPaths().addListener((obs, oldValue, newValue) -> storePushToApplicationPath(newValue));
        EasyBind.listen(pushToApplicationPreferences.emacsArgumentsProperty(), (obs, oldValue, newValue) -> put(PUSH_EMACS_ADDITIONAL_PARAMETERS, newValue));
        EasyBind.listen(pushToApplicationPreferences.vimServerProperty(), (obs, oldValue, newValue) -> put(PUSH_VIM_SERVER, newValue));
        return pushToApplicationPreferences;
    }

    private void storePushToApplicationPath(Map<String, String> commandPair) {
        commandPair.forEach((key, value) -> {
            switch (key) {
                case PushToApplications.EMACS ->
                        put(PUSH_EMACS_PATH, value);
                case PushToApplications.LYX ->
                        put(PUSH_LYXPIPE, value);
                case PushToApplications.TEXMAKER ->
                        put(PUSH_TEXMAKER_PATH, value);
                case PushToApplications.TEXSTUDIO ->
                        put(PUSH_TEXSTUDIO_PATH, value);
                case PushToApplications.TEXWORKS ->
                        put(PUSH_TEXWORKS_PATH, value);
                case PushToApplications.VIM ->
                        put(PUSH_VIM, value);
                case PushToApplications.WIN_EDT ->
                        put(PUSH_WINEDT_PATH, value);
                case PushToApplications.SUBLIME_TEXT ->
                        put(PUSH_SUBLIME_TEXT_PATH, value);
                case PushToApplications.VSCODE ->
                        put(PUSH_VSCODE_PATH, value);
            }
        });
    }

    // endregion

    // region NameDisplayPreferences

    @Override
    public NameDisplayPreferences getNameDisplayPreferences() {
        if (nameDisplayPreferences != null) {
            return nameDisplayPreferences;
        }

        nameDisplayPreferences = new NameDisplayPreferences(
                getNameDisplayStyle(),
                getNameAbbreviationStyle());

        EasyBind.listen(nameDisplayPreferences.displayStyleProperty(), (obs, oldValue, newValue) -> {
            putBoolean(NAMES_NATBIB, newValue == NameDisplayPreferences.DisplayStyle.NATBIB);
            putBoolean(NAMES_AS_IS, newValue == NameDisplayPreferences.DisplayStyle.AS_IS);
            putBoolean(NAMES_FIRST_LAST, newValue == NameDisplayPreferences.DisplayStyle.FIRSTNAME_LASTNAME);
        });
        EasyBind.listen(nameDisplayPreferences.abbreviationStyleProperty(), (obs, oldValue, newValue) -> {
            putBoolean(ABBR_AUTHOR_NAMES, newValue == NameDisplayPreferences.AbbreviationStyle.FULL);
            putBoolean(NAMES_LAST_ONLY, newValue == NameDisplayPreferences.AbbreviationStyle.LASTNAME_ONLY);
        });

        return nameDisplayPreferences;
    }

    private NameDisplayPreferences.AbbreviationStyle getNameAbbreviationStyle() {
        NameDisplayPreferences.AbbreviationStyle abbreviationStyle = NameDisplayPreferences.AbbreviationStyle.NONE; // default
        if (getBoolean(ABBR_AUTHOR_NAMES)) {
            abbreviationStyle = NameDisplayPreferences.AbbreviationStyle.FULL;
        } else if (getBoolean(NAMES_LAST_ONLY)) {
            abbreviationStyle = NameDisplayPreferences.AbbreviationStyle.LASTNAME_ONLY;
        }
        return abbreviationStyle;
    }

    private NameDisplayPreferences.DisplayStyle getNameDisplayStyle() {
        NameDisplayPreferences.DisplayStyle displayStyle = NameDisplayPreferences.DisplayStyle.LASTNAME_FIRSTNAME; // default
        if (getBoolean(NAMES_NATBIB)) {
            displayStyle = NameDisplayPreferences.DisplayStyle.NATBIB;
        } else if (getBoolean(NAMES_AS_IS)) {
            displayStyle = NameDisplayPreferences.DisplayStyle.AS_IS;
        } else if (getBoolean(NAMES_FIRST_LAST)) {
            displayStyle = NameDisplayPreferences.DisplayStyle.FIRSTNAME_LASTNAME;
        }
        return displayStyle;
    }

    // endregion

    // region: Main table, main table column, and search dialog column preferences

    public MainTablePreferences getMainTablePreferences() {
        if (mainTablePreferences != null) {
            return mainTablePreferences;
        }

        mainTablePreferences = new MainTablePreferences(
                getMainTableColumnPreferences(),
                getBoolean(AUTO_RESIZE_MODE),
                getBoolean(EXTRA_FILE_COLUMNS));

        EasyBind.listen(mainTablePreferences.resizeColumnsToFitProperty(),
                (obs, oldValue, newValue) -> putBoolean(AUTO_RESIZE_MODE, newValue));
        EasyBind.listen(mainTablePreferences.extraFileColumnsEnabledProperty(),
                (obs, oldValue, newValue) -> putBoolean(EXTRA_FILE_COLUMNS, newValue));

        return mainTablePreferences;
    }

    public ColumnPreferences getMainTableColumnPreferences() {
        if (mainTableColumnPreferences != null) {
            return mainTableColumnPreferences;
        }

        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, COLUMN_WIDTHS, COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(COLUMN_SORT_ORDER, columns);
        mainTableColumnPreferences = new ColumnPreferences(columns, columnSortOrder);

        mainTableColumnPreferences.getColumns().addListener((InvalidationListener) change -> {
            putStringList(COLUMN_NAMES, getColumnNamesAsStringList(mainTableColumnPreferences));
            putStringList(COLUMN_WIDTHS, getColumnWidthsAsStringList(mainTableColumnPreferences));
            putStringList(COLUMN_SORT_TYPES, getColumnSortTypesAsStringList(mainTableColumnPreferences));
        });
        mainTableColumnPreferences.getColumnSortOrder().addListener((InvalidationListener) change ->
                putStringList(COLUMN_SORT_ORDER, getColumnSortOrderAsStringList(mainTableColumnPreferences)));

        return mainTableColumnPreferences;
    }

    public ColumnPreferences getSearchDialogColumnPreferences() {
        if (searchDialogColumnPreferences != null) {
            return searchDialogColumnPreferences;
        }

        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, SEARCH_DIALOG_COLUMN_WIDTHS, SEARCH_DIALOG_COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(SEARCH_DIALOG_COLUMN_SORT_ORDER, columns);
        searchDialogColumnPreferences = new ColumnPreferences(columns, columnSortOrder);

        searchDialogColumnPreferences.getColumns().addListener((InvalidationListener) change -> {
            // MainTable and SearchResultTable use the same set of columnNames
            // putStringList(SEARCH_DIALOG_COLUMN_NAMES, getColumnNamesAsStringList(columnPreferences));
            putStringList(SEARCH_DIALOG_COLUMN_WIDTHS, getColumnWidthsAsStringList(searchDialogColumnPreferences));
            putStringList(SEARCH_DIALOG_COLUMN_SORT_TYPES, getColumnSortTypesAsStringList(searchDialogColumnPreferences));
        });
        searchDialogColumnPreferences.getColumnSortOrder().addListener((InvalidationListener) change ->
                putStringList(SEARCH_DIALOG_COLUMN_SORT_ORDER, getColumnSortOrderAsStringList(searchDialogColumnPreferences)));

        return searchDialogColumnPreferences;
    }

    // --- Generic column handling ---
    @SuppressWarnings("SameParameterValue")
    private List<MainTableColumnModel> getColumns(String columnNamesList, String columnWidthList, String sortTypeList, double defaultWidth) {
        List<String> columnNames = getStringList(columnNamesList);
        List<Double> columnWidths = getStringList(columnWidthList)
                .stream()
                .map(string -> {
                    try {
                        return Double.parseDouble(string);
                    } catch (NumberFormatException e) {
                        LOGGER.error("Exception while parsing column widths. Choosing default.", e);
                        return defaultWidth;
                    }
                }).toList();

        List<TableColumn.SortType> columnSortTypes = getStringList(sortTypeList)
                .stream()
                .map(TableColumn.SortType::valueOf).toList();

        List<MainTableColumnModel> columns = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            MainTableColumnModel columnModel = MainTableColumnModel.parse(columnNames.get(i));

            if (i < columnWidths.size()) {
                columnModel.widthProperty().setValue(columnWidths.get(i));
            }

            if (i < columnSortTypes.size()) {
                columnModel.sortTypeProperty().setValue(columnSortTypes.get(i));
            }

            columns.add(columnModel);
        }
        return columns;
    }

    private List<MainTableColumnModel> getColumnSortOrder(String sortOrderList, List<MainTableColumnModel> tableColumns) {
        List<MainTableColumnModel> columnsOrdered = new ArrayList<>();
        getStringList(sortOrderList).forEach(columnName -> tableColumns.stream().filter(column -> column.getName().equals(columnName))
                                                                       .findFirst()
                                                                       .ifPresent(columnsOrdered::add));

        return columnsOrdered;
    }

    private static List<String> getColumnNamesAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumns().stream()
                                .map(MainTableColumnModel::getName)
                                .toList();
    }

    private static List<String> getColumnWidthsAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumns().stream()
                                .map(column -> column.widthProperty().getValue().toString())
                                .toList();
    }

    private static List<String> getColumnSortTypesAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumns().stream()
                                .map(column -> column.sortTypeProperty().getValue().toString())
                                .toList();
    }

    private static List<String> getColumnSortOrderAsStringList(ColumnPreferences columnPreferences) {
        return columnPreferences.getColumnSortOrder().stream()
                                .map(MainTableColumnModel::getName)
                                .collect(Collectors.toList());
    }
    // endregion

    /**
     * For the export configuration, generates the SelfContainedSaveOrder having the reference to TABLE resolved.
     */
    private SelfContainedSaveOrder getSelfContainedTableSaveOrder() {
        List<MainTableColumnModel> sortOrder = getMainTableColumnPreferences().getColumnSortOrder();
        return new SelfContainedSaveOrder(
                SaveOrder.OrderType.SPECIFIED,
                sortOrder.stream().flatMap(model -> model.getSortCriteria().stream()).toList());
    }

    @Override
    public SelfContainedSaveConfiguration getSelfContainedExportConfiguration() {
        SaveOrder exportSaveOrder = getExportSaveOrder();
        SelfContainedSaveOrder saveOrder = switch (exportSaveOrder.getOrderType()) {
            case TABLE ->
                    this.getSelfContainedTableSaveOrder();
            case SPECIFIED ->
                    SelfContainedSaveOrder.of(exportSaveOrder);
            case ORIGINAL ->
                    SaveOrder.getDefaultSaveOrder();
        };

        return new SelfContainedSaveConfiguration(
                saveOrder, false, BibDatabaseWriter.SaveType.WITH_JABREF_META_DATA, getLibraryPreferences()
                .shouldAlwaysReformatOnSave());
    }

    @Override
    public KeyBindingRepository getKeyBindingRepository() {
        if (keyBindingRepository != null) {
            return keyBindingRepository;
        }

        keyBindingRepository = new KeyBindingRepository(getStringList(BIND_NAMES), getStringList(BINDINGS));

        EasyBind.listen(keyBindingRepository.getBindingsProperty(), (obs, oldValue, newValue) -> {
            putStringList(BIND_NAMES, keyBindingRepository.getBindNames());
            putStringList(BINDINGS, keyBindingRepository.getBindings());
        });

        return keyBindingRepository;
    }

    /**
     * In GUI mode, we can lookup the directory better
     */
    @Override
    protected Path getDefaultPath() {
        return NativeDesktop.get().getDefaultFileChooserDirectory();
    }

    @Override
    protected boolean moveToTrashSupported() {
        return NativeDesktop.get().moveToTrashSupported();
    }
}
