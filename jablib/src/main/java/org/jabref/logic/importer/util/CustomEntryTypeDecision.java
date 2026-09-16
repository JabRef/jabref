package org.jabref.logic.importer.util;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;

import com.google.common.hash.Hashing;
import org.jspecify.annotations.NullMarked;

/// Identifies the question JabRef asks when a library brings a custom entry type: store the definition from the file,
/// replacing the one stored at that time. A declined question is not asked again - unless one of the definitions changes.
@NullMarked
public class CustomEntryTypeDecision {

    private CustomEntryTypeDecision() {
    }

    /// Returns a fixed-length fingerprint, because a definition can be longer than a preference key or value may be.
    public static String fingerprint(BibEntryType typeFromFile, Optional<BibEntryType> storedType, BibDatabaseMode mode) {
        String decision = mode.getAsString() + ": " + signature(typeFromFile)
                + storedType.map(stored -> " replacing " + signature(stored)).orElse("");
        return Hashing.sha256().hashString(decision, StandardCharsets.UTF_8).toString();
    }

    /// Canonical form of what [org.jabref.model.entry.types.EntryTypeFactory#nameAndFieldsAreEqual(BibEntryType, BibEntryType)]
    /// compares: the field collections are sets, so their order must not change the signature
    private static String signature(BibEntryType entryType) {
        String required = entryType.getRequiredFields().stream()
                                   .map(orFields -> sortedNames(orFields.getFields()))
                                   .sorted()
                                   .collect(Collectors.joining(";"));
        return entryType.getType().getName().toLowerCase(Locale.ROOT)
                + " req[" + required + "]"
                + " opt[" + sortedNames(entryType.getOptionalFields().stream().map(BibField::field).toList()) + "]"
                + " detail[" + sortedNames(entryType.getDetailOptionalFields()) + "]";
    }

    private static String sortedNames(Collection<Field> fields) {
        return fields.stream()
                     .map(field -> field.getName().toLowerCase(Locale.ROOT))
                     .sorted()
                     .collect(Collectors.joining("/"));
    }
}
