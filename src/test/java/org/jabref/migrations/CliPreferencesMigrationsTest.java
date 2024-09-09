package org.jabref.migrations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jabref.logic.preferences.JabRefCliCliPreferences;
import org.jabref.logic.preferences.CliPreferences;

import com.airhacks.afterburner.injection.Injector;
import com.github.javakeyring.Keyring;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CliPreferencesMigrationsTest {

    private JabRefCliCliPreferences prefs;
    private java.util.prefs.Preferences mainPrefsNode;

    private final String[] oldStylePatterns = new String[]{"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[]{"[citationkey]",
            "[citationkey] - [title]"};

    @BeforeEach
    void setUp() {
        prefs = mock(JabRefCliCliPreferences.class, Answers.RETURNS_DEEP_STUBS);
        Injector.setModelOrService(CliPreferences.class, prefs);
        mainPrefsNode = mock(java.util.prefs.Preferences.class);
    }

    @Test
    void oldStyleBibtexkeyPattern0() {
        when(prefs.get(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[0]);
        when(mainPrefsNode.get(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[0]);
        when(prefs.hasKey(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs).put(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        verify(mainPrefsNode).put(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
    }

    @Test
    void oldStyleBibtexkeyPattern1() {
        when(prefs.get(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[1]);
        when(mainPrefsNode.get(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[1]);
        when(prefs.hasKey(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs).put(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
        verify(mainPrefsNode).put(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
    }

    @Test
    void arbitraryBibtexkeyPattern() {
        String arbitraryPattern = "[anyUserPrividedString]";

        when(prefs.get(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(arbitraryPattern);
        when(mainPrefsNode.get(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(arbitraryPattern);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs, never()).put(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
        verify(mainPrefsNode, never()).put(JabRefCliCliPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
    }

    @Test
    void previewStyleReviewToComment() {
        String oldPreviewStyle = "<font face=\"sans-serif\">__NEWLINE__"
                + "Customized preview style using reviews and comments:__NEWLINE__"
                + "\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}__NEWLINE__"
                + "\\begin{comment} Something: \\format[HTMLChars]{\\comment} special \\end{comment}__NEWLINE__"
                + "</font>__NEWLINE__";

        String newPreviewStyle = "<font face=\"sans-serif\">__NEWLINE__"
                + "Customized preview style using reviews and comments:__NEWLINE__"
                + "\\begin{comment}<BR><BR><b>Comment: </b> \\format[Markdown,HTMLChars(keepCurlyBraces)]{\\comment} \\end{comment}__NEWLINE__"
                + "\\begin{comment} Something: \\format[Markdown,HTMLChars(keepCurlyBraces)]{\\comment} special \\end{comment}__NEWLINE__"
                + "</font>__NEWLINE__";

        when(prefs.get(JabRefCliCliPreferences.PREVIEW_STYLE)).thenReturn(oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyle(prefs);

        verify(prefs).put(JabRefCliCliPreferences.PREVIEW_STYLE, newPreviewStyle);
    }

    @Test
    void upgradeColumnPreferencesAlreadyMigrated() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_NAMES)).thenReturn(columnNames);
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_WIDTHS)).thenReturn(columnWidths);

        PreferencesMigrations.upgradeColumnPreferences(prefs);

        verify(prefs, never()).put(JabRefCliCliPreferences.COLUMN_NAMES, "anyString");
        verify(prefs, never()).put(JabRefCliCliPreferences.COLUMN_WIDTHS, "anyString");
    }

    @Test
    void upgradeColumnPreferencesFromWithoutTypes() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");
        List<String> updatedNames = Arrays.asList("groups", "files", "linked_id", "field:entrytype", "field:author/editor", "field:title", "field:year", "field:journal/booktitle", "field:citationkey", "special:printed");
        List<String> updatedWidths = Arrays.asList("28", "28", "28", "75", "300", "470", "60", "130", "100", "30");
        List<String> newSortTypes = Arrays.asList("ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING");

        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_NAMES)).thenReturn(columnNames);
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_WIDTHS)).thenReturn(columnWidths);

        PreferencesMigrations.upgradeColumnPreferences(prefs);

        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_NAMES, updatedNames);
        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_WIDTHS, updatedWidths);
        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_SORT_TYPES, newSortTypes);
    }

    @Test
    void changeColumnPreferencesVariableNamesFor51() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        when(prefs.getStringList("columnNames")).thenReturn(columnNames);
        when(prefs.getStringList("columnWidths")).thenReturn(columnWidths);
        when(prefs.getStringList("mainTableColumnSortTypes")).thenReturn(columnNames);
        when(prefs.getStringList("mainTableColumnSortOrder")).thenReturn(columnWidths);

        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_NAMES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_WIDTHS)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_SORT_TYPES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_SORT_ORDER)).thenReturn(Collections.emptyList());

        PreferencesMigrations.changeColumnVariableNamesFor51(prefs);

        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_NAMES, columnNames);
        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_WIDTHS, columnWidths);
        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_NAMES, columnNames);
        verify(prefs).putStringList(JabRefCliCliPreferences.COLUMN_WIDTHS, columnWidths);
    }

    @Test
    void changeColumnPreferencesVariableNamesBackwardsCompatibility() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        when(prefs.getStringList("columnNames")).thenReturn(columnNames);
        when(prefs.getStringList("columnWidths")).thenReturn(columnWidths);
        when(prefs.getStringList("mainTableColumnSortTypes")).thenReturn(columnNames);
        when(prefs.getStringList("mainTableColumnSortOrder")).thenReturn(columnWidths);

        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_NAMES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_WIDTHS)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_SORT_TYPES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefCliCliPreferences.COLUMN_SORT_ORDER)).thenReturn(Collections.emptyList());

        PreferencesMigrations.upgradeColumnPreferences(prefs);

        verify(prefs, never()).put("columnNames", "anyString");
        verify(prefs, never()).put("columnWidths", "anyString");
        verify(prefs, never()).put("mainTableColumnSortTypes", "anyString");
        verify(prefs, never()).put("mainTableColumnSortOrder", "anyString");
    }

    @Test
    void restoreColumnVariablesForBackwardCompatibility() {
        List<String> updatedNames = Arrays.asList("groups", "files", "linked_id", "field:entrytype", "field:author/editor", "field:title", "field:year", "field:journal/booktitle", "field:citationkey", "special:printed");
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("100", "100", "100", "100", "100", "100", "100");

        when(prefs.getStringList("columnNames")).thenReturn(updatedNames);

        when(prefs.get(JabRefCliCliPreferences.MAIN_FONT_SIZE)).thenReturn("11.2");

        PreferencesMigrations.restoreVariablesForBackwardCompatibility(prefs);

        verify(prefs).putStringList("columnNames", columnNames);
        verify(prefs).putStringList("columnWidths", columnWidths);
        verify(prefs).put("columnSortTypes", "");
        verify(prefs).put("columnSortOrder", "");

        verify(prefs).putInt(JabRefCliCliPreferences.MAIN_FONT_SIZE, 11);
    }

    @Test
    void moveApiKeysToKeyRing() throws Exception {
        final String V5_9_FETCHER_CUSTOM_KEY_NAMES = "fetcherCustomKeyNames";
        final String V5_9_FETCHER_CUSTOM_KEYS = "fetcherCustomKeys";
        final Keyring keyring = mock(Keyring.class);

        when(prefs.getStringList(V5_9_FETCHER_CUSTOM_KEY_NAMES)).thenReturn(List.of("FetcherA", "FetcherB", "FetcherC"));
        when(prefs.getStringList(V5_9_FETCHER_CUSTOM_KEYS)).thenReturn(List.of("KeyA", "KeyB", "KeyC"));
        when(prefs.getInternalPreferences().getUserAndHost()).thenReturn("user-host");

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class, Answers.RETURNS_DEEP_STUBS)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            PreferencesMigrations.moveApiKeysToKeyring(prefs);

            verify(keyring).setPassword(eq("org.jabref.customapikeys"), eq("FetcherA"), any());
            verify(keyring).setPassword(eq("org.jabref.customapikeys"), eq("FetcherB"), any());
            verify(keyring).setPassword(eq("org.jabref.customapikeys"), eq("FetcherC"), any());
            verify(prefs).deleteKey(V5_9_FETCHER_CUSTOM_KEYS);
        }
    }
}
