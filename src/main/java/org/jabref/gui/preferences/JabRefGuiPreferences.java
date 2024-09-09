package org.jabref.gui.preferences;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.gui.entryeditor.EntryEditorPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.strings.StringUtil;

import com.tobiasdiez.easybind.EasyBind;

public class JabRefGuiPreferences extends JabRefCliPreferences implements GuiPreferences {

    private static final String ENTRY_EDITOR_HEIGHT = "entryEditorHeightFX";

    /**
     * Holds the horizontal divider position of the preview view when it is shown inside the entry editor
     */
    private static final String ENTRY_EDITOR_PREVIEW_DIVIDER_POS = "entryEditorPreviewDividerPos";

    private static final String JOURNAL_POPUP = "journalPopup";

    private static JabRefGuiPreferences singleton;

    private EntryEditorPreferences entryEditorPreferences;

    private JabRefGuiPreferences() {
        super();

        defaults.put(JOURNAL_POPUP, EntryEditorPreferences.JournalPopupEnabled.FIRST_START.toString());

        defaults.put(ENTRY_EDITOR_HEIGHT, 0.65);
        defaults.put(ENTRY_EDITOR_PREVIEW_DIVIDER_POS, 0.5);
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
}
