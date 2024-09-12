package org.jabref.gui.preferences;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import javafx.beans.InvalidationListener;
import javafx.collections.ListChangeListener;
import javafx.collections.SetChangeListener;

import org.jabref.gui.CoreGuiPreferences;
import org.jabref.gui.WorkspacePreferences;
import org.jabref.gui.autocompleter.AutoCompletePreferences;
import org.jabref.gui.duplicationFinder.DuplicateResolverDialog;
import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.gui.externalfiles.UnlinkedFilesDialogPreferences;
import org.jabref.gui.externalfiletype.ExternalFileType;
import org.jabref.gui.externalfiletype.ExternalFileTypes;
import org.jabref.gui.frame.ExternalApplicationsPreferences;
import org.jabref.gui.frame.SidePanePreferences;
import org.jabref.gui.groups.GroupViewMode;
import org.jabref.gui.groups.GroupsPreferences;
import org.jabref.gui.mergeentries.DiffMode;
import org.jabref.gui.mergeentries.MergeDialogPreferences;
import org.jabref.gui.sidepane.SidePaneType;
import org.jabref.gui.theme.Theme;
import org.jabref.logic.externalfiles.DateRange;
import org.jabref.logic.externalfiles.ExternalFileSorter;
import org.jabref.logic.importer.fetcher.DoiFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.AutoCompleteFirstNameMode;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.push.CitationCommandString;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.util.io.FileHistory;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JabRefGuiPreferences extends JabRefCliPreferences implements GuiPreferences {

    // Public because needed for pref migration
    public static final String AUTOCOMPLETER_COMPLETE_FIELDS = "autoCompleteFields";
    public static final String MAIN_FONT_SIZE = "mainFontSize";

    private static final Logger LOGGER = LoggerFactory.getLogger(JabRefCliPreferences.class);

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
    // endregion

    private static final String RECENT_DATABASES = "recentDatabases";

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

    private static final String LAST_FOCUSED = "lastFocused";
    private static final String ID_ENTRY_GENERATOR = "idEntryGenerator";
    private static final String SELECTED_SLR_CATALOGS = "selectedSlrCatalogs";
    private static final String UNLINKED_FILES_SELECTED_EXTENSION = "unlinkedFilesSelectedExtension";
    private static final String UNLINKED_FILES_SELECTED_DATE_RANGE = "unlinkedFilesSelectedDateRange";
    private static final String UNLINKED_FILES_SELECTED_SORT = "unlinkedFilesSelectedSort";

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

        // region coreGuiPreferences
        // Set DOI to be the default ID entry generator
        defaults.put(ID_ENTRY_GENERATOR, DoiFetcher.NAME);
        defaults.put(RECENT_DATABASES, "");
        defaults.put(LAST_FOCUSED, "");
        // endregion

        // region workspace
        defaults.put(MAIN_FONT_SIZE, 9);
        defaults.put(OVERRIDE_DEFAULT_FONT_SIZE, false);
        defaults.put(OPEN_LAST_EDITED, Boolean.TRUE);
        defaults.put(THEME, Theme.BASE_CSS);
        defaults.put(THEME_SYNC_OS, Boolean.FALSE);
        defaults.put(CONFIRM_DELETE, Boolean.TRUE);
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
    }

    @Deprecated
    public static JabRefGuiPreferences getInstance() {
        if (JabRefGuiPreferences.singleton == null) {
            JabRefGuiPreferences.singleton = new JabRefGuiPreferences();
        }
        return JabRefGuiPreferences.singleton;
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
                getStringList(LAST_EDITED).stream()
                                          .map(Path::of)
                                          .collect(Collectors.toList()),
                Path.of(get(LAST_FOCUSED)),
                getFileHistory(),
                get(ID_ENTRY_GENERATOR),
                getDouble(SIDE_PANE_WIDTH));

        EasyBind.listen(coreGuiPreferences.positionXProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_POS_X, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.positionYProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_POS_Y, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.sizeXProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_WIDTH, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.sizeYProperty(), (obs, oldValue, newValue) -> putDouble(MAIN_WINDOW_HEIGHT, newValue.doubleValue()));
        EasyBind.listen(coreGuiPreferences.windowMaximisedProperty(), (obs, oldValue, newValue) -> putBoolean(WINDOW_MAXIMISED, newValue));
        EasyBind.listen(coreGuiPreferences.sidePaneWidthProperty(), (obs, oldValue, newValue) -> putDouble(SIDE_PANE_WIDTH, newValue.doubleValue()));

        coreGuiPreferences.getLastFilesOpened().addListener((ListChangeListener<Path>) change -> {
            if (change.getList().isEmpty()) {
                remove(LAST_EDITED);
            } else {
                putStringList(LAST_EDITED, coreGuiPreferences.getLastFilesOpened().stream()
                                                             .map(Path::toAbsolutePath)
                                                             .map(Path::toString)
                                                             .collect(Collectors.toList()));
            }
        });
        EasyBind.listen(coreGuiPreferences.lastFocusedFileProperty(), (obs, oldValue, newValue) -> {
            if (newValue != null) {
                put(LAST_FOCUSED, newValue.toAbsolutePath().toString());
            } else {
                remove(LAST_FOCUSED);
            }
        });
        coreGuiPreferences.getFileHistory().addListener((InvalidationListener) change -> storeFileHistory(coreGuiPreferences.getFileHistory()));
        EasyBind.listen(coreGuiPreferences.lastSelectedIdBasedFetcherProperty(), (obs, oldValue, newValue) -> put(ID_ENTRY_GENERATOR, newValue));

        return coreGuiPreferences;
    }

    private FileHistory getFileHistory() {
        return FileHistory.of(getStringList(RECENT_DATABASES).stream()
                                                             .map(Path::of)
                                                             .toList());
    }

    private void storeFileHistory(FileHistory history) {
        putStringList(RECENT_DATABASES, history.stream()
                                               .map(Path::toAbsolutePath)
                                               .map(Path::toString)
                                               .toList());
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
}
