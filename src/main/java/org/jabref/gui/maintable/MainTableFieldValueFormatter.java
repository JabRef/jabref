package org.jabref.gui.maintable;

import java.util.Optional;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.OrFields;
import org.jabref.preferences.PreferencesService;

import static org.jabref.gui.maintable.MainTableNameFormatPreferences.AbbreviationStyle;
import static org.jabref.gui.maintable.MainTableNameFormatPreferences.DisplayStyle;

public class MainTableFieldValueFormatter {
    private final DisplayStyle displayStyle;
    private final AbbreviationStyle abbreviationStyle;

    MainTableFieldValueFormatter(PreferencesService preferences) {
        MainTableNameFormatPreferences nameFormatPreferences = preferences.getMainTableNameFormatPreferences();
        this.displayStyle = nameFormatPreferences.getDisplayStyle();
        this.abbreviationStyle = nameFormatPreferences.getAbbreviationStyle();
    }

    /**
     * Format fields for {@link BibEntryTableViewModel}, according to user preferences and with latex translated to
     * unicode if possible.
     *
     * @param fields the fields argument of {@link BibEntryTableViewModel#getFields(OrFields)}.
     * @param entry the BibEntry of {@link BibEntryTableViewModel}.
     * @param database the BibDatabase of {@link BibEntryTableViewModel}.
     * @return The formatted name field.
     */
    public String formatFieldsValuesLatexFree(final OrFields fields, final BibEntry entry,
                                              final BibDatabase database) {
        for (Field field : fields) {
            if (field.getProperties().contains(FieldProperty.PERSON_NAMES)) {
                Optional<String> name = entry.getResolvedFieldOrAlias(field, database);

                if (name.isPresent()) {
                    return formatFieldWithAuthorValueLatexFree(name.get());
                }
            } else {
                Optional<String> content = entry.getResolvedFieldOrAliasLatexFree(field, database);

                if (content.isPresent()) {
                    return content.get();
                }
            }
        }
        return "";
    }

    /**
     * Format a name field for the table, according to user preferences and with latex expressions translated if
     * possible.
     *
     * @param nameToFormat The contents of the name field.
     * @return The formatted name field.
     */
    public String formatFieldWithAuthorValueLatexFree(final String nameToFormat) {
        if (nameToFormat == null) {
            return null;
        }

        AuthorList authors = AuthorList.parse(nameToFormat);

        if (((displayStyle == DisplayStyle.FIRSTNAME_LASTNAME)
                || (displayStyle == DisplayStyle.LASTNAME_FIRSTNAME))
                && abbreviationStyle == AbbreviationStyle.LASTNAME_ONLY) {
            return authors.getAsLastNamesLatexFree(false);
        }

        return switch (displayStyle) {
            case AS_IS -> authors.getAsUnformattedLatexFree();
            case FIRSTNAME_LASTNAME -> authors.getAsFirstLastNamesLatexFree(
                    abbreviationStyle == AbbreviationStyle.FULL,
                    false);
            case LASTNAME_FIRSTNAME -> authors.getAsLastFirstNamesLatexFree(
                    abbreviationStyle == AbbreviationStyle.FULL,
                    false);
            case NATBIB -> authors.getAsNatbibLatexFree();
        };
    }
}
