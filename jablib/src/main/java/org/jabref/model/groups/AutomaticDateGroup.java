package org.jabref.model.groups;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class AutomaticDateGroup extends AutomaticGroup {

    private final DateGranularity granularity;
    private final Field field;

    public AutomaticDateGroup(String name, GroupHierarchyType context, Field field) {
        super(name, context);

        this.field = field;
        granularity = DateGranularity.YEAR;
    }

    public AutomaticDateGroup(String name, GroupHierarchyType context, Field field, DateGranularity granularity) {
        super(name, context);
        this.field = field;
        this.granularity = granularity;
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        var out = new LinkedHashSet<GroupTreeNode>();

        DateGroup.extractDate(field, entry).ifPresent(d -> {
            switch (granularity) {
                case YEAR -> {
                    DateGroup.extractYear(field, entry).ifPresent(y -> {
                        String key = "%04d".formatted(y);
                        out.add(new GroupTreeNode(new DateGroup(key, GroupHierarchyType.INDEPENDENT, field, key)));
                    });
                }
                case MONTH -> DateGroup.getDateKey(d, "YYYY-MM").ifPresent(key -> {
                    out.add(new GroupTreeNode(new DateGroup(key, GroupHierarchyType.INDEPENDENT, field, key)));
                });
                case FULL_DATE -> DateGroup.getDateKey(d, "YYYY-MM-DD").ifPresent(key -> {
                    out.add(new GroupTreeNode(new DateGroup(key, GroupHierarchyType.INDEPENDENT, field, key)));
                });
            }
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
        return Objects.equals(field, that.field) && granularity == that.granularity;
    }
}
