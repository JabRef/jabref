package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.logic.integrity.IntegrityCheck.Checker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class UrlChecker implements Checker {

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(FieldName.URL);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        if (!value.get().contains("://")) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain a protocol") + ": http[s]://, file://, ftp://, ...", entry,
                    FieldName.URL));
        }

        return Collections.emptyList();
    }
}
