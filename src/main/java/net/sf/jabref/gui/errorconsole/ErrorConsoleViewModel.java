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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ToggleButton;
import javafx.util.Callback;

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

public class ErrorConsoleViewModel {

    private BooleanProperty developerInformation = new SimpleBooleanProperty();
    private final String CREATEISSUEURL = "https://github.com/JabRef/jabref/issues/new";
    private final DateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
    private final Date date = new Date();

    /**
     * Create allMessage ListView, which shows the filtered entries (default at first only the Log entries),
     * when the ToggleButton "developerButton" is disable.
     * If ToggleButton "developerButton" is enable, then it should show all entries
     * @param allMessage
     * @param developerButton (default is disable at start)
     */

    public void setUpListView(ListView allMessage, ToggleButton developerButton) {
        ObservableList<ObservableMessageWithPriority> masterData = ObservableMessages.INSTANCE.messagesPropety();

        FilteredList<ObservableMessageWithPriority> filteredList = new FilteredList<>(masterData, t -> !t.isFilteredProperty().get());
        allMessage.setItems(filteredList);

        developerInformation.bind(developerButton.selectedProperty());
        developerInformation.addListener((observable, oldValue, newValue) -> {
            if (newValue) {
                masterData.forEach(message -> message.setIsFiltered(false));

            } else {
                masterData.forEach(message -> message.setIsFiltered(message.getPriority() != MessagePriority.LOW));
            }
        });

        // handler for listCell appearance (example for exception Cell)
        allMessage.setCellFactory(new Callback<ListView<ObservableMessageWithPriority>, ListCell<ObservableMessageWithPriority>>() {
            @Override
            public ListCell<ObservableMessageWithPriority> call(ListView<ObservableMessageWithPriority> listView) {
                return new ListCell<ObservableMessageWithPriority>() {
                    @Override
                    public void updateItem(ObservableMessageWithPriority omp, boolean empty) {
                        super.updateItem(omp, empty);
                        if (omp != null) {
                            setText(omp.getMessage());
                            getStyleClass().clear();
                            if (omp.getPriority() == MessagePriority.HIGH) {
                                if (developerInformation.getValue()) {
                                    getStyleClass().add("exception");
                                }
                            } else if (omp.getPriority() == MessagePriority.MEDIUM) {
                                if (developerInformation.getValue()) {
                                    getStyleClass().add("output");
                                }
                            } else {
                                getStyleClass().add("log");
                            }
                        } else {
                            setText("");
                        }
                    }
                };
            }
        });
    }

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

    // handler for create Issues on GitHub by click of Create Issue Button
    public void createIssue() {
        try {
            String info = String.format("JabRef %s%n%s %s %s %nJava %s\n\n", Globals.BUILD_INFO.getVersion(), BuildInfo.OS,
                    BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION);
            String issueTitle = "?title=" + URLEncoder.encode(Localization.lang("Automatic Bug Report-") + dateFormat.format(date), "UTF-8");
            String issueBody = "&body=" + URLEncoder.encode(info, "UTF-8");
            JabRefGUI.getMainFrame().output(Localization.lang("Create Issue on GitHub is successful!"));
            FXDialogs.showInformationDialogAndWait(Localization.lang("Create Issue successful!"),
                    Localization.lang("Your issue was created in your browser!") + "\n\n" +
                            Localization.lang("The log and exception information was copied to your clipboard.") + "\n\n" +
                            Localization.lang("Please paste this information (with Ctrl+V) in the issue description!"));
            JabRefDesktop.openBrowser(CREATEISSUEURL + issueTitle + issueBody);
            //get contents of listview
            ObservableList<ObservableMessageWithPriority> masterData = ObservableMessages.INSTANCE.messagesPropety();
            String listViewContent = "";
            for (ObservableMessageWithPriority message : masterData) {
                listViewContent += message.getMessage() + System.lineSeparator();
            }
            // format the contents of listview in Issue Description
            String issueDetails = ("<details>\n" + "<summary>" + Localization.lang("Detail information:") + "</summary>\n```\n" + listViewContent + "\n```\n</details>");
            new ClipBoardManager().setClipboardContents(issueDetails);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
