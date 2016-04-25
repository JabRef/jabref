/*  Copyright (C) 2003-2016 JabRef contributors.
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
package net.sf.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.SidePaneComponent;
import net.sf.jabref.gui.SidePaneManager;
import net.sf.jabref.gui.actions.BrowseAction;
import net.sf.jabref.gui.help.HelpAction;
import net.sf.jabref.gui.help.HelpFiles;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.worker.AbstractWorker;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.openoffice.OOBibStyle;
import net.sf.jabref.logic.openoffice.OpenOfficePreferences;
import net.sf.jabref.logic.openoffice.StyleLoader;
import net.sf.jabref.logic.openoffice.UndefinedParagraphFormatException;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.model.database.BibDatabase;
import net.sf.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This test panel can be opened by reflection from JabRef, passing the JabRefFrame as an
 * argument to the start() method. It displays buttons for testing interaction functions
 * between JabRef and OpenOffice.
 */
public class OpenOfficePanel extends AbstractWorker {

    private static final Log LOGGER = LogFactory.getLog(OpenOfficePanel.class);

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
    private final JButton help = new HelpAction(Localization.lang("OpenOffice/LibreOffice integration"),
            HelpFiles.OPENOFFICE_LIBREOFFICE).getHelpButton();
    private OOBibBase ooBase;
    private JabRefFrame frame;
    private SidePaneManager manager;
    private OOBibStyle style;
    private StyleSelectDialog styleDialog;
    private boolean dialogOkPressed;
    private boolean autoDetected;
    private String sOffice;
    private IOException connectException;
    private final OpenOfficePreferences preferences;
    private final StyleLoader loader;

    private static OpenOfficePanel instance;


    private OpenOfficePanel() {
        Icon connectImage = IconTheme.JabRefIcon.CONNECT_OPEN_OFFICE.getSmallIcon();

        connect = new JButton(connectImage);
        manualConnect = new JButton(connectImage);
        connect.setToolTipText(Localization.lang("Connect"));
        manualConnect.setToolTipText(Localization.lang("Manual connect"));
        selectDocument = new JButton(IconTheme.JabRefIcon.OPEN.getSmallIcon());
        selectDocument.setToolTipText(Localization.lang("Select Writer document"));
        update = new JButton(IconTheme.JabRefIcon.REFRESH.getSmallIcon());
        update.setToolTipText(Localization.lang("Sync OpenOffice/LibreOffice bibliography"));
        preferences = new OpenOfficePreferences(Globals.prefs);
        loader = new StyleLoader(preferences, Globals.journalAbbreviationLoader.getRepository(),
                Globals.prefs.getDefaultEncoding());
    }

    public static OpenOfficePanel getInstance() {
        if (OpenOfficePanel.instance == null) {
            OpenOfficePanel.instance = new OpenOfficePanel();
        }
        return OpenOfficePanel.instance;
    }

    public SidePaneComponent getSidePaneComponent() {
        return comp;
    }

    public void init(JabRefFrame jabRefFrame, SidePaneManager spManager) {
        this.frame = jabRefFrame;
        this.manager = spManager;
        comp = new OOPanel(spManager, IconTheme.getImage("openoffice"), "OpenOffice/LibreOffice", this);
        initPanel();
        spManager.register(getName(), comp);
    }

    public JMenuItem getMenuItem() {
        if (preferences.showPanel()) {
            manager.show(getName());
        }
        JMenuItem item = new JMenuItem(Localization.lang("OpenOffice/LibreOffice connection"),
                IconTheme.getImage("openoffice"));
        item.addActionListener(event -> manager.show(getName()));
        return item;
    }

    private void initPanel() {

        connect.addActionListener(e -> connect(true));
        manualConnect.addActionListener(e -> connect(false));

        selectDocument.setToolTipText(Localization.lang("Select which open Writer document to work on"));
        selectDocument.addActionListener(e -> {

            try {
                ooBase.selectDocument();
                frame.output(Localization.lang("Connected to document") + ": "
                        + ooBase.getCurrentDocumentTitle().orElse(""));
            } catch (UnknownPropertyException | WrappedTargetException | IndexOutOfBoundsException |
                    NoSuchElementException | NoDocumentException ex) {
                JOptionPane.showMessageDialog(frame, ex.getMessage(), Localization.lang("Error"),
                        JOptionPane.ERROR_MESSAGE);
                LOGGER.warn("Problem connecting", ex);
            }

        });

        setStyleFile.addActionListener(event -> {

            if (styleDialog == null) {
                styleDialog = new StyleSelectDialog(frame, preferences, loader);
            }
            styleDialog.setVisible(true);
            styleDialog.getStyle().ifPresent(selectedStyle -> {
                style = selectedStyle;
                try {
                    style.ensureUpToDate();
                } catch (IOException e) {
                    LOGGER.warn("Unable to reload style file '" + style.getPath() + "'", e);
                }
                frame.setStatus(Localization.lang("Current style is '%0'", style.getName()));
            });

        });

        pushEntries.setToolTipText(Localization.lang("Cite selected entries between parenthesis"));
        pushEntries.addActionListener(e -> pushEntries(true, true, false));
        pushEntriesInt.setToolTipText(Localization.lang("Cite selected entries with in-text citation"));
        pushEntriesInt.addActionListener(e -> pushEntries(false, true, false));
        pushEntriesEmpty.setToolTipText(
                Localization.lang("Insert a citation without text (the entry will appear in the reference list)"));
        pushEntriesEmpty.addActionListener(e -> pushEntries(false, false, false));
        pushEntriesAdvanced.setToolTipText(Localization.lang("Cite selected entries with extra information"));
        pushEntriesAdvanced.addActionListener(e -> pushEntries(false, true, true));

        update.setToolTipText(Localization.lang("Ensure that the bibliography is up-to-date"));
        Action updateAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    if (style == null) {
                        style = loader.getUsedStyle();
                    } else {
                        style.ensureUpToDate();
                    }

                    ooBase.updateSortedReferenceMarks();

                    List<BibDatabase> databases = getBaseList();
                    List<String> unresolvedKeys = ooBase.refreshCiteMarkers(databases, style);
                    ooBase.rebuildBibTextSection(databases, style);
                    if (!unresolvedKeys.isEmpty()) {
                        JOptionPane.showMessageDialog(frame,
                                Localization.lang(
                                        "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current database.",
                                        unresolvedKeys.get(0)),
                                Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                    }
                } catch (UndefinedCharacterFormatException ex) {
                    reportUndefinedCharacterFormat(ex);
                } catch (UndefinedParagraphFormatException ex) {
                    reportUndefinedParagraphFormat(ex);
                } catch (ConnectionLostException ex) {
                    showConnectionLostErrorMessage();
                } catch (IOException ex) {
                    JOptionPane.showMessageDialog(frame,
                            Localization
                                    .lang("You must select either a valid style file, or use one of the default styles."),
                            Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.warn("Problem with style file", ex);
                    return;
                } catch (BibEntryNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang(
                                    "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current database.",
                                    ex.getBibtexKey()),
                            Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.debug("BibEntry not found", ex);
                } catch (Exception ex) {
                    LOGGER.warn("Could not update bibliography", ex);
                }
            }
        };
        update.addActionListener(updateAction);

        merge.setToolTipText(Localization.lang("Combine pairs of citations that are separated by spaces only"));
        merge.addActionListener(e -> {
            try {
                ooBase.combineCiteMarkers(getBaseList(), style);
            } catch (UndefinedCharacterFormatException ex) {
                reportUndefinedCharacterFormat(ex);
            } catch (Exception ex) {
                LOGGER.warn("Problem combining cite markers", ex);
            }

        });
        settingsB.addActionListener(e -> showSettingsPopup());
        manageCitations.addActionListener(e -> {
            try {
                CitationManager cm = new CitationManager(frame, ooBase);
                cm.showDialog();
            } catch (NoSuchElementException | WrappedTargetException | UnknownPropertyException ex) {
                LOGGER.warn("Problem showing citation manager", ex);
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
        diag = new JDialog((JFrame) null, "OpenOffice/LibreOffice panel", false);

        FormBuilder mainBuilder = FormBuilder.create().layout(new FormLayout("fill:pref:grow", "p,p,p,p,p,p,p,p,p,p"));

        FormBuilder topRowBuilder = FormBuilder.create()
                .layout(new FormLayout("fill:pref:grow, 1dlu, fill:pref:grow, 1dlu, fill:pref:grow, "
                        + "1dlu, fill:pref:grow, 1dlu, fill:pref:grow", "pref"));
        topRowBuilder.add(connect).xy(1, 1);
        topRowBuilder.add(manualConnect).xy(3, 1);
        topRowBuilder.add(selectDocument).xy(5, 1);
        topRowBuilder.add(update).xy(7, 1);
        topRowBuilder.add(help).xy(9, 1);
        mainBuilder.add(topRowBuilder.getPanel()).xy(1, 1);
        mainBuilder.add(setStyleFile).xy(1, 2);
        mainBuilder.add(pushEntries).xy(1, 3);
        mainBuilder.add(pushEntriesInt).xy(1, 4);
        mainBuilder.add(pushEntriesAdvanced).xy(1, 5);
        mainBuilder.add(pushEntriesEmpty).xy(1, 6);
        mainBuilder.add(merge).xy(1, 7);
        mainBuilder.add(manageCitations).xy(1, 8);
        mainBuilder.add(settingsB).xy(1, 9);

        JPanel content = new JPanel();
        comp.setContentContainer(content);
        content.setLayout(new BorderLayout());
        content.add(mainBuilder.getPanel(), BorderLayout.CENTER);

        frame.getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.getKeyPrefs().getKey(KeyBinding.REFRESH_OO), "Refresh OO");
        frame.getTabbedPane().getActionMap().put("Refresh OO", updateAction);

    }

    private List<BibDatabase> getBaseList() {
        List<BibDatabase> databases = new ArrayList<>();
        if (preferences.useAllDatabases()) {
            for (BasePanel basePanel : frame.getBasePanelList()) {
                databases.add(basePanel.getDatabase());
            }
        } else {
            databases.add(frame.getCurrentBasePanel().getDatabase());
        }

        return databases;
    }

    private void connect(boolean auto) {
        String ooJarsDirectory;
        if (auto) {
            AutoDetectPaths adp = new AutoDetectPaths(diag, preferences);
            if (adp.runAutodetection()) {
                autoDetected = true;
                dialogOkPressed = true;
                diag.dispose();
            } else if (!adp.canceled()) {
                JOptionPane.showMessageDialog(diag, Localization.lang("Autodetection failed"),
                        Localization.lang("Autodetection failed"), JOptionPane.ERROR_MESSAGE);
            } else {
                frame.setStatus(Localization.lang("Operation canceled."));
            }
            if (!autoDetected) {
                return;
            }

            ooJarsDirectory = preferences.getJarsPath();
            sOffice = preferences.getExecutablePath();
        } else { // Manual connect

            showConnectDialog();
            if (!dialogOkPressed) {
                return;
            }

            String ooPath = preferences.getOOPath();
            String ooJars = preferences.getJarsPath();
            sOffice = preferences.getExecutablePath();

            if (OS.WINDOWS) {
                ooJarsDirectory = ooPath + OpenOfficePreferences.WINDOWS_JARS_SUBPATH;
                sOffice = ooPath + OpenOfficePreferences.WINDOWS_EXECUTABLE_SUBPATH
                        + OpenOfficePreferences.WINDOWS_EXECUTABLE;
            } else if (OS.OS_X) {
                sOffice = ooPath + OpenOfficePreferences.OSX_EXECUTABLE_SUBPATH + OpenOfficePreferences.OSX_EXECUTABLE;
                ooJarsDirectory = ooPath + OpenOfficePreferences.OSX_JARS_SUBPATH;
            } else {
                // Linux:
                ooJarsDirectory = ooJars + "/program/classes";
            }
        }

        // Add OO jars to the classpath:
        try {
            List<File> jarFiles = Arrays.asList(new File(ooJarsDirectory, "unoil.jar"),
                    new File(ooJarsDirectory, "jurt.jar"), new File(ooJarsDirectory, "juh.jar"),
                    new File(ooJarsDirectory, "ridl.jar"));
            List<URL> jarList = new ArrayList<>(jarFiles.size());
            for (File jarFile : jarFiles) {
                if (!jarFile.exists()) {
                    throw new IOException("File not found: " + jarFile.getPath());
                }
                jarList.add(jarFile.toURI().toURL());
            }
            addURL(jarList);

            // Show progress dialog:
            final JDialog progDiag = new AutoDetectPaths(diag, preferences).showProgressDialog(diag,
                    Localization.lang("Connecting"),
                    Localization.lang("Please wait..."), false);
            getWorker().run(); // Do the actual connection, using Spin to get off the EDT.
            progDiag.dispose();
            diag.dispose();
            if (ooBase == null) {
                throw connectException;
            }

            if (ooBase.isConnectedToDocument()) {
                frame.output(Localization.lang("Connected to document") + ": "
                        + ooBase.getCurrentDocumentTitle().orElse(""));
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
        } catch (IOException e) {
            LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", e);
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Could not connect to running OpenOffice/LibreOffice.") + "\n"
                            + Localization.lang("Make sure you have installed OpenOffice/LibreOffice with Java support.") + "\n"
                            + Localization.lang("If connecting manually, please verify program and library paths.")
                            + "\n" + "\n" + Localization.lang("Error message:") + " " + e.getMessage());
        }
    }

    @Override
    public void run() {
        try {
            // Connect:
            ooBase = new OOBibBase(sOffice, true);
        } catch (Exception e) {
            ooBase = null;
            connectException = new IOException(e.getMessage());
        }
    }



    // The methods addFile and associated final Class[] parameters were gratefully copied from
    // anthony_miguel @ http://forum.java.sun.com/thread.jsp?forum=32&thread=300557&tstart=0&trange=15
    private static final Class<?>[] CLASS_PARAMETERS = new Class[] {URL.class};


    private static void addURL(List<URL> jarList) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", CLASS_PARAMETERS);
            method.setAccessible(true);
            for (URL anU : jarList) {
                method.invoke(sysloader, anU);
            }
        } catch (SecurityException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException |
                InvocationTargetException e) {
            LOGGER.error("Could not add URL to system classloader", e);
            throw new IOException("Error, could not add URL to system classloader", e);

        }
    }


    private void showConnectDialog() {

        dialogOkPressed = false;
        final JDialog cDiag = new JDialog(frame, Localization.lang("Set connection parameters"), true);
        final JTextField ooPath = new JTextField(30);
        JButton browseOOPath = new JButton(Localization.lang("Browse"));
        ooPath.setText(preferences.getOOPath());
        browseOOPath.addActionListener(BrowseAction.buildForDir(ooPath));

        final JTextField ooExec = new JTextField(30);
        JButton browseOOExec = new JButton(Localization.lang("Browse"));
        ooExec.setText(preferences.getExecutablePath());
        browseOOExec.addActionListener(BrowseAction.buildForFile(ooExec));

        final JTextField ooJars = new JTextField(30);
        JButton browseOOJars = new JButton(Localization.lang("Browse"));
        browseOOJars.addActionListener(BrowseAction.buildForDir(ooJars));
        ooJars.setText(preferences.getJarsPath());

        FormBuilder builder = FormBuilder.create()
                .layout(
                        new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "pref"));
        if (OS.WINDOWS || OS.OS_X) {
            builder.add(Localization.lang("Path to OpenOffice/LibreOffice directory")).xy(1, 1);
            builder.add(ooPath).xy(3, 1);
            builder.add(browseOOPath).xy(5, 1);
        } else {
            builder.add(Localization.lang("Path to OpenOffice/LibreOffice executable")).xy(1, 1);
            builder.add(ooExec).xy(3, 1);
            builder.add(browseOOExec).xy(5, 1);

            builder.appendColumns("4dlu, pref");
            builder.add(Localization.lang("Path to OpenOffice/LibreOffice library dir")).xy(1, 3);
            builder.add(ooJars).xy(3, 3);
            builder.add(browseOOJars).xy(5, 3);
        }
        builder.padding("5dlu, 5dlu, 5dlu, 5dlu");
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));
        ActionListener tfListener = e -> {
            preferences.updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText());
            cDiag.dispose();
        };

        ooPath.addActionListener(tfListener);
        ooExec.addActionListener(tfListener);
        ooJars.addActionListener(tfListener);
        ok.addActionListener(e -> {
            preferences.updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText());
            dialogOkPressed = true;
            cDiag.dispose();
        });

        cancel.addActionListener(e -> cDiag.dispose());

        bb.addGlue();
        bb.addRelatedGap();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.padding("5dlu, 5dlu, 5dlu, 5dlu");
        cDiag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        cDiag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        cDiag.pack();
        cDiag.setLocationRelativeTo(frame);
        cDiag.setVisible(true);

    }

    private void pushEntries(boolean inParenthesisIn, boolean withText, boolean addPageInfo) {
        if (!ooBase.isConnectedToDocument()) {
            JOptionPane.showMessageDialog(frame,
                    Localization.lang("Not connected to any Writer document. Please"
                            + " make sure a document is open, and use the 'Select Writer document' button to connect to it."),
                    Localization.lang("Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        Boolean inParenthesis = inParenthesisIn;
        String pageInfo = null;
        if (addPageInfo) {
            AdvancedCiteDialog acd = new AdvancedCiteDialog(frame);
            acd.showDialog();
            if (acd.canceled()) {
                return;
            }
            if (!acd.getPageInfo().isEmpty()) {
                pageInfo = acd.getPageInfo();
            }
            inParenthesis = acd.isInParenthesisCite();

        }

        BasePanel panel = frame.getCurrentBasePanel();
        if (panel != null) {
            final BibDatabase database = panel.getDatabase();
            List<BibEntry> entries = panel.getSelectedEntries();
            if (!entries.isEmpty()) {
                try {
                    if (style == null) {
                        style = loader.getUsedStyle();
                    }
                    ooBase.insertEntry(entries, database, getBaseList(), style, inParenthesis, withText, pageInfo,
                            preferences.syncWhenCiting());
                } catch (FileNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame,
                            Localization
                                    .lang("You must select either a valid style file, or use one of the default styles."),
                            Localization.lang("No valid style file defined"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.warn("Problem with style file", ex);
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
        JOptionPane.showMessageDialog(frame,
                Localization.lang("Connection to OpenOffice/LibreOffice has been lost. "
                        + "Please make sure OpenOffice/LibreOffice is running, and try to reconnect."),
                Localization.lang("Connection lost"), JOptionPane.ERROR_MESSAGE);
    }

    private void reportUndefinedParagraphFormat(UndefinedParagraphFormatException ex) {
        JOptionPane
                .showMessageDialog(
                        frame, "<html>"
                                + Localization.lang(
                                        "Your style file specifies the paragraph format '%0', "
                                                + "which is undefined in your current OpenOffice/LibreOffice document.",
                                        ex.getFormatName())
                                + "<br>"
                                + Localization
                                        .lang("The paragraph format is controlled by the property 'ReferenceParagraphFormat' or 'ReferenceHeaderParagraphFormat' in the style file.")
                                + "</html>",
                        "", JOptionPane.ERROR_MESSAGE);
    }

    private void reportUndefinedCharacterFormat(UndefinedCharacterFormatException ex) {
        JOptionPane
                .showMessageDialog(
                        frame, "<html>"
                                + Localization.lang(
                                        "Your style file specifies the character format '%0', "
                                                + "which is undefined in your current OpenOffice/LibreOffice document.",
                                        ex.getFormatName())
                                + "<br>"
                                + Localization
                                        .lang("The character format is controlled by the citation property 'CitationCharacterFormat' in the style file.")
                                + "</html>",
                        "", JOptionPane.ERROR_MESSAGE);
    }

    private void showSettingsPopup() {
        JPopupMenu menu = new JPopupMenu();
        final JCheckBoxMenuItem autoSync = new JCheckBoxMenuItem(
                Localization.lang("Automatically sync bibliography when inserting citations"),
                preferences.syncWhenCiting());
        final JRadioButtonMenuItem useActiveBase = new JRadioButtonMenuItem(
                Localization.lang("Look up BibTeX entries in the active tab only"));
        final JRadioButtonMenuItem useAllBases = new JRadioButtonMenuItem(
                Localization.lang("Look up BibTeX entries in all open databases"));
        final JMenuItem clearConnectionSettings = new JMenuItem(Localization.lang("Clear connection settings"));
        ButtonGroup bg = new ButtonGroup();
        bg.add(useActiveBase);
        bg.add(useAllBases);
        if (preferences.useAllDatabases()) {
            useAllBases.setSelected(true);
        } else {
            useActiveBase.setSelected(true);
        }

        autoSync.addActionListener(e -> preferences.setSyncWhenCiting(autoSync.isSelected()));

        useAllBases.addActionListener(e -> preferences.setUseAllDatabases(useAllBases.isSelected()));

        useActiveBase.addActionListener(e -> preferences.setUseAllDatabases(!useActiveBase.isSelected()));

        clearConnectionSettings.addActionListener(e -> frame.output(preferences.clearConnectionSettings()));

        menu.add(autoSync);
        menu.addSeparator();
        menu.add(useActiveBase);
        menu.add(useAllBases);
        menu.addSeparator();
        menu.add(clearConnectionSettings);
        menu.show(settingsB, 0, settingsB.getHeight());
    }

    public String getName() {
        return "OpenOffice/LibreOffice";
    }


    private class OOPanel extends SidePaneComponent {

        private final OpenOfficePanel openOfficePanel;


        public OOPanel(SidePaneManager sidePaneManager, Icon url, String s, OpenOfficePanel panel) {
            super(sidePaneManager, url, s);
            openOfficePanel = panel;
        }

        @Override
        public String getName() {
            return openOfficePanel.getName();
        }

        @Override
        public void componentClosing() {
            preferences.setShowPanel(false);
        }

        @Override
        public void componentOpening() {
            preferences.setShowPanel(true);
        }

        @Override
        public int getRescalingWeight() {
            return 0;
        }
    }

}
