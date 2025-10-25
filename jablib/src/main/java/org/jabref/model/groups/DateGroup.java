package org.jabref.model.groups;

import java.util.Optional;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.Date;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.StandardField;

/**
 * Group that matches entries based on their date/year field value.
 * Supports grouping by YEAR, MONTH, or FULL_DATE granularity.
 */
public class DateGroup extends AbstractGroup {

    String date; // bucket key: "YYYY" or "YYYY-MM" or "YYYY-MM-DD"
    private final Field field;

    public DateGroup(String groupName, GroupHierarchyType context, Field searchField, String date) {
        super(groupName, context);
        field = searchField;
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

    /**
     * Returns a date group key from {@code d}.
     * Format is inferred from {@code dateKeyFormat} by dash count: 0→YYYY, 1→YYYY-MM, 2→YYYY-MM-DD.
     * If required parts are missing, returns {@link java.util.Optional#empty()}.
     *
     * @param d the parsed date
     * @param dateKeyFormat sample format used only for its number of dashes
     * @return optional key string in the requested granularity
     */
    static Optional<String> getDateKey(Date d, String dateKeyFormat) {
        int numOfdashes = (int) dateKeyFormat.chars().filter(ch -> ch == '-').count();
        Optional<Integer> y = d.getYear();
        return switch (numOfdashes) {
            case 0 ->
                    y.map(val -> "%04d".formatted(val)); // "YYYY"
            case 1 -> { // "YYYY-MM"
                if (d.getYear().isPresent() && d.getMonth().isPresent()) {
                    String out = "%04d-%02d".formatted(d.getYear().get(), d.getMonth().get().getNumber());
                    yield Optional.of(out);
                } else {
                    yield Optional.empty();
                }
            }
            case 2 -> { // "YYYY-MM-DD"
                if (d.getYear().isPresent() && d.getMonth().isPresent() && d.getDay().isPresent()) {
                    String out = "%04d-%02d-%02d".formatted(
                            d.getYear().get(),
                            d.getMonth().get().getNumber(),
                            d.getDay().get());
                    yield Optional.of(out);
                } else {
                    yield Optional.empty();
                }
            }
            default ->
                    Optional.empty();
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
