package org.jabref.model.database;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.metadata.FileDirectoryPreferences;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

public class BibDatabaseContextTest {

    // The 'currentWorkingDir' variable is used to re-create the part
    // of the JabRef internal state that we get when the 'jabref
    // biblio.bib' command is invoked from a command line (on
    // Unix/Linux, but I guess on Windows clones as well). In the
    // above-mentioned command, the current working directory must be
    // used as a 'biblio.bib' parent directory. Since the current
    // working directory is different on various computers that invoke
    // the test, we can not hard-code it into the test but must
    // determine it at run-time:
    private String currentWorkingDir;

    // Store the minimal preferences for the
    // BibDatabaseContext.getFileDirectories(File,
    // FileDirectoryPreferences) incocation:
    private FileDirectoryPreferences preferences;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        Map<String, String> mapFieldDirs = new HashMap<>();
        preferences = new FileDirectoryPreferences("saulius", mapFieldDirs, true);
        currentWorkingDir = Paths.get(System.getProperty("user.dir")).toString();
    }

    @Test
    public void getFileDirectoriesWithEmptyDbParent() {
        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(new File("biblio.bib"));
        List<String> fileDirectories = dbContext.getFileDirectories( "file", preferences );
        assertEquals(Collections.singletonList(currentWorkingDir),
                fileDirectories);
    }

    @Test
    public void getFileDirectoriesWithRelativeDbParent() {
        String dbDirectory = "relative/subdir";
        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(new File(dbDirectory + "/" + "biblio.bib"));
        List<String> fileDirectories = dbContext.getFileDirectories("file", preferences);
        assertEquals(Collections.singletonList(currentWorkingDir + "/" + dbDirectory),
                fileDirectories);
    }

    @Test
    public void getFileDirectoriesWithRelativeDottedDbParent() {
        String dbDirectory = "./relative/subdir";
        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(new File(dbDirectory + "/" + "biblio.bib"));
        List<String> fileDirectories = dbContext.getFileDirectories("file", preferences);
        assertEquals(Collections.singletonList(currentWorkingDir + "/" + dbDirectory),
                fileDirectories);
    }

    @Test
    public void getFileDirectoriesWithAbsoluteDbParent() {
        String dbDirectory = "/absolute/subdir";
        BibDatabaseContext dbContext = new BibDatabaseContext();
        dbContext.setDatabaseFile(new File(dbDirectory + "/" + "biblio.bib"));
        List<String> fileDirectories = dbContext.getFileDirectories("file", preferences);
        assertEquals(Collections.singletonList(dbDirectory), fileDirectories);
    }
}
