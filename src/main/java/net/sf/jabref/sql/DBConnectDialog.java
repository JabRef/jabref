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
package net.sf.jabref.sql;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog box for collecting database connection strings from the user
 *
 * @author pattonlk
 */
public class DBConnectDialog extends JDialog {

    private static List<String> FORMATTED_NAMES = Arrays
            .stream(DatabaseType.values())
            .map(DatabaseType::getFormattedName)
            .collect(Collectors.toList());

    // input fields
    private final JComboBox<String> cmbServerType = new JComboBox<>();
    private final JTextField txtServerHostname = new JTextField(40);
    private final JTextField txtDatabase = new JTextField(40);
    private final JTextField txtUsername = new JTextField(40);
    private final JPasswordField pwdPassword = new JPasswordField(40);

    private DBStrings dbStrings = new DBStrings();
    private boolean connectedToDB;

    /**
     * Creates a new instance of DBConnectDialog
     */
    public DBConnectDialog(JFrame parent, DBStrings dbs) {
        super(Objects.requireNonNull(parent), Localization.lang("Connect to SQL database"), true);

        this.setResizable(false);
        this.setLocationRelativeTo(parent);

        dbStrings = Objects.requireNonNull(dbs);

        // build collections of components
        ArrayList<JLabel> lhs = new ArrayList<>();
        JLabel lblServerType = new JLabel();
        lhs.add(lblServerType);
        JLabel lblServerHostname = new JLabel();
        lhs.add(lblServerHostname);
        JLabel lblDatabase = new JLabel();
        lhs.add(lblDatabase);
        JLabel lblUsername = new JLabel();
        lhs.add(lblUsername);
        JLabel lblPassword = new JLabel();
        lhs.add(lblPassword);

        ArrayList<JComponent> rhs = new ArrayList<>();
        rhs.add(cmbServerType);
        rhs.add(txtServerHostname);
        rhs.add(txtDatabase);
        rhs.add(txtUsername);
        rhs.add(pwdPassword);

        // setup label text
        lblServerType.setText(Localization.lang("Server type") + ':');
        lblServerHostname.setText(Localization.lang("Server hostname") + ':');
        lblDatabase.setText(Localization.lang("Database") + ':');
        lblUsername.setText(Localization.lang("Username") + ':');
        lblPassword.setText(Localization.lang("Password") + ':');

        // set label text alignment
        for (JLabel label : lhs) {
            label.setHorizontalAlignment(SwingConstants.RIGHT);
        }

        // set button text
        JButton btnConnect = new JButton();
        btnConnect.setText(Localization.lang("Connect"));
        JButton btnCancel = new JButton();
        btnCancel.setText(Localization.lang("Cancel"));

        // init input fields to current DB strings
        for (String aSrv : FORMATTED_NAMES) {
            cmbServerType.addItem(aSrv);
        }

        cmbServerType.setSelectedItem(dbStrings.getDbPreferences().getServerType().getFormattedName());
        txtServerHostname.setText(dbStrings.getDbPreferences().getServerHostname());
        txtDatabase.setText(dbStrings.getDbPreferences().getDatabase());
        txtUsername.setText(dbStrings.getDbPreferences().getUsername());
        pwdPassword.setText(dbStrings.getPassword());

        // construct dialog
        FormBuilder builder = FormBuilder.create().layout(new
                FormLayout("right:pref, 4dlu, fill:pref", "pref, 4dlu, pref, 4dlu, pref, 4dlu, pref, 4dlu, pref"));

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // add labels and input fields
        builder.add(lblServerType).xy(1, 1);
        builder.add(cmbServerType).xy(3, 1);
        builder.add(lblServerHostname).xy(1, 3);
        builder.add(txtServerHostname).xy(3, 3);
        builder.add(lblDatabase).xy(1, 5);
        builder.add(txtDatabase).xy(3, 5);
        builder.add(lblUsername).xy(1, 7);
        builder.add(txtUsername).xy(3, 7);
        builder.add(lblPassword).xy(1, 9);
        builder.add(pwdPassword).xy(3, 9);

        // add the panel to the CENTER of your dialog:
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        // add buttons are added in a similar way:
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(btnConnect);
        bb.addButton(btnCancel);
        bb.addGlue();

        // add the buttons to the SOUTH of your dialog:
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        ActionListener connectAction = e -> {
            Optional<String> errorMessage = checkInput();

            if (errorMessage.isPresent()) {
                JOptionPane.showMessageDialog(null, errorMessage.get(), Localization.lang("Input error"),
                        JOptionPane.ERROR_MESSAGE);
            } else {
                storeSettings();
                setVisible(false);
                setConnectToDB(true);
            }
        };

        btnConnect.addActionListener(connectAction);
        txtDatabase.addActionListener(connectAction);
        txtServerHostname.addActionListener(connectAction);
        txtUsername.addActionListener(connectAction);
        pwdPassword.addActionListener(connectAction);

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
                setConnectToDB(false);
            }
        };
        btnCancel.addActionListener(cancelAction);

        // Key bindings:
        ActionMap am = builder.getPanel().getActionMap();
        InputMap im = builder.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);
    }

    /**
     * Checks the user input, and ensures that required fields have entries
     *
     * @return Appropriate error message to be displayed to user
     */
    private Optional<String> checkInput() {

        String[] fields = {Localization.lang("Server hostname"), Localization.lang("Database"),
                Localization.lang("Username")};
        String[] errors = new String[fields.length];
        int cnt = 0;

        if (txtServerHostname.getText().trim().isEmpty()) {
            errors[cnt] = fields[0];
            cnt++;
        }

        if (txtDatabase.getText().trim().isEmpty()) {
            errors[cnt] = fields[1];
            cnt++;
        }

        if (txtUsername.getText().trim().isEmpty()) {
            errors[cnt] = fields[2];
            cnt++;
        }

        StringBuilder errMsg = new StringBuilder(Localization.lang("Please specify the")).append(' ');

        switch (cnt) {
        case 0:
            errMsg = new StringBuilder();
            break;
        case 1:
            errMsg.append(errors[0]).append('.');
            break;
        case 2:
            errMsg.append(errors[0]).append(" and ").append(errors[1]).append('.');
            break;
        default: // Will be 3 at most
            errMsg.append(errors[0]).append(", ").append(errors[1]).append(", and ").append(errors[2]).append('.');
            break;
        }

        if (errMsg.toString().isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(errMsg.toString());
        }
    }

    /**
     * Save user input.
     */
    private void storeSettings() {
        DBStringsPreferences preferences = new DBStringsPreferences(
                cmbServerType.getSelectedItem().toString(),
                txtServerHostname.getText(),
                txtUsername.getText(),
                txtDatabase.getText());

        // Store these settings so they appear as default next time:
        preferences.storeToPreferences(Globals.prefs);

        dbStrings.setDbPreferences(preferences);

        char[] pwd = pwdPassword.getPassword();
        String tmp = new String(pwd);
        dbStrings.setPassword(tmp);
        Arrays.fill(pwd, '0');

    }

    public DBStrings getDBStrings() {
        return dbStrings;
    }

    public void setDBStrings(DBStrings dbStrings) {
        this.dbStrings = dbStrings;
    }

    public boolean isConnectedToDB() {
        return connectedToDB;
    }

    private void setConnectToDB(boolean connectToDB) {
        this.connectedToDB = connectToDB;
    }

}
