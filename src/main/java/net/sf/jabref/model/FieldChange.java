/*
 * Copyright (C) 2003-2016 JabRef contributors.
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

package net.sf.jabref.model;

import java.util.Objects;

import net.sf.jabref.model.entry.BibEntry;

/**
 *
 */
public class FieldChange {

    private final BibEntry entry;
    private final String field;
    private final String oldValue;
    private final String newValue;


    public FieldChange(BibEntry entry, String field, String oldValue, String newValue) {
        this.entry = entry;
        this.field = field;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public BibEntry getEntry() {
        return this.entry;
    }

    public String getField() {
        return this.field;
    }

    public String getOldValue() {
        return this.oldValue;
    }

    public String getNewValue() {
        return this.newValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entry, field, newValue, oldValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FieldChange) {
            FieldChange other = (FieldChange) obj;
            if (entry == null) {
                if (other.entry != null) {
                    return false;
                }
            } else if (!entry.equals(other.entry)) {
                return false;
            }
            if (field == null) {
                if (other.field != null) {
                    return false;
                }
            } else if (!field.equals(other.field)) {
                return false;
            }
            if (newValue == null) {
                if (other.newValue != null) {
                    return false;
                }
            } else if (!newValue.equals(other.newValue)) {
                return false;
            }
            if (oldValue == null) {
                if (other.oldValue != null) {
                    return false;
                }
            } else if (!oldValue.equals(other.oldValue)) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "FieldChange [entry=" + entry.getCiteKeyOptional().orElse("") + ", field=" + field + ", oldValue="
                + oldValue + ", newValue=" + newValue + "]";
    }
}
