package net.sf.jabref.logic.protectedterms;

import java.util.List;
import java.util.Objects;


public class ProtectedTermsList implements Comparable<ProtectedTermsList> {

    private final String description;
    private final List<String> termsList;
    private final String location;
    private final boolean internalList;
    private boolean enabled;


    public ProtectedTermsList(String description, List<String> termList, String location, boolean internalList) {
        this.description = Objects.requireNonNull(description);
        this.termsList = Objects.requireNonNull(termList);
        this.location = Objects.requireNonNull(location);
        this.internalList = internalList;
    }

    public ProtectedTermsList(String description, List<String> termList, String location) {
        this(description, termList, location, false);
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
        return String.join("\n", termsList);
    }

    @Override
    public int compareTo(ProtectedTermsList otherList) {
        return this.getDescription().compareTo(otherList.getDescription());
    }

    public boolean isInternalList() {
        return internalList;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
