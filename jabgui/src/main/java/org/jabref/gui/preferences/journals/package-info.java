/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
/// Preference pages for journal and conference abbreviation lists.
/// [JournalAbbreviationsTab] and [ConferenceAbbreviationsTab] share the editor and
/// [JournalAbbreviationsTabViewModel], while keeping their lists and settings separate.
/// File persistence uses [AbbreviationsFileViewModel]; lookup belongs to [org.jabref.logic.journals]
/// and [org.jabref.logic.conferences].
///
/// @see <a href="https://devdocs.jabref.org/code-howtos/journal-abbreviations.html">Abbreviation lists</a>
@NullMarked
package org.jabref.gui.preferences.journals;

import org.jspecify.annotations.NullMarked;
