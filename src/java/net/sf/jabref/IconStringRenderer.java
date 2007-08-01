/*
 * Created on Jul 23, 2004
 *
 */
package net.sf.jabref;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * @author sarahspi
 *
 */
public class IconStringRenderer extends DefaultTableCellRenderer {

    String toolTip;

    public IconStringRenderer(String toolTip) {
	this.toolTip = toolTip;
    }

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
	  		retval.setToolTipText(toolTip);
	  	}
	    return retval;
	  }
	
}
