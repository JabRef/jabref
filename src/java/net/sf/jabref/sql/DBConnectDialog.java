/*
 * DBConnectDialog.java
 *
 * Created on October 2, 2007, 10:39 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.sf.jabref.sql;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JOptionPane;

import net.sf.jabref.Globals;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.DefaultFormBuilder;
import com.jgoodies.forms.layout.FormLayout;


/**
 * Dialog box for collecting database connection strings from the user
 *
 * @author pattonlk
 */
public class DBConnectDialog extends JDialog {

    // labels
    JLabel lblServerType     = new JLabel();
    JLabel lblServerHostname = new JLabel();
    JLabel lblDatabase       = new JLabel();
    JLabel lblUsername       = new JLabel();
    JLabel lblPassword       = new JLabel();

    // input fields
    JComboBox cmbServerType = new JComboBox();
    JTextField txtServerHostname = new JTextField(40);
    JTextField txtDatabase = new JTextField(40);
    JTextField txtUsername = new JTextField(40);        
    JPasswordField pwdPassword = new JPasswordField(40);
    JButton btnConnect = new JButton();
    JButton btnCancel = new JButton();

    // array for holding components on left-hand and right-hand sides
    ArrayList<JLabel> lhs = new ArrayList<JLabel>();
    ArrayList<JComponent> rhs = new ArrayList<JComponent>();

    DBStrings dbStrings = new DBStrings();

    private boolean connectToDB = false;

    /** Creates a new instance of DBConnectDialog */
    public DBConnectDialog( JFrame parent, DBStrings dbs) {

        super(parent, Globals.lang("Connect to SQL Database"), true);

        this.setResizable(false);
        this.setLocationRelativeTo(parent);

        dbStrings = dbs;

        // build collections of components
        lhs.add(lblServerType);
        lhs.add(lblServerHostname);
        lhs.add(lblDatabase);
        lhs.add(lblUsername);
        lhs.add(lblPassword);

        rhs.add(cmbServerType);
        rhs.add(txtServerHostname);
        rhs.add(txtDatabase);
        rhs.add(txtUsername);
        rhs.add(pwdPassword);

        // setup label text
        lblServerType.setText(Globals.lang("Server Type :"));
        lblServerHostname.setText(Globals.lang("Server Hostname :"));
        lblDatabase.setText(Globals.lang("Database :"));
        lblUsername.setText(Globals.lang("Username :"));
        lblPassword.setText(Globals.lang("Password :"));

        // set label text alignment
        for (JLabel label : lhs){
            label.setHorizontalAlignment(JLabel.RIGHT);
        }
        
        // set button text
        btnConnect.setText(Globals.lang("Connect"));
        btnCancel.setText(Globals.lang("Cancel"));

        // init input fields to current DB strings
        String[] srv = dbStrings.getServerTypes();
        for (int i=0; i<srv.length; i++) {
           cmbServerType.addItem(srv[i]);
        }

        txtServerHostname.setText(dbStrings.getServerHostname());
        txtDatabase.setText(dbStrings.getDatabase());
        txtUsername.setText(dbStrings.getUsername());
        pwdPassword.setText(dbStrings.getPassword());


        // construct dialog
        DefaultFormBuilder builder = new DefaultFormBuilder(new
                                 FormLayout("right:pref, 4dlu, fill:pref", ""));

        builder.getPanel().setBorder(BorderFactory.createEmptyBorder(5,5,5,5));


        // add labels and input fields
        builder.append(lblServerType);
        builder.append(cmbServerType);
        builder.nextLine();
        builder.append(lblServerHostname);
        builder.append(txtServerHostname);
        builder.nextLine();
        builder.append(lblDatabase);
        builder.append(txtDatabase);
        builder.nextLine();
        builder.append(lblUsername);
        builder.append(txtUsername);
        builder.nextLine();
        builder.append(lblPassword);
        builder.append(pwdPassword);
        builder.nextLine();

        // add the panel to the CENTER of your dialog:
        getContentPane().add(builder.getPanel(), BorderLayout.CENTER);

        // add buttons are added in a similar way:
        ButtonBarBuilder bb = new ButtonBarBuilder();
        bb.addGlue();
        bb.addGridded(btnConnect);
        bb.addGridded(btnCancel);
        bb.addGlue();

        // add the buttons to the SOUTH of your dialog:
        getContentPane().add(bb.getPanel(), BorderLayout.SOUTH);
        pack();

        ActionListener connectAction = new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                String errorMessage = checkInput();

                if ( errorMessage==null) {
                    storeSettings();
                    setVisible(false);
                    setConnectToDB(true);
                } else {
                    JOptionPane.showMessageDialog(null, errorMessage,
                            "Input Error", JOptionPane.ERROR_MESSAGE);
                }

            }
        };

        btnConnect.addActionListener(connectAction);
        txtDatabase.addActionListener(connectAction);
        txtServerHostname.addActionListener(connectAction);
        txtUsername.addActionListener(connectAction);
        pwdPassword.addActionListener(connectAction);

        btnCancel.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
                setConnectToDB(false);
            }
        });

    }

    /**
     * Checks the user input, and ensures that required fields have entries
     *
     * @return 
     *      Appropriate error message to be displayed to user
     */
    private String checkInput () {

        String[] fields = {"Server Hostname", "Database", "Username"};
        String[] errors = new String[fields.length];
        int cnt = 0;

        if (txtServerHostname.getText().trim().equals("")) {
            errors[cnt] = fields[0];
            cnt++;
        }

        if (txtDatabase.getText().trim().equals("")) {
            errors[cnt] = fields[1];
            cnt++;
        }

        if (txtUsername.getText().trim().equals("")) {
            errors[cnt] = fields[2];
            cnt++;
        }

        String errMsg = Globals.lang("Please specify the ");

        switch (cnt) {
            case 0:
                errMsg = null;
                break;
            case 1:
                errMsg = errMsg + errors[0] + ".";
                break;
            case 2:
                errMsg = errMsg + errors[0] + " and " + errors[1] + "."; 
                break;
            case 3:
                errMsg = errMsg + errors[0] + ", " +  errors[1] 
                      + ", and " + errors[2] + ".";
                break;
            default:

        }

        return errMsg;
    }

    /**
     * Save user input.
     */
    private void storeSettings () {
        dbStrings.setServerType(cmbServerType.getSelectedItem().toString());
        dbStrings.setServerHostname(txtServerHostname.getText());
        dbStrings.setDatabase(txtDatabase.getText());
        dbStrings.setUsername(txtUsername.getText());

        char[] pwd = pwdPassword.getPassword();
        String tmp = "";
        for (int i=0; i<pwd.length; i++) {
            tmp = tmp + pwd[i];
        }
        dbStrings.setPassword(tmp);
        tmp = "";
        Arrays.fill(pwd, '0');

    }

    public DBStrings getDBStrings() {
        return dbStrings;
    }

    public void setDBStrings(DBStrings dbStrings) { 
        this.dbStrings = dbStrings;
    }

    public boolean getConnectToDB() {
        return connectToDB;
    }

    public void setConnectToDB(boolean connectToDB) {
        this.connectToDB = connectToDB;
    }

}