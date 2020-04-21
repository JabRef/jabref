package org.jabref.logic.integrity;

import org.jabref.model.entry.BibEntry;

import java.util.List;

@FunctionalInterface
public interface Checker {
    List<IntegrityMessage> check(BibEntry entry);
}
