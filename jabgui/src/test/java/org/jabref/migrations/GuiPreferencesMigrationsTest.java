package org.jabref.migrations;

import java.util.Arrays;
import java.util.List;
import java.util.prefs.Preferences;

import org.jabref.gui.preferences.JabRefGuiPreferences;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.logic.preferences.JabRefCliPreferences;

import com.airhacks.afterburner.injection.Injector;
import com.github.javakeyring.Keyring;
import com.github.javakeyring.PasswordAccessException;
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

class GuiPreferencesMigrationsTest {

    private JabRefGuiPreferences preferences;
    private Preferences mainPrefsNode;

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[citationkey]",
            "[citationkey] - [title]"};

    @BeforeEach
    void setUp() {
        preferences = mock(JabRefGuiPreferences.class, Answers.RETURNS_DEEP_STUBS);
        Injector.setModelOrService(CliPreferences.class, preferences);
        mainPrefsNode = mock(Preferences.class);
    }

    @Test
    void oldStyleBibtexkeyPattern0() {
        when(preferences.get(JabRefCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[0]);
        when(mainPrefsNode.get(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[0]);
        when(preferences.hasKey(JabRefCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(preferences, mainPrefsNode);

        verify(preferences).put(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        verify(mainPrefsNode).put(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
    }

    @Test
    void oldStyleBibtexkeyPattern1() {
        when(preferences.get(JabRefCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[1]);
        when(mainPrefsNode.get(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[1]);
        when(preferences.hasKey(JabRefCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(preferences, mainPrefsNode);

        verify(preferences).put(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
        verify(mainPrefsNode).put(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
    }

    @Test
    void arbitraryBibtexkeyPattern() {
        String arbitraryPattern = "[anyUserPrividedString]";

        when(preferences.get(JabRefCliPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(arbitraryPattern);
        when(mainPrefsNode.get(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(arbitraryPattern);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(preferences, mainPrefsNode);

        verify(preferences, never()).put(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
        verify(mainPrefsNode, never()).put(JabRefCliPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
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

        when(preferences.get(JabRefGuiPreferences.PREVIEW_STYLE)).thenReturn(oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyle(preferences);

        verify(preferences).put(JabRefGuiPreferences.PREVIEW_STYLE, newPreviewStyle);
    }

    @Test
    void upgradeColumnPreferencesAlreadyMigrated() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_NAMES)).thenReturn(columnNames);
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_WIDTHS)).thenReturn(columnWidths);

        PreferencesMigrations.upgradeColumnPreferences(preferences);

        verify(preferences, never()).put(JabRefGuiPreferences.COLUMN_NAMES, "anyString");
        verify(preferences, never()).put(JabRefGuiPreferences.COLUMN_WIDTHS, "anyString");
    }

    @Test
    void upgradeColumnPreferencesFromWithoutTypes() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");
        List<String> updatedNames = Arrays.asList("groups", "files", "linked_id", "field:entrytype", "field:author/editor", "field:title", "field:year", "field:journal/booktitle", "field:citationkey", "special:printed");
        List<String> updatedWidths = Arrays.asList("28", "28", "28", "75", "300", "470", "60", "130", "100", "30");
        List<String> newSortTypes = Arrays.asList("ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING");

        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_NAMES)).thenReturn(columnNames);
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_WIDTHS)).thenReturn(columnWidths);

        PreferencesMigrations.upgradeColumnPreferences(preferences);

        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_NAMES, updatedNames);
        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_WIDTHS, updatedWidths);
        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_SORT_TYPES, newSortTypes);
    }

    @Test
    void changeColumnPreferencesVariableNamesFor51() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        when(preferences.getStringList("columnNames")).thenReturn(columnNames);
        when(preferences.getStringList("columnWidths")).thenReturn(columnWidths);
        when(preferences.getStringList("mainTableColumnSortTypes")).thenReturn(columnNames);
        when(preferences.getStringList("mainTableColumnSortOrder")).thenReturn(columnWidths);

        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_NAMES)).thenReturn(List.of());
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_WIDTHS)).thenReturn(List.of());
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_SORT_TYPES)).thenReturn(List.of());
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_SORT_ORDER)).thenReturn(List.of());

        PreferencesMigrations.changeColumnVariableNamesFor51(preferences);

        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_NAMES, columnNames);
        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_WIDTHS, columnWidths);
        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_NAMES, columnNames);
        verify(preferences).putStringList(JabRefGuiPreferences.COLUMN_WIDTHS, columnWidths);
    }

    @Test
    void changeColumnPreferencesVariableNamesBackwardsCompatibility() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        when(preferences.getStringList("columnNames")).thenReturn(columnNames);
        when(preferences.getStringList("columnWidths")).thenReturn(columnWidths);
        when(preferences.getStringList("mainTableColumnSortTypes")).thenReturn(columnNames);
        when(preferences.getStringList("mainTableColumnSortOrder")).thenReturn(columnWidths);

        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_NAMES)).thenReturn(List.of());
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_WIDTHS)).thenReturn(List.of());
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_SORT_TYPES)).thenReturn(List.of());
        when(preferences.getStringList(JabRefGuiPreferences.COLUMN_SORT_ORDER)).thenReturn(List.of());

        PreferencesMigrations.upgradeColumnPreferences(preferences);

        verify(preferences, never()).put("columnNames", "anyString");
        verify(preferences, never()).put("columnWidths", "anyString");
        verify(preferences, never()).put("mainTableColumnSortTypes", "anyString");
        verify(preferences, never()).put("mainTableColumnSortOrder", "anyString");
    }

    @Test
    void restoreColumnVariablesForBackwardCompatibility() {
        List<String> updatedNames = Arrays.asList("groups", "files", "linked_id", "field:entrytype", "field:author/editor", "field:title", "field:year", "field:journal/booktitle", "field:citationkey", "special:printed");
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("100", "100", "100", "100", "100", "100", "100");

        when(preferences.getStringList("columnNames")).thenReturn(updatedNames);

        when(preferences.get("mainFontSize")).thenReturn("11.2");

        PreferencesMigrations.restoreVariablesForBackwardCompatibility(preferences);

        verify(preferences).putStringList("columnNames", columnNames);
        verify(preferences).putStringList("columnWidths", columnWidths);
        verify(preferences).put("columnSortTypes", "");
        verify(preferences).put("columnSortOrder", "");

        verify(preferences).putInt("mainFontSize", 11);
    }

    @Test
    void moveApiKeysToKeyRing() throws PasswordAccessException {
        final String V5_9_FETCHER_CUSTOM_KEY_NAMES = "fetcherCustomKeyNames";
        final String V5_9_FETCHER_CUSTOM_KEYS = "fetcherCustomKeys";
        final Keyring keyring = mock(Keyring.class);

        when(preferences.getStringList(V5_9_FETCHER_CUSTOM_KEY_NAMES)).thenReturn(List.of("FetcherA", "FetcherB", "FetcherC"));
        when(preferences.getStringList(V5_9_FETCHER_CUSTOM_KEYS)).thenReturn(List.of("KeyA", "KeyB", "KeyC"));
        when(preferences.getInternalPreferences().getUserAndHost()).thenReturn("user-host");

        try (MockedStatic<Keyring> keyringFactory = Mockito.mockStatic(Keyring.class, Answers.RETURNS_DEEP_STUBS)) {
            keyringFactory.when(Keyring::create).thenReturn(keyring);

            PreferencesMigrations.moveApiKeysToKeyring(preferences);

            verify(keyring).setPassword(eq("org.jabref.customapikeys"), eq("FetcherA"), any());
            verify(keyring).setPassword(eq("org.jabref.customapikeys"), eq("FetcherB"), any());
            verify(keyring).setPassword(eq("org.jabref.customapikeys"), eq("FetcherC"), any());
            verify(preferences).deleteKey(V5_9_FETCHER_CUSTOM_KEYS);
        }
    }

    @Test
    void resolveBibTexStringsFields() {
        String oldPrefsValue = "author;booktitle;editor;editora;editorb;editorc;institution;issuetitle;journal;journalsubtitle;journaltitle;mainsubtitle;month;publisher;shortauthor;shorteditor;subtitle;titleaddon";
        String expectedValue = "author;booktitle;editor;editora;editorb;editorc;institution;issuetitle;journal;journalsubtitle;journaltitle;mainsubtitle;month;publisher;shortauthor;shorteditor;subtitle;titleaddon;monthfiled";
        when(preferences.get(JabRefCliPreferences.RESOLVE_STRINGS_FOR_FIELDS)).thenReturn(oldPrefsValue);

        PreferencesMigrations.upgradeResolveBibTeXStringsFields(preferences);
        verify(preferences).put(JabRefCliPreferences.RESOLVE_STRINGS_FOR_FIELDS, expectedValue);
    }
}
