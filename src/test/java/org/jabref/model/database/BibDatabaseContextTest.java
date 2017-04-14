package org.jabref.model.database;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.jabref.model.metadata.FileDirectoryPreferences;

import org.junit.Rule;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertTrue;

public class BibDatabaseContextTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    FileDirectoryPreferences preferences;

    @Before
    public void setUp() {
        Map<String, String> mapFieldDirs = new HashMap<>();
        mapFieldDirs.put("pdf", "/home/saulius/jabref");
        preferences = new FileDirectoryPreferences("saulius", mapFieldDirs, true);
    }

    @Test
    public void getFileDirectoriesWithEmptyDbParent() {
        BibDatabaseContext dbContext = new BibDatabaseContext();
        List<String> fileDirectories = dbContext.getFileDirectories( "file", preferences );
        assertTrue(fileDirectories.get(0).equals(""));
    }

}
