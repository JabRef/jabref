package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.model.entry.BibEntry;

@FunctionalInterface
public interface Checker {
    List<IntegrityMessage> check(BibEntry entry);
}
