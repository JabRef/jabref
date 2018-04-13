package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.gui.worker.AbstractWorker;
import org.jabref.logic.bibtexkeypattern.BibtexKeyGenerator;
import org.jabref.logic.bibtexkeypattern.BibtexKeyPatternPreferences;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.openoffice.OOBibStyle;
import org.jabref.logic.openoffice.OpenOfficePreferences;
import org.jabref.logic.openoffice.StyleLoader;
import org.jabref.logic.openoffice.UndefinedParagraphFormatException;
import org.jabref.logic.util.OS;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.Defaults;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;
import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.WrappedTargetException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This test panel can be opened by reflection from JabRef, passing the JabRefFrame as an
 * argument to the start() method. It displays buttons for testing interaction functions
 * between JabRef and OpenOffice.
 */
public class OpenOfficePanel extends AbstractWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenOfficePanel.class);

    private OpenOfficeSidePanel sidePane;
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
    private final JButton exportCitations = new JButton(Localization.lang("Export cited"));
    private final JButton settingsB = new JButton(Localization.lang("Settings"));
    private final JButton help = new HelpAction(Localization.lang("OpenOffice/LibreOffice integration"),
            HelpFile.OPENOFFICE_LIBREOFFICE).getHelpButton();
    private OOBibBase ooBase;
    private JabRefFrame frame;
    private OOBibStyle style;
    private StyleSelectDialog styleDialog;
    private boolean dialogOkPressed;
    private IOException connectException;
    private final OpenOfficePreferences preferences;
    private final StyleLoader loader;

    public OpenOfficePanel(JabRefFrame jabRefFrame, SidePaneManager spManager) {
        Icon connectImage = IconTheme.JabRefIcon.CONNECT_OPEN_OFFICE.getSmallIcon();

        connect = new JButton(connectImage);
        manualConnect = new JButton(connectImage);
        connect.setToolTipText(Localization.lang("Connect"));
        manualConnect.setToolTipText(Localization.lang("Manual connect"));
        connect.setPreferredSize(new Dimension(24, 24));
        manualConnect.setPreferredSize(new Dimension(24, 24));

        selectDocument = new JButton(IconTheme.JabRefIcon.OPEN.getSmallIcon());
        selectDocument.setToolTipText(Localization.lang("Select Writer document"));
        selectDocument.setPreferredSize(new Dimension(24, 24));
        update = new JButton(IconTheme.JabRefIcon.REFRESH.getSmallIcon());
        update.setToolTipText(Localization.lang("Sync OpenOffice/LibreOffice bibliography"));
        update.setPreferredSize(new Dimension(24, 24));
        preferences = Globals.prefs.getOpenOfficePreferences();
        loader = new StyleLoader(preferences,
                Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader),
                Globals.prefs.getDefaultEncoding());

        this.frame = jabRefFrame;
        sidePane = new OpenOfficeSidePanel(spManager, IconTheme.getImage("openoffice"), "OpenOffice/LibreOffice", preferences);
        initPanel();
        spManager.register(sidePane);
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
                                        "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
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
                } catch (BibEntryNotFoundException ex) {
                    JOptionPane.showMessageDialog(frame,
                            Localization.lang(
                                    "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                    ex.getBibtexKey()),
                            Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
                    LOGGER.debug("BibEntry not found", ex);
                } catch (com.sun.star.lang.IllegalArgumentException | PropertyVetoException | UnknownPropertyException | WrappedTargetException | NoSuchElementException |
                        CreationException ex) {
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
            } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                    CreationException | NoSuchElementException | WrappedTargetException | IOException |
                    BibEntryNotFoundException ex) {
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

        exportCitations.addActionListener(event -> exportEntries());

        selectDocument.setEnabled(false);
        pushEntries.setEnabled(false);
        pushEntriesInt.setEnabled(false);
        pushEntriesEmpty.setEnabled(false);
        pushEntriesAdvanced.setEnabled(false);
        update.setEnabled(false);
        merge.setEnabled(false);
        manageCitations.setEnabled(false);
        exportCitations.setEnabled(false);
        diag = new JDialog((JFrame) null, "OpenOffice/LibreOffice panel", false);

        FormBuilder mainBuilder = FormBuilder.create()
                .layout(new FormLayout("fill:pref:grow", "p,p,p,p,p,p,p,p,p,p,p"));

        FormBuilder topRowBuilder = FormBuilder.create()
                .layout(new FormLayout(
                        "fill:pref:grow, 1dlu, fill:pref:grow, 1dlu, fill:pref:grow, 1dlu, fill:pref:grow, 1dlu, fill:pref",
                        "pref"));
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
        mainBuilder.add(exportCitations).xy(1, 9);
        mainBuilder.add(settingsB).xy(1, 10);

        JPanel content = new JPanel();
        sidePane.setContentContainer(content);
        content.setLayout(new BorderLayout());
        content.add(mainBuilder.getPanel(), BorderLayout.CENTER);

        frame.getTabbedPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.getKeyPrefs().getKey(KeyBinding.REFRESH_OO), "Refresh OO");
        frame.getTabbedPane().getActionMap().put("Refresh OO", updateAction);

    }

    private void exportEntries() {
        try {
            if (style == null) {
                style = loader.getUsedStyle();
            } else {
                style.ensureUpToDate();
            }

            ooBase.updateSortedReferenceMarks();

            List<BibDatabase> databases = getBaseList();
            List<String> unresolvedKeys = ooBase.refreshCiteMarkers(databases, style);
            BibDatabase newDatabase = ooBase.generateDatabase(databases);
            if (!unresolvedKeys.isEmpty()) {
                JOptionPane.showMessageDialog(frame,
                        Localization.lang(
                                "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                unresolvedKeys.get(0)),
                        Localization.lang("Unable to generate new library"), JOptionPane.ERROR_MESSAGE);
            }

            Defaults defaults = new Defaults(Globals.prefs.getDefaultBibDatabaseMode());

            BibDatabaseContext databaseContext = new BibDatabaseContext(newDatabase, defaults);
            this.frame.addTab(databaseContext, true);

        } catch (BibEntryNotFoundException ex) {
            JOptionPane.showMessageDialog(frame,
                    Localization.lang(
                            "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                            ex.getBibtexKey()),
                    Localization.lang("Unable to synchronize bibliography"), JOptionPane.ERROR_MESSAGE);
            LOGGER.debug("BibEntry not found", ex);
        } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                UndefinedCharacterFormatException | NoSuchElementException | WrappedTargetException | IOException |
                CreationException e) {
            LOGGER.warn("Problem generating new database.", e);
        }

    }

    private List<BibDatabase> getBaseList() {
        List<BibDatabase> databases = new ArrayList<>();
        if (preferences.getUseAllDatabases()) {
            for (BasePanel basePanel : frame.getBasePanelList()) {
                databases.add(basePanel.getDatabase());
            }
        } else {
            databases.add(frame.getCurrentBasePanel().getDatabase());
        }

        return databases;
    }

    private void connect(boolean autoDetect) {
        if (autoDetect) {
            DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(diag, preferences);

            if (!officeInstallation.isInstalled()) {
                JOptionPane.showMessageDialog(diag, Localization.lang("Autodetection failed"),
                        Localization.lang("Autodetection failed"), JOptionPane.ERROR_MESSAGE);
                return;
            }
            diag.dispose();
        } else {
            showManualConnectionDialog();
            if (!dialogOkPressed) {
                return;
            }
        }

        JDialog progressDialog = null;

        try {
            // Add OO JARs to the classpath
            loadOpenOfficeJars(Paths.get(preferences.getInstallationPath()));

            // Show progress dialog:
            progressDialog = new DetectOpenOfficeInstallation(diag, preferences)
                    .showProgressDialog(diag, Localization.lang("Connecting"), Localization.lang("Please wait..."));
            getWorker().run(); // Do the actual connection, using Spin to get off the EDT.
            progressDialog.dispose();
            diag.dispose();
            if (ooBase == null) {
                throw connectException;
            }

            if (ooBase.isConnectedToDocument()) {
                frame.output(Localization.lang("Connected to document") + ": " + ooBase.getCurrentDocumentTitle().orElse(""));
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
            exportCitations.setEnabled(true);

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
        } finally {
            if (progressDialog != null) {
                progressDialog.dispose();
            }
        }
    }

    private void loadOpenOfficeJars(Path configurationPath) throws IOException {
        List<Optional<Path>> filePaths = OpenOfficePreferences.OO_JARS.stream().map(jar -> FileUtil.find(jar, configurationPath)).collect(Collectors.toList());

        if (!filePaths.stream().allMatch(Optional::isPresent)) {
            throw new IOException("(Not all) required Open Office Jars were found inside installation path.");
        }

        List<URL> jarURLs = new ArrayList<>(OpenOfficePreferences.OO_JARS.size());
        for (Optional<Path> jarPath : filePaths) {
            jarURLs.add((jarPath.get().toUri().toURL()));
        }
        addURL(jarURLs);
    }

    @Override
    public void run() {
        try {
            // Connect
            ooBase = new OOBibBase(preferences.getExecutablePath(), true);
        } catch (UnknownPropertyException |
                CreationException | NoSuchElementException | WrappedTargetException | IOException |
                NoDocumentException | BootstrapException | InvocationTargetException | IllegalAccessException e) {
            ooBase = null;
            connectException = new IOException(e.getMessage());
        }
    }

    private static void addURL(List<URL> jarList) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", (Class<?>[]) new Class[] {URL.class});
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

    private void showManualConnectionDialog() {
        dialogOkPressed = false;
        final JDialog cDiag = new JDialog(frame, Localization.lang("Set connection parameters"), true);
        final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();

        final DialogService dirDialog = new FXDialogService();
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                .withInitialDirectory(nativeDesktop.getApplicationDirectory()).build();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(nativeDesktop.getApplicationDirectory()).build();
        DialogService fileDialog = new FXDialogService();

        // Path fields
        final JTextField ooPath = new JTextField(30);
        JButton browseOOPath = new JButton(Localization.lang("Browse"));
        ooPath.setText(preferences.getInstallationPath());
        browseOOPath.addActionListener(e ->
                DefaultTaskExecutor.runInJavaFXThread(() -> dirDialog.showDirectorySelectionDialog(dirDialogConfiguration))
                        .ifPresent(f -> ooPath.setText(f.toAbsolutePath().toString()))
        );

        final JTextField ooExec = new JTextField(30);
        JButton browseOOExec = new JButton(Localization.lang("Browse"));
        ooExec.setText(preferences.getExecutablePath());
        browseOOExec.addActionListener(e ->
                DefaultTaskExecutor.runInJavaFXThread(() -> fileDialog.showFileOpenDialog(fileDialogConfiguration))
                        .ifPresent(f -> ooExec.setText(f.toAbsolutePath().toString()))
        );

        final JTextField ooJars = new JTextField(30);
        ooJars.setText(preferences.getJarsPath());
        JButton browseOOJars = new JButton(Localization.lang("Browse"));
        browseOOJars.addActionListener(e ->
                DefaultTaskExecutor.runInJavaFXThread(() -> dirDialog.showDirectorySelectionDialog(dirDialogConfiguration))
                        .ifPresent(f -> ooJars.setText(f.toAbsolutePath().toString()))
        );

        FormBuilder builder = FormBuilder.create()
                .layout(new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, fill:pref", "pref"));

        if (OS.WINDOWS || OS.OS_X) {
            builder.add(Localization.lang("Path to OpenOffice/LibreOffice directory")).xy(1, 1);
            builder.add(ooPath).xy(3, 1);
            builder.add(browseOOPath).xy(5, 1);
        } else {
            builder.add(Localization.lang("Path to OpenOffice/LibreOffice executable")).xy(1, 1);
            builder.add(ooExec).xy(3, 1);
            builder.add(browseOOExec).xy(5, 1);

            builder.appendRows("4dlu, pref");
            builder.add(Localization.lang("Path to OpenOffice/LibreOffice library dir")).xy(1, 3);
            builder.add(ooJars).xy(3, 3);
            builder.add(browseOOJars).xy(5, 3);
        }
        builder.padding("5dlu, 5dlu, 5dlu, 5dlu");

        cDiag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        // Buttons
        JButton ok = new JButton(Localization.lang("OK"));
        JButton cancel = new JButton(Localization.lang("Cancel"));

        ok.addActionListener(e -> {
            if (OS.WINDOWS || OS.OS_X) {
                preferences.updateConnectionParams(ooPath.getText(), ooPath.getText(), ooPath.getText());
            } else {
                preferences.updateConnectionParams(ooPath.getText(), ooExec.getText(), ooJars.getText());
            }
            dialogOkPressed = true;
            cDiag.dispose();
        });
        cancel.addActionListener(e -> cDiag.dispose());

        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addRelatedGap();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();
        bb.padding("5dlu, 5dlu, 5dlu, 5dlu");
        cDiag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);

        // Finish and show dirDialog
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
            AdvancedCiteDialog citeDialog = new AdvancedCiteDialog(frame);
            citeDialog.showDialog();
            if (citeDialog.canceled()) {
                return;
            }
            if (!citeDialog.getPageInfo().isEmpty()) {
                pageInfo = citeDialog.getPageInfo();
            }
            inParenthesis = citeDialog.isInParenthesisCite();

        }

        BasePanel panel = frame.getCurrentBasePanel();
        if (panel != null) {
            final BibDatabase database = panel.getDatabase();
            List<BibEntry> entries = panel.getSelectedEntries();
            if (!entries.isEmpty() && checkThatEntriesHaveKeys(entries)) {

                try {
                    if (style == null) {
                        style = loader.getUsedStyle();
                    }
                    ooBase.insertEntry(entries, database, getBaseList(), style, inParenthesis, withText, pageInfo,
                            preferences.getSyncWhenCiting());
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
                } catch (com.sun.star.lang.IllegalArgumentException | UnknownPropertyException | PropertyVetoException |
                        CreationException | NoSuchElementException | WrappedTargetException | IOException |
                        BibEntryNotFoundException | IllegalTypeException | PropertyExistException |
                        NotRemoveableException ex) {
                    LOGGER.warn("Could not insert entry", ex);
                }
            }

        }

    }

    /**
     * Check that all entries in the list have BibTeX keys, if not ask if they should be generated
     *
     * @param entries A list of entries to be checked
     * @return true if all entries have BibTeX keys, if it so may be after generating them
     */
    private boolean checkThatEntriesHaveKeys(List<BibEntry> entries) {
        // Check if there are empty keys
        boolean emptyKeys = false;
        for (BibEntry entry : entries) {
            if (!entry.getCiteKeyOptional().isPresent()) {
                // Found one, no need to look further for now
                emptyKeys = true;
                break;
            }
        }

        // If no empty keys, return true
        if (!emptyKeys) {
            return true;
        }

        // Ask if keys should be generated
        String[] options = {Localization.lang("Generate keys"), Localization.lang("Cancel")};
        int answer = JOptionPane.showOptionDialog(this.frame,
                Localization.lang("Cannot cite entries without BibTeX keys. Generate keys now?"),
                Localization.lang("Cite"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options,
                null);
        BasePanel panel = frame.getCurrentBasePanel();
        if ((answer == JOptionPane.OK_OPTION) && (panel != null)) {
            // Generate keys
            BibtexKeyPatternPreferences prefs = Globals.prefs.getBibtexKeyPatternPreferences();
            NamedCompound undoCompound = new NamedCompound(Localization.lang("Cite"));
            for (BibEntry entry : entries) {
                if (!entry.getCiteKeyOptional().isPresent()) {
                    // Generate key
                    new BibtexKeyGenerator(panel.getBibDatabaseContext(), prefs)
                            .generateAndSetKey(entry)
                            .ifPresent(change -> undoCompound.addEdit(new UndoableKeyChange(change)));
                }
            }
            undoCompound.end();
            // Add all undos
            panel.getUndoManager().addEdit(undoCompound);
            // Now every entry has a key
            return true;
        } else {
            // No, we canceled (or there is no panel to get the database from, highly unlikely)
            return false;
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
                preferences.getSyncWhenCiting());
        final JRadioButtonMenuItem useActiveBase = new JRadioButtonMenuItem(
                Localization.lang("Look up BibTeX entries in the active tab only"));
        final JRadioButtonMenuItem useAllBases = new JRadioButtonMenuItem(
                Localization.lang("Look up BibTeX entries in all open libraries"));
        final JMenuItem clearConnectionSettings = new JMenuItem(Localization.lang("Clear connection settings"));

        ButtonGroup lookupButtonGroup = new ButtonGroup();
        lookupButtonGroup.add(useActiveBase);
        lookupButtonGroup.add(useAllBases);
        if (preferences.getUseAllDatabases()) {
            useAllBases.setSelected(true);
        } else {
            useActiveBase.setSelected(true);
        }

        autoSync.addActionListener(e -> {
            preferences.setSyncWhenCiting(autoSync.isSelected());
            Globals.prefs.setOpenOfficePreferences(preferences);
        });
        useAllBases.addActionListener(e -> {
            preferences.setUseAllDatabases(useAllBases.isSelected());
            Globals.prefs.setOpenOfficePreferences(preferences);
        });
        useActiveBase.addActionListener(e -> {
            preferences.setUseAllDatabases(!useActiveBase.isSelected());
            Globals.prefs.setOpenOfficePreferences(preferences);
        });
        clearConnectionSettings.addActionListener(e -> {
            preferences.clearConnectionSettings();
            Globals.prefs.setOpenOfficePreferences(preferences);
        });

        menu.add(autoSync);
        menu.addSeparator();
        menu.add(useActiveBase);
        menu.add(useAllBases);
        menu.addSeparator();
        menu.add(clearConnectionSettings);
        menu.show(settingsB, 0, settingsB.getHeight());
    }

    public SidePaneComponent.ToggleAction getToggleAction() {
        return sidePane.getToggleAction();
    }

}
