package org.jabref.gui.preferences;

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
import java.util.prefs.BackingStoreException;
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
import org.jabref.gui.newentry.NewEntryDialogTab;
import org.jabref.gui.newentry.NewEntryPreferences;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.specialfields.SpecialFieldsPreferences;
import org.jabref.gui.theme.Theme;
import org.jabref.logic.JabRefException;
import org.jabref.logic.bst.BstPreviewLayout;
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.citationstyle.CSLStyleUtils;
import org.jabref.logic.citationstyle.CitationStylePreviewLayout;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.TextBasedPreviewLayout;
import org.jabref.logic.preferences.AutoCompleteFirstNameMode;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.types.EntryType;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.metadata.SelfContainedSaveOrder;

import com.airhacks.afterburner.injection.Injector;
import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefGuiPreferences extends JabRefCliPreferences implements GuiPreferences {

    // Public because needed for pref migration
    public static final String AUTOCOMPLETER_COMPLETE_FIELDS = "autoCompleteFields";

    // region Preview - public for pref migrations
    public static final String PREVIEW_STYLE = "previewStyle";
    public static final String CYCLE_PREVIEW_POS = "cyclePreviewPos";
    public static final String CYCLE_PREVIEW = "cyclePreview";
    public static final String PREVIEW_AS_TAB = "previewAsTab";
    public static final String PREVIEW_IN_ENTRY_TABLE_TOOLTIP = "previewInEntryTableTooltip";
    public static final String PREVIEW_BST_LAYOUT_PATHS = "previewBstLayoutPaths";
    public static final String COVER_IMAGE_DOWNLOAD = "coverDownload";
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

    // region WorkspacePreferences
    private static final String OVERRIDE_DEFAULT_FONT_SIZE = "overrideDefaultFontSize";
    private static final String MAIN_FONT_SIZE = "mainFontSize";
    private static final String THEME = "fxTheme";
    private static final String THEME_SYNC_OS = "themeSyncOs";
    private static final String OPEN_LAST_EDITED = "openLastEdited";
    private static final String SHOW_ADVANCED_HINTS = "showAdvancedHints";
    private static final String CONFIRM_DELETE = "confirmDelete";
    private static final String CONFIRM_HIDE_TAB_BAR = "confirmHideTabBar";
    private static final String SELECTED_SLR_CATALOGS = "selectedSlrCatalogs";
    // endregion

    // region core GUI preferences
    private static final String MAIN_WINDOW_POS_X = "mainWindowPosX";
    private static final String MAIN_WINDOW_POS_Y = "mainWindowPosY";
    private static final String MAIN_WINDOW_WIDTH = "mainWindowSizeX";
    private static final String MAIN_WINDOW_HEIGHT = "mainWindowSizeY";
    private static final String MAIN_WINDOW_MAXIMISED = "windowMaximised";
    private static final String MAIN_WINDOW_SIDEPANE_WIDTH = "sidePaneWidthFX";
    private static final String MAIN_WINDOW_EDITOR_HEIGHT = "entryEditorHeightFX";
    // endregion

    private static final String SIDE_PANE_COMPONENT_PREFERRED_POSITIONS = "sidePaneComponentPreferredPositions";
    private static final String SIDE_PANE_COMPONENT_NAMES = "sidePaneComponentNames";

    // region main table, main table columns, save columns
    private static final String AUTO_RESIZE_MODE = "autoResizeMode";
    private static final String EXTRA_FILE_COLUMNS = "extraFileColumns";

    private static final String SEARCH_DIALOG_COLUMN_WIDTHS = "searchTableColumnWidths";
    private static final String SEARCH_DIALOG_COLUMN_SORT_TYPES = "searchDialogColumnSortTypes";
    private static final String SEARCH_DIALOG_COLUMN_SORT_ORDER = "searchDalogColumnSortOrder";
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
    private static final String EMAIL_SUBJECT = "emailSubject";
    private static final String KINDLE_EMAIL = "kindleEmail";
    private static final String OPEN_FOLDERS_OF_ATTACHED_FILES = "openFoldersOfAttachedFiles";
    private static final String FILE_BROWSER_COMMAND = "fileBrowserCommand";
    // endregion

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

    private static final String UNLINKED_FILES_SELECTED_EXTENSION = "unlinkedFilesSelectedExtension";
    private static final String UNLINKED_FILES_SELECTED_DATE_RANGE = "unlinkedFilesSelectedDateRange";
    private static final String UNLINKED_FILES_SELECTED_SORT = "unlinkedFilesSelectedSort";

    private static final String INCLUDE_CROSS_REFERENCES = "includeCrossReferences";
    private static final String ASK_FOR_INCLUDING_CROSS_REFERENCES = "askForIncludingCrossReferences";

    // region Donation preferences
    private static final String DONATION_NEVER_SHOW = "donationNeverShow";
    private static final String DONATION_LAST_SHOWN_EPOCH_DAY = "donationLastShownEpochDay";
    // endregion

    // region NewEntryPreferences
    private static final String CREATE_ENTRY_APPROACH = "latestApproach";
    private static final String CREATE_ENTRY_EXPAND_RECOMMENDED = "typesRecommendedExpanded";
    private static final String CREATE_ENTRY_EXPAND_OTHER = "typesOtherExpanded";
    private static final String CREATE_ENTRY_EXPAND_CUSTOM = "typesCustomExpanded";
    private static final String CREATE_ENTRY_IMMEDIATE_TYPE = "latestImmediateType";
    private static final String CREATE_ENTRY_ID_LOOKUP_GUESSING = "idLookupGuessing";
    private static final String CREATE_ENTRY_ID_FETCHER_NAME = "latestIdFetcherName";
    private static final String CREATE_ENTRY_INTERPRET_PARSER_NAME = "latestInterpretParserName";
    // endregion

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
    private NameDisplayPreferences nameDisplayPreferences;
    private MainTablePreferences mainTablePreferences;
    private ColumnPreferences mainTableColumnPreferences;
    private ColumnPreferences searchDialogColumnPreferences;
    private KeyBindingRepository keyBindingRepository;
    private CopyToPreferences copyToPreferences;
    private NewEntryPreferences newEntryPreferences;
    private DonationPreferences donationPreferences;

    private JabRefGuiPreferences() {
        super();

        defaults.put(JOURNAL_POPUP, EntryEditorPreferences.JournalPopupEnabled.FIRST_START.toString());

        defaults.put(ENTRY_EDITOR_PREVIEW_DIVIDER_POS, 0.5);
        // endregion

        defaults.put(SPECIALFIELDSENABLED, Boolean.TRUE);

        // region PreviewStyle
        defaults.put(CYCLE_PREVIEW, "Preview;" + CSLStyleLoader.DEFAULT_STYLE);
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

        // By default disable "Fit table horizontally on the screen"
        defaults.put(AUTO_RESIZE_MODE, Boolean.FALSE);

        defaults.put(COVER_IMAGE_DOWNLOAD, Boolean.TRUE);
    }

    /**
     * @deprecated Never ever add a call to this method. There should be only one caller.
     * All other usages should get the preferences passed (or injected).
     * The JabRef team leaves the {@code @deprecated} annotation to have IntelliJ listing this method with a strike-through.
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
        copyToPreferences = getCopyToPreferencesFromBackingStore(CopyToPreferences.getDefault());

        EasyBind.listen(copyToPreferences.shouldAskForIncludingCrossReferencesProperty(), (obs, oldValue, newValue) -> putBoolean(ASK_FOR_INCLUDING_CROSS_REFERENCES, newValue));
        EasyBind.listen(copyToPreferences.shouldIncludeCrossReferencesProperty(), (obs, oldValue, newValue) -> putBoolean(INCLUDE_CROSS_REFERENCES, newValue));

        return copyToPreferences;
    }

    private CopyToPreferences getCopyToPreferencesFromBackingStore(CopyToPreferences defaults) {
        return new CopyToPreferences(
                getBoolean(ASK_FOR_INCLUDING_CROSS_REFERENCES, defaults.getShouldAskForIncludingCrossReferences()),
                getBoolean(INCLUDE_CROSS_REFERENCES, defaults.getShouldIncludeCrossReferences())
        );
    }

    @Override
    public void clear() throws BackingStoreException {
        super.clear();

        getDonationPreferences().setAll(DonationPreferences.getDefault());
        getGroupsPreferences().setAll(GroupsPreferences.getDefault());
        getCopyToPreferences().setAll(CopyToPreferences.getDefault());
        getGuiPreferences().setAll(CoreGuiPreferences.getDefault());
        getSpecialFieldsPreferences().setAll(SpecialFieldsPreferences.getDefault());
        getExternalApplicationsPreferences().setAll(ExternalApplicationsPreferences.getDefault());
        getMainTableColumnPreferences().setAll(ColumnPreferences.getDefault());
        getMergeDialogPreferences().setAll(MergeDialogPreferences.getDefault());
        getMainTablePreferences().setAll(MainTablePreferences.getDefault());
        getNewEntryPreferences().setAll(NewEntryPreferences.getDefault());
        getSearchDialogColumnPreferences().setAll(ColumnPreferences.getDefault());
        getUnlinkedFilesDialogPreferences().setAll(UnlinkedFilesDialogPreferences.getDefault());
        getWorkspacePreferences().setAll(WorkspacePreferences.getDefault());
        getAutoCompletePreferences().setAll(AutoCompletePreferences.getDefault());
        getSidePanePreferences().setAll(SidePanePreferences.getDefault());
    }

    @Override
    public void importPreferences(Path path) throws JabRefException {
        super.importPreferences(path);

        // in case of incomplete or corrupt xml fall back to current preferences
        getDonationPreferences().setAll(getDonationPreferencesFromBackingStore(getDonationPreferences()));
        getGroupsPreferences().setAll(getGroupsPreferencesfromBackingStore(getGroupsPreferences()));
        getCopyToPreferences().setAll(getCopyToPreferencesFromBackingStore(getCopyToPreferences()));
        getGuiPreferences().setAll(getCoreGuiPreferencesFromBackingStore(getGuiPreferences()));
        getSpecialFieldsPreferences().setAll(getSpecialFieldsPreferencesFromBackingStore(getSpecialFieldsPreferences()));
        getExternalApplicationsPreferences().setAll(getExternalApplicationsPreferencesFromBackingStore(getExternalApplicationsPreferences()));
        getMainTableColumnPreferences().setAll(getMainTableColumnPreferencesFromBackingStore(getMainTableColumnPreferences()));
        getMergeDialogPreferences().setAll(getMergeDialogPreferencesFromBackingStore(getMergeDialogPreferences()));
        getMainTablePreferences().setAll(getMainTablePreferencesFromBackingStore(getMainTablePreferences()));
        getNewEntryPreferences().setAll(getNewEntryPreferencesFromBackingStore(getNewEntryPreferences()));
        getSearchDialogColumnPreferences().setAll(getSearchDialogColumnPreferencesFromBackingStore(getSearchDialogColumnPreferences()));
        getUnlinkedFilesDialogPreferences().setAll(getUnlinkedFilesDialogPreferences());
        getWorkspacePreferences().setAll(getWorkspacePreferencesFromBackingStore(getWorkspacePreferences()));
        getAutoCompletePreferences().setAll(getAutoCompletePreferencesFromBackingStore(getAutoCompletePreferences()));
        getSidePanePreferences().setAll(getSidePanePreferencesFromBackingStore(getSidePanePreferences()));
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
                getBoolean(SMART_FILE_ANNOTATIONS),
                getBoolean(DEFAULT_SHOW_SOURCE),
                getBoolean(VALIDATE_IN_ENTRY_EDITOR),
                getBoolean(ALLOW_INTEGER_EDITION_BIBTEX),
                getBoolean(AUTOLINK_FILES_ENABLED),
                EntryEditorPreferences.JournalPopupEnabled.fromString(get(JOURNAL_POPUP)),
                CitationFetcherType.SEMANTIC_SCHOLAR,
                getBoolean(SHOW_SCITE_TAB),
                getBoolean(SHOW_USER_COMMENTS_FIELDS),
                getDouble(ENTRY_EDITOR_PREVIEW_DIVIDER_POS));

        EasyBind.listen(entryEditorPreferences.entryEditorTabs(), (_, _, newValue) -> storeEntryEditorTabs(newValue));
        // defaultEntryEditorTabs are read-only
        EasyBind.listen(entryEditorPreferences.shouldOpenOnNewEntryProperty(), (_, _, newValue) -> putBoolean(AUTO_OPEN_FORM, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowRecommendationsTabProperty(), (_, _, newValue) -> putBoolean(SHOW_RECOMMENDATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowAiSummaryTabProperty(), (_, _, newValue) -> putBoolean(SHOW_AI_SUMMARY, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowAiChatTabProperty(), (_, _, newValue) -> putBoolean(SHOW_AI_CHAT, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowLatexCitationsTabProperty(), (_, _, newValue) -> putBoolean(SHOW_LATEX_CITATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.shouldShowFileAnnotationsTabProperty(), (_, _, newValue) -> putBoolean(SMART_FILE_ANNOTATIONS, newValue));
        EasyBind.listen(entryEditorPreferences.showSourceTabByDefaultProperty(), (_, _, newValue) -> putBoolean(DEFAULT_SHOW_SOURCE, newValue));
        EasyBind.listen(entryEditorPreferences.enableValidationProperty(), (_, _, newValue) -> putBoolean(VALIDATE_IN_ENTRY_EDITOR, newValue));
        EasyBind.listen(entryEditorPreferences.allowIntegerEditionBibtexProperty(), (_, _, newValue) -> putBoolean(ALLOW_INTEGER_EDITION_BIBTEX, newValue));
        EasyBind.listen(entryEditorPreferences.autoLinkEnabledProperty(), (_, _, newValue) -> putBoolean(AUTOLINK_FILES_ENABLED, newValue));
        EasyBind.listen(entryEditorPreferences.enableJournalPopupProperty(), (_, _, newValue) -> put(JOURNAL_POPUP, newValue.toString()));
        EasyBind.listen(entryEditorPreferences.shouldShowLSciteTabProperty(), (_, _, newValue) -> putBoolean(SHOW_SCITE_TAB, newValue));
        EasyBind.listen(entryEditorPreferences.showUserCommentsFieldsProperty(), (_, _, newValue) -> putBoolean(SHOW_USER_COMMENTS_FIELDS, newValue));
        EasyBind.listen(entryEditorPreferences.previewWidthDividerPositionProperty(), (_, _, newValue) -> putDouble(ENTRY_EDITOR_PREVIEW_DIVIDER_POS, newValue.doubleValue()));
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

        MergeDialogPreferences defaults = MergeDialogPreferences.getDefault();

        mergeDialogPreferences = getMergeDialogPreferencesFromBackingStore(defaults);

        EasyBind.listen(mergeDialogPreferences.mergeDiffModeProperty(), (_, _, newValue) -> put(MERGE_ENTRIES_DIFF_MODE, newValue.name()));
        EasyBind.listen(mergeDialogPreferences.mergeShouldShowDiffProperty(), (_, _, newValue) -> putBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeShouldShowUnifiedDiffProperty(), (_, _, newValue) -> putBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeHighlightWordsProperty(), (_, _, newValue) -> putBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeShowChangedFieldOnlyProperty(), (_, _, newValue) -> putBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS, newValue));
        EasyBind.listen(mergeDialogPreferences.mergeApplyToAllEntriesProperty(), (_, _, newValue) -> putBoolean(MERGE_APPLY_TO_ALL_ENTRIES, newValue));
        EasyBind.listen(mergeDialogPreferences.allEntriesDuplicateResolverDecisionProperty(), (_, _, newValue) -> put(DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES, newValue.name()));

        return mergeDialogPreferences;
    }

    private MergeDialogPreferences getMergeDialogPreferencesFromBackingStore(MergeDialogPreferences defaults) {
        return new MergeDialogPreferences(
                DiffMode.valueOf(get(MERGE_ENTRIES_DIFF_MODE, defaults.getMergeDiffMode().name())),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF, defaults.getMergeShouldShowDiff()),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, defaults.getMergeShouldShowUnifiedDiff()),
                getBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS, defaults.getMergeHighlightWords()),
                getBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS, defaults.shouldMergeShowChangedFieldsOnly()),
                getBoolean(MERGE_APPLY_TO_ALL_ENTRIES, defaults.shouldMergeApplyToAllEntries()),
                DuplicateResolverDialog.DuplicateResolverResult.valueOf(
                        get(DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES, defaults.getAllEntriesDuplicateResolverDecision().name()))
        );
    }

    // region auto complete preferences
    @Override
    public AutoCompletePreferences getAutoCompletePreferences() {
        if (autoCompletePreferences != null) {
            return autoCompletePreferences;
        }

        autoCompletePreferences = getAutoCompletePreferencesFromBackingStore(AutoCompletePreferences.getDefault());

        EasyBind.listen(autoCompletePreferences.autoCompleteProperty(), (obs, oldValue, newValue) -> putBoolean(AUTO_COMPLETE, newValue));
        EasyBind.listen(autoCompletePreferences.firstNameModeProperty(), (obs, oldValue, newValue) -> put(AUTOCOMPLETER_FIRSTNAME_MODE, newValue.name()));
        autoCompletePreferences.getCompleteFields().addListener((SetChangeListener<Field>) c ->
                putStringList(AUTOCOMPLETER_COMPLETE_FIELDS, autoCompletePreferences.getCompleteFields().stream()
                                                                                    .map(Field::getName)
                                                                                    .collect(Collectors.toList())));
        EasyBind.listen(autoCompletePreferences.nameFormatProperty(), (_, _, _) -> {
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

    private AutoCompletePreferences getAutoCompletePreferencesFromBackingStore(AutoCompletePreferences defaults) {
        AutoCompletePreferences.NameFormat nameFormat = defaults.getNameFormat();
        final boolean firstLast = getBoolean(AUTOCOMPLETER_FIRST_LAST, false);
        final boolean lastFirst = getBoolean(AUTOCOMPLETER_LAST_FIRST, false);
        if (firstLast && !lastFirst) {
            nameFormat = AutoCompletePreferences.NameFormat.FIRST_LAST;
        } else if (!firstLast && lastFirst) {
            nameFormat = AutoCompletePreferences.NameFormat.LAST_FIRST;
        }

        return new AutoCompletePreferences(getBoolean(AUTO_COMPLETE, defaults.shouldAutoComplete()),
                AutoCompleteFirstNameMode.parse(get(AUTOCOMPLETER_FIRSTNAME_MODE, defaults.getFirstNameMode().name())),
                nameFormat,
                getFieldSequencedSet(AUTOCOMPLETER_COMPLETE_FIELDS, defaults.getCompleteFields()));
    }
    // endregion

    // region core GUI preferences
    public CoreGuiPreferences getGuiPreferences() {
        if (coreGuiPreferences != null) {
            return coreGuiPreferences;
        }

        coreGuiPreferences = getCoreGuiPreferencesFromBackingStore(CoreGuiPreferences.getDefault());

        EasyBind.listen(coreGuiPreferences.positionXProperty(), (_, _, newValue) -> putDouble(MAIN_WINDOW_POS_X, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.positionYProperty(), (_, _, newValue) -> putDouble(MAIN_WINDOW_POS_Y, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.sizeXProperty(), (_, _, newValue) -> putDouble(MAIN_WINDOW_WIDTH, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.sizeYProperty(), (_, _, newValue) -> putDouble(MAIN_WINDOW_HEIGHT, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.windowMaximisedProperty(), (_, _, newValue) -> putBoolean(MAIN_WINDOW_MAXIMISED, newValue));
        EasyBind.listen(coreGuiPreferences.horizontalDividerPositionProperty(), (_, _, newValue) -> putDouble(MAIN_WINDOW_SIDEPANE_WIDTH, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.getVerticalDividerPositionProperty(), (_, _, newValue) -> putDouble(MAIN_WINDOW_EDITOR_HEIGHT, newValue.doubleValue()));

        return coreGuiPreferences;
    }

    private CoreGuiPreferences getCoreGuiPreferencesFromBackingStore(CoreGuiPreferences defaults) {
        return new CoreGuiPreferences(
                getDouble(MAIN_WINDOW_POS_X, defaults.getPositionX()),
                getDouble(MAIN_WINDOW_POS_Y, defaults.getPositionY()),
                getDouble(MAIN_WINDOW_WIDTH, defaults.getSizeX()),
                getDouble(MAIN_WINDOW_HEIGHT, defaults.getSizeY()),
                getBoolean(MAIN_WINDOW_MAXIMISED, defaults.isWindowMaximised()),
                getDouble(MAIN_WINDOW_SIDEPANE_WIDTH, defaults.getHorizontalDividerPosition()),
                getDouble(MAIN_WINDOW_EDITOR_HEIGHT, defaults.getVerticalDividerPosition()));
    }
    // endregion

    @Override
    public WorkspacePreferences getWorkspacePreferences() {
        if (workspacePreferences != null) {
            return workspacePreferences;
        }

        workspacePreferences = getWorkspacePreferencesFromBackingStore(WorkspacePreferences.getDefault());

        EasyBind.listen(workspacePreferences.languageProperty(), (_, oldValue, newValue) -> {
            put(LANGUAGE, newValue.getId());
            if (oldValue != newValue) {
                setLanguageDependentDefaultValues();
                Localization.setLanguage(newValue);
            }
        });

        EasyBind.listen(workspacePreferences.shouldOverrideDefaultFontSizeProperty(), (_, _, newValue) ->
                putBoolean(OVERRIDE_DEFAULT_FONT_SIZE, newValue));
        EasyBind.listen(workspacePreferences.mainFontSizeProperty(), (_, _, newValue) ->
                putInt(MAIN_FONT_SIZE, newValue));
        EasyBind.listen(workspacePreferences.themeProperty(), (_, _, newValue) ->
                put(THEME, newValue.getName()));
        EasyBind.listen(workspacePreferences.themeSyncOsProperty(), (_, _, newValue) ->
                putBoolean(THEME_SYNC_OS, newValue));
        EasyBind.listen(workspacePreferences.openLastEditedProperty(), (_, _, newValue) ->
                putBoolean(OPEN_LAST_EDITED, newValue));
        EasyBind.listen(workspacePreferences.showAdvancedHintsProperty(), (_, _, newValue) ->
                putBoolean(SHOW_ADVANCED_HINTS, newValue));
        EasyBind.listen(workspacePreferences.confirmDeleteProperty(), (_, _, newValue) ->
                putBoolean(CONFIRM_DELETE, newValue));
        EasyBind.listen(workspacePreferences.hideTabBarProperty(), (_, _, newValue) ->
                putBoolean(CONFIRM_HIDE_TAB_BAR, newValue));
        workspacePreferences.getSelectedSlrCatalogs().addListener((ListChangeListener<String>) _ ->
                putStringList(SELECTED_SLR_CATALOGS, workspacePreferences.getSelectedSlrCatalogs()));
        return workspacePreferences;
    }

    private WorkspacePreferences getWorkspacePreferencesFromBackingStore(WorkspacePreferences defaults) {
        return new WorkspacePreferences(
                getLanguage(),
                getBoolean(OVERRIDE_DEFAULT_FONT_SIZE, defaults.shouldOverrideDefaultFontSize()),
                getInt(MAIN_FONT_SIZE, defaults.getMainFontSize()),
                defaults.getDefaultFontSize(), // FixMe
                new Theme(get(THEME, Theme.BASE_CSS)),
                getBoolean(THEME_SYNC_OS, defaults.shouldThemeSyncOs()),
                getBoolean(OPEN_LAST_EDITED, defaults.shouldOpenLastEdited()),
                getBoolean(SHOW_ADVANCED_HINTS, defaults.shouldShowAdvancedHints()),
                getBoolean(CONFIRM_DELETE, defaults.shouldConfirmDelete()),
                getBoolean(CONFIRM_HIDE_TAB_BAR, defaults.shouldHideTabBar()),
                getStringList(SELECTED_SLR_CATALOGS));
    }

    @Override
    public UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferences() {
        if (unlinkedFilesDialogPreferences != null) {
            return unlinkedFilesDialogPreferences;
        }

        unlinkedFilesDialogPreferences = getUnlinkedFilesDialogPreferencesFromBackingStore(UnlinkedFilesDialogPreferences.getDefault());

        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedExtensionProperty(), (obs, oldValue, newValue) -> put(UNLINKED_FILES_SELECTED_EXTENSION, newValue));
        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedDateRangeProperty(), (obs, oldValue, newValue) -> put(UNLINKED_FILES_SELECTED_DATE_RANGE, newValue.name()));
        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedSortProperty(), (obs, oldValue, newValue) -> put(UNLINKED_FILES_SELECTED_SORT, newValue.name()));

        return unlinkedFilesDialogPreferences;
    }

    private UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferencesFromBackingStore(UnlinkedFilesDialogPreferences defaults) {
        return new UnlinkedFilesDialogPreferences(
                get(UNLINKED_FILES_SELECTED_EXTENSION, defaults.getUnlinkedFilesSelectedExtension()),
                DateRange.parse(get(UNLINKED_FILES_SELECTED_DATE_RANGE, defaults.getUnlinkedFilesSelectedDateRange().name())),
                ExternalFileSorter.parse(get(UNLINKED_FILES_SELECTED_SORT, defaults.getUnlinkedFilesSelectedSort().name()))
        );
    }

    // region SidePanePreferences
    @Override
    public SidePanePreferences getSidePanePreferences() {
        if (sidePanePreferences != null) {
            return sidePanePreferences;
        }

        sidePanePreferences = getSidePanePreferencesFromBackingStore(SidePanePreferences.getDefault());

        sidePanePreferences.visiblePanes().addListener((InvalidationListener) _ ->
                storeVisibleSidePanes(sidePanePreferences.visiblePanes()));
        sidePanePreferences.getPreferredPositions().addListener((InvalidationListener) _ ->
                storeSidePanePreferredPositions(sidePanePreferences.getPreferredPositions()));
        EasyBind.listen(sidePanePreferences.webSearchFetcherSelectedProperty(), (_, _, newValue) -> putInt(SELECTED_FETCHER_INDEX, newValue));

        return sidePanePreferences;
    }

    private SidePanePreferences getSidePanePreferencesFromBackingStore(SidePanePreferences defaults) {
        Set<SidePaneType> backingStoreVisiblePanes = getVisibleSidePanes(defaults.visiblePanes());
        Map<SidePaneType, Integer> backingStorePreferredPositions = getSidePanePreferredPositions(defaults);
        return new SidePanePreferences(
                backingStoreVisiblePanes.isEmpty() ? defaults.visiblePanes() : backingStoreVisiblePanes,
                backingStorePreferredPositions.isEmpty() ? defaults.getPreferredPositions() : backingStorePreferredPositions,
                getInt(SELECTED_FETCHER_INDEX, defaults.getWebSearchFetcherSelected())
        );
    }

    private Set<SidePaneType> getVisibleSidePanes(Set<SidePaneType> defaults) {
        Set<SidePaneType> visiblePanes = new HashSet<>();
        if (getBoolean(WEB_SEARCH_VISIBLE, defaults.contains(SidePaneType.WEB_SEARCH))) {
            visiblePanes.add(SidePaneType.WEB_SEARCH);
        }
        if (getBoolean(GROUP_SIDEPANE_VISIBLE, defaults.contains(SidePaneType.GROUPS))) {
            visiblePanes.add(SidePaneType.GROUPS);
        }
        if (getBoolean(OO_SHOW_PANEL, defaults.contains(SidePaneType.OPEN_OFFICE))) {
            visiblePanes.add(SidePaneType.OPEN_OFFICE);
        }
        return visiblePanes;
    }

    private void storeVisibleSidePanes(Set<SidePaneType> visiblePanes) {
        putBoolean(WEB_SEARCH_VISIBLE, visiblePanes.contains(SidePaneType.WEB_SEARCH));
        putBoolean(GROUP_SIDEPANE_VISIBLE, visiblePanes.contains(SidePaneType.GROUPS));
        putBoolean(OO_SHOW_PANEL, visiblePanes.contains(SidePaneType.OPEN_OFFICE));
    }

    private Map<SidePaneType, Integer> getSidePanePreferredPositions(SidePanePreferences defaults) {
        // If either one is missing the preferences are corrupt or empty, thus fall back to default
        if (!hasKey(SIDE_PANE_COMPONENT_NAMES) || !hasKey(SIDE_PANE_COMPONENT_PREFERRED_POSITIONS)) {
            LOGGER.debug("SidePane preferred positions corrupt, falling back to default");
            return defaults.getPreferredPositions();
        }

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

        externalApplicationsPreferences = getExternalApplicationsPreferencesFromBackingStore(ExternalApplicationsPreferences.getDefault());

        EasyBind.listen(externalApplicationsPreferences.eMailSubjectProperty(),
                (_, _, newValue) -> put(EMAIL_SUBJECT, newValue));
        EasyBind.listen(externalApplicationsPreferences.autoOpenEmailAttachmentsFolderProperty(),
                (_, _, newValue) -> putBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES, newValue));
        EasyBind.listen(externalApplicationsPreferences.useCustomTerminalProperty(),
                (_, _, newValue) -> putBoolean(USE_DEFAULT_CONSOLE_APPLICATION, !newValue)); // mind the !
        externalApplicationsPreferences.getExternalFileTypes().addListener((SetChangeListener<ExternalFileType>) c ->
                put(EXTERNAL_FILE_TYPES, ExternalFileTypes.toStringList(externalApplicationsPreferences.getExternalFileTypes())));
        EasyBind.listen(externalApplicationsPreferences.customTerminalCommandProperty(),
                (_, _, newValue) -> put(CONSOLE_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.useCustomFileBrowserProperty(),
                (_, _, newValue) -> putBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION, !newValue)); // mind the !
        EasyBind.listen(externalApplicationsPreferences.customFileBrowserCommandProperty(),
                (_, _, newValue) -> put(FILE_BROWSER_COMMAND, newValue));
        EasyBind.listen(externalApplicationsPreferences.kindleEmailProperty(),
                (_, _, newValue) -> put(KINDLE_EMAIL, newValue));

        return externalApplicationsPreferences;
    }

    private ExternalApplicationsPreferences getExternalApplicationsPreferencesFromBackingStore(ExternalApplicationsPreferences defaults) {
        return new ExternalApplicationsPreferences(
                get(EMAIL_SUBJECT, defaults.getEmailSubject()),
                getBoolean(OPEN_FOLDERS_OF_ATTACHED_FILES, defaults.shouldAutoOpenEmailAttachmentsFolder()),
                ExternalFileTypes.fromString(get(EXTERNAL_FILE_TYPES, ExternalFileTypes.toStringList(defaults.getExternalFileTypes()))),
                !getBoolean(USE_DEFAULT_CONSOLE_APPLICATION, !defaults.useCustomTerminal()), // mind the !
                get(CONSOLE_COMMAND, defaults.getCustomTerminalCommand()),
                !getBoolean(USE_DEFAULT_FILE_BROWSER_APPLICATION, !defaults.useCustomFileBrowser()), // mind the !
                get(FILE_BROWSER_COMMAND, defaults.getCustomFileBrowserCommand()),
                get(KINDLE_EMAIL, defaults.getKindleEmail())
        );
    }

    public GroupsPreferences getGroupsPreferences() {
        if (groupsPreferences != null) {
            return groupsPreferences;
        }

        groupsPreferences = getGroupsPreferencesfromBackingStore(GroupsPreferences.getDefault());

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

    private GroupsPreferences getGroupsPreferencesfromBackingStore(GroupsPreferences defaults) {
        return new GroupsPreferences(
                getBoolean(GROUP_VIEW_INTERSECTION, defaults.groupViewModeProperty().contains(GroupViewMode.INTERSECTION)),
                getBoolean(GROUP_VIEW_FILTER, defaults.groupViewModeProperty().contains(GroupViewMode.FILTER)),
                getBoolean(GROUP_VIEW_INVERT, defaults.groupViewModeProperty().contains(GroupViewMode.INVERT)),
                getBoolean(AUTO_ASSIGN_GROUP, defaults.shouldAutoAssignGroup()),
                getBoolean(DISPLAY_GROUP_COUNT, defaults.shouldDisplayGroupCount()),
                GroupHierarchyType.valueOf(
                        get(DEFAULT_HIERARCHICAL_CONTEXT, defaults.getDefaultHierarchicalContext().name())
                )
        );
    }

    public SpecialFieldsPreferences getSpecialFieldsPreferences() {
        if (specialFieldsPreferences != null) {
            return specialFieldsPreferences;
        }

        specialFieldsPreferences = getSpecialFieldsPreferencesFromBackingStore(SpecialFieldsPreferences.getDefault());

        EasyBind.listen(specialFieldsPreferences.specialFieldsEnabledProperty(), (obs, oldValue, newValue) -> putBoolean(SPECIALFIELDSENABLED, newValue));

        return specialFieldsPreferences;
    }

    private SpecialFieldsPreferences getSpecialFieldsPreferencesFromBackingStore(SpecialFieldsPreferences defaults) {
        return new SpecialFieldsPreferences(
                getBoolean(SPECIALFIELDSENABLED, defaults.isSpecialFieldsEnabled())
        );
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
                                                       .collect(Collectors.toList()),
                getBoolean(COVER_IMAGE_DOWNLOAD)
        );

        previewPreferences.getLayoutCycle().addListener((InvalidationListener) c -> storePreviewLayouts(previewPreferences.getLayoutCycle()));
        EasyBind.listen(previewPreferences.layoutCyclePositionProperty(), (obs, oldValue, newValue) -> putInt(CYCLE_PREVIEW_POS, newValue));
        EasyBind.listen(previewPreferences.customPreviewLayoutProperty(), (obs, oldValue, newValue) -> put(PREVIEW_STYLE, newValue.getText()));
        EasyBind.listen(previewPreferences.showPreviewAsExtraTabProperty(), (obs, oldValue, newValue) -> putBoolean(PREVIEW_AS_TAB, newValue));
        EasyBind.listen(previewPreferences.showPreviewEntryTableTooltip(), (obs, oldValue, newValue) -> putBoolean(PREVIEW_IN_ENTRY_TABLE_TOOLTIP, newValue));
        previewPreferences.getBstPreviewLayoutPaths().addListener((InvalidationListener) c -> storeBstPaths(previewPreferences.getBstPreviewLayoutPaths()));
        EasyBind.listen(previewPreferences.shouldDownloadCoversProperty(), (_, _, newValue) -> putBoolean(COVER_IMAGE_DOWNLOAD, newValue));
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
                        if (CSLStyleUtils.isCitationStyleFile(layout)) {
                            BibEntryTypesManager entryTypesManager = Injector.instantiateModelOrService(BibEntryTypesManager.class);
                            return CSLStyleUtils.createCitationStyleFromFile(layout)
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
                                                 }).toList()
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

        mainTablePreferences = getMainTablePreferencesFromBackingStore(MainTablePreferences.getDefault());

        EasyBind.listen(mainTablePreferences.resizeColumnsToFitProperty(),
                (obs, oldValue, newValue) -> putBoolean(AUTO_RESIZE_MODE, newValue));
        EasyBind.listen(mainTablePreferences.extraFileColumnsEnabledProperty(),
                (obs, oldValue, newValue) -> putBoolean(EXTRA_FILE_COLUMNS, newValue));

        return mainTablePreferences;
    }

    private MainTablePreferences getMainTablePreferencesFromBackingStore(MainTablePreferences defaults) {
        return new MainTablePreferences(
                getMainTableColumnPreferences(),
                getBoolean(AUTO_RESIZE_MODE, defaults.getResizeColumnsToFit()),
                getBoolean(EXTRA_FILE_COLUMNS, defaults.getExtraFileColumnsEnabled())
        );
    }

    public ColumnPreferences getMainTableColumnPreferences() {
        if (mainTableColumnPreferences != null) {
            return mainTableColumnPreferences;
        }

        mainTableColumnPreferences = getMainTableColumnPreferencesFromBackingStore(ColumnPreferences.getDefault());

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

        searchDialogColumnPreferences = getSearchDialogColumnPreferencesFromBackingStore(ColumnPreferences.getDefault());

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

    private ColumnPreferences getMainTableColumnPreferencesFromBackingStore(ColumnPreferences defaults) {
        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, COLUMN_WIDTHS, COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(COLUMN_SORT_ORDER, columns);
        return new ColumnPreferences(
                columns.isEmpty() ? defaults.getColumns() : columns,
                columnSortOrder.isEmpty() ? defaults.getColumnSortOrder() : columnSortOrder
        );
    }

    private ColumnPreferences getSearchDialogColumnPreferencesFromBackingStore(ColumnPreferences defaults) {
        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, SEARCH_DIALOG_COLUMN_WIDTHS, SEARCH_DIALOG_COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(SEARCH_DIALOG_COLUMN_SORT_ORDER, columns);
        return new ColumnPreferences(
                columns.isEmpty() ? defaults.getColumns() : columns,
                columnSortOrder.isEmpty() ? defaults.getColumnSortOrder() : columnSortOrder
        );
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

    @Override
    public NewEntryPreferences getNewEntryPreferences() {
        if (newEntryPreferences != null) {
            return newEntryPreferences;
        }

        newEntryPreferences = getNewEntryPreferencesFromBackingStore(NewEntryPreferences.getDefault());

        EasyBind.listen(newEntryPreferences.latestApproachProperty(), (_, _, newValue) -> putInt(CREATE_ENTRY_APPROACH, List.of(NewEntryDialogTab.values()).indexOf(newValue)));
        EasyBind.listen(newEntryPreferences.typesRecommendedExpandedProperty(), (_, _, newValue) -> putBoolean(CREATE_ENTRY_EXPAND_RECOMMENDED, newValue));
        EasyBind.listen(newEntryPreferences.typesOtherExpandedProperty(), (_, _, newValue) -> putBoolean(CREATE_ENTRY_EXPAND_OTHER, newValue));
        EasyBind.listen(newEntryPreferences.typesCustomExpandedProperty(), (_, _, newValue) -> putBoolean(CREATE_ENTRY_EXPAND_CUSTOM, newValue));
        EasyBind.listen(newEntryPreferences.latestImmediateTypeProperty(), (_, _, newValue) -> put(CREATE_ENTRY_IMMEDIATE_TYPE, newValue.getDisplayName()));
        EasyBind.listen(newEntryPreferences.idLookupGuessingProperty(), (_, _, newValue) -> putBoolean(CREATE_ENTRY_ID_LOOKUP_GUESSING, newValue));
        EasyBind.listen(newEntryPreferences.latestIdFetcherProperty(), (_, _, newValue) -> put(CREATE_ENTRY_ID_FETCHER_NAME, newValue));
        EasyBind.listen(newEntryPreferences.latestInterpretParserProperty(), (_, _, newValue) -> put(CREATE_ENTRY_INTERPRET_PARSER_NAME, newValue));

        return newEntryPreferences;
    }

    private NewEntryPreferences getNewEntryPreferencesFromBackingStore(NewEntryPreferences defaults) {
        final int approachIndex = getInt(CREATE_ENTRY_APPROACH, List.of(NewEntryDialogTab.values()).indexOf(defaults.getLatestApproach()));
        NewEntryDialogTab approach = NewEntryDialogTab.values().length > approachIndex
                                     ? NewEntryDialogTab.values()[approachIndex]
                                     : NewEntryDialogTab.values()[0];

        final String immediateTypeName = get(CREATE_ENTRY_IMMEDIATE_TYPE, defaults.getLatestImmediateType().getDisplayName());
        EntryType immediateType = StandardEntryType.Article;
        for (StandardEntryType type : StandardEntryType.values()) {
            if (type.getDisplayName().equals(immediateTypeName)) {
                immediateType = type;
                break;
            }
        }

        return new NewEntryPreferences(
                approach,
                getBoolean(CREATE_ENTRY_EXPAND_RECOMMENDED, defaults.getTypesRecommendedExpanded()),
                getBoolean(CREATE_ENTRY_EXPAND_OTHER, defaults.getTypesOtherExpanded()),
                getBoolean(CREATE_ENTRY_EXPAND_CUSTOM, defaults.getTypesCustomExpanded()),
                immediateType,
                getBoolean(CREATE_ENTRY_ID_LOOKUP_GUESSING, defaults.getIdLookupGuessing()),
                get(CREATE_ENTRY_ID_FETCHER_NAME, defaults.getLatestIdFetcher()),
                get(CREATE_ENTRY_INTERPRET_PARSER_NAME, defaults.getLatestInterpretParser())
        );
    }

    // region Donation preferences
    public DonationPreferences getDonationPreferences() {
        if (donationPreferences != null) {
            return donationPreferences;
        }

        donationPreferences = getDonationPreferencesFromBackingStore(DonationPreferences.getDefault());

        EasyBind.listen(donationPreferences.neverShowAgainProperty(), (_, _, newValue) -> putBoolean(DONATION_NEVER_SHOW, newValue));
        EasyBind.listen(donationPreferences.lastShownEpochDayProperty(), (_, _, newValue) -> putInt(DONATION_LAST_SHOWN_EPOCH_DAY, newValue.intValue()));
        return donationPreferences;
    }

    private DonationPreferences getDonationPreferencesFromBackingStore(DonationPreferences defaults) {
        return new DonationPreferences(
                getBoolean(DONATION_NEVER_SHOW, defaults.isNeverShowAgain()),
                getInt(DONATION_LAST_SHOWN_EPOCH_DAY, defaults.getLastShownEpochDay()));
    }
    // endregion

    /**
     * In GUI mode, we can look up the directory better
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
