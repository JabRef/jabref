/*  Copyright (C) 2003-2015 JabRef contributors.
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
import net.sf.jabref.gui.*;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import javax.swing.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;

/**
 * This test panel can be opened by reflection from JabRef, passing the JabRefFrame as an
 * argument to the start() method. It displays buttons for testing interaction functions
 * between JabRef and OpenOffice.
 */
public class OpenOfficePanel extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(OpenOfficePanel.class);

    public static final String DEFAULT_AUTHORYEAR_STYLE_PATH = "/resource/openoffice/default_authoryear.jstyle";
    public static final String DEFAULT_NUMERICAL_STYLE_PATH = "/resource/openoffice/default_numerical.jstyle";

    private OOPanel comp;
    private JDialog diag;
    private final JButton connect;
    private final JButton manualConnect;
    private final JButton selectDocument;
    private final JButton setStyleFile = new JButton(Localization.lang("Select style"));
    private final JButton pushEntries = new JButton(Localization.lang("Cite"));
    private final JButton pushEntriesInt = new JButton(Localization.lang("Cite in-text"));
    private final JButton pushEntriesEmpty = new JButton(Localization.lang("Insert empty citation"));
    private final JButton pushEntriesAdvanced = new JButton(Localization.lang("Cite special"));
    private final JButton update;
    private final JButton merge = new JButton(Localization.lang("Merge citations"));
    private final JButton manageCitations = new JButton(Localization.lang("Manage citations"));
    private final JButton settingsB = new JButton(Localization.lang("Settings"));
    private final JButton help = new HelpAction("OpenOfficeIntegration.html").getHelpButton();
    private String styleFile;
    private OOBibBase ooBase;
    private JabRefFrame frame;
    private SidePaneManager manager;
    private  OOBibStyle style;
    private  boolean useDefaultAuthoryearStyle;
    private  boolean useDefaultNumericalStyle;
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

        connect = new JButton(connectImage);
        manualConnect = new JButton(connectImage);
        connect.setToolTipText(Localization.lang("Connect"));
        manualConnect.setToolTipText(Localization.lang("Manual connect"));
        selectDocument = new JButton(IconTheme.JabRefIcon.OPEN.getSmallIcon());
        selectDocument.setToolTipText(Localization.lang("Select Writer document"));
        update = new JButton(IconTheme.JabRefIcon.REFRESH.getSmallIcon());
        update.setToolTipText(Localization.lang("Sync OO bibliography"));
        if (OS.WINDOWS) {
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_PATH, "C:\\Program Files\\OpenOffice.org 4");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH,
                    "C:\\Program Files\\OpenOffice.org 4\\program\\soffice.exe");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_JARS_PATH, "C:\\Program Files\\OpenOffice.org 4\\program\\classes");
        } else if (OS.OS_X) {
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH, "/Applications/OpenOffice.org.app/Contents/MacOS/soffice.bin");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_PATH, "/Applications/OpenOffice.org.app");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_JARS_PATH,
                    "/Applications/OpenOffice.org.app/Contents/Resources/java");
        } else { // Linux
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_PATH, "/opt/openoffice.org3");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_EXECUTABLE_PATH, "/usr/lib/openoffice/program/soffice");
            Globals.prefs.putDefaultValue(JabRefPreferences.OO_JARS_PATH, "/opt/openoffice.org/basis3.0");
        }

        Globals.prefs.putDefaultValue(JabRefPreferences.SYNC_OO_WHEN_CITING, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.SHOW_OO_PANEL, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.USE_ALL_OPEN_BASES, true);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_USE_DEFAULT_AUTHORYEAR_STYLE, true);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_USE_DEFAULT_NUMERICAL_STYLE, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_CHOOSE_STYLE_DIRECTLY, false);
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_DIRECT_FILE, "");
        Globals.prefs.putDefaultValue(JabRefPreferences.OO_STYLE_DIRECTORY, "");
        styleFile = Globals.prefs.get(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE);

    }

    public SidePaneComponent getSidePaneComponent() {
        return comp;
    }

    public void init(JabRefFrame jrFrame, SidePaneManager spManager) {
        frame = jrFrame;
        this.manager = spManager;
        comp = new OOPanel(spManager, IconTheme.getImage("openoffice"), Localization.lang("OpenOffice"), this);
        initPanel();
        spManager.register(getName(), comp);
    }

    public JMenuItem getMenuItem() {
        if (Globals.prefs.getBoolean(JabRefPreferences.SHOW_OO_PANEL)) {
            manager.show(getName());
        }
        JMenuItem item = new JMenuItem(Localization.lang("OpenOffice/LibreOffice connection"),
                IconTheme.getImage("openoffice"));
        item.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                manager.show(getName());
            }
        });
        return item;
    }

    private void initPanel() {

        useDefaultAuthoryearStyle = Globals.prefs.getBoolean(JabRefPreferences.OO_USE_DEFAULT_AUTHORYEAR_STYLE);
        useDefaultNumericalStyle = Globals.prefs.getBoolean(JabRefPreferences.OO_USE_DEFAULT_NUMERICAL_STYLE);
        Action al = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                connect(true);
            }
        };
        connect.addActionListener(al);

        manualConnect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                connect(false);
            }
        });
        selectDocument.setToolTipText(Localization.lang("Select which open Writer document to work on"));
        selectDocument.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    ooBase.selectDocument();
                    frame.output(Localization.lang("Connected to document") + ": " + ooBase.getCurrentDocumentTitle());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(frame, ex.getMessage(), Localization.lang("Error"),
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        setStyleFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (styleDialog == null) {
                    styleDialog = new StyleSelectDialog(frame, styleFile);
                }
                styleDialog.setVisible(true);
                if (styleDialog.isOkPressed()) {
                    useDefaultAuthoryearStyle = Globals.prefs.getBoolean(JabRefPreferences.OO_USE_DEFAULT_AUTHORYEAR_STYLE);
                    useDefaultNumericalStyle = Globals.prefs.getBoolean(JabRefPreferences.OO_USE_DEFAULT_NUMERICAL_STYLE);
                    styleFile = Globals.prefs.get(JabRefPreferences.OO_BIBLIOGRAPHY_STYLE_FILE);
                    try {
                        readStyleFile();
                    } catch (Exception ex) {
                        LOGGER.warn("Could not read style file", ex);
                    }
                }
            }
        });

        pushEntries.setToolTipText(Localization.lang("Cite selected entries between parenthesis"));
        pushEntries.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pushEntries(true, true, false);
            }
        });
        pushEntriesInt.setToolTipText(Localization.lang("Cite selected entries with in-text citation"));
        pushEntriesInt.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pushEntries(false, true, false);
            }
        });
        pushEntriesEmpty.setToolTipText(Localization.lang("Insert a citation without text (the entry will appear in the reference list)"));
        pushEntriesEmpty.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                pushEntries(false, false, false);
            }
        });
        pushEntriesAdvanced.setToolTipText(Localization.lang("Cite selected entries with extra information"));
        pushEntriesAdvanced.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                pushEntries(false, true, true);
            }
        });

        update.setToolTipText(Localization.lang("Ensure that the bibliography is up-to-date"));
        Action updateAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    try {
                        if (style == null) {
                            readStyleFile();
                        } else {
                            style.ensureUpToDate();
                        }
                    } catch (Throwable ex) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("You must select either a valid style file, or use one of the default styles."),
                                Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    ooBase.updateSortedReferenceMarks();

                    java.util.List<BibDatabase> databases = getBaseList();
                    java.util.List<String> unresolvedKeys = ooBase.refreshCiteMarkers
                            (databases, style);
                    ooBase.rebuildBibTextSection(databases, style);
                    //ooBase.sync(frame.getCurrentBasePanel().database(), style);
                    if (!unresolvedKeys.isEmpty()) {
                        JOptionPane.showMessageDialog(frame, Localization.lang("Your OpenOffice document references the BibTeX key '%0', which could not be found in your current database.",
                                unresolvedKeys.get(0)), Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (UndefinedCharacterFormatException ex) {
                    reportUndefinedCharacterFormat(ex);
                } catch (UndefinedParagraphFormatException ex) {
                    reportUndefinedParagraphFormat(ex);
                } catch (ConnectionLostException ex) {
                    showConnectionLostErrorMessage();
                } catch (BibEntryNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("Your OpenOffice document references the BibTeX key '%0', which could not be found in your current database.",
                            ex.getBibtexKey()), Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                }
                catch (Exception e1) {
                    LOGGER.warn("Could not update bibliography", e1);
                }
            }
        };
        update.addActionListener(updateAction);

        merge.setToolTipText(Localization.lang("Combine pairs of citations that are separated by spaces only"));
        merge.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    ooBase.combineCiteMarkers(getBaseList(), style);
                } catch (UndefinedCharacterFormatException e) {
                    reportUndefinedCharacterFormat(e);
                } catch (Exception e) {
                    LOGGER.warn("Problem combining cite markers", e);
                }

            }
        });

        settingsB.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                showSettingsPopup();
            }
        });

        manageCitations.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                try {
                    CitationManager cm = new CitationManager(frame, ooBase);
                    cm.showDialog();
                } catch (Exception e) {
                    LOGGER.warn("Problem showing citation manager", e);
                }
            }
        });

        selectDocument.setEnabled(false);
        pushEntries.setEnabled(false);
        pushEntriesInt.setEnabled(false);
        pushEntriesEmpty.setEnabled(false);
        pushEntriesAdvanced.setEnabled(false);
        update.setEnabled(false);
        merge.setEnabled(false);
        manageCitations.setEnabled(false);
        diag = new JDialog((JFrame) null, "OpenOffice panel", false);

        DefaultFormBuilder b = new DefaultFormBuilder(new FormLayout("fill:pref:grow",
                //"p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu,p,0dlu"));
                "p,p,p,p,p,p,p,p,p,p"));

        //ButtonBarBuilder bb = new ButtonBarBuilder();
        DefaultFormBuilder bb = new DefaultFormBuilder(new FormLayout
                ("fill:pref:grow, 1dlu, fill:pref:grow, 1dlu, fill:pref:grow, "
                        + "1dlu, fill:pref:grow, 1dlu, fill:pref:grow", ""));
        bb.append(connect);
        bb.append(manualConnect);
        bb.append(selectDocument);
        bb.append(update);
        bb.append(help);
        b.append(bb.getPanel());
        b.append(setStyleFile);
        b.append(pushEntries);
        b.append(pushEntriesInt);
        b.append(pushEntriesAdvanced);
        b.append(pushEntriesEmpty);
        b.append(merge);
        b.append(manageCitations);
        b.append(settingsB);

        JPanel content = new JPanel();
        comp.setContentContainer(content);
        content.setLayout(new BorderLayout());
        content.add(b.getPanel(), BorderLayout.CENTER);

        frame.getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.getKeyPrefs().getKey(KeyBinding.REFRESH_OO), "Refresh OO");
        frame.getTabbedPane().getActionMap().put("Refresh OO", updateAction);

    }

    private java.util.List<BibDatabase> getBaseList() {
        java.util.List<BibDatabase> databases = new ArrayList<>();
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_ALL_OPEN_BASES)) {
            for (BasePanel basePanel : frame.getBasePanelList()) {
                databases.add(basePanel.database());
            }
        } else {
            databases.add(frame.getCurrentBasePanel().database());
        }

        return databases;
    }

    private void connect(boolean auto) {
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

            ooBaseDirectory = Globals.prefs.get(JabRefPreferences.OO_JARS_PATH);
            sOffice = Globals.prefs.get(JabRefPreferences.OO_EXECUTABLE_PATH);
        }
        else { // Manual connect

            showConnectDialog();
            if (!dialogOkPressed) {
                return;
            }

            String ooPath = Globals.prefs.get(JabRefPreferences.OO_PATH);
            String ooJars = Globals.prefs.get(JabRefPreferences.OO_JARS_PATH);
            sOffice = Globals.prefs.get(JabRefPreferences.OO_EXECUTABLE_PATH);

            if (OS.WINDOWS) {
                ooBaseDirectory = ooPath + "\\program\\classes";
                sOffice = ooPath + "\\program\\soffice.exe";
            }
            else if (OS.OS_X) {
                sOffice = ooPath + "/Contents/MacOS/soffice.bin";
                ooBaseDirectory = ooPath + "/Contents/Resources/java";
            }
            else {
                // Linux:
                ooBaseDirectory = ooJars + "/program/classes";
            }
        }

        // Add OO jars to the classpath:
        try {
            File[] jarFiles = new File[] {new File(ooBaseDirectory, "unoil.jar"), new File(ooBaseDirectory, "jurt.jar"),
                    new File(ooBaseDirectory, "juh.jar"), new File(ooBaseDirectory, "ridl.jar")};
            URL[] jarList = new URL[jarFiles.length];
            for (int i = 0; i < jarList.length; i++) {
                if (!jarFiles[i].exists()) {
                    throw new Exception("File not found: " + jarFiles[i].getPath());
                }
                jarList[i] = jarFiles[i].toURI().toURL();
            }
            addURL(jarList);

            // Show progress dialog:
            final JDialog progDiag = new AutoDetectPaths(diag).showProgressDialog(diag, Localization.lang("Connecting"),
                    Localization.lang("Please wait..."), false);
            getWorker().run(); // Do the actual connection, using Spin to get off the EDT.
            progDiag.dispose();
            diag.dispose();
            if (ooBase == null) {
                throw connectException;
            }

            if (ooBase.isConnectedToDocument()) {
                frame.output(Localization.lang("Connected to document") + ": " + ooBase.getCurrentDocumentTitle());
            }

            // Enable actions that depend on Connect:
            selectDocument.setEnabled(true);
            pushEntries.setEnabled(true);
            pushEntriesInt.setEnabled(true);
            pushEntriesEmpty.setEnabled(true);
            pushEntriesAdvanced.setEnabled(true);
            update.setEnabled(true);
            merge.setEnabled(true);
            manageCitations.setEnabled(true);

        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", e);
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Unable to connect. One possible reason is that JabRef "
                            + "and OpenOffice/LibreOffice are not both running in either 32 bit mode or 64 bit mode."));
        } catch (Throwable e) {
            LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", e);
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Could not connect to running OpenOffice.")
                        + "\n"
                        + Localization.lang("Make sure you have installed OpenOffice with Java support.")
                        + "\n"
                        + Localization.lang("If connecting manually, please verify program and library paths.")
                        + "\n"
                        + "\n"
                        + Localization.lang("Error message:") + " " + e.getMessage());
            }
        }

    @Override
    public void run() {
        try {
            // Connect:
            ooBase = new OOBibBase(sOffice, true);
        } catch (Throwable e) {
            ooBase = null;
            connectException = e;
            //JOptionPane.showMessageDialog(frame, Globals.lang("Unable to connect"));
        }
    }

    /**
     * Read the style file. Record the last modified time of the file.
     * @throws Exception
     */
    private void readStyleFile() throws Exception {
        if (useDefaultAuthoryearStyle) {
            URL defPath = JabRef.class.getResource(DEFAULT_AUTHORYEAR_STYLE_PATH);
            Reader r = new InputStreamReader(defPath.openStream());
            style = new OOBibStyle(r);
        }
        else if (useDefaultNumericalStyle) {
            URL defPath = JabRef.class.getResource(DEFAULT_NUMERICAL_STYLE_PATH);
            Reader r = new InputStreamReader(defPath.openStream());
            style = new OOBibStyle(r);
        }
        else {
            style = new OOBibStyle(new File(styleFile));
        }
    }


    // The methods addFile and associated final Class[] parameters were gratefully copied from
    // anthony_miguel @ http://forum.java.sun.com/thread.jsp?forum=32&thread=300557&tstart=0&trange=15
    private static final Class<?>[] CLASS_PARAMETERS = new Class[] {URL.class};


    private static void addURL(URL[] u) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;

        try {
            Method method = sysclass.getDeclaredMethod("addURL", CLASS_PARAMETERS);
            method.setAccessible(true);
            for (URL anU : u) {
                method.invoke(sysloader, anU);
            }
        } catch (Throwable t) {
            LOGGER.error("Could not add URL to system classloader", t);
            throw new IOException("Error, could not add URL to system classloader");
        }
    }

    private void updateConnectionParams(String ooPath, String ooExec, String ooJars) {
        Globals.prefs.put(JabRefPreferences.OO_PATH, ooPath);
        Globals.prefs.put(JabRefPreferences.OO_EXECUTABLE_PATH, ooExec);
        Globals.prefs.put(JabRefPreferences.OO_JARS_PATH, ooJars);
    }

    private void showConnectDialog() {
        dialogOkPressed = false;
        final JDialog cDiag = new JDialog(frame, Localization.lang("Set connection parameters"), true);
        final JTextField ooPath = new JTextField(30);
        JButton browseOOPath = new JButton(Localization.lang("Browse"));
        ooPath.setText(Globals.prefs.get(JabRefPreferences.OO_PATH));
        final JTextField ooExec = new JTextField(30);
        JButton browseOOExec = new JButton(Localization.lang("Browse"));
        browseOOExec.addActionListener(BrowseAction.buildForFile(ooExec));
        final JTextField ooJars = new JTextField(30);
        JButton browseOOJars = new JButton(Localization.lang("Browse"));
        browseOOJars.addActionListener(BrowseAction.buildForDir(ooJars));
        ooExec.setText(Globals.prefs.get(JabRefPreferences.OO_EXECUTABLE_PATH));
        ooJars.setText(Globals.prefs.get(JabRefPreferences.OO_JARS_PATH));
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
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        ActionListener tfListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText());
                cDiag.dispose();
            }
        };

        ooPath.addActionListener(tfListener);
        ooExec.addActionListener(tfListener);
        ooJars.addActionListener(tfListener);
        ok.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText());
                dialogOkPressed = true;
                cDiag.dispose();
            }
        });
        cancel.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {
                cDiag.dispose();
            }
        });
        bb.addGlue();
        bb.addRelatedGap();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        cDiag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        cDiag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        cDiag.pack();
        cDiag.setLocationRelativeTo(frame);
        cDiag.setVisible(true);

    }

    private void pushEntries(boolean inParenthesis, boolean withText, boolean addPageInfo) {
        if (!ooBase.isConnectedToDocument()) {
            JOptionPane.showMessageDialog(frame, Localization.lang("Not connected to any Writer document. Please"
                            + " make sure a document is open, and use the 'Select Writer document' button to connect to it."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        String pageInfo = null;
        if (addPageInfo) {
            AdvancedCiteDialog acd = new AdvancedCiteDialog(frame);
            acd.showDialog();
            if (acd.cancelled()) {
                return;
            }
            if (!acd.getPageInfo().isEmpty()) {
                pageInfo = acd.getPageInfo();
            }
            inParenthesis = acd.isInParenthesisCite();

        }

        BasePanel panel = frame.getCurrentBasePanel();
        if (panel != null) {
            final BibDatabase database = panel.database();
            BibEntry[] entries = panel.getSelectedEntries();
            if (entries.length > 0) {
                try {
                    if (style == null) {
                        readStyleFile();
                    }
                    ooBase.insertEntry(entries, database, getBaseList(), style, inParenthesis, withText,
                            pageInfo, Globals.prefs.getBoolean(JabRefPreferences.SYNC_OO_WHEN_CITING));
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame, Localization.lang("You must select either a valid style file, or use one of the default styles."),
                            Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                } catch (ConnectionLostException ex) {
                    showConnectionLostErrorMessage();
                } catch (UndefinedCharacterFormatException ex) {
                    reportUndefinedCharacterFormat(ex);
                } catch (UndefinedParagraphFormatException ex) {
                    reportUndefinedParagraphFormat(ex);
                } catch (Exception ex) {
                    LOGGER.warn("Could not insert entry", ex);
                }
            }

        }

    }

    private void showConnectionLostErrorMessage() {
        JOptionPane.showMessageDialog(frame, Localization.lang("Connection to OpenOffice has been lost. "
                        + "Please make sure OpenOffice is running, and try to reconnect."),
                Localization.lang("Connection lost"), JOptionPane.ERROR_MESSAGE);
    }


    private void reportUndefinedParagraphFormat(UndefinedParagraphFormatException ex) {
        JOptionPane.showMessageDialog(frame, "<html>" + Localization.lang("Your style file specifies the paragraph format '%0', "
                        + "which is undefined in your current OpenOffice document.", ex.getFormatName()) + "<br>"
                + Localization.lang("The paragraph format is controlled by the property 'ReferenceParagraphFormat' or 'ReferenceHeaderParagraphFormat' in the style file.")
                + "</html>",
                "", JOptionPane.ERROR_MESSAGE);
    }

    private void reportUndefinedCharacterFormat(UndefinedCharacterFormatException ex) {
        JOptionPane.showMessageDialog(frame, "<html>" + Localization.lang("Your style file specifies the character format '%0', "
                        + "which is undefined in your current OpenOffice document.", ex.getFormatName()) + "<br>"
                + Localization.lang("The character format is controlled by the citation property 'CitationCharacterFormat' in the style file.")
                + "</html>",
                "", JOptionPane.ERROR_MESSAGE);
    }


    private void showSettingsPopup() {
        JPopupMenu menu = new JPopupMenu();
        final JCheckBoxMenuItem autoSync = new JCheckBoxMenuItem(
                Localization.lang("Automatically sync bibliography when inserting citations"),
                Globals.prefs.getBoolean(JabRefPreferences.SYNC_OO_WHEN_CITING));
        final JRadioButtonMenuItem useActiveBase = new JRadioButtonMenuItem
                (Localization.lang("Look up BibTeX entries in the active tab only"));
        final JRadioButtonMenuItem useAllBases = new JRadioButtonMenuItem
                (Localization.lang("Look up BibTeX entries in all open databases"));
        final JMenuItem clearConnectionSettings = new JMenuItem
                (Localization.lang("Clear connection settings"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(useActiveBase);
        bg.add(useAllBases);
        if (Globals.prefs.getBoolean(JabRefPreferences.USE_ALL_OPEN_BASES)) {
            useAllBases.setSelected(true);
        } else {
            useActiveBase.setSelected(true);
        }

        autoSync.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean(JabRefPreferences.SYNC_OO_WHEN_CITING, autoSync.isSelected());
            }
        });
        useAllBases.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean(JabRefPreferences.USE_ALL_OPEN_BASES, useAllBases.isSelected());
            }
        });
        useActiveBase.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.putBoolean(JabRefPreferences.USE_ALL_OPEN_BASES, !useActiveBase.isSelected());
            }
        });
        clearConnectionSettings.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                Globals.prefs.clear(JabRefPreferences.OO_PATH);
                Globals.prefs.clear(JabRefPreferences.OO_EXECUTABLE_PATH);
                Globals.prefs.clear(JabRefPreferences.OO_JARS_PATH);
                frame.output(Localization.lang("Cleared connection settings."));
            }
        });

        menu.add(autoSync);
        menu.addSeparator();
        menu.add(useActiveBase);
        menu.add(useAllBases);
        menu.addSeparator();
        menu.add(clearConnectionSettings);
        menu.show(settingsB, 0, settingsB.getHeight());
    }


    public String getName() {
        return "OpenOffice";
    }

    class OOPanel extends SidePaneComponent {

        private final OpenOfficePanel ooPanel;
        public OOPanel(SidePaneManager sidePaneManager, Icon url, String s, OpenOfficePanel panel) {
            super(sidePaneManager, url, s);
            ooPanel = panel;
        }

        @Override
        public String getName() {
            return ooPanel.getName();
        }

        @Override
        public void componentClosing() {
            Globals.prefs.putBoolean(JabRefPreferences.SHOW_OO_PANEL, false);
        }

        @Override
        public void componentOpening() {
            Globals.prefs.putBoolean(JabRefPreferences.SHOW_OO_PANEL, true);
        }
    }

}
