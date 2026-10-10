package org.jabref.logic.importer.util;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

import org.jabref.logic.exporter.MetaDataSerializer;
import org.jabref.model.database.BibDatabaseMode;
import org.jabref.model.entry.BibEntryType;
import org.jabref.model.entry.field.BibField;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.OrFields;

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

    /// Serialize the definition after ordering its field sets. The v2 format preserves custom field properties,
    /// but does not encode whether an optional field is important or detailed.
    private static String signature(BibEntryType entryType) {
        Collection<OrFields> required = entryType.getRequiredFields().stream()
                                                 .map(orFields -> new OrFields(sortedFields(orFields.getFields())))
                                                 .sorted(Comparator.comparing(FieldFactory::serializeOrFieldsV2, String.CASE_INSENSITIVE_ORDER))
                                                 .toList();
        Collection<BibField> allFields = entryType.getAllBibFields().stream()
                                                  .sorted(Comparator.comparing(field -> field.field().getName(), String.CASE_INSENSITIVE_ORDER))
                                                  .toList();
        BibEntryType orderedType = new BibEntryType(entryType.getType(), allFields, required);
        String serialized = MetaDataSerializer.serializeCustomEntryTypesV2(orderedType);
        String detailedFields = FieldFactory.serializeFieldsListV2(sortedFields(entryType.getDetailOptionalFields()));
        return (serialized + " detail[" + detailedFields + "]").toLowerCase(Locale.ROOT);
    }

    private static Collection<Field> sortedFields(Collection<Field> fields) {
        return fields.stream()
                     .sorted(Comparator.comparing(Field::getName, String.CASE_INSENSITIVE_ORDER))
                     .toList();
    }
}
