package net.sf.jabref.logic.bibtex.comparator;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import net.sf.jabref.logic.config.SaveOrderConfig;
import net.sf.jabref.logic.util.strings.StringUtil;
import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.FieldProperties;
import net.sf.jabref.model.entry.InternalBibtexFields;
import net.sf.jabref.model.entry.MonthUtil;

/**
 *
 * A comparator for BibEntry fields
 *
 * Initial Version:
 *
 * @author alver
 * @version Date: Oct 13, 2005 Time: 10:10:04 PM To
 *
 * TODO: Testcases
 *
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

    public FieldComparator(String field, boolean reversed) {
        this.fieldName = Objects.requireNonNull(field);
        this.field = fieldName.split(FieldName.FIELD_SEPARATOR);
        fieldType = determineFieldType();
        isNumeric = InternalBibtexFields.isNumeric(this.field[0]);

        if(fieldType == FieldType.MONTH) {
            /*
             * [ 1598777 ] Month sorting
             *
             * http://sourceforge.net/tracker/index.php?func=detail&aid=1598777&group_id=92314&atid=600306
             */
            multiplier = reversed ? 1 : -1;
        } else {
            multiplier = reversed ? -1 : 1;
        }
    }

    public FieldComparator(SaveOrderConfig.SortCriterion sortCriterion) {
        this(sortCriterion.field, sortCriterion.descending);
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
        if(BibEntry.TYPE_HEADER.equals(this.field[0])) {
            return FieldType.TYPE;
        } else if (InternalBibtexFields.getFieldExtras(this.field[0]).contains(FieldProperties.PERSON_NAMES)) {
            return FieldType.NAME;
        } else if (FieldName.YEAR.equals(this.field[0])) {
            return FieldType.YEAR;
        } else if(FieldName.MONTH.equals(this.field[0])) {
            return FieldType.MONTH;
        } else {
            return FieldType.OTHER;
        }
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
        } else if(f1 == null) {
            return multiplier;
        } else if (f2 == null) {
            return -multiplier;
        }

        // Now we now that both f1 and f2 are != null
        if (fieldType == FieldType.NAME) {
            f1 = AuthorList.fixAuthorForAlphabetization(f1);
            f2 = AuthorList.fixAuthorForAlphabetization(f2);
        } else if (fieldType == FieldType.YEAR) {
            Integer f1year = StringUtil.intValueOfWithNull(f1);
            Integer f2year = StringUtil.intValueOfWithNull(f2);
            int comparisonResult = Integer.compare(f1year == null ? 0 : f1year, f2year == null ? 0 : f2year);
            return comparisonResult * multiplier;
        } else if (fieldType == FieldType.MONTH) {
            return Integer.compare(MonthUtil.getMonth(f1).number, MonthUtil.getMonth(f2).number) * multiplier;
        }

        if (isNumeric) {
            Integer i1 = StringUtil.intValueOfWithNull(f1);
            Integer i2 = StringUtil.intValueOfWithNull(f2);

            if ((i2 != null) && (i1 != null)) {
                // Ok, parsing was successful. Update f1 and f2:
                return i1.compareTo(i2) * multiplier;
            } else if (i1 != null) {
                // The first one was parseable, but not the second one.
                // This means we consider one < two
                return -1 * multiplier;
            } else if (i2 != null) {
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

    private String getField(BibEntry entry) {
        for (String aField : field) {
            Optional<String> o = entry.getFieldOrAlias(aField);
            if (o.isPresent()) {
                return o.get();
            }
        }
        return null;
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
