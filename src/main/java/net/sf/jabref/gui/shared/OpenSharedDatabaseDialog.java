package net.sf.jabref.gui.shared;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.SQLException;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import net.sf.jabref.BibDatabaseContext;
import net.sf.jabref.Defaults;
import net.sf.jabref.Globals;
import net.sf.jabref.JabRefException;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabaseMode;
import net.sf.jabref.model.database.DatabaseLocation;
import net.sf.jabref.shared.DBMSConnectionProperties;
import net.sf.jabref.shared.DBMSConnector;
import net.sf.jabref.shared.DBMSType;
import net.sf.jabref.shared.exception.DatabaseNotSupportedException;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class OpenSharedDatabaseDialog extends JDialog {

    private final JabRefFrame frame;

    private final GridBagLayout gridBagLayout = new GridBagLayout();
    private final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    private final JPanel connectionPanel = new JPanel();
    private final JPanel buttonPanel = new JPanel();

    private final JLabel databaseTypeLabel = new JLabel(Localization.lang("Database type") + ":");
    private final JLabel hostPortLabel = new JLabel(Localization.lang("Host") + "/" + Localization.lang("Port") + ":");
    private final JLabel databaseLabel = new JLabel(Localization.lang("Database") + ":");
    private final JLabel userLabel = new JLabel(Localization.lang("User") + ":");
    private final JLabel passwordLabel = new JLabel(Localization.lang("Password") + ":");

    private final JTextField hostField = new JTextField(12);
    private final JTextField portField = new JTextField(4);
    private final JTextField userField = new JTextField(14);
    private final JTextField databaseField = new JTextField(14);

    private final JPasswordField passwordField = new JPasswordField(14);
    private final JComboBox<DBMSType> dbmsTypeDropDown = new JComboBox<>();

    private final JButton connectButton = new JButton(Localization.lang("Connect"));
    private final JButton cancelButton = new JButton(Localization.lang("Cancel"));

    private static final String SHARED_DATABASE_TYPE = "sharedDatabaseType";
    private static final String SHARED_DATABASE_HOST = "sharedDatabaseHost";
    private static final String SHARED_DATABASE_PORT = "sharedDatabasePort";
    private static final String SHARED_DATABASE_NAME = "sharedDatabaseName";
    private static final String SHARED_DATABASE_USER = "sharedDatabaseUser";

    private DBMSConnectionProperties connectionProperties;
    private BibDatabaseContext bibDatabaseContext;

    /**
     * @param frame the JabRef Frame
     */
    public OpenSharedDatabaseDialog(JabRefFrame frame) {
        super(frame, Localization.lang("Open shared database"));
        this.frame = frame;
        initLayout();
        applyGlobalPrefs();
        setupActions();
        pack();
        setLocationRelativeTo(frame);
    }

    public void openSharedDatabase() {
        try {
            bibDatabaseContext.getDBSynchronizer().openSharedDatabase(connectionProperties);
            frame.addTab(bibDatabaseContext, true);
            setGlobalPrefs();
            bibDatabaseContext.getDBSynchronizer().registerListener(new SharedDatabaseUIManager(frame));
            frame.output(Localization.lang("Connection_to_%0_server_stablished.", connectionProperties.getType().toString()));
            dispose();
        } catch (ClassNotFoundException exception) {
            JOptionPane.showMessageDialog(OpenSharedDatabaseDialog.this, exception.getMessage(),
                    Localization.lang("Driver error"), JOptionPane.ERROR_MESSAGE);
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(OpenSharedDatabaseDialog.this, exception.getMessage(),
                    Localization.lang("Connection error"), JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseNotSupportedException exception) {
            new MigrationHelpDialog(this).setVisible(true);
        }
    }

    /**
     * Defines and sets the different actions up.
     */
    private void setupActions() {

        Action openAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    checkFields();
                    BibDatabaseMode selectedMode = Globals.prefs.getDefaultBibDatabaseMode();

                    bibDatabaseContext = new BibDatabaseContext(new Defaults(selectedMode),
                            DatabaseLocation.SHARED);

                    connectionProperties = new DBMSConnectionProperties();
                    connectionProperties.setType((DBMSType) dbmsTypeDropDown.getSelectedItem());
                    connectionProperties.setHost(hostField.getText());
                    connectionProperties.setPort(Integer.parseInt(portField.getText()));
                    connectionProperties.setDatabase(databaseField.getText());
                    connectionProperties.setUser(userField.getText());
                    connectionProperties.setPassword(new String(passwordField.getPassword())); //JPasswordField.getPassword() does not return a String, but a char array.

                    openSharedDatabase();

                } catch (JabRefException exception) {
                    JOptionPane.showMessageDialog(OpenSharedDatabaseDialog.this, exception.getMessage(),
                            Localization.lang("Warning"), JOptionPane.WARNING_MESSAGE);
                }
            }
        };
        connectButton.addActionListener(openAction);
        cancelButton.addActionListener(e -> dispose());

        /**
         * Set up a listener which updates the default port number once the selection in dbmsTypeDropDown has changed.
         */
        Action dbmsTypeDropDownAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                portField.setText(Integer.toString(((DBMSType) dbmsTypeDropDown.getSelectedItem()).getDefaultPort()));
            }
        };
        dbmsTypeDropDown.addActionListener(dbmsTypeDropDownAction);

        // Add enter button action listener
        connectButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
                "Enter_pressed");
        connectButton.getActionMap().put("Enter_pressed", openAction);

    }

    /**
     * Fetches possibly saved data and configures the control elements respectively.
     */
    private void applyGlobalPrefs() {
        Optional<String> sharedDatabaseType = Globals.prefs.getAsOptional(SHARED_DATABASE_TYPE);
        Optional<String> sharedDatabaseHost = Globals.prefs.getAsOptional(SHARED_DATABASE_HOST);
        Optional<String> sharedDatabasePort = Globals.prefs.getAsOptional(SHARED_DATABASE_PORT);
        Optional<String> sharedDatabaseName = Globals.prefs.getAsOptional(SHARED_DATABASE_NAME);
        Optional<String> sharedDatabaseUser = Globals.prefs.getAsOptional(SHARED_DATABASE_USER);

        if (sharedDatabaseType.isPresent()) {
            Optional<DBMSType> dbmsType = DBMSType.fromString(sharedDatabaseType.get());
            if (dbmsType.isPresent()) {
                dbmsTypeDropDown.setSelectedItem(dbmsType.get());
            }
        }

        if (sharedDatabaseHost.isPresent()) {
            hostField.setText(sharedDatabaseHost.get());
        }

        if (sharedDatabasePort.isPresent()) {
            portField.setText(sharedDatabasePort.get());
        } else {
            portField.setText(Integer.toString(((DBMSType) dbmsTypeDropDown.getSelectedItem()).getDefaultPort()));
        }

        if (sharedDatabaseName.isPresent()) {
            databaseField.setText(sharedDatabaseName.get());
        }

        if (sharedDatabaseUser.isPresent()) {
            userField.setText(sharedDatabaseUser.get());
        }
    }

    /**
     * Set up the layout and position the control units in their right place.
     */
    private void initLayout() {

        setResizable(false);

        Insets defautInsets = new Insets(4, 15, 4, 4);

        connectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Localization.lang("Connection")));
        connectionPanel.setLayout(gridBagLayout);

        Set<DBMSType> availableDBMSTypes = DBMSConnector.getAvailableDBMSTypes();
        DefaultComboBoxModel<DBMSType> comboBoxModel = new DefaultComboBoxModel<>(
                availableDBMSTypes.toArray(new DBMSType[availableDBMSTypes.size()]));

        dbmsTypeDropDown.setModel(comboBoxModel);

        gridBagConstraints.insets = defautInsets;
        gridBagConstraints.fill = GridBagConstraints.BOTH;
        gridBagLayout.setConstraints(connectionPanel, gridBagConstraints);

        //1. column
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        connectionPanel.add(databaseTypeLabel, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        connectionPanel.add(hostPortLabel, gridBagConstraints);

        gridBagConstraints.gridy = 2;
        connectionPanel.add(databaseLabel, gridBagConstraints);

        gridBagConstraints.gridy = 3;
        connectionPanel.add(userLabel, gridBagConstraints);

        gridBagConstraints.gridy = 4;
        connectionPanel.add(passwordLabel, gridBagConstraints);

        // 2. column
        gridBagConstraints.gridwidth = 2;

        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        connectionPanel.add(dbmsTypeDropDown, gridBagConstraints);

        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1; // the hostField is smaller than the others.
        gridBagConstraints.insets = new Insets(4, 15, 4, 0);
        connectionPanel.add(hostField, gridBagConstraints);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.insets = defautInsets;
        connectionPanel.add(databaseField, gridBagConstraints);

        gridBagConstraints.gridy = 3;
        connectionPanel.add(userField, gridBagConstraints);

        gridBagConstraints.gridy = 4;
        connectionPanel.add(passwordField, gridBagConstraints);

        // 3. column
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(4, 0, 4, 4);
        connectionPanel.add(portField, gridBagConstraints);

        gridBagConstraints.insets = new Insets(4, 4, 4, 4);

        ButtonBarBuilder bsb = new ButtonBarBuilder(buttonPanel);
        bsb.addGlue();
        bsb.addButton(connectButton);
        bsb.addRelatedGap();
        bsb.addButton(cancelButton);

        getContentPane().setLayout(gridBagLayout);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagLayout.setConstraints(connectionPanel, gridBagConstraints);
        getContentPane().add(connectionPanel);
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(10, 0, 12, 13);
        gridBagLayout.setConstraints(buttonPanel, gridBagConstraints);
        getContentPane().add(buttonPanel);

        setModal(true); // Owner window should be disabled while this dialog is opened.
    }

    /**
     * Saves the data from this dialog persistently to facilitate the usage.
     */
    public void setGlobalPrefs() {
        Globals.prefs.put(SHARED_DATABASE_TYPE, ((DBMSType) dbmsTypeDropDown.getSelectedItem()).toString());
        Globals.prefs.put(SHARED_DATABASE_HOST, hostField.getText());
        Globals.prefs.put(SHARED_DATABASE_PORT, portField.getText());
        Globals.prefs.put(SHARED_DATABASE_NAME, databaseField.getText());
        Globals.prefs.put(SHARED_DATABASE_USER, userField.getText());
    }

    private boolean isEmptyField(JTextField field) {
        return field.getText().trim().length() == 0;
    }

    /**
     * Checks every required text field for emptiness.
     */
    private void checkFields() throws JabRefException {
        if (isEmptyField(hostField)) {
            hostField.requestFocus();
            throw new JabRefException(Localization.lang("Required_field_\"%0\"_is_empty.", Localization.lang("Host")));
        }
        if (isEmptyField(portField)) {
            portField.requestFocus();
            throw new JabRefException(Localization.lang("Required_field_\"%0\"_is_empty.", Localization.lang("Port")));
        }
        if (isEmptyField(databaseField)) {
            databaseField.requestFocus();
            throw new JabRefException(
                    Localization.lang("Required_field_\"%0\"_is_empty.", Localization.lang("Database")));
        }
        if (isEmptyField(userField)) {
            userField.requestFocus();
            throw new JabRefException(Localization.lang("Required_field_\"%0\"_is_empty.", Localization.lang("User")));
        }
    }

}
