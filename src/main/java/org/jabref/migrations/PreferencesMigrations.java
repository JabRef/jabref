package org.jabref.migrations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.scene.control.TableColumn;

import org.jabref.gui.maintable.ColumnPreferences;
import org.jabref.gui.maintable.MainTableColumnModel;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPattern;
import org.jabref.logic.cleanup.FieldFormatterCleanups;
import org.jabref.logic.util.OS;
import org.jabref.model.entry.field.SpecialField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.EntryTypeFactory;
import org.jabref.model.strings.StringUtil;
import org.jabref.preferences.CleanupPreferences;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreferencesMigrations {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreferencesMigrations.class);

    private PreferencesMigrations() {
    }

    /**
     * Perform checks and changes for users with a preference set from an older JabRef version.
     */
    public static void runMigrations(JabRefPreferences preferences) {
        Preferences mainPrefsNode = Preferences.userRoot().node("/org/jabref");

        upgradePrefsToOrgJabRef(mainPrefsNode);
        upgradeSortOrder(preferences);
        upgradeFaultyEncodingStrings(preferences);
        upgradeLabelPatternToCitationKeyPattern(preferences, mainPrefsNode);
        upgradeImportFileAndDirePatterns(preferences, mainPrefsNode);
        upgradeStoredBibEntryTypes(preferences, mainPrefsNode);
        upgradeKeyBindingsToJavaFX(preferences);
        addCrossRefRelatedFieldsForAutoComplete(preferences);
        upgradePreviewStyleFromReviewToComment(preferences);
        // changeColumnVariableNamesFor51 needs to be run before upgradeColumnPre50Preferences to ensure
        // backwardcompatibility, as it copies the old values to new variable names and keeps th old sored with the old
        // variable names. However, the variables from 5.0 need to be copied to the new variable name too.
        changeColumnVariableNamesFor51(preferences);
        upgradeColumnPreferences(preferences);
        restoreVariablesForBackwardCompatibility(preferences);
        upgradePreviewStyleAllowMarkdown(preferences);
        upgradeCleanups(preferences);
    }

    /**
     * Migrate all preferences from net/sf/jabref to org/jabref
     */
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
                newValue = newValue.replaceAll("net\\.sf", "org");
            }
            to.put(key, newValue);
        }
        for (String child : from.childrenNames()) {
            Preferences childNode = from.node(child);
            Preferences newChildNode = to.node(child);
            copyPrefsRecursively(childNode, newChildNode);
        }
    }

    /**
     * Added from Jabref 2.11 beta 4 onwards to fix wrong encoding names
     */
    private static void upgradeFaultyEncodingStrings(JabRefPreferences prefs) {
        String defaultEncoding = prefs.get(JabRefPreferences.DEFAULT_ENCODING);
        if (defaultEncoding == null) {
            return;
        }

        Map<String, String> encodingMap = new HashMap<>();
        encodingMap.put("UTF8", "UTF-8");
        encodingMap.put("Cp1250", "CP1250");
        encodingMap.put("Cp1251", "CP1251");
        encodingMap.put("Cp1252", "CP1252");
        encodingMap.put("Cp1253", "CP1253");
        encodingMap.put("Cp1254", "CP1254");
        encodingMap.put("Cp1257", "CP1257");
        encodingMap.put("ISO8859_1", "ISO8859-1");
        encodingMap.put("ISO8859_2", "ISO8859-2");
        encodingMap.put("ISO8859_3", "ISO8859-3");
        encodingMap.put("ISO8859_4", "ISO8859-4");
        encodingMap.put("ISO8859_5", "ISO8859-5");
        encodingMap.put("ISO8859_6", "ISO8859-6");
        encodingMap.put("ISO8859_7", "ISO8859-7");
        encodingMap.put("ISO8859_8", "ISO8859-8");
        encodingMap.put("ISO8859_9", "ISO8859-9");
        encodingMap.put("ISO8859_13", "ISO8859-13");
        encodingMap.put("ISO8859_15", "ISO8859-15");
        encodingMap.put("KOI8_R", "KOI8-R");
        encodingMap.put("Big5_HKSCS", "Big5-HKSCS");
        encodingMap.put("EUC_JP", "EUC-JP");

        if (encodingMap.containsKey(defaultEncoding)) {
            prefs.put(JabRefPreferences.DEFAULT_ENCODING, encodingMap.get(defaultEncoding));
        }
    }

    /**
     * Upgrade the sort order preferences for the current version
     * The old preference is kept in case an old version of JabRef is used with
     * these preferences, but it is only used when the new preference does not
     * exist
     */
    private static void upgradeSortOrder(JabRefPreferences prefs) {
        if (prefs.get(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, null) == null) {
            if (prefs.getBoolean("exportInStandardOrder", false)) {
                prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, StandardField.AUTHOR.getName());
                prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, StandardField.EDITOR.getName());
                prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, StandardField.YEAR.getName());
                prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            } else if (prefs.getBoolean("exportInTitleOrder", false)) {
                // exportInTitleOrder => title, author, editor
                prefs.putBoolean(JabRefPreferences.EXPORT_IN_SPECIFIED_ORDER, true);
                prefs.put(JabRefPreferences.EXPORT_PRIMARY_SORT_FIELD, StandardField.TITLE.getName());
                prefs.put(JabRefPreferences.EXPORT_SECONDARY_SORT_FIELD, StandardField.AUTHOR.getName());
                prefs.put(JabRefPreferences.EXPORT_TERTIARY_SORT_FIELD, StandardField.EDITOR.getName());
                prefs.putBoolean(JabRefPreferences.EXPORT_PRIMARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_SECONDARY_SORT_DESCENDING, false);
                prefs.putBoolean(JabRefPreferences.EXPORT_TERTIARY_SORT_DESCENDING, false);
            }
        }
    }

    /**
     * Migrate all customized entry types from versions <=3.7
     */
    private static void upgradeStoredBibEntryTypes(JabRefPreferences prefs, Preferences mainPrefsNode) {
        try {
            if (mainPrefsNode.nodeExists(JabRefPreferences.CUSTOMIZED_BIBTEX_TYPES) ||
                    mainPrefsNode.nodeExists(JabRefPreferences.CUSTOMIZED_BIBLATEX_TYPES)) {
                // skip further processing as prefs already have been migrated
            } else {
                LOGGER.info("Migrating old custom entry types.");
                CustomEntryTypePreferenceMigration.upgradeStoredBibEntryTypes(prefs.getGeneralPreferences().getDefaultBibDatabaseMode());
            }
        } catch (BackingStoreException ex) {
            LOGGER.error("Migrating old custom entry types failed.", ex);
        }
    }

    /**
     * Migrate LabelPattern configuration from versions <=3.5 to new CitationKeyPatterns.
     * <p>
     * Introduced in <a href="https://github.com/JabRef/jabref/pull/1704">#1704</a>
     */
    private static void upgradeLabelPatternToCitationKeyPattern(JabRefPreferences prefs, Preferences mainPrefsNode) {
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

    /**
     * Migrate Import File Name and Directory name Patterns from versions <=4.0 to new BracketedPatterns
     */
    private static void migrateFileImportPattern(String oldStylePattern, String newStylePattern,
                                                 JabRefPreferences prefs, Preferences mainPrefsNode) {
        String preferenceFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        if ((preferenceFileNamePattern != null) &&
                oldStylePattern.equals(preferenceFileNamePattern)) {
            // Upgrade the old-style File Name pattern to new one:
            mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePattern);
            LOGGER.info("migrated old style " + JabRefPreferences.IMPORT_FILENAMEPATTERN +
                    " value \"" + oldStylePattern + "\" to new value \"" +
                    newStylePattern + "\" in the preference file");

            if (prefs.hasKey(JabRefPreferences.IMPORT_FILENAMEPATTERN)) {
                // Update also the key in the current application settings, if necessary:
                String fileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
                if (oldStylePattern.equals(fileNamePattern)) {
                    prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePattern);
                    LOGGER.info("migrated old style " + JabRefPreferences.IMPORT_FILENAMEPATTERN +
                            " value \"" + oldStylePattern + "\" to new value \"" +
                            newStylePattern + "\" in the running application");
                }
            }
        }
    }

    static void upgradeImportFileAndDirePatterns(JabRefPreferences prefs, Preferences mainPrefsNode) {
        // Migrate Import patterns
        // Check for prefs node for Version <= 4.0
        if (mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null) != null) {
            String[] oldStylePatterns = new String[]{
                    "\\bibtexkey",
                    "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
            String[] newStylePatterns = new String[]{"[citationkey]",
                    "[citationkey] - [title]"};

            String[] oldDisplayStylePattern = new String[]{"bibtexkey", "bibtexkey - title"};

            for (int i = 0; i < oldStylePatterns.length; i++) {
                migrateFileImportPattern(oldStylePatterns[i], newStylePatterns[i], prefs, mainPrefsNode);
            }
            for (int i = 0; i < oldDisplayStylePattern.length; i++) {
                migrateFileImportPattern(oldDisplayStylePattern[i], newStylePatterns[i], prefs, mainPrefsNode);
            }
        }
        // Directory preferences are not yet migrated, since it is not quote clear how to parse and reinterpret
        // the user defined old-style patterns, and the default pattern is "".
    }

    private static void upgradeKeyBindingsToJavaFX(JabRefPreferences prefs) {
        UnaryOperator<String> replaceKeys = (str) -> {
            String result = str.replace("ctrl ", "ctrl+");
            result = result.replace("shift ", "shift+");
            result = result.replace("alt ", "alt+");
            result = result.replace("meta ", "meta+");

            return result;
        };

        List<String> keys = prefs.getStringList(JabRefPreferences.BINDINGS);
        keys.replaceAll(replaceKeys);
        prefs.putStringList(JabRefPreferences.BINDINGS, keys);
    }

    private static void addCrossRefRelatedFieldsForAutoComplete(JabRefPreferences prefs) {
        // LinkedHashSet because we want to retain the order and add new fields to the end
        Set<String> keys = new LinkedHashSet<>(prefs.getStringList(JabRefPreferences.AUTOCOMPLETER_COMPLETE_FIELDS));
        keys.add("crossref");
        keys.add("related");
        keys.add("entryset");
        prefs.putStringList(JabRefPreferences.AUTOCOMPLETER_COMPLETE_FIELDS, new ArrayList<>(keys));
    }

    private static void migrateTypedKeyPrefs(JabRefPreferences prefs, Preferences oldPatternPrefs)
            throws BackingStoreException {
        LOGGER.info("Found old Bibtex Key patterns which will be migrated to new version.");

        GlobalCitationKeyPattern keyPattern = GlobalCitationKeyPattern.fromPattern(
                prefs.get(JabRefPreferences.DEFAULT_CITATION_KEY_PATTERN));
        for (String key : oldPatternPrefs.keys()) {
            keyPattern.addCitationKeyPattern(EntryTypeFactory.parse(key), oldPatternPrefs.get(key, null));
        }

        prefs.storeGlobalCitationKeyPattern(keyPattern);
    }

    static void upgradePreviewStyleFromReviewToComment(JabRefPreferences prefs) {
        String currentPreviewStyle = prefs.getPreviewStyle();
        String migratedStyle = currentPreviewStyle.replace("\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}", "\\begin{comment}<BR><BR><b>Comment: </b> \\format[HTMLChars]{\\comment} \\end{comment}")
                                                  .replace("<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>", "<b><i>\\bibtextype</i><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>")
                                                  .replace("\\end{bibtexkey}</b><br>__NEWLINE__", "\\end{citationkey}</b><br>__NEWLINE__");
        prefs.setPreviewStyle(migratedStyle);
    }

    static void upgradePreviewStyleAllowMarkdown(JabRefPreferences prefs) {
        String currentPreviewStyle = prefs.getPreviewStyle();
        String migratedStyle = currentPreviewStyle.replace("\\format[HTMLChars]{\\comment}", "\\format[Markdown,HTMLChars]{\\comment}")
                                                  .replace("<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>", "<b><i>\\bibtextype</i><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>")
                                                  .replace("\\end{bibtexkey}</b><br>__NEWLINE__", "\\end{citationkey}</b><br>__NEWLINE__");

        prefs.setPreviewStyle(migratedStyle);
    }

    /**
     * The former preferences default of columns was a simple list of strings ("author;title;year;..."). Since 5.0
     * the preferences store the type of the column too, so that the formerly hardwired columns like the graphic groups
     * column or the other icon columns can be reordered in the main table and behave like any other field column
     * ("groups;linked_id;field:author;special:readstatus;extrafile:pdf;...").
     * <p>
     * Simple strings are by default parsed as a FieldColumn, so there is nothing to do there, but the formerly hard
     * wired columns need to be added.
     * <p>
     * In 5.1 variable names in JabRefPreferences have changed to offer backward compatibility with pre 5.0 releases
     * Pre 5.1: columnNames, columnWidths, columnSortTypes, columnSortOrder
     * Since 5.1: mainTableColumnNames, mainTableColumnWidths, mainTableColumnSortTypes, mainTableColumnSortOrder
     */
    static void upgradeColumnPreferences(JabRefPreferences preferences) {
        List<String> columnNames = preferences.getStringList(JabRefPreferences.COLUMN_NAMES);
        List<Double> columnWidths = preferences.getStringList(JabRefPreferences.COLUMN_WIDTHS)
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
                                                             .map(field -> MainTableColumnModel.Type.SPECIALFIELD)
                                                             .orElse(MainTableColumnModel.Type.NORMALFIELD);

                if (i < columnWidths.size()) {
                    columnWidth = columnWidths.get(i);
                }

                columns.add(new MainTableColumnModel(type, name, columnWidth));
            }

            preferences.putStringList(JabRefPreferences.COLUMN_NAMES,
                    columns.stream()
                           .map(MainTableColumnModel::getName)
                           .collect(Collectors.toList()));

            preferences.putStringList(JabRefPreferences.COLUMN_WIDTHS,
                    columns.stream()
                           .map(MainTableColumnModel::getWidth)
                           .map(Double::intValue)
                           .map(Object::toString)
                           .collect(Collectors.toList()));

            // ASCENDING by default
            preferences.putStringList(JabRefPreferences.COLUMN_SORT_TYPES,
                    columns.stream()
                           .map(MainTableColumnModel::getSortType)
                           .map(TableColumn.SortType::toString)
                           .collect(Collectors.toList()));
        }
    }

    static void changeColumnVariableNamesFor51(JabRefPreferences preferences) {
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

    /**
     * In 5.0 the format of column names have changed. That made newer versions of JabRef preferences incompatible with
     * earlier versions of JabRef. As some complains came up, we decided to change the variable names and to clear the
     * variable contents if they are unreadable, so former versions of JabRef would automatically create preferences
     * they can deal with.
     */
    static void restoreVariablesForBackwardCompatibility(JabRefPreferences preferences) {
        List<String> oldColumnNames = preferences.getStringList(JabRefPreferences.COLUMN_NAMES);
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
            preferences.putStringList("columnNames", fieldColumnNames);

            List<String> fieldColumnWidths = new ArrayList<>(Collections.emptyList());
            for (int i = 0; i < fieldColumnNames.size(); i++) {
                fieldColumnWidths.add("100");
            }
            preferences.putStringList("columnWidths", fieldColumnWidths);

            preferences.put("columnSortTypes", "");
            preferences.put("columnSortOrder", "");
        }

        // Ensure font size is a parsable int variable
        try {
            // some versions stored the font size as double to the **same** key
            // since the preference store is type-safe, we need to add this workaround
            String fontSizeAsString = preferences.get(JabRefPreferences.MAIN_FONT_SIZE);
            int fontSizeAsInt = (int) Math.round(Double.parseDouble(fontSizeAsString));
            preferences.putInt(JabRefPreferences.MAIN_FONT_SIZE, fontSizeAsInt);
        } catch (ClassCastException e) {
            // already an integer
        }
    }

    /**
     * In version 6.0 the formatting of the CleanUps preferences changed. Instead of using several keys that have have a variable name a single preference key is introduced containing just the active cleanup jobs. Also instead of a combined field for the field formatters and the enabled status of all of them, they are split for easier parsing.
     * <p>
     * <h3>Changes:</h3>
     * <table>
     * <tr> <td>                key                     </td> <td>  value </td> </tr>
     * <tr> <td colspan="2">    CLEANUP - old format:   </td> </tr>
     * <tr> <td> CleanUpCLEAN_UP_DOI    </td> <td>  enabled </td> </tr>
     * <tr> <td> CleanUpRENAME_PDF      </td> <td>  disabled </td> </tr>
     * <tr> <td> CleanUpMOVE_PDF        </td> <td>  enabled<br>
     * <tr> <td colspan="2"> ... </td> </tr>
     * <tr> <td> &nbsp; </td> </tr>
     * <tr> <td colspan="2"> CLEANUP_JOBS - new format: </td> </tr>
     * <tr> <td> CleanUpJobs            </td> <td> CLEAN_UP_DOI;RENAME_PDF;MOVE_PDF </td> </tr>
     * <tr> <td> &nbsp; </td> </tr>
     * <tr> <td colspan="2"> CLEANUP_FORMATTERS - old format: </td> </tr>
     * <tr> <td> CleanUpFormatters     </td> <td> ENABLED\nfield[formatter,formatter...]\nfield[...]\nfield[...]... </td> </tr>
     * <tr> <td> &nbsp; </td> </tr>
     * <tr> <td colspan="2"> CLEANUP_FORMATTERS - new format: </td> </tr>
     * <tr> <td> CleanUpFormattersEnabled </td> <td> TRUE </td> </tr>
     * <tr> <td> CleanUpFormatters        </td> <td> field[formatter,formatter...]\nfield[...]\nfield[...]... </td> </tr>
     * </table>
     */
    private static void upgradeCleanups(JabRefPreferences prefs) {
        final String V5_8_CLEANUP = "CleanUp";
        final String V6_0_CLEANUP_JOBS = "CleanUpJobs";

        final String V5_8_CLEANUP_FIELD_FORMATTERS = "CleanUpFormatters";
        final String V6_0_CLEANUP_FIELD_FORMATTERS = "CleanUpFormatters";
        final String V6_0_CLEANUP_FIELD_FORMATTERS_ENABLED = "CleanUpFormattersEnabled";

        List<String> activeJobs = new ArrayList<>();
        for (CleanupPreferences.CleanupStep action : EnumSet.allOf(CleanupPreferences.CleanupStep.class)) {
            Optional<String> job = prefs.getAsOptional(V5_8_CLEANUP + action.name());
            if (job.isPresent() && Boolean.parseBoolean(job.get())) {
                activeJobs.add(action.name());
                // prefs.deleteKey(V5_8_CLEANUP + action.name()); // for backward compatibility in comments
            }
        }
        if (!activeJobs.isEmpty()) {
            prefs.put(V6_0_CLEANUP_JOBS, String.join(";", activeJobs));
        }

        List<String> formatterCleanups = List.of(StringUtil.unifyLineBreaks(prefs.get(V5_8_CLEANUP_FIELD_FORMATTERS), "\n")
                                                           .split("\n"));
        if (formatterCleanups.size() >= 2
                && (formatterCleanups.get(0).equals(FieldFormatterCleanups.ENABLED)
                || formatterCleanups.get(0).equals(FieldFormatterCleanups.DISABLED))) {
            prefs.putBoolean(V6_0_CLEANUP_FIELD_FORMATTERS_ENABLED, formatterCleanups.get(0).equals(FieldFormatterCleanups.ENABLED)
                    ? Boolean.TRUE
                    : Boolean.FALSE);

            prefs.put(V6_0_CLEANUP_FIELD_FORMATTERS, String.join(OS.NEWLINE, formatterCleanups.subList(1, formatterCleanups.size() - 1)));
        }
    }
}
