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
