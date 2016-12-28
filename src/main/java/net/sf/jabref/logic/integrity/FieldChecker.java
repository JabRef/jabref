package net.sf.jabref.logic.integrity;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import net.sf.jabref.model.entry.BibEntry;

public abstract class FieldChecker implements IntegrityCheck.Checker {
    protected final String field;

    public FieldChecker(String field) {
        this.field = field;
    }

    @Override
    public List<IntegrityMessage> check(BibEntry entry) {
        Optional<String> value = entry.getField(field);
        if (!value.isPresent()) {
            return Collections.emptyList();
        }

        return checkValue(value.get(), entry);
    }

    protected abstract List<IntegrityMessage> checkValue(String value, BibEntry entry);
}
