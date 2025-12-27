package org.jabref.logic.journals.quality;

import java.util.List;

/**
 * Interface for all quality checkers.
 */
public interface Checker {
    List<Finding> check(List<AbbreviationEntry> entries);
    String code();
}
