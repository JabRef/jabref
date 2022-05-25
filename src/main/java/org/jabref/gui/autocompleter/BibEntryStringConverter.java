package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

import org.jabref.model.entry.BibEntry;

public class BibEntryStringConverter extends StringConverter<BibEntry> {

    public BibEntryStringConverter() { }

    @Override
    public String toString(BibEntry entry) {
        return entry.getTitle().get();
    }

    @Override
    public BibEntry fromString(String string) {
        return null;
    }
}
