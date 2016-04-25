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

package net.sf.jabref;

import java.awt.Font;
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
import net.sf.jabref.gui.ParserResultWarningDialog;
import net.sf.jabref.gui.util.FocusRequester;
import net.sf.jabref.importer.AutosaveStartupPrompter;
import net.sf.jabref.importer.OpenDatabaseAction;
import net.sf.jabref.importer.ParserResult;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.preferences.LastFocusedTabPreferences;
import net.sf.jabref.migrations.PreferencesMigrations;

import com.jgoodies.looks.plastic.Plastic3DLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyBluer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JabRefGUI {

    private static final Log LOGGER = LogFactory.getLog(JabRefGUI.class);

    private static JabRefFrame mainFrame;

    private final List<ParserResult> loaded;
    private final boolean isBlank;

    private final List<File> postponed = new ArrayList<>();
    private final List<ParserResult> failed = new ArrayList<>();
    private final List<ParserResult> toOpenTab = new ArrayList<>();


    public JabRefGUI(List<ParserResult> loaded, boolean isBlank) {
        this.loaded = loaded;
        this.isBlank = isBlank;
        openWindow();
    }

    private void openWindow() {
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

        // If the option is enabled, open the last edited databases, if any.
        if (!isBlank && Globals.prefs.getBoolean(JabRefPreferences.OPEN_LAST_EDITED)
                && (Globals.prefs.get(JabRefPreferences.LAST_EDITED) != null)) {
            openLastEditedDatabase();
        }

        GUIGlobals.init();
        GUIGlobals.currentFont = new Font(Globals.prefs.get(JabRefPreferences.FONT_FAMILY),
                Globals.prefs.getInt(JabRefPreferences.FONT_STYLE), Globals.prefs.getInt(JabRefPreferences.FONT_SIZE));

        LOGGER.debug("Initializing frame");
        JabRefGUI.mainFrame = new JabRefFrame();

        // Add all loaded databases to the frame:
        boolean first = true;
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
                        JabRefGUI.getMainFrame().addParserResult(pr, first);
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
            String message = "<html>" + Localization.lang("Error opening file '%0'.", pr.getFile().getName()) + "<p>"
                    + pr.getErrorMessage() + "</html>";

            JOptionPane.showMessageDialog(JabRefGUI.getMainFrame(), message, Localization.lang("Error opening file"),
                    JOptionPane.ERROR_MESSAGE);
        }

        if (Globals.prefs.getBoolean(JabRefPreferences.DISPLAY_KEY_WARNING_DIALOG_AT_STARTUP)) {
            int i = 0;
            for (ParserResult pr : loaded) {
                ParserResultWarningDialog.showParserResultWarningDialog(pr, JabRefGUI.getMainFrame(), i++);
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

        for (int i = 0; (i < loaded.size()) && (i < JabRefGUI.getMainFrame().getBasePanelCount()); i++) {
            ParserResult pr = loaded.get(i);
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

        if (!loaded.isEmpty()) {
            new FocusRequester(JabRefGUI.getMainFrame().getCurrentBasePanel().mainTable);
        }
    }

    private void openLastEditedDatabase() {
        // How to handle errors in the databases to open?
        List<String> names = Globals.prefs.getStringList(JabRefPreferences.LAST_EDITED);
        lastEdLoop: for (String name : names) {
            File fileToOpen = new File(name);

            for (ParserResult pr : loaded) {
                if ((pr.getFile() != null) && pr.getFile().equals(fileToOpen)) {
                    continue lastEdLoop;
                }
            }

            if (fileToOpen.exists()) {
                ParserResult pr = OpenDatabaseAction.loadDatabaseOrAutoSave(name, false);

                if (pr.isNullResult()) {
                    LOGGER.error(Localization.lang("Error opening file") + " '" + fileToOpen.getPath() + "'");
                } else {
                    loaded.add(pr);
                }
            }
        }
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

    public static JabRefFrame getMainFrame() {
        return mainFrame;
    }

    // Only used for testing, other than that do NOT set the mainFrame...
    public static void setMainFrame(JabRefFrame mainFrame) {
        JabRefGUI.mainFrame = mainFrame;
    }

}
