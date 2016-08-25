package net.sf.jabref;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.importer.ParserResultWarningDialog;
import net.sf.jabref.gui.importer.actions.OpenDatabaseAction;
import net.sf.jabref.gui.importer.worker.AutosaveStartupPrompter;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.gui.worker.VersionWorker;
import net.sf.jabref.logic.importer.ImportFormatPreferences;
import net.sf.jabref.logic.importer.OpenDatabase;
import net.sf.jabref.logic.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.OS;
import net.sf.jabref.logic.util.Version;
import net.sf.jabref.migrations.PreferencesMigrations;
import net.sf.jabref.preferences.JabRefPreferences;
import net.sf.jabref.preferences.VersionPreferences;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBluer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabRefGUI {
    private static final Log LOGGER = LogFactory.getLog(JabRefGUI.class);

    private static JabRefFrame mainFrame;

    private final List<ParserResult> bibDatabases;
    private final boolean isBlank;
    private final List<File> postponed = new ArrayList<>();
    private final List<ParserResult> failed = new ArrayList<>();
    private final List<ParserResult> toOpenTab = new ArrayList<>();

    private String focusedFile;

    public JabRefGUI(List<ParserResult> argsDatabases, boolean isBlank) {
        this.bibDatabases = argsDatabases;
        this.isBlank = isBlank;

        // passed file (we take the first one) should be focused
        if (!argsDatabases.isEmpty()) {
            focusedFile = argsDatabases.get(0).getFile().get().getAbsolutePath();
        } else {
            focusedFile = Globals.prefs.get(JabRefPreferences.LAST_FOCUSED);
        }

        openWindow();
        JabRefGUI.checkForNewVersion(false);
    }

    public static void checkForNewVersion(boolean manualExecution) {
        Version toBeIgnored = new VersionPreferences(Globals.prefs).getIgnoredVersion();
        Version currentVersion = Globals.BUILD_INFO.getVersion();
        new VersionWorker(JabRefGUI.getMainFrame(), manualExecution, currentVersion, toBeIgnored).execute();
    }

    private void openWindow() {
        // Perform checks and changes for users with a preference set from an older JabRef version.
        PreferencesMigrations.upgradeSortOrder();
        PreferencesMigrations.upgradeFaultyEncodingStrings();
        PreferencesMigrations.upgradeLabelPatternToBibtexKeyPattern();

        // This property is set to make the Mac OSX Java VM move the menu bar to the top of the screen
        if (OS.OS_X) {
            System.setProperty("apple.laf.useScreenMenuBar", "true");
        }

        // Set antialiasing on everywhere. This only works in JRE >= 1.5.
        // Or... it doesn't work, period.
        // TODO test and maybe remove this! I found this commented out with no additional info ( payload@lavabit.com )
        // Enabled since JabRef 2.11 beta 4
        System.setProperty("swing.aatext", "true");
        // Default is "on".
        // "lcd" instead of "on" because of http://wiki.netbeans.org/FaqFontRendering and http://docs.oracle.com/javase/6/docs/technotes/guides/2d/flags.html#aaFonts
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        // look and feel. This MUST be the first thing to do before loading any Swing-specific code!
        setLookAndFeel();

        // If the option is enabled, open the last edited databases, if any.
        if (!isBlank && Globals.prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)) {
            openLastEditedDatabases();
        }

        GUIGlobals.init();

        LOGGER.debug("Initializing frame");
        JabRefGUI.mainFrame = new JabRefFrame();

        // Add all bibDatabases databases to the frame:
        boolean first = false;
        if (!bibDatabases.isEmpty()) {
            for (Iterator<ParserResult> parserResultIterator = bibDatabases.iterator(); parserResultIterator.hasNext();) {
                ParserResult pr = parserResultIterator.next();

                // Define focused tab
                if (pr.getFile().get().getAbsolutePath().equals(focusedFile)) {
                    first = true;
                }

                if (pr.isInvalid()) {
                    failed.add(pr);
                    parserResultIterator.remove();
                } else if (!pr.isPostponedAutosaveFound()) {
                    if (pr.toOpenTab()) {
                        // things to be appended to an opened tab should be done after opening all tabs
                        // add them to the list
                        toOpenTab.add(pr);
                    } else {
                        JabRefGUI.getMainFrame().addParserResult(pr, first);
                        first = false;
                    }
                } else {
                    parserResultIterator.remove();
                    postponed.add(pr.getFile().get());
                }
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult pr : toOpenTab) {
            JabRefGUI.getMainFrame().addParserResult(pr, first);
            first = false;
        }

        // Start auto save timer:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_SAVE)) {
            Globals.startAutoSaveManager(JabRefGUI.getMainFrame());
        }

        // If we are set to remember the window location, we also remember the maximised
        // state. This needs to be set after the window has been made visible, so we
        // do it here:
        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            JabRefGUI.getMainFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        JabRefGUI.getMainFrame().setVisible(true);

        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            JabRefGUI.getMainFrame().setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        for (ParserResult pr : failed) {
            String message = "<html>" + Localization.lang("Error opening file '%0'.", pr.getFile().get().getName())
                    + "<p>"
                    + pr.getErrorMessage() + "</html>";

            JOptionPane.showMessageDialog(JabRefGUI.getMainFrame(), message, Localization.lang("Error opening file"),
                    JOptionPane.ERROR_MESSAGE);
        }

        // Display warnings, if any
        int tabNumber = 0;
        for (ParserResult pr : bibDatabases) {
            ParserResultWarningDialog.showParserResultWarningDialog(pr, JabRefGUI.getMainFrame(), tabNumber++);
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        // Note that we have to check whether i does not go over getBasePanelCount().
        // This is because importToOpen might have been used, which adds to
        // loadedDatabases, but not to getBasePanelCount()

        for (int i = 0; (i < bibDatabases.size()) && (i < JabRefGUI.getMainFrame().getBasePanelCount()); i++) {
            ParserResult pr = bibDatabases.get(i);
            BasePanel panel = JabRefGUI.getMainFrame().getBasePanelAt(i);
            OpenDatabaseAction.performPostOpenActions(panel, pr, true);
        }

        LOGGER.debug("Finished adding panels");

        // If any database loading was postponed due to an autosave, schedule them
        // for handing now:
        if (!postponed.isEmpty()) {
            AutosaveStartupPrompter asp = new AutosaveStartupPrompter(JabRefGUI.getMainFrame(), postponed);
            SwingUtilities.invokeLater(asp);
        }

        if (!bibDatabases.isEmpty()) {
            new FocusRequester(JabRefGUI.getMainFrame().getCurrentBasePanel().getMainTable());
        }
    }

    private void openLastEditedDatabases() {
        if (Globals.prefs.get(JabRefPreferences.LAST_EDITED) == null) {
            return;
        }

        List<String> lastFiles = Globals.prefs.getStringList(JabRefPreferences.LAST_EDITED);

        for (String fileName : lastFiles) {
            File dbFile = new File(fileName);

            // Already parsed via command line parameter, e.g., "jabref.jar somefile.bib"
            if (isLoaded(dbFile) || !dbFile.exists()) {
                continue;
            }

            ParserResult parsedDatabase = OpenDatabase.loadDatabaseOrAutoSave(fileName, false,
                    ImportFormatPreferences.fromPreferences(Globals.prefs));

            if (parsedDatabase.isNullResult()) {
                LOGGER.error(Localization.lang("Error opening file") + " '" + dbFile.getPath() + "'");
            } else {
                bibDatabases.add(parsedDatabase);
            }
        }
    }

    private boolean isLoaded(File fileToOpen) {
        for (ParserResult pr : bibDatabases) {
            if (pr.getFile().isPresent() && pr.getFile().get().equals(fileToOpen)) {
                return true;
            }
        }
        return false;
    }

    private void setLookAndFeel() {
        try {
            String lookFeel;
            String systemLookFeel = UIManager.getSystemLookAndFeelClassName();

            if (Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_LOOK_AND_FEEL)) {
                // FIXME: Problems with OpenJDK and GTK L&F
                // See https://github.com/JabRef/jabref/issues/393, https://github.com/JabRef/jabref/issues/638
                if (System.getProperty("java.runtime.name").contains("OpenJDK")) {
                    // Metal L&F
                    lookFeel = UIManager.getCrossPlatformLookAndFeelClassName();
                    LOGGER.warn(
                            "There seem to be problems with OpenJDK and the default GTK Look&Feel. Using Metal L&F instead. Change to another L&F with caution.");
                } else {
                    lookFeel = systemLookFeel;
                }
            } else {
                lookFeel = Globals.prefs.get(JabRefPreferences.WIN_LOOK_AND_FEEL);
            }

            // FIXME: Open JDK problem
            if (UIManager.getCrossPlatformLookAndFeelClassName().equals(lookFeel)
                    && !System.getProperty("java.runtime.name").contains("OpenJDK")) {
                // try to avoid ending up with the ugly Metal L&F
                Plastic3DLookAndFeel lnf = new Plastic3DLookAndFeel();
                Plastic3DLookAndFeel.setCurrentTheme(new SkyBluer());
                com.jgoodies.looks.Options.setPopupDropShadowEnabled(true);
                UIManager.setLookAndFeel(lnf);
            } else {
                try {
                    UIManager.setLookAndFeel(lookFeel);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                        UnsupportedLookAndFeelException e) {
                    // specified look and feel does not exist on the classpath, so use system l&f
                    UIManager.setLookAndFeel(systemLookFeel);
                    // also set system l&f as default
                    Globals.prefs.put(JabRefPreferences.WIN_LOOK_AND_FEEL, systemLookFeel);
                    // notify the user
                    JOptionPane.showMessageDialog(JabRefGUI.getMainFrame(),
                            Localization
                                    .lang("Unable to find the requested look and feel and thus the default one is used."),
                            Localization.lang("Warning"), JOptionPane.WARNING_MESSAGE);
                    LOGGER.warn("Unable to find requested look and feel", e);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Look and feel could not be set", e);
        }

        // In JabRef v2.8, we did it only on NON-Mac. Now, we try on all platforms
        boolean overrideDefaultFonts = Globals.prefs.getBoolean(JabRefPreferences.OVERRIDE_DEFAULT_FONTS);
        if (overrideDefaultFonts) {
            int fontSize = Globals.prefs.getInt(JabRefPreferences.MENU_FONT_SIZE);
            UIDefaults defaults = UIManager.getDefaults();
            Enumeration<Object> keys = defaults.keys();
            for (Object key : Collections.list(keys)) {
                if ((key instanceof String) && ((String) key).endsWith(".font")) {
                    FontUIResource font = (FontUIResource) UIManager.get(key);
                    font = new FontUIResource(font.getName(), font.getStyle(), fontSize);
                    defaults.put(key, font);
                }
            }
        }
    }

    public static JabRefFrame getMainFrame() {
        return mainFrame;
    }

    // Only used for testing, other than that do NOT set the mainFrame...
    public static void setMainFrame(JabRefFrame mainFrame) {
        JabRefGUI.mainFrame = mainFrame;
    }

}
