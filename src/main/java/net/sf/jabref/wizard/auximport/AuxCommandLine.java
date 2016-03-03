package net.sf.jabref.wizard.auximport;

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
            AuxFileParser auxParser = new AuxFileParser(auxFile, database);
            subDatabase = auxParser.getGeneratedBibDatabase();
            // print statistics
            System.out.println(auxParser.getInformation(true));
        }
        return subDatabase;
    }
}
