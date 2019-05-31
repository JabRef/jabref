package org.jabref.model.texparser;

import java.nio.file.Path;
import java.util.List;

public interface TexParser {

    /**
     * @param texFile Path to a TEX file
     * @return a TexParserResult, which contains the generated BibDatabase and all data related to the bibliographic
     * entries
     */
    TexParserResult parse(Path texFile);

    /**
     * @param texFiles List of Path objects linked to a TEX file
     * @return a list of TexParserResult objects, which contains the generated BibDatabase and all data related to the
     * bibliographic entries
     */
    TexParserResult parse(List<Path> texFiles);
}
