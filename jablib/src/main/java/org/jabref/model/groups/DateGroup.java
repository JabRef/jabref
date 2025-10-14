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
import org.jabref.model.strings.LatexToUnicodeAdapter;

/**
 * Matches based on a latex free last name in a specified field. The field is parsed as an author list and the last names are resolved of latex.
 */
public class DateGroup extends AbstractGroup {

    private final Field field;
    String date;

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

    @Override
    public boolean contains(BibEntry entry) {
        return extractYear(this.field, entry)
        .map(y -> String.format("%04d", y).equals(date))
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
