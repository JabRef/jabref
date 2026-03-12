package org.jabref.gui.openoffice;

import org.jabref.gui.preferences.GuiPreferences;
import org.jabref.logic.citationkeypattern.CitationKeyPatternPreferences;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.layout.LayoutFormatterPreferences;

/**
 * Bundle of preference-related dependencies used in OpenOfficePanel and related classes.
 */
public class PreferencesBundle {
    public final GuiPreferences guiPreferences;
    public final OpenOfficePreferences openOfficePreferences;
    public final LayoutFormatterPreferences layoutFormatterPreferences;
    public final CitationKeyPatternPreferences citationKeyPatternPreferences;

    public PreferencesBundle(GuiPreferences guiPreferences,
                             OpenOfficePreferences openOfficePreferences,
                             LayoutFormatterPreferences layoutFormatterPreferences,
                             CitationKeyPatternPreferences citationKeyPatternPreferences) {
        this.guiPreferences = guiPreferences;
        this.openOfficePreferences = openOfficePreferences;
        this.layoutFormatterPreferences = layoutFormatterPreferences;
        this.citationKeyPatternPreferences = citationKeyPatternPreferences;
    }
}
