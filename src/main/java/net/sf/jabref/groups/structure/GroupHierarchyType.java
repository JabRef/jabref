package net.sf.jabref.groups.structure;

public enum GroupHierarchyType {

    /** Group's contents are independent of its hierarchical position. */
    INDEPENDENT,

    /**
     * Group's content is the intersection of its own content with its
     * supergroup's content.
     */
    REFINING, // INTERSECTION

    /**
     * Group's content is the union of its own content with its subgroups'
     * content.
     */
    INCLUDING; // UNION

    public static GroupHierarchyType getByNumber(int type) {
        GroupHierarchyType[] types = values();
        if(type >= 0 && type < types.length) {
            return types[type];
        } else {
            return null;
        }
    }
}
