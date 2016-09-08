package net.sf.jabref.preferences;

import java.util.List;
import java.util.Map;

import net.sf.jabref.logic.citationstyle.CitationStyle;

import static net.sf.jabref.Globals.prefs;


public class PreviewPreferences {

    private static final String CYCLE_PREVIEW_POS = "cyclePreviewPos";
    private static final String CYCLE_PREVIEW = "cyclePreview";
    private static final String PREVIEW_PANEL_HEIGHT = "previewPanelHeight";
    private static final String PREVIEW_STYLE = "previewStyle";
    private static final String PREVIEW_ENABLED = "previewEnabled";

    private final JabRefPreferences preferences;


    public PreviewPreferences(JabRefPreferences preferences) {
        this.preferences = preferences;
    }

    public static void putDefaults(Map<String, Object> defaults) {
        defaults.put(CYCLE_PREVIEW, "Preview;" + CitationStyle.DEFAULT);
        defaults.put(CYCLE_PREVIEW_POS, 0);

        defaults.put(PREVIEW_PANEL_HEIGHT, 200);
        defaults.put(PREVIEW_ENABLED, Boolean.TRUE);
        defaults.put(PREVIEW_STYLE,
                "<font face=\"sans-serif\">"
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
                        + "</dd>__NEWLINE__<p></p></font>");
    }

    public List<String> getPreviewCycle() {
        return preferences.getStringList(CYCLE_PREVIEW);
    }

    public int getCyclePreviewPosition() {
        return prefs.getInt(CYCLE_PREVIEW_POS);
    }

    public PreviewPreferences setCyclePreviewPosition(final int position) {
        int pos = position;
        while (pos < 0) {
            pos += getPreviewCycle().size();
        }
        pos %= getPreviewCycle().size();
        preferences.putInt(CYCLE_PREVIEW_POS, pos);
        return this;
    }

    public PreviewPreferences setPreviewCycle(List<String> previewCircle) {
        preferences.putStringList(CYCLE_PREVIEW, previewCircle);
        return this;
    }

    public int getPreviewPanelHeight() {
        return preferences.getInt(PREVIEW_PANEL_HEIGHT);
    }

    public PreviewPreferences setPreviewPanelHeight(int height) {
        preferences.putInt(PREVIEW_PANEL_HEIGHT, height);
        return this;
    }

    public String getPreviewStyleDefault() {
        return (String) preferences.defaults.get(PREVIEW_STYLE);
    }

    public String getPreviewStyle() {
        return preferences.get(PREVIEW_STYLE);
    }

    public PreviewPreferences setPreviewStyle(String preview0) {
        preferences.put(PREVIEW_STYLE, preview0);
        return this;
    }

    public boolean isPreviewEnabled() {
        return preferences.getBoolean(PREVIEW_ENABLED);
    }

    public PreviewPreferences setPreviewEnabled(boolean previewEnabled) {
        preferences.putBoolean(PREVIEW_ENABLED, previewEnabled);
        return this;
    }

}
