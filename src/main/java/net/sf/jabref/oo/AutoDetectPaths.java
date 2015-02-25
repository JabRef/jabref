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
package net.sf.jabref.oo;

import net.sf.jabref.Globals;
import net.sf.jabref.AbstractWorker;

import javax.swing.*;
import java.io.File;
import java.io.FileFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Tools for automatically detecting jar and executable paths to OpenOffice.
 */
public class AutoDetectPaths extends AbstractWorker {

    boolean foundPaths = false;
    boolean fileSearchCancelled = false;
    JDialog prog;
    private JDialog parent;

    public AutoDetectPaths(JDialog parent) {
        this.parent = parent;
    }

    public boolean runAutodetection() {
        try {
            if (checkAutoDetectedPaths())
                return true;
            init();
            getWorker().run();
            update();
            return foundPaths;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    public void run() {
        foundPaths = autoDetectPaths();
    }

    public boolean getResult() {
        return foundPaths;
    }

    public boolean cancelled() {
        return fileSearchCancelled;
    }

    public void init() throws Throwable {
        prog = showProgressDialog(parent, Globals.lang("Autodetecting paths..."),
            Globals.lang("Please wait..."), true);
    }

    public void update() {
        prog.dispose();
    }

    public boolean autoDetectPaths() {

        if (Globals.ON_WIN) {
            List<File> progFiles = findProgramFilesDir();
            File sOffice = null;
            if (fileSearchCancelled)
                return false;
            for (File dir : progFiles) {
                sOffice = findFileDir(dir, "soffice.exe");
                if (sOffice != null)
                    break;
            }
            if (sOffice == null) {
                JOptionPane.showMessageDialog(parent, Globals.lang("Unable to autodetect OpenOffice installation. Please choose the installation directory manually."),
                        Globals.lang("Could not find OpenOffice installation"), JOptionPane.INFORMATION_MESSAGE);
                JFileChooser jfc = new JFileChooser(new File("C:\\"));
                jfc.setDialogType(JFileChooser.OPEN_DIALOG);
                jfc.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    public boolean accept(File file) {
                        return file.isDirectory();
                    }

                    public String getDescription() {
                        return Globals.lang("Directories");
                    }
                });
                jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                jfc.showOpenDialog(parent);
                if (jfc.getSelectedFile() != null)
                    sOffice = jfc.getSelectedFile();
            }
            if (sOffice == null)
                return false;

            Globals.prefs.put("ooExecutablePath", new File(sOffice, "soffice.exe").getPath());
            File unoil = findFileDir(sOffice.getParentFile(), "unoil.jar");
            if (fileSearchCancelled)
                return false;
            File jurt = findFileDir(sOffice.getParentFile(), "jurt.jar");
            if (fileSearchCancelled)
                return false;
            if ((unoil != null) && (jurt != null)) {
                Globals.prefs.put("ooUnoilPath", unoil.getPath());
                Globals.prefs.put("ooJurtPath", jurt.getPath());
                return true;
            }
            else return false;

        }
        else if (Globals.ON_MAC) {
            File rootDir = new File("/Applications");
            File[] files = rootDir.listFiles();
            for (File file : files) {
                if (file.isDirectory() && file.getName().equals("OpenOffice.org.app")) {
                    rootDir = file;
                    //System.out.println("Setting starting dir to: "+file.getPath());
                    break;
                }
            }
            //System.out.println("Searching for soffice.bin");
            File sOffice = findFileDir(rootDir, "soffice.bin");
            //System.out.println("Found: "+(sOffice != null ? sOffice.getPath() : "-"));
            if (fileSearchCancelled)
                return false;
            if (sOffice != null) {
                Globals.prefs.put("ooExecutablePath", new File(sOffice, "soffice.bin").getPath());
                //System.out.println("Searching for unoil.jar");
                File unoil = findFileDir(rootDir, "unoil.jar");
                //System.out.println("Found: "+(unoil != null ? unoil.getPath(): "-"));
                if (fileSearchCancelled)
                    return false;
                //System.out.println("Searching for jurt.jar");
                File jurt = findFileDir(rootDir, "jurt.jar");
                //System.out.println("Found: "+(jurt != null ? jurt.getPath(): "-"));
                if (fileSearchCancelled)
                    return false;
                if ((unoil != null) && (jurt != null)) {
                    Globals.prefs.put("ooUnoilPath", unoil.getPath());
                    Globals.prefs.put("ooJurtPath", jurt.getPath());
                    return true;
                }
                else return false;
            }
            else return false;
        }
        else {
            // Linux:
            String usrRoot = "/usr/lib";
            File inUsr = findFileDir(new File("/usr/lib"), "soffice");
            if (fileSearchCancelled)
                return false;
            if (inUsr == null) {
                inUsr = findFileDir(new File("/usr/lib64"), "soffice");
                if (inUsr != null) usrRoot = "/usr/lib64";
            }

            if (fileSearchCancelled)
                return false;
            File inOpt = findFileDir(new File("/opt"), "soffice");
            if (fileSearchCancelled)
                return false;
            if ((inUsr != null) && (inOpt == null)) {
                return setupPreferencesForOO(usrRoot, inUsr);
            }
            else if ((inOpt != null) && (inUsr == null)) {
                Globals.prefs.put("ooExecutablePath", new File(inOpt, "soffice.bin").getPath());
                File unoil = findFileDir(new File("/opt"), "unoil.jar");
                File jurt = findFileDir(new File("/opt"), "jurt.jar");
                if ((unoil != null) && (jurt != null)) {
                    Globals.prefs.put("ooUnoilPath", unoil.getPath());
                    Globals.prefs.put("ooJurtPath", jurt.getPath());
                    return true;
                }
                else return false;
            }
            else if (inOpt != null) { // Found both
                JRadioButton optRB = new JRadioButton(inOpt.getPath(), true);
                JRadioButton usrRB = new JRadioButton(inUsr.getPath(), false);
                ButtonGroup bg = new ButtonGroup();
                bg.add(optRB);
                bg.add(usrRB);
                DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("left:pref", ""));
                b.append(Globals.lang("Found more than one OpenOffice executable. Please choose which one to connect to:"));
                b.append(optRB);
                b.append(usrRB);
                int answer = JOptionPane.showConfirmDialog(null, b.getPanel(), Globals.lang("Choose OpenOffice executable"),
                        JOptionPane.OK_CANCEL_OPTION);
                if (answer == JOptionPane.CANCEL_OPTION)
                    return false;
                else {
                    if (optRB.isSelected()) {
                        return setupPreferencesForOO("/opt", inOpt);
                    }
                    else {
                        return setupPreferencesForOO(usrRoot, inUsr);
                    }

                }
            }
            else return false;
        }


    }

    private boolean setupPreferencesForOO(String usrRoot, File inUsr) {
        Globals.prefs.put("ooExecutablePath", new File(inUsr, "soffice.bin").getPath());
        File unoil = findFileDir(new File(usrRoot), "unoil.jar");
        if (fileSearchCancelled)
            return false;
        File jurt = findFileDir(new File(usrRoot), "jurt.jar");
        if (fileSearchCancelled)
            return false;
        if ((unoil != null) && (jurt != null)) {
            Globals.prefs.put("ooUnoilPath", unoil.getPath());
            Globals.prefs.put("ooJurtPath", jurt.getPath());
            return true;
        }
        else return false;
    }

    /**
     * Search for Program files directory.
     * @return the File pointing to the Program files directory, or null if not found.
     *   Since we are not including a library for Windows integration, this method can't
     *   find the Program files dir in localized Windows installations.
     */
    private static java.util.List<File> findProgramFilesDir() {
        List<File> dirList = new ArrayList<File>();
        File root = new File("C:\\");
        File[] dirs = root.listFiles(new FileFilter() {
            public boolean accept(File file) {
                return file.isDirectory();
            }
        });
        for (File dir : dirs) {
            if (dir.getName().toLowerCase().equals("program files"))
                dirList.add(dir);
            else if (dir.getName().toLowerCase().equals("program files (x86)"))
                dirList.add(dir);
        }
        return dirList;
    }

    public static boolean checkAutoDetectedPaths() {

        if (Globals.prefs.hasKey("ooUnoilPath") && Globals.prefs.hasKey("ooJurtPath")
                && Globals.prefs.hasKey("ooExecutablePath")) {
            return new File(Globals.prefs.get("ooUnoilPath"), "unoil.jar").exists()
                    && new File(Globals.prefs.get("ooJurtPath"), "jurt.jar").exists()
                    && new File(Globals.prefs.get("ooExecutablePath")).exists();
        }
        else return false;
    }

    /**
     * Search for a file, starting at the given directory.
     * @param startDir The starting point.
     * @param filename The name of the file to search for.
     * @return The directory where the file was first found, or null if not found.
     */
    public File findFileDir(File startDir, String filename) {
        if (fileSearchCancelled)
            return null;
        File[] files = startDir.listFiles();
        if (files == null)
            return null;
        File result = null;
        for (File file : files) {
            if (fileSearchCancelled)
                return null;
            if (file.isDirectory()) {
                result = findFileDir(file, filename);
                if (result != null)
                    break;
            } else if (file.getName().equals(filename)) {
                result = startDir;
                break;
            }
        }
        return result;
    }

    public JDialog showProgressDialog(JDialog parent, String title, String message, boolean includeCancelButton) {
        fileSearchCancelled = false;
        final JDialog prog;
        JProgressBar bar = new JProgressBar(JProgressBar.HORIZONTAL);
        JButton cancel = new JButton(Globals.lang("Cancel"));
        cancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                fileSearchCancelled = true;
                ((JButton)event.getSource()).setEnabled(false);
            }
        });
        prog = new JDialog(parent, title, false);
        bar.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        bar.setIndeterminate(true);
        if (includeCancelButton)
            prog.add(cancel, BorderLayout.SOUTH);
        prog.add(new JLabel(message), BorderLayout.NORTH);
        prog.add(bar, BorderLayout.CENTER);
        prog.pack();
        prog.setLocationRelativeTo(null);//parent);
        //SwingUtilities.invokeLater(new Runnable() {
        //    public void run() {
                prog.setVisible(true);
        //    }
        //});
        return prog;
    }
}
