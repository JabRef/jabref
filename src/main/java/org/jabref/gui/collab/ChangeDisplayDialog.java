package org.jabref.gui.collab;

import java.awt.BorderLayout;
import java.awt.Insets;
import java.util.Collections;
import java.util.Enumeration;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefDialog;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.database.BibDatabase;

class ChangeDisplayDialog extends JabRefDialog implements TreeSelectionListener {

    private final JTree tree;
    private final JPanel infoPanel = new JPanel();
    private final JCheckBox cb = new JCheckBox(Localization.lang("Accept change"));
    private final JLabel rootInfo = new JLabel(Localization.lang("Select the tree nodes to view and accept or reject changes") + '.');
    private ChangeViewModel selected;
    private JComponent infoShown;
    private boolean okPressed;


    public ChangeDisplayDialog(JFrame owner, final BasePanel panel,
            BibDatabase secondary, final DefaultMutableTreeNode root) {
        super(owner, Localization.lang("External changes"), true, ChangeDisplayDialog.class);
        BibDatabase localSecondary;

        // Just to be sure, put in an empty secondary base if none is given:
        if (secondary == null) {
            localSecondary = new BibDatabase();
        } else {
            localSecondary = secondary;
        }
        tree = new JTree(root);
        tree.addTreeSelectionListener(this);

        JSplitPane pane = new JSplitPane();
        pane.setLeftComponent(new JScrollPane(tree));
        JPanel infoBorder = new JPanel();
        pane.setRightComponent(infoBorder);

        cb.setMargin(new Insets(2, 2, 2, 2));
        cb.setEnabled(false);
        infoPanel.setLayout(new BorderLayout());
        infoBorder.setLayout(new BorderLayout());
        infoBorder.setBorder(BorderFactory.createEtchedBorder());
        infoBorder.add(infoPanel, BorderLayout.CENTER);
        setInfo(rootInfo);
        infoPanel.add(cb, BorderLayout.SOUTH);

        JButton ok = new JButton(Localization.lang("OK"));
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(ok);
        JButton cancel = new JButton(Localization.lang("Cancel"));
        buttonPanel.add(cancel);

        getContentPane().add(pane, BorderLayout.CENTER);
        getContentPane().add(buttonPanel, BorderLayout.SOUTH);

        cb.addChangeListener(e -> {
            if (selected != null) {
                selected.setAccepted(cb.isSelected());
            }
        });

        cancel.addActionListener(e -> dispose());

        ok.addActionListener(e -> {

            // Perform all accepted changes:
            // Store all edits in an Undoable object:
            NamedCompound ce = new NamedCompound(Localization.lang("Merged external changes"));
            Enumeration<ChangeViewModel> enumer = root.children();
            boolean anyDisabled = false;
            for (ChangeViewModel c : Collections.list(enumer)) {
                boolean allAccepted = false;
                if (c.isAcceptable() && c.isAccepted()) {
                    allAccepted = c.makeChange(panel, localSecondary, ce);
                }

                if (!allAccepted) {
                    anyDisabled = true;
                }
            }
            ce.end();
            panel.getUndoManager().addEdit(ce);
            if (anyDisabled) {
                panel.markBaseChanged();
            }
            panel.markExternalChangesAsResolved();
            dispose();
            okPressed = true;
        });

        pack();
    }

    public boolean isOkPressed() {
        return okPressed;
    }

    private void setInfo(JComponent comp) {
        if (infoShown != null) {
            infoPanel.remove(infoShown);
        }
        infoShown = comp;
        infoPanel.add(infoShown, BorderLayout.CENTER);
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    /**
     * valueChanged
     *
     * @param e TreeSelectionEvent
     */
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        Object o = tree.getLastSelectedPathComponent();
        if (o instanceof ChangeViewModel) {
            selected = (ChangeViewModel) o;
            setInfo(selected.description());
            cb.setSelected(selected.isAccepted());
            cb.setEnabled(selected.isAcceptable());
        } else {
            setInfo(rootInfo);
            selected = null;
            cb.setEnabled(false);
        }
    }
}
