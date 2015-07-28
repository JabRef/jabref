package net.sf.jabref.journals.logic;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

public class AbbreviationTool {

    public static void main(String[] args) throws IOException {
        JournalAbbreviationRepository ap = new JournalAbbreviationRepository();
        ap.readJournalListFromFile(new File(args[0]));
        Files.write(ap.toPropertiesString(), new File(args[0]), Charsets.UTF_8);
    }

}
