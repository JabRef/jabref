package net.sf.jabref.gui.groups;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import net.sf.jabref.Globals;
import net.sf.jabref.gui.BasePanel;
import net.sf.jabref.gui.JabRefFrame;
import net.sf.jabref.gui.keyboard.KeyBinding;
import net.sf.jabref.gui.undo.NamedCompound;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.layout.format.LatexToUnicodeFormatter;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.groups.ExplicitGroup;
import net.sf.jabref.model.groups.GroupHierarchyType;
import net.sf.jabref.model.groups.GroupTreeNode;
import net.sf.jabref.model.groups.GroupsUtil;
import net.sf.jabref.model.groups.KeywordGroup;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

/**
 * Dialog for creating or modifying groups. Operates directly on the Vector containing group information.
 */
class AutoGroupDialog extends JDialog implements CaretListener {

    private final JTextField remove = new JTextField(60);
    private final JTextField field = new JTextField(60);
    private final JTextField deliminator = new JTextField(60);
    private final JRadioButton keywords = new JRadioButton(
            Localization.lang("Generate groups from keywords in a BibTeX field"));
    private final JRadioButton authors = new JRadioButton(Localization.lang("Generate groups for author last names"));
    private final JRadioButton editors = new JRadioButton(Localization.lang("Generate groups for editor last names"));
    private final JCheckBox useCustomDelimiter = new JCheckBox(
            Localization.lang("Use the following delimiter character(s):"));
    private final JButton ok = new JButton(Localization.lang("OK"));
    private final GroupTreeNodeViewModel m_groupsRoot;
    private final JabRefFrame frame;
    private final BasePanel panel;


    /**
     * @param groupsRoot The original set of groups, which is required as undo information when all groups are cleared.
     */
    public AutoGroupDialog(JabRefFrame jabrefFrame, BasePanel basePanel,
            GroupTreeNodeViewModel groupsRoot, String defaultField, String defaultRemove, String defaultDeliminator) {
        super(jabrefFrame, Localization.lang("Automatically create groups"), true);
        frame = jabrefFrame;
        panel = basePanel;
        m_groupsRoot = groupsRoot;
        field.setText(defaultField);
        remove.setText(defaultRemove);
        deliminator.setText(defaultDeliminator);
        useCustomDelimiter.setSelected(true);
        ActionListener okListener = e -> {
            dispose();

            try {
                GroupTreeNode autoGroupsRoot = GroupTreeNode.fromGroup(
                        new ExplicitGroup(Localization.lang("Automatically created groups"),
                                GroupHierarchyType.INCLUDING,
                                Globals.prefs.getKeywordDelimiter()));
                Set<String> keywords;
                String fieldText = field.getText().toLowerCase().trim();
                if (this.keywords.isSelected()) {
                    if (useCustomDelimiter.isSelected()) {
                        keywords = GroupsUtil.findDeliminatedWordsInField(panel.getDatabase(), fieldText,
                                deliminator.getText());
                    } else {
                        keywords = GroupsUtil.findAllWordsInField(panel.getDatabase(), fieldText, remove.getText());

                    }
                } else if (authors.isSelected()) {
                    List<String> fields = new ArrayList<>(2);
                    fields.add(FieldName.AUTHOR);
                    keywords = GroupsUtil.findAuthorLastNames(panel.getDatabase(), fields);
                    fieldText = FieldName.AUTHOR;
                } else { // editors.isSelected() as it is a radio button group.
                    List<String> fields = new ArrayList<>(2);
                    fields.add(FieldName.EDITOR);
                    keywords = GroupsUtil.findAuthorLastNames(panel.getDatabase(), fields);
                    fieldText = FieldName.EDITOR;
                }

                LatexToUnicodeFormatter formatter = new LatexToUnicodeFormatter();

                for (String keyword : keywords) {
                    KeywordGroup group = new KeywordGroup(formatter.format(keyword), fieldText, keyword, false, false,
                            GroupHierarchyType.INDEPENDENT, Globals.prefs.getKeywordDelimiter());
                    autoGroupsRoot.addChild(GroupTreeNode.fromGroup(group));
                }

                autoGroupsRoot.moveTo(m_groupsRoot.getNode());
                NamedCompound ce = new NamedCompound(Localization.lang("Automatically create groups"));
                UndoableAddOrRemoveGroup undo = new UndoableAddOrRemoveGroup(m_groupsRoot, new GroupTreeNodeViewModel(autoGroupsRoot), UndoableAddOrRemoveGroup.ADD_NODE);
                ce.addEdit(undo);

                panel.markBaseChanged(); // a change always occurs
                frame.output(Localization.lang("Created groups."));
                ce.end();
                panel.getUndoManager().addEdit(ce);
            } catch (IllegalArgumentException exception) {
                frame.showMessage(exception.getLocalizedMessage());
            }
        };
        remove.addActionListener(okListener);
        field.addActionListener(okListener);
        field.addCaretListener(this);
        AbstractAction cancelAction = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        };
        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(cancelAction);
        ok.addActionListener(okListener);
        // Key bindings:
        JPanel main = new JPanel();
        ActionMap am = main.getActionMap();
        InputMap im = main.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.getKeyPrefs().getKey(KeyBinding.CLOSE_DIALOG), "close");
        am.put("close", cancelAction);

        ButtonGroup bg = new ButtonGroup();
        bg.add(keywords);
        bg.add(authors);
        bg.add(editors);
        keywords.setSelected(true);

        FormBuilder b = FormBuilder.create();
        b.layout(new FormLayout("left:20dlu, 4dlu, left:pref, 4dlu, fill:60dlu",
                "p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p, 2dlu, p"));
        b.add(keywords).xyw(1, 1, 5);
        b.add(Localization.lang("Field to group by") + ":").xy(3, 3);
        b.add(field).xy(5, 3);
        b.add(Localization.lang("Characters to ignore") + ":").xy(3, 5);
        b.add(remove).xy(5, 5);
        b.add(useCustomDelimiter).xy(3, 7);
        b.add(deliminator).xy(5, 7);
        b.add(authors).xyw(1, 9, 5);
        b.add(editors).xyw(1, 11, 5);
        b.build();
        b.border(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel opt = new JPanel();
        ButtonBarBuilder bb = new ButtonBarBuilder(opt);
        bb.addGlue();
        bb.addButton(ok);
        bb.addButton(cancel);
        bb.addGlue();

        main.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        opt.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        getContentPane().add(main, BorderLayout.CENTER);
        getContentPane().add(b.getPanel(), BorderLayout.CENTER);
        getContentPane().add(opt, BorderLayout.SOUTH);

        updateComponents();
        pack();
        setLocationRelativeTo(frame);
    }

    @Override
    public void caretUpdate(CaretEvent e) {
        updateComponents();
    }

    private void updateComponents() {
        String groupField = field.getText().trim();
        ok.setEnabled(groupField.matches("\\w+"));
    }
}
