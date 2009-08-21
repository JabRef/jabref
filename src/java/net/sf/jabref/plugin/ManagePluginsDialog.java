/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.sf.jabref.plugin;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.swing.EventTableModel;
import com.jgoodies.forms.builder.ButtonBarBuilder;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import javax.swing.*;

import net.sf.jabref.GUIGlobals;
import net.sf.jabref.Globals;
import net.sf.jabref.HelpAction;
import net.sf.jabref.JabRefFrame;
import net.sf.jabref.net.URLDownload;
import net.sf.jabref.gui.FileDialogs;
import net.sf.jabref.plugin.PluginInstaller.NameAndVersion;

/**
 *
 * @author alver
 */
public class ManagePluginsDialog {

    private JabRefFrame frame;
    private JDialog diag;
    private SortedList<NameAndVersion> plugins, pluginsOther;
    private JTable table, tableOther;
    private TableFormat tableFormat, tableFormatOther;
    private JButton close = new JButton(Globals.lang("Close")),
            install = new JButton(Globals.lang("Install plugin")),
            download = new JButton(Globals.lang("Download plugin")),
            remove = new JButton(Globals.lang("Delete")),
            help = new JButton(Globals.lang("Help"));
    
    
    public ManagePluginsDialog(JabRefFrame frame) {
        this.frame = frame;
        diag = new JDialog(frame, Globals.lang("Plugin manager"), false);
        help.addActionListener(new HelpAction(Globals.helpDiag, GUIGlobals.pluginHelp, "Help"));
        JPanel pan = new JPanel();
        pan.setLayout(new BorderLayout());

        JLabel lab = new JLabel
                (Globals.lang("Plugins installed in your user plugin directory (%0):",
                PluginCore.userPluginDir.getPath()));
        lab.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pan.add(lab, BorderLayout.NORTH);

        // Table for user dir plugins:
        table = new JTable();
        // Table for other plugiuns
        tableOther = new JTable();
        tableOther.setRowSelectionAllowed(false);
        tableOther.setColumnSelectionAllowed(false);
        tableOther.setCellSelectionEnabled(false);
        buildList();
        table.setPreferredScrollableViewportSize(new Dimension(500, 200));
        tableOther.setPreferredScrollableViewportSize(new Dimension(500, 100));
        pan.add(new JScrollPane(table), BorderLayout.CENTER);
        diag.getContentPane().add(pan, BorderLayout.NORTH);
        pan = new JPanel();
        pan.setLayout(new BorderLayout());
        lab = new JLabel(Globals.lang("Plugins installed in other locations:"));
        lab.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        pan.add(lab, BorderLayout.NORTH);
        pan.add(new JScrollPane(tableOther), BorderLayout.CENTER);

        diag.getContentPane().add(pan, BorderLayout.CENTER);
        
        ButtonBarBuilder b = new ButtonBarBuilder();
        b.addGlue();
        b.addGridded(install);
        b.addGridded(download);
        b.addGridded(remove);
        b.addGridded(close);
        b.addRelatedGap();
        b.addGridded(help);
        
        b.addGlue();
        b.getPanel().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        diag.getContentPane().add(b.getPanel(), BorderLayout.SOUTH);
        diag.pack();
        diag.setLocationRelativeTo(frame);
        
        install.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                installPlugin();
            }
        });
        
        download.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                downloadPlugin();
            }
        });
        
        Action closeListener = new AbstractAction() {
            public void actionPerformed(ActionEvent arg0) {
                diag.dispose();
            }
        };
        close.addActionListener(closeListener);
        
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                removeSelected();
            }
        });
        
        // Key bindings:
        ActionMap am = b.getPanel().getActionMap();
        InputMap im = b.getPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        im.put(Globals.prefs.getKey("Close dialog"), "close");
        am.put("close", closeListener);

    }
    
    private void removeSelected() {
        int[] sel = table.getSelectedRows();
        if (sel.length > 0) {
            String message = Globals.lang("Delete the %0 selected plugins?", String.valueOf(sel.length));
            String title = Globals.lang("Delete plugins");
            if (sel.length == 1) {
                message = Globals.lang("Delete the selected plugin?");
                title = Globals.lang("Delete plugin");
            }
            int reply = JOptionPane.showConfirmDialog(frame, message, title, JOptionPane.YES_NO_OPTION);
            if (reply != JOptionPane.YES_OPTION)
                return;
            boolean success = true;
            for (int i=0; i<sel.length; i++) {
                PluginInstaller.NameAndVersion nav = plugins.get(sel[i]);
                success = PluginInstaller.deletePlugin(nav) & success;
            }
            if (!success) {

                JOptionPane.showMessageDialog(frame, sel.length > 1 ?
                        Globals.lang("Plugins will be deleted next time JabRef starts up.")
                        : Globals.lang("Plugin will be deleted next time JabRef starts up."),
                        sel.length > 1 ? Globals.lang("Delete plugins") : Globals.lang("Delete plugin"),
                        JOptionPane.INFORMATION_MESSAGE);
            }
            buildList();
        }
    }
    
    private void buildList() {
        plugins = new SortedList<NameAndVersion>(PluginInstaller.findInstalledPlugins());
        // Move those plugins that are not installed in the user plugin dir to another list:
        EventList<NameAndVersion> outsideUserDir = new BasicEventList<NameAndVersion>();
        for (Iterator<NameAndVersion> i = plugins.iterator(); i.hasNext();) {
            NameAndVersion nav = i.next();
            if (!nav.inUserDirectory) {
                outsideUserDir.add(nav);
                i.remove();
            }
        }
        pluginsOther = new SortedList<NameAndVersion>(outsideUserDir);
        tableFormatOther = new PluginTableFormat();
        EventTableModel tableModel = new EventTableModel(pluginsOther, tableFormatOther);
        tableOther.setModel(tableModel);
        tableOther.getColumnModel().getColumn(0).setPreferredWidth(200);
        tableOther.getColumnModel().getColumn(1).setPreferredWidth(50);
        tableOther.getColumnModel().getColumn(2).setPreferredWidth(50);

        tableFormat = new PluginTableFormat();
        EventTableModel tableModelOther = new EventTableModel(plugins, tableFormat);
        table.setModel(tableModelOther);
        table.getColumnModel().getColumn(0).setPreferredWidth(200);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(50);
    }
    
    public void setVisible(boolean visible) {
        diag.setVisible(visible);
    }
    
    public void installPlugin() {
        String filename = FileDialogs.getNewFile(frame, new File(System.getProperty("user.home")),
            ".jar", JFileChooser.OPEN_DIALOG, false);
        if (filename == null)
            return;
        File f = new File(filename);
        if (f != null) {
            if (!f.exists()) {
                JOptionPane.showMessageDialog(frame, Globals.lang("File not found")+".",
                        Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
            } else {
                installFromFile(f);
            }
        }

    }

    public void downloadPlugin() {
        String url = JOptionPane.showInputDialog(Globals.lang("Enter download URL"));
        if (url == null)
            return;
        try {
            installFromURL(new URL(url));
        } catch (MalformedURLException e) {
            JOptionPane.showMessageDialog(frame, Globals.lang("Invalid URL"),
                    Globals.lang("Plugin installer"), JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public void installFromURL(URL url) {
        try {
            File tmpFile = File.createTempFile("jabref-plugin", ".jar");
            tmpFile.deleteOnExit();
            URLDownload ud = new URLDownload(frame, url, tmpFile);
            ud.download();
            String path = url.getPath();
            int pos = path.lastIndexOf('/');
            if ((pos >= 0) && (pos < path.length()-1))
                path = path.substring(pos+1);
            PluginInstaller.installPlugin(frame, tmpFile, path);
            tmpFile.delete();
            buildList();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public void installFromFile(File file) {
        PluginInstaller.installPlugin(frame, file, null);
        buildList();
    }
    
    class PluginTableFormat implements TableFormat<NameAndVersion> {

        public int getColumnCount() {
            return 3;
        }

        public String getColumnName(int col) {
            if (col == 0)
                return Globals.lang("Plugin name");
            else if (col == 1)
                return Globals.lang("Version");
            else return Globals.lang("Status");
        }

        public Object getColumnValue(NameAndVersion nav, int col) {
            if (col == 0)
                return nav.name;
            else if (col == 1) {
                if (!nav.version.equals(PluginInstaller.VersionNumber.ZERO))
                    return nav.version.toString();
                else return Globals.lang("Unknown");
            }
            else {
                int status = nav.getStatus();
                if (status == 0)
                    return Globals.lang("Not loaded");
                else if (status == 1)
                    return Globals.lang("Loaded");
                else
                    return Globals.lang("Error");
            }
        }
        
    }
}
