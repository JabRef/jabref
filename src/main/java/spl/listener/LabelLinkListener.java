package spl.listener;

import net.sf.jabref.MetaData;
import net.sf.jabref.gui.desktop.JabRefDesktop;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

public class LabelLinkListener implements MouseListener {

    private final Component component;

    public LabelLinkListener(Component c, String link) {
        this.component = c;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        try {
            JabRefDesktop.openExternalViewer(new MetaData(), "http://www.mr-dlib.org/docs/jabref_metadata_extraction_alpha.php", "url");
        } catch (IOException exc) {
            exc.printStackTrace();
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        component.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    public void mouseExited(MouseEvent e) {
        component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

}
