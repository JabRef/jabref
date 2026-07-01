package org.jabref.gui.preferences;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;
import javafx.scene.control.TableColumn;

import org.jabref.gui.CoreGuiPreferences;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.edit.CopyToPreferences;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.entryeditor.EntryEditorTabModel;
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
import org.jabref.logic.citationstyle.CSLStyleLoader;
import org.jabref.logic.exporter.BibDatabaseWriter;
import org.jabref.logic.exporter.ExportPreferences;
import org.jabref.logic.exporter.SelfContainedSaveConfiguration;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.importer.fetcher.MrDlibPreferences;
import org.jabref.logic.importer.fetcher.citation.CitationCountFetcherType;
import org.jabref.logic.importer.fetcher.citation.CitationFetcherType;
import org.jabref.logic.journals.JournalAbbreviationRepository;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.preferences.AutoCompleteFirstNameMode;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.preview.CitationStylePreviewLayout;
import org.jabref.logic.preview.PreviewLayout;
import org.jabref.logic.preview.TextBasedPreviewLayout;
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
    public static final String PREVIEW_CYCLE_POS = "cyclePreviewPos";
    public static final String PREVIEW_CYCLE = "cyclePreview";
    public static final String PREVIEW_AS_TAB = "previewAsTab";
    public static final String PREVIEW_IN_ENTRY_TABLE_TOOLTIP = "previewInEntryTableTooltip";
    public static final String PREVIEW_BST_LAYOUT_PATHS = "previewBstLayoutPaths";
    public static final String PREVIEW_COVER_IMAGE_DOWNLOAD = "coverDownload";
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
    private static final String GROUP_SHOW_AI_CHAT = "groupShowAiChat";
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

    // region EntryEditorPreferences
    // The tab list is persisted across the many SHOW_* flags and the CUSTOM_TAB_* series below; this synthetic key is
    // never written to the backing store and only serves as the tabModels binding's reporting key in
    // getPreferences()/getDefaults() (see bindMap/PUSH_APPLICATIONS_PATHS_KEY for the same pattern).
    private static final String ENTRY_EDITOR_TABS = "entryEditorTabs";
    private static final String CUSTOM_TAB_NAME = "customTabName_";
    private static final String CUSTOM_TAB_FIELDS = "customTabFields_";
    private static final String AUTO_OPEN_FORM = "autoOpenForm";
    private static final String SHOW_REQUIRED_FIELDS = "showRequiredFields";
    private static final String SHOW_IMPORTANT_OPTIONAL_FIELDS = "showImportantOptionalFields";
    private static final String SHOW_DETAIL_OPTIONAL_FIELDS = "showDetailOptionalFields";
    private static final String SHOW_DEPRECATED_FIELDS = "showDeprecatedFields";
    private static final String SHOW_OTHER_FIELDS = "showOtherFields";
    private static final String SHOW_RECOMMENDATIONS = "showRecommendations";
    private static final String SHOW_AI_SUMMARY = "showAiSummary";
    private static final String SHOW_AI_CHAT = "showAiChat";
    private static final String SHOW_LATEX_CITATIONS = "showLatexCitations";
    private static final String SHOW_FILE_ANNOTATIONS = "showFileAnnotations";
    private static final String DEFAULT_SHOW_SOURCE = "defaultShowSource";
    private static final String VALIDATE_IN_ENTRY_EDITOR = "validateInEntryEditor";
    private static final String ALLOW_INTEGER_EDITION_BIBTEX = "allowIntegerEditionBibtex";
    private static final String AUTOLINK_FILES_ENABLED = "autoLinkFilesEnabled";
    private static final String JOURNAL_POPUP = "journalPopup";
    private static final String SHOW_SCITE_TAB = "showSciteTab";
    private static final String SHOW_USER_COMMENTS_FIELDS = "showUserCommentsFields";
    private static final String SHOW_COMMENTS_TAB = "showCommentsTab";
    private static final String SHOW_MATHSCINET_TAB = "showMathSciNetTab";
    private static final String SHOW_SOURCE_TAB = "showSourceTab";
    private static final String SHOW_FULLTEXT_SEARCH_TAB = "showFulltextSearchTab";
    private static final String ENTRY_EDITOR_PREVIEW_DIVIDER_POS = "entryEditorPreviewDividerPos";
    private static final String CITATION_FETCHER_TYPE = "citationFetcherType";
    private static final String CITATION_COUNT_FETCHER_TYPE = "citationCountFetcherType";
    // endregion

    // region MrDlibPreferences
    private static final String MRDLIB_ACCEPT_RECOMMENDATIONS = "acceptRecommendations";
    private static final String MRDLIB_SEND_LANGUAGE_DATA = "sendLanguageData";
    private static final String MRDLIB_SEND_OS_DATA = "sendOSData";
    private static final String MRDLIB_SEND_TIMEZONE_DATA = "sendTimezoneData";
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
    private MrDlibPreferences mrDlibPreferences;

    /// @deprecated Never ever add a call to this method. There should be only one caller.
    /// All other usages should get the preferences passed (or injected).
    /// The JabRef team leaves the `@deprecated` annotation to have IntelliJ listing this method with a strike-through.
    @Deprecated
    public static JabRefGuiPreferences getInstance() {
        if (JabRefGuiPreferences.singleton == null) {
            JabRefGuiPreferences.singleton = new JabRefGuiPreferences();
        }
        return JabRefGuiPreferences.singleton;
    }

    @Override
    public void clear() throws BackingStoreException {
        // ensure registration of bindings
        getCopyToPreferences();
        getEntryEditorPreferences();
        getMergeDialogPreferences();
        getAutoCompletePreferences();

        super.clear();

        getDonationPreferences().setAll(DonationPreferences.getDefault());
        getGroupsPreferences().setAll(GroupsPreferences.getDefault());
        getGuiPreferences().setAll(CoreGuiPreferences.getDefault());
        getSpecialFieldsPreferences().setAll(SpecialFieldsPreferences.getDefault());
        getExternalApplicationsPreferences().setAll(ExternalApplicationsPreferences.getDefault());
        getMainTableColumnPreferences().setAll(ColumnPreferences.getDefault());
        getMainTablePreferences().setAll(MainTablePreferences.getDefault());
        getNewEntryPreferences().setAll(NewEntryPreferences.getDefault());
        getSearchDialogColumnPreferences().setAll(ColumnPreferences.getDefault());
        getUnlinkedFilesDialogPreferences().setAll(UnlinkedFilesDialogPreferences.getDefault());
        getWorkspacePreferences().setAll(WorkspacePreferences.getDefault());
        getSidePanePreferences().setAll(SidePanePreferences.getDefault());
        getNameDisplayPreferences().setAll(NameDisplayPreferences.getDefault());
        getPreviewPreferences().setAll(PreviewPreferences.getDefaultWithStyles(
                getLayoutFormatterPreferences(),
                Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                Injector.instantiateModelOrService(BibEntryTypesManager.class)));
        getMrDlibPreferences().setAll(MrDlibPreferences.getDefault());
    }

    @Override
    public void importPreferences(Path path) throws JabRefException {
        // ensure registration of bindings
        getCopyToPreferences();
        getEntryEditorPreferences();
        getMergeDialogPreferences();
        getAutoCompletePreferences();

        super.importPreferences(path);

        // in case of incomplete or corrupt xml fall back to current preferences
        getDonationPreferences().setAll(getDonationPreferencesFromBackingStore(getDonationPreferences()));
        getGroupsPreferences().setAll(getGroupsPreferencesFromBackingStore(getGroupsPreferences()));
        getGuiPreferences().setAll(getCoreGuiPreferencesFromBackingStore(getGuiPreferences()));
        getSpecialFieldsPreferences().setAll(getSpecialFieldsPreferencesFromBackingStore(getSpecialFieldsPreferences()));
        getExternalApplicationsPreferences().setAll(getExternalApplicationsPreferencesFromBackingStore(getExternalApplicationsPreferences()));
        getMainTableColumnPreferences().setAll(getMainTableColumnPreferencesFromBackingStore(getMainTableColumnPreferences()));
        getMainTablePreferences().setAll(getMainTablePreferencesFromBackingStore(getMainTablePreferences()));
        getNewEntryPreferences().setAll(getNewEntryPreferencesFromBackingStore(getNewEntryPreferences()));
        getSearchDialogColumnPreferences().setAll(getSearchDialogColumnPreferencesFromBackingStore(getSearchDialogColumnPreferences()));
        getUnlinkedFilesDialogPreferences().setAll(getUnlinkedFilesDialogPreferences());
        getWorkspacePreferences().setAll(getWorkspacePreferencesFromBackingStore(getWorkspacePreferences()));
        getSidePanePreferences().setAll(getSidePanePreferencesFromBackingStore(getSidePanePreferences()));
        getNameDisplayPreferences().setAll(getNameDisplayPreferencesFromBackingStore(getNameDisplayPreferences()));
        getPreviewPreferences().setAll(getPreviewPreferencesFromBackingStore(getPreviewPreferences()));
        getMrDlibPreferences().setAll(getMrDlibPreferencesFromBackingStore(getMrDlibPreferences()));
    }

    // region CopyToPreferences
    public CopyToPreferences getCopyToPreferences() {
        if (copyToPreferences != null) {
            return copyToPreferences;
        }

        CopyToPreferences defaultValues = CopyToPreferences.getDefault();

        copyToPreferences = new CopyToPreferences(
                getBoolean(ASK_FOR_INCLUDING_CROSS_REFERENCES, defaultValues.getShouldAskForIncludingCrossReferences()),
                getBoolean(INCLUDE_CROSS_REFERENCES, defaultValues.getShouldIncludeCrossReferences()));

        bindBoolean(copyToPreferences.shouldAskForIncludingCrossReferencesProperty(), ASK_FOR_INCLUDING_CROSS_REFERENCES, defaultValues.getShouldAskForIncludingCrossReferences());
        bindBoolean(copyToPreferences.shouldIncludeCrossReferencesProperty(), INCLUDE_CROSS_REFERENCES, defaultValues.getShouldIncludeCrossReferences());

        return copyToPreferences;
    }
    // endregion

    // region EntryEditorPreferences
    public EntryEditorPreferences getEntryEditorPreferences() {
        if (entryEditorPreferences != null) {
            return entryEditorPreferences;
        }

        EntryEditorPreferences defaultValues = EntryEditorPreferences.getDefault();

        entryEditorPreferences = new EntryEditorPreferences(
                getEntryEditorTabs(defaultValues),
                getBoolean(AUTO_OPEN_FORM, defaultValues.shouldOpenOnNewEntry()),
                getBoolean(DEFAULT_SHOW_SOURCE, defaultValues.showSourceTabByDefault()),
                getBoolean(VALIDATE_IN_ENTRY_EDITOR, defaultValues.shouldEnableValidation()),
                getBoolean(ALLOW_INTEGER_EDITION_BIBTEX, defaultValues.shouldAllowIntegerEditionBibtex()),
                getBoolean(AUTOLINK_FILES_ENABLED, defaultValues.autoLinkFilesEnabled()),
                EntryEditorPreferences.JournalPopupEnabled.fromString(get(JOURNAL_POPUP, defaultValues.shouldEnableJournalPopup().name())),
                CitationFetcherType.valueOf(get(CITATION_FETCHER_TYPE, defaultValues.getCitationFetcherType().name())),
                CitationCountFetcherType.valueOf(get(CITATION_COUNT_FETCHER_TYPE, defaultValues.getCitationCountFetcherType().name())),
                getBoolean(SHOW_USER_COMMENTS_FIELDS, defaultValues.shouldShowUserCommentsFields()),
                getDouble(ENTRY_EDITOR_PREVIEW_DIVIDER_POS, defaultValues.getPreviewWidthDividerPosition())
        );

        bindCustomList(entryEditorPreferences.getTabModels(), ENTRY_EDITOR_TABS, defaultValues.getTabModels(),
                this::storeTabConfigs, () -> getEntryEditorTabs(defaultValues));
        bindBoolean(entryEditorPreferences.shouldOpenOnNewEntryProperty(), AUTO_OPEN_FORM, defaultValues.shouldOpenOnNewEntry());
        bindBoolean(entryEditorPreferences.showSourceTabByDefaultProperty(), DEFAULT_SHOW_SOURCE, defaultValues.showSourceTabByDefault());
        bindBoolean(entryEditorPreferences.enableValidationProperty(), VALIDATE_IN_ENTRY_EDITOR, defaultValues.shouldEnableValidation());
        bindBoolean(entryEditorPreferences.allowIntegerEditionBibtexProperty(), ALLOW_INTEGER_EDITION_BIBTEX, defaultValues.shouldAllowIntegerEditionBibtex());
        bindBoolean(entryEditorPreferences.autoLinkEnabledProperty(), AUTOLINK_FILES_ENABLED, defaultValues.autoLinkFilesEnabled());
        bindObject(entryEditorPreferences.enableJournalPopupProperty(), JOURNAL_POPUP, defaultValues.shouldEnableJournalPopup(),
                EntryEditorPreferences.JournalPopupEnabled::name, EntryEditorPreferences.JournalPopupEnabled::fromString);
        bindDouble(entryEditorPreferences.previewWidthDividerPositionProperty(), ENTRY_EDITOR_PREVIEW_DIVIDER_POS, defaultValues.getPreviewWidthDividerPosition());
        bindObject(entryEditorPreferences.citationFetcherTypeProperty(), CITATION_FETCHER_TYPE, defaultValues.getCitationFetcherType(),
                CitationFetcherType::name, CitationFetcherType::valueOf);
        bindBoolean(entryEditorPreferences.showUserCommentsFieldsProperty(), SHOW_USER_COMMENTS_FIELDS, defaultValues.shouldShowUserCommentsFields());
        bindObject(entryEditorPreferences.citationCountFetcherTypeProperty(), CITATION_COUNT_FETCHER_TYPE, defaultValues.getCitationCountFetcherType(),
                CitationCountFetcherType::name, CitationCountFetcherType::valueOf);

        return entryEditorPreferences;
    }

    /// The single source of truth for the entry editor's tab list: the user-configured field-set tabs,
    /// followed by every static (built-in) tab's visibility flag, in {@link EntryEditorTabModel.BuiltIn} order.
    private List<EntryEditorTabModel> getEntryEditorTabs(EntryEditorPreferences defaults) {
        List<EntryEditorTabModel> tabModels = new ArrayList<>();

        // Always-present leading tab. Its visibility is owned by PreviewPreferences (PREVIEW_AS_TAB /
        // showPreviewAsExtraTab) — the single source the factory gates on — so the model bit is a constant
        // and is not persisted separately here.
        tabModels.add(new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.PREVIEW, true));

        tabModels.addAll(List.<EntryEditorTabModel>of(
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.REQUIRED_FIELDS,
                        getBoolean(SHOW_REQUIRED_FIELDS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.REQUIRED_FIELDS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.IMPORTANT_OPTIONAL_FIELDS,
                        getBoolean(SHOW_IMPORTANT_OPTIONAL_FIELDS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.IMPORTANT_OPTIONAL_FIELDS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.DETAIL_OPTIONAL_FIELDS,
                        getBoolean(SHOW_DETAIL_OPTIONAL_FIELDS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.DETAIL_OPTIONAL_FIELDS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.DEPRECATED_FIELDS,
                        getBoolean(SHOW_DEPRECATED_FIELDS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.DEPRECATED_FIELDS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.OTHER_FIELDS,
                        getBoolean(SHOW_OTHER_FIELDS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.OTHER_FIELDS)))
        ));

        Map<String, Set<Field>> tabNamesToFields = new LinkedHashMap<>();
        List<String> tabNames = getSeries(CUSTOM_TAB_NAME);
        List<String> tabFields = getSeries(CUSTOM_TAB_FIELDS);
        if (tabNames.isEmpty() || (tabNames.size() != tabFields.size())) {
            tabNamesToFields = EntryEditorPreferences.getDefaultEntryEditorTabs();
        } else {
            for (int i = 0; i < tabNames.size(); i++) {
                tabNamesToFields.put(tabNames.get(i), FieldFactory.parseFieldList(tabFields.get(i)));
            }
        }
        tabNamesToFields.forEach((name, fields) ->
                tabModels.add(new EntryEditorTabModel.CustomizedFieldsTab(name, fields)));

        tabModels.addAll(List.<EntryEditorTabModel>of(
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.RELATED_ARTICLES,
                        getBoolean(SHOW_RECOMMENDATIONS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.RELATED_ARTICLES))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.AI_SUMMARY,
                        getBoolean(SHOW_AI_SUMMARY, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.AI_SUMMARY))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.AI_CHAT,
                        getBoolean(SHOW_AI_CHAT, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.AI_CHAT))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.LATEX_CITATIONS,
                        getBoolean(SHOW_LATEX_CITATIONS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.LATEX_CITATIONS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.FILE_ANNOTATIONS,
                        getBoolean(SHOW_FILE_ANNOTATIONS, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.FILE_ANNOTATIONS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.CITATION_INFORMATION,
                        getBoolean(SHOW_SCITE_TAB, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.CITATION_INFORMATION))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.COMMENTS,
                        getBoolean(SHOW_COMMENTS_TAB, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.COMMENTS))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.MATH_SCI_NET,
                        getBoolean(SHOW_MATHSCINET_TAB, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.MATH_SCI_NET))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.SOURCE,
                        getBoolean(SHOW_SOURCE_TAB, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.SOURCE))),
                new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.FULLTEXT_SEARCH_RESULTS,
                        getBoolean(SHOW_FULLTEXT_SEARCH_TAB, defaults.isTabVisible(EntryEditorTabModel.BuiltIn.FULLTEXT_SEARCH_RESULTS)))
        ));

        return tabModels;
    }

    private void storeTabConfigs(List<EntryEditorTabModel> configs) {
        List<EntryEditorTabModel.CustomizedFieldsTab> fieldSetTabs = configs.stream()
                                                                            .filter(EntryEditorTabModel.CustomizedFieldsTab.class::isInstance)
                                                                            .map(EntryEditorTabModel.CustomizedFieldsTab.class::cast)
                                                                            .toList();

        for (int i = 0; i < fieldSetTabs.size(); i++) {
            put(CUSTOM_TAB_NAME + i, fieldSetTabs.get(i).name());
            put(CUSTOM_TAB_FIELDS + i, fieldSetTabs.get(i).fields().stream()
                                                   .map(Field::getName)
                                                   .collect(Collectors.joining(STRINGLIST_DELIMITER.toString())));
        }
        purgeSeries(CUSTOM_TAB_NAME, fieldSetTabs.size());
        purgeSeries(CUSTOM_TAB_FIELDS, fieldSetTabs.size());

        for (EntryEditorTabModel config : configs) {
            if (config instanceof EntryEditorTabModel.BuiltInTab(
                    EntryEditorTabModel.BuiltIn type,
                    boolean visible
            )) {
                switch (type) {
                    case PREVIEW -> {
                        // Preview-tab visibility is stored as showPreviewAsExtraTab in PreviewPreferences; nothing to persist here.
                    }
                    case REQUIRED_FIELDS ->
                            putBoolean(SHOW_REQUIRED_FIELDS, visible);
                    case IMPORTANT_OPTIONAL_FIELDS ->
                            putBoolean(SHOW_IMPORTANT_OPTIONAL_FIELDS, visible);
                    case DETAIL_OPTIONAL_FIELDS ->
                            putBoolean(SHOW_DETAIL_OPTIONAL_FIELDS, visible);
                    case DEPRECATED_FIELDS ->
                            putBoolean(SHOW_DEPRECATED_FIELDS, visible);
                    case OTHER_FIELDS ->
                            putBoolean(SHOW_OTHER_FIELDS, visible);
                    case RELATED_ARTICLES ->
                            putBoolean(SHOW_RECOMMENDATIONS, visible);
                    case AI_SUMMARY ->
                            putBoolean(SHOW_AI_SUMMARY, visible);
                    case AI_CHAT ->
                            putBoolean(SHOW_AI_CHAT, visible);
                    case FILE_ANNOTATIONS ->
                            putBoolean(SHOW_FILE_ANNOTATIONS, visible);
                    case LATEX_CITATIONS ->
                            putBoolean(SHOW_LATEX_CITATIONS, visible);
                    case CITATION_INFORMATION ->
                            putBoolean(SHOW_SCITE_TAB, visible);
                    case COMMENTS ->
                            putBoolean(SHOW_COMMENTS_TAB, visible);
                    case MATH_SCI_NET ->
                            putBoolean(SHOW_MATHSCINET_TAB, visible);
                    case SOURCE ->
                            putBoolean(SHOW_SOURCE_TAB, visible);
                    case FULLTEXT_SEARCH_RESULTS ->
                            putBoolean(SHOW_FULLTEXT_SEARCH_TAB, visible);
                }
            }
        }
    }
    // endregion

    // region MergeDialogPreferences
    @Override
    public MergeDialogPreferences getMergeDialogPreferences() {
        if (mergeDialogPreferences != null) {
            return mergeDialogPreferences;
        }

        MergeDialogPreferences defaultValues = MergeDialogPreferences.getDefault();

        mergeDialogPreferences = new MergeDialogPreferences(
                DiffMode.valueOf(get(MERGE_ENTRIES_DIFF_MODE, defaultValues.getMergeDiffMode().name())),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_DIFF, defaultValues.getMergeShouldShowDiff()),
                getBoolean(MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, defaultValues.getMergeShouldShowUnifiedDiff()),
                getBoolean(MERGE_ENTRIES_HIGHLIGHT_WORDS, defaultValues.getMergeHighlightWords()),
                getBoolean(MERGE_SHOW_ONLY_CHANGED_FIELDS, defaultValues.shouldMergeShowChangedFieldsOnly()),
                getBoolean(MERGE_APPLY_TO_ALL_ENTRIES, defaultValues.shouldMergeApplyToAllEntries()),
                DuplicateResolverDialog.DuplicateResolverResult.valueOf(
                        get(DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES, defaultValues.getAllEntriesDuplicateResolverDecision().name()))
        );

        bindObject(mergeDialogPreferences.mergeDiffModeProperty(), MERGE_ENTRIES_DIFF_MODE, defaultValues.getMergeDiffMode(),
                DiffMode::name, DiffMode::valueOf);
        bindBoolean(mergeDialogPreferences.mergeShouldShowDiffProperty(), MERGE_ENTRIES_SHOULD_SHOW_DIFF, defaultValues.getMergeShouldShowDiff());
        bindBoolean(mergeDialogPreferences.mergeShouldShowUnifiedDiffProperty(), MERGE_ENTRIES_SHOULD_SHOW_UNIFIED_DIFF, defaultValues.getMergeShouldShowUnifiedDiff());
        bindBoolean(mergeDialogPreferences.mergeHighlightWordsProperty(), MERGE_ENTRIES_HIGHLIGHT_WORDS, defaultValues.getMergeHighlightWords());
        bindBoolean(mergeDialogPreferences.mergeShowChangedFieldOnlyProperty(), MERGE_SHOW_ONLY_CHANGED_FIELDS, defaultValues.shouldMergeShowChangedFieldsOnly());
        bindBoolean(mergeDialogPreferences.mergeApplyToAllEntriesProperty(), MERGE_APPLY_TO_ALL_ENTRIES, defaultValues.shouldMergeApplyToAllEntries());
        bindObject(mergeDialogPreferences.allEntriesDuplicateResolverDecisionProperty(), DUPLICATE_RESOLVER_DECISION_RESULT_ALL_ENTRIES, defaultValues.getAllEntriesDuplicateResolverDecision(),
                DuplicateResolverDialog.DuplicateResolverResult::name, DuplicateResolverDialog.DuplicateResolverResult::valueOf);

        return mergeDialogPreferences;
    }
    // endregion

    // region AutoCompletePreferences
    @Override
    public AutoCompletePreferences getAutoCompletePreferences() {
        if (autoCompletePreferences != null) {
            return autoCompletePreferences;
        }

        AutoCompletePreferences defaultValues = AutoCompletePreferences.getDefault();

        autoCompletePreferences = new AutoCompletePreferences(
                getBoolean(AUTO_COMPLETE, defaultValues.shouldAutoComplete()),
                AutoCompleteFirstNameMode.parse(get(AUTOCOMPLETER_FIRSTNAME_MODE, defaultValues.getFirstNameMode().name())),
                readExclusiveFlags(defaultValues.getNameFormat(), AutoCompletePreferences.NameFormat.BOTH,
                        Map.entry(AUTOCOMPLETER_LAST_FIRST, AutoCompletePreferences.NameFormat.LAST_FIRST),
                        Map.entry(AUTOCOMPLETER_FIRST_LAST, AutoCompletePreferences.NameFormat.FIRST_LAST)),
                getFieldSequencedSet(AUTOCOMPLETER_COMPLETE_FIELDS, defaultValues.getCompleteFields()));

        bindBoolean(autoCompletePreferences.autoCompleteProperty(), AUTO_COMPLETE, defaultValues.shouldAutoComplete());
        bindObject(autoCompletePreferences.firstNameModeProperty(), AUTOCOMPLETER_FIRSTNAME_MODE, defaultValues.getFirstNameMode(),
                AutoCompleteFirstNameMode::name, AutoCompleteFirstNameMode::parse);
        // NameFormat.BOTH is the implicit value (both flags stored false); LAST_FIRST wins over FIRST_LAST on the
        // non-canonical "both true" state, per readExclusiveFlags.
        bindExclusiveFlags(autoCompletePreferences.nameFormatProperty(), defaultValues.getNameFormat(), AutoCompletePreferences.NameFormat.BOTH,
                Map.entry(AUTOCOMPLETER_LAST_FIRST, AutoCompletePreferences.NameFormat.LAST_FIRST),
                Map.entry(AUTOCOMPLETER_FIRST_LAST, AutoCompletePreferences.NameFormat.FIRST_LAST));
        bindSet(autoCompletePreferences.getCompleteFields(), AUTOCOMPLETER_COMPLETE_FIELDS, defaultValues.getCompleteFields(),
                set -> putStringList(AUTOCOMPLETER_COMPLETE_FIELDS, set.stream().map(Field::getName).collect(Collectors.toList())),
                () -> getFieldSequencedSet(AUTOCOMPLETER_COMPLETE_FIELDS, defaultValues.getCompleteFields()));

        return autoCompletePreferences;
    }
    // endregion

    // region (core) GuiPreferences
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

    // region WorkspacePreferences
    @Override
    public WorkspacePreferences getWorkspacePreferences() {
        if (workspacePreferences != null) {
            return workspacePreferences;
        }

        workspacePreferences = getWorkspacePreferencesFromBackingStore(WorkspacePreferences.getDefault());

        EasyBind.listen(workspacePreferences.languageProperty(), (_, oldValue, newValue) -> {
            put(LANGUAGE, newValue.getId());
            if (oldValue != newValue) {
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
                new Theme(get(THEME, Theme.SYSTEM)),
                getBoolean(THEME_SYNC_OS, defaults.shouldThemeSyncOs()),
                getBoolean(OPEN_LAST_EDITED, defaults.shouldOpenLastEdited()),
                getBoolean(SHOW_ADVANCED_HINTS, defaults.shouldShowAdvancedHints()),
                getBoolean(CONFIRM_DELETE, defaults.shouldConfirmDelete()),
                getBoolean(CONFIRM_HIDE_TAB_BAR, defaults.shouldHideTabBar()),
                getStringList(SELECTED_SLR_CATALOGS));
    }
    // endregion

    // region UnlinkedFilesDialogPreferences
    @Override
    public UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferences() {
        if (unlinkedFilesDialogPreferences != null) {
            return unlinkedFilesDialogPreferences;
        }

        unlinkedFilesDialogPreferences = getUnlinkedFilesDialogPreferencesFromBackingStore(UnlinkedFilesDialogPreferences.getDefault());

        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedExtensionProperty(), (_, _, newValue) -> put(UNLINKED_FILES_SELECTED_EXTENSION, newValue));
        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedDateRangeProperty(), (_, _, newValue) -> put(UNLINKED_FILES_SELECTED_DATE_RANGE, newValue.name()));
        EasyBind.listen(unlinkedFilesDialogPreferences.unlinkedFilesSelectedSortProperty(), (_, _, newValue) -> put(UNLINKED_FILES_SELECTED_SORT, newValue.name()));

        return unlinkedFilesDialogPreferences;
    }

    private UnlinkedFilesDialogPreferences getUnlinkedFilesDialogPreferencesFromBackingStore(UnlinkedFilesDialogPreferences defaults) {
        return new UnlinkedFilesDialogPreferences(
                get(UNLINKED_FILES_SELECTED_EXTENSION, defaults.getUnlinkedFilesSelectedExtension()),
                DateRange.parse(get(UNLINKED_FILES_SELECTED_DATE_RANGE, defaults.getUnlinkedFilesSelectedDateRange().name())),
                ExternalFileSorter.parse(get(UNLINKED_FILES_SELECTED_SORT, defaults.getUnlinkedFilesSelectedSort().name()))
        );
    }
    // endregion

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
                backingStoreVisiblePanes,
                backingStorePreferredPositions,
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
            LOGGER.debug("SidePane preferred positions corrupt or empty, falling back to default");
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

    // region ExternalApplicationsPreferences
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
        externalApplicationsPreferences.getExternalFileTypes().addListener((SetChangeListener<ExternalFileType>) _ ->
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
    // endregion

    // region GroupsPreferences
    public GroupsPreferences getGroupsPreferences() {
        if (groupsPreferences != null) {
            return groupsPreferences;
        }

        groupsPreferences = getGroupsPreferencesFromBackingStore(GroupsPreferences.getDefault());

        groupsPreferences.groupViewModeProperty().addListener((SetChangeListener<GroupViewMode>) _ -> {
            putBoolean(GROUP_VIEW_INTERSECTION, groupsPreferences.groupViewModeProperty().contains(GroupViewMode.INTERSECTION));
            putBoolean(GROUP_VIEW_FILTER, groupsPreferences.groupViewModeProperty().contains(GroupViewMode.FILTER));
            putBoolean(GROUP_VIEW_INVERT, groupsPreferences.groupViewModeProperty().contains(GroupViewMode.INVERT));
        });
        EasyBind.listen(groupsPreferences.autoAssignGroupProperty(), (_, _, newValue) -> putBoolean(AUTO_ASSIGN_GROUP, newValue));
        EasyBind.listen(groupsPreferences.displayGroupCountProperty(), (_, _, newValue) -> putBoolean(DISPLAY_GROUP_COUNT, newValue));
        EasyBind.listen(groupsPreferences.defaultHierarchicalContextProperty(), (_, _, newValue) -> put(DEFAULT_HIERARCHICAL_CONTEXT, newValue.name()));
        EasyBind.listen(groupsPreferences.showAiChatButtonProperty(), (_, _, newValue) -> putBoolean(GROUP_SHOW_AI_CHAT, newValue));

        return groupsPreferences;
    }

    private GroupsPreferences getGroupsPreferencesFromBackingStore(GroupsPreferences defaults) {
        return new GroupsPreferences(
                getBoolean(GROUP_VIEW_INTERSECTION, defaults.groupViewModeProperty().contains(GroupViewMode.INTERSECTION)),
                getBoolean(GROUP_VIEW_FILTER, defaults.groupViewModeProperty().contains(GroupViewMode.FILTER)),
                getBoolean(GROUP_VIEW_INVERT, defaults.groupViewModeProperty().contains(GroupViewMode.INVERT)),
                getBoolean(AUTO_ASSIGN_GROUP, defaults.shouldAutoAssignGroup()),
                getBoolean(DISPLAY_GROUP_COUNT, defaults.shouldDisplayGroupCount()),
                GroupHierarchyType.valueOf(
                        get(DEFAULT_HIERARCHICAL_CONTEXT, defaults.getDefaultHierarchicalContext().name())
                ),
                getBoolean(GROUP_SHOW_AI_CHAT, defaults.showAiChatButton())
        );
    }
    // endregion

    // region SpecialFieldsPreferences
    public SpecialFieldsPreferences getSpecialFieldsPreferences() {
        if (specialFieldsPreferences != null) {
            return specialFieldsPreferences;
        }

        specialFieldsPreferences = getSpecialFieldsPreferencesFromBackingStore(SpecialFieldsPreferences.getDefault());

        EasyBind.listen(specialFieldsPreferences.specialFieldsEnabledProperty(), (_, _, newValue) -> putBoolean(SPECIALFIELDSENABLED, newValue));

        return specialFieldsPreferences;
    }

    private SpecialFieldsPreferences getSpecialFieldsPreferencesFromBackingStore(SpecialFieldsPreferences defaults) {
        return new SpecialFieldsPreferences(
                getBoolean(SPECIALFIELDSENABLED, defaults.isSpecialFieldsEnabled())
        );
    }
    // endregion

    // region PreviewPreferences
    public PreviewPreferences getPreviewPreferences() {
        if (previewPreferences != null) {
            return previewPreferences;
        }

        this.previewPreferences = getPreviewPreferencesFromBackingStore(PreviewPreferences.getDefault());

        previewPreferences.getLayoutCycle().addListener((InvalidationListener) _ -> putStringList(PREVIEW_CYCLE, previewLayoutsToStrings(previewPreferences.getLayoutCycle())));
        EasyBind.listen(previewPreferences.layoutCyclePositionProperty(), (_, _, newValue) -> putInt(PREVIEW_CYCLE_POS, newValue));
        // must be stored with __NEWLINE__ instead of \n so that our migration correctly triggers, in getText it will be replaced by \n
        EasyBind.listen(previewPreferences.customPreviewLayoutProperty(), (_, _, newValue) -> put(PREVIEW_STYLE, newValue.replace("\n", "__NEWLINE__")));
        EasyBind.listen(previewPreferences.showPreviewAsExtraTabProperty(), (_, _, newValue) -> putBoolean(PREVIEW_AS_TAB, newValue));
        EasyBind.listen(previewPreferences.showPreviewEntryTableTooltip(), (_, _, newValue) -> putBoolean(PREVIEW_IN_ENTRY_TABLE_TOOLTIP, newValue));
        previewPreferences.getBstPreviewLayoutPaths().addListener((InvalidationListener) _ -> storeBstPaths(previewPreferences.getBstPreviewLayoutPaths()));
        EasyBind.listen(previewPreferences.shouldDownloadCoversProperty(), (_, _, newValue) -> putBoolean(PREVIEW_COVER_IMAGE_DOWNLOAD, newValue));
        return this.previewPreferences;
    }

    private PreviewPreferences getPreviewPreferencesFromBackingStore(PreviewPreferences defaults) {
        // Mutable lists required
        String customPreviewLayout = get(PREVIEW_STYLE, defaults.getCustomPreviewLayout());
        List<PreviewLayout> layouts = getPreviewLayouts(getStringList(PREVIEW_CYCLE), customPreviewLayout);
        if (layouts.isEmpty()) {
            layouts = new ArrayList<>(defaults.getLayoutCycle());
        }

        List<Path> bstPaths;
        if (hasKey(PREVIEW_BST_LAYOUT_PATHS)) {
            bstPaths = getStringList(PREVIEW_BST_LAYOUT_PATHS).stream().map(Path::of).collect(Collectors.toList());
        } else {
            bstPaths = new ArrayList<>(defaults.getBstPreviewLayoutPaths());
        }

        return new PreviewPreferences(
                layouts,
                getPreviewCyclePosition(layouts, getInt(PREVIEW_CYCLE_POS, defaults.getLayoutCyclePosition())),
                customPreviewLayout,
                getBoolean(PREVIEW_AS_TAB, defaults.shouldShowPreviewAsExtraTab()),
                getBoolean(PREVIEW_IN_ENTRY_TABLE_TOOLTIP, defaults.shouldShowPreviewEntryTableTooltip()),
                bstPaths,
                getBoolean(PREVIEW_COVER_IMAGE_DOWNLOAD, defaults.shouldDownloadCovers())
        );
    }

    private void storeBstPaths(List<Path> bstPaths) {
        putStringList(PREVIEW_BST_LAYOUT_PATHS, bstPaths.stream().map(Path::toAbsolutePath).map(Path::toString).toList());
    }

    private List<PreviewLayout> getPreviewLayouts(List<String> cycle, String customPreviewLayout) {
        // For backwards compatibility always add at least the default preview to the cycle
        if (cycle.isEmpty()) {
            cycle.addAll(List.of(TextBasedPreviewLayout.NAME, CSLStyleLoader.DEFAULT_STYLE));
        }

        return cycle.stream()
                    .map(layout -> PreviewLayout.of(
                            layout,
                            customPreviewLayout,
                            getStringList(PREVIEW_BST_LAYOUT_PATHS).stream().map(Path::of).toList(),
                            getLayoutFormatterPreferences(),
                            Injector.instantiateModelOrService(JournalAbbreviationRepository.class),
                            Injector.instantiateModelOrService(BibEntryTypesManager.class))
                    ).filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }

    private List<String> previewLayoutsToStrings(List<PreviewLayout> previewCycle) {
        return previewCycle.stream()
                           .map(layout -> {
                               if (layout instanceof CitationStylePreviewLayout citationStyleLayout) {
                                   return citationStyleLayout.getFilePath();
                               } else {
                                   return layout.getName();
                               }
                           }).toList();
    }

    private int getPreviewCyclePosition(List<PreviewLayout> layouts, int defaultPosition) {
        int storedCyclePos = getInt(PREVIEW_CYCLE_POS, defaultPosition);
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

        nameDisplayPreferences = getNameDisplayPreferencesFromBackingStore(NameDisplayPreferences.getDefault());

        EasyBind.listen(nameDisplayPreferences.displayStyleProperty(), (_, _, newValue) -> {
            putBoolean(NAMES_NATBIB, newValue == NameDisplayPreferences.DisplayStyle.NATBIB);
            putBoolean(NAMES_AS_IS, newValue == NameDisplayPreferences.DisplayStyle.AS_IS);
            putBoolean(NAMES_FIRST_LAST, newValue == NameDisplayPreferences.DisplayStyle.FIRSTNAME_LASTNAME);
        });
        EasyBind.listen(nameDisplayPreferences.abbreviationStyleProperty(), (_, _, newValue) -> {
            putBoolean(ABBR_AUTHOR_NAMES, newValue == NameDisplayPreferences.AbbreviationStyle.FULL);
            putBoolean(NAMES_LAST_ONLY, newValue == NameDisplayPreferences.AbbreviationStyle.LASTNAME_ONLY);
        });

        return nameDisplayPreferences;
    }

    private NameDisplayPreferences getNameDisplayPreferencesFromBackingStore(NameDisplayPreferences defaults) {
        return new NameDisplayPreferences(
                getNameDisplayStyleFromBackingStore(defaults),
                getNameAbbreviationStyleFromBackingStore(defaults)
        );
    }

    private NameDisplayPreferences.DisplayStyle getNameDisplayStyleFromBackingStore(NameDisplayPreferences defaults) {
        if (!hasKey(NAMES_NATBIB) && !hasKey(NAMES_AS_IS) && !hasKey(NAMES_FIRST_LAST)) {
            return defaults.getDisplayStyle();
        }

        if (getBoolean(NAMES_NATBIB, false)) {
            return NameDisplayPreferences.DisplayStyle.NATBIB;
        }
        if (getBoolean(NAMES_AS_IS, false)) {
            return NameDisplayPreferences.DisplayStyle.AS_IS;
        }
        if (getBoolean(NAMES_FIRST_LAST, false)) {
            return NameDisplayPreferences.DisplayStyle.FIRSTNAME_LASTNAME;
        }

        return NameDisplayPreferences.DisplayStyle.LASTNAME_FIRSTNAME;
    }

    private NameDisplayPreferences.AbbreviationStyle getNameAbbreviationStyleFromBackingStore(NameDisplayPreferences defaults) {
        if (!hasKey(ABBR_AUTHOR_NAMES) && !hasKey(NAMES_LAST_ONLY)) {
            return defaults.getAbbreviationStyle();
        }

        if (getBoolean(ABBR_AUTHOR_NAMES, false)) {
            return NameDisplayPreferences.AbbreviationStyle.FULL;
        }
        if (getBoolean(NAMES_LAST_ONLY, false)) {
            return NameDisplayPreferences.AbbreviationStyle.LASTNAME_ONLY;
        }

        return NameDisplayPreferences.AbbreviationStyle.NONE;
    }
    // endregion

    // region MainTablePreferences
    public MainTablePreferences getMainTablePreferences() {
        if (mainTablePreferences != null) {
            return mainTablePreferences;
        }

        mainTablePreferences = getMainTablePreferencesFromBackingStore(MainTablePreferences.getDefault());

        EasyBind.listen(mainTablePreferences.resizeColumnsToFitProperty(),
                (_, _, newValue) -> putBoolean(AUTO_RESIZE_MODE, newValue));
        EasyBind.listen(mainTablePreferences.extraFileColumnsEnabledProperty(),
                (_, _, newValue) -> putBoolean(EXTRA_FILE_COLUMNS, newValue));

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

        mainTableColumnPreferences.getColumns().addListener((InvalidationListener) _ -> {
            putStringList(COLUMN_NAMES, getColumnNamesAsStringList(mainTableColumnPreferences));
            putStringList(COLUMN_WIDTHS, getColumnWidthsAsStringList(mainTableColumnPreferences));
            putStringList(COLUMN_SORT_TYPES, getColumnSortTypesAsStringList(mainTableColumnPreferences));
        });
        mainTableColumnPreferences.getColumnSortOrder().addListener((InvalidationListener) _ ->
                putStringList(COLUMN_SORT_ORDER, getColumnSortOrderAsStringList(mainTableColumnPreferences)));

        return mainTableColumnPreferences;
    }

    private ColumnPreferences getMainTableColumnPreferencesFromBackingStore(ColumnPreferences defaults) {
        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, COLUMN_WIDTHS, COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(COLUMN_SORT_ORDER, columns);
        return new ColumnPreferences(
                columns.isEmpty() ? defaults.getColumns() : columns,
                columnSortOrder.isEmpty() ? defaults.getColumnSortOrder() : columnSortOrder
        );
    }
    // endregion

    // region SearchDialogColumnPreferences
    public ColumnPreferences getSearchDialogColumnPreferences() {
        if (searchDialogColumnPreferences != null) {
            return searchDialogColumnPreferences;
        }

        searchDialogColumnPreferences = getSearchDialogColumnPreferencesFromBackingStore(ColumnPreferences.getDefault());

        searchDialogColumnPreferences.getColumns().addListener((InvalidationListener) _ -> {
            // MainTable and SearchResultTable use the same set of columnNames
            // putStringList(SEARCH_DIALOG_COLUMN_NAMES, getColumnNamesAsStringList(columnPreferences));
            putStringList(SEARCH_DIALOG_COLUMN_WIDTHS, getColumnWidthsAsStringList(searchDialogColumnPreferences));
            putStringList(SEARCH_DIALOG_COLUMN_SORT_TYPES, getColumnSortTypesAsStringList(searchDialogColumnPreferences));
        });
        searchDialogColumnPreferences.getColumnSortOrder().addListener((InvalidationListener) _ ->
                putStringList(SEARCH_DIALOG_COLUMN_SORT_ORDER, getColumnSortOrderAsStringList(searchDialogColumnPreferences)));

        return searchDialogColumnPreferences;
    }

    private ColumnPreferences getSearchDialogColumnPreferencesFromBackingStore(ColumnPreferences defaults) {
        List<MainTableColumnModel> columns = getColumns(COLUMN_NAMES, SEARCH_DIALOG_COLUMN_WIDTHS, SEARCH_DIALOG_COLUMN_SORT_TYPES, ColumnPreferences.DEFAULT_COLUMN_WIDTH);
        List<MainTableColumnModel> columnSortOrder = getColumnSortOrder(SEARCH_DIALOG_COLUMN_SORT_ORDER, columns);
        return new ColumnPreferences(
                columns.isEmpty() ? defaults.getColumns() : columns,
                columnSortOrder.isEmpty() ? defaults.getColumnSortOrder() : columnSortOrder
        );
    }
    // endregion

    // region generic column handling
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

    // region NewEntryPreferences
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
    // endregion

    // region DonationPreferences
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

    // region MrDlibPreferences
    @Override
    public MrDlibPreferences getMrDlibPreferences() {
        if (mrDlibPreferences != null) {
            return mrDlibPreferences;
        }

        mrDlibPreferences = getMrDlibPreferencesFromBackingStore(MrDlibPreferences.getDefault());

        EasyBind.listen(mrDlibPreferences.acceptRecommendationsProperty(), (_, _, newValue) -> putBoolean(MRDLIB_ACCEPT_RECOMMENDATIONS, newValue));
        EasyBind.listen(mrDlibPreferences.sendLanguageProperty(), (_, _, newValue) -> putBoolean(MRDLIB_SEND_LANGUAGE_DATA, newValue));
        EasyBind.listen(mrDlibPreferences.sendOsProperty(), (_, _, newValue) -> putBoolean(MRDLIB_SEND_OS_DATA, newValue));
        EasyBind.listen(mrDlibPreferences.sendTimezoneProperty(), (_, _, newValue) -> putBoolean(MRDLIB_SEND_TIMEZONE_DATA, newValue));

        return mrDlibPreferences;
    }

    private MrDlibPreferences getMrDlibPreferencesFromBackingStore(MrDlibPreferences defaults) {
        return new MrDlibPreferences(
                getBoolean(MRDLIB_ACCEPT_RECOMMENDATIONS, defaults.shouldAcceptRecommendations()),
                getBoolean(MRDLIB_SEND_LANGUAGE_DATA, defaults.shouldSendLanguage()),
                getBoolean(MRDLIB_SEND_OS_DATA, defaults.shouldSendOs()),
                getBoolean(MRDLIB_SEND_TIMEZONE_DATA, defaults.shouldSendTimezone()));
    }
    // endregion

    @Override
    public SelfContainedSaveConfiguration getSelfContainedExportConfiguration() {
        SaveOrder exportSaveOrder = getExportSaveOrder(ExportPreferences.getDefault().getExportSaveOrder());
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

    /// For the export configuration, generates the SelfContainedSaveOrder having the reference to TABLE resolved.
    private SelfContainedSaveOrder getSelfContainedTableSaveOrder() {
        List<MainTableColumnModel> sortOrder = getMainTableColumnPreferences().getColumnSortOrder();
        return new SelfContainedSaveOrder(
                SaveOrder.OrderType.SPECIFIED,
                sortOrder.stream().flatMap(model -> model.getSortCriteria().stream()).toList());
    }

    @Override
    public KeyBindingRepository getKeyBindingRepository() {
        if (keyBindingRepository != null) {
            return keyBindingRepository;
        }

        keyBindingRepository = new KeyBindingRepository(getStringList(BIND_NAMES), getStringList(BINDINGS));

        EasyBind.listen(keyBindingRepository.getBindingsProperty(), (_, _, _) -> {
            putStringList(BIND_NAMES, keyBindingRepository.getBindNames());
            putStringList(BINDINGS, keyBindingRepository.getBindings());
        });

        return keyBindingRepository;
    }

    /// In GUI mode, we can look up the directory better
    @Override
    protected Path getDefaultPath() {
        return NativeDesktop.get().getDefaultFileChooserDirectory();
    }

    @Override
    protected boolean moveToTrashSupported() {
        return NativeDesktop.get().moveToTrashSupported();
    }
}
