package org.jabref.model.groups;

public enum GroupHierarchyType {

    /**
     * Group's contents are independent of its hierarchical position.
     */
    INDEPENDENT,

    /**
     * Group's content is the intersection of its own content with its supergroup's content.
     */
    REFINING, // INTERSECTION

    /**
     * Group's content is the union of its own content with its subgroups' content.
     */
    INCLUDING; // UNION

    /**
     * Returns the hierarchy type from its position in this enum.
     * If the specified position is out of the enums bounds, then {@link #INDEPENDENT} is returned.
     */
    public static GroupHierarchyType getByNumberOrDefault(int type) {
        GroupHierarchyType[] types = values();
        if (type >= 0 && type < types.length) {
            return types[type];
        } else {
            return INDEPENDENT;
        }
    }
}
