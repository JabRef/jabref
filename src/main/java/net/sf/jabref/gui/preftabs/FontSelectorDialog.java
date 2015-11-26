/*  Copyright (C) 2003-2015 JabRef contributors.
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sf.jabref.gui.preftabs;

/*
  Taken from JpicEdt
modified slightly by nizar batada for JabRef

 EepicViewFactory.java - February 11, 2002 - jPicEdt, a picture editor for LaTeX.
 copyright (C) 1999-2002 Sylvain Reynal
 Portions copyright (C) 2000, 2001 Slava Pestov
 Portions copyright (C) 1999 Jason Ginchereau

 D\uFFFDpartement de Physique
 Ecole Nationale Sup\uFFFDrieure de l'Electronique et de ses Applications (ENSEA)
 6, avenue du Ponceau
 F-95014 CERGY CEDEX

 Tel : +33 130 736 245
 Fax : +33 130 736 667
 e-mail : reynal@ensea.fr
 jPicEdt web page : http://trashx.ensea.fr/jpicedt/

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

import net.sf.jabref.logic.l10n.Localization;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * A font chooser widget.
 * @author Slava Pestov (jEdit), Sylvain Reynal
 * @since jpicedt 1.3.2.beta-9
 * @version $Id$
 * <p>
 * $Log$
 * Revision 1.8  2007/07/19 01:35:35  coezbek
 * JabRef 2.4b1 Plug-In System established. Yeah!
 *
 * Revision 1.7  2006/04/26 08:46:57  kiar
 * fix dialog.show() deprecation messages, change build.xml
 *
 * Revision 1.6  2004/02/27 23:28:41  mortenalver
 * Some code tidying, no effect on behaviour (hopefully)
 *
 * Revision 1.5  2004/02/24 23:30:18  mortenalver
 * Added more translations, and started work on a Replace string feature
 *
 * Revision 1.4  2004/02/17 09:14:02  mortenalver
 * Similar update in FontSelector preview.
 *
 * Revision 1.3  2004/02/17 07:35:22  mortenalver
 * Experimenting with antialiasing in table.
 *
 * Revision 1.2  2003/12/14 23:48:02  mortenalver
 * .
 *
 * Revision 1.1  2003/11/07 22:18:07  nbatada
 * modified it slightly from initial version
 *
 * Revision 1.1  2003/11/07 22:14:34  nbatada
 * modified it from initial version
 *
 * Revision 1.4  2003/11/02 01:51:06  reynal
 * Cleaned-up i18n labels
 *
 * Revision 1.3  2003/08/31 22:05:40  reynal
 *
 * Enhanced class interface for some widgets.
 *

 */

class FontSelector extends JButton {

    private static final String PLAIN = "plain";
    private static final String BOLD = "bold";
    private static final String BOLD_ITALIC = "bold-italic";
    private static final String ITALIC = "italic";


    /** init with a default font */
    public FontSelector() {
        this(new Font("SansSerif", Font.PLAIN, 10));
    }

    /** init with the given font */
    private FontSelector(Font font) {
        setFont(font);
        setRequestFocusEnabled(false);
        addActionListener(new ActionHandler());
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        updateText();
    }

    /**
     * update button's text content from the current button's font.
     */
    private void updateText() {
        Font font = getFont();
        String styleString;
        switch (font.getStyle()) {
        case Font.PLAIN:
            styleString = FontSelector.PLAIN;
            break;
        case Font.BOLD:
            styleString = FontSelector.BOLD;
            break;
        case Font.ITALIC:
            styleString = FontSelector.ITALIC;
            break;
        case Font.BOLD | Font.ITALIC:
            styleString = FontSelector.BOLD_ITALIC;
            break;
        default:
            styleString = "UNKNOWN!!!???";
            break;
        }

        setText(font.getFamily() + " " + font.getSize() + " " + styleString);
    }


    /**
     * button's action-listener ; open a FontSelectorDialog
     */
    private class ActionHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent evt) {
            Font font = new FontSelectorDialog(FontSelector.this, getFont()).getSelectedFont();
            if (font != null) {
                setFont(font);
            }
        }
    }

}

///////////////////////////////////////////////////////////////////////////////

public class FontSelectorDialog extends JDialog {

    private static final String PLAIN = "plain";
    private static final String BOLD = "bold";
    private static final String BOLD_ITALIC = "bold-italic";
    private static final String ITALIC = "italic";


    public FontSelectorDialog(Component comp, Font font) {

        //super(JOptionPane.getFrameForComponent(comp),jpicedt.Localizer.currentLocalizer().get("widget.FontSelector"),true); //
        super(JOptionPane.getFrameForComponent(comp), Localization.lang("FontSelector"), true); //
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 6, 6));

        JPanel familyPanel = createTextFieldAndListPanel(
                Localization.lang("Font Family"),
                familyField = new JTextField(),
                familyList = new JList<>(getFontList()));
        listPanel.add(familyPanel);

        String[] sizes = {"9", "10", "12", "14", "16", "18", "24"};
        JPanel sizePanel = createTextFieldAndListPanel(
                Localization.lang("Font Size"),
                sizeField = new JTextField(),
                sizeList = new JList<>(sizes));
        listPanel.add(sizePanel);

        String[] styles = {FontSelectorDialog.PLAIN, FontSelectorDialog.BOLD, FontSelectorDialog.ITALIC, FontSelectorDialog.BOLD_ITALIC};

        JPanel stylePanel = createTextFieldAndListPanel(
                Localization.lang("Font Style"),
                styleField = new JTextField(),
                styleList = new JList<>(styles));
        styleField.setEditable(false);
        listPanel.add(stylePanel);

        familyList.setSelectedValue(font.getFamily(), true);
        familyField.setText(font.getFamily());
        sizeList.setSelectedValue(String.valueOf(font.getSize()), true);
        sizeField.setText(String.valueOf(font.getSize()));
        styleList.setSelectedIndex(font.getStyle());
        styleField.setText(styleList.getSelectedValue());

        ListHandler listHandler = new ListHandler();
        familyList.addListSelectionListener(listHandler);
        sizeList.addListSelectionListener(listHandler);
        styleList.addListSelectionListener(listHandler);

        content.add(BorderLayout.NORTH, listPanel);

        //preview = new JLabel("Font Preview");

        /* --------------------------------------------------------
           |  Experimental addition by Morten Alver. I want to    |
           |  enable antialiasing in the preview field, since I'm |
           |  working on introducing this in the table view.      |
           -------------------------------------------------------- */
        preview = new JLabel(Localization.lang("Font Preview")) {
            @Override
            public void paint(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint
                (RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                super.paint(g2);
            }

        };

        preview.setBorder(new TitledBorder(Localization.lang("Font Preview")));

        updatePreview();

        Dimension prefSize = preview.getPreferredSize();
        prefSize.height = 50;
        preview.setPreferredSize(prefSize);

        content.add(BorderLayout.CENTER, preview);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.X_AXIS));
        buttons.setBorder(new EmptyBorder(12, 0, 0, 0));
        buttons.add(Box.createGlue());

        ok = new JButton(Localization.lang("Ok"));
        ok.addActionListener(new ActionHandler());
        getRootPane().setDefaultButton(ok);
        buttons.add(ok);

        buttons.add(Box.createHorizontalStrut(6));

        cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(new ActionHandler());
        buttons.add(cancel);

        buttons.add(Box.createGlue());

        content.add(BorderLayout.SOUTH, buttons);

        pack();
        setLocationRelativeTo(JOptionPane.getFrameForComponent(comp));
        setVisible(true); // show(); -> deprecated since 1.5
    }

    private void ok() {
        isOK = true;
        dispose();
    }

    private void cancel() {
        dispose();
    }

    public Font getSelectedFont() {
        if (!isOK) {
            return null;
        }

        int size;
        try {
            size = Integer.parseInt(sizeField.getText());
        } catch (Exception e) {
            size = 14;
        }

        return new Font(familyField.getText(), styleList.getSelectedIndex(), size);
    }


    // private members
    private boolean isOK;
    private final JTextField familyField;
    private final JList<String> familyList;
    private final JTextField sizeField;
    private final JList<String> sizeList;
    private final JTextField styleField;
    private final JList<String> styleList;
    private final JLabel preview;
    private final JButton ok;
    private final JButton cancel;

    /**
     * For some reason the default Java fonts show up in the
     * list with .bold, .bolditalic, and .italic extensions.
     */
    private static final String[] HIDEFONTS = {".bold", ".italic"};


    // [pending] from GeneralCustomizer :
    // GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
    private static String[] getFontList() {
        try {
            Class<?> GEClass = Class.forName("java.awt.GraphicsEnvironment");
            Object GEInstance = GEClass.getMethod("getLocalGraphicsEnvironment").invoke(null);

            String[] nameArray = (String[]) GEClass.getMethod("getAvailableFontFamilyNames").invoke(GEInstance);
            Vector<String> nameVector = new Vector<>(nameArray.length);

            for (int i = 0, j; i < nameArray.length; i++) {
                for (j = 0; j < FontSelectorDialog.HIDEFONTS.length; j++) {
                    if (nameArray[i].contains(FontSelectorDialog.HIDEFONTS[j])) {
                        break;
                    }
                }

                if (j == FontSelectorDialog.HIDEFONTS.length) {
                    nameVector.addElement(nameArray[i]);
                }
            }

            String[] _array = new String[nameVector.size()];
            nameVector.copyInto(_array);
            return _array;
        } catch (Exception ex) {
            return null;//return Toolkit.getDefaultToolkit().getFontList();
        }
    }

    private static JPanel createTextFieldAndListPanel(String label, JTextField textField, JList<String> list) {
        GridBagLayout layout = new GridBagLayout();
        JPanel panel = new JPanel(layout);

        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = cons.gridy = 0;
        cons.gridwidth = cons.gridheight = 1;
        cons.fill = GridBagConstraints.BOTH;
        cons.weightx = 1.0f;

        JLabel _label = new JLabel(label);
        layout.setConstraints(_label, cons);
        panel.add(_label);

        cons.gridy = 1;
        Component vs = Box.createVerticalStrut(6);
        layout.setConstraints(vs, cons);
        panel.add(vs);

        cons.gridy = 2;
        layout.setConstraints(textField, cons);
        panel.add(textField);

        cons.gridy = 3;
        vs = Box.createVerticalStrut(6);
        layout.setConstraints(vs, cons);
        panel.add(vs);

        cons.gridy = 4;
        cons.gridheight = GridBagConstraints.REMAINDER;
        cons.weighty = 1.0f;
        JScrollPane scroller = new JScrollPane(list);
        layout.setConstraints(scroller, cons);
        panel.add(scroller);

        return panel;
    }

    private void updatePreview() {
        String family = familyField.getText();
        int size;
        try {
            size = Integer.parseInt(sizeField.getText());
        } catch (Exception e) {
            size = 14;
        }
        int style = styleList.getSelectedIndex();
        preview.setFont(new Font(family, style, size));
    }


    private class ActionHandler implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent evt) {
            if (evt.getSource() == ok) {
                ok();
            } else if (evt.getSource() == cancel) {
                cancel();
            }
        }
    }

    private class ListHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent evt)
        {
            Object source = evt.getSource();
            if (source == familyList) {
                String family = familyList.getSelectedValue();
                if (family != null) {
                    familyField.setText(family);
                }
            }
            else if (source == sizeList) {
                String size = sizeList.getSelectedValue();
                if (size != null) {
                    sizeField.setText(size);
                }
            }
            else if (source == styleList) {
                String style = styleList.getSelectedValue();
                if (style != null) {
                    styleField.setText(style);
                }
            }
            updatePreview();
        }
    }
    /*public static void main(String args[])
        {
            Font font = new FontSelectorDialog(null,new Font("Times",Font.PLAIN,12)).getSelectedFont();

        }
     */
}
