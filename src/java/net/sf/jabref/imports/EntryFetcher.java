package net.sf.jabref.imports;

import net.sf.jabref.gui.ImportInspectionDialog;
import net.sf.jabref.BasePanel;
import net.sf.jabref.JabRefFrame;

import javax.swing.*;
import java.net.URL;

/**
 * Created by IntelliJ IDEA.
 * User: alver
 * Date: Mar 26, 2006
 * Time: 1:50:58 AM
 * To change this template use File | Settings | File Templates.
 */
public interface EntryFetcher extends ImportInspectionDialog.CallBack {

    public void processQuery(String query, ImportInspectionDialog dialog,
                             JabRefFrame frame);

    public String getTitle();

    public URL getIcon();

    public String getHelpPage();

    public JPanel getOptionsPanel();
}
