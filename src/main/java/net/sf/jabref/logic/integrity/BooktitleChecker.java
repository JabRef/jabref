package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class BooktitleChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        String field = FieldName.BOOKTITLE;
        Optional<String> value = entry.getField(field);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        if (value.get().toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("booktitle ends with 'conference on'"), entry, field));
        }

        return Collections.emptyList();
    }
}
