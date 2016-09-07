package net.sf.jabref.logic.importer;

import java.io.InputStream;
import java.util.List;

import net.sf.jabref.model.entry.BibEntry;

public interface Parser {

    List<BibEntry> parseEntries(InputStream inputStream) throws ParserException;
}
