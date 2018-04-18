package org.jabref.gui.preftabs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.BasePanel;
import org.jabref.gui.PreviewPanel;
import org.jabref.gui.customjfx.CustomJFXPanel;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.TestEntry;
import org.jabref.preferences.PreviewPreferences;

import com.google.common.primitives.Ints;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.factories.Paddings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreviewPrefsTab extends JPanel implements PrefsTab {

    private static final Logger LOGGER = LoggerFactory.getLogger(PreviewPrefsTab.class);

    private SwingWorker<List<CitationStyle>, Void> discoverCitationStyleWorker;

    private final DefaultListModel<Object> availableModel = new DefaultListModel<>();
    private final DefaultListModel<Object> chosenModel = new DefaultListModel<>();

    private final JList<Object> available = new JList<>(availableModel);
    private final JList<Object> chosen = new JList<>(chosenModel);

    private final JButton btnRight = new JButton("»");
    private final JButton btnLeft = new JButton("«");
    private final JButton btnUp = new JButton(Localization.lang("Up"));
    private final JButton btnDown = new JButton(Localization.lang("Down"));


    private final JTextArea layout = new JTextArea("", 1, 1);
    private final JButton btnTest = new JButton(Localization.lang("Test"));
    private final JButton btnDefault = new JButton(Localization.lang("Default"));
    private final JScrollPane scrollPane = new JScrollPane(layout);


    public PreviewPrefsTab() {
        setupLogic();
        setupGui();
    }

    private void setupLogic() {
        chosen.getSelectionModel().addListSelectionListener(event -> {
            boolean selectionEmpty = ((ListSelectionModel) event.getSource()).isSelectionEmpty();
            btnLeft.setEnabled(!selectionEmpty);
            btnDown.setEnabled(!selectionEmpty);
            btnUp.setEnabled(!selectionEmpty);
        });

        available.getSelectionModel()
                .addListSelectionListener(e -> btnRight.setEnabled(!((ListSelectionModel) e.getSource()).isSelectionEmpty()));

        btnRight.addActionListener(event -> {
            for (Object object : available.getSelectedValuesList()) {
                availableModel.removeElement(object);
                chosenModel.addElement(object);
            }
        });

        btnLeft.addActionListener(event -> {
            for (Object object : chosen.getSelectedValuesList()) {
                availableModel.addElement(object);
                chosenModel.removeElement(object);
            }
        });

        btnUp.addActionListener(event -> {
            List<Integer> newSelectedIndices = new ArrayList<>();
            for (int oldIndex : chosen.getSelectedIndices()) {
                boolean alreadyTaken = newSelectedIndices.contains(oldIndex - 1);
                int newIndex = (oldIndex > 0 && !alreadyTaken) ? oldIndex - 1 : oldIndex;
                chosenModel.add(newIndex, chosenModel.remove(oldIndex));
                newSelectedIndices.add(newIndex);
            }
            chosen.setSelectedIndices(Ints.toArray(newSelectedIndices));
        });

        btnDown.addActionListener(event -> {
            List<Integer> newSelectedIndices = new ArrayList<>();
            int[] selectedIndices = chosen.getSelectedIndices();
            for (int i = selectedIndices.length - 1; i >= 0; i--) {
                int oldIndex = selectedIndices[i];
                boolean alreadyTaken = newSelectedIndices.contains(oldIndex + 1);
                int newIndex = (oldIndex < chosenModel.getSize() - 1 && !alreadyTaken) ? oldIndex + 1 : oldIndex;
                chosenModel.add(newIndex, chosenModel.remove(oldIndex));
                newSelectedIndices.add(newIndex);
            }
            chosen.setSelectedIndices(Ints.toArray(newSelectedIndices));
        });

        btnDefault.addActionListener(event -> layout.setText(Globals.prefs.getPreviewPreferences()
                .getPreviewStyleDefault().replace("__NEWLINE__", "\n")));

        btnTest.addActionListener(event -> {
            try {
                PreviewPanel testPane = new PreviewPanel(null, null);
                testPane.setFixedLayout(layout.getText());
                testPane.setEntry(TestEntry.getTestEntry());
                JFXPanel container = CustomJFXPanel.wrap(new Scene(testPane));
                container.setPreferredSize(new Dimension(800, 350));
                JOptionPane.showMessageDialog(PreviewPrefsTab.this, container, Localization.lang("Preview"), JOptionPane.PLAIN_MESSAGE);
            } catch (StringIndexOutOfBoundsException exception) {
                LOGGER.warn("Parsing error.", exception);
                JOptionPane.showMessageDialog(null,
                        Localization.lang("Parsing error") + ": " + Localization.lang("illegal backslash expression")
                                + ".\n" + exception.getMessage(),
                        Localization.lang("Parsing error"), JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private void setupGui() {
        JPanel chooseStyle = FormBuilder.create()
                .columns("0:grow, $lcgap, pref, $lcgap, 0:grow")
                .rows("pref, $lg, fill:pref:grow, $lg, pref:grow, $lg, pref:grow, $lg, pref:grow")
                .padding(Paddings.DIALOG)

                .addSeparator(Localization.lang("Current Preview")).xyw(1, 1, 5)
                .add(available).xywh(1, 3, 1, 7)
                .add(chosen).xywh(5, 3, 1, 7)

                .add(btnRight).xy(3, 3, "fill, bottom")
                .add(btnLeft).xy(3, 5, "fill, top")
                .add(btnUp).xy(3, 7, "fill, bottom")
                .add(btnDown).xy(3, 9, "fill, top")
                .build();

        JPanel preview = FormBuilder.create()
                .columns("pref:grow, $lcgap, pref, $lcgap, pref")
                .rows("pref, $lg, fill:pref:grow")
                .padding(Paddings.DIALOG)

                .addSeparator(Localization.lang("Preview")).xy(1, 1)
                .add(btnTest).xy(3, 1)
                .add(btnDefault).xy(5, 1)
                .add(scrollPane).xyw(1, 3, 5)
                .build();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(chooseStyle, BorderLayout.CENTER);
        add(preview, BorderLayout.PAGE_END);
    }

    @Override
    public void setValues() {
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences();

        chosenModel.clear();
        boolean isPreviewChosen = false;
        for (String style : previewPreferences.getPreviewCycle()) {
            // in case the style is not a valid citation style file, an empty Optional is returned
            Optional<CitationStyle> citationStyle = CitationStyle.createCitationStyleFromFile(style);
            if (citationStyle.isPresent()) {
                chosenModel.addElement(citationStyle.get());
            } else {
                if (isPreviewChosen) {
                    LOGGER.error("Preview is already in the list, something went wrong");
                    continue;
                }
                isPreviewChosen = true;
                chosenModel.addElement(Localization.lang("Preview"));
            }
        }

        availableModel.clear();
        if (!isPreviewChosen) {
            availableModel.addElement(Localization.lang("Preview"));
        }

        btnLeft.setEnabled(!chosen.isSelectionEmpty());
        btnRight.setEnabled(!available.isSelectionEmpty());
        btnUp.setEnabled(!chosen.isSelectionEmpty());
        btnDown.setEnabled(!chosen.isSelectionEmpty());

        if (discoverCitationStyleWorker != null) {
            discoverCitationStyleWorker.cancel(true);
        }

        discoverCitationStyleWorker = new SwingWorker<List<CitationStyle>, Void>() {
            @Override
            protected List<CitationStyle> doInBackground() throws Exception {
                return CitationStyle.discoverCitationStyles();
            }

            @Override
            public void done() {
                if (this.isCancelled()) {
                    return;
                }
                try {
                    get().stream()
                            .filter(style -> !previewPreferences.getPreviewCycle().contains(style.getFilePath()))
                            .sorted(Comparator.comparing(CitationStyle::getTitle))
                            .forEach(availableModel::addElement);

                    btnRight.setEnabled(!availableModel.isEmpty());
                } catch (InterruptedException | ExecutionException e) {
                    LOGGER.error("something went wrong while adding the discovered CitationStyles to the list ");
                }
            }
        };
        discoverCitationStyleWorker.execute();

        layout.setText(Globals.prefs.getPreviewPreferences().getPreviewStyle().replace("__NEWLINE__", "\n"));
    }

    @Override
    public void storeSettings() {
        List<String> styles = new ArrayList<>();
        Enumeration<Object> elements = chosenModel.elements();
        while (elements.hasMoreElements()) {
            Object obj = elements.nextElement();
            if (obj instanceof CitationStyle) {
                styles.add(((CitationStyle) obj).getFilePath());
            } else if (obj instanceof String) {
                styles.add("Preview");
            }
        }
        PreviewPreferences previewPreferences = Globals.prefs.getPreviewPreferences()
                .getBuilder()
                .withPreviewCycle(styles)
                .withPreviewStyle(layout.getText().replace("\n", "__NEWLINE__"))
                .build();
        Globals.prefs.storePreviewPreferences(previewPreferences);

        // update preview
        for (BasePanel basePanel : JabRefGUI.getMainFrame().getBasePanelList()) {
            basePanel.getPreviewPanel().updateLayout();
        }
    }

    @Override
    public boolean validateSettings() {
        return !chosenModel.isEmpty();
    }

    @Override
    public String getTabName() {
        return Localization.lang("Entry preview");
    }

}
