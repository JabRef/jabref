package org.jabref.logic.importer.util;

import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.entry.identifier.MathSciNetId;
import org.jabref.model.strings.StringUtil;

public class IdentifierParser {
    private final BibEntry entry;

    public IdentifierParser(BibEntry entry) {
        Objects.requireNonNull(entry);
        this.entry = entry;
    }

    public Optional<? extends Identifier> parse(Field field, String input) {
        if (StringUtil.isBlank(input)) {
            return Optional.empty();
        }

        if (StandardField.DOI.equals(field)) {
            return DOI.parse(input);
        } else if (StandardField.ISBN.equals(field)) {
            return ISBN.parse(input);
        } else if (StandardField.EPRINT.equals(field)) {
            return parseEprint(input);
        } else if (StandardField.MR_NUMBER.equals(field)) {
            return MathSciNetId.parse(input);
        }

        return Optional.empty();
    }

    private Optional<? extends Identifier> parseEprint(String eprint) {
        Optional<String> eprintType = entry.getField(StandardField.EPRINTTYPE);
        return ArXivIdentifier.parse(eprint);
    }
}
