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

    /**
     * Gets the field used for date extraction.
     * Required by GroupSerializer for serialization.
     *
     * @return The field (e.g., StandardField.DATE or StandardField.YEAR)
     */
    public Field getField() {
        return field;
    }

    /**
     * Gets the granularity level for date grouping.
     * Required by GroupSerializer for serialization.
     *
     * @return The granularity (YEAR, MONTH, or FULL_DATE)
     */
    public DateGranularity getGranularity() {
        return granularity;
    }

    @Override
    public Set<GroupTreeNode> createSubgroups(BibEntry entry) {
        LinkedHashSet<GroupTreeNode> out = new LinkedHashSet<GroupTreeNode>();

        DateGroup.extractDate(field, entry).ifPresent(date -> {
            switch (granularity) {
                case YEAR -> {
                    date.getYear().ifPresent(year -> {
                        String key = "%04d".formatted(year);
                        out.add(new GroupTreeNode(new DateGroup(key, GroupHierarchyType.INDEPENDENT, field, key)));
                    });
                }
                case MONTH ->
                        DateGroup.getDateKey(date, "YYYY-MM").ifPresent(key -> {
                            out.add(new GroupTreeNode(new DateGroup(key, GroupHierarchyType.INDEPENDENT, field, key)));
                        });
                case FULL_DATE ->
                        DateGroup.getDateKey(date, "YYYY-MM-DD").ifPresent(key -> {
                            out.add(new GroupTreeNode(new DateGroup(key, GroupHierarchyType.INDEPENDENT, field, key)));
                        });
            }
        });
        return out;
    }

    @Override
    public AbstractGroup deepCopy() {
        return new AutomaticDateGroup(this.name.getValue(), this.context, this.field, this.granularity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, granularity);
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
