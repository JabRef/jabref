package net.sf.jabref.logic.protectterms;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

import joptsimple.internal.Strings;

public class ProtectTermsList implements Comparable<ProtectTermsList> {

    private final String description;
    private final List<String> termsList;
    private final String location;


    public ProtectTermsList(String description, List<String> termList, String location) {
        this.description = Objects.requireNonNull(description);
        this.termsList = Objects.requireNonNull(termList);
        this.location = Objects.requireNonNull(location);
    }


    public String getDescription() {
        return description;
    }


    public List<String> getTermList() {
        return termsList;
    }


    public String getLocation() {
        return location;
    }

    public String getTermListing() {
        return Strings.join(termsList, "\n");
    }

    public void ensureUpToDate() throws IOException {
        // TODO Auto-generated method stub

    }

    @Override
    public int compareTo(ProtectTermsList otherList) {
        return this.getDescription().compareTo(otherList.getDescription());
    }

}
