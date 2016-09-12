package net.sf.jabref.gui.protectedterms;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JTextField;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.protectedterms.ProtectedTermsLoader;
import net.sf.jabref.logic.util.FileExtensions;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

public class NewProtectedTermsFileDialog extends JDialog {

    private final JTextField newFile = new JTextField();
    private final JTextField newDescription = new JTextField();
    private final JCheckBox enabled = new JCheckBox(Localization.lang("Enabled"));
    private boolean addOKPressed;
    private final ProtectedTermsLoader loader;
    private JFrame parent;

    public NewProtectedTermsFileDialog(JDialog parent, ProtectedTermsLoader loader) {
        super(parent, Localization.lang("New protected terms file"), true);
        this.loader = loader;
        setupDialog();
        setLocationRelativeTo(parent);
    }

    public NewProtectedTermsFileDialog(JabRefFrame mainFrame, ProtectedTermsLoader loader) {
        super(mainFrame, Localization.lang("New protected terms file"), true);
        parent = mainFrame;
        this.loader = loader;
        setupDialog();
        setLocationRelativeTo(mainFrame);
    }

    private void setupDialog() {
        JButton browse = new JButton(Localization.lang("Browse"));
        FileDialog dialog = new FileDialog(parent).withExtension(FileExtensions.TERMS);
        dialog.setDefaultExtension(FileExtensions.TERMS);
        browse.addActionListener(e -> {
            Optional<Path> file = dialog.showDialogAndGetSelectedFile();
            file.ifPresent(f -> newFile.setText(f.toAbsolutePath().toString()));
        });

        // Build content panel
        FormBuilder builder = FormBuilder.create();
        builder.layout(new FormLayout("left:pref, 4dlu, fill:100dlu:grow, 4dlu, pref", "p, 4dlu, p, 4dlu, p"));
        builder.add(Localization.lang("Description")).xy(1, 1);
        builder.add(newDescription).xyw(3, 1, 3);
        builder.add(Localization.lang("File")).xy(1, 3);
        builder.add(newFile).xy(3, 3);
        builder.add(browse).xy(5, 3);
        builder.add(enabled).xyw(1, 5, 5);
        enabled.setSelected(true);
        builder.padding("10dlu, 10dlu, 10dlu, 10dlu");
        getContentPane().add(builder.build(), BorderLayout.CENTER);

        // Buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JButton addOKButton = new JButton(Localization.lang("OK"));
        JButton addCancelButton = new JButton(Localization.lang("Cancel"));
        bb.addGlue();
        bb.addButton(addOKButton);
        bb.addButton(addCancelButton);
        bb.addGlue();
        bb.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        addOKButton.addActionListener(e -> {
            addOKPressed = true;
            loader.addNewProtectedTermsList(newDescription.getText(), newFile.getText(),
                    enabled.isSelected());
            dispose();
        });

        Action cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                addOKPressed = false;
                dispose();
            }
        };
        addCancelButton.addActionListener(cancelAction);

        // Key bindings:
        bb.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        bb.getPanel().getActionMap().put("close", cancelAction);
        pack();
    }

    public boolean isOKPressed() {
        return addOKPressed;
    }

}
