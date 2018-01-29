package org.jabref.gui.exporter;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialog for creating or modifying custom exports.
 */
class CustomExportDialog extends JabRefDialog {

    private static final Logger LOGGER = LoggerFactory.getLogger(CustomExportDialog.class);

    private final JTextField name = new JTextField(60);
    private final JTextField layoutFile = new JTextField(60);
    private final JTextField extension = new JTextField(60);
    private JabRefFrame frame;
    private boolean okPressed;

    public CustomExportDialog(final JabRefFrame parent, final String exporterName, final String layoutFileName,
            final String extensionName) {
        this(parent);
        frame = parent;
        name.setText(exporterName);
        layoutFile.setText(layoutFileName);
        extension.setText(extensionName);
    }

    public CustomExportDialog(final JabRefFrame parent) {
        super(parent, Localization.lang("Edit custom export"), true, CustomExportDialog.class);
        frame = parent;
        ActionListener okListener = e -> {
            Path layoutFileDir = Paths.get(layoutFile.getText()).getParent();
            if (layoutFileDir != null) {
                Globals.prefs.put(JabRefPreferences.EXPORT_WORKING_DIRECTORY, layoutFileDir.toString());

            }

            // Check that there are no empty strings.
            if (layoutFile.getText().isEmpty() || name.getText().isEmpty() || extension.getText().isEmpty()
                    || !layoutFile.getText().endsWith(".layout")) {

                LOGGER.info("One field is empty!"); //TODO: Better error message
                return;
            }

            // Handling of : and ; must also be done.

            okPressed = true;
            dispose();
        };

        layoutFile.setText(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY));

        JButton ok = new JButton(Localization.lang("OK"));
        ok.addActionListener(okListener);
        name.addActionListener(okListener);
        layoutFile.addActionListener(okListener);
        extension.addActionListener(okListener);

        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(e -> dispose());

        JButton browse = new JButton(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileType.LAYOUT)
                .withDefaultExtension(FileType.LAYOUT)
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.EXPORT_WORKING_DIRECTORY)).build();
        DialogService ds = new FXDialogService();
        browse.addActionListener(
                e -> DefaultTaskExecutor.runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration))
                        .ifPresent(f -> layoutFile.setText(f.toAbsolutePath().toString())));

        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        // Key bindings:
        JPanel main = new JPanel();
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        // Layout starts here.
        GridBagLayout gbl = new GridBagLayout();
        main.setLayout(gbl);
        main.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                Localization.lang("Export properties")));

        // Main panel:
        GridBagConstraints con = new GridBagConstraints();
        con.weightx = 0;
        con.gridwidth = 1;
        con.insets = new Insets(3, 5, 3, 5);
        con.anchor = GridBagConstraints.EAST;
        con.fill = GridBagConstraints.NONE;
        con.gridx = 0;
        con.gridy = 0;
        JLabel nl = new JLabel(Localization.lang("Export name") + ':');
        gbl.setConstraints(nl, con);
        main.add(nl);
        con.gridy = 1;
        JLabel nr = new JLabel(Localization.lang("Main layout file") + ':');
        gbl.setConstraints(nr, con);
        main.add(nr);
        con.gridy = 2;
        JLabel nf = new JLabel(Localization.lang("Extension") + ':');
        gbl.setConstraints(nf, con);
        main.add(nf);

        con.gridwidth = 2;
        con.weightx = 1;
        con.anchor = GridBagConstraints.WEST;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.gridy = 0;
        con.gridx = 1;
        gbl.setConstraints(name, con);
        main.add(name);
        con.gridy = 1;
        con.gridwidth = 1;
        gbl.setConstraints(layoutFile, con);
        main.add(layoutFile);
        con.gridx = 2;
        con.weightx = 0;
        gbl.setConstraints(browse, con);
        main.add(browse);
        con.weightx = 1;
        con.gridwidth = 2;
        con.gridx = 1;
        con.gridy = 2;
        gbl.setConstraints(extension, con);
        main.add(extension);

        JPanel buttons = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(buttons);
        buttons.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        setSize(700, 200);

        setLocationRelativeTo(parent);
    }

    public boolean okPressed() {
        return okPressed;
    }

    public String layoutFile() {
        return layoutFile.getText();
    }

    public String name() {
        return name.getText();
    }

    public String extension() {
        String ext = extension.getText();
        if (ext.startsWith(".")) {
            return ext;
        } else if (ext.startsWith("*.")) {
            return ext.substring(1);
        } else {
            return '.' + ext;
        }
    }

}
