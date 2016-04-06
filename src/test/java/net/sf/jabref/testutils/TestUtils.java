package net.sf.jabref.testutils;

import net.sf.jabref.JabRef;
import net.sf.jabref.JabRefMain;

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
        JabRefMain.main(args);
    }

    /**
     * Closes the current instance of JabRef.
     */
    public static void closeJabRef() {
        if (JabRef.mainFrame != null) {
            JabRef.mainFrame.dispose();
        }
    }

}
