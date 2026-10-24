/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
/// Shared abbreviation records, parsing and lookup, with journal-specific loading and configuration.
/// [org.jabref.logic.journals.AbbreviationRepository] provides shared lookup operations;
/// [org.jabref.logic.journals.JournalAbbreviationRepository] adds LTWA support.
/// Conference-specific types are in [org.jabref.logic.conferences].
/// [org.jabref.logic.journals.AbbreviationPreferences] stores external list paths shared by the independent
/// [org.jabref.logic.journals.JournalAbbreviationPreferences] and
/// [org.jabref.logic.conferences.ConferenceAbbreviationPreferences] settings.
/// [org.jabref.logic.journals.AbbreviationLoader] shares resource extraction and external CSV loading.
/// [org.jabref.logic.journals.JournalAbbreviationLoader] loads journal data for
/// [org.jabref.logic.journals.JournalAbbreviationRepository].
///
/// @see <a href="https://devdocs.jabref.org/architecture-and-components.html#preferences">Preferences</a>
package org.jabref.logic.journals;
