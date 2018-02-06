package org.jabref.model.auxparser;

import java.nio.file.Path;

public interface AuxParser {
    /**
     * Executes the parsing logic and returns a result containing all information and the generated BibDatabase.
     *
     * @param auxFile Path to the LaTeX AUX file
     * @return an AuxParserResult containing the generated BibDatabase and parsing statistics
     */
    AuxParserResult parse(Path auxFile);
}
