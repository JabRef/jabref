package org.jabref.model.groups;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import org.jabref.model.entry.field.StandardField;

/**
 * This group contains entries, which were manually assigned to it.
 * Assignments are stored in the {@link StandardField#GROUPS} field.
 * Thus, internally, we represent {@link ExplicitGroup} as a special {@link WordKeywordGroup} operating on
 * {@link StandardField#GROUPS}.
 */
public class ExplicitGroup extends WordKeywordGroup {

    /**
     * Previous versions of JabRef stored the linked entries directly in the "jabref-meta" comment at the end of the
     * file. These keys are still parsed and stored in this field.
     */
    private final List<String> legacyEntryKeys = new ArrayList<>();
    public static Map<String,Boolean> branchCoverage = new HashMap<>();
    static{
        branchCoverage.put("equals_branch_1",false);
        branchCoverage.put("equals_branch_2",false);
        branchCoverage.put("equals_branch_3",false);
    }

    public ExplicitGroup(String name, GroupHierarchyType context, Character keywordSeparator) {
        super(name, context, StandardField.GROUPS, name, true, keywordSeparator, true);

    }




    public void addLegacyEntryKey(String key) {
        this.legacyEntryKeys.add(key);
    }

    @Override
    public AbstractGroup deepCopy() {
        ExplicitGroup copy = new ExplicitGroup(getName(), getHierarchicalContext(), keywordSeparator);
        copy.legacyEntryKeys.addAll(legacyEntryKeys);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            branchCoverage.put("equals_branch_1", true);
            return true;
        }
        if (!(o instanceof ExplicitGroup)) {
            branchCoverage.put("equals_branch_2", true);
            return false;
        }
        ExplicitGroup other = (ExplicitGroup) o;

        boolean result = Objects.equals(getName(), other.getName())
                && Objects.equals(getHierarchicalContext(), other.getHierarchicalContext())
                && Objects.equals(getIconName(), other.getIconName())
                && Objects.equals(getDescription(), other.getDescription())
                && Objects.equals(getColor(), other.getColor())
                && Objects.equals(isExpanded(), other.isExpanded())
                && Objects.equals(getLegacyEntryKeys(), other.getLegacyEntryKeys());
        branchCoverage.put("equals_branch_3", true);
        return result;
    }


    public static void printCoverage(){
        for(Map.Entry<String, Boolean> entry: branchCoverage.entrySet()){
            System.out.println("test equals()" +entry.getKey() + "was" + (entry.getValue()?"hit":"not hit"));
        }
    }


    public void clearLegacyEntryKeys() {
        legacyEntryKeys.clear();
    }

    public List<String> getLegacyEntryKeys() {
        return Collections.unmodifiableList(legacyEntryKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name.getValue(), context, legacyEntryKeys, iconName, color, description, isExpanded);
    }

    @Override
    public boolean isDynamic() {
        return false;
    }
}
