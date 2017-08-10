package org.jabref.gui.externalfiletype;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.jabref.Globals;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.IconTheme;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.OS;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * This class produces a dialog box for editing an external file type.
 */
public class ExternalFileTypeEntryEditor {

    private JFrame fParent;
    private JDialog dParent;
    private JDialog diag;
    private final JTextField extension = new JTextField();
    private final JTextField name = new JTextField();
    private final JTextField mimeType = new JTextField();
    private final JTextField application = new JTextField();
    private final JLabel icon = new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
    private final JButton ok = new JButton(Localization.lang("OK"));
    private final JButton cancel = new JButton(Localization.lang("Cancel"));
    private final JRadioButton useDefault = new JRadioButton(Localization.lang("Default"));
    private final JRadioButton other = new JRadioButton("");
    private final String editFileTitle = Localization.lang("Edit file type");
    private final String newFileTitle = Localization.lang("Add new file type");

    private ExternalFileType entry;
    private boolean okPressed;

    private final ActionListener browsePressed = e -> {
        String appDir = application.getText().trim();
        if (appDir.isEmpty()) {
            appDir = Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY);
        }

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .withInitialDirectory(Paths.get(appDir)).build();
        DialogService ds = new FXDialogService();

        Optional<Path> path = DefaultTaskExecutor
                .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));

        path.ifPresent(applicationDir -> {
            if (applicationDir.getParent() != null) {
                Globals.prefs.put(JabRefPreferences.WORKING_DIRECTORY, applicationDir.getParent().toString());
            }
            application.setText(applicationDir.toString());
        });
    };

    public ExternalFileTypeEntryEditor(JFrame parent, ExternalFileType entry) {
        fParent = parent;
        init(entry);
    }

    public ExternalFileTypeEntryEditor(JDialog parent, ExternalFileType entry) {
        dParent = parent;
        init(entry);
    }

    private void init(ExternalFileType inEntry) {
        entry = inEntry;
        icon.setText(null);

        ButtonGroup bg = new ButtonGroup();
        bg.add(useDefault);
        bg.add(other);

        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:150dlu, 4dlu, fill:pref",
                "p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p"));
        builder.add(Localization.lang("Icon")).xy(1, 1);
        builder.add(icon).xy(3, 1);
        builder.add(Localization.lang("Name")).xy(1, 3);
        builder.add(name).xy(3, 3);
        builder.add(Localization.lang("Extension")).xy(1, 5);
        builder.add(extension).xy(3, 5);
        builder.add(Localization.lang("MIME type")).xy(1, 7);
        builder.add(mimeType).xy(3, 7);
        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        builder.add(Localization.lang("Application")).xy(1, 9);
        JButton browseBut = new JButton(Localization.lang("Browse"));
        if (OS.WINDOWS) {
            builder.add(useDefault).xy(3, 9);
            builder.appendRows("2dlu, p");
            JPanel p1 = new JPanel();
            builder.add(p1).xy(1, 11);
            JPanel p2 = new JPanel();
            application.setPreferredSize(new Dimension(300, application.getPreferredSize().height));
            BorderLayout bl = new BorderLayout();
            bl.setHgap(4);
            p2.setLayout(bl);
            p2.add(other, BorderLayout.WEST);
            p2.add(application, BorderLayout.CENTER);
            builder.add(p2).xy(3, 11);
            builder.add(browseBut).xy(5, 11);
        } else {
            builder.add(application).xy(3, 9);
            builder.add(browseBut).xy(5, 9);
        }
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        ok.addActionListener(e -> {
            okPressed = true;

            storeSettings(ExternalFileTypeEntryEditor.this.entry);
            diag.dispose();

        });
        cancel.addActionListener(e -> diag.dispose());

        if (OS.WINDOWS) {
            application.getDocument().addDocumentListener(new DocumentListener() {

                private void handle() {
                    if (application.getText().isEmpty()) {
                        useDefault.setSelected(true);
                    } else {
                        other.setSelected(true);
                    }
                }

                @Override
                public void insertUpdate(DocumentEvent documentEvent) {
                    handle();
                }

                @Override
                public void removeUpdate(DocumentEvent documentEvent) {
                    handle();
                }

                @Override
                public void changedUpdate(DocumentEvent documentEvent) {
                    handle();
                }
            });
        }

        String title = editFileTitle;

        if (entry.getName().isEmpty()) {
            title = newFileTitle;
        }

        if (dParent == null) {
            diag = new JDialog(fParent, title, true);
        } else {
            diag = new JDialog(dParent, title, true);
        }
        diag.getContentPane().add(builder.getPanel(), BorderLayout.CENTER);
        diag.getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        diag.pack();

        browseBut.addActionListener(browsePressed);

        if (dParent == null) {
            diag.setLocationRelativeTo(fParent);
        } else {
            diag.setLocationRelativeTo(dParent);
        }

        setValues(entry);
    }

    public void setEntry(ExternalFileType entry) {
        this.entry = entry;
        if (entry.getName().isEmpty()) {
            diag.setTitle(newFileTitle);
        } else {
            diag.setTitle(editFileTitle);
        }
        setValues(entry);
    }

    public void setVisible(boolean visible) {
        if (visible) {
            okPressed = false;
        }
        diag.setVisible(visible);
    }

    private void setValues(ExternalFileType entry) {
        name.setText(entry.getName());
        extension.setText(entry.getExtension());
        mimeType.setText(entry.getMimeType());
        application.setText(entry.getOpenWithApplication());
        icon.setIcon(entry.getIcon());
        if (application.getText().isEmpty()) {
            useDefault.setSelected(true);
        } else {
            other.setSelected(true);
        }
    }

    private void storeSettings(ExternalFileType fileTypeEntry) {
        fileTypeEntry.setName(name.getText().trim());
        fileTypeEntry.setMimeType(mimeType.getText().trim());
        // Set extension, but remove initial dot if user has added that:
        String ext = extension.getText().trim();
        if (!ext.isEmpty() && (ext.charAt(0) == '.')) {
            fileTypeEntry.setExtension(ext.substring(1));
        } else {
            fileTypeEntry.setExtension(ext);
        }

        if (OS.WINDOWS) {
            // On Windows, store application as empty if the "Default" option is selected,
            // or if the application name is empty:
            if (useDefault.isSelected() || application.getText().trim().isEmpty()) {
                fileTypeEntry.setOpenWith("");
            } else {
                fileTypeEntry.setOpenWith(application.getText().trim());
            }
        } else {
            fileTypeEntry.setOpenWith(application.getText().trim());
        }
    }

    public boolean okPressed() {
        return okPressed;
    }

}
