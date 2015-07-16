package net.sf.jabref.external;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.sf.jabref.BibtexEntry;
import net.sf.jabref.BibtexEntryType;
import net.sf.jabref.BibtexFields;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class RegExpFileSearchTests {

    private String filesDirectory = "/src/test/resources/net/sf/jabref/imports/unlinkedFilesTestFolder";

    
    @Before
    public void setUp(){
        Globals.prefs = JabRefPreferences.getInstance();
    }

    @Test
    public void testFindFiles() {
        //given
        List<BibtexEntry> entries = new ArrayList<BibtexEntry>();
        BibtexEntry entry = new BibtexEntry("123", BibtexEntryType.ARTICLE);
        entry.setField(BibtexFields.KEY_FIELD, "pdfInDatabase");
        entry.setField("year", "2001");
        entries.add(entry);

        List<String> extensions = Arrays.asList("pdf");

        List<File> dirs = Arrays.asList(new File(filesDirectory));

        //when
        Map<BibtexEntry, java.util.List<File>> result = RegExpFileSearch.findFilesForSet(entries, extensions, dirs,
                "**/[bibtexkey].*\\\\.[extension]");

        //then
        assertEquals(1, result.keySet().size());
    }
    
    @After
    public void tearDown(){
        Globals.prefs = null;
    }

}
