package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

import org.jabref.model.entry.BibEntry;

public class TitleStringConverter extends StringConverter<BibEntry> {

    public TitleStringConverter() { }

    @Override
    public String toString(BibEntry entry) {
        if(entry != null) {
            return entry.getTitle().get();
        }
        else{
            return null;
        }
    }

    @Override
    public BibEntry fromString(String string) {
        //TODO: need to properly implement fromString for titles
        return null;
    }
}
