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
package net.sf.jabref;

import java.awt.Font;
import java.io.File;
import java.net.Authenticator;
import java.nio.charset.Charset;
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

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBluer;
import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.exporter.AutoSaveManager;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.ParserResultWarningDialog;
import net.sf.jabref.gui.remote.JabRefMessageHandler;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.importer.AutosaveStartupPrompter;
import net.sf.jabref.importer.OpenDatabaseAction;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.ProxyAuthenticator;
import net.sf.jabref.logic.net.ProxyPreferences;
import net.sf.jabref.logic.net.ProxyRegisterer;
import net.sf.jabref.logic.preferences.LastFocusedTabPreferences;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.logic.remote.client.RemoteListenerClient;
import net.sf.jabref.logic.util.io.FileBasedLock;
import net.sf.jabref.migrations.PreferencesMigrations;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JabRef Main Class - The application gets started here.
 */
public class JabRef {
    private static final Log LOGGER = LogFactory.getLog(JabRef.class);

    public static JabRefFrame mainFrame;

    public void start(String[] args) {
        JabRefPreferences preferences = JabRefPreferences.getInstance();

        ProxyPreferences proxyPreferences = ProxyPreferences.loadFromPreferences(preferences);
        ProxyRegisterer.register(proxyPreferences);
        if (proxyPreferences.isUseProxy() && proxyPreferences.isUseAuthentication()) {
            Authenticator.setDefault(new ProxyAuthenticator());
        }

        Globals.startBackgroundTasks();
        Globals.prefs = preferences;
        Localization.setLanguage(preferences.get(JabRefPreferences.LANGUAGE));
        Globals.prefs.setLanguageDependentDefaultValues();

        // Update which fields should be treated as numeric, based on preferences:
        InternalBibtexFields.setNumericFieldsFromPrefs();

        /* Build list of Import and Export formats */
        Globals.IMPORT_FORMAT_READER.resetImportFormats();
        CustomEntryTypesManager.loadCustomEntryTypes(preferences);
        ExportFormats.initAllExports();

        // Read list(s) of journal names and abbreviations
        Globals.journalAbbreviationLoader = new JournalAbbreviationLoader(Globals.prefs);

        // Check for running JabRef
        RemotePreferences remotePreferences = new RemotePreferences(Globals.prefs);
        if (remotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.open(new JabRefMessageHandler(), remotePreferences.getPort());

            if (!Globals.REMOTE_LISTENER.isOpen()) {
                // we are not alone, there is already a server out there, try to contact already running JabRef:
                if (RemoteListenerClient.sendToActiveJabRefInstance(args, remotePreferences.getPort())) {
                    // We have successfully sent our command line options through the socket to another JabRef instance.
                    // So we assume it's all taken care of, and quit.
                    System.out.println(Localization.lang("Arguments passed on to running JabRef instance. Shutting down."));
                    JabRefExecutorService.INSTANCE.shutdownEverything();
                    return;
                }
            }
            // we are alone, we start the server
            Globals.REMOTE_LISTENER.start();
        }

        // override used newline character with the one stored in the preferences
        // The preferences return the system newline character sequence as default
        Globals.NEWLINE = Globals.prefs.get(JabRefPreferences.NEWLINE);

        ArgumentProcessor argumentProcessor = new ArgumentProcessor(args, true);

        if ((!(argumentProcessor.hasParserResults())) || argumentProcessor.shouldShutDown()) {
            JabRefExecutorService.INSTANCE.shutdownEverything();
            return;
        }

        SwingUtilities.invokeLater(() -> openWindow(argumentProcessor));
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
                    LOGGER.warn("There seem to be problems with OpenJDK and the default GTK Look&Feel. Using Metal L&F instead. Change to another L&F with caution.");
                } else {
                    lookFeel = systemLookFeel;
                }
            } else {
                lookFeel = Globals.prefs.get(JabRefPreferences.WIN_LOOK_AND_FEEL);
            }

            // FIXME: Open JDK problem
            if (UIManager.getCrossPlatformLookAndFeelClassName().equals(lookFeel) && !System.getProperty("java.runtime.name").contains("OpenJDK")) {
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
                    JOptionPane.showMessageDialog(JabRef.mainFrame,
                            Localization
                                    .lang("Unable to find the requested Look & Feel and thus the default one is used."),
                            Localization.lang("Warning"), JOptionPane.WARNING_MESSAGE);
                    LOGGER.warn("Unable to find requested Look and Feel", e);
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

    private void openWindow(ArgumentProcessor argumentProcessor) {
        // Perform checks and changes for users with a preference set from an older JabRef version.
        PreferencesMigrations.replaceAbstractField();
        PreferencesMigrations.upgradeSortOrder();
        PreferencesMigrations.upgradeFaultyEncodingStrings();

        // This property is set to make the Mac OSX Java VM move the menu bar to
        // the top of the screen, where Mac users expect it to be.
        System.setProperty("apple.laf.useScreenMenuBar", "true");

        // Set antialiasing on everywhere. This only works in JRE >= 1.5.
        // Or... it doesn't work, period.
        // TODO test and maybe remove this! I found this commented out with no additional info ( payload@lavabit.com )
        // Enabled since JabRef 2.11 beta 4
        System.setProperty("swing.aatext", "true");
        // Default is "on".
        // "lcd" instead of "on" because of http://wiki.netbeans.org/FaqFontRendering and http://docs.oracle.com/javase/6/docs/technotes/guides/2d/flags.html#aaFonts
        System.setProperty("awt.useSystemAAFontSettings", "lcd");

        // Look & Feel. This MUST be the first thing to do before loading any Swing-specific code!
        setLookAndFeel();

        if (!argumentProcessor.hasParserResults()) {
            return; // Should never happen as openWindow will never be called then
        }
        List<ParserResult> loaded = argumentProcessor.getParserResults().get();
        // If the option is enabled, open the last edited databases, if any.
        if (!argumentProcessor.isBlank() && Globals.prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)
                && (Globals.prefs.get(JabRefPreferences.LAST_EDITED) != null)) {
            // How to handle errors in the databases to open?
            List<String> names = Globals.prefs.getStringList(JabRefPreferences.LAST_EDITED);
            lastEdLoop:
            for (String name : names) {
                File fileToOpen = new File(name);

                for (ParserResult pr : loaded) {
                    if ((pr.getFile() != null) && pr.getFile().equals(fileToOpen)) {
                        continue lastEdLoop;
                    }
                }

                if (fileToOpen.exists()) {
                    ParserResult pr = JabRef.openBibFile(name, false);

                    if (pr != null) { // TODO: Double check that pr is never null here and remove check

                        if (pr == ParserResult.INVALID_FORMAT) { // TODO: INVALID_FORMAT and FILE_LOCKED are the same...
                            System.out.println(
                                    Localization.lang("Error opening file") + " '" + fileToOpen.getPath() + "'");
                        } else if (pr != ParserResult.FILE_LOCKED) {
                            loaded.add(pr);
                        }

                    }
                }
            }
        }

        GUIGlobals.init();
        GUIGlobals.currentFont = new Font(Globals.prefs.get(JabRefPreferences.FONT_FAMILY),
                Globals.prefs.getInt(JabRefPreferences.FONT_STYLE), Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));

        LOGGER.debug("Initializing frame");
        JabRef.mainFrame = new JabRefFrame(this);

        // Add all loaded databases to the frame:
        boolean first = true;
        List<File> postponed = new ArrayList<>();
        List<ParserResult> failed = new ArrayList<>();
        List<ParserResult> toOpenTab = new ArrayList<>();
        if (!loaded.isEmpty()) {
            for (Iterator<ParserResult> i = loaded.iterator(); i.hasNext();) {
                ParserResult pr = i.next();

                if (new LastFocusedTabPreferences(Globals.prefs).hadLastFocus(pr.getFile())) {
                    first = true;
                }

                if (pr.isInvalid()) {
                    failed.add(pr);
                    i.remove();
                } else if (!pr.isPostponedAutosaveFound()) {
                    if (pr.toOpenTab()) {
                        // things to be appended to an opened tab should be done after opening all tabs
                        // add them to the list
                        toOpenTab.add(pr);
                    } else {
                        JabRef.mainFrame.addParserResult(pr, first);
                        first = false;
                    }
                } else {
                    i.remove();
                    postponed.add(pr.getFile());
                }
            }
        }

        // finally add things to the currently opened tab
        for (ParserResult pr : toOpenTab) {
            JabRef.mainFrame.addParserResult(pr, first);
            first = false;
        }

        // Start auto save timer:
        if (Globals.prefs.getBoolean(JabRefPreferences.AUTO_SAVE)) {
            Globals.startAutoSaveManager(JabRef.mainFrame);
        }

        // If we are set to remember the window location, we also remember the maximised
        // state. This needs to be set after the window has been made visible, so we
        // do it here:
        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            JabRef.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        JabRef.mainFrame.setVisible(true);

        if (Globals.prefs.getBoolean(JabRefPreferences.WINDOW_MAXIMISED)) {
            JabRef.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        for (ParserResult pr : failed) {
            String message = "<html>" + Localization.lang("Error opening file '%0'.", pr.getFile().getName()) + "<p>"
                    + pr.getErrorMessage() + "</html>";

            JOptionPane.showMessageDialog(JabRef.mainFrame, message, Localization.lang("Error opening file"),
                    JOptionPane.ERROR_MESSAGE);
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP)) {
            int i = 0;
            for (ParserResult pr : loaded) {
                ParserResultWarningDialog.showParserResultWarningDialog(pr, mainFrame, i++);
            }
        }

        // After adding the databases, go through each and see if
        // any post open actions need to be done. For instance, checking
        // if we found new entry types that can be imported, or checking
        // if the database contents should be modified due to new features
        // in this version of JabRef.
        // Note that we have to check whether i does not go over getBasePanelCount().
        // This is because importToOpen might have been used, which adds to
        // loaded, but not to getBasePanelCount()

        for (int i = 0; (i < loaded.size()) && (i < JabRef.mainFrame.getBasePanelCount()); i++) {
            ParserResult pr = loaded.get(i);
            BasePanel panel = JabRef.mainFrame.getBasePanelAt(i);
            OpenDatabaseAction.performPostOpenActions(panel, pr, true);
        }

        LOGGER.debug("Finished adding panels");

        // If any database loading was postponed due to an autosave, schedule them
        // for handing now:
        if (!postponed.isEmpty()) {
            AutosaveStartupPrompter asp = new AutosaveStartupPrompter(JabRef.mainFrame, postponed);
            SwingUtilities.invokeLater(asp);
        }

        if (!loaded.isEmpty()) {
            new FocusRequester(JabRef.mainFrame.getCurrentBasePanel().mainTable);
        }
    }

    public static ParserResult openBibFile(String name, boolean ignoreAutosave) {
        // String in OpenDatabaseAction.java
        LOGGER.info("Opening: " + name);
        File file = new File(name);
        if (!file.exists()) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            System.err.println(Localization.lang("Error") + ": " + Localization.lang("File not found"));
            return pr;

        }
        try {

            if (!ignoreAutosave) {
                boolean autoSaveFound = AutoSaveManager.newerAutoSaveExists(file);
                if (autoSaveFound) {
                    // We have found a newer autosave. Make a note of this, so it can be
                    // handled after startup:
                    ParserResult postp = new ParserResult(null, null, null);
                    postp.setPostponedAutosaveFound(true);
                    postp.setFile(file);
                    return postp;
                }
            }

            if (!FileBasedLock.waitForFileLock(file, 10)) {
                System.out.println(Localization.lang("Error opening file") + " '" + name + "'. "
                        + "File is locked by another JabRef instance.");
                return ParserResult.FILE_LOCKED;
            }

            Charset encoding = Globals.prefs.getDefaultEncoding();
            ParserResult pr = OpenDatabaseAction.loadDatabase(file, encoding);
            if (pr == null) { // TODO: double-check that pr is never null here and remove code
                pr = new ParserResult(null, null, null);
                pr.setFile(file);
                pr.setInvalid(true);
                return pr;

            }
            pr.setFile(file);
            if (pr.hasWarnings()) {
                for (String aWarn : pr.warnings()) {
                    LOGGER.warn(aWarn);
                }

            }
            return pr;
        } catch (Throwable ex) {
            ParserResult pr = new ParserResult(null, null, null);
            pr.setFile(file);
            pr.setInvalid(true);
            pr.setErrorMessage(ex.getMessage());
            LOGGER.info("Problem opening .bib-file", ex);
            return pr;
        }

    }
}
