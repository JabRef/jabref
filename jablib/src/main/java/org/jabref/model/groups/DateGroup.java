package org.jabref.model.groups;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.strings.LatexToUnicodeAdapter;

/**
 * Matches based on a latex free last name in a specified field. The field is parsed as an author list and the last names are resolved of latex.
 */
public class DateGroup extends AbstractGroup {

    private final Field field;
    String date; // bucket key: "YYYY" or "YYYY-MM" or "YYYY-MM-DD"

    public DateGroup(String groupName, GroupHierarchyType context, Field searchField, String date) {
        super(groupName, context);
        field=searchField;
        this.date = date;
    }

    

    static Optional<Integer> extractYear(Field field, BibEntry bibEntry) {
        return bibEntry.getField(field)
                       .flatMap(Date::parse)
                       .flatMap(Date::getYear);
    }

    static Optional<Date> extractDate(Field field, BibEntry entry) {
        boolean isCore =
                (field == StandardField.DATE)
             || (field == StandardField.YEAR)
             || (field == StandardField.MONTH)
             || (field == StandardField.DAY);

        if (isCore) {
            // use alias resolution so DATE <-> YEAR/MONTH/DAY works either way
            return entry.getFieldOrAlias(StandardField.DATE).flatMap(Date::parse);
        } else {
            // e.g. urldate or any custom date-like field
            return entry.getField(field).flatMap(Date::parse);
        }
    }

    static Optional<String> getDateKey(Date d, String dateKeyFormat) {
        int numOfdashes = (int) dateKeyFormat.chars().filter(ch -> ch == '-').count();
        // normalize parts
        Optional<Integer> y = d.getYear();
        return switch (numOfdashes) {
            case 0 -> y.map(val -> String.format("%04d", val)); // "YYYY"
            case 1 -> { // "YYYY-MM"
                if (d.getYear().isPresent() && d.getMonth().isPresent()) {
                    String out = String.format("%04d-%02d", d.getYear().get(), d.getMonth().get().getNumber());
                    yield Optional.of(out);
                } else {
                    yield Optional.empty();
                }
            }
            case 2 -> { // "YYYY-MM-DD"
                if (d.getYear().isPresent() && d.getMonth().isPresent() && d.getDay().isPresent()) {
                    String out = String.format("%04d-%02d-%02d",
                            d.getYear().get(),
                            d.getMonth().get().getNumber(),
                            d.getDay().get());
                    yield Optional.of(out);
                } else {
                    yield Optional.empty();
                }
            }
            default -> Optional.empty();
        };
    }


    @Override
    public boolean contains(BibEntry entry) {
        return extractDate(this.field, entry)
                .flatMap(d -> getDateKey(d, this.date))
                .map(key -> key.equals(this.date))
                .orElse(false);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new DateGroup(getName(), getHierarchicalContext(), this.field, this.date);
    }

    @Override
    public boolean isDynamic() {
        return true;
    }
}
