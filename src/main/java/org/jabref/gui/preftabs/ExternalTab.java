package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.externalfiletype.ExternalFileTypeEditor;
import org.jabref.gui.push.PushToApplication;
import org.jabref.gui.push.PushToApplicationButton;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

class ExternalTab extends JPanel implements PrefsTab {
    private final JabRefPreferences prefs;

    private final JabRefFrame frame;

    private final JTextField emailSubject;
    private final JTextField citeCommand;
    private final JCheckBox openFoldersOfAttachedFiles;

    private final JRadioButton defaultConsole;
    private final JRadioButton executeConsole;
    private final JTextField consoleCommand;
    private final JButton browseButton;

    private final JRadioButton adobeAcrobatReader;
    private final JRadioButton sumatraReader;
    private final JTextField adobeAcrobatReaderPath;
    private final JTextField sumatraReaderPath;
    private final JButton browseAdobeAcrobatReader;
    private final JButton browseSumatraReader;

    public ExternalTab(JabRefFrame frame, PreferencesDialog prefsDiag, JabRefPreferences prefs) {
        this.prefs = prefs;
        this.frame = frame;
        setLayout(new BorderLayout());

        JButton editFileTypes = new JButton(Localization.lang("Manage external file types"));
        citeCommand = new JTextField(25);
        editFileTypes.addActionListener(ExternalFileTypeEditor.getAction(prefsDiag));


        defaultConsole = new JRadioButton(Localization.lang("Use default terminal emulator"));
        executeConsole = new JRadioButton(Localization.lang("Execute command") + ":");
        consoleCommand = new JTextField();
        browseButton = new JButton(Localization.lang("Browse"));

        adobeAcrobatReader = new JRadioButton(Localization.lang("Adobe Acrobat Reader"));
        adobeAcrobatReaderPath = new JTextField();
        browseAdobeAcrobatReader = new JButton(Localization.lang("Browse"));

        sumatraReader = new JRadioButton(Localization.lang("Sumatra Reader"));
        sumatraReaderPath = new JTextField();
        browseSumatraReader = new JButton(Localization.lang("Browse"));


        JLabel commandDescription = new JLabel(Localization.lang(
                "Note: Use the placeholder %0 for the location of the opened library file.", "%DIR"));

        ButtonGroup consoleOptions = new ButtonGroup();
        consoleOptions.add(defaultConsole);
        consoleOptions.add(executeConsole);

        ButtonGroup  readerOptions = new ButtonGroup();
        readerOptions.add(adobeAcrobatReader);

        JPanel pdfOptionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints pdfLayoutConstrains = new GridBagConstraints();

        JPanel consoleOptionPanel = new JPanel(new GridBagLayout());
        GridBagConstraints layoutConstraints = new GridBagConstraints();

        defaultConsole.addActionListener(e -> updateExecuteConsoleButtonAndFieldEnabledState());
        executeConsole.addActionListener(e -> updateExecuteConsoleButtonAndFieldEnabledState());
        browseButton.addActionListener(e -> showConsoleChooser());

        browseAdobeAcrobatReader.addActionListener(e -> showAdobeChooser());

        layoutConstraints.fill = GridBagConstraints.HORIZONTAL;

        pdfLayoutConstrains.fill = GridBagConstraints.HORIZONTAL;

        layoutConstraints.gridx = 0;
        layoutConstraints.gridy = 0;
        layoutConstraints.insets = new Insets(0, 0, 6, 0);
        consoleOptionPanel.add(defaultConsole, layoutConstraints);

        layoutConstraints.gridy = 1;
        consoleOptionPanel.add(executeConsole, layoutConstraints);

        layoutConstraints.gridx = 1;
        consoleOptionPanel.add(consoleCommand, layoutConstraints);

        layoutConstraints.gridx = 2;
        layoutConstraints.insets = new Insets(0, 4, 6, 0);
        consoleOptionPanel.add(browseButton, layoutConstraints);

        layoutConstraints.gridx = 1;
        layoutConstraints.gridy = 2;
        consoleOptionPanel.add(commandDescription, layoutConstraints);

        pdfLayoutConstrains.gridx = 0;
        pdfLayoutConstrains.gridy = 0;
        pdfLayoutConstrains.insets = new Insets(0, 0, 6, 0);
        pdfOptionPanel.add(adobeAcrobatReader, pdfLayoutConstrains);

        pdfLayoutConstrains.gridx = 1;
        pdfOptionPanel.add(adobeAcrobatReaderPath,pdfLayoutConstrains);

        pdfLayoutConstrains.gridx = 2;
        pdfOptionPanel.add(browseAdobeAcrobatReader, pdfLayoutConstrains);

        if (OS.WINDOWS) {
            readerOptions.add(sumatraReader);
            browseSumatraReader.addActionListener(e -> showSumatraChooser());
            pdfLayoutConstrains.gridy = 1;
            pdfLayoutConstrains.gridx = 0;
            pdfOptionPanel.add(sumatraReader,pdfLayoutConstrains);

            pdfLayoutConstrains.gridx = 1;
            pdfOptionPanel.add(sumatraReaderPath, pdfLayoutConstrains);

            pdfLayoutConstrains.gridx = 2;
            pdfOptionPanel.add(browseSumatraReader, pdfLayoutConstrains);
        }

        FormLayout layout = new FormLayout(
                "1dlu, 8dlu, left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref", "");

        DefaultFormBuilder builder = new DefaultFormBuilder(layout);

        builder.appendSeparator(Localization.lang("Sending of emails"));
        builder.append(new JPanel());
        JLabel lab = new JLabel(Localization.lang("Subject for sending an email with references").concat(":"));
        builder.append(lab);
        emailSubject = new JTextField(25);
        builder.append(emailSubject);
        builder.nextLine();
        builder.append(new JPanel());
        openFoldersOfAttachedFiles = new JCheckBox(Localization.lang("Automatically open folders of attached files"));
        builder.append(openFoldersOfAttachedFiles);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("External programs"));
        builder.nextLine();

        JPanel butpan = new JPanel();
        butpan.setLayout(new GridLayout(3, 3));
        for (PushToApplication pushToApplication : frame.getPushApplications().getApplications()) {
            addSettingsButton(pushToApplication, butpan);
        }
        builder.append(new JPanel());
        builder.append(butpan, 3);

        builder.nextLine();
        lab = new JLabel(Localization.lang("Cite command") + ':');
        JPanel pan = new JPanel();
        builder.append(pan);
        builder.append(lab);
        builder.append(citeCommand);

        builder.nextLine();
        builder.append(pan);
        builder.append(editFileTypes);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Open console"));
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(consoleOptionPanel);
        builder.nextLine();

        builder.appendSeparator(Localization.lang("Open PDF"));
        builder.nextLine();
        builder.append(new JPanel());
        builder.append(pdfOptionPanel);

        pan = builder.getPanel();
        pan.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(pan, BorderLayout.CENTER);

    }

    private void addSettingsButton(final PushToApplication pt, JPanel p) {
        JButton button = new JButton(Localization.lang("Settings for %0", pt.getApplicationName()),
                pt.getIcon());
        button.addActionListener(e -> PushToApplicationButton.showSettingsDialog(frame, pt, pt.getSettingsPanel()));
        p.add(button);
    }

    @Override
    public void setValues() {

        emailSubject.setText(prefs.get(JabRefPreferences.EMAIL_SUBJECT));
        openFoldersOfAttachedFiles.setSelected(prefs.getBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES));

        citeCommand.setText(prefs.get(JabRefPreferences.CITE_COMMAND));

        defaultConsole.setSelected(Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));
        executeConsole.setSelected(!Globals.prefs.getBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION));

        consoleCommand.setText(Globals.prefs.get(JabRefPreferences.CONSOLE_COMMAND));

        adobeAcrobatReaderPath.setText(Globals.prefs.get(JabRefPreferences.ADOBE_ACROBAT_COMMAND));
        if (OS.WINDOWS) {
            sumatraReaderPath.setText(Globals.prefs.get(JabRefPreferences.SUMATRA_PDF_COMMAND));

            if (Globals.prefs.get(JabRefPreferences.USE_PDF_READER).equals(adobeAcrobatReaderPath.getText())) {
                adobeAcrobatReader.setSelected(true);
            } else if (Globals.prefs.get(JabRefPreferences.USE_PDF_READER).equals(sumatraReaderPath.getText())) {
                sumatraReader.setSelected(true);
            }
        }

        updateExecuteConsoleButtonAndFieldEnabledState();
    }

    @Override
    public void storeSettings() {
        prefs.put(JabRefPreferences.EMAIL_SUBJECT, emailSubject.getText());
        prefs.putBoolean(JabRefPreferences.OPEN_FOLDERS_OF_ATTACHED_FILES, openFoldersOfAttachedFiles.isSelected());
        prefs.put(JabRefPreferences.CITE_COMMAND, citeCommand.getText());
        prefs.putBoolean(JabRefPreferences.USE_DEFAULT_CONSOLE_APPLICATION, defaultConsole.isSelected());
        prefs.put(JabRefPreferences.CONSOLE_COMMAND, consoleCommand.getText());
        prefs.put(JabRefPreferences.ADOBE_ACROBAT_COMMAND, adobeAcrobatReaderPath.getText());
        if (OS.WINDOWS) {
            prefs.put(JabRefPreferences.SUMATRA_PDF_COMMAND, sumatraReaderPath.getText());
        }
        readerSelected();
    }

    @Override
    public boolean validateSettings() {
        return true;
    }

    @Override
    public String getTabName() {
        return Localization.lang("External programs");
    }

    private void updateExecuteConsoleButtonAndFieldEnabledState() {
        browseButton.setEnabled(executeConsole.isSelected());
        consoleCommand.setEnabled(executeConsole.isSelected());
    }

    private void showConsoleChooser() {
        JFileChooser consoleChooser = new JFileChooser();
        int answer = consoleChooser.showOpenDialog(ExternalTab.this);
        if (answer == JFileChooser.APPROVE_OPTION) {
            consoleCommand.setText(consoleChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void showAdobeChooser() {
        JFileChooser adobeChooser = new JFileChooser();
        int answer = adobeChooser.showOpenDialog(ExternalTab.this);
        if (answer == JFileChooser.APPROVE_OPTION) {
            adobeAcrobatReaderPath.setText(adobeChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void showSumatraChooser() {
        JFileChooser adobeChooser = new JFileChooser();
        int answer = adobeChooser.showOpenDialog(ExternalTab.this);
        if (answer == JFileChooser.APPROVE_OPTION) {
            sumatraReaderPath.setText(adobeChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void readerSelected() {
        if (adobeAcrobatReader.isSelected()) {
            prefs.put(JabRefPreferences.USE_PDF_READER, adobeAcrobatReaderPath.getText());
        }
        else if (sumatraReader.isSelected()) {
            prefs.put(JabRefPreferences.USE_PDF_READER, sumatraReaderPath.getText());
        }
    }
}
