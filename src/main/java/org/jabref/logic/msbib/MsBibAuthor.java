package org.jabref.logic.msbib;

import org.jabref.logic.formatter.bibtexfields.RemoveEnclosingBracesFormatter;
import org.jabref.model.entry.Author;

public class MsBibAuthor {

    private static final RemoveEnclosingBracesFormatter REMOVE_BRACES_FORMATTER = new RemoveEnclosingBracesFormatter();

    private String firstName;
    private String middleName;
    private final Author author;
    private boolean isCorporate;

    public MsBibAuthor(Author author) {
        this.author = author;

        StringBuilder sb = new StringBuilder();
        author.getGivenName().ifPresent(firstNames -> {

            String[] names = firstNames.split(" ");
            for (int i = 1; i < names.length; i++) {
                sb.append(names[i]);
                sb.append(" ");
            }
            this.middleName = sb.toString().trim();
            this.firstName = names[0];
        });
    }

    public MsBibAuthor(Author author, boolean isCorporate) {
        this(author);
        this.isCorporate = isCorporate;
    }

    public String getFirstName() {
        if (!"".equals(firstName)) {
            return firstName;
        }
        return author.getGivenName().orElse(null);
    }

    public String getMiddleName() {
        if ("".equals(middleName)) {
            return null;
        }
        return middleName;
    }

    public String getLastName() {
        return REMOVE_BRACES_FORMATTER.format(author.getNamePrefixAndFamilyName());
    }

    public String getFirstLast() {
        return author.getGivenFamily(false);
    }

    public String getLastFirst() {
        return author.getFamilyGiven(false);
    }

    public boolean isCorporate() {
        return isCorporate;
    }
}
