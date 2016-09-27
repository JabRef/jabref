package net.sf.jabref.logic.msbib;

import java.util.List;

import net.sf.jabref.model.entry.AuthorList;
import net.sf.jabref.model.strings.StringUtil;

/**
 * @author Michael Wrighton, S M Mahbub Murshed
 *
 * S M Mahbub Murshed : added few functions for convenience. May 15, 2007
 *
 * History
 * Dec 16, 2011 - Changed parseName(String) to export authorname with
 * 				  more than 3 names correctly
 *
 */
public class PersonName {

    private String givenName;
    private String surname;
    private String middleName;


    public PersonName() {
        // Empty constructor
    }

    public PersonName(String name) {
        parseName(name);
    }

    public PersonName(String firstName, String middleName, String lastName) {
        givenName = firstName;
        this.middleName = middleName;
        surname = lastName;
    }

    private void parseName(String author) {
        String authorMod = AuthorList.fixAuthorLastNameFirst(author, false);

        //Formating names and replacing escape Char for ',' back to a comma
        //            XMLChars xmlChars = new XMLChars();
        //            authorMod = xmlChars.format(authorMod).replace("&#44;", ",");

        int endOfLastName = authorMod.indexOf(',');

        // Tokenize just the firstName and middleNames as we have the surname
        // before the comma.
        List<String> names = StringUtil.tokenizeToList(authorMod.substring(endOfLastName + 1).trim(), " \n\r");
        if (endOfLastName >= 0) {
            names.add(authorMod.substring(0, endOfLastName));
        }

        int amountOfNames = names.size();

        if (amountOfNames == 1) {
            surname = names.get(0);
        } else if (amountOfNames == 2) {
            givenName = names.get(0);
            surname = names.get(1);
        }
        else {
            givenName = names.get(0);
            middleName = "";
            for (int i = 1; i < (amountOfNames - 1); i++) {
                middleName += ' ' + names.get(i);
            }
            middleName = middleName.trim();
            surname = names.get(amountOfNames - 1);
        }
    }

    public String getGivenNames() {
        StringBuilder result = new StringBuilder();
        if (givenName != null) {
            result.append(givenName);
        }
        if (middleName != null) {
            result.append(' ').append(middleName);
        }
        return result.toString();
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String lastName) {
        surname = lastName;
    }

    public String getFirstname() {
        return givenName;
    }

    public void setFirstname(String firstName) {
        givenName = firstName;
    }

    public String getMiddlename() {
        return middleName;
    }

    public void setMiddlename(String middleName) {
        this.middleName = middleName;
    }

    public String getFullname() {
        StringBuilder fullName = new StringBuilder();
        if ((givenName != null) && !givenName.isEmpty()) {
            fullName.append(givenName).append(' ');
        }
        if ((middleName != null) && !middleName.isEmpty()) {
            fullName.append(middleName).append(' ');
        }
        if ((surname != null) && !surname.isEmpty()) {
            fullName.append(surname);
        }

        return fullName.toString().trim();
    }

    @Override
    public String toString() {
        return surname;
    }
}
