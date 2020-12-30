package org.jabref.model.groups;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

/**
 * Matches entries based on the last name of a list of authors in a specified field.
 */
public class LastNameGroup extends KeywordGroup {
    public LastNameGroup(String name, GroupHierarchyType context, Field searchField, String searchExpression) {
        super(name, context, searchField, searchExpression, true);
    }

    static List<String> getAsLastNamesLatexFree(Field field, BibEntry bibEntry) {
        return bibEntry.getField(field)
                       .map(AuthorList::parse)
                       .map(AuthorList::getAuthors)
                       .map(authors ->
                               authors.stream()
                                      .map(Author::getLastLatexFree)
                                      .flatMap(Optional::stream)
                                      .collect(Collectors.toList()))
                       .orElse(Collections.emptyList());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return getAsLastNamesLatexFree(searchField, entry).stream().anyMatch(name -> name.equals(searchExpression));
    }

    @Override
    public AbstractGroup deepCopy() {
        return new LastNameGroup(getName(), context, searchField, searchExpression);
    }

    @Override
    public boolean equals(Object other) {
        if (super.equals(other)) {
            LastNameGroup otherGroup = (LastNameGroup) other;
            return (this.searchField.equals(otherGroup.searchField) &&
                    this.searchExpression.equals(otherGroup.searchExpression));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), searchField, searchExpression);
    }
}
