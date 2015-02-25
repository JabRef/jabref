/*  Copyright (C) 2003-2011 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package net.sf.jabref;

import java.awt.Component;
import java.awt.Dimension;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

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
   
    public static String exceptionToString(Throwable t){
        StringWriter stackTraceWriter = new StringWriter();
        t.printStackTrace(new PrintWriter(stackTraceWriter));
        return stackTraceWriter.toString();
    }
    
    /**
     * @param args
     *            We will pass these arguments to JabRef later.
     */
    public static void main(String[] args) {

        String javaVersion = System.getProperty("java.version", null);

        if (javaVersion.compareTo("1.6") < 0) {
            String javaVersionWarning = "\n" + 
                "WARNING: You are running Java version 1.6 or lower (" + javaVersion + " to be exact).\n" +
                "         JabRef needs at least a Java Runtime Environment 1.6 or higher.\n" +
                "         JabRef should not start properly and output an error message\n" +
                "         (probably java.lang.UnsupportedClassVersionError ... (Unsupported major.minor version 49.0)\n" +
                "         See http://jabref.sf.net/faq.php for more information.\n";

            System.out.println(javaVersionWarning);
        }

        String javaVendor = System.getProperty("java.vendor", null);
        if ((!javaVendor.contains("Sun Microsystems")) && (!javaVendor.contains("Oracle"))) {
            System.out.println("\n" + 
                    "WARNING: You are not running a Java version from Oracle (or Sun Microsystems).\n" +
                    "         Your java vendor is: " + javaVendor + "\n" +
                    "         If JabRef crashes please consider switching to an Oracle Java Runtime.\n" +
                    "         See http://jabref.sf.net/faq.php for more information.\n");
        }

        try {
            // We need to load this class dynamically, or otherwise the Java 
            // runtime would crash while loading JabRefMain itself.
            Method method = Class.forName("net.sf.jabref.JabRef").getMethod(
                "main", args.getClass());
            method.invoke(null, new Object[] { args });

        } catch (InvocationTargetException e) {
            
            String errorMessage = 
                "\nERROR while starting or running JabRef:\n\n" + 
                exceptionToString(e.getCause()) + "\n" + 
                "Please first check if this problem and a solution is already known. Find our...\n" +
                "  * ...FAQ at http://jabref.sf.net/faq.php and our...\n" +
                "  * ...user mailing-list at http://sf.net/mailarchive/forum.php?forum_name=jabref-users\n\n" + 
                "If you do not find a solution there, please let us know about the problem by writing a bug report.\n" +
                "You can find our bug tracker at http://sourceforge.net/p/jabref/bugs/\n\n" +
                "  * If the bug has already been reported there, please add your comments to the existing bug.\n" +
                "  * If the bug has not been reported yet, then we need the complete error message given above\n" +
                "    and a description of what you did before the error occured.\n\n" +
                "We also need the following information (you can copy and paste all this):\n" +
                "  * Java Version: " + javaVersion + "\n" + 
                "  * Java Vendor: " + javaVendor + "\n" + 
                "  * Operating System: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")\n" +
                "  * Hardware Architecture: " + System.getProperty("os.arch") + "\n\n" +
        		"We are sorry for the trouble and thanks for reporting problems with JabRef!\n";
            
            System.out.println(errorMessage);
            
            JEditorPane pane = new JEditorPane("text/html", 
                "<html>The following error occurred while running JabRef:<p><font color=\"red\">" +
                exceptionToString(e.getCause()).replaceAll("\\n", "<br>") + 
                "</font></p>" + 
                "<p>Please first check if this problem and a solution is already known. Find our...</p>" +
                "<ul><li>...FAQ at <b>http://jabref.sf.net/faq.php</b> and our..." +
                "<li>...user mailing-list at <b>http://sf.net/mailarchive/forum.php?forum_name=jabref-users</b></ul>" + 
                "If you do not find a solution there, please let us know about the problem by writing a bug report.<br>" +
                "You can find our bug tracker at <a href=\"http://sourceforge.net/p/jabref/bugs/\"><b>http://sourceforge.net/p/jabref/bugs/</b></a>.<br>"  +
                "<ul><li>If the bug has already been reported there, please add your comments to the existing bug.<br>" +
                "<li>If the bug has not been reported yet, then we need the complete error message given above<br>" +
                "and a description of what you did before the error occured.</ul>" +
                "We also need the following information (you can copy and paste all this):</p>" +
                "<ul><li>Java Version: " + javaVersion +
                "<li>Java Vendor: " + javaVendor +  
                "<li>Operating System: " + System.getProperty("os.name") + " (" + System.getProperty("os.version") + ")" +
                "<li>Hardware Architecture: " + System.getProperty("os.arch") + "</ul>" +
                "We are sorry for the trouble and thanks for reporting problems with JabRef!</html>");
            pane.setEditable(false);
            pane.setOpaque(false);
            pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            
            Component componentToDisplay;
            if (pane.getPreferredSize().getHeight() > 700){
                JScrollPane sPane = new JScrollPane(pane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                sPane.setBorder(BorderFactory.createEmptyBorder());
                sPane.setPreferredSize(new Dimension((int)pane.getPreferredSize().getWidth() + 30, 700));
                componentToDisplay = sPane;
            } else {
                componentToDisplay = pane;
            }
            
            JOptionPane.showMessageDialog(null, componentToDisplay, "An error occurred while running JabRef", JOptionPane.ERROR_MESSAGE);
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
            
            String errorMessage = 
                exceptionToString(e) + "\n" +
                "This means that your Java version (" + javaVersion + ") is not high enough to run JabRef.\n" +
                		"Please update your Java Runtime Environment to a version 1.6 or higher.\n";
            
            System.out.println(errorMessage);
            
            JEditorPane pane = new JEditorPane("text/html", 
                "<html>You are using Java version " + javaVersion + ", but JabRef needs version 1.6 or higher." +
                "<p>Please update your Java Runtime Environment.</p>" +
                "<p>For more information visit <b>http://jabref.sf.net/faq.php</b>.</p></html>");
            pane.setEditable(false);
            pane.setOpaque(false);
            pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
            
            JOptionPane.showMessageDialog(null, pane, "Insufficient Java Version Installed", JOptionPane.ERROR_MESSAGE);
        }
    }
}
