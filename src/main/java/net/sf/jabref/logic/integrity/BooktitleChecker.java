package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class BooktitleChecker extends FieldChecker {

    public BooktitleChecker() {
        super(FieldName.BOOKTITLE);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        if (value.toLowerCase(Locale.ENGLISH).endsWith("conference on")) {
            return Collections.singletonList(
                    new IntegrityMessage(Localization.lang("booktitle ends with 'conference on'"), entry, field));
        }

        return Collections.emptyList();
    }
}
