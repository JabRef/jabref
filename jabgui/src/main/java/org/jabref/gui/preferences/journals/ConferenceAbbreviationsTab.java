package org.jabref.gui.preferences.journals;

import org.jspecify.annotations.NullMarked;

/// Manages conference abbreviation lists with the shared abbreviation editor.
@NullMarked
public class ConferenceAbbreviationsTab extends JournalAbbreviationsTab {
    // [impl->req~abbreviations.conference-preferences~1]
    public ConferenceAbbreviationsTab() {
        super(AbbreviationListType.CONFERENCES);
    }
}
