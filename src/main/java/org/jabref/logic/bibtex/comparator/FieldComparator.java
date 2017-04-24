package org.jabref.logic.bibtex.comparator;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.FieldProperty;
import org.jabref.model.entry.InternalBibtexFields;
import org.jabref.model.entry.Month;
import org.jabref.model.metadata.SaveOrderConfig;
import org.jabref.model.strings.StringUtil;

/**
 * A comparator for BibEntry fields
 */
public class FieldComparator implements Comparator<BibEntry> {

    private static final Collator COLLATOR = getCollator();

    enum FieldType {
        NAME, TYPE, YEAR, MONTH, OTHER
    }

    private final String[] field;
    private final String fieldName;
    private final FieldType fieldType;
    private final boolean isNumeric;
    private final int multiplier;

    public FieldComparator(String field) {
        this(field, false);
    }

    public FieldComparator(SaveOrderConfig.SortCriterion sortCriterion) {
        this(sortCriterion.field, sortCriterion.descending);
    }

    public FieldComparator(String field, boolean descending) {
        this.fieldName = Objects.requireNonNull(field);
        this.field = fieldName.split(FieldName.FIELD_SEPARATOR);
        fieldType = determineFieldType();
        isNumeric = InternalBibtexFields.isNumeric(this.field[0]);
        multiplier = descending ? -1 : 1;
    }

    private static Collator getCollator() {
        try {
            return new RuleBasedCollator(
                    ((RuleBasedCollator) Collator.getInstance()).getRules().replace("<'\u005f'", "<' '<'\u005f'"));
        } catch (ParseException e) {
            return Collator.getInstance();
        }
    }

    private FieldType determineFieldType() {
        if (BibEntry.TYPE_HEADER.equals(this.field[0])) {
            return FieldType.TYPE;
        } else if (InternalBibtexFields.getFieldProperties(this.field[0]).contains(FieldProperty.PERSON_NAMES)) {
            return FieldType.NAME;
        } else if (FieldName.YEAR.equals(this.field[0])) {
            return FieldType.YEAR;
        } else if (FieldName.MONTH.equals(this.field[0])) {
            return FieldType.MONTH;
        } else {
            return FieldType.OTHER;
        }
    }

    private String getField(BibEntry entry) {
        for (String aField : field) {
            Optional<String> o = entry.getFieldOrAliasLatexFree(aField);
            if (o.isPresent()) {
                return o.get();
            }
        }
        return null;
    }

    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        String f1;
        String f2;

        if (fieldType == FieldType.TYPE) {
            // Sort by type.
            f1 = e1.getType();
            f2 = e2.getType();
        } else {
            // If the field is author or editor, we rearrange names so they are
            // sorted according to last name.
            f1 = getField(e1);
            f2 = getField(e2);
        }

        // Catch all cases involving null:
        if ((f1 == null) && (f2 == null)) {
            return 0;
        } else if (f1 == null) {
            return multiplier;
        } else if (f2 == null) {
            return -multiplier;
        }

        // Now we know that both f1 and f2 are != null
        if (fieldType == FieldType.NAME) {
            f1 = AuthorList.fixAuthorForAlphabetization(f1);
            f2 = AuthorList.fixAuthorForAlphabetization(f2);
        } else if (fieldType == FieldType.YEAR) {
            Integer f1year = StringUtil.intValueOfOptional(f1).orElse(0);
            Integer f2year = StringUtil.intValueOfOptional(f2).orElse(0);
            int comparisonResult = Integer.compare(f1year, f2year);
            return comparisonResult * multiplier;
        } else if (fieldType == FieldType.MONTH) {
            int month1 = Month.parse(f1).map(Month::getNumber).orElse(-1);
            int month2 = Month.parse(f2).map(Month::getNumber).orElse(-1);
            return Integer.compare(month1, month2) * multiplier;
        }

        if (isNumeric) {
            Optional<Integer> i1 = StringUtil.intValueOfOptional(f1);
            Optional<Integer> i2 = StringUtil.intValueOfOptional(f2);

            if ((i2.isPresent()) && (i1.isPresent())) {
                // Ok, parsing was successful. Update f1 and f2:
                return i1.get().compareTo(i2.get()) * multiplier;
            } else if (i1.isPresent()) {
                // The first one was parseable, but not the second one.
                // This means we consider one < two
                return -1 * multiplier;
            } else if (i2.isPresent()) {
                // The second one was parseable, but not the first one.
                // This means we consider one > two
                return 1 * multiplier;
            }
            // Else none of them were parseable, and we can fall back on comparing strings.
        }

        String ours = f1.toLowerCase(Locale.ENGLISH);
        String theirs = f2.toLowerCase(Locale.ENGLISH);
        return COLLATOR.compare(ours, theirs) * multiplier;
    }

    /**
     * Returns the field this Comparator compares by.
     *
     * @return The field name.
     */
    public String getFieldName() {
        return fieldName;
    }
}
