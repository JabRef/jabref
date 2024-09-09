package org.jabref.gui.autocompleter;

import javafx.util.StringConverter;

import org.jabref.logic.preferences.AutoCompleteFirstNameMode;
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
        switch (preferences.getNameFormat()) {
            case FIRST_LAST:
                autoCompFF = true;
                autoCompLF = false;
                break;
            case LAST_FIRST:
                autoCompFF = false;
                autoCompLF = true;
                break;
            default:
            case BOTH:
                autoCompFF = true;
                autoCompLF = true;
                break;
        }

        autoCompleteFirstNameMode = preferences.getFirstNameMode();
    }

    @Override
    public String toString(Author author) {
        if (autoCompLF) {
            switch (autoCompleteFirstNameMode) {
                case ONLY_ABBREVIATED:
                    return author.getFamilyGiven(true);
                case ONLY_FULL:
                    return author.getFamilyGiven(false);
                case BOTH:
                    return author.getFamilyGiven(true);
                default:
                    break;
            }
        }
        if (autoCompFF) {
            switch (autoCompleteFirstNameMode) {
                case ONLY_ABBREVIATED:
                    return author.getGivenFamily(true);
                case ONLY_FULL:
                    return author.getGivenFamily(false);
                case BOTH:
                    return author.getGivenFamily(true);
                default:
                    break;
            }
        }
        return author.getNamePrefixAndFamilyName();
    }

    @Override
    public Author fromString(String string) {
        return AuthorList.parse(string).getAuthor(0);
    }
}
