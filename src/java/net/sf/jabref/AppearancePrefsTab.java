package net.sf.jabref;

import java.util.Vector;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import com.jgoodies.forms.layout.*;
import com.jgoodies.forms.factories.*;
import com.jgoodies.forms.builder.*;
import net.sf.jabref.gui.ColorSetupPanel;

class AppearancePrefsTab extends JPanel implements PrefsTab {

    JabRefPreferences _prefs;
    private JCheckBox colorCodes, antialias;
    private GridBagLayout gbl = new GridBagLayout();
    private JButton fontButton = new JButton(Globals.lang("Set table font"));
    private ColorSetupPanel colorPanel = new ColorSetupPanel();
    private Font font = GUIGlobals.CURRENTFONT;
    private int oldMenuFontSize;
    private JTextField fontSize;

    /**
     * Customization of appearance parameters.
     *
     * @param prefs a <code>JabRefPreferences</code> value
     */
    public AppearancePrefsTab(JabRefPreferences prefs) {
        _prefs = prefs;
         setLayout(new BorderLayout());

        // Font sizes:
        fontSize = new JTextField();


        colorCodes = new JCheckBox(Globals.lang
                   ("Color codes for required and optional fields"));
        antialias = new JCheckBox(Globals.lang
                  ("Use antialiasing font"));

        FormLayout layout = new FormLayout
                ("1dlu, 8dlu, left:pref, 4dlu, fill:pref, 4dlu, fill:60dlu, 4dlu, fill:pref",
                        "");
        DefaultFormBuilder builder = new DefaultFormBuilder(layout);
        builder.setLeadingColumnOffset(2);
        JLabel lab;
        builder.appendSeparator(Globals.lang("General"));
        JPanel p1 = new JPanel();
        lab = new JLabel(Globals.lang("Menu and label font size") + ":");
        p1.add(lab);
        p1.add(fontSize);
        builder.append(p1);
        builder.nextLine();
        builder.append(antialias);
        builder.nextLine();
        builder.appendSeparator(Globals.lang("Table appearance"));
        builder.append(colorCodes);
        builder.nextLine();
        builder.append(fontButton);
        builder.nextLine();
        builder.append(colorPanel);



    JPanel upper = new JPanel(),
        sort = new JPanel(),
        namesp = new JPanel(),
            iconCol = new JPanel();
    upper.setLayout(gbl);
    sort.setLayout(gbl);
        namesp.setLayout(gbl);
        iconCol.setLayout(gbl);



    fontButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            // JDialog dl = new EntryCustomizationDialog(ths);
            Font f=new FontSelectorDialog
                (null, GUIGlobals.CURRENTFONT).getSelectedFont();
            if(f==null)
                return;
            else
                font = f;
        }
        });
    /*menuFontButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             Font f=new FontSelectorDialog
                 (null, menuFont).getSelectedFont();
             if(f==null)
                 return;
             else
                 menuFont = f;
         }
         });*/

    JPanel pan = builder.getPanel();
    pan.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
    add(pan, BorderLayout.CENTER);
    }

    public void setValues() {
        colorCodes.setSelected(_prefs.getBoolean("tableColorCodesOn"));
        antialias.setSelected(_prefs.getBoolean("antialias"));
        fontSize.setText("" + _prefs.getInt("menuFontSize"));
        oldMenuFontSize = _prefs.getInt("menuFontSize");

        colorPanel.setValues();
    }

    /**
     * Store changes to table preferences. This method is called when
     * the user clicks Ok.
     *
     */
    public void storeSettings() {

        _prefs.putBoolean("tableColorCodesOn", colorCodes.isSelected());
        _prefs.putBoolean("antialias", antialias.isSelected());
        _prefs.put("fontFamily", font.getFamily());
        _prefs.putInt("fontStyle", font.getStyle());
        _prefs.putInt("fontSize", font.getSize());
        GUIGlobals.CURRENTFONT = font;
        colorPanel.storeSettings();
        try {
            int size = Integer.parseInt(fontSize.getText());
            if (size != oldMenuFontSize) {
                _prefs.putInt("menuFontSize", size);
                JOptionPane.showMessageDialog(null, Globals.lang("You have changed the menu and label font size. "
                        + "You must restart JabRef for this to come into effect."), Globals.lang("Changed font settings"),
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
    }

    public boolean readyToClose() {
        try {
            // Test if font size is a number:
            int size = Integer.parseInt(fontSize.getText());

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog
                    (null, Globals.lang("You must enter an integer value in the text field for") + " '" +
                            Globals.lang("Menu and label font size") + "'", Globals.lang("Changed font settings"),
                            JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;

    }

}
