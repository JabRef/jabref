package org.jabref.gui.shared;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.jabref.Globals;
import org.jabref.JabRefException;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.exporter.SaveDatabaseAction;
import org.jabref.gui.help.HelpAction;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.help.HelpFile;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.shared.DBMSConnection;
import org.jabref.logic.shared.DBMSConnectionProperties;
import org.jabref.logic.shared.exception.InvalidDBMSConnectionPropertiesException;
import org.jabref.logic.shared.prefs.SharedDatabasePreferences;
import org.jabref.logic.shared.security.Password;
import org.jabref.logic.util.FileType;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.shared.DBMSType;
import org.jabref.model.database.shared.DatabaseLocation;
import org.jabref.model.database.shared.DatabaseNotSupportedException;
import org.jabref.preferences.JabRefPreferences;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectToSharedDatabaseDialog extends JabRefDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectToSharedDatabaseDialog.class);

    private final JabRefFrame frame;

    private final GridBagLayout gridBagLayout = new GridBagLayout();
    private final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    private final JPanel connectionPanel = new JPanel();
    private final JPanel filePanel = new JPanel();
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
    private final JTextField fileLocationField = new JTextField(20);

    private final JPasswordField passwordField = new JPasswordField(14);
    private final JComboBox<DBMSType> dbmsTypeDropDown = new JComboBox<>();

    private final JButton connectButton = new JButton(Localization.lang("Connect"));
    private final JButton cancelButton = new JButton(Localization.lang("Cancel"));
    private final JButton browseButton = new JButton(Localization.lang("Browse"));
    private final JButton helpButton = new HelpAction(HelpFile.SQL_DATABASE).getHelpButton();

    private final JCheckBox rememberPassword = new JCheckBox(Localization.lang("Remember password?"));
    private final JCheckBox autosaveFile = new JCheckBox(Localization.lang("Automatically save the library to"));

    private final SharedDatabasePreferences prefs = new SharedDatabasePreferences();

    private DBMSConnectionProperties connectionProperties;

    /**
     * @param frame the JabRef Frame
     */
    public ConnectToSharedDatabaseDialog(JabRefFrame frame) {
        super(frame, Localization.lang("Connect to shared database"), ConnectToSharedDatabaseDialog.class);
        this.frame = frame;
        initLayout();
        updateEnableState();
        applyPreferences();
        setupActions();
        pack();
        setLocationRelativeTo(frame);
    }

    public void openSharedDatabase() {

        if (isSharedDatabaseAlreadyPresent()) {
            JOptionPane.showMessageDialog(ConnectToSharedDatabaseDialog.this,
                    Localization.lang("You are already connected to a database using entered connection details."),
                    Localization.lang("Warning"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (autosaveFile.isSelected()) {

            Path localFilePath = Paths.get(fileLocationField.getText());

            if (Files.exists(localFilePath) && !Files.isDirectory(localFilePath)) {
                int answer = JOptionPane.showConfirmDialog(this,
                        Localization.lang("'%0' exists. Overwrite file?", localFilePath.getFileName().toString()),
                        Localization.lang("Existing file"), JOptionPane.YES_NO_OPTION);
                if (answer == JOptionPane.NO_OPTION) {
                    fileLocationField.requestFocus();
                    return;
                }
            }
        }

        setLoadingConnectButtonText(true);

        try {
            BasePanel panel = new SharedDatabaseUIManager(frame).openNewSharedDatabaseTab(connectionProperties);
            setPreferences();
            dispose();
            if (!fileLocationField.getText().isEmpty()) {
                try {
                    new SaveDatabaseAction(panel, Paths.get(fileLocationField.getText())).runCommand();
                } catch (Throwable e) {
                    LOGGER.error("Error while saving the database", e);
                }
            }

            return; // setLoadingConnectButtonText(false) should not be reached regularly.
        } catch (SQLException | InvalidDBMSConnectionPropertiesException exception) {
            JOptionPane.showMessageDialog(ConnectToSharedDatabaseDialog.this, exception.getMessage(),
                    Localization.lang("Connection error"), JOptionPane.ERROR_MESSAGE);
        } catch (DatabaseNotSupportedException exception) {
            new MigrationHelpDialog(this).setVisible(true);
        }

        setLoadingConnectButtonText(false);
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

                    connectionProperties = new DBMSConnectionProperties();
                    connectionProperties.setType((DBMSType) dbmsTypeDropDown.getSelectedItem());
                    connectionProperties.setHost(hostField.getText());
                    connectionProperties.setPort(Integer.parseInt(portField.getText()));
                    connectionProperties.setDatabase(databaseField.getText());
                    connectionProperties.setUser(userField.getText());
                    connectionProperties.setPassword(new String(passwordField.getPassword())); //JPasswordField.getPassword() does not return a String, but a char array.

                    openSharedDatabase();
                } catch (JabRefException exception) {
                    JOptionPane.showMessageDialog(ConnectToSharedDatabaseDialog.this, exception.getMessage(),
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
        browseButton.addActionListener(e -> showFileChooser());
        autosaveFile.addActionListener(e -> updateEnableState());
    }

    /**
     * Fetches possibly saved data and configures the control elements respectively.
     */
    private void applyPreferences() {
        Optional<String> sharedDatabaseType = prefs.getType();
        Optional<String> sharedDatabaseHost = prefs.getHost();
        Optional<String> sharedDatabasePort = prefs.getPort();
        Optional<String> sharedDatabaseName = prefs.getName();
        Optional<String> sharedDatabaseUser = prefs.getUser();
        Optional<String> sharedDatabasePassword = prefs.getPassword();
        boolean sharedDatabaseRememberPassword = prefs.getRememberPassword();

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

        if (sharedDatabasePassword.isPresent() && sharedDatabaseUser.isPresent()) {
            try {
                passwordField.setText(
                        new Password(sharedDatabasePassword.get().toCharArray(), sharedDatabaseUser.get()).decrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not read the password due to decryption problems.", e);
            }
        }

        rememberPassword.setSelected(sharedDatabaseRememberPassword);
    }

    /**
     * Set up the layout and position the control units in their right place.
     */
    private void initLayout() {

        setResizable(false);

        Insets defautInsets = new Insets(4, 15, 4, 4);

        connectionPanel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Localization.lang("Connection")));
        connectionPanel.setLayout(gridBagLayout);

        Set<DBMSType> availableDBMSTypes = DBMSConnection.getAvailableDBMSTypes();
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

        gridBagConstraints.gridy = 5;
        connectionPanel.add(rememberPassword, gridBagConstraints);

        // 3. column
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(4, 0, 4, 4);
        connectionPanel.add(portField, gridBagConstraints);

        // help button
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.insets = new Insets(10, 10, 0, 0);
        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        helpPanel.add(helpButton);

        // add panel
        getContentPane().setLayout(gridBagLayout);
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagLayout.setConstraints(connectionPanel, gridBagConstraints);
        getContentPane().add(connectionPanel);

        // filePanel
        filePanel.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), Localization.lang("File")));
        filePanel.setLayout(gridBagLayout);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;

        filePanel.add(autosaveFile, gridBagConstraints);
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 1;
        filePanel.add(fileLocationField, gridBagConstraints);
        gridBagConstraints.gridx = 2;
        filePanel.add(browseButton, gridBagConstraints);

        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagLayout.setConstraints(filePanel, gridBagConstraints);
        getContentPane().add(filePanel);

        // buttonPanel
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

        buttonPanel.add(connectButton);
        buttonPanel.add(cancelButton);
        buttonPanel.add(helpPanel);

        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new Insets(5, 5, 5, 5);
        gridBagLayout.setConstraints(buttonPanel, gridBagConstraints);
        getContentPane().add(buttonPanel);

        setModal(true); // Owner window should be disabled while this dialog is opened.
    }

    /**
     * Saves the data from this dialog persistently to facilitate the usage.
     */
    private void setPreferences() {
        prefs.setType(dbmsTypeDropDown.getSelectedItem().toString());
        prefs.setHost(hostField.getText());
        prefs.setPort(portField.getText());
        prefs.setName(databaseField.getText());
        prefs.setUser(userField.getText());

        if (rememberPassword.isSelected()) {
            try {
                prefs.setPassword(new Password(passwordField.getPassword(), userField.getText()).encrypt());
            } catch (GeneralSecurityException | UnsupportedEncodingException e) {
                LOGGER.error("Could not store the password due to encryption problems.", e);
            }
        } else {
            prefs.clearPassword(); // for the case that the password is already set
        }

        prefs.setRememberPassword(rememberPassword.isSelected());
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
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Host")));
        }
        if (isEmptyField(portField)) {
            portField.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("Port")));
        }
        if (isEmptyField(databaseField)) {
            databaseField.requestFocus();
            throw new JabRefException(
                    Localization.lang("Required field \"%0\" is empty.", Localization.lang("Library")));
        }
        if (isEmptyField(userField)) {
            userField.requestFocus();
            throw new JabRefException(Localization.lang("Required field \"%0\" is empty.", Localization.lang("User")));
        }
        if (autosaveFile.isSelected() && isEmptyField(fileLocationField)) {
            fileLocationField.requestFocus();
            throw new JabRefException(Localization.lang("Please enter a valid file path."));
        }
    }

    /**
     * Sets the connectButton according to the current connection state.
     */
    private void setLoadingConnectButtonText(boolean isLoading) {
        connectButton.setEnabled(!isLoading);
        if (isLoading) {
            connectButton.setText(Localization.lang("Connecting..."));
        } else {
            connectButton.setText(Localization.lang("Connect"));
        }
    }

    /**
     * Checks whether a database with the given @link {@link DBMSConnectionProperties} is already opened.
     */
    private boolean isSharedDatabaseAlreadyPresent() {
        List<BasePanel> panels = JabRefGUI.getMainFrame().getBasePanelList();
        return panels.parallelStream().anyMatch(panel -> {
            BibDatabaseContext context = panel.getBibDatabaseContext();

            return ((context.getLocation() == DatabaseLocation.SHARED) &&
                    this.connectionProperties.equals(context.getDBMSSynchronizer().getConnectionProperties()));
        });
    }

    private void showFileChooser() {
        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileType.BIBTEX_DB)
                .withDefaultExtension(FileType.BIBTEX_DB)
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        DialogService ds = new FXDialogService();

        Optional<Path> path = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));
        path.ifPresent(p -> fileLocationField.setText(p.toString()));
    }

    private void updateEnableState() {
        fileLocationField.setEnabled(autosaveFile.isSelected());
        browseButton.setEnabled(autosaveFile.isSelected());
    }
}
