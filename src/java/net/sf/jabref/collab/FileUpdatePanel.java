package net.sf.jabref.collab;

import net.sf.jabref.*;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;
import java.io.IOException;

public class FileUpdatePanel extends SidePaneComponent implements ActionListener {

  JButton test = new JButton(Globals.lang("Review changes"));
  BasePanel panel;
  JabRefFrame frame;
  SidePaneManager manager;
  
  public FileUpdatePanel(JabRefFrame frame, BasePanel panel,
                         SidePaneManager manager, JabRefPreferences prefs) {
    super(manager, GUIGlobals.saveIconFile, Globals.lang("File changed"));
    this.panel = panel;
    this.frame = frame;
    this.manager = manager;
    
    JPanel main = new JPanel();
    main.setLayout(new BorderLayout());
    JLabel lab = new JLabel("<html><center>Your file has<BR>been modified<BR>by another process!</center></html>",
            JLabel.CENTER);
    
    main.add(lab, BorderLayout.CENTER);
    main.add(test, BorderLayout.SOUTH);
    main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
    
    add(main, BorderLayout.CENTER);
    test.addActionListener(this);
  }

  /**
   * actionPerformed
   *
   * @param e ActionEvent
   */
  public void actionPerformed(ActionEvent e) {
      manager.hideAway(this);
    ChangeScanner scanner = new ChangeScanner(frame, panel); //, panel.database(), panel.metaData());
    //try {
      scanner.changeScan(panel.file());
      panel.setUpdatedExternally(false);
    //} catch (IOException ex) {
    //    ex.printStackTrace();
    //}
  }
}
