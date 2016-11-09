package net.sf.jabref.gui.errorconsole;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javafx.beans.property.ListProperty;

import net.sf.jabref.Globals;
import net.sf.jabref.JabRefGUI;
import net.sf.jabref.gui.ClipBoardManager;
import net.sf.jabref.gui.FXDialogs;
import net.sf.jabref.gui.desktop.JabRefDesktop;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.logging.LogMessages;
import net.sf.jabref.logic.util.BuildInfo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.utils.URIBuilder;
import org.apache.logging.log4j.core.LogEvent;

public class ErrorConsoleViewModel {

    private static final Log LOGGER = LogFactory.getLog(ErrorConsoleViewModel.class);
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final Date date = new Date();
    private ListProperty<LogEvent> allMessagesData = LogMessages.getInstance().messagesProperty();

    public ListProperty<LogEvent> allMessagesDataproperty() {
        return this.allMessagesData;
    }

    /**
     * Handler for get of log messages in listview
     *
     * @return all messages as String
     */
    private String getLogMessagesAsString() {
        StringBuilder logMessagesContent = new StringBuilder();
        for (LogEvent message : allMessagesDataproperty()) {
            logMessagesContent.append(message.getMessage().getFormattedMessage() + System.lineSeparator());
        }
        return logMessagesContent.toString();
    }

    /**
     * Handler for copy of Log Entry in clipboard by click of Copy Log Button
     */
    public void copyLog() {
        new ClipBoardManager().setClipboardContents(getLogMessagesAsString());
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
            FXDialogs.showInformationDialogAndWait(Localization.lang("Issue report successful"),
                    Localization.lang("Your issue was reported in your browser.") + "\n\n" +
                            Localization.lang("The log and exception information was copied to your clipboard.") + "\n\n" +
                            Localization.lang("Please paste this information (with Ctrl+V) in the issue description."));
            URIBuilder uriBuilder = new URIBuilder().setScheme("https").setHost("github.com").setPath("/JabRef/jabref/issues/new")
                    .setParameter("title", issueTitle).setParameter("body", issueBody);
            JabRefDesktop.openBrowser(uriBuilder.build().toString());
            // Format the contents of listview in Issue Description
            String issueDetails = ("<details>\n" + "<summary>" + "Detail information:" + "</summary>\n```\n" + getLogMessagesAsString() + "\n```\n</details>");
            new ClipBoardManager().setClipboardContents(issueDetails);
        } catch (IOException e) {
            LOGGER.error(e);
        } catch (URISyntaxException e) {
            LOGGER.error(e);
        }

    }
}
