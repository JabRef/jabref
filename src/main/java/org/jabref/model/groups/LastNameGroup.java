package org.jabref.model.groups;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.strings.LatexToUnicodeAdapter;

/**
 * Matches based on a latex free last name in a specified field. The field is parsed as an author list and the last names are resolved of latex.
 */
public class LastNameGroup extends KeywordGroup {
    public LastNameGroup(String groupName, GroupHierarchyType context, Field searchField, String lastName) {
        super(groupName, context, searchField, LatexToUnicodeAdapter.format(lastName), true);
    }

    static List<String> getAsLastNamesLatexFree(Field field, BibEntry bibEntry) {
        return bibEntry.getField(field).stream()
                       .map(AuthorList::parse)
                       .map(AuthorList::latexFree)
                       .map(AuthorList::getAuthors)
                       .flatMap(Collection::stream)
                       .map(Author::getLast)
                       .flatMap(Optional::stream)
                       .collect(Collectors.toList());
    }

    @Override
    public boolean contains(BibEntry entry) {
        return getAsLastNamesLatexFree(getSearchField(), entry).stream().anyMatch(name -> name.equals(getSearchExpression()));
    }

    @Override
    public AbstractGroup deepCopy() {
        return new LastNameGroup(getName(), getHierarchicalContext(), getSearchField(), getSearchExpression());
    }
}
