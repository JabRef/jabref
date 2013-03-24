package spl.gui;


import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import net.sf.jabref.Globals;
import net.sf.jabref.MetaData;
import net.sf.jabref.Util;

import org.sciplore.beans.Document;

import spl.DocumentsWrapper;
import spl.SplWebClient;
import spl.listener.LabelLinkListener;
import spl.localization.LocalizationSupport;

import com.jgoodies.forms.builder.ButtonBarBuilder;

public class MetaDataListDialog extends JDialog {
    private JPanel contentPane;
    private JTable tableMetadata;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JScrollPane scrollPane;
    private JPanel panelWait;
    private JLabel labelFetch;
    private JLabel iconLabel;
    private JButton blankButton;
    private JLabel labelLogo;
    private JButton moreInformationButton;
    private JPanel panelMetadata;
    private DefaultTableModel tableModel;
    private int result;
    private Document xmlDocuments;
    private String fileName;
    private SplWebClient.WebServiceStatus webserviceStatus;
    private Component thisDialog;
    private boolean showBlankButton;
    private CardLayout cardLayou = new CardLayout();

    public MetaDataListDialog(String fileName, boolean showBlankButton) {
        $$$setupUI$$$();
        this.showBlankButton = showBlankButton;
        this.thisDialog = this;
        this.fileName = fileName;
        this.labelLogo.addMouseListener(new LabelLinkListener(this.labelLogo, "www.mr-dlib.org"));
        this.setTitle(LocalizationSupport.message("Mr._dLib_Metadata_Entries_Associated_With_PDF_File"));
        this.tableMetadata.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setContentPane(contentPane);
        pack();
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        buttonCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        blankButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onBlank();
            }
        });

        moreInformationButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onInfo();
            }
        });

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        contentPane.registerKeyboardAction(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        cardLayou.show(panelMetadata, "panelWait");
        //this.scrollPane.setVisible(false);
        //this.blankButton.setVisible(false);
        this.moreInformationButton.setVisible(true);
        this.setSize(616, 366);
    }

    private void onInfo() {
        try {
            Util.openExternalViewer(new MetaData(), "http://www.mr-dlib.org/docs/jabref_metadata_extraction_alpha.php", "url");
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    private void onBlank() {
        this.result = JOptionPane.NO_OPTION;
        dispose();
    }

    private void onOK() {
        this.result = JOptionPane.OK_OPTION;
        dispose();
    }

    private void onCancel() {
        this.result = JOptionPane.CANCEL_OPTION;
        dispose();
    }

    public void showDialog() {
        SwingWorker worker = new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground() {
                System.out.println("Starting Webclient...");
                webserviceStatus = SplWebClient.getMetaData(new File(fileName));
                return null;
            }

            @Override
            public void done() {
                if (webserviceStatus == SplWebClient.WebServiceStatus.OK) {
                    xmlDocuments = SplWebClient.metadata;
                    if (xmlDocuments != null /*&& xmlDocuments.getDocuments() != null && xmlDocuments.getDocuments().size() > 0*/) {
                        DocumentsWrapper documents = new DocumentsWrapper(xmlDocuments);
                        List<Vector> vectorList = documents.getDocuments();
                        for (Vector vector : vectorList) {
                            tableModel.addRow(vector);
                        }

                        tableMetadata.getSelectionModel().setSelectionInterval(0, 0);
                        cardLayou.show(panelMetadata, "scrollPane");
                        //panelWait.setVisible(false);
                        //scrollPane.setVisible(true);
                        moreInformationButton.setVisible(true);
                    } else {
                        iconLabel.setVisible(false);
                        labelFetch.setText(LocalizationSupport.message("No_metadata_found."));
                        blankButton.setVisible(showBlankButton);
                    }
                }
                if (webserviceStatus == SplWebClient.WebServiceStatus.NO_METADATA) {
                    iconLabel.setVisible(false);
                    labelFetch.setText(LocalizationSupport.message("No_metadata_found."));
                    blankButton.setVisible(showBlankButton);
                }
                if (webserviceStatus == SplWebClient.WebServiceStatus.UNAVAILABLE) {
                    iconLabel.setVisible(false);
                    labelFetch.setText(LocalizationSupport.message("Mr._dLib_web_service_is_temporarily_unavailable."));
                    blankButton.setVisible(showBlankButton);
                }
                if (webserviceStatus == SplWebClient.WebServiceStatus.OUTDATED) {
                    iconLabel.setVisible(false);
                    labelFetch.setText(LocalizationSupport.message("The_Mr._dLib_web_service_version_you_trying_to_access_is_outdated."));
                    blankButton.setVisible(showBlankButton);
                    JOptionPane.showMessageDialog(thisDialog, LocalizationSupport.message("This_JabRef_version_is_trying_to_access_an_old_version_of_Mr._dLib's_webservice_that_is_not_working_any_more.\nPlease_visit_http://jabref.sourceforge.net_or_http://www.mr-dlib.org_for_more_information_and_updates."), LocalizationSupport.message("Web_Service_Version_Outdated"), JOptionPane.INFORMATION_MESSAGE);
                }
                if (webserviceStatus == SplWebClient.WebServiceStatus.WEBSERVICE_DOWN) {
                    iconLabel.setVisible(false);
                    labelFetch.setText(LocalizationSupport.message("Mr._dLib_web_service_is_temporarily_down._Please_try_again_later."));
                    blankButton.setVisible(showBlankButton);
                }
                if (webserviceStatus == SplWebClient.WebServiceStatus.NO_INTERNET) {
                    iconLabel.setVisible(false);
                    labelFetch.setText(LocalizationSupport.message("No_Internet_Connection."));
                    blankButton.setVisible(showBlankButton);
                    JOptionPane.showMessageDialog(thisDialog, LocalizationSupport.message("You_are_not_connected_to_the_Internet._To_access_Mr._dLib_web_service_an_internet_connection_is_needed."), LocalizationSupport.message("No_Internet_Connection."), JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        worker.execute();
        this.pack();
        this.setVisible(true);
    }

    public Document getXmlDocuments() {
        return xmlDocuments;
    }

    private void createUIComponents() {
        this.tableModel = new MyTableModel();
        this.tableModel.addColumn(LocalizationSupport.message("Title"));
        this.tableModel.addColumn(LocalizationSupport.message("Author(s)"));
        this.tableModel.addColumn(LocalizationSupport.message("Published_Year"));
        this.tableMetadata = new JTable(this.tableModel);
    }

    public JTable getTableMetadata() {
        return tableMetadata;
    }

    public int getResult() {
        return result;
    }

    public JButton getBlankButton() {
        return blankButton;
    }

    private void $$$setupUI$$$() {
        createUIComponents();
        contentPane = new JPanel();
        contentPane.setLayout(new BorderLayout());
        labelLogo = new JLabel();
        labelLogo.setIcon(new ImageIcon(getClass().getResource("/spl/gui/mrdlib header.png")));
        labelLogo.setText("");
        contentPane.add(labelLogo, BorderLayout.NORTH);
        panelMetadata = new JPanel();
        panelMetadata.setLayout(cardLayou);


        panelMetadata.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        scrollPane = new JScrollPane();
        scrollPane.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null));
        tableMetadata.setAutoCreateRowSorter(false);
        tableMetadata.setEnabled(true);
        tableMetadata.setFillsViewportHeight(true);
        tableMetadata.setShowVerticalLines(true);
        scrollPane.setViewportView(tableMetadata);
        panelMetadata.add(scrollPane, "scrollPane");
        panelWait = new JPanel();
        panelWait.setLayout(new BorderLayout());
        panelWait.setBackground(new Color(-1));
        panelMetadata.add(panelWait, "panelWait");
        panelWait.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLoweredBevelBorder(), null));
        iconLabel = new JLabel();
        iconLabel.setBackground(new Color(-1));
        iconLabel.setHorizontalAlignment(0);
        iconLabel.setHorizontalTextPosition(11);
        iconLabel.setIcon(new ImageIcon(getClass().getResource("/spl/gui/ajax-loader.gif")));
        iconLabel.setText("");
        panelWait.add(iconLabel, BorderLayout.CENTER);
        labelFetch = new JLabel();
        labelFetch.setHorizontalAlignment(JLabel.CENTER);
        labelFetch.setFont(new Font(labelFetch.getFont().getName(), labelFetch.getFont().getStyle(), 13));
        labelFetch.setText(Globals.lang("Fetching Metadata..."));
        panelWait.add(labelFetch, BorderLayout.SOUTH);

        cardLayou.show(panelMetadata, "panelWait");
        panelMetadata.setPreferredSize(new Dimension(400, 200));
        contentPane.add(panelMetadata, BorderLayout.CENTER);

        buttonOK = new JButton(Globals.lang("Ok"));
        buttonCancel = new JButton(Globals.lang("Cancel"));
        moreInformationButton = new JButton(Globals.lang("More information"));
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addButton(moreInformationButton);
        bb.addButton(buttonOK);
        bb.addButton(buttonCancel);
        bb.addGlue();
        blankButton = new JButton();
        blankButton.setText("");
        contentPane.add(bb.getPanel(), BorderLayout.SOUTH);
        iconLabel.setLabelFor(scrollPane);


    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return contentPane;
    }

    public class MyTableModel extends DefaultTableModel {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

}
