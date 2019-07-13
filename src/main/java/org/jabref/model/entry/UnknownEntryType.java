package org.jabref.model.entry;

import org.jabref.model.strings.StringUtil;

public class UnknownEntryType implements EntryType {
    private final String name;

    public UnknownEntryType(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplayName() {
        return StringUtil.capitalizeFirst(name);
    }
}
