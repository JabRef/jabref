package org.jabref.logic.protectedterms;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

import org.jabref.logic.util.OS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtectedTermsList implements Comparable<ProtectedTermsList> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedTermsList.class);

    private String description;
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

    public boolean createAndWriteHeading(String newDescription) {
        description = newDescription;
        return addProtectedTerm("# " + newDescription, true);
    }

    public boolean addProtectedTerm(String term) {
        return addProtectedTerm(term, false);
    }

    public boolean addProtectedTerm(String term, boolean create) {
        Objects.requireNonNull(term);
        // Cannot add to internal lists
        if (internalList) {
            return false;
        }

        Path p = Path.of(location);
        String s = OS.NEWLINE + term;
        try (BufferedWriter writer = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                create ? StandardOpenOption.CREATE : StandardOpenOption.APPEND)) {
            writer.write(s);
            termsList.add(term);
        } catch (IOException ioe) {
            LOGGER.warn("Problem adding protected term to list", ioe);
            return false;
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ProtectedTermsList)) {
            return false;
        }
        ProtectedTermsList otherList = (ProtectedTermsList) o;
        return (this.location.equals(otherList.location)) && (this.description.equals(otherList.description));
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, description);
    }
}
