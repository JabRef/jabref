/*
 * Created on Jul 23, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package net.sf.jabref;

import java.awt.Component;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author sarahspi
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IconStringRenderer extends DefaultTableCellRenderer {

	  /*
	   * @see TableCellRenderer#getTableCellRendererComponent(JTable, Object, boolean, boolean, int, int)
	   */
	  public Component getTableCellRendererComponent(JTable table, Object value,
	                                                 boolean isSelected, boolean hasFocus, 
	                                                 int row, int column) {
	  	JLabel retval = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,row,column);	  	
	  	if (value instanceof JLabel) {
	  		retval.setText(((JLabel)value).getText());
	  		retval.setIcon(((JLabel)value).getIcon());
	  	}
	    return retval;
	  }
	
}
