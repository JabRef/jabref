package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;

public class UrlChecker extends FieldChecker {

    public UrlChecker() {
        super(FieldName.URL);
    }

    @Override
    protected List<IntegrityMessage> checkValue(String value, BibEntry entry) {
        if (!value.contains("://")) {
            return Collections.singletonList(new IntegrityMessage(
                    Localization.lang("should contain a protocol") + ": http[s]://, file://, ftp://, ...", entry,
                    FieldName.URL));
        }

        return Collections.emptyList();
    }
}
