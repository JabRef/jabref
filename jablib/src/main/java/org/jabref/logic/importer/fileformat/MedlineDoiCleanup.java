package org.jabref.logic.importer.fileformat;

import java.util.Map;
import java.util.Optional;

import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.field.UnknownField;
import org.jabref.model.entry.identifier.DOI;

import org.jspecify.annotations.NullMarked;

@NullMarked
final class MedlineDoiCleanup {

    private MedlineDoiCleanup() {
    }

    static void cleanup(Map<Field, String> fields) {
        Optional.ofNullable(fields.remove(new UnknownField("article-doi")))
                .ifPresent(value -> fields.putIfAbsent(StandardField.DOI, DOI.parse(value).map(DOI::asString).orElse(value)));

        if (fields.containsKey(StandardField.DOI)) {
            return;
        }

        Optional.ofNullable(fields.get(new UnknownField("location-id")))
                .flatMap(DOI::findInText)
                .or(() -> Optional.ofNullable(fields.get(new UnknownField("source"))).flatMap(DOI::findInText))
                .map(DOI::asString)
                .ifPresent(doi -> fields.put(StandardField.DOI, doi));
    }
}
