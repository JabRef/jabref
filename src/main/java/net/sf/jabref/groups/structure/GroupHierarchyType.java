/**
 * Copyright (C) 2015 JabRef contributors
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
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
