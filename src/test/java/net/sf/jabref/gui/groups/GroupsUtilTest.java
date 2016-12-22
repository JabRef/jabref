package net.sf.jabref.gui.groups;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.importer.fileformat.BibtexParser;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Assert;
import org.junit.Test;

public class GroupsUtilTest {


    @Test
    public void test() throws IOException {
        try (BufferedReader fr = Files.newBufferedReader(Paths.get("src/test/resources/testbib/testjabref.bib"),
                StandardCharsets.UTF_8)) {

            ParserResult result = BibtexParser.parse(fr,
                    JabRefPreferences.getInstance().getImportFormatPreferences());

            BibDatabase db = result.getDatabase();

            List<String> fieldList = new ArrayList<>();
            fieldList.add("author");

            Set<String> authorSet = AutoGroupDialog.findAuthorLastNames(db, fieldList);
            Assert.assertTrue(authorSet.contains("Brewer"));
            Assert.assertEquals(15, authorSet.size());

            Set<String> keywordSet = AutoGroupDialog.findDeliminatedWordsInField(db, "keywords", ";");
            Assert.assertTrue(keywordSet.contains("Brain"));
            Assert.assertEquals(60, keywordSet.size());

            Set<String> wordSet = AutoGroupDialog.findAllWordsInField(db, "month", "");
            Assert.assertTrue(wordSet.contains("Feb"));
            Assert.assertTrue(wordSet.contains("Mar"));
            Assert.assertTrue(wordSet.contains("May"));
            Assert.assertTrue(wordSet.contains("Jul"));
            Assert.assertTrue(wordSet.contains("Dec"));
            Assert.assertEquals(5, wordSet.size());
        }
    }

}
