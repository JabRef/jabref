package org.jabref.migrations;

import java.util.prefs.Preferences;

import org.jabref.Globals;
import org.jabref.JabRefMain;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PreferencesMigrationsTest {

    private JabRefPreferences prefs;

    private final String[] oldStylePatterns = new String[] {"\\bibtexkey",
                                                            "\\bibtexkey\\begin{title} - \\format[RemoveBrackets]{\\title}\\end{title}"};
    private final String[] newStylePatterns = new String[] {"[bibtexkey]",
                                                            "[bibtexkey] - [fulltitle]"};

    private final String oldPreviewStyle = "<font face=\"sans-serif\">"
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

    private final String newPreviewStyle = "<font face=\"sans-serif\">"
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

    @BeforeEach
    public void setUp() {
        prefs = JabRefPreferences.getInstance();
        Globals.prefs = prefs;
    }

    @Test
    public void testOldStyleBibtexkeyPattern0() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[0]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        assertEquals(newStylePatterns[0], updatedPrefsFileNamePattern);
        assertEquals(newStylePatterns[0], updatedMainNodeFileNamePattern);
    }

    @Test
    public void testOldStyleBibtexkeyPattern1() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[1]);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, oldStylePatterns[1]);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        assertEquals(newStylePatterns[1], updatedPrefsFileNamePattern);
        assertEquals(newStylePatterns[1], updatedMainNodeFileNamePattern);
    }

    @Test
    public void testArbitraryBibtexkeyPattern() {
        String previousFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);
        String arbitraryPattern = "[anyUserPrividedString]";

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, arbitraryPattern);

        PreferencesMigrations.upgradeImportFileAndDirePatterns();

        String updatedPrefsFileNamePattern = prefs.get(JabRefPreferences.IMPORT_FILENAMEPATTERN);
        String updatedMainNodeFileNamePattern = mainPrefsNode.get(JabRefPreferences.IMPORT_FILENAMEPATTERN, null);

        prefs.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);
        mainPrefsNode.put(JabRefPreferences.IMPORT_FILENAMEPATTERN, previousFileNamePattern);

        assertEquals(arbitraryPattern, updatedPrefsFileNamePattern);
        assertEquals(arbitraryPattern, updatedMainNodeFileNamePattern);
    }

    @Test
    public void testPreviewStyle() {
        String previousStyle = prefs.get(JabRefPreferences.PREVIEW_STYLE);
        final Preferences mainPrefsNode = Preferences.userNodeForPackage(JabRefMain.class);

        prefs.put(JabRefPreferences.PREVIEW_STYLE, oldPreviewStyle);
        mainPrefsNode.put(JabRefPreferences.PREVIEW_STYLE, oldPreviewStyle);

        PreferencesMigrations.upgradePreviewStyleFromReviewToComment();

        String updatedPrefsPreviewStyle = prefs.get(JabRefPreferences.PREVIEW_STYLE);
        String updatedMainNodePreviewStyle = mainPrefsNode.get(JabRefPreferences.PREVIEW_STYLE, null);

        prefs.put(JabRefPreferences.PREVIEW_STYLE, previousStyle);
        mainPrefsNode.put(JabRefPreferences.PREVIEW_STYLE, previousStyle);

        assertEquals(newPreviewStyle, updatedPrefsPreviewStyle);
        assertEquals(newPreviewStyle, updatedMainNodePreviewStyle);
    }
}
