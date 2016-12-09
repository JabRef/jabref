package net.sf.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import net.sf.jabref.logic.util.MetadataSerializationConfiguration;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.strings.StringUtil;

/**
 * This group contains entries, which were manually assigned to it.
 * Assignments are stored in the {@link FieldName#GROUPS} field.
 * Thus, internally, we represent {@link ExplicitGroup} as a special {@link SimpleKeywordGroup} operating on
 * {@link FieldName#GROUPS}.
 */
public class ExplicitGroup extends SimpleKeywordGroup {

    /**
     * Previous versions of JabRef stored the linked entries directly in the "jabref-meta" comment at the end of the
     * file. These keys are still parsed and stored in this field.
     */
    private final List<String> legacyEntryKeys = new ArrayList<>();

    public ExplicitGroup(String name, GroupHierarchyType context, Character keywordSeparator) {
        super(name, context, FieldName.GROUPS, name, true, keywordSeparator);
    }

    public void addLegacyEntryKey(String key) {
        this.legacyEntryKeys.add(key);
    }

    @Override
    public AbstractGroup deepCopy() {
        ExplicitGroup copy = new ExplicitGroup(getName(), getContext(), keywordSeparator);
        copy.legacyEntryKeys.addAll(legacyEntryKeys);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExplicitGroup)) {
            return false;
        }
        ExplicitGroup other = (ExplicitGroup) o;
        return Objects.equals(getName(), other.getName()) && Objects.equals(getHierarchicalContext(),
                other.getHierarchicalContext()) && Objects.equals(getLegacyEntryKeys(), other.getLegacyEntryKeys());
    }

    public void clearLegacyEntryKeys() {
        legacyEntryKeys.clear();
    }

    public List<String> getLegacyEntryKeys() {
        return Collections.unmodifiableList(legacyEntryKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, context, legacyEntryKeys);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }
}
