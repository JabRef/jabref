/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
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
