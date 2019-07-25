package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.entry.BibEntry;

public interface TexParser {

    /**
     * For testing purposes.
     *
     * @param citeString String that contains a citation
     * @return a TexParserResult, where Path is foo/bar and lineNumber is 1
     */
    TexParserResult parse(String citeString);

    /**
     * Parse a single TEX file.
     *
     * @param texFile Path to a TEX file
     * @return a TexParserResult, which contains all data related to the bibliographic entries
     */
    TexParserResult parse(Path texFile);

    /**
     * Parse a list of TEX files.
     *
     * @param texFiles List of Path objects linked to a TEX file
     * @return a TexParserResult, which contains all data related to the bibliographic entries
     */
    TexParserResult parse(List<Path> texFiles);

    /**
     * Parse a list of TEX files for searching a given entry.
     *
     * @param entry the BibEntry we are looking for
     * @param texFiles List of Path objects linked to a TEX file
     * @return a TexParserResult, which contains all data related to the bibliographic entries
     */
    TexParserResult parse(BibEntry entry, List<Path> texFiles);
}
