package org.jabref.model.entry.types;

import org.jabref.model.entry.BibEntryType;
import org.jabref.model.util.OptionalUtil;

public class EntryTypeFactory {

    private EntryTypeFactory() {
    }

    /**
     * Checks whether two EntryTypeFactory are equal or not based on the equality of the type names and on the equality of
     * the required and optional field lists
     *
     * @param type1 the first EntryType to compare
     * @param type2 the secend EntryType to compare
     * @return returns true if the two compared entry types have the same name and equal required and optional fields
     */
    public static boolean isEqualNameAndFieldBased(BibEntryType type1, BibEntryType type2) {
        if ((type1 == null) && (type2 == null)) {
            return true;
        } else if ((type1 == null) || (type2 == null)) {
            return false;
        } else {
            return type1.getType().equals(type2.getType())
                    && type1.getRequiredFields().equals(type2.getRequiredFields())
                    && type1.getOptionalFields().equals(type2.getOptionalFields())
                    && type1.getSecondaryOptionalFields().equals(type2.getSecondaryOptionalFields());
        }
    }

    public static boolean isExclusiveBiblatex(EntryType type) {
        return isBiblatex(type) && !isBibtex(type);
    }

    private static boolean isBibtex(EntryType type) {
        return BibtexEntryTypeDefinitions.ALL.stream().anyMatch(bibEntryType -> bibEntryType.getType().equals(type));
    }

    private static boolean isBiblatex(EntryType type) {
        return BiblatexEntryTypeDefinitions.ALL.stream().anyMatch(bibEntryType -> bibEntryType.getType().equals(type));
    }

    public static EntryType parse(String typeName) {
        return OptionalUtil.orElse(StandardEntryType.fromName(typeName), new UnknownEntryType(typeName));
    }
}
