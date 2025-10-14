package org.jabref.model.groups;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class AutomaticDateGroup extends AutomaticGroup{

    private final Field field;

    public AutomaticDateGroup(String name, GroupHierarchyType context, Field field) {
        super(name, context);

        this.field = field;
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        var out = new LinkedHashSet<GroupTreeNode>();
        DateGroup.extractYear(field, entry).ifPresent(y->{
            String year = String.format("%04d", y);
            DateGroup child = new DateGroup(year, GroupHierarchyType.INDEPENDENT, field, year);
            out.add(new GroupTreeNode(child));
        });
        return out;       
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticDateGroup(this.name.getValue(), this.context, this.field);

    }

    @Override
    public int hashCode() {
        return Objects.hash(field);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AutomaticDateGroup that = (AutomaticDateGroup) o;
        return Objects.equals(field, that.field);
    }

    
}
