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
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.error.MessagePriority;
import net.sf.jabref.logic.error.ObservableMessageWithPriority;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.ObservableMessages;
import net.sf.jabref.logic.util.BuildInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ErrorConsoleViewModel {

    private static final Log LOGGER = LogFactory.getLog(ErrorConsoleViewModel.class);
    private final String REPORTISSUEURL = "https://github.com/JabRef/jabref/issues/new";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final Date date = new Date();

    // handler for copy of Log Entry in the List by click of Copy Log Button
    public void copyLog() {
        ObservableList<ObservableMessageWithPriority> masterData = ObservableMessages.INSTANCE.messagesPropety();
        masterData.forEach(message -> message.setIsFiltered(message.getPriority() != MessagePriority.LOW));
        FilteredList<ObservableMessageWithPriority> filteredLowPriorityList = new FilteredList<>(masterData, t -> !t.isFilteredProperty().get());
        String logContentCopy = "";

        for (ObservableMessageWithPriority message : filteredLowPriorityList) {
            logContentCopy += message.getMessage() + System.lineSeparator();
        }
        new ClipBoardManager().setClipboardContents(logContentCopy);
        JabRefGUI.getMainFrame().output(Localization.lang("Log is copied"));
    }

    // handler for report Issues on GitHub by click of Report Issue Button
    public void reportIssue() {
        try {
            String info = String.format("JabRef %s%n%s %s %s %nJava %s\n\n", Globals.BUILD_INFO.getVersion(), BuildInfo.OS,
                    BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION);
            String issueTitle = "?title=" + URLEncoder.encode("Automatic Bug Report-" + dateFormat.format(date), "UTF-8");
            String issueBody = "&body=" + URLEncoder.encode(info, "UTF-8");
            JabRefGUI.getMainFrame().output(Localization.lang("Issue on GitHub successfully reported."));
            FXDialogs.showInformationDialogAndWait("Report Issue successful",
                    "Your issue was reported in your browser." + "\n\n" +
                            "The log and exception information was copied to your clipboard." + "\n\n" +
                            "Please paste this information (with Ctrl+V) in the issue description.");
            JabRefDesktop.openBrowser(REPORTISSUEURL + issueTitle + issueBody);
            //get contents of listview
            ObservableList<ObservableMessageWithPriority> masterData = ObservableMessages.INSTANCE.messagesPropety();
            String listViewContent = "";
            for (ObservableMessageWithPriority message : masterData) {
                listViewContent += message.getMessage() + System.lineSeparator();
            }
            // format the contents of listview in Issue Description
            String issueDetails = ("<details>\n" + "<summary>" + "Detail information:" + "</summary>\n```\n" + listViewContent + "\n```\n</details>");
            new ClipBoardManager().setClipboardContents(issueDetails);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }
}
