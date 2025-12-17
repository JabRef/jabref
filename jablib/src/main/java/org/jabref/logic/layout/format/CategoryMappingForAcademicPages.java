package org.jabref.logic.layout.format;

import java.util.Objects;

import org.jabref.logic.layout.LayoutFormatter;
import org.jabref.model.entry.types.EntryTypeFactory;

/*
This formatter is strictly used to map the category field in a academic pages markdown correctly:
InProceedings --> conferences
Article --> journals
all other types --> manuscripts
 */
public class CategoryMappingForAcademicPages implements LayoutFormatter {
    public String format(String EntryType) {
        if (Objects.equals(EntryTypeFactory.parse(EntryType).getDisplayName(), "InProceedings")) {
            return "conferences";
        }
        if (Objects.equals(EntryTypeFactory.parse(EntryType).getDisplayName(), "Article")) {
            return "journals";
        }
        return "manuscripts";
    }
}
