package org.jabref.gui.contentselector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.fieldeditors.FieldEditor;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.entry.Keyword;
import org.jabref.model.entry.KeywordList;
import org.jabref.model.metadata.MetaData;

import com.jgoodies.forms.layout.Sizes;
import com.jgoodies.looks.Options;

/**
 * A combo-box and a manage button that will add selected strings to an
 * associated entry editor.
 *
 * Used to manage keywords and authors for instance.
 */
public class FieldContentSelector extends JComponent {

    private static final int MAX_CONTENT_SELECTOR_WIDTH = 240; // The max width of the combobox for content selectors.
    private final JComboBox<String> comboBox;

    private final FieldEditor editor;

    private final MetaData metaData;

    private final AbstractAction action;
    private final String delimiter;


    /**
     *
     * Create a new FieldContentSelector.
     *
     * @param frame
     *            The one JabRef-Frame.
     * @param panel
     *            The basepanel the entry-editor is on.
     * @param owner
     *            The window/frame/dialog which should be the owner of the
     *            content selector dialog.
     * @param editor
     *            The entry editor which will be appended by the text selected
     *            by the user from the combobox.
     * @param action
     *            The action that will be performed to after an item from the
     *            combobox has been appended to the text in the entryeditor.
     * @param horizontalLayout
     *            Whether to put a 2 pixel horizontal strut between combobox and
     *            button.
     */
    public FieldContentSelector(JabRefFrame frame, final BasePanel panel, Window owner, final FieldEditor editor,
                                final AbstractAction action, boolean horizontalLayout, String delimiter) {


        this.editor = editor;
        this.metaData = panel.getBibDatabaseContext().getMetaData();
        this.action = action;
        this.delimiter = delimiter;

        comboBox = new JComboBox<String>() {

            @Override
            public Dimension getPreferredSize() {
                Dimension parents = super.getPreferredSize();
                if (parents.width > MAX_CONTENT_SELECTOR_WIDTH) {
                    parents.width = MAX_CONTENT_SELECTOR_WIDTH;
                }
                return parents;
            }
        };

        GridBagLayout gbl = new GridBagLayout();
        GridBagConstraints con = new GridBagConstraints();

        setLayout(gbl);

        // comboBox.setEditable(true);

        comboBox.setMaximumRowCount(35);

        // Set the width of the popup independent of the size of th box itself:
        comboBox.putClientProperty(Options.COMBO_POPUP_PROTOTYPE_DISPLAY_VALUE_KEY,
                "The longest text in the combo popup menu. And even longer.");

        rebuildComboBox();

        con.gridwidth = horizontalLayout ? 3 : GridBagConstraints.REMAINDER;
        con.fill = GridBagConstraints.HORIZONTAL;
        con.weightx = 1;
        gbl.setConstraints(comboBox, con);

        comboBox.addActionListener(e -> {
            /*
             * These conditions signify arrow key navigation in the dropdown
             * list, so we should not react to it. I'm not sure if this is
             * well defined enough to be guaranteed to work everywhere.
             */
            if ("comboBoxChanged".equals(e.getActionCommand()) && (e.getModifiers() == 0)) {
                return;
            }

            selectionMade();
        });
        // Add an action for the Enter key that signals a selection:
        comboBox.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "enter");
        comboBox.getActionMap().put("enter", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                selectionMade();
                comboBox.setPopupVisible(false);
            }
        });

        add(comboBox);

        if (horizontalLayout) {
            add(Box.createHorizontalStrut(Sizes.dialogUnitXAsPixel(2, this)));
        }

        JButton manage = new JButton(Localization.lang("Manage"));
        gbl.setConstraints(manage, con);
        add(manage);

        manage.addActionListener(e -> {
            ContentSelectorDialog csd = new ContentSelectorDialog(owner, frame, panel, true, editor.getFieldName());
            csd.setLocationRelativeTo(frame);

            // Calling setVisible(true) will open the modal dialog and block
            // for the dialog to close.
            csd.setVisible(true);

            // So we need to rebuild the ComboBox afterwards
            rebuildComboBox();
        });
    }

    private void selectionMade() {
        // The first element is empty to avoid a preselection
        if (comboBox.getSelectedIndex() == 0) {
            return;
        }

        String chosen = (String) comboBox.getSelectedItem();
        if ((chosen == null) || chosen.isEmpty()) {
            return;
        }

        String currentText = editor.getText();
        KeywordList words = KeywordList.parse(currentText, this.delimiter.charAt(0));
        boolean alreadyInList = words.contains(new Keyword(chosen));

        // not the first word and no duplicate -> we need a comma
        if (!"".equals(currentText) && !alreadyInList) {
            editor.append(FieldContentSelector.this.delimiter);
        }

        // no duplicate -> add it
        if (!alreadyInList) {
            editor.append(chosen);
        }

        comboBox.setSelectedIndex(0);

        // Fire event that we changed the editor
        if (action != null) {
            action.actionPerformed(new ActionEvent(editor, 0, ""));
        }

        // Transfer focus to the editor.
        editor.requestFocus();
    }

    public void rebuildComboBox() {
        comboBox.removeAllItems();

        // To have an empty field as the default for the combobox
        comboBox.addItem("");
        for (String item : metaData.getContentSelectorValuesForField(editor.getFieldName())) {
            comboBox.addItem(item);
        }
    }

}
