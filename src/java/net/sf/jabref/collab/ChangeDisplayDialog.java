package net.sf.jabref.collab;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import net.sf.jabref.Globals;
import java.awt.Insets;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Enumeration;
import net.sf.jabref.BasePanel;
import net.sf.jabref.undo.NamedCompound;

public class ChangeDisplayDialog extends JDialog implements TreeSelectionListener {

  DefaultMutableTreeNode root;
  JTree tree;
  JPanel infoPanel = new JPanel(),
      buttonPanel = new JPanel(),
      infoBorder = new JPanel();
  JButton ok = new JButton(Globals.lang("Ok")),
      cancel = new JButton(Globals.lang("Cancel"));
  JCheckBox cb = new JCheckBox(Globals.lang("Accept change"));
  JLabel rootInfo = new JLabel(Globals.lang("Select the tree nodes to view and accept or reject changes")+".");
  Change selected = null;
  JComponent infoShown = null;

  public ChangeDisplayDialog(JFrame owner, final BasePanel panel, final DefaultMutableTreeNode root) {
    super(owner, Globals.lang("External changes"), true);
    this.root = root;
    tree = new JTree(root);
    tree.addTreeSelectionListener(this);
    JSplitPane pane = new JSplitPane();
    pane.setLeftComponent(new JScrollPane(tree));
    pane.setRightComponent(infoBorder);

    cb.setMargin(new Insets(2, 2, 2, 2));
    cb.setEnabled(false);
    infoPanel.setLayout(new BorderLayout());
    infoBorder.setLayout(new BorderLayout());
    infoBorder.setBorder(BorderFactory.createEtchedBorder());
    infoBorder.add(infoPanel, BorderLayout.CENTER);
    setInfo(rootInfo);
    infoPanel.add(cb, BorderLayout.SOUTH);

    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    getContentPane().add(pane, BorderLayout.CENTER);
    getContentPane().add(buttonPanel, BorderLayout.SOUTH);

    cb.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        if (selected != null)
          selected.setAccepted(cb.isSelected());
      }
    });
    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        dispose();
      }
    });
    ok.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {

        // Perform all accepted changes:
        // Store all edits in an Undoable object:
        NamedCompound ce = new NamedCompound(Globals.lang("Merged external changes"));
        Enumeration enumer = root.children();
        for (; enumer.hasMoreElements();) {
          Change c = (Change)enumer.nextElement();
          if (c.isAcceptable() && c.isAccepted())
            c.makeChange(panel, ce);
        }
        ce.end();
        panel.undoManager.addEdit(ce);
        panel.markBaseChanged();
        panel.setUpdatedExternally(false);
        dispose();
      }
    });

    pack();
  }

  private void setInfo(JComponent comp) {
    if (infoShown != null)
      infoPanel.remove(infoShown);
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
  public void valueChanged(TreeSelectionEvent e) {
    Object o = tree.getLastSelectedPathComponent();
    if (o instanceof Change) {
      selected = (Change)o;
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
