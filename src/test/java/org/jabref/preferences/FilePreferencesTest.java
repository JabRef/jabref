package org.jabref.preferences;
import org.junit.jupiter.api.Test;

import java.util.Set;

public class FilePreferencesTest {

    @Test
    public void testGetMainFD(){
        FilePreferences filePreferences = new FilePreferences("user", "", false, "", "", false, false, null, Set.of(), false, null, false, false);

        filePreferences.setMainFileDirectory("");
        filePreferences.getMainFileDirectory();
        System.out.println("testing getMainFD 0 branch:");
        FilePreferences.printCov();

        FilePreferences.resetBranchCov();

        filePreferences.setMainFileDirectory("/path/path2");
        filePreferences.getMainFileDirectory();
        System.out.println("testing getMainFD 1 branch:");
        FilePreferences.printCov();
    }
}
