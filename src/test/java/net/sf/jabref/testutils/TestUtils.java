package net.sf.jabref.testutils;

import net.sf.jabref.JabRef;

import java.security.Permission;

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
        disableSystemExit();
        try {
            String[] args = {"-p", " ", PATH_TO_TEST_BIBTEX};
            JabRef.main(args);
        } catch (ExitException ignored) {

        } finally {
            enableSystemExit();
        }
    }

    /**
     * Closes the current instance of JabRef.
     */
    public static void closeJabRef() {
        JabRef jabref = JabRef.singleton;
        if (jabref != null) {
            JabRef.jrf.dispose();
        }
    }


    @SuppressWarnings("serial")
    private static class ExitException extends SecurityException {
    }


    private static void disableSystemExit() {
        final SecurityManager securityManager = new SecurityManager() {

            public void checkPermission(Permission permission) {
                if (permission.getName().contains("exitVM")) {
                    throw new ExitException();
                }
            }
        };
        System.setSecurityManager(securityManager);
    }

    private static void enableSystemExit() {
        System.setSecurityManager(null);
    }

}
