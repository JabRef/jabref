package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

import org.jabref.model.entry.Author;
import org.jabref.model.entry.AuthorList;

public class PersonNameStringConverter extends StringConverter<Author> {

    private final boolean autoCompFF;
    private final boolean autoCompLF;
    private final AutoCompleteFirstNameMode autoCompleteFirstNameMode;

    public PersonNameStringConverter(boolean autoCompFF, boolean autoCompLF, AutoCompleteFirstNameMode autoCompleteFirstNameMode) {
        this.autoCompFF = autoCompFF;
        this.autoCompLF = autoCompLF;
        this.autoCompleteFirstNameMode = autoCompleteFirstNameMode;
    }

    public PersonNameStringConverter(AutoCompletePreferences preferences) {
        if (preferences.getOnlyCompleteFirstLast()) {
            autoCompFF = true;
            autoCompLF = false;
        } else if (preferences.getOnlyCompleteLastFirst()) {
            autoCompFF = false;
            autoCompLF = true;
        } else {
            autoCompFF = true;
            autoCompLF = true;
        }

        autoCompleteFirstNameMode = preferences.getFirstNameMode();
    }

    @Override
    public String toString(Author author) {

        if (autoCompLF) {
            switch (autoCompleteFirstNameMode) {
                case ONLY_ABBREVIATED:
                    return author.getLastFirst(true);
                case ONLY_FULL:
                    return author.getLastFirst(false);
                case BOTH:
                    return author.getLastFirst(true);
                default:
                    break;
            }
        }
        if (autoCompFF) {
            switch (autoCompleteFirstNameMode) {
                case ONLY_ABBREVIATED:
                    return author.getFirstLast(true);
                case ONLY_FULL:
                    return author.getFirstLast(false);
                case BOTH:
                    return author.getFirstLast(true);
                default:
                    break;
            }
        }
        return author.getLastOnly();
    }

    @Override
    public Author fromString(String string) {
        return AuthorList.parse(string).getAuthor(0);
    }
}
