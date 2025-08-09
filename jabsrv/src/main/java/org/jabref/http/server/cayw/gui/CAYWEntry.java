package org.jabref.http.server.cayw.gui;

import java.util.Objects;

import org.jabref.model.entry.BibEntry;

public record CAYWEntry(
        BibEntry bibEntry,
        String label, // Used in the list
        String shortLabel, // Used on the buttons ("chips")
        String description // Used when hovering and used as bases on the second line
) {

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        CAYWEntry caywEntry = (CAYWEntry) o;
        return Objects.equals(bibEntry(), caywEntry.bibEntry());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bibEntry());
    }
}
