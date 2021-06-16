package org.jabref.logic.texparser;

import java.nio.file.Path;
import java.util.List;

import org.jabref.model.texparser.LatexParserResult;

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
     * @return a LatexParserResult, which contains all data related to the bibliographic entries
     */
    LatexParserResult parse(Path latexFile);

    /**
     * Parse a list of LaTeX files.
     *
     * @param latexFiles List of Path objects linked to a LaTeX file
     * @return a LatexParserResult, which contains all data related to the bibliographic entries
     */
    LatexParserResult parse(List<Path> latexFiles);
}
