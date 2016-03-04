package net.sf.jabref.cli;

import net.sf.jabref.logic.auxparser.AuxParser;
import net.sf.jabref.logic.auxparser.AuxParserResult;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.logic.util.strings.StringUtil;

public class AuxCommandLine {
    private final String auxFile;
    private final BibDatabase database;

    public AuxCommandLine(String auxFile, BibDatabase database) {
        this.auxFile = StringUtil.getCorrectFileName(auxFile, "aux");
        this.database = database;
    }

    public BibDatabase perform() {
        BibDatabase subDatabase = null;

        if (!auxFile.isEmpty() && (database != null)) {
            AuxParser auxParser = new AuxParser(auxFile, database);
            AuxParserResult result = auxParser.parse();
            subDatabase = result.getGeneratedBibDatabase();
            // print statistics
            System.out.println(result.getInformation(true));
        }
        return subDatabase;
    }
}
