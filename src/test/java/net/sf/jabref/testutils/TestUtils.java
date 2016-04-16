package net.sf.jabref.testutils;

import net.sf.jabref.JabRefGUI;
import net.sf.jabref.JabRef;

/**
 * UtilsClass for UnitTests.
 *
 * @author kahlert, cordes
 */
public class TestUtils {

    public static final String PATH_TO_TEST_BIBTEX = "src/test/resources/net/sf/jabref/bibtexFiles/test.bib";


    /**
     * Initialize JabRef. Can be cleaned up with
     * {@link TestUtils#closeJabRef()}
     *
     * @see TestUtils#closeJabRef()
     */
    public static void initJabRef() {
        String[] args = {"-p", " ", TestUtils.PATH_TO_TEST_BIBTEX};
        JabRef.main(args);
    }

    /**
     * Closes the current instance of JabRef.
     */
    public static void closeJabRef() {
        if (JabRefGUI.mainFrame != null) {
            JabRefGUI.mainFrame.dispose();
        }
    }

}
