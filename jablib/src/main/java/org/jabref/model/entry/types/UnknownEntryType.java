package org.jabref.model.entry.types;

import java.util.Locale;
import java.util.Objects;

import org.jabref.model.strings.StringUtil;

import org.jspecify.annotations.NonNull;

public class UnknownEntryType implements EntryType {
    private final String name;

    public UnknownEntryType(@NonNull String name) {
        this.name = name.toLowerCase(Locale.ENGLISH);
    }

    @Override
    public String toString() {
        return "UnknownEntryType{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return StringUtil.capitalizeFirst(name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if ((o == null) || (getClass() != o.getClass())) {
            return false;
        }
        UnknownEntryType that = (UnknownEntryType) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
