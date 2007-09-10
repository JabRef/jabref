package net.sf.jabref;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is a class compiled under Java 1.4.2 that will start the real JabRef and
 * print some warnings if no Java 1.5 and higher and no JRE from Sun
 * Microsystems is found.
 * 
 * Caution: We cannot use any other class from JabRef here (for instance no
 * calls to Globals.lang() are possible), since then it could not be run using
 * Java 1.4.
 * 
 * @author oezbek
 * 
 */
public class JabRefMain {

    /**
     * @param args
     *            We will pass these arguments to JabRef later.
     */
    public static void main(String[] args) {

        String javaVersion = System.getProperty("java.version", null);

        if (javaVersion.compareTo("1.5") < 0) {
            String javaVersionWarning = "\n" + 
                "WARNING: You are running Java version 1.4 or lower (" + javaVersion + " to be exact).\n" +
                "         JabRef needs at least a Java Runtime Environment 1.5 or higher.\n" +
                "         JabRef should not start properly and output an error message\n" +
                "         (probably java.lang.UnsupportedClassVersionError ... (Unsupported major.minor version 49.0)\n" +
                "         See http://jabref.sf.net/faq.php for more information.\n";
            
            System.out.println(javaVersionWarning);
        }

        String javaVendor = System.getProperty("java.vendor", null);
        if (javaVendor.indexOf("Sun Microsystems") == -1) {
            System.out.println("\n" + 
                    "WARNING: You are not running a Java version from Sun Microsystems.\n" +
                    "         Your java vendor is: " + javaVendor + "\n" +
                    "         If JabRef crashes please consider switching to a Sun Java Runtime.\n" +
                    "         See http://jabref.sf.net/faq.php for more information.\n");
        }

        try {
            // We need to load this class dynamically, or otherwise the Java 
            // runtime would crash while loading JabRefMain itself.
            Method method = Class.forName("net.sf.jabref.JabRef").getMethod(
                "main", new Class[] { args.getClass() });
            method.invoke(null, new Object[] { args });

        } catch (InvocationTargetException e) {
            System.out.println("ERROR while starting or running JabRef:\n");
            e.getCause().printStackTrace();
            System.out.println();
            System.out.println(
                    "Please tell the JabRef developers about this by writing a bug report.\n" +
                    "You can find our bug tracker at http://sourceforge.net/tracker/?atid=600306&group_id=92314\n" +
                    "If the bug has already been reported there, please add your comments to the existing bug.\n" +
                    "If the bug has not been reported yet, then we need the complete error message given above\n" +
                    "and a description of what you did before the error occured.\n" +
                    "In most cases we also need your JabRef version, the java version\n" +
                    "(use 'java -version' for this) and the operating system you are using.\n" +
            		"\n" +
            		"We are sorry for the trouble and thanks for reporting problems with JabRef!\n");
        } catch (SecurityException e) {
            System.out.println("ERROR: You are running JabRef in a sandboxed"
                + " environment that does not allow it to be started.");
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.out
                .println("This error should not happen."
                    + " Write an email to the JabRef developers and tell them 'NoSuchMethodException in JabRefMain'");
        } catch (ClassNotFoundException e) {
            System.out
                .println("This error should not happen."
                    + " Write an email to the JabRef developers and tell them 'ClassNotFoundException in JabRefMain'");
        } catch (IllegalArgumentException e) {
            System.out
                .println("This error should not happen."
                    + " Write an email to the JabRef developers and tell them 'IllegalArgumentException in JabRefMain'");
        } catch (IllegalAccessException e) {
            System.out
                .println("This error should not happen."
                    + " Write an email to the JabRef developers and tell them 'IllegalAccessException in JabRefMain'");
        } catch (UnsupportedClassVersionError e){
            e.printStackTrace();
            
            System.out.println("\nThis means that your Java version (" + javaVersion + ") is not high enough to run JabRef.\nPlease update your Java Runtime Environment to a version 1.5 or higher.\n");
        }
    }
}
