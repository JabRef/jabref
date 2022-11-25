package org.jabref.logic.integrity;

import java.util.List;

import org.jabref.model.database.BibDatabase;

@FunctionalInterface
public interface DatabaseChecker {
    List<IntegrityMessage> check(BibDatabase database);
}
