package net.sf.jabref;
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

import java.awt.event.*;
import java.awt.*;
import java.util.Vector;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.*;

/**
 * A font chooser widget.
 * @author Slava Pestov (jEdit), Sylvain Reynal
 * @since jpicedt 1.3.2.beta-9
 * @version $Id$
 * <p>
 * $Log$
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

	static final String PLAIN="plain";
	static final String BOLD="bold";
	static final String BOLD_ITALIC="bold-italic";
	static final String ITALIC="italic";

	/** init with a default font */
	public FontSelector(){
		this(new Font("SansSerif", Font.PLAIN, 10));
	}

	/** init with the given font */
	public FontSelector(Font font){
		setFont(font);
		setRequestFocusEnabled(false);
		addActionListener(new ActionHandler());
	}

	public void setFont(Font font){
		super.setFont(font);
		updateText();
	}

	/**
	 * update button's text content from the current button's font.
	 */
	private void updateText(){
		Font font = getFont();
		String styleString;
		switch(font.getStyle()){
		case Font.PLAIN:
			styleString = PLAIN;
			break;
		case Font.BOLD:
			styleString = BOLD;
			break;
		case Font.ITALIC:
			styleString = ITALIC;
			break;
		case Font.BOLD | Font.ITALIC:
			styleString = BOLD_ITALIC;
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
	class ActionHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt) {
			Font font = new FontSelectorDialog(FontSelector.this,getFont()).getSelectedFont();
			if(font != null){
				setFont(font);
			}
		}
	}

}



///////////////////////////////////////////////////////////////////////////////

/**
 *
 */
public class FontSelectorDialog extends JDialog {

	/**
	 *
	 */
    	static final String PLAIN="plain";
	static final String BOLD="bold";
	static final String BOLD_ITALIC="bold-italic";
	static final String ITALIC="italic";

	public FontSelectorDialog(Component comp, Font font) {

	    //super(JOptionPane.getFrameForComponent(comp),jpicedt.Localizer.currentLocalizer().get("widget.FontSelector"),true); //
	    super(JOptionPane.getFrameForComponent(comp),"FontSelector",true); //
		JPanel content = new JPanel(new BorderLayout());
		content.setBorder(new EmptyBorder(12,12,12,12));
		setContentPane(content);

		JPanel listPanel = new JPanel(new GridLayout(1,3,6,6));

		JPanel familyPanel = createTextFieldAndListPanel(
								 "Font Family",
								 familyField = new JTextField(),
								 familyList = new JList(getFontList()));
		listPanel.add(familyPanel);

		String[] sizes = { "9", "10", "12", "14", "16", "18", "24" };
		JPanel sizePanel = createTextFieldAndListPanel(
							       "Font Size",
		                       sizeField = new JTextField(),
		                       sizeList = new JList(sizes));
		listPanel.add(sizePanel);

		String[] styles = {PLAIN,BOLD,ITALIC,BOLD_ITALIC};

		JPanel stylePanel = createTextFieldAndListPanel(
								"Font Style",
		                        styleField = new JTextField(),
		                        styleList = new JList(styles));
		styleField.setEditable(false);
		listPanel.add(stylePanel);

		familyList.setSelectedValue(font.getFamily(),true);
		familyField.setText(font.getFamily());
		sizeList.setSelectedValue(String.valueOf(font.getSize()),true);
		sizeField.setText(String.valueOf(font.getSize()));
		styleList.setSelectedIndex(font.getStyle());
		styleField.setText((String)styleList.getSelectedValue());

		ListHandler listHandler = new ListHandler();
		familyList.addListSelectionListener(listHandler);
		sizeList.addListSelectionListener(listHandler);
		styleList.addListSelectionListener(listHandler);

		content.add(BorderLayout.NORTH,listPanel);

		//preview = new JLabel("Font Preview");
		
		/* --------------------------------------------------------
		   |  Experimental addition by Morten Alver. I want to    |
		   |  enable antialiasing in the preview field, since I'm |
		   |  working on introducing this in the table view.      |
		   -------------------------------------------------------- */
		preview = new JLabel("Font Preview") {
			public void paint(Graphics g) {
			    Graphics2D g2 = (Graphics2D)g;
			    Font f = g2.getFont();//new Font("Plain", Font.PLAIN, 24);
			    g2.setColor(getBackground());
			    g2.fill(g2.getClipBounds());
			    g2.setColor(getForeground());
			    //g2.setFont(f);
			    RenderingHints rh = g2.getRenderingHints();
			    rh.put(RenderingHints.KEY_ANTIALIASING,
				   RenderingHints.VALUE_ANTIALIAS_ON);
			    g2.setRenderingHints(rh);
			    g2.drawString(getText(), 40, 15+f.getSize());
			}

		    };



		preview.setBorder(new TitledBorder("Font Preview"));

		updatePreview();

		Dimension prefSize = preview.getPreferredSize();
		prefSize.height = 50;
		preview.setPreferredSize(prefSize);

		content.add(BorderLayout.CENTER,preview);

		JPanel buttons = new JPanel();
		buttons.setLayout(new BoxLayout(buttons,BoxLayout.X_AXIS));
		buttons.setBorder(new EmptyBorder(12,0,0,0));
		buttons.add(Box.createGlue());

		ok = new JButton("OK");
		ok.addActionListener(new ActionHandler());
		getRootPane().setDefaultButton(ok);
		buttons.add(ok);

		buttons.add(Box.createHorizontalStrut(6));

		cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionHandler());
		buttons.add(cancel);

		buttons.add(Box.createGlue());

		content.add(BorderLayout.SOUTH,buttons);

		pack();
		setLocationRelativeTo(JOptionPane.getFrameForComponent(comp));
		show();
	}

	public void ok(){
		isOK = true;
		dispose();
	}

	public void cancel(){
		dispose();
	}

	public Font getSelectedFont(){
		if(!isOK)
			return null;

		int size;
		try{
			size = Integer.parseInt(sizeField.getText());
		}
		catch(Exception e){
			size = 14;
		}

		return new Font(familyField.getText(),styleList.getSelectedIndex(),size);
	}

	// private members
	private boolean isOK;
	private JTextField familyField;
	private JList familyList;
	private JTextField sizeField;
	private JList sizeList;
	private JTextField styleField;
	private JList styleList;
	private JLabel preview;
	private JButton ok;
	private JButton cancel;

	/**
	 * For some reason the default Java fonts show up in the
	 * list with .bold, .bolditalic, and .italic extensions.
	 */
	private static final String[] HIDEFONTS = {".bold",".italic"};

	// [pending] from GeneralCustomizer :
	// GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()
	private String[] getFontList(){
		try{
			Class GEClass = Class.forName("java.awt.GraphicsEnvironment");
			Object GEInstance = GEClass.getMethod("getLocalGraphicsEnvironment", null).invoke(null, null);

			String[] nameArray = (String[])GEClass.getMethod("getAvailableFontFamilyNames", null).invoke(GEInstance, null);
			Vector nameVector = new Vector(nameArray.length);

			for(int i = 0, j; i < nameArray.length; i++){
				for(j = 0; j < HIDEFONTS.length; j++){
					if(nameArray[i].indexOf(HIDEFONTS[j]) >= 0) break;
				}

				if(j == HIDEFONTS.length) nameVector.addElement(nameArray[i]);
			}

			String[] _array = new String[nameVector.size()];
			nameVector.copyInto(_array);
			return _array;
		}
		catch(Exception ex){
		    return null;//return Toolkit.getDefaultToolkit().getFontList();
		}
	}

	private JPanel createTextFieldAndListPanel(String label,JTextField textField, JList list){
		GridBagLayout layout = new GridBagLayout();
		JPanel panel = new JPanel(layout);

		GridBagConstraints cons = new GridBagConstraints();
		cons.gridx = cons.gridy = 0;
		cons.gridwidth = cons.gridheight = 1;
		cons.fill = GridBagConstraints.BOTH;
		cons.weightx = 1.0f;

		JLabel _label = new JLabel(label);
		layout.setConstraints(_label,cons);
		panel.add(_label);

		cons.gridy = 1;
		Component vs = Box.createVerticalStrut(6);
		layout.setConstraints(vs,cons);
		panel.add(vs);

		cons.gridy = 2;
		layout.setConstraints(textField,cons);
		panel.add(textField);

		cons.gridy = 3;
		vs = Box.createVerticalStrut(6);
		layout.setConstraints(vs,cons);
		panel.add(vs);

		cons.gridy = 4;
		cons.gridheight = GridBagConstraints.REMAINDER;
		cons.weighty = 1.0f;
		JScrollPane scroller = new JScrollPane(list);
		layout.setConstraints(scroller,cons);
		panel.add(scroller);

		return panel;
	}

	private void updatePreview(){
		String family = familyField.getText();
		int size;
		try{
			size = Integer.parseInt(sizeField.getText());
		}
		catch(Exception e){
			size = 14;
		}
		int style = styleList.getSelectedIndex();
		preview.setFont(new Font(family,style,size));
	}

	class ActionHandler implements ActionListener {
		public void actionPerformed(ActionEvent evt){
			if(evt.getSource() == ok)ok();
			else if(evt.getSource() == cancel)cancel();
		}
	}

	class ListHandler implements ListSelectionListener {
		public void valueChanged(ListSelectionEvent evt)
		{
			Object source = evt.getSource();
			if(source == familyList) {
				String family = (String)familyList.getSelectedValue();
				if(family != null)
					familyField.setText(family);
			}
			else if(source == sizeList) {
				String size = (String)sizeList.getSelectedValue();
				if(size != null)
					sizeField.setText(size);
			}
			else if(source == styleList) {
				String style = (String)styleList.getSelectedValue();
				if(style != null)
					styleField.setText(style);
			}
			updatePreview();
		}
	}
    public static void main(String args[])
	{
	    Font font = new FontSelectorDialog(null,new Font("Times",Font.PLAIN,12)).getSelectedFont();

	}

}
