package org.jabref.logic.bibtex.comparator;

import java.text.Collator;
import java.text.ParseException;
import java.text.RuleBasedCollator;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;
import java.util.HashMap;

import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Month;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldProperty;
import org.jabref.model.entry.field.InternalField;
import org.jabref.model.entry.field.OrFields;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.metadata.SaveOrder;
import org.jabref.model.strings.StringUtil;

/**
 * A comparator for BibEntry fields
 */
public class FieldComparator implements Comparator<BibEntry> {

    private static final Collator COLLATOR = getCollator();

    enum FieldType {
        NAME, TYPE, YEAR, MONTH, OTHER
    }

    private final OrFields fields;
    private final FieldType fieldType;
    private final boolean isNumeric;
    private final int multiplier;

    public FieldComparator(Field field) {
        this(new OrFields(field), false);
    }

    public FieldComparator(SaveOrder.SortCriterion sortCriterion) {
        this(new OrFields(sortCriterion.field), sortCriterion.descending);
    }

    public FieldComparator(OrFields fields, boolean descending) {
        this.fields = fields;
        fieldType = determineFieldType();
        isNumeric = this.fields.getPrimary().isNumeric();
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
        if (InternalField.TYPE_HEADER == this.fields.getPrimary()) {
            return FieldType.TYPE;
        } else if (this.fields.getPrimary().getProperties().contains(FieldProperty.PERSON_NAMES)) {
            return FieldType.NAME;
        } else if (StandardField.YEAR == this.fields.getPrimary()) {
            return FieldType.YEAR;
        } else if (StandardField.MONTH == this.fields.getPrimary()) {
            return FieldType.MONTH;
        } else {
            return FieldType.OTHER;
        }
    }

    private String getFieldValue(BibEntry entry) {
        for (Field aField : fields.getFields()) {
            Optional<String> o = entry.getFieldOrAliasLatexFree(aField);
            if (o.isPresent()) {
                return o.get();
            }
        }
        return null;
    }

	static HashMap<String, Boolean> branchCoverage = new HashMap<String, Boolean>();
    @Override
    public int compare(BibEntry e1, BibEntry e2) {
        String f1;
        String f2;

        if (fieldType == FieldType.TYPE) {
			branchCoverage.put("0.T", true);
            // Sort by type.
            f1 = e1.getType().getDisplayName();
            f2 = e2.getType().getDisplayName();
        } else {
			branchCoverage.put("0.F", true);
            f1 = getFieldValue(e1);
            f2 = getFieldValue(e2);
        }

        // Catch all cases involving null:
        if ((f1 == null) && (f2 == null)) {
			branchCoverage.put("1.0.T", true);
            return 0;
        } else if (f1 == null) {
			branchCoverage.put("1.1.T", true);
            return -multiplier;
        } else if (f2 == null) {
			branchCoverage.put("1.*.F", true);
            return +multiplier;
        }

        // Now we know that both f1 and f2 are != null
        if (fieldType == FieldType.NAME) {
			branchCoverage.put("2.0.T", true);
            f1 = AuthorList.fixAuthorForAlphabetization(f1);
            f2 = AuthorList.fixAuthorForAlphabetization(f2);
        } else if (fieldType == FieldType.YEAR) {
			branchCoverage.put("2.1.T", true);
            int f1year;
            try {
                f1year = StringUtil.intValueOf(f1);
            } catch (NumberFormatException ex) {
			    branchCoverage.put("2.1.1.Catch", true);
                f1year = 0;
            }
            int f2year;
            try {
                f2year = StringUtil.intValueOf(f2);
            } catch (NumberFormatException ex) {
			    branchCoverage.put("2.1.2.Catch", true);
                f2year = 0;
            }
            int comparisonResult = Integer.compare(f1year, f2year);
            return comparisonResult * multiplier;
        } else if (fieldType == FieldType.MONTH) {
			branchCoverage.put("2.2.Catch", true);
            int month1 = Month.parse(f1).map(Month::getNumber).orElse(-1);
            int month2 = Month.parse(f2).map(Month::getNumber).orElse(-1);
            return Integer.compare(month1, month2) * multiplier;
        }else{            
			branchCoverage.put("2.X.F", true);
        }

        if (isNumeric) {
			branchCoverage.put("3.1.T", true);
            // Cannot use {@link org.jabref.logic.util.comparator.NumericFieldComparator}, because
            //   we need the "Else both are strings" branch and
            //   unparseable strings are sorted differently.
            int i1;
            boolean i1present;
            try {
                i1 = StringUtil.intValueOf(f1);
                i1present = true;
            } catch (NumberFormatException ex) {
			    branchCoverage.put("3.1.1.Catch", true);
                i1 = 0;
                i1present = false;
            }
            int i2;
            boolean i2present;
            try {
                i2 = StringUtil.intValueOf(f2);
                i2present = true;
            } catch (NumberFormatException ex) {
			    branchCoverage.put("3.1.2.Catch", true);
                i2 = 0;
                i2present = false;
            }

            if (i1present && i2present) {
			    branchCoverage.put("3.1.1.T", true);
                // Ok, parsing was successful. Update f1 and f2:
                return Integer.compare(i1, i2) * multiplier;
            } else if (i1present) {
			    branchCoverage.put("3.1.2.T", true);
                // The first one was parsable, but not the second one.
                // This means we consider one < two
                return -1 * multiplier;
            } else if (i2present) {
			    branchCoverage.put("3.1.3.T", true);
                // The second one was parsable, but not the first one.
                // This means we consider one > two
                return multiplier;
            }else{
			    branchCoverage.put("3.1.4.T", true);
            }
            // Else none of them were parseable, and we can fall back on comparing strings.
        }

        String ours = f1.toLowerCase(Locale.ENGLISH);
        String theirs = f2.toLowerCase(Locale.ENGLISH);
        return COLLATOR.compare(ours, theirs) * multiplier;
    }
}
