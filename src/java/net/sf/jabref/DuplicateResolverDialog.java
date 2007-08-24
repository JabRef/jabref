package net.sf.jabref;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.StringWriter;

import javax.swing.*;

import net.sf.jabref.export.LatexFieldFormatter;


// created by : ?
//
// modified : r.nagel 2.09.2004
//            - insert close button

public class DuplicateResolverDialog extends JDialog {

    public static final int
        NOT_CHOSEN = -1,
        KEEP_BOTH = 0,
        KEEP_UPPER = 1,
        KEEP_LOWER = 2,
        AUTOREMOVE_EXACT = 3,
        BREAK      = 5,  // close
        IMPORT_AND_DELETE_OLD = 1,
        IMPORT_AND_KEEP_OLD = 0,
        DO_NOT_IMPORT = 2,
        DUPLICATE_SEARCH = 1,
        IMPORT_CHECK = 2,
        INSPECTION = 3,
        DUPLICATE_SEARCH_WITH_EXACT = 4;

    final Dimension DIM = new Dimension(650, 600);

    PreviewPanel p1, p2;
    JTextArea ta1, ta2;
    JTabbedPane tabbed = new JTabbedPane();
    GridBagLayout gbl = new GridBagLayout();
    GridBagConstraints con = new GridBagConstraints();
    JButton first, second, both,
        cancel = new JButton(Globals.lang("Cancel")),
        removeExact = null;
    JPanel options = new JPanel(),
        main = new JPanel(),
        source = new JPanel();
    int status = NOT_CHOSEN;
    boolean block = true;
    TitleLabel lab;

  public DuplicateResolverDialog(JFrame frame, BibtexEntry one, BibtexEntry two, int type) {
      super(frame, Globals.lang("Possible duplicate entries"), true);
      init(one, two, type);
      Util.placeDialog(this, frame);
  }

    public DuplicateResolverDialog(JDialog frame, BibtexEntry one, BibtexEntry two, int type) {
        super(frame, Globals.lang("Possible duplicate entries"), true);
        init(one, two, type);
        Util.placeDialog(this, frame);
    }

    private void init(BibtexEntry one, BibtexEntry two, int type) {
      switch (type) {
          case DUPLICATE_SEARCH:
              first = new JButton(Globals.lang("Keep upper"));
              second = new JButton(Globals.lang("Keep lower"));
              both = new JButton(Globals.lang("Keep both"));
              break;
          case INSPECTION:
              first = new JButton(Globals.lang("Remove old entry"));
              second = new JButton(Globals.lang("Remove entry from import"));
              both = new JButton(Globals.lang("Keep both"));
              break;
          case DUPLICATE_SEARCH_WITH_EXACT:
              first = new JButton(Globals.lang("Keep upper"));
              second = new JButton(Globals.lang("Keep lower"));
              both = new JButton(Globals.lang("Keep both"));
              removeExact = new JButton(Globals.lang("Automatically remove exact duplicates"));
              break;
          default:
              first = new JButton(Globals.lang("Import and remove old entry"));
                  second = new JButton(Globals.lang("Do not import entry"));
                  both = new JButton(Globals.lang("Import and keep old entry"));
      }

    String layout = Globals.prefs.get("preview0");
    p1 = new PreviewPanel(null, one, null, new MetaData(), layout);
    p2 = new PreviewPanel(null, two, null, new MetaData(), layout);
    ta1 = new JTextArea();
    ta2 = new JTextArea();
    ta1.setEditable(false);
    ta2.setEditable(false);

    //ta1.setPreferredSize(dim);
    //ta2.setPreferredSize(dim);

    setSourceView(one, two);

    //getContentPane().setLayout();
    main.setLayout(gbl);
    source.setLayout(gbl);
    con.insets = new Insets(10,10,0,10);
    con.fill = GridBagConstraints.BOTH;
    con.gridwidth = GridBagConstraints.REMAINDER;
    con.weightx = 1;
    con.weighty = 0;
    lab = new TitleLabel((type==DUPLICATE_SEARCH) ? "" :
                                  Globals.lang("Entry in current database"));
    gbl.setConstraints(lab, con);
    main.add(lab);
    con.weighty = 1;
    con.insets = new Insets(5,10,10,10);
    JScrollPane sp = new JScrollPane(p1);
    gbl.setConstraints(sp, con);
    main.add(sp);
    con.weighty = 0;
    con.insets = new Insets(10,10,0,10);
    lab = new TitleLabel((type==DUPLICATE_SEARCH) ? "" :
                                  Globals.lang("Entry in import"));
    gbl.setConstraints(lab, con);
    main.add(lab);
    con.weighty = 1;
    con.insets = new Insets(5,10,10,10);
    sp = new JScrollPane(ta1);
    gbl.setConstraints(sp, con);
    source.add(sp);
    sp = new JScrollPane(p2);
    gbl.setConstraints(sp, con);
    main.add(sp);
    sp = new JScrollPane(ta2);
    gbl.setConstraints(sp, con);
    source.add(sp);
    tabbed.add(Globals.lang("Short form"), main);
    tabbed.add(Globals.lang("Complete record"), source);
    if (removeExact != null)
        options.add(removeExact);
    options.add(first);
    options.add(second);
    options.add(both);
    if (type != IMPORT_CHECK) {
        options.add(Box.createHorizontalStrut(5));
        options.add(cancel);
    }

    first.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_UPPER;
        block = false;
        dispose();
      }
    });

    second.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_LOWER;
        block = false;
        dispose();
      }
    });

    both.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = KEEP_BOTH;
        block = false;
        dispose();
      }
    });

    if (removeExact != null)
        removeExact.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                status = AUTOREMOVE_EXACT;
                block = false;
                dispose();
            }
        });

    cancel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        status = BREAK;
        block = false;
        dispose();
      }
    });

    getContentPane().add(tabbed, BorderLayout.CENTER);
    getContentPane().add(options, BorderLayout.SOUTH);
    pack();


    if (getHeight() > DIM.height) {
        setSize(new Dimension(getWidth(), DIM.height));
    }
    if (getWidth() > DIM.width) {
        setSize(new Dimension(DIM.width, getHeight()));
    }


    both.requestFocus();

  }

  private void setSourceView(BibtexEntry one, BibtexEntry two) {
    try {
      StringWriter sw = new StringWriter();
      one.write(sw, new LatexFieldFormatter(), false);
      ta1.setText(sw.getBuffer().toString());
      sw = new StringWriter();
      two.write(sw, new LatexFieldFormatter(), false);
      ta2.setText(sw.getBuffer().toString());
    }
    catch (IOException ex) {
    }
  }



  public void setEntries(BibtexEntry newOne, BibtexEntry newTwo) {
    setSourceView(newOne, newTwo);
    p1.setEntry(newOne);
    p2.setEntry(newTwo);
    status = NOT_CHOSEN;
    p1.revalidate();
    p1.repaint();
    block = true;
  }

public boolean isBlocking() {
  return block;
}

  public int getSelected() {
    return status;
  }

  public static int resolveDuplicate(JFrame frame, BibtexEntry one, BibtexEntry two) {
    DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two,
                                                              DUPLICATE_SEARCH);
    drd.setVisible(true); // drd.show(); -> deprecated since 1.5
    return drd.getSelected();
  }

  public static int resolveDuplicate(JDialog frame, BibtexEntry one, BibtexEntry two) {
    DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, one, two,
                                                              DUPLICATE_SEARCH);
    drd.setVisible(true); // drd.show(); -> deprecated since 1.5
    return drd.getSelected();
  }

  public static int resolveDuplicateInImport(JabRefFrame frame, BibtexEntry existing,
                                           BibtexEntry imported) {
    DuplicateResolverDialog drd = new DuplicateResolverDialog(frame, existing, imported,
                                                              IMPORT_CHECK);
    drd.setVisible(true); // drd.show(); -> deprecated since 1.5
    return drd.getSelected();
  }

}
