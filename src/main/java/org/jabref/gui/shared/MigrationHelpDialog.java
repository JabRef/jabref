package org.jabref.gui.shared;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

import org.jabref.gui.JabRefDialog;
import org.jabref.gui.help.HelpAction;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;

public class MigrationHelpDialog extends JabRefDialog {

    public MigrationHelpDialog(ConnectToSharedDatabaseDialog openSharedDatabaseDialog) {
        super(openSharedDatabaseDialog, Localization.lang("Migration help information"), MigrationHelpDialog.class);

        String migrationMessage = Localization
                .lang("Entered database has obsolete structure and is no longer supported.");
        JLabel migrationLabel = new JLabel(migrationMessage);
        migrationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String helpMessage = Localization.lang("Click here to learn about the migration of pre-3.6 databases.");
        JLabel helpLabel = new HelpAction(HelpFile.SQL_DATABASE_MIGRATION).getHelpLabel(helpMessage);
        helpLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        String informationMessage = Localization.lang("However, a new database was created alongside the pre-3.6 one.");
        JLabel informationLabel = new JLabel(informationMessage);
        informationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        Action openAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                openSharedDatabaseDialog.openSharedDatabase();
            }
        };

        JButton okButton = new JButton(Localization.lang("OK"));
        okButton.addActionListener(openAction);
        okButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        okButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "Enter_pressed");
        okButton.getActionMap().put("Enter_pressed", openAction);

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(okButton, BorderLayout.CENTER);

        JPanel contentPanel = new JPanel();
        contentPanel.setBorder(new EmptyBorder(9, 9, 9, 9));
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(migrationLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(helpLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        contentPanel.add(informationLabel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        contentPanel.add(buttonPanel);

        add(contentPanel);

        setResizable(false);
        pack();
        setLocationRelativeTo(openSharedDatabaseDialog);
    }
}
