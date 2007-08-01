package net.sf.jabref.wizard.text.gui ;

import java.util.Enumeration;
import java.util.Vector;

import javax.swing.DefaultListModel;

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
