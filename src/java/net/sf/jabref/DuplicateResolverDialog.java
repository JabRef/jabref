package net.sf.jabref;

import javax.swing.JDialog;
import java.awt.HeadlessException;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import javax.swing.*;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


/**
 * <p>Title: </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author not attributable
 * @version 1.0
 */

public class DuplicateResolverDialog extends JDialog {

  public static final int
      KEEP_BOTH = 0,
      KEEP_UPPER = 1,
      KEEP_LOWER = 2;

  PreviewPanel p1, p2;
  GridBagLayout gbl = new GridBagLayout();
  GridBagConstraints con = new GridBagConstraints();
  JButton first = new JButton(Globals.lang("Keep upper")),
      second = new JButton(Globals.lang("Keep lower")),
      both = new JButton(Globals.lang("Keep both"));
  JPanel options = new JPanel(),
      main = new JPanel();
  int status = KEEP_BOTH;

  public DuplicateResolverDialog(JabRefFrame frame, BibtexEntry one, BibtexEntry two) throws HeadlessException {
    super(frame, Globals.lang("Resolve duplicate entries"), true);

    p1 = new PreviewPanel(one);
    p2 = new PreviewPanel(two);
    //getContentPane().setLayout();
    main.setLayout(gbl);
    con.insets = new Insets(10,10,10,10);
    con.fill = GridBagConstraints.BOTH;
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.weightx = 1;
    con.weighty = 1;
    JScrollPane sp = new JScrollPane(p1);
    gbl.setConstraints(sp, con);
    main.add(sp);
    sp = new JScrollPane(p2);
    gbl.setConstraints(sp, con);
    main.add(sp);

    options.add(first);
    options.add(second);
    options.add(both);

    first.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_UPPER;
        dispose();
      }
    });

    second.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_LOWER;
        dispose();
      }
    });

    both.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_BOTH;
        dispose();
      }
    });

    getContentPane().add(main, BorderLayout.CENTER);
    getContentPane().add(options, BorderLayout.SOUTH);
    pack();
    Util.placeDialog(this, frame);
  }

  public int getSelected() {
    return status;
  }

  public static int resolveDuplicate(JabRefFrame frame, BibtexEntry one, BibtexEntry two) {
    DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two);
    drd.show();
    return drd.getSelected();
  }

}
