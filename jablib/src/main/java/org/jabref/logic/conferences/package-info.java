/* Project policy
 > [!IMPORTANT]
 > This project does not accept fully AI-generated pull requests. AI tools may only be used for assistance. You must understand and take responsibility for every change you submit.
 >
 > Read and follow:
 > • [AGENTS.md](./AGENTS.md)
 > • [CONTRIBUTING.md](./CONTRIBUTING.md)
*/
/// Conference abbreviation configuration and lookup.
/// [org.jabref.logic.conferences.ConferenceAbbreviationLoader] loads built-in and external lists without LTWA.
/// [org.jabref.logic.conferences.ConferenceAbbreviationRepository] loads conference records without LTWA data.
/// [org.jabref.logic.conferences.ConferenceAbbreviationPreferences] keeps external conference lists separate
/// from journal settings. Shared record and lookup types are in [org.jabref.logic.journals].
///
/// @see <a href="https://devdocs.jabref.org/architecture-and-components.html#preferences">Preferences</a>
@NullMarked
package org.jabref.logic.conferences;

import org.jspecify.annotations.NullMarked;
