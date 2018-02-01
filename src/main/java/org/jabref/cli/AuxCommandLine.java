package org.jabref.cli;

import java.nio.file.Paths;

import org.jabref.gui.auximport.AuxParserResultViewModel;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.model.auxparser.AuxParser;
import org.jabref.model.auxparser.AuxParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.strings.StringUtil;

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
            AuxParser auxParser = new DefaultAuxParser(database);
            AuxParserResult result = auxParser.parse(Paths.get(auxFile));
            subDatabase = result.getGeneratedBibDatabase();
            // print statistics
            System.out.println(new AuxParserResultViewModel(result).getInformation(true));
        }
        return subDatabase;
    }
}
