package net.sf.jabref.wizard.text.gui ;

import javax.swing.DefaultListModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.event.TableModelListener;
import java.util.*;

public class HintListModel extends DefaultListModel
{
  public void setData(Vector newData)
  {
    clear() ;
    if (newData != null)
    {
      for(Enumeration myEnum = newData.elements() ; myEnum.hasMoreElements() ; )
      {
        addElement(myEnum.nextElement());
      }
    }
  }

  public void valueUpdated(int index)
  {
    super.fireContentsChanged(this, index, index);
  }
}
