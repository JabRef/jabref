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
package net.sf.jabref.openoffice;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import net.sf.jabref.*;
import net.sf.jabref.exporter.layout.Layout;
import net.sf.jabref.exporter.layout.LayoutHelper;
import net.sf.jabref.external.push.PushToApplication;
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibtexDatabase;
import net.sf.jabref.model.entry.BibtexEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This test panel can be opened by reflection from JabRef, passing the JabRefFrame as an
 * argument to the start() method. It displays buttons for testing interaction functions
 * between JabRef and OpenOffice.
 */
public class OpenOfficePanel extends AbstractWorker implements PushToApplication {

    public static final String defaultAuthorYearStylePath = "/resource/openoffice/default_authoryear.jstyle";
    public static final String defaultNumericalStylePath = "/resource/openoffice/default_numerical.jstyle";

    // This field indicates whether the running JabRef supports post formatters in Layout:
    public static boolean postLayoutSupported;

    static {
        OpenOfficePanel.postLayoutSupported = true;
        try {
            Layout l = new LayoutHelper(new StringReader("")).
                    getLayoutFromText(Globals.FORMATTER_PACKAGE);
            l.setPostFormatter(null);
        } catch (NoSuchMethodError ex) {
            OpenOfficePanel.postLayoutSupported = false;
        } catch (Exception ignore) {

        }

    }

    private OOPanel comp;
    private JDialog diag;
    private static JButton
            connect;
    private static JButton manualConnect;
    private static JButton selectDocument;
    private static final JButton setStyleFile = new JButton(Localization.lang("Select style"));
    private static final JButton pushEntries = new JButton(Localization.lang("Cite"));
    private static final JButton pushEntriesInt = new JButton(Localization.lang("Cite in-text"));
    private static final JButton pushEntriesEmpty = new JButton(Localization.lang("Insert empty citation"));
    private static final JButton pushEntriesAdvanced = new JButton(Localization.lang("Cite special"));
    private static final JButton focus = new JButton("Focus OO document");
    private static JButton update;
    private static final JButton insertFullRef = new JButton("Insert reference text");
    private static final JButton merge = new JButton(Localization.lang("Merge citations"));
    private static final JButton manageCitations = new JButton(Localization.lang("Manage citations"));
    private static final JButton settingsB = new JButton(Localization.lang("Settings"));
    private static final JButton help = new JButton(IconTheme.JabRefIcon.HELP.getSmallIcon());
    private static final JButton test = new JButton("Test");
    private JRadioButton inPar;
    private JRadioButton inText;
    private JPanel settings;
    private static String styleFile;
    private static OOBibBase ooBase;
    private static JabRefFrame frame;
    private SidePaneManager manager;
    private static OOBibStyle style;
    private static boolean useDefaultAuthoryearStyle;
    private static boolean useDefaultNumericalStyle;
    private StyleSelectDialog styleDialog;
    private boolean dialogOkPressed;
    private boolean autoDetected;
    private String sOffice;
    private Throwable connectException;

    private static OpenOfficePanel instance;


    public static OpenOfficePanel getInstance() {
        if (OpenOfficePanel.instance == null) {
            OpenOfficePanel.instance = new OpenOfficePanel();
        }
        return OpenOfficePanel.instance;
    }

    private OpenOfficePanel() {
        Icon connectImage = IconTheme.JabRefIcon.CONNECT_OPEN_OFFICE.getSmallIcon();

        OpenOfficePanel.connect = new JButton(connectImage);
        OpenOfficePanel.manualConnect = new JButton(connectImage);
        OpenOfficePanel.connect.setToolTipText(Localization.lang("Connect"));
        OpenOfficePanel.manualConnect.setToolTipText(Localization.lang("Manual connect"));
        OpenOfficePanel.selectDocument = new JButton(IconTheme.JabRefIcon.OPEN.getSmallIcon());
        OpenOfficePanel.selectDocument.setToolTipText(Localization.lang("Select Writer document"));
        OpenOfficePanel.update = new JButton(IconTheme.JabRefIcon.REFRESH.getSmallIcon());
        OpenOfficePanel.update.setToolTipText(Localization.lang("Sync OO bibliography"));
        String defExecutable;
        String defJarsDir;
        if (OS.WINDOWS) {
            Globals.prefs.putDefaultValue("ooPath", "C:\\Program Files\\OpenOffice.org 3");
            Globals.prefs.putDefaultValue("ooExecutablePath", "C:\\Program Files\\OpenOffice.org 2.3\\program\\soffice.exe");
            Globals.prefs.putDefaultValue("ooJarsPath", "C:\\Program Files\\OpenOffice.org 2.3\\program\\classes");
        } else if (OS.OS_X) {
            Globals.prefs.putDefaultValue("ooExecutablePath", "/Applications/OpenOffice.org.app/Contents/MacOS/soffice.bin");
            Globals.prefs.putDefaultValue("ooPath", "/Applications/OpenOffice.org.app");
            Globals.prefs.putDefaultValue("ooJarsPath", "/Applications/OpenOffice.org.app/Contents/basis-link");
        } else { // Linux
            //Globals.prefs.putDefaultValue("ooPath", "/usr/lib/openoffice");
            Globals.prefs.putDefaultValue("ooPath", "/opt/openoffice.org3");
            Globals.prefs.putDefaultValue("ooExecutablePath", "/usr/lib/openoffice/program/soffice");
            //Globals.prefs.putDefaultValue("ooJarsPath", "/usr/share/java/openoffice");
            Globals.prefs.putDefaultValue("ooJarsPath", "/opt/openoffice.org/basis3.0");
        }
        Globals.prefs.putDefaultValue("connectToOO3", Boolean.TRUE);

        //Globals.prefs.putDefaultValue("ooStyleFileDirectories", System.getProperty("user.home")+";false");
        Globals.prefs.putDefaultValue("ooStyleFileLastDir", System.getProperty("user.home"));
        Globals.prefs.putDefaultValue("ooInParCitation", true);
        Globals.prefs.putDefaultValue("syncOOWhenCiting", false);
        Globals.prefs.putDefaultValue("showOOPanel", false);
        Globals.prefs.putDefaultValue("useAllOpenBases", true);
        Globals.prefs.putDefaultValue("ooUseDefaultAuthoryearStyle", true);
        Globals.prefs.putDefaultValue("ooUseDefaultNumericalStyle", false);
        Globals.prefs.putDefaultValue("ooChooseStyleDirectly", false);
        Globals.prefs.putDefaultValue("ooDirectFile", "");
        Globals.prefs.putDefaultValue("ooStyleDirectory", "");
        OpenOfficePanel.styleFile = Globals.prefs.get("ooBibliographyStyleFile");

    }

    public SidePaneComponent getSidePaneComponent() {
        return comp;
    }

    public void init(JabRefFrame frame, SidePaneManager manager) {
        OpenOfficePanel.frame = frame;
        this.manager = manager;
        comp = new OOPanel(manager, IconTheme.getImage("openoffice"), Localization.lang("OpenOffice"));
        try {
            initPanel();
            manager.register(getName(), comp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public JMenuItem getMenuItem() {
        if (Globals.prefs.getBoolean("showOOPanel")) {
            manager.show(getName());
        }
        JMenuItem item = new JMenuItem(Localization.lang("OpenOffice/LibreOffice connection"), IconTheme.getImage("openoffice"));
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                manager.show(getName());
            }
        });
        return item;
    }

    public String getShortcutKey() {
        return null;
    }

    private void initPanel() {

        OpenOfficePanel.useDefaultAuthoryearStyle = Globals.prefs.getBoolean("ooUseDefaultAuthoryearStyle");
        OpenOfficePanel.useDefaultNumericalStyle = Globals.prefs.getBoolean("ooUseDefaultNumericalStyle");
        Action al = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                connect(true);
            }
        };
        OpenOfficePanel.connect.addActionListener(al);

        OpenOfficePanel.manualConnect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                connect(false);
            }
        });
        OpenOfficePanel.selectDocument.setToolTipText(Localization.lang("Select which open Writer document to work on"));
        OpenOfficePanel.selectDocument.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    OpenOfficePanel.ooBase.selectDocument();
                    OpenOfficePanel.frame.output(Localization.lang("Connected to document") + ": " + OpenOfficePanel.ooBase.getCurrentDocumentTitle());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(OpenOfficePanel.frame, ex.getMessage(), Localization.lang("Error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        OpenOfficePanel.setStyleFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (styleDialog == null) {
                    styleDialog = new StyleSelectDialog(OpenOfficePanel.frame, OpenOfficePanel.styleFile);
                }
                styleDialog.setVisible(true);
                if (styleDialog.isOkPressed()) {
                    OpenOfficePanel.useDefaultAuthoryearStyle = Globals.prefs.getBoolean("ooUseDefaultAuthoryearStyle");
                    OpenOfficePanel.useDefaultNumericalStyle = Globals.prefs.getBoolean("ooUseDefaultNumericalStyle");
                    OpenOfficePanel.styleFile = Globals.prefs.get("ooBibliographyStyleFile");
                    try {
                        readStyleFile();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        OpenOfficePanel.pushEntries.setToolTipText(Localization.lang("Cite selected entries"));
        OpenOfficePanel.pushEntries.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pushEntries(true, true, false);
            }
        });
        OpenOfficePanel.pushEntriesInt.setToolTipText(Localization.lang("Cite selected entries with in-text citation"));
        OpenOfficePanel.pushEntriesInt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pushEntries(false, true, false);
            }
        });
        OpenOfficePanel.pushEntriesEmpty.setToolTipText(Localization.lang("Insert a citation without text (the entry will appear in the reference list)"));
        OpenOfficePanel.pushEntriesEmpty.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                pushEntries(false, false, false);
            }
        });
        OpenOfficePanel.pushEntriesAdvanced.setToolTipText(Localization.lang("Cite selected entries with extra information"));
        OpenOfficePanel.pushEntriesAdvanced.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                pushEntries(false, true, true);
            }
        });

        OpenOfficePanel.focus.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                OpenOfficePanel.ooBase.setFocus();
            }
        });
        OpenOfficePanel.update.setToolTipText(Localization.lang("Ensure that the bibliography is up-to-date"));
        Action updateAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    try {
                        if (OpenOfficePanel.style == null) {
                            readStyleFile();
                        } else {
                            OpenOfficePanel.style.ensureUpToDate();
                        }
                    } catch (Throwable ex) {
                        JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("You must select either a valid style file, or use one of the default styles."),
                                Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    OpenOfficePanel.ooBase.updateSortedReferenceMarks();

                    java.util.List<BibtexDatabase> databases = getBaseList();
                    java.util.List<String> unresolvedKeys = OpenOfficePanel.ooBase.refreshCiteMarkers
                            (databases, OpenOfficePanel.style);
                    OpenOfficePanel.ooBase.rebuildBibTextSection(databases, OpenOfficePanel.style);
                    //ooBase.sync(frame.basePanel().database(), style);
                    if (!unresolvedKeys.isEmpty()) {
                        JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("Your OpenOffice document references the BibTeX key '%0', which could not be found in your current database.",
                                unresolvedKeys.get(0)), Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (UndefinedCharacterFormatException ex) {
                    reportUndefinedCharacterFormat(ex);
                } catch (UndefinedParagraphFormatException ex) {
                    reportUndefinedParagraphFormat(ex);
                } catch (ConnectionLostException ex) {
                    showConnectionLostErrorMessage();
                } catch (BibtexEntryNotFoundException ex) {
                    JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("Your OpenOffice document references the BibTeX key '%0', which could not be found in your current database.",
                            ex.getBibtexKey()), Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
        OpenOfficePanel.update.addActionListener(updateAction);

        OpenOfficePanel.insertFullRef.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    insertFullRefs();
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        OpenOfficePanel.merge.setToolTipText(Localization.lang("Combine pairs of citations that are separated by spaces only"));
        OpenOfficePanel.merge.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    OpenOfficePanel.ooBase.combineCiteMarkers(getBaseList(), OpenOfficePanel.style);
                } catch (UndefinedCharacterFormatException e) {
                    reportUndefinedCharacterFormat(e);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        OpenOfficePanel.settingsB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showSettingsPopup();
            }
        });

        OpenOfficePanel.help.addActionListener(new HelpAction(GUIGlobals.helpDiag, "OpenOfficeIntegration.html"));

        OpenOfficePanel.manageCitations.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    CitationManager cm = new CitationManager(OpenOfficePanel.frame, OpenOfficePanel.ooBase);
                    cm.showDialog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        OpenOfficePanel.test.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    //pushEntries(false, true, true);

                    //ooBase.testFrameHandling();

                    //ooBase.combineCiteMarkers(frame.basePanel().database(), style);
                    //insertUsingBST();
                    //ooBase.testFootnote();
                    //ooBase.refreshCiteMarkers(frame.basePanel().database(), style);
                    //ooBase.createBibTextSection(true);
                    //ooBase.clearBibTextSectionContent();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });

        OpenOfficePanel.selectDocument.setEnabled(false);
        OpenOfficePanel.pushEntries.setEnabled(false);
        OpenOfficePanel.pushEntriesInt.setEnabled(false);
        OpenOfficePanel.pushEntriesEmpty.setEnabled(false);
        OpenOfficePanel.pushEntriesAdvanced.setEnabled(false);
        OpenOfficePanel.focus.setEnabled(false);
        OpenOfficePanel.update.setEnabled(false);
        OpenOfficePanel.insertFullRef.setEnabled(false);
        OpenOfficePanel.merge.setEnabled(false);
        OpenOfficePanel.manageCitations.setEnabled(false);
        OpenOfficePanel.test.setEnabled(false);
        diag = new JDialog((JFrame) null, "OpenOffice panel", false);

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("fill:pref:grow",
                //"p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu"));
                "p,p,p,p,p,p,p,p,p,p"));

        //ButtonBarBuilder bb = new ButtonBarBuilder();
        DefaultFormBuilder bb = new DefaultFormBuilder(new FormLayout
                ("fill:pref:grow, 1dlu, fill:pref:grow, 1dlu, fill:pref:grow, "
                        + "1dlu, fill:pref:grow, 1dlu, fill:pref:grow", ""));
        bb.append(OpenOfficePanel.connect);
        bb.append(OpenOfficePanel.manualConnect);
        bb.append(OpenOfficePanel.selectDocument);
        bb.append(OpenOfficePanel.update);
        bb.append(OpenOfficePanel.help);

        //b.append(connect);
        //b.append(manualConnect);
        //b.append(selectDocument);
        b.append(bb.getPanel());
        b.append(OpenOfficePanel.setStyleFile);
        b.append(OpenOfficePanel.pushEntries);
        b.append(OpenOfficePanel.pushEntriesInt);
        b.append(OpenOfficePanel.pushEntriesAdvanced);
        b.append(OpenOfficePanel.pushEntriesEmpty);
        b.append(OpenOfficePanel.merge);
        b.append(OpenOfficePanel.manageCitations);
        b.append(OpenOfficePanel.settingsB);
        //b.append(focus);
        //b.append(update);

        //b.append(insertFullRef);
        //b.append(test);
        //diag.getContentPane().add(b.getPanel(), BorderLayout.CENTER);

        JPanel content = new JPanel();
        comp.setContentContainer(content);
        content.setLayout(new BorderLayout());
        content.add(b.getPanel(), BorderLayout.CENTER);

        OpenOfficePanel.frame.getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.prefs.getKey("Refresh OO"), "Refresh OO");
        OpenOfficePanel.frame.getTabbedPane().getActionMap().put("Refresh OO", updateAction);

        //diag.pack();
        //diag.setVisible(true);
    }

    private java.util.List<BibtexDatabase> getBaseList() {
        java.util.List<BibtexDatabase> databases = new ArrayList<BibtexDatabase>();
        if (Globals.prefs.getBoolean("useAllOpenBases")) {
            for (int i = 0; i < OpenOfficePanel.frame.baseCount(); i++) {
                databases.add(OpenOfficePanel.frame.baseAt(i).database());
            }
        } else {
            databases.add(OpenOfficePanel.frame.basePanel().database());
        }

        return databases;
    }

    private void connect(boolean auto) {
        /*if (ooBase != null) {
            try {
                java.util.List<XTextDocument> list = ooBase.getTextDocuments();
                // TODO: how to find the title of the documents?
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return;
        }*/

        String unoilDir;
        String ooBaseDirectory;
        if (auto) {
            AutoDetectPaths adp = new AutoDetectPaths(diag);

            if (adp.runAutodetection()) {
                autoDetected = true;
                dialogOkPressed = true;
                diag.dispose();
            } else if (!adp.cancelled()) {
                JOptionPane.showMessageDialog(diag,
                        Localization.lang("Autodetection failed"),
                        Localization.lang("Autodetection failed"),
                        JOptionPane.ERROR_MESSAGE);
            }
            if (!autoDetected) {
                return;
            }

            // User clicked Auto, and the system successfully detected paths:
            unoilDir = Globals.prefs.get("ooUnoilPath");
            ooBaseDirectory = Globals.prefs.get("ooJurtPath");
            sOffice = Globals.prefs.get("ooExecutablePath");

            //System.out.println("unoilDir: "+unoilDir);
            //System.out.println("ooBaseDir: "+ooBaseDirectory);
            //System.out.println("soffice: "+sOffice);

        }
        else { // Manual connect

            showConnectDialog();
            if (!dialogOkPressed) {
                return;
            }

            String ooPath = Globals.prefs.get("ooPath");
            String ooJars = Globals.prefs.get("ooJarsPath");
            sOffice = Globals.prefs.get("ooExecutablePath");

            boolean openOffice3 = true;//Globals.prefs.getBoolean("connectToOO3");
            if (OS.WINDOWS) {
                //if (openOffice3) {
                unoilDir = ooPath + "\\Basis\\program\\classes";
                ooBaseDirectory = ooPath + "\\URE\\java";
                sOffice = ooPath + "\\program\\soffice.exe";
                //}

            }
            else if (OS.OS_X) {
                //if (openOffice3) {
                sOffice = ooPath + "/Contents/MacOS/soffice.bin";
                ooBaseDirectory = ooPath + "/Contents/basis-link/ure-link/share/java";
                unoilDir = ooPath + "/Contents/basis-link/program/classes";
                //}

            }
            else {
                // Linux:
                //if (openOffice3) {
                unoilDir = ooJars + "/program/classes";
                ooBaseDirectory = ooJars + "/ure-link/share/java";
                //sOffice = ooPath+"/program/soffice";
                //}

            }
        }

        // Add OO jars to the classpath:
        try {
            File[] jarFiles = new File[] {
                    new File(unoilDir, "unoil.jar"),
                    new File(ooBaseDirectory, "jurt.jar"),
                    new File(ooBaseDirectory, "juh.jar"),
                    new File(ooBaseDirectory, "ridl.jar")};
            URL[] jarList = new URL[jarFiles.length];
            for (int i = 0; i < jarList.length; i++) {
                if (!jarFiles[i].exists()) {
                    throw new Exception(Localization.lang("File not found") + ": " + jarFiles[i].getPath());
                }
                jarList[i] = jarFiles[i].toURI().toURL();
            }
            OpenOfficePanel.addURL(jarList);

            // Show progress dialog:
            final JDialog progDiag = new AutoDetectPaths(diag).showProgressDialog(diag, Localization.lang("Connecting"),
                    Localization.lang("Please wait..."), false);
            getWorker().run(); // Do the actual connection, using Spin to get off the EDT.
            progDiag.dispose();
            diag.dispose();
            if (OpenOfficePanel.ooBase == null) {
                throw connectException;
            }

            if (OpenOfficePanel.ooBase.isConnectedToDocument()) {
                OpenOfficePanel.frame.output(Localization.lang("Connected to document") + ": " + OpenOfficePanel.ooBase.getCurrentDocumentTitle());
            }

            // Enable actions that depend on Connect:
            OpenOfficePanel.selectDocument.setEnabled(true);
            OpenOfficePanel.pushEntries.setEnabled(true);
            OpenOfficePanel.pushEntriesInt.setEnabled(true);
            OpenOfficePanel.pushEntriesEmpty.setEnabled(true);
            OpenOfficePanel.pushEntriesAdvanced.setEnabled(true);
            OpenOfficePanel.focus.setEnabled(true);
            OpenOfficePanel.update.setEnabled(true);
            OpenOfficePanel.insertFullRef.setEnabled(true);
            OpenOfficePanel.merge.setEnabled(true);
            OpenOfficePanel.manageCitations.setEnabled(true);
            OpenOfficePanel.test.setEnabled(true);

        } catch (Throwable e) {
            e.printStackTrace();
            if (e instanceof UnsatisfiedLinkError) {
                JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("Unable to connect. One possible reason is that JabRef "
                        + "and OpenOffice/LibreOffice are not both running in either 32 bit mode or 64 bit mode."));

            }
            else {
                JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("Could not connect to running OpenOffice.\n"
                        + "Make sure you have installed OpenOffice with Java support.\nIf connecting manually, please verify program and library paths.\n"
                        + "\nError message: " + e.getMessage()));
            }
        }
    }

    @Override
    public void run() {
        try {
            // Connect:
            OpenOfficePanel.ooBase = new OOBibBase(sOffice, true);
        } catch (Throwable e) {
            OpenOfficePanel.ooBase = null;
            connectException = e;
            //JOptionPane.showMessageDialog(frame, Globals.lang("Unable to connect"));
        }
    }

    /**
     * Read the style file. Record the last modified time of the file.
     * @throws Exception
     */
    private void readStyleFile() throws Exception {
        if (OpenOfficePanel.useDefaultAuthoryearStyle) {
            URL defPath = JabRef.class.getResource(OpenOfficePanel.defaultAuthorYearStylePath);
            Reader r = new InputStreamReader(defPath.openStream());
            OpenOfficePanel.style = new OOBibStyle(r);
        }
        else if (OpenOfficePanel.useDefaultNumericalStyle) {
            URL defPath = JabRef.class.getResource(OpenOfficePanel.defaultNumericalStylePath);
            Reader r = new InputStreamReader(defPath.openStream());
            OpenOfficePanel.style = new OOBibStyle(r);
        }
        else {
            OpenOfficePanel.style = new OOBibStyle(new File(OpenOfficePanel.styleFile));
        }
    }


    // The methods addFile and associated final Class[] parameters were gratefully copied from
    // anthony_miguel @ http://forum.java.sun.com/thread.jsp?forum=32&thread=300557&tstart=0&trange=15
    private static final Class[] parameters = new Class[] {URL.class};


    private static void addURL(URL[] u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", OpenOfficePanel.parameters);
            method.setAccessible(true);
            for (URL anU : u) {
                method.invoke(sysloader, anU);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    private void updateConnectionParams(String ooPath, String ooExec, String ooJars, boolean oo3) {
        Globals.prefs.put("ooPath", ooPath);
        Globals.prefs.put("ooExecutablePath", ooExec);
        Globals.prefs.put("ooJarsPath", ooJars);
        Globals.prefs.putBoolean("connectToOO3", oo3);
    }

    private void showConnectDialog() {
        dialogOkPressed = false;
        final JDialog diag = new JDialog(OpenOfficePanel.frame, Localization.lang("Set connection parameters"), true);
        final JTextField ooPath = new JTextField(30);
        JButton browseOOPath = new JButton(Localization.lang("Browse"));
        ooPath.setText(Globals.prefs.get("ooPath"));
        final JTextField ooExec = new JTextField(30);
        JButton browseOOExec = new JButton(Localization.lang("Browse"));
        browseOOExec.addActionListener(BrowseAction.buildForFile(ooExec));
        final JTextField ooJars = new JTextField(30);
        JButton browseOOJars = new JButton(Localization.lang("Browse"));
        browseOOJars.addActionListener(BrowseAction.buildForDir(ooJars));
        ooExec.setText(Globals.prefs.get("ooExecutablePath"));
        ooJars.setText(Globals.prefs.get("ooJarsPath"));
        DefaultFormBuilder builder = new DefaultFormBuilder(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", ""));
        if (OS.WINDOWS || OS.OS_X) {
            builder.append(Localization.lang("Path to OpenOffice directory"));
            builder.append(ooPath);
            builder.append(browseOOPath);
            builder.nextLine();
        }
        else {
            builder.append(Localization.lang("Path to OpenOffice executable"));
            builder.append(ooExec);
            builder.append(browseOOExec);
            builder.nextLine();

            builder.append(Localization.lang("Path to OpenOffice library dir"));
            builder.append(ooJars);
            builder.append(browseOOJars);
            builder.nextLine();
        }

        ButtonBarBuilder bb = new ButtonBarBuilder();
        JButton ok = new JButton(Localization.lang("Ok"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        //JButton auto = new JButton(Globals.lang("Autodetect"));
        ActionListener tfListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText(),
                        true);
                diag.dispose();
            }
        };

        ooPath.addActionListener(tfListener);
        ooExec.addActionListener(tfListener);
        ooJars.addActionListener(tfListener);
        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText(),
                        true);
                dialogOkPressed = true;
                diag.dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                diag.dispose();
            }
        });
        bb.addGlue();
        bb.addRelatedGap();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(OpenOfficePanel.frame);
        diag.setVisible(true);

    }

    private void pushEntries(boolean inParenthesis, boolean withText, boolean addPageInfo) {
        if (!OpenOfficePanel.ooBase.isConnectedToDocument()) {
            JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("Not connected to any Writer document. Please"
                            + " make sure a document is open, and use the 'Select Writer document' button to connect to it."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        String pageInfo = null;
        if (addPageInfo) {
            AdvancedCiteDialog acd = new AdvancedCiteDialog(OpenOfficePanel.frame);
            acd.showDialog();
            if (acd.cancelled()) {
                return;
            }
            if (!acd.getPageInfo().isEmpty()) {
                pageInfo = acd.getPageInfo();
            }
            inParenthesis = acd.isInParenthesisCite();

        }

        BasePanel panel = OpenOfficePanel.frame.basePanel();
        final BibtexDatabase database = panel.database();
        if (panel != null) {
            BibtexEntry[] entries = panel.getSelectedEntries();
            if (entries.length > 0) {
                try {
                    if (OpenOfficePanel.style == null) {
                        readStyleFile();
                    }
                    OpenOfficePanel.ooBase.insertEntry(entries, database, getBaseList(), OpenOfficePanel.style, inParenthesis, withText,
                            pageInfo, Globals.prefs.getBoolean("syncOOWhenCiting"));
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("You must select either a valid style file, or use one of the default styles."),
                            Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                } catch (ConnectionLostException ex) {
                    showConnectionLostErrorMessage();
                } catch (UndefinedCharacterFormatException ex) {
                    reportUndefinedCharacterFormat(ex);
                } catch (UndefinedParagraphFormatException ex) {
                    reportUndefinedParagraphFormat(ex);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }

    }

    private void showConnectionLostErrorMessage() {
        JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("Connection to OpenOffice has been lost. "
                        + "Please make sure OpenOffice is running, and try to reconnect."),
                Localization.lang("Connection lost"), JOptionPane.ERROR_MESSAGE);
    }

    private void insertFullRefs() {
        try {
            // Create or clear bibliography:
            /*boolean hadBib = ooBase.createBibTextSection(true);
            if (hadBib)
                ooBase.clearBibTextSectionContent();
              */
            BasePanel panel = OpenOfficePanel.frame.basePanel();
            final BibtexDatabase database = panel.database();
            Map<BibtexEntry, BibtexDatabase> entries = new LinkedHashMap<BibtexEntry, BibtexDatabase>();
            if (panel != null) {
                BibtexEntry[] e = panel.getSelectedEntries();
                ArrayList<BibtexEntry> el = new ArrayList<BibtexEntry>();
                for (BibtexEntry anE : e) {
                    entries.put(anE, database);
                }

                OpenOfficePanel.ooBase.insertFullReferenceAtViewCursor(entries, OpenOfficePanel.style, "Default");
            }
        } catch (UndefinedParagraphFormatException ex) {
            reportUndefinedParagraphFormat(ex);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    private void reportUndefinedParagraphFormat(UndefinedParagraphFormatException ex) {
        JOptionPane.showMessageDialog(OpenOfficePanel.frame, "<html>" + Localization.lang("Your style file specifies the paragraph format '%0', "
                        + "which is undefined in your current OpenOffice document.", ex.getFormatName()) + "<br>"
                + Localization.lang("The paragraph format is controlled by the property 'ReferenceParagraphFormat' or 'ReferenceHeaderParagraphFormat' in the style file.")
                + "</html>",
                Localization.lang(""), JOptionPane.ERROR_MESSAGE);
    }

    private void reportUndefinedCharacterFormat(UndefinedCharacterFormatException ex) {
        JOptionPane.showMessageDialog(OpenOfficePanel.frame, "<html>" + Localization.lang("Your style file specifies the character format '%0', "
                        + "which is undefined in your current OpenOffice document.", ex.getFormatName()) + "<br>"
                + Localization.lang("The character format is controlled by the citation property 'CitationCharacterFormat' in the style file.")
                + "</html>",
                Localization.lang(""), JOptionPane.ERROR_MESSAGE);
    }

    public void insertUsingBST() {
        try {
            BasePanel panel = OpenOfficePanel.frame.basePanel();
            final BibtexDatabase database = panel.database();
            if (panel != null) {
                BibtexEntry[] entries = panel.getSelectedEntries();
                ArrayList<BibtexEntry> el = new ArrayList<BibtexEntry>();
                Collections.addAll(el, entries);

                BstWrapper wrapper = new BstWrapper();
                //wrapper.loadBstFile(new File("/home/usr/share/texmf-tetex/bibtex/bst/base/plain.bst"));
                wrapper.loadBstFile(new File("/home/usr/share/texmf-tetex/bibtex/bst/ams/amsalpha.bst"));
                Map<String, String> result = wrapper.processEntries(el, database);
                for (String key : result.keySet()) {
                    System.out.println("Key: " + key);
                    System.out.println("Entry: " + result.get(key));
                    OpenOfficePanel.ooBase.insertMarkedUpTextAtViewCursor(result.get(key), "Default");
                }
                //System.out.println(result);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void showSettingsPopup() {
        JPopupMenu menu = new JPopupMenu();
        final JCheckBoxMenuItem autoSync = new JCheckBoxMenuItem(
                Localization.lang("Automatically sync bibliography when inserting citations"),
                Globals.prefs.getBoolean("syncOOWhenCiting"));
        final JRadioButtonMenuItem useActiveBase = new JRadioButtonMenuItem
                (Localization.lang("Look up BibTeX entries in the active tab only"));
        final JRadioButtonMenuItem useAllBases = new JRadioButtonMenuItem
                (Localization.lang("Look up BibTeX entries in all open databases"));
        final JMenuItem clearConnectionSettings = new JMenuItem
                (Localization.lang("Clear connection settings"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(useActiveBase);
        bg.add(useAllBases);
        if (Globals.prefs.getBoolean("useAllOpenBases")) {
            useAllBases.setSelected(true);
        } else {
            useActiveBase.setSelected(true);
        }

        autoSync.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean("syncOOWhenCiting", autoSync.isSelected());
            }
        });
        useAllBases.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean("useAllOpenBases", useAllBases.isSelected());
            }
        });
        useActiveBase.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean("useAllOpenBases", !useActiveBase.isSelected());
            }
        });
        clearConnectionSettings.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.clear("ooPAth");
                Globals.prefs.clear("ooExecutablePath");
                Globals.prefs.clear("ooJarsPath");
                Globals.prefs.clear("connectToOO3");
                Globals.prefs.clear("ooUnoilPath");
                Globals.prefs.clear("ooJurtPath");
                OpenOfficePanel.frame.output(Localization.lang("Cleared connection settings."));
            }
        });

        menu.add(autoSync);
        menu.addSeparator();
        menu.add(useActiveBase);
        menu.add(useAllBases);
        menu.addSeparator();
        menu.add(clearConnectionSettings);
        menu.show(OpenOfficePanel.settingsB, 0, OpenOfficePanel.settingsB.getHeight());
    }

    private void pushEntries(boolean inParenthesis, BibtexEntry[] entries) {

        final BibtexDatabase database = OpenOfficePanel.frame.basePanel().database();
        if (entries.length > 0) {

            String pageInfo = null;
            //if (addPageInfo) {
            AdvancedCiteDialog acd = new AdvancedCiteDialog(OpenOfficePanel.frame);
            acd.showDialog();
            if (acd.cancelled()) {
                return;
            }
            if (!acd.getPageInfo().isEmpty()) {
                pageInfo = acd.getPageInfo();
            }
            inParenthesis = acd.isInParenthesisCite();

            //}

            try {
                OpenOfficePanel.ooBase.insertEntry(entries, database, getBaseList(), OpenOfficePanel.style, inParenthesis, true,
                        pageInfo, Globals.prefs.getBoolean("syncOOWhenCiting"));
            } catch (ConnectionLostException ex) {
                showConnectionLostErrorMessage();
            } catch (UndefinedCharacterFormatException ex) {
                reportUndefinedCharacterFormat(ex);
            } catch (UndefinedParagraphFormatException ex) {
                reportUndefinedParagraphFormat(ex);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public String getName() {
        return "Push to OpenOffice";
    }

    @Override
    public String getApplicationName() {
        return "OpenOffice";
    }

    @Override
    public String getTooltip() {
        return "Push selection to OpenOffice";
    }

    @Override
    public Icon getIcon() {
        return IconTheme.getImage("openoffice");
    }

    @Override
    public JPanel getSettingsPanel() {
        return null;
        /*if (settings == null)
            initSettingsPanel();
        return settings;*/
    }

    private void initSettingsPanel() {
        boolean inParen = Globals.prefs.getBoolean("ooInParCitation");
        inPar = new JRadioButton(Localization.lang("Use in-parenthesis citation"), inParen);
        inText = new JRadioButton(Localization.lang("Use in-text citation"), !inParen);
        ButtonGroup bg = new ButtonGroup();
        bg.add(inPar);
        bg.add(inText);
        settings = new JPanel();
        settings.setLayout(new BorderLayout());
        settings.add(inPar, BorderLayout.NORTH);
        settings.add(inText, BorderLayout.SOUTH);
    }

    @Override
    public void storeSettings() {
        Globals.prefs.putBoolean("ooInParCitation", inPar.isSelected());
    }

    @Override
    public void pushEntries(BibtexDatabase bibtexDatabase, BibtexEntry[] entries, String s, MetaData metaData) {
        if (OpenOfficePanel.ooBase == null) {
            connect(true);
        }
        if (OpenOfficePanel.ooBase != null) {
            try {
                if (OpenOfficePanel.style == null) {
                    readStyleFile();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(OpenOfficePanel.frame, Localization.lang("You must select either a valid style file, or use one of the default styles."),
                        Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            pushEntries(Globals.prefs.getBoolean("ooInParCitation"), entries);
        }
    }

    @Override
    public void operationCompleted(BasePanel basePanel) {

    }

    @Override
    public boolean requiresBibtexKeys() {
        return true;
    }


    class OOPanel extends SidePaneComponent {

        public OOPanel(SidePaneManager sidePaneManager, Icon url, String s) {
            super(sidePaneManager, url, s);
        }

        @Override
        public String getName() {
            return OpenOfficePanel.this.getName();
        }

        @Override
        public void componentClosing() {
            Globals.prefs.putBoolean("showOOPanel", false);
        }

        @Override
        public void componentOpening() {
            Globals.prefs.putBoolean("showOOPanel", true);
        }
    }

}
