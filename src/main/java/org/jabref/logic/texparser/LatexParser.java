package org.jabref.logic.texparser;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.jabref.model.texparser.LatexParserResult;
import org.jabref.model.texparser.LatexParserResults;

/**
 * Parses a LaTeX file
 */
public interface LatexParser {

    /**
     * For testing purposes.
     *
     * @param citeString String that contains a citation
     * @return a LatexParserResult, where Path is "" and lineNumber is 1
     */
    LatexParserResult parse(String citeString);

    /**
     * Parse a single LaTeX file.
     *
     * @param latexFile Path to a LaTeX file
     * @return Optional LatexParserResult, which contains all data related to the bibliographic entries, or empty if the file does not exist
     */
    Optional<LatexParserResult> parse(Path latexFile);

    /**
     * Parse a list of LaTeX files.
     *
     * @param latexFiles List of Path objects linked to a LaTeX file
     * @return a LatexParserResults, which contains all data related to the bibliographic entries
     */
    LatexParserResults parse(List<Path> latexFiles);
}
