/*  Copyright (C) 2016 JabRef contributors.
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
package net.sf.jabref.gui.errorconsole;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.collections.ObservableList;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.error.LogMessageWithPriority;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.LogMessage;
import net.sf.jabref.logic.util.BuildInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;

public class ErrorConsoleViewModel {

    private static final Log LOGGER = LogFactory.getLog(ErrorConsoleViewModel.class);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final Date date = new Date();

    /**
     * Handler for copy of Log Entry in clipboard by click of Copy Log Button
     */
    public void copyLog() {
        ObservableList<LogMessageWithPriority> masterData = LogMessage.getInstance().messagesProperty();
        StringBuilder logContentCopy = new StringBuilder();

        for (LogMessageWithPriority message : masterData) {
            logContentCopy.append(message.getMessage() + System.lineSeparator());
        }
        new ClipBoardManager().setClipboardContents(logContentCopy.toString());
        JabRefGUI.getMainFrame().output(Localization.lang("Log copied to clipboard."));
    }

    /**
     * Handler for report Issues on GitHub by click of Report Issue Button
     */
    public void reportIssue() {
        try {
            String issueTitle = "Automatic Bug Report-" + dateFormat.format(date);
            String issueBody = String.format("JabRef %s%n%s %s %s %nJava %s\n\n", Globals.BUILD_INFO.getVersion(), BuildInfo.OS,
                    BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION);
            JabRefGUI.getMainFrame().output(Localization.lang("Issue on GitHub successfully reported."));
            FXDialogs.showInformationDialogAndWait("Issue report successful",
                    "Your issue was reported in your browser." + "\n\n" +
                            "The log and exception information was copied to your clipboard." + "\n\n" +
                            "Please paste this information (with Ctrl+V) in the issue description.");
            URIBuilder uriBuilder = new URIBuilder().setScheme("https").setHost("github.com").setPath("/JabRef/jabref/issues/new")
                    .setParameter("title", issueTitle).setParameter("body", issueBody);
            JabRefDesktop.openBrowser(uriBuilder.build().toString());
            // Get contents of listview
            ObservableList<LogMessageWithPriority> masterData = LogMessage.getInstance().messagesProperty();
            String listViewContent = "";
            for (LogMessageWithPriority message : masterData) {
                listViewContent += message.getMessage() + System.lineSeparator();
            }
            // Format the contents of listview in Issue Description
            String issueDetails = ("<details>\n" + "<summary>" + "Detail information:" + "</summary>\n```\n" + listViewContent + "\n```\n</details>");
            new ClipBoardManager().setClipboardContents(issueDetails);
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (URISyntaxException e) {
            LOGGER.error(e);
        }

    }
}
