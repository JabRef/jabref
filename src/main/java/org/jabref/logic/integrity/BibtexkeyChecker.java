package org.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.logic.integrity.IntegrityCheck.Checker;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.strings.StringUtil;

/**
 * Currently only checks the key if there is an author, year, and title present.
 */
public class BibtexkeyChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> valuekey = entry.getCiteKeyOptional();
        Optional<String> valueauthor = entry.getField(FieldName.AUTHOR);
        Optional<String> valuetitle = entry.getField(FieldName.TITLE);
        Optional<String> valueyear = entry.getField(FieldName.YEAR);
        String authortitleyear = entry.getAuthorTitleYear(100);

        if (!valueauthor.isPresent() || !valuetitle.isPresent() || !valueyear.isPresent()) {
            return Collections.emptyList();
        }

        if (StringUtil.isBlank(valuekey)) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("empty BibTeX key") + ": " + authortitleyear, entry, BibEntry.KEY_FIELD));
        }

        return Collections.emptyList();
    }
}
