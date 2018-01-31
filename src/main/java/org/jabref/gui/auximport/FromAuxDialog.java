package org.jabref.gui.auximport;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.jabref.Globals;
import org.jabref.gui.BasePanel;
import org.jabref.gui.DialogService;
import org.jabref.gui.FXDialogService;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.util.DefaultTaskExecutor;
import org.jabref.gui.util.FileDialogConfiguration;
import org.jabref.logic.auxparser.DefaultAuxParser;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.FileType;
import org.jabref.model.auxparser.AuxParser;
import org.jabref.model.auxparser.AuxParserResult;
import org.jabref.model.database.BibDatabase;
import org.jabref.preferences.JabRefPreferences;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * A wizard dialog for generating a new sub database from existing TeX AUX file
 */
public class FromAuxDialog extends JabRefDialog {

    private final JPanel statusPanel = new JPanel();
    private final JPanel buttons = new JPanel();
    private final JButton generateButton = new JButton();
    private final JButton cancelButton = new JButton();
    private final JButton parseButton = new JButton();

    private final JComboBox<String> dbChooser = new JComboBox<>();
    private JTextField auxFileField;

    private JList<String> notFoundList;
    private JTextArea statusInfos;

    // all open databases from JabRefFrame
    private final JTabbedPane parentTabbedPane;

    private boolean generatePressed;

    private AuxParserResult auxParserResult;

    private final JabRefFrame parentFrame;

    public FromAuxDialog(JabRefFrame frame, String title, boolean modal, JTabbedPane viewedDBs) {
        super(frame, title, modal, FromAuxDialog.class);

        parentTabbedPane = viewedDBs;
        parentFrame = frame;

        jbInit();
        pack();
        setSize(600, 500);
    }

    private void jbInit() {
        JPanel panel1 = new JPanel();

        panel1.setLayout(new BorderLayout());
        generateButton.setText(Localization.lang("Generate"));
        generateButton.setEnabled(false);
        generateButton.addActionListener(e -> {
            generatePressed = true;
            dispose();
        });
        cancelButton.setText(Localization.lang("Cancel"));
        cancelButton.addActionListener(e -> dispose());

        parseButton.setText(Localization.lang("Parse"));
        parseButton.addActionListener(e -> parseActionPerformed());

        initPanels();

        // insert the buttons
        ButtonBarBuilder bb = new ButtonBarBuilder();
        JPanel buttonPanel = bb.getPanel();
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        bb.addGlue();
        bb.addButton(parseButton);
        bb.addRelatedGap();
        bb.addButton(generateButton);
        bb.addButton(cancelButton);
        bb.addGlue();
        this.setModal(true);
        this.setResizable(true);
        this.setTitle(Localization.lang("AUX file import"));
        JLabel desc = new JLabel("<html><h3>" + Localization.lang("AUX file import") + "</h3><p>"
                + Localization.lang("This feature generates a new library based on which entries "
                        + "are needed in an existing LaTeX document.")
                + "</p>" + "<p>"
                + Localization.lang("You need to select one of your open libraries from which to choose "
                        + "entries, as well as the AUX file produced by LaTeX when compiling your document.")
                + "</p></html>");
        desc.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        panel1.add(desc, BorderLayout.NORTH);

        JPanel centerPane = new JPanel(new BorderLayout());
        centerPane.add(buttons, BorderLayout.NORTH);
        centerPane.add(statusPanel, BorderLayout.CENTER);

        getContentPane().add(panel1, BorderLayout.NORTH);
        getContentPane().add(centerPane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        // Key bindings:
        ActionMap am = statusPanel.getActionMap();
        InputMap im = statusPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private void initPanels() {
        // collect the names of all open databases
        int len = parentTabbedPane.getTabCount();
        int toSelect = -1;
        for (int i = 0; i < len; i++) {
            dbChooser.addItem(parentTabbedPane.getTitleAt(i));
            if (parentFrame.getBasePanelAt(i) == parentFrame.getCurrentBasePanel()) {
                toSelect = i;
            }
        }
        if (toSelect >= 0) {
            dbChooser.setSelectedIndex(toSelect);
        }

        auxFileField = new JTextField("", 25);
        JButton browseAuxFileButton = new JButton(Localization.lang("Browse"));

        FileDialogConfiguration fileDialogConfiguration = new FileDialogConfiguration.Builder()
                .addExtensionFilter(FileType.AUX)
                .withDefaultExtension(FileType.AUX)
                .withInitialDirectory(Globals.prefs.get(JabRefPreferences.WORKING_DIRECTORY)).build();
        DialogService ds = new FXDialogService();

        browseAuxFileButton.addActionListener(e -> {
            Optional<Path> file = DefaultTaskExecutor
                    .runInJavaFXThread(() -> ds.showFileOpenDialog(fileDialogConfiguration));
            file.ifPresent(f -> auxFileField.setText(f.toAbsolutePath().toString()));
        });

        notFoundList = new JList<>();
        JScrollPane listScrollPane = new JScrollPane(notFoundList);
        statusInfos = new JTextArea("", 5, 20);
        JScrollPane statusScrollPane = new JScrollPane(statusInfos);
        statusInfos.setEditable(false);

        DefaultFormBuilder b = new DefaultFormBuilder(
                new FormLayout("left:pref, 4dlu, fill:pref:grow, 4dlu, left:pref", ""), buttons);
        b.appendSeparator(Localization.lang("Options"));
        b.append(Localization.lang("Reference library") + ":");
        b.append(dbChooser, 3);
        b.nextLine();
        b.append(Localization.lang("LaTeX AUX file") + ":");
        b.append(auxFileField);
        b.append(browseAuxFileButton);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        b = new DefaultFormBuilder(new FormLayout("fill:pref:grow, 4dlu, fill:pref:grow", "pref, pref, fill:pref:grow"),
                statusPanel);
        b.appendSeparator(Localization.lang("Result"));
        b.append(Localization.lang("Unknown BibTeX entries") + ":");
        b.append(Localization.lang("Messages") + ":");
        b.nextLine();
        b.append(listScrollPane);
        b.append(statusScrollPane);
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }

    private void parseActionPerformed() {
        parseButton.setEnabled(false);
        BasePanel bp = (BasePanel) parentTabbedPane.getComponentAt(dbChooser.getSelectedIndex());
        notFoundList.removeAll();
        statusInfos.setText(null);
        BibDatabase refBase = bp.getDatabase();
        String auxName = auxFileField.getText();

        if ((auxName != null) && (refBase != null) && !auxName.isEmpty()) {
            AuxParser auxParser = new DefaultAuxParser(refBase);
            auxParserResult = auxParser.parse(Paths.get(auxName));
            notFoundList.setListData(auxParserResult.getUnresolvedKeys().toArray(new String[auxParserResult.getUnresolvedKeys().size()]));
            statusInfos.append(new AuxParserResultViewModel(auxParserResult).getInformation(false));

            generateButton.setEnabled(true);

            // the generated database contains no entries -> no active generate-button
            if (!auxParserResult.getGeneratedBibDatabase().hasEntries()) {
                statusInfos.append("\n" + Localization.lang("empty library"));
                generateButton.setEnabled(false);
            }
        } else {
            generateButton.setEnabled(false);
        }

        parseButton.setEnabled(true);
    }

    public boolean generatePressed() {
        return generatePressed;
    }

    public BibDatabase getGenerateDB() {
        return auxParserResult.getGeneratedBibDatabase();
    }

}
