package org.jabref.testutils;

import org.jabref.JabRefGUI;
import org.jabref.JabRefMain;

public class TestUtils {

    private static final String PATH_TO_TEST_BIBTEX = "src/test/resources/org/jabref/bibtexFiles/test.bib";

    /**
     * Initialize JabRef. Can be cleaned up with
     * {@link TestUtils#closeJabRef()}
     *
     * @see TestUtils#closeJabRef()
     */
    public static void initJabRef() {
        String[] args = {"-p", " ", TestUtils.PATH_TO_TEST_BIBTEX};
        JabRefMain.main(args);
    }

    /**
     * Closes the current instance of JabRef.
     */
    public static void closeJabRef() {
        if (JabRefGUI.getMainFrame() != null) {
            JabRefGUI.getMainFrame().dispose();
        }
    }

}
