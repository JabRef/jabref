package org.jabref.migrations;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.SequencedMap;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.scene.control.TableColumn;

import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.gui.theme.StyleSheet;
import org.jabref.gui.theme.ThemeColorScheme;
import org.jabref.gui.theme.ThemePreset;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.cleanup.CleanupPreferences;
import org.jabref.logic.cleanup.FieldFormatterCleanupActions;
import org.jabref.logic.os.OS;
import org.jabref.logic.preferences.JabRefCliPreferences;
import org.jabref.logic.preview.TextBasedPreviewLayout;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.strings.StringUtil;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;

import com.github.javakeyring.Keyring;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

public class PreferencesMigrations {

    public static final String V4_0_IMPORT_FILENAME_PATTERN = "importFileNamePattern";

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesMigrations.class);

    /// The names the default tabs were stored under, in every translation shipped between JabRef 3.8.2 and
    /// the 6.0 alphas (harvested from the v3.8.2, v4.3.1, and v5.15 tags plus the current bundles), lower-cased.
    /// Inlined instead of read from the current bundles: translations change and languages get dropped (the
    /// "Review" l10n key is already gone), which would silently stop the migration from recognizing names
    /// stored by old versions. Untranslated languages fell back to the English name.
    private static final Set<String> LEGACY_GENERAL_TAB_NAMES = Set.of(
            "algemeen", "allgemein", "allmänt", "genel", "general", "generale", "generellt", "generelt",
            "geral", "général", "ogólne", "pangkalahatan", "tổng quát", "umum", "yleinen", "γενικά",
            "общие", "一般", "基本设置", "通用", "일반");

    private static final Set<String> LEGACY_ABSTRACT_TAB_NAMES = Set.of(
            "abstract", "abstrak", "abstrakt", "resumen", "resumo", "résumé", "sammanfattning",
            "sammendrag", "sommario", "tóm tắt", "zusammenfassung", "özet", "περίληψη", "абстракція",
            "резюме", "الملخص", "چکیده", "摘要", "概要", "개요");

    private static final Set<String> LEGACY_COMMENTS_TAB_NAMES = Set.of(
            "comentarios", "comentários", "commentaires", "commenti", "comments", "komentar",
            "komentarze", "komento", "kommentare", "kommentarer", "kommentit", "nhận xét",
            "opmerkingen", "yorumlar", "σχόλια", "коментарі", "комментарии", "تعليقات", "نظرات",
            "コメント", "注释", "註解", "코멘트");

    private static final Set<String> LEGACY_REVIEW_TAB_NAMES = Set.of(
            "review", "gözden geçir", "kommentarer", "mag balig-aral", "periksa ulang", "recensie",
            "remarques", "revisar", "rivedi", "überprüfung", "xem xét lại", "просмотр", "論評", "评论");

    private PreferencesMigrations() {
    }

    /// Perform checks and changes for users with a preference set from an older JabRef version.
    public static void runMigrations(JabRefGuiPreferences preferences) {
        Preferences mainPrefsNode = Preferences.userRoot().node("/org/jabref");

        upgradePrefsToOrgJabRef(mainPrefsNode);
        upgradeSortOrder(preferences);
        upgradeLabelPatternToCitationKeyPattern(preferences, mainPrefsNode);
        upgradeImportFileAndDirePatterns(preferences);
        upgradeStoredBibEntryTypes(preferences, mainPrefsNode, preferences.getCustomEntryTypesRepository());
        upgradeKeyBindingsToJavaFX(preferences);
        upgradeMacKeyBindingDefaults(preferences, OS.OS_X);
        addCrossRefRelatedFieldsForAutoComplete(preferences);
        upgradePreviewStyle(preferences);
        upgradeBuiltinPreviewName(preferences);
        // changeColumnVariableNamesFor51 needs to be run before upgradeColumnPre50Preferences to ensure
        // backward compatibility, as it copies the old values to new variable names and keeps th old sored with the old
        // variable names. However, the variables from 5.0 need to be copied to the new variable name too.
        changeColumnVariableNamesFor51(preferences);
        upgradeColumnPreferences(preferences);
        restoreVariablesForBackwardCompatibility(preferences);
        upgradeCleanups(preferences);
        moveApiKeysToKeyring(preferences);
        upgradeResolveBibTeXStringsFields(preferences);
        upgradeTheme(preferences);
        migrateFileAnnotationsTabVisibility(preferences);
        upgradeEntryEditorCustomTabs(preferences);
    }

    /// Up to and including v6.0-alpha.6, custom entry editor tabs were stored in two parallel numbered
    /// series (`customTabName_0`/`customTabFields_0`, ...). Since [#16598](https://github.com/JabRef/jabref/pull/16598)
    /// they are stored as one JSON object, `{"tab name": ["field pattern",...], ...}`. The old keys are
    /// kept in case an old version of JabRef is used with these preferences; they are only read when the
    /// new key does not exist yet.
    static void upgradeEntryEditorCustomTabs(JabRefGuiPreferences prefs) {
        final String V6_0_ALPHA_CUSTOM_TAB_NAME = "customTabName_";
        final String V6_0_ALPHA_CUSTOM_TAB_FIELDS = "customTabFields_";
        final String V6_0_ENTRY_EDITOR_CUSTOM_TABS = "entryEditorCustomTabs";

        if (prefs.get(V6_0_ENTRY_EDITOR_CUSTOM_TABS, null) != null) {
            return;
        }

        SequencedMap<String, List<String>> customTabs = new LinkedHashMap<>();
        String tabName;
        boolean anyStored = false;
        for (int i = 0; (tabName = prefs.get(V6_0_ALPHA_CUSTOM_TAB_NAME + i, null)) != null; i++) {
            anyStored = true;
            List<String> fields = prefs.getStringList(V6_0_ALPHA_CUSTOM_TAB_FIELDS + i);
            if (isLegacyDefaultTab(tabName, fields)) {
                LOGGER.info("Dropping entry editor tab '{}': it only repeats fields of the \"Main\" tab.", tabName);
            } else {
                customTabs.put(tabName, fields);
            }
        }
        if (!anyStored) {
            return;
        }

        LOGGER.info("Migrating {} custom entry editor tab(s) to the JSON preference format.", customTabs.size());
        prefs.put(V6_0_ENTRY_EDITOR_CUSTOM_TABS, new ObjectMapper().writeValueAsString(customTabs));
    }

    /// Whether a pre-v6.0-alpha.7 custom tab is one of the former default tabs "General", "Abstract",
    /// "Comments" (JabRef 5.x), or "Review" (JabRef ≤ 4.x).
    ///
    /// Old versions wrote the default tabs to the store as if the user had defined them (any "Save" in the
    /// preferences dialog and the former per-startup General-fields migration did), so the store cannot
    /// tell defaults from customizations. Since [#12711](https://github.com/JabRef/jabref/issues/12711) the
    /// "Main" tab shows all these fields, so such a tab is pure duplication. A tab counts as a default only
    /// if its name and its fields match as a shipped pair: a translation of "Abstract"/"Comments"/"Review"
    /// (names were stored localized, and not necessarily in the current language) with exactly its shipped
    /// single field, or a translation of "General" with exactly one of the shipped default field sets. A
    /// cross-match (e.g. an "Abstract" tab holding the General fields) is a customization and is kept.
    private static boolean isLegacyDefaultTab(String name, List<String> fields) {
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        // Locale.ROOT: a Turkish UI locale would fold "I" to a dotless "ı" and break the comparison.
        Set<String> stored = fields.stream().map(field -> field.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        if (stored.equals(Set.of(StandardField.ABSTRACT.getName()))) {
            return LEGACY_ABSTRACT_TAB_NAMES.contains(normalizedName);
        }
        if (stored.equals(Set.of(StandardField.COMMENT.getName()))) {
            return LEGACY_COMMENTS_TAB_NAMES.contains(normalizedName);
        }
        if (stored.equals(Set.of(StandardField.REVIEW.getName()))) {
            return LEGACY_REVIEW_TAB_NAMES.contains(normalizedName);
        }
        return legacyGeneralFieldSets().contains(stored) && LEGACY_GENERAL_TAB_NAMES.contains(normalizedName);
    }

    /// The "General" default field set as shipped over time: `comment` variant (JabRef ≤ 4.0), `groups`
    /// variant (4.x); with special fields: 2019 base set, then `doi`, `eprint`, `url` (v5);
    /// `citationcount` and `icore` (v6.0-alpha, in either order); `eprinttype` (v6.0-alpha.5).
    private static Set<Set<String>> legacyGeneralFieldSets() {
        Set<String> v3 = Set.of("crossref", "keywords", "file", "doi", "url", "comment", "owner", "timestamp");
        Set<String> v4 = Set.of("crossref", "keywords", "file", "doi", "url", "groups", "owner", "timestamp");
        Set<String> v5 = EnumSet.allOf(SpecialField.class).stream().map(SpecialField::getName).collect(Collectors.toSet());
        v5.addAll(List.of("crossref", "keywords", "file", "groups", "owner", "timestamp"));
        Set<String> v5_1 = withFields(v5, "doi", "eprint", "url");
        Set<String> alphaCitationCount = withFields(v5_1, "citationcount");
        Set<String> alphaIcore = withFields(v5_1, "icore");
        Set<String> alphaBoth = withFields(alphaCitationCount, "icore");
        return Set.of(v3, v4, v5, v5_1, alphaCitationCount, alphaIcore, alphaBoth, withFields(alphaBoth, "eprinttype"));
    }

    private static Set<String> withFields(Set<String> fields, String... more) {
        Set<String> result = new HashSet<>(fields);
        result.addAll(List.of(more));
        return result;
    }

    /// The legacy key `smartFileAnnotations` toggled a "smart visibility" mode. Mode was adapted for all tabs in
    /// EntryEditor as content driven visibility.
    static void migrateFileAnnotationsTabVisibility(JabRefGuiPreferences prefs) {
        final String V_6_0_SHOW_FILE_ANNOTATIONS = "showFileAnnotations";
        final String LEGACY_SMART_FILE_ANNOTATIONS = "smartFileAnnotations";

        if (prefs.get(V_6_0_SHOW_FILE_ANNOTATIONS, null) == null
                && prefs.get(LEGACY_SMART_FILE_ANNOTATIONS, null) != null) {
            prefs.putBoolean(V_6_0_SHOW_FILE_ANNOTATIONS, true);
        }
    }

    /// Migrate all preferences from net/sf/jabref to org/jabref
    private static void upgradePrefsToOrgJabRef(Preferences mainPrefsNode) {
        try {
            if (mainPrefsNode.childrenNames().length != 0) {
                // skip further processing as prefs already have been migrated
                LOGGER.debug("New prefs node already exists with content - skipping migration");
            } else {
                if (mainPrefsNode.parent().parent().nodeExists("net/sf/jabref")) {
                    LOGGER.info("Migrating old preferences.");
                    Preferences oldNode = mainPrefsNode.parent().parent().node("net/sf/jabref");
                    copyPrefsRecursively(oldNode, mainPrefsNode);
                }
            }
        } catch (BackingStoreException ex) {
            LOGGER.error("Migrating old preferences failed.", ex);
        }
    }

    private static void copyPrefsRecursively(Preferences from, Preferences to) throws BackingStoreException {
        for (String key : from.keys()) {
            String newValue = from.get(key, "");
            if (newValue.contains("net.sf")) {
                newValue = newValue.replace("net.sf", "org");
            }
            to.put(key, newValue);
        }
        for (String child : from.childrenNames()) {
            Preferences childNode = from.node(child);
            Preferences newChildNode = to.node(child);
            copyPrefsRecursively(childNode, newChildNode);
        }
    }

    /// Upgrade the sort order preferences for the current version
    /// The old preference is kept in case an old version of JabRef is used with
    /// these preferences, but it is only used when the new preference does not
    /// exist
    private static void upgradeSortOrder(JabRefCliPreferences prefs) {
        if (prefs.get(JabRefCliPreferences.EXPORT_IN_SPECIFIED_ORDER, null) == null) {
            if (prefs.getBoolean("exportInStandardOrder", false)) {
                prefs.putBoolean(JabRefCliPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefCliPreferences.EXPORT_PRIMARY_SORT_FIELD, StandardField.AUTHOR.getName());
                prefs.put(JabRefCliPreferences.EXPORT_SECONDARY_SORT_FIELD, StandardField.EDITOR.getName());
                prefs.put(JabRefCliPreferences.EXPORT_TERTIARY_SORT_FIELD, StandardField.YEAR.getName());
                prefs.putBoolean(JabRefCliPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefCliPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefCliPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            } else if (prefs.getBoolean("exportInTitleOrder", false)) {
                // exportInTitleOrder => title, author, editor
                prefs.putBoolean(JabRefCliPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefCliPreferences.EXPORT_PRIMARY_SORT_FIELD, StandardField.TITLE.getName());
                prefs.put(JabRefCliPreferences.EXPORT_SECONDARY_SORT_FIELD, StandardField.AUTHOR.getName());
                prefs.put(JabRefCliPreferences.EXPORT_TERTIARY_SORT_FIELD, StandardField.EDITOR.getName());
                prefs.putBoolean(JabRefCliPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefCliPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefCliPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            }
        }
    }

    /// Migrate all customized entry types from versions <=3.7
    private static void upgradeStoredBibEntryTypes(JabRefCliPreferences prefs, Preferences mainPrefsNode, BibEntryTypesManager entryTypesManager) {
        try {
            if (mainPrefsNode.nodeExists(JabRefCliPreferences.CUSTOMIZED_BIBTEX_TYPES) ||
                    mainPrefsNode.nodeExists(JabRefCliPreferences.CUSTOMIZED_BIBLATEX_TYPES)) {
                // skip further processing as prefs already have been migrated
            } else {
                LOGGER.info("Migrating old custom entry types.");
                CustomEntryTypePreferenceMigration.upgradeStoredBibEntryTypes(
                        prefs.getLibraryPreferences().getDefaultBibDatabaseMode(),
                        prefs,
                        entryTypesManager);
            }
        } catch (BackingStoreException ex) {
            LOGGER.error("Migrating old custom entry types failed.", ex);
        }
    }

    /// Migrate LabelPattern configuration from versions <=3.5 to new CitationKeyPatterns.
    ///
    /// Introduced in <a href="https://github.com/JabRef/jabref/pull/1704">#1704</a>
    private static void upgradeLabelPatternToCitationKeyPattern(JabRefCliPreferences prefs, Preferences mainPrefsNode) {
        final String V3_6_DEFAULT_BIBTEX_KEYPATTERN = "defaultBibtexKeyPattern";
        final String V3_6_BIBTEX_KEYPATTERN_NODE = "bibtexkeypatterns";
        final String V3_3_DEFAULT_LABELPATTERN = "defaultLabelPattern";
        final String V3_3_LOGIC_LABELPATTERN = "logic/labelpattern"; // version 3.3 - 3.5, mind the case
        final String V3_0_LOGIC_LABELPATTERN = "logic/labelPattern"; // node used for version 3.0 - 3.2
        final String LEGACY_LABELPATTERN = "labelPattern"; // version <3.0

        try {
            // Migrate default pattern
            if (mainPrefsNode.get(V3_6_DEFAULT_BIBTEX_KEYPATTERN, null) == null) {
                // Check whether old defaultLabelPattern is set
                String oldDefault = mainPrefsNode.get(V3_3_DEFAULT_LABELPATTERN, null);
                if (oldDefault != null) {
                    prefs.put(V3_6_DEFAULT_BIBTEX_KEYPATTERN, oldDefault);
                    LOGGER.info("Upgraded old default key generator pattern '{}' to new version.", oldDefault);
                }
            }
            // Pref node already exists do not migrate from previous version
            if (mainPrefsNode.nodeExists(V3_6_BIBTEX_KEYPATTERN_NODE)) {
                return;
            }

            // Migrate type specific patterns
            if (mainPrefsNode.nodeExists(V3_3_LOGIC_LABELPATTERN)) {
                migrateTypedKeyPrefs(prefs, mainPrefsNode.node(V3_3_LOGIC_LABELPATTERN));
            } else if (mainPrefsNode.nodeExists(V3_0_LOGIC_LABELPATTERN)) {
                migrateTypedKeyPrefs(prefs, mainPrefsNode.node(V3_0_LOGIC_LABELPATTERN));
            } else if (mainPrefsNode.nodeExists(LEGACY_LABELPATTERN)) {
                migrateTypedKeyPrefs(prefs, mainPrefsNode.node(LEGACY_LABELPATTERN));
            }
        } catch (BackingStoreException e) {
            LOGGER.error("Migrating old bibtexKeyPatterns failed.", e);
        }
    }

    static void upgradeResolveBibTeXStringsFields(JabRefCliPreferences prefs) {
        String oldPrefsValue = "author;booktitle;editor;editora;editorb;editorc;institution;issuetitle;journal;journalsubtitle;journaltitle;mainsubtitle;month;publisher;shortauthor;shorteditor;subtitle;titleaddon";
        String currentPrefs = prefs.get(JabRefCliPreferences.RESOLVE_STRINGS_FOR_FIELDS, null);

        if (oldPrefsValue.equals(currentPrefs)) {
            currentPrefs += ";monthfiled";
            prefs.put(JabRefCliPreferences.RESOLVE_STRINGS_FOR_FIELDS, currentPrefs);
        }
    }

    /// Migrate Import File Name and Directory name Patterns from versions <=4.0 to new BracketedPatterns
    static void upgradeImportFileAndDirePatterns(JabRefCliPreferences prefs) {
        if (prefs.hasKey(V4_0_IMPORT_FILENAME_PATTERN)) {
            String[] oldStylePatterns = new String[] {
                    "\\bibtexkey",
                    "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
            String[] newStylePatterns = new String[] {"[citationkey]",
                    "[citationkey] - [title]"};

            String[] oldDisplayStylePattern = new String[] {"bibtexkey", "bibtexkey - title"};

            for (int i = 0; i < oldStylePatterns.length; i++) {
                migrateFileImportPattern(oldStylePatterns[i], newStylePatterns[i], prefs);
            }
            for (int i = 0; i < oldDisplayStylePattern.length; i++) {
                migrateFileImportPattern(oldDisplayStylePattern[i], newStylePatterns[i], prefs);
            }
        }
        // Directory preferences are not yet migrated, since it is not quite clear how to parse and reinterpret
        // the user-defined old-style patterns, and the default pattern is "".
    }

    private static void migrateFileImportPattern(String oldStylePattern,
                                                 String newStylePattern,
                                                 JabRefCliPreferences prefs) {
        String preferenceFileNamePattern = prefs.get(V4_0_IMPORT_FILENAME_PATTERN, null);

        if (oldStylePattern.equals(preferenceFileNamePattern)) {
            // Upgrade the old-style File Name pattern to new one:
            prefs.put(V4_0_IMPORT_FILENAME_PATTERN, newStylePattern);
            LOGGER.info("migrated old style {} value \"{}\" to new value \"{}\" in the preference file", V4_0_IMPORT_FILENAME_PATTERN, oldStylePattern, newStylePattern);

            // Update also the key in the current application settings, if necessary:
            if (oldStylePattern.equals(prefs.getFilePreferences().getFileNamePattern())) {
                prefs.getFilePreferences().setFileNamePattern(newStylePattern);
                LOGGER.info("migrated old style {} value \"{}\" to new value \"{}\" in the running application", V4_0_IMPORT_FILENAME_PATTERN, oldStylePattern, newStylePattern);
            }
        }
    }

    static void upgradeKeyBindingsToJavaFX(JabRefCliPreferences prefs) {
        UnaryOperator<String> replaceKeys = str -> {
            // Legacy bindings use a space before the key (e.g. "ctrl A"); already-migrated
            // bindings use a plus (e.g. "ctrl+A" or "shortcut+A") and must be left untouched,
            // otherwise intentional macOS "ctrl+" bindings would be rewritten to "shortcut+" on every startup.
            boolean isLegacyFormat = str.contains("ctrl ") || str.contains("shift ")
                    || str.contains("alt ") || str.contains("meta ");
            if (!isLegacyFormat) {
                return str;
            }

            String result = str.replace("ctrl ", "shortcut+");
            result = result.replace("shift ", "shift+");
            result = result.replace("alt ", "alt+");
            result = result.replace("meta ", "meta+");

            return result;
        };

        List<String> keys = new ArrayList<>(prefs.getStringList(JabRefGuiPreferences.BINDINGS));
        keys.replaceAll(replaceKeys);
        prefs.putStringList(JabRefGuiPreferences.BINDINGS, keys);
    }

    /// Updates unchanged key bindings in snapshots persisted before macOS-specific defaults were introduced.
    static void upgradeMacKeyBindingDefaults(JabRefCliPreferences prefs, boolean isMacOs) {
        if (!isMacOs || prefs.getBoolean(JabRefGuiPreferences.MACOS_KEY_BINDING_DEFAULTS_MIGRATED, false)) {
            return;
        }

        List<String> bindNames = prefs.getStringList(JabRefGuiPreferences.BIND_NAMES);
        List<String> bindings = prefs.getStringList(JabRefGuiPreferences.BINDINGS);
        if (bindNames.isEmpty() || bindNames.size() != bindings.size()) {
            return;
        }

        List<String> migratedBindings = new ArrayList<>(bindings);
        for (int i = 0; i < bindNames.size(); i++) {
            String bindingName = bindNames.get(i);
            String persistedBinding = bindings.get(i);
            for (KeyBinding keyBinding : KeyBinding.values()) {
                if (keyBinding.getConstant().equals(bindingName)) {
                    int index = i;
                    keyBinding.getMacDefaultReplacement(persistedBinding)
                              .ifPresent(replacement -> migratedBindings.set(index, replacement));
                    break;
                }
            }
        }

        prefs.putStringList(JabRefGuiPreferences.BINDINGS, migratedBindings);
        prefs.putBoolean(JabRefGuiPreferences.MACOS_KEY_BINDING_DEFAULTS_MIGRATED, true);
    }

    private static void addCrossRefRelatedFieldsForAutoComplete(JabRefCliPreferences prefs) {
        // LinkedHashSet because we want to retain the order and add new fields to the end
        String oldPrefs = "author;editor;title;journal;publisher;keywords";
        String newFieldsToAdd = "crossref;related;entryset";
        String currentPrefs = prefs.get(JabRefGuiPreferences.AUTOCOMPLETER_COMPLETE_FIELDS, null);

        if (oldPrefs.equals(currentPrefs)) {
            currentPrefs += ";" + newFieldsToAdd;
            prefs.put(JabRefGuiPreferences.AUTOCOMPLETER_COMPLETE_FIELDS, currentPrefs);
        }
    }

    private static void migrateTypedKeyPrefs(JabRefCliPreferences prefs, Preferences oldPatternPrefs)
            throws BackingStoreException {
        LOGGER.info("Found old Bibtex Key patterns which will be migrated to new version.");

        GlobalCitationKeyPatterns keyPattern = GlobalCitationKeyPatterns.fromPattern(
                prefs.get(JabRefCliPreferences.CITATION_KEY_DEFAULT_PATTERN, null));
        for (String key : oldPatternPrefs.keys()) {
            keyPattern.addCitationKeyPattern(EntryTypeFactory.parse(key), oldPatternPrefs.get(key, null));
        }

        prefs.storeGlobalCitationKeyPattern(keyPattern);
    }

    /// Customizable preview style migrations
    ///
    /// - Since v5.0-alpha the custom preview layout shows the 'comment' field instead of the 'review' field (<a href="https://github.com/JabRef/jabref/pull/4100">#4100</a>).
    /// - Since v5.1 a marker enables markdown in comments (<a href="https://github.com/JabRef/jabref/pull/6232">#6232</a>).
    /// - Since v5.2 'bibtexkey' is rebranded as citationkey (<a href="https://github.com/JabRef/jabref/pull/6875">#6875</a>).
    ///
    protected static void upgradePreviewStyle(JabRefGuiPreferences prefs) {
        String currentPreviewStyle = prefs.get(JabRefGuiPreferences.PREVIEW_STYLE, TextBasedPreviewLayout.DEFAULT);
        String migratedStyle = currentPreviewStyle.replace("\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}", "\\begin{comment}<BR><BR><b>Comment: </b> \\format[Markdown,HTMLChars]{\\comment} \\end{comment}")
                                                  .replace("\\format[HTMLChars]{\\comment}", "\\format[Markdown,HTMLChars]{\\comment}")
                                                  .replace("\\format[Markdown,HTMLChars]{\\comment}", "\\format[Markdown,HTMLChars(keepCurlyBraces)]{\\comment}")
                                                  .replace("\\format[HTMLChars]{\\abstract}", "\\format[LatexToUnicode,HTMLChars]{\\abstract}")
                                                  .replace("<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>", "<b><i>\\bibtextype</i><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>")
                                                  .replace("\\end{bibtexkey}</b><br>__NEWLINE__", "\\end{citationkey}</b><br>__NEWLINE__")
                                                  .replace("\\end{pages}__NEWLINE__\\begin{abstract}", """
                                                          \\end{pages}__NEWLINE__\
                                                          \\begin{doi}<BR>doi <a href="https://doi.org/\\format[DOIStrip]{\\doi}">\\format[DOIStrip]{\\doi}</a>\\end{doi}__NEWLINE__\
                                                          \\begin{url}<BR>URL <a href="\\url">\\url</a>\\end{url}__NEWLINE__\
                                                          \\begin{abstract}\
                                                          """);
        prefs.put(JabRefGuiPreferences.PREVIEW_STYLE, migratedStyle);
    }

    /// Since v6-alpha6 built in preview style will be stored by internal identifier instead by display name.
    protected static void upgradeBuiltinPreviewName(JabRefGuiPreferences prefs) {
        String previewCycle = prefs.get(JabRefGuiPreferences.PREVIEW_CYCLE, "");
        prefs.put(JabRefGuiPreferences.PREVIEW_CYCLE, previewCycle.replace("Customized preview style", TextBasedPreviewLayout.NAME));
    }

    /// The former preferences default of columns was a simple list of strings ("author;title;year;..."). Since 5.0
    /// the preferences store the type of the column too, so that the formerly hardwired columns like the graphic groups
    /// column or the other icon columns can be reordered in the main table and behave like any other field column
    /// ("groups;linked_id;field:author;special:readstatus;extrafile:pdf;...").
    ///
    /// Simple strings are by default parsed as a FieldColumn, so there is nothing to do there, but the formerly hard
    /// wired columns need to be added.
    ///
    /// In 5.1 variable names in JabRefPreferences have changed to offer backward compatibility with pre 5.0 releases
    /// Pre 5.1: columnNames, columnWidths, columnSortTypes, columnSortOrder
    /// Since 5.1: mainTableColumnNames, mainTableColumnWidths, mainTableColumnSortTypes, mainTableColumnSortOrder
    static void upgradeColumnPreferences(JabRefCliPreferences preferences) {
        List<String> columnNames = preferences.getStringList(JabRefGuiPreferences.COLUMN_NAMES);
        List<Double> columnWidths = preferences.getStringList(JabRefGuiPreferences.COLUMN_WIDTHS)
                                               .stream()
                                               .map(string -> {
                                                   try {
                                                       return Double.parseDouble(string);
                                                   } catch (NumberFormatException e) {
                                                       return ColumnPreferences.DEFAULT_COLUMN_WIDTH;
                                                   }
                                               }).toList();

        // "field:"
        String normalFieldTypeString = MainTableColumnModel.Type.NORMALFIELD.getName() + MainTableColumnModel.COLUMNS_QUALIFIER_DELIMITER;

        if (!columnNames.isEmpty() && columnNames.stream().noneMatch(name -> name.contains(normalFieldTypeString))) {
            List<MainTableColumnModel> columns = new ArrayList<>();
            columns.add(new MainTableColumnModel(MainTableColumnModel.Type.GROUPS));
            columns.add(new MainTableColumnModel(MainTableColumnModel.Type.FILES));
            columns.add(new MainTableColumnModel(MainTableColumnModel.Type.LINKED_IDENTIFIER));

            for (int i = 0; i < columnNames.size(); i++) {
                String name = columnNames.get(i);
                double columnWidth = ColumnPreferences.DEFAULT_COLUMN_WIDTH;

                MainTableColumnModel.Type type = SpecialField.fromName(name)
                                                             .map(_ -> MainTableColumnModel.Type.SPECIALFIELD)
                                                             .orElse(MainTableColumnModel.Type.NORMALFIELD);

                if (i < columnWidths.size()) {
                    columnWidth = columnWidths.get(i);
                }

                columns.add(new MainTableColumnModel(type, name, columnWidth));
            }

            preferences.putStringList(JabRefGuiPreferences.COLUMN_NAMES,
                    columns.stream()
                           .map(MainTableColumnModel::getName)
                           .collect(Collectors.toList()));

            preferences.putStringList(JabRefGuiPreferences.COLUMN_WIDTHS,
                    columns.stream()
                           .map(MainTableColumnModel::getWidth)
                           .map(Double::intValue)
                           .map(Object::toString)
                           .collect(Collectors.toList()));

            // ASCENDING by default
            preferences.putStringList(JabRefGuiPreferences.COLUMN_SORT_TYPES,
                    columns.stream()
                           .map(MainTableColumnModel::getSortType)
                           .map(TableColumn.SortType::toString)
                           .collect(Collectors.toList()));
        }
    }

    static void changeColumnVariableNamesFor51(JabRefCliPreferences preferences) {
        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        final String V5_0_COLUMN_NAMES = "columnNames";
        final String V5_0_COLUMN_WIDTHS = "columnWidths";
        final String V5_0_COLUMN_SORT_TYPES = "columnSortTypes";
        final String V5_0_COLUMN_SORT_ORDER = "columnSortOrder";

        final String V5_1_COLUMN_NAMES = "mainTableColumnNames";
        final String V5_1_COLUMN_WIDTHS = "mainTableColumnWidths";
        final String V5_1_COLUMN_SORT_TYPES = "mainTableColumnSortTypes";
        final String V5_1_COLUMN_SORT_ORDER = "mainTableColumnSortOrder";

        List<String> oldColumnNames = preferences.getStringList(V5_0_COLUMN_NAMES);
        List<String> columnNames = preferences.getStringList(V5_1_COLUMN_NAMES);
        if (!oldColumnNames.isEmpty() && columnNames.isEmpty()) {
            preferences.putStringList(V5_1_COLUMN_NAMES, preferences.getStringList(V5_0_COLUMN_NAMES));
            preferences.putStringList(V5_1_COLUMN_WIDTHS, preferences.getStringList(V5_0_COLUMN_WIDTHS));
            preferences.putStringList(V5_1_COLUMN_SORT_TYPES, preferences.getStringList(V5_0_COLUMN_SORT_TYPES));
            preferences.putStringList(V5_1_COLUMN_SORT_ORDER, preferences.getStringList(V5_0_COLUMN_SORT_ORDER));
        }
    }

    /// In 5.0 the format of column names have changed. That made newer versions of JabRef preferences incompatible with
    /// earlier versions of JabRef. As some complains came up, we decided to change the variable names and to clear the
    /// variable contents if they are unreadable, so former versions of JabRef would automatically create preferences
    /// they can deal with.
    static void restoreVariablesForBackwardCompatibility(JabRefCliPreferences preferences) {
        final String V5_0_COLUMN_NAMES = "columnNames";
        final String V5_0_COLUMN_WIDTHS = "columnWidths";
        final String V5_0_COLUMN_SORT_TYPES = "columnSortTypes";
        final String V5_0_COLUMN_SORT_ORDER = "columnSortOrder";
        final String V5_0_MAIN_FONT_SIZE = "mainFontSize";

        List<String> oldColumnNames = preferences.getStringList(V5_0_COLUMN_NAMES);
        List<String> fieldColumnNames = oldColumnNames.stream()
                                                      .filter(columnName -> columnName.startsWith("field:") || columnName.startsWith("special:"))
                                                      .map(columnName -> {
                                                          if (columnName.startsWith("field:")) {
                                                              return columnName.substring(6);
                                                          } else { // special
                                                              return columnName.substring(8);
                                                          }
                                                      }).collect(Collectors.toList());

        if (!fieldColumnNames.isEmpty()) {
            preferences.putStringList(V5_0_COLUMN_NAMES, fieldColumnNames);

            List<String> fieldColumnWidths = new ArrayList<>(List.of());
            for (int i = 0; i < fieldColumnNames.size(); i++) {
                fieldColumnWidths.add("100");
            }
            preferences.putStringList(V5_0_COLUMN_WIDTHS, fieldColumnWidths);

            preferences.put(V5_0_COLUMN_SORT_TYPES, "");
            preferences.put(V5_0_COLUMN_SORT_ORDER, "");
        }

        // Ensure font size is a parsable int variable
        try {
            // some versions stored the font size as double to the **same** key
            // since the preference store is type-safe, we need to add this workaround
            String fontSizeAsString = preferences.get(V5_0_MAIN_FONT_SIZE, null);
            if (fontSizeAsString == null) {
                return;
            }

            int fontSizeAsInt = (int) Math.round(Double.parseDouble(fontSizeAsString));
            preferences.putInt(V5_0_MAIN_FONT_SIZE, fontSizeAsInt);
        } catch (ClassCastException e) {
            // already an integer
        }
    }

    /// In version 6.0 the formatting of the CleanUps preferences changed. Instead of using several keys that have have a variable name a single preference key is introduced containing just the active cleanup jobs. Also instead of a combined field for the field formatters and the enabled status of all of them, they are split for easier parsing.
    ///
    /// <h3>Changes:</h3>
    /// <table>
    /// <tr> <td>                key                     </td> <td>  value </td> </tr>
    /// <tr> <td colspan="2">    CLEANUP - old format:   </td> </tr>
    /// <tr> <td> CleanUpCLEAN_UP_DOI    </td> <td>  enabled </td> </tr>
    /// <tr> <td> CleanUpRENAME_PDF      </td> <td>  disabled </td> </tr>
    /// <tr> <td> CleanUpMOVE_PDF        </td> <td>  enabled<br>
    /// <tr> <td colspan="2"> ... </td> </tr>
    /// <tr> <td> &nbsp; </td> </tr>
    /// <tr> <td colspan="2"> CLEANUP_JOBS - new format: </td> </tr>
    /// <tr> <td> CleanUpJobs            </td> <td> `CLEAN_UP_DOI;RENAME_PDF;MOVE_PDF `</td> </tr>
    /// <tr> <td> &nbsp; </td> </tr>
    /// <tr> <td colspan="2"> CLEANUP_FORMATTERS - old format: </td> </tr>
    /// <tr> <td> CleanUpFormatters     </td> <td> `ENABLED\nfield[formatter,formatter...]\nfield[...]\nfield[...]... `</td> </tr>
    /// <tr> <td> &nbsp; </td> </tr>
    /// <tr> <td colspan="2"> CLEANUP_FORMATTERS - new format: </td> </tr>
    /// <tr> <td> CleanUpFormattersEnabled </td> <td> TRUE </td> </tr>
    /// <tr> <td> CleanUpFormatters        </td> <td> `field[formatter,formatter...]\nfield[...]\nfield[...]... `</td> </tr>
    /// </table>
    static void upgradeCleanups(JabRefCliPreferences prefs) {
        final String V5_8_CLEANUP = "CleanUp";
        final String V6_0_CLEANUP_JOBS = "CleanUpJobs";
        final String V6_0_CLEANUP_REMOVED_ISSN = "CLEAN_UP_ISSN";

        final String V5_8_CLEANUP_FIELD_FORMATTERS = "CleanUpFormatters";
        final String V6_0_CLEANUP_FIELD_FORMATTERS = "CleanUpFormatters";
        final String V6_0_CLEANUP_FIELD_FORMATTERS_ENABLED = "CleanUpFormattersEnabled";

        if (prefs.hasKey(V6_0_CLEANUP_JOBS)) {
            List<String> cleanupJobs = prefs.getStringList(V6_0_CLEANUP_JOBS);
            if (cleanupJobs.contains(V6_0_CLEANUP_REMOVED_ISSN)) {
                prefs.putStringList(V6_0_CLEANUP_JOBS,
                        cleanupJobs.stream()
                                   .filter(job -> !V6_0_CLEANUP_REMOVED_ISSN.equals(job))
                                   .toList());
            }
        }

        List<String> activeJobs = new ArrayList<>();
        for (CleanupPreferences.CleanupStep action : EnumSet.allOf(CleanupPreferences.CleanupStep.class)) {
            Optional<String> job = Optional.ofNullable(prefs.get(V5_8_CLEANUP + action.name(), null));
            if (job.isPresent() && Boolean.parseBoolean(job.get())) {
                activeJobs.add(action.name());
                // prefs.deleteKey(V5_8_CLEANUP + action.name()); // for backward compatibility in comments
            }
        }
        if (!activeJobs.isEmpty()) {
            prefs.put(V6_0_CLEANUP_JOBS, String.join(";", activeJobs));
        }

        List<String> formatterCleanups = List.of(StringUtil.unifyLineBreaks(prefs.get(V5_8_CLEANUP_FIELD_FORMATTERS, ""), "\n")
                                                           .split("\n"));
        if (formatterCleanups.size() >= 2
                && (FieldFormatterCleanupActions.ENABLED.equals(formatterCleanups.getFirst())
                || FieldFormatterCleanupActions.DISABLED.equals(formatterCleanups.getFirst()))) {
            prefs.putBoolean(V6_0_CLEANUP_FIELD_FORMATTERS_ENABLED, FieldFormatterCleanupActions.ENABLED.equals(formatterCleanups.getFirst())
                                                                    ? Boolean.TRUE
                                                                    : Boolean.FALSE);

            prefs.put(V6_0_CLEANUP_FIELD_FORMATTERS, String.join(OS.NEWLINE, formatterCleanups.subList(1, formatterCleanups.size() - 1)));
        }
    }

    static void moveApiKeysToKeyring(JabRefCliPreferences preferences) {
        final String V5_9_FETCHER_CUSTOM_KEY_NAMES = "fetcherCustomKeyNames";
        final String V5_9_FETCHER_CUSTOM_KEYS = "fetcherCustomKeys";

        List<String> names = preferences.getStringList(V5_9_FETCHER_CUSTOM_KEY_NAMES);
        List<String> keys = preferences.getStringList(V5_9_FETCHER_CUSTOM_KEYS);

        if (!keys.isEmpty() && names.size() == keys.size()) {
            try (final Keyring keyring = Keyring.create()) {
                for (int i = 0; i < names.size(); i++) {
                    keyring.setPassword("org.jabref.customapikeys", names.get(i), new Password(
                            keys.get(i),
                            preferences.getInternalPreferences().getUserHostInfo().getUserHostString())
                            .encrypt());
                }
                preferences.deleteKey(V5_9_FETCHER_CUSTOM_KEYS);
            } catch (Exception ex) {
                LOGGER.error("Unable to open key store", ex);
            }
        }
    }

    /// upgrade the old theme css names of the theme to the new theme properties
    /// Theme names were changed in [#15573](https://github.com/JabRef/jabref/pull/15573)
    ///
    /// The old keys are deleted after reading them, so the migration runs at most once --
    /// migrations run on every startup, and re-applying the old value each time would
    /// overwrite whatever the user selected in the new UI in the meantime.
    static void upgradeTheme(JabRefGuiPreferences preferences) {
        String theme = preferences.get("fxTheme", null);
        // The old preference defaulted to syncing with the OS color scheme
        boolean themeSyncOs = preferences.getBoolean("themeSyncOs", true);

        if (theme != null) {
            preferences.deleteKey("fxTheme");
        }
        if (preferences.get("themeSyncOs", null) != null) {
            preferences.deleteKey("themeSyncOs");
        }

        if (theme == null) {
            // Fresh install, or already migrated: keep the new defaults (follow the system color scheme)
            return;
        }

        if ("".equals(theme) && themeSyncOs) {
            // Old default behavior: follow the OS color scheme -- matches the new defaults
            return;
        }

        // no value means light theme when sync with os theme switch is not on
        if ("".equals(theme) || "Base.css".equals(theme) || "light".equals(theme)) {
            preferences.getWorkspacePreferences().setTheme(ThemePreset.JABREF);
            preferences.getWorkspacePreferences().setColorScheme(ThemeColorScheme.LIGHT);

            return;
        }

        if ("Dark.css".equals(theme) || "dark".equals(theme)) {
            preferences.getWorkspacePreferences().setTheme(ThemePreset.JABREF);
            preferences.getWorkspacePreferences().setColorScheme(ThemeColorScheme.DARK);

            return;
        }

        preferences.getWorkspacePreferences().setCustomTheme(StyleSheet.create(theme));
    }
}
