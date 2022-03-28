package org.jabref.logic.importer.util;

import java.util.Optional;
import java.util.function.Function;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.identifier.ArXivIdentifier;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.entry.identifier.ISBN;
import org.jabref.model.entry.identifier.Identifier;
import org.jabref.model.entry.identifier.MathSciNetId;
import org.jabref.model.strings.StringUtil;

public class IdentifierParser {
    public static Optional<? extends Identifier> parse(Field field, String input) {
        if (StringUtil.isBlank(input)) {
            return Optional.empty();
        }

        Function<String, Optional<? extends Identifier>> parser = getParserForField(field);
        return parser.apply(input);
    }

    private static Function<String, Optional<? extends Identifier>> getParserForField(Field field) {
        if (StandardField.DOI.equals(field)) {
            return DOI::parse;
        } else if (StandardField.ISBN.equals(field)) {
            return ISBN::parse;
        } else if (StandardField.EPRINT.equals(field)) {
            return ArXivIdentifier::parse;
        } else if (StandardField.MR_NUMBER.equals(field)) {
            return MathSciNetId::parse;
        }

        // By default, just return empty optional
        return input -> Optional.empty();
    }
}
