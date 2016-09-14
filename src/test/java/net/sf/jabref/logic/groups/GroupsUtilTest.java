package net.sf.jabref.logic.groups;

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
import net.sf.jabref.model.groups.GroupsUtil;
import net.sf.jabref.preferences.JabRefPreferences;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

            Set<String> authorSet = GroupsUtil.findAuthorLastNames(db, fieldList);
            assertTrue(authorSet.contains("Brewer"));
            assertEquals(15, authorSet.size());

            Set<String> keywordSet = GroupsUtil.findDeliminatedWordsInField(db, "keywords", ";");
            assertTrue(keywordSet.contains("Brain"));
            assertEquals(60, keywordSet.size());

            Set<String> wordSet = GroupsUtil.findAllWordsInField(db, "month", "");
            assertTrue(wordSet.contains("Feb"));
            assertTrue(wordSet.contains("Mar"));
            assertTrue(wordSet.contains("May"));
            assertTrue(wordSet.contains("Jul"));
            assertTrue(wordSet.contains("Dec"));
            assertEquals(5, wordSet.size());
        }
    }

}
