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

import java.net.Authenticator;

import javax.swing.SwingUtilities;

import net.sf.jabref.bibtex.InternalBibtexFields;
import net.sf.jabref.cli.ArgumentProcessor;
import net.sf.jabref.exporter.ExportFormats;
import net.sf.jabref.gui.remote.JabRefMessageHandler;
import net.sf.jabref.logic.CustomEntryTypesManager;
import net.sf.jabref.logic.journals.JournalAbbreviationLoader;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.net.ProxyAuthenticator;
import net.sf.jabref.logic.net.ProxyPreferences;
import net.sf.jabref.logic.net.ProxyRegisterer;
import net.sf.jabref.logic.remote.RemotePreferences;
import net.sf.jabref.logic.remote.client.RemoteListenerClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * JabRef MainClass
 */
public class JabRefMain {

    private static final Log LOGGER = LogFactory.getLog(JabRefMain.class);


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> start(args));
    }



    private static void start(String[] args) {
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
                    LOGGER.info(Localization.lang("Arguments passed on to running JabRef instance. Shutting down."));
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

        // Process arguments
        ArgumentProcessor argumentProcessor = new ArgumentProcessor(args, ArgumentProcessor.Mode.INITIAL_START);

        // See if we should shut down now
        if (argumentProcessor.shouldShutDown()) {
            JabRefExecutorService.INSTANCE.shutdownEverything();
            return;
        }

        // If not, start GUI
        SwingUtilities
                .invokeLater(() -> new JabRefGUI(argumentProcessor.getParserResults(),
                        argumentProcessor.isBlank()));
    }

}
