package org.jabref.gui.preferences;

import java.util.Optional;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Line;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.actions.ActionFactory;
import org.jabref.gui.actions.StandardActions;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.remote.JabRefMessageHandler;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.journals.JournalAbbreviationPreferences;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.remote.RemotePreferences;
import org.jabref.logic.remote.RemoteUtil;
import org.jabref.preferences.JabRefPreferences;

class AdvancedTab extends Pane implements PrefsTab {

    private final JabRefPreferences preferences;
    private final CheckBox useRemoteServer;
    private final CheckBox useIEEEAbrv;
    private final TextField remoteServerPort;
    private final CheckBox useCaseKeeperOnSearch;
    private final CheckBox useUnitFormatterOnSearch;
    private final GridPane builder = new GridPane();

    private final RemotePreferences remotePreferences;
    private final DialogService dialogService;

    public AdvancedTab(DialogService dialogService, JabRefPreferences prefs) {
        this.dialogService = dialogService;
        preferences = prefs;
        remotePreferences = prefs.getRemotePreferences();

        useRemoteServer = new CheckBox(Localization.lang("Listen for remote operation on port") + ':');
        useIEEEAbrv = new CheckBox(Localization.lang("Use IEEE LaTeX abbreviations"));
        remoteServerPort = new TextField();
        useCaseKeeperOnSearch = new CheckBox(Localization.lang("Add {} to specified title words on search to keep the correct case"));
        useUnitFormatterOnSearch = new CheckBox(Localization.lang("Format units by adding non-breaking separators and keeping the correct case on search"));

        Label remoteOperation = new Label(Localization.lang("Remote operation"));
        remoteOperation.getStyleClass().add("sectionHeader");
        builder.add(remoteOperation, 2, 1);
        builder.add(new Separator(), 2, 1);
        builder.add(new Pane(), 1, 2);
        Label label1 = new Label(Localization.lang("This feature lets new files be opened or imported into an "

                + "already running instance of JabRef<BR>instead of opening a new instance. For instance, this "

                + "is useful when you open a file in JabRef<br>from your web browser."

                + "<BR>Note that this will prevent you from running more than one instance of JabRef at a time."));
        label1.setVisible(false);
        builder.add(label1, 2, 22);

        Label textLabel1 = new Label("    This feature lets new files be opened or imported into an already running instance of JabRef instead of opening a new instance. For");
        builder.add(textLabel1, 2, 3);
        Label textLabel2 = new Label("instance, this is useful when you open a file in JabRef from your web browser. ");
        builder.add(textLabel2, 2, 4);
        Label textLabel3 = new Label("    Note that this will prevent you from running more than one instance of JabRef at a time.");
        builder.add(textLabel3, 2, 5);
        builder.add(new Line(), 2, 6);
        builder.add(new Pane(), 2, 7);

        HBox p = new HBox();
        p.getChildren().add(useRemoteServer);
        p.getChildren().add(remoteServerPort);

        ActionFactory factory = new ActionFactory(preferences.getKeyBindingRepository());
        Button help = factory.createIconButton(StandardActions.HELP, new HelpAction(HelpFile.REMOTE).getCommand());
        help.setMaxWidth(Double.MAX_VALUE);
        p.getChildren().add(help);

        builder.add(p, 2, 9);
        builder.add(new Label(""), 1, 10);

        Label explore = new Label(Localization.lang("Search %0", "IEEEXplore"));
        explore.getStyleClass().add("sectionHeader");
        builder.add(explore, 2, 11);
        builder.add(new Separator(), 2, 11);
        builder.add(new Pane(), 2, 12);
        builder.add(useIEEEAbrv, 2, 13);

        builder.add(new Line(), 2, 16);
        builder.add(new Label(""), 1, 17);

        Label importConversions = new Label(Localization.lang("Import conversions"));
        importConversions.getStyleClass().add("sectionHeader");
        builder.add(importConversions, 2, 18);

        builder.add(useCaseKeeperOnSearch, 2, 19);
        builder.add(new Pane(), 2, 20);
        builder.add(useUnitFormatterOnSearch, 2, 21);

    }

    @Override
    public Node getBuilder() {
        return builder;
    }

    @Override
    public void setValues() {
        useRemoteServer.setSelected(remotePreferences.useRemoteServer());
        remoteServerPort.setText(String.valueOf(remotePreferences.getPort()));
        useIEEEAbrv.setSelected(Globals.prefs.getJournalAbbreviationPreferences().useIEEEAbbreviations());
        useCaseKeeperOnSearch.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH));
        useUnitFormatterOnSearch.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH));
    }

    @Override
    public void storeSettings() {
        JournalAbbreviationPreferences journalAbbreviationPreferences = Globals.prefs.getJournalAbbreviationPreferences();
        if (journalAbbreviationPreferences.useIEEEAbbreviations() != useIEEEAbrv.isSelected()) {
            journalAbbreviationPreferences.setUseIEEEAbbreviations(useIEEEAbrv.isSelected());
            Globals.prefs.storeJournalAbbreviationPreferences(journalAbbreviationPreferences);
            Globals.journalAbbreviationLoader.update(journalAbbreviationPreferences);
        }
        storeRemoteSettings();

        preferences.putBoolean(JabRefPreferences.USE_CASE_KEEPER_ON_SEARCH, useCaseKeeperOnSearch.isSelected());
        preferences.putBoolean(JabRefPreferences.USE_UNIT_FORMATTER_ON_SEARCH, useUnitFormatterOnSearch.isSelected());
    }

    private void storeRemoteSettings() {
        getPortAsInt().ifPresent(newPort -> {
            if (remotePreferences.isDifferentPort(newPort)) {
                remotePreferences.setPort(newPort);

                if (remotePreferences.useRemoteServer()) {

                    DefaultTaskExecutor.runInJavaFXThread(() -> dialogService.showWarningDialogAndWait(Localization.lang("Remote server port"),
                            Localization.lang("Remote server port")
                                    .concat(" ")
                                    .concat(Localization.lang("You must restart JabRef for this to come into effect."))));

                }
            }
        });

        remotePreferences.setUseRemoteServer(useRemoteServer.isSelected());
        if (remotePreferences.useRemoteServer()) {
            Globals.REMOTE_LISTENER.openAndStart(new JabRefMessageHandler(), remotePreferences.getPort());
        } else {
            Globals.REMOTE_LISTENER.stop();
        }
        preferences.setRemotePreferences(remotePreferences);
    }

    private Optional<Integer> getPortAsInt() {
        try {
            return Optional.of(Integer.parseInt(remoteServerPort.getText()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean validateSettings() {
        try {
            int portNumber = Integer.parseInt(remoteServerPort.getText());
            if (RemoteUtil.isUserPort(portNumber)) {
                return true;
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {

            DefaultTaskExecutor.runInJavaFXThread(()-> dialogService.showErrorDialogAndWait(Localization.lang("Remote server port"),
                    Localization.lang("You must enter an integer value in the interval 1025-65535 in the text field for")
                            + " '" + Localization.lang("Remote server port") + '\''));

            return false;
        }
    }

    @Override
    public String getTabName() {
        return Localization.lang("Advanced");
    }
}
