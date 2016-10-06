package net.sf.jabref.logic.importer;

import java.io.InputStream;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

/**
 * A parser converts an {@link InputStream} into a list of {@link BibEntry}.
 */
public interface Parser {

    List<BibEntry> parseEntries(InputStream inputStream) throws ParseException;
}
