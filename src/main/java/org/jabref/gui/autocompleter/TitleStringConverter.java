package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

import org.jabref.model.entry.BibEntry;

public class TitleStringConverter extends StringConverter<BibEntry> {

    public TitleStringConverter() { }

    @Override
    public String toString(BibEntry entry) {
        return entry.getTitle().get();
    }

    @Override
    public BibEntry fromString(String string) {
        return null;
    }
}
