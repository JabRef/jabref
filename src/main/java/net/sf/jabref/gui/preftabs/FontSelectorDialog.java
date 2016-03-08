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
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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


///////////////////////////////////////////////////////////////////////////////

public class FontSelectorDialog extends JDialog {

    private static final String PLAIN = "plain";
    private static final String BOLD = "bold";
    private static final String BOLD_ITALIC = "bold-italic";
    private static final String ITALIC = "italic";

    private static final String[] styles = {PLAIN, BOLD, ITALIC, BOLD_ITALIC};

    private static final String[] sizes = {"9", "10", "12", "14", "16", "18", "24"};

    // private members
    private boolean isOK;
    private final JTextField familyField = new JTextField();
    private final JList<String> familyList;
    private final JTextField sizeField = new JTextField();
    private final JList<String> sizeList = new JList<>(sizes);
    private final JTextField styleField = new JTextField();
    private final JList<String> styleList = new JList<>(styles);
    private final JLabel preview;

    /**
     * For some reason the default Java fonts show up in the
     * list with .bold, .bolditalic, and .italic extensions.
     */
    private static final String[] HIDEFONTS = {".bold", ".italic"};


    public FontSelectorDialog(Component comp, Font font) {

        super(JOptionPane.getFrameForComponent(comp), Localization.lang("FontSelector"), true); //
        JPanel content = new JPanel(new BorderLayout());
        content.setBorder(new EmptyBorder(12, 12, 12, 12));
        setContentPane(content);

        JPanel listPanel = new JPanel(new GridLayout(1, 3, 6, 6));

        familyList = new JList<>(getFontList());

        JPanel familyPanel = createTextFieldAndListPanel(Localization.lang("Font Family"), familyField, familyList);
        listPanel.add(familyPanel);

        JPanel sizePanel = createTextFieldAndListPanel(Localization.lang("Font Size"), sizeField, sizeList);
        listPanel.add(sizePanel);

        JPanel stylePanel = createTextFieldAndListPanel(Localization.lang("Font Style"), styleField, styleList);
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

        JButton ok = new JButton(Localization.lang("OK"));
        ok.addActionListener(e -> {
            isOK = true;
            dispose();
        });
        getRootPane().setDefaultButton(ok);
        buttons.add(ok);

        buttons.add(Box.createHorizontalStrut(6));

        JButton cancel = new JButton(Localization.lang("Cancel"));
        cancel.addActionListener(e -> dispose());
        buttons.add(cancel);

        buttons.add(Box.createGlue());

        content.add(BorderLayout.SOUTH, buttons);

        pack();
        setLocationRelativeTo(JOptionPane.getFrameForComponent(comp));
        setVisible(true);
    }

    public Optional<Font> getSelectedFont() {
        if (!isOK) {
            return Optional.empty();
        }

        int size;
        try {
            size = Integer.parseInt(sizeField.getText());
        } catch (NumberFormatException e) {
            size = 14;
        }

        return Optional.of(new Font(familyField.getText(), styleList.getSelectedIndex(), size));
    }



    private static String[] getFontList() {
        try {
            String[] nameArray = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            List<String> nameList = new ArrayList<>(nameArray.length);
            for (String fontName : nameArray) {
                boolean hidden = false;
                for (String hiddenName : FontSelectorDialog.HIDEFONTS) {
                    if (fontName.contains(hiddenName)) {
                        hidden = true;
                        break;
                    }
                }

                if (!hidden) {
                    nameList.add(fontName);
                }
            }
            String[] resultArray = new String[nameList.size()];
            return nameList.toArray(resultArray);
        } catch (SecurityException | IllegalArgumentException ex) {
            return new String[0];
        }
    }

    private static JPanel createTextFieldAndListPanel(String labelString, JTextField textField, JList<String> list) {
        GridBagLayout layout = new GridBagLayout();
        JPanel panel = new JPanel(layout);

        GridBagConstraints cons = new GridBagConstraints();
        cons.gridx = cons.gridy = 0;
        cons.gridwidth = cons.gridheight = 1;
        cons.fill = GridBagConstraints.BOTH;
        cons.weightx = 1.0f;

        JLabel label = new JLabel(labelString);
        layout.setConstraints(label, cons);
        panel.add(label);

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
        } catch (NumberFormatException e) {
            size = 14;
        }
        int style = styleList.getSelectedIndex();
        preview.setFont(new Font(family, style, size));
    }


    private class ListHandler implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent evt) {
            Object source = evt.getSource();
            if (familyList.equals(source)) {
                String family = familyList.getSelectedValue();
                if (family != null) {
                    familyField.setText(family);
                }
            } else if (sizeList.equals(source)) {
                String size = sizeList.getSelectedValue();
                if (size != null) {
                    sizeField.setText(size);
                }
            } else if (styleList.equals(source)) {
                String style = styleList.getSelectedValue();
                if (style != null) {
                    styleField.setText(style);
                }
            }
            updatePreview();
        }
    }
}
