package org.jabref.migrations;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PreferencesMigrationsTest {

    private JabRefPreferences prefs;
    private Preferences mainPrefsNode;

    private final String[] oldStylePatterns = new String[]{"\\bibtexkey",
            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[]{"[citationkey]",
            "[citationkey] - [title]"};

    @BeforeEach
    void setUp() {
        prefs = mock(JabRefPreferences.class);
        mainPrefsNode = mock(Preferences.class);
    }

    @Test
    void testOldStyleBibtexkeyPattern0() {
        when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[0]);
        when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[0]);
        when(prefs.hasKey(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
        verify(mainPrefsNode).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[0]);
    }

    @Test
    void testOldStyleBibtexkeyPattern1() {

        when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(oldStylePatterns[1]);
        when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(oldStylePatterns[1]);
        when(prefs.hasKey(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(true);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
        verify(mainPrefsNode).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, newStylePatterns[1]);
    }

    @Test
    void testArbitraryBibtexkeyPattern() {
        String arbitraryPattern = "[anyUserPrividedString]";

        when(prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN)).thenReturn(arbitraryPattern);
        when(mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null)).thenReturn(arbitraryPattern);

        PreferencesMigrations.upgradeImportFileAndDirePatterns(prefs, mainPrefsNode);

        verify(prefs, never()).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
        verify(mainPrefsNode, never()).put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
    }

    @Test
    void testPreviewStyle() {
        String oldPreviewStyle = "<font face=\"sans-serif\">"
                + "<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                + "\\end{bibtexkey}</b><br>__NEWLINE__"
                + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                // Include the booktitle field for @inproceedings, @proceedings, etc.
                + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                + "\\begin{review}<BR><BR><b>Review: </b> \\format[HTMLChars]{\\review} \\end{review}"
                + "</dd>__NEWLINE__<p></p></font>";

        String newPreviewStyle = "<font face=\"sans-serif\">"
                + "<b><i>\\bibtextype</i><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>"
                + "\\end{citationkey}</b><br>__NEWLINE__"
                + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                // Include the booktitle field for @inproceedings, @proceedings, etc.
                + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                + "\\begin{comment}<BR><BR><b>Comment: </b> \\format[HTMLChars]{\\comment} \\end{comment}"
                + "</dd>__NEWLINE__<p></p></font>";

        prefs.setPreviewStyle(oldPreviewStyle);
        when(prefs.getPreviewStyle()).thenReturn(oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyleFromReviewToComment(prefs);

        verify(prefs).setPreviewStyle(newPreviewStyle);
    }

    @Test
    void upgradePreviewStyleAllowMarkupDefault() {
        String oldPreviewStyle = "<font face=\"sans-serif\">"
                + "<b><i>\\bibtextype</i><a name=\"\\bibtexkey\">\\begin{bibtexkey} (\\bibtexkey)</a>"
                + "\\end{bibtexkey}</b><br>__NEWLINE__"
                + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                // Include the booktitle field for @inproceedings, @proceedings, etc.
                + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                + "\\begin{comment}<BR><BR><b>Comment: </b> \\format[HTMLChars]{\\comment} \\end{comment}"
                + "</dd>__NEWLINE__<p></p></font>";

        String newPreviewStyle = "<font face=\"sans-serif\">"
                + "<b><i>\\bibtextype</i><a name=\"\\citationkey\">\\begin{citationkey} (\\citationkey)</a>"
                + "\\end{citationkey}</b><br>__NEWLINE__"
                + "\\begin{author} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\author}<BR>\\end{author}__NEWLINE__"
                + "\\begin{editor} \\format[Authors(LastFirst,Initials,Semicolon,Amp),HTMLChars]{\\editor} "
                + "<i>(\\format[IfPlural(Eds.,Ed.)]{\\editor})</i><BR>\\end{editor}__NEWLINE__"
                + "\\begin{title} \\format[HTMLChars]{\\title} \\end{title}<BR>__NEWLINE__"
                + "\\begin{chapter} \\format[HTMLChars]{\\chapter}<BR>\\end{chapter}__NEWLINE__"
                + "\\begin{journal} <em>\\format[HTMLChars]{\\journal}, </em>\\end{journal}__NEWLINE__"
                // Include the booktitle field for @inproceedings, @proceedings, etc.
                + "\\begin{booktitle} <em>\\format[HTMLChars]{\\booktitle}, </em>\\end{booktitle}__NEWLINE__"
                + "\\begin{school} <em>\\format[HTMLChars]{\\school}, </em>\\end{school}__NEWLINE__"
                + "\\begin{institution} <em>\\format[HTMLChars]{\\institution}, </em>\\end{institution}__NEWLINE__"
                + "\\begin{publisher} <em>\\format[HTMLChars]{\\publisher}, </em>\\end{publisher}__NEWLINE__"
                + "\\begin{year}<b>\\year</b>\\end{year}\\begin{volume}<i>, \\volume</i>\\end{volume}"
                + "\\begin{pages}, \\format[FormatPagesForHTML]{\\pages} \\end{pages}__NEWLINE__"
                + "\\begin{abstract}<BR><BR><b>Abstract: </b> \\format[HTMLChars]{\\abstract} \\end{abstract}__NEWLINE__"
                + "\\begin{comment}<BR><BR><b>Comment: </b> \\format[Markdown,HTMLChars]{\\comment} \\end{comment}"
                + "</dd>__NEWLINE__<p></p></font>";

        prefs.setPreviewStyle(oldPreviewStyle);
        when(prefs.getPreviewStyle()).thenReturn(oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyleAllowMarkdown(prefs);

        verify(prefs).setPreviewStyle(newPreviewStyle);
    }

    @Test
    void upgradePreviewStyleAllowMarkupCustomized() {
        String oldPreviewStyle = "<font face=\"sans-serif\">"
                + "My highly customized format only using comments:<br>"
                + "\\begin{comment} Something: \\format[HTMLChars]{\\comment} special \\end{comment}"
                + "</dd>__NEWLINE__<p></p></font>";

        String newPreviewStyle = "<font face=\"sans-serif\">"
                + "My highly customized format only using comments:<br>"
                + "\\begin{comment} Something: \\format[Markdown,HTMLChars]{\\comment} special \\end{comment}"
                + "</dd>__NEWLINE__<p></p></font>";

        prefs.setPreviewStyle(oldPreviewStyle);
        when(prefs.getPreviewStyle()).thenReturn(oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyleAllowMarkdown(prefs);

        verify(prefs).setPreviewStyle(newPreviewStyle);
    }

    @Test
    void testUpgradeColumnPreferencesAlreadyMigrated() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        when(prefs.getStringList(JabRefPreferences.COLUMN_NAMES)).thenReturn(columnNames);
        when(prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS)).thenReturn(columnWidths);

        PreferencesMigrations.upgradeColumnPreferences(prefs);

        verify(prefs, never()).put(JabRefPreferences.COLUMN_NAMES, "anyString");
        verify(prefs, never()).put(JabRefPreferences.COLUMN_WIDTHS, "anyString");
    }

    @Test
    void testUpgradeColumnPreferencesFromWithoutTypes() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");
        List<String> updatedNames = Arrays.asList("groups", "files", "linked_id", "field:entrytype", "field:author/editor", "field:title", "field:year", "field:journal/booktitle", "field:citationkey", "special:printed");
        List<String> updatedWidths = Arrays.asList("28", "28", "28", "75", "300", "470", "60", "130", "100", "30");
        List<String> newSortTypes = Arrays.asList("ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING", "ASCENDING");

        when(prefs.getStringList(JabRefPreferences.COLUMN_NAMES)).thenReturn(columnNames);
        when(prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS)).thenReturn(columnWidths);

        PreferencesMigrations.upgradeColumnPreferences(prefs);

        verify(prefs).putStringList(JabRefPreferences.COLUMN_NAMES, updatedNames);
        verify(prefs).putStringList(JabRefPreferences.COLUMN_WIDTHS, updatedWidths);
        verify(prefs).putStringList(JabRefPreferences.COLUMN_SORT_TYPES, newSortTypes);
    }

    @Test
    void testChangeColumnPreferencesVariableNamesFor51() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        when(prefs.getStringList("columnNames")).thenReturn(columnNames);
        when(prefs.getStringList("columnWidths")).thenReturn(columnWidths);
        when(prefs.getStringList("mainTableColumnSortTypes")).thenReturn(columnNames);
        when(prefs.getStringList("mainTableColumnSortOrder")).thenReturn(columnWidths);

        when(prefs.getStringList(JabRefPreferences.COLUMN_NAMES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefPreferences.COLUMN_SORT_TYPES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefPreferences.COLUMN_SORT_ORDER)).thenReturn(Collections.emptyList());

        PreferencesMigrations.changeColumnVariableNamesFor51(prefs);

        verify(prefs).putStringList(JabRefPreferences.COLUMN_NAMES, columnNames);
        verify(prefs).putStringList(JabRefPreferences.COLUMN_WIDTHS, columnWidths);
        verify(prefs).putStringList(JabRefPreferences.COLUMN_NAMES, columnNames);
        verify(prefs).putStringList(JabRefPreferences.COLUMN_WIDTHS, columnWidths);
    }

    @Test
    void testChangeColumnPreferencesVariableNamesBackwardsCompatibility() {
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("75", "300", "470", "60", "130", "100", "30");

        // The variable names have to be hardcoded, because they have changed between 5.0 and 5.1
        when(prefs.getStringList("columnNames")).thenReturn(columnNames);
        when(prefs.getStringList("columnWidths")).thenReturn(columnWidths);
        when(prefs.getStringList("mainTableColumnSortTypes")).thenReturn(columnNames);
        when(prefs.getStringList("mainTableColumnSortOrder")).thenReturn(columnWidths);

        when(prefs.getStringList(JabRefPreferences.COLUMN_NAMES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefPreferences.COLUMN_WIDTHS)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefPreferences.COLUMN_SORT_TYPES)).thenReturn(Collections.emptyList());
        when(prefs.getStringList(JabRefPreferences.COLUMN_SORT_ORDER)).thenReturn(Collections.emptyList());

        PreferencesMigrations.upgradeColumnPreferences(prefs);

        verify(prefs, never()).put("columnNames", "anyString");
        verify(prefs, never()).put("columnWidths", "anyString");
        verify(prefs, never()).put("mainTableColumnSortTypes", "anyString");
        verify(prefs, never()).put("mainTableColumnSortOrder", "anyString");
    }

    @Test
    void testRestoreColumnVariablesForBackwardCompatibility() {
        List<String> updatedNames = Arrays.asList("groups", "files", "linked_id", "field:entrytype", "field:author/editor", "field:title", "field:year", "field:journal/booktitle", "field:citationkey", "special:printed");
        List<String> columnNames = Arrays.asList("entrytype", "author/editor", "title", "year", "journal/booktitle", "citationkey", "printed");
        List<String> columnWidths = Arrays.asList("100", "100", "100", "100", "100", "100", "100");

        when(prefs.getStringList(JabRefPreferences.COLUMN_NAMES)).thenReturn(updatedNames);

        when(prefs.get(JabRefPreferences.MAIN_FONT_SIZE)).thenReturn("11.2");

        PreferencesMigrations.restoreVariablesForBackwardCompatibility(prefs);

        verify(prefs).putStringList("columnNames", columnNames);
        verify(prefs).putStringList("columnWidths", columnWidths);
        verify(prefs).put("columnSortTypes", "");
        verify(prefs).put("columnSortOrder", "");

        verify(prefs).putInt(JabRefPreferences.MAIN_FONT_SIZE, 11);
    }
}
