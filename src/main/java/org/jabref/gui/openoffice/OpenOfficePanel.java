package org.jabref.gui.openoffice;

import java.awt.BorderLayout;
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

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.icon.IconTheme;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableKeyChange;
import org.jabref.gui.util.BackgroundTask;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.DirectoryDialogConfiguration;
import org.jabref.gui.util.FileDialogConfiguration;
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
 * Pane to manage the interaction between JabRef and OpenOffice.
 */
public class OpenOfficePanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenOfficePanel.class);
    private final DialogService dialogService;

    private JDialog diag;
    private final Button connect;
    private final Button manualConnect;
    private final Button selectDocument;
    private final Button setStyleFile = new Button(Localization.lang("Select style"));
    private final Button pushEntries = new Button(Localization.lang("Cite"));
    private final Button pushEntriesInt = new Button(Localization.lang("Cite in-text"));
    private final Button pushEntriesEmpty = new Button(Localization.lang("Insert empty citation"));
    private final Button pushEntriesAdvanced = new Button(Localization.lang("Cite special"));
    private final Button update;
    private final Button merge = new Button(Localization.lang("Merge citations"));
    private final Button manageCitations = new Button(Localization.lang("Manage citations"));
    private final Button exportCitations = new Button(Localization.lang("Export cited"));
    private final Button settingsB = new Button(Localization.lang("Settings"));
    private final Button help;
    private final VBox vbox = new VBox();

    private OOBibBase ooBase;
    private final JabRefFrame frame;
    private OOBibStyle style;
    private StyleSelectDialog styleDialog;
    private boolean dialogOkPressed;
    private final OpenOfficePreferences preferences;
    private final StyleLoader loader;

    public OpenOfficePanel(JabRefFrame jabRefFrame) {
        Node connectImage = IconTheme.JabRefIcons.CONNECT_OPEN_OFFICE.getGraphicNode();

        ActionFactory factory = new ActionFactory(Globals.getKeyPrefs());

        connect = new Button();
        connect.setGraphic(connectImage);
        manualConnect = new Button();
        manualConnect.setGraphic(connectImage);
        connect.setTooltip(new Tooltip(Localization.lang("Connect")));
        manualConnect.setTooltip(new Tooltip(Localization.lang("Manual connect")));
        HelpAction helpCommand = new HelpAction(HelpFile.OPENOFFICE_LIBREOFFICE);

        help = factory.createIconButton(StandardActions.HELP, helpCommand.getCommand());

        selectDocument = new Button();
        selectDocument.setGraphic(IconTheme.JabRefIcons.OPEN.getGraphicNode());
        selectDocument.setTooltip(new Tooltip(Localization.lang("Select Writer document")));

        update = new Button();
        update.setGraphic(IconTheme.JabRefIcons.REFRESH.getGraphicNode());
        update.setTooltip(new Tooltip(Localization.lang("Sync OpenOffice/LibreOffice bibliography")));
        preferences = Globals.prefs.getOpenOfficePreferences();
        loader = new StyleLoader(preferences,
                                 Globals.prefs.getLayoutFormatterPreferences(Globals.journalAbbreviationLoader),
                                 Globals.prefs.getDefaultEncoding());

        this.frame = jabRefFrame;
        initPanel();
        dialogService = frame.getDialogService();
    }

    public Node getContent() {
        return vbox;
    }

    private void initPanel() {

        connect.setOnAction(e -> connectAutomatically());
        manualConnect.setOnAction(e -> connectManually());

        selectDocument.setTooltip(new Tooltip(Localization.lang("Select which open Writer document to work on")));
        selectDocument.setOnAction(e -> {

            try {
                ooBase.selectDocument();
                frame.output(Localization.lang("Connected to document") + ": "
                             + ooBase.getCurrentDocumentTitle().orElse(""));
            } catch (UnknownPropertyException | WrappedTargetException | IndexOutOfBoundsException |
                     NoSuchElementException | NoDocumentException ex) {
                LOGGER.warn("Problem connecting", ex);
                dialogService.showErrorDialogAndWait(ex);
            }

        });

        setStyleFile.setOnAction(event -> {

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

        pushEntries.setTooltip(new Tooltip(Localization.lang("Cite selected entries between parenthesis")));
        pushEntries.setOnAction(e -> pushEntries(true, true, false));
        pushEntriesInt.setTooltip(new Tooltip(Localization.lang("Cite selected entries with in-text citation")));
        pushEntriesInt.setOnAction(e -> pushEntries(false, true, false));
        pushEntriesEmpty.setTooltip(new Tooltip(Localization.lang("Insert a citation without text (the entry will appear in the reference list)")));
        pushEntriesEmpty.setOnAction(e -> pushEntries(false, false, false));
        pushEntriesAdvanced.setTooltip(new Tooltip(Localization.lang("Cite selected entries with extra information")));
        pushEntriesAdvanced.setOnAction(e -> pushEntries(false, true, true));

        update.setTooltip(new Tooltip(Localization.lang("Ensure that the bibliography is up-to-date")));

        update.setOnAction(event -> {
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
                    dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"),
                                                         Localization.lang("Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                                                           unresolvedKeys.get(0)));

                }
            } catch (UndefinedCharacterFormatException ex) {
                reportUndefinedCharacterFormat(ex);
            } catch (UndefinedParagraphFormatException ex) {
                reportUndefinedParagraphFormat(ex);
            } catch (ConnectionLostException ex) {
                showConnectionLostErrorMessage();
            } catch (IOException ex) {
                LOGGER.warn("Problem with style file", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("No valid style file defined"),
                                                     Localization.lang("You must select either a valid style file, or use one of the default styles."));

            } catch (BibEntryNotFoundException ex) {
                LOGGER.debug("BibEntry not found", ex);
                dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"), Localization.lang(
                                                                                                                                "Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                                                                                                                ex.getBibtexKey()));

            } catch (com.sun.star.lang.IllegalArgumentException | PropertyVetoException | UnknownPropertyException | WrappedTargetException | NoSuchElementException |
                     CreationException ex) {
                LOGGER.warn("Could not update bibliography", ex);
            }

        });

        merge.setTooltip(new Tooltip(Localization.lang("Combine pairs of citations that are separated by spaces only")));
        merge.setOnAction(e -> {
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
        settingsB.setOnAction(e -> showSettingsPopup());
        manageCitations.setOnAction(e -> {
            try {
                CitationManager cm = new CitationManager(ooBase, dialogService);
                cm.showDialog();
            } catch (NoSuchElementException | WrappedTargetException | UnknownPropertyException ex) {
                LOGGER.warn("Problem showing citation manager", ex);
            }
        });

        exportCitations.setOnAction(event -> exportEntries());

        selectDocument.setDisable(true);
        pushEntries.setDisable(true);
        pushEntriesInt.setDisable(true);
        pushEntriesEmpty.setDisable(true);
        pushEntriesAdvanced.setDisable(true);
        update.setDisable(true);
        merge.setDisable(true);
        manageCitations.setDisable(true);
        exportCitations.setDisable(true);
        diag = new JDialog((JFrame) null, "OpenOffice/LibreOffice panel", false);

        HBox hbox = new HBox();
        hbox.getChildren().addAll(connect, manualConnect, selectDocument, update, help);
        vbox.setFillWidth(true);
        vbox.getChildren().addAll(hbox, setStyleFile, pushEntries, pushEntriesInt, pushEntriesAdvanced, pushEntriesEmpty, merge, manageCitations, exportCitations, settingsB);
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

                dialogService.showErrorDialogAndWait(Localization.lang("Unable to generate new library"),
                                                     Localization.lang("Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                                                       unresolvedKeys.get(0)));

            }

            Defaults defaults = new Defaults(Globals.prefs.getDefaultBibDatabaseMode());

            BibDatabaseContext databaseContext = new BibDatabaseContext(newDatabase, defaults);
            this.frame.addTab(databaseContext, true);

        } catch (BibEntryNotFoundException ex) {
            LOGGER.debug("BibEntry not found", ex);
            dialogService.showErrorDialogAndWait(Localization.lang("Unable to synchronize bibliography"),
                                                 Localization.lang("Your OpenOffice/LibreOffice document references the BibTeX key '%0', which could not be found in your current library.",
                                                                   ex.getBibtexKey()));

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

    private void connectAutomatically() {
        BackgroundTask
                      .wrap(() -> {
                          DetectOpenOfficeInstallation officeInstallation = new DetectOpenOfficeInstallation(diag, preferences, dialogService);

                          Boolean installed = officeInstallation.isInstalled().get();
                          if ((installed == null) || !installed) {
                              throw new IllegalStateException("OpenOffice Installation could not be detected.");
                          }
                          return null; // can not use BackgroundTask.wrap(Runnable) because Runnable.run() can't throw exceptions
                      })
                      .onSuccess(x -> connect())
                      .onFailure(ex -> dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), ex))
                      .executeWith(Globals.TASK_EXECUTOR);
    }

    private void connectManually() {
        showManualConnectionDialog();
        if (!dialogOkPressed) {
            return;
        }

        connect();
    }

    private void connect() {
        JDialog progressDialog = null;

        try {
            // Add OO JARs to the classpath
            loadOpenOfficeJars(Paths.get(preferences.getInstallationPath()));

            // Show progress dialog:
            progressDialog = new DetectOpenOfficeInstallation(diag, preferences, dialogService)
                                                                                               .showProgressDialog(diag, Localization.lang("Connecting"), Localization.lang("Please wait..."));
            JDialog finalProgressDialog = progressDialog;
            BackgroundTask
                          .wrap(this::createBibBase)
                          .onFinished(() -> SwingUtilities.invokeLater(() -> {
                              finalProgressDialog.dispose();
                              diag.dispose();
                          }))
                          .onSuccess(ooBase -> {
                              this.ooBase = ooBase;

                              if (ooBase.isConnectedToDocument()) {
                                  frame.output(Localization.lang("Connected to document") + ": " + ooBase.getCurrentDocumentTitle().orElse(""));
                              }

                              // Enable actions that depend on Connect:
                              selectDocument.setDisable(false);
                              pushEntries.setDisable(false);
                              pushEntriesInt.setDisable(false);
                              pushEntriesEmpty.setDisable(false);
                              pushEntriesAdvanced.setDisable(false);
                              update.setDisable(false);
                              merge.setDisable(false);
                              manageCitations.setDisable(false);
                              exportCitations.setDisable(false);

                          })
                          .onFailure(ex -> dialogService.showErrorDialogAndWait(Localization.lang("Autodetection failed"), Localization.lang("Autodetection failed"), ex))
                          .executeWith(Globals.TASK_EXECUTOR);
            diag.dispose();

        } catch (UnsatisfiedLinkError e) {
            LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", e);

            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Unable to connect. One possible reason is that JabRef "
                                                                                                               + "and OpenOffice/LibreOffice are not both running in either 32 bit mode or 64 bit mode.")));

        } catch (IOException e) {
            LOGGER.warn("Could not connect to running OpenOffice/LibreOffice", e);

            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Could not connect to running OpenOffice/LibreOffice."),
                                                                                             Localization.lang("Could not connect to running OpenOffice/LibreOffice.") + "\n"
                                                                                                                                                                        + Localization.lang("Make sure you have installed OpenOffice/LibreOffice with Java support.") + "\n"
                                                                                                                                                                        + Localization.lang("If connecting manually, please verify program and library paths.")
                                                                                                                                                                        + "\n" + "\n" + Localization.lang("Error message:"),
                                                                                             e));

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

    private OOBibBase createBibBase() throws IOException, InvocationTargetException, IllegalAccessException,
        WrappedTargetException, BootstrapException, UnknownPropertyException, NoDocumentException,
        NoSuchElementException, CreationException {
        // Connect
        return new OOBibBase(preferences.getExecutablePath(), true);
    }

    private static void addURL(List<URL> jarList) throws IOException {
        URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
        Class<URLClassLoader> sysclass = URLClassLoader.class;
        try {
            Method method = sysclass.getDeclaredMethod("addURL", URL.class);
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
        final JDialog cDiag = new JDialog((JFrame) null, Localization.lang("Set connection parameters"), true);
        final NativeDesktop nativeDesktop = JabRefDesktop.getNativeDesktop();

        final DialogService dialogService = this.dialogService;
        DirectoryDialogConfiguration dirDialogConfiguration = new DirectoryDialogConfiguration.Builder()
                                                                                                        .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                                                        .build();

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                                                                                               .withInitialDirectory(nativeDesktop.getApplicationDirectory())
                                                                                               .build();

        // Path fields
        final JTextField ooPath = new JTextField(30);
        JButton browseOOPath = new JButton(Localization.lang("Browse"));
        ooPath.setText(preferences.getInstallationPath());
        browseOOPath.addActionListener(e -> DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showDirectorySelectionDialog(dirDialogConfiguration))
                                                               .ifPresent(f -> ooPath.setText(f.toAbsolutePath().toString())));

        final JTextField ooExec = new JTextField(30);
        JButton browseOOExec = new JButton(Localization.lang("Browse"));
        ooExec.setText(preferences.getExecutablePath());
        browseOOExec.addActionListener(e -> DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showFileOpenDialog(fileDialogConfiguration))
                                                               .ifPresent(f -> ooExec.setText(f.toAbsolutePath().toString())));

        final JTextField ooJars = new JTextField(30);
        ooJars.setText(preferences.getJarsPath());
        JButton browseOOJars = new JButton(Localization.lang("Browse"));
        browseOOJars.addActionListener(e -> DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showDirectorySelectionDialog(dirDialogConfiguration))
                                                               .ifPresent(f -> ooJars.setText(f.toAbsolutePath().toString())));

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
        cDiag.setVisible(true);
    }

    private void pushEntries(boolean inParenthesisIn, boolean withText, boolean addPageInfo) {
        if (!ooBase.isConnectedToDocument()) {

            DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(
                                                                                             Localization.lang("Error pushing entries"), Localization.lang("Not connected to any Writer document. Please"
                                                                                                                                                           + " make sure a document is open, and use the 'Select Writer document' button to connect to it.")));

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

                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(
                                                                                                     Localization.lang("No valid style file defined"),
                                                                                                     Localization.lang("You must select either a valid style file, or use one of the default styles.")));

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
        boolean citePressed = dialogService.showConfirmationDialogAndWait(Localization.lang("Cite"),
                                                                          Localization.lang("Cannot cite entries without BibTeX keys. Generate keys now?"),
                                                                          Localization.lang("Generate keys"),
                                                                          Localization.lang("Cancel"));

        BasePanel panel = frame.getCurrentBasePanel();
        if (citePressed && (panel != null)) {
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
        DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Connection lost"),
                                                                                         Localization.lang("Connection to OpenOffice/LibreOffice has been lost. "
                                                                                                           + "Please make sure OpenOffice/LibreOffice is running, and try to reconnect.")));

    }

    private void reportUndefinedParagraphFormat(UndefinedParagraphFormatException ex) {
        DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Undefined paragraph format"),
                                                                                         Localization.lang("Your style file specifies the paragraph format '%0', "
                                                                                                           + "which is undefined in your current OpenOffice/LibreOffice document.",
                                                                                                           ex.getFormatName())
                                                                                                                                          + "\n" +
                                                                                                                                          Localization.lang("The paragraph format is controlled by the property 'ReferenceParagraphFormat' or 'ReferenceHeaderParagraphFormat' in the style file.")));

    }

    private void reportUndefinedCharacterFormat(UndefinedCharacterFormatException ex) {
        DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showErrorDialogAndWait(Localization.lang("Undefined character format"),
                                                                                         Localization.lang(
                                                                                                           "Your style file specifies the character format '%0', "
                                                                                                           + "which is undefined in your current OpenOffice/LibreOffice document.",
                                                                                                           ex.getFormatName())
                                                                                                                                          + "\n"
                                                                                                                                          + Localization.lang("The character format is controlled by the citation property 'CitationCharacterFormat' in the style file.")

        ));
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
        //  menu.show(settingsB, 0, settingsB.getHeight());
    }
}
