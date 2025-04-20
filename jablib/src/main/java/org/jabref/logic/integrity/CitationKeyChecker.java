package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.StringUtil;

/**
 * Currently only checks the key if there is an author, year, and title present.
 */
public class CitationKeyChecker implements EntryChecker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> author = entry.getField(StandardField.AUTHOR);
        Optional<String> title = entry.getField(StandardField.TITLE);
        Optional<String> year = entry.getField(StandardField.YEAR);
        if (author.isEmpty() || title.isEmpty() || year.isEmpty()) {
            return Collections.emptyList();
        }

        if (StringUtil.isBlank(entry.getCitationKey())) {
            String authorTitleYear = entry.getAuthorTitleYear(100);
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("empty citation key") + ": " + authorTitleYear, entry, InternalField.KEY_FIELD));
        }

        return Collections.emptyList();
    }
}
