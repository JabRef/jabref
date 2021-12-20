package org.jabref.logic.msbib;

import org.jabref.model.entry.Author;

public class MsBibAuthor {

    private String firstName;
    private String middleName;
    private final Author author;
    private boolean isCorporate;
    private String affiliation;
    private String areaOfStudy;

    public MsBibAuthor(Author author) {
        this.author = author;

        StringBuilder sb = new StringBuilder();
        author.getFirst().ifPresent(firstNames -> {

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

    public MsBibAuthor(Author author, boolean isCorporate, String affiliation, String areaOfStudy) {
        this(author,isCorporate);
        this.affiliation = affiliation;
        this.areaOfStudy = areaOfStudy;
    }

    public String getFirstName() {

        if (!"".equals(firstName)) {
            return firstName;
        }
        return author.getFirst().orElse(null);
    }

    public String getMiddleName() {
        if ("".equals(middleName)) {
            return null;
        }
        return middleName;
    }

    public String getLastName() {
        return author.getLastOnly();
    }

    public String getFirstLast() {
        return author.getFirstLast(false);
    }

    public String getLastFirst() {
        return author.getLastFirst(false);
    }

    public String getAffiliation(){return this.affiliation;};

    public String getAreaOfStudy(){return this.areaOfStudy;};

    public boolean isCorporate() {
        return isCorporate;
    }
}
