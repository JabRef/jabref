package net.sf.jabref.gui;

import net.sf.jabref.model.entry.BibEntry;

/**
 * Entry containers work on a single entry, which can be asked for
 */
@FunctionalInterface
public interface EntryContainer {

    BibEntry getEntry();
}
