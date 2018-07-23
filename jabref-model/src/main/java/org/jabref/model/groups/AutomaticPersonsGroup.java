package org.jabref.model.groups;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.util.OptionalUtil;

public class AutomaticPersonsGroup extends AutomaticGroup {

    private String field;

    public AutomaticPersonsGroup(String name, GroupHierarchyType context, String field) {
        super(name, context);
        this.field = field;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AutomaticPersonsGroup that = (AutomaticPersonsGroup) o;
        return Objects.equals(field, that.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticPersonsGroup(this.name, this.context, this.field);
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        Optional<AuthorList> authorList = entry.getLatexFreeField(field)
                .map(AuthorList::parse);
        return OptionalUtil.flatMap(authorList, AuthorList::getAuthors)
                .map(Author::getLast)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(lastName -> !lastName.isEmpty())
                .map(lastName -> new WordKeywordGroup(lastName, GroupHierarchyType.INDEPENDENT, field, lastName, true, ' ', true))
                .map(GroupTreeNode::new)
                .collect(Collectors.toSet());
    }

    public String getField() {
        return field;
    }
}
