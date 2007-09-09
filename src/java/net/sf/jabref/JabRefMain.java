package net.sf.jabref;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * This is a class compiled under Java 1.4.2 that will start the real JabRef, if
 * Java 1.5 or higher is installed.
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

        if (javaVersion.compareTo("1.4") <= 0) {
            System.out
                .println("WARNING: You are running Java version 1.4 or lower ("
                    + javaVersion + " to be exact).");
            System.out
                .println("         JabRef needs at least a Java Runtime Environment 1.5 or higher.");
            System.out
                .println("         JabRef should not start properly and output a strange error message.");
            System.out
                .println("         See http://jabref.sf.net/faq.php for more information.");
            System.out.println();
        }

        String javaVendor = System.getProperty("java.vendor", null);
        if (!javaVendor.contains("Sun Microsystems")) {
            System.out
                .println("WARNING: You are not running a Java version from Sun Microsystems.");
            System.out.println("         Your java vendor is: " + javaVendor);
            System.out
                .println("         If JabRef crashes please consider switching to a Sun Java Runtime.");
            System.out
                .println("         See http://jabref.sf.net/faq.php for more information.");
            System.out.println();
        }

        try {
            Method method = Class.forName("net.sf.jabref.JabRef").getMethod(
                "main", new Class[] { args.getClass() });
            method.invoke(null, new Object[] { args });

        } catch (InvocationTargetException e) {
            System.out.println("ERROR while starting JabRef:");
            e.getCause().printStackTrace();
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
        }

    }
}
