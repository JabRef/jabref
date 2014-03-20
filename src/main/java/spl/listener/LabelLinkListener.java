package spl.listener;


import net.sf.jabref.MetaData;
import net.sf.jabref.Util;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Christoph Arbeit
 * Date: 28.09.2010
 * Time: 12:06:57
 * To change this template use File | Settings | File Templates.
 */
public class LabelLinkListener implements MouseListener {

        private String link;
        private Component component;

        public LabelLinkListener(Component c, String link) {
            this.link = link;
            this.component = c;
        }

        public void mousePressed(MouseEvent e) {
            try {
                Util.openExternalViewer(new MetaData(), "http://www.mr-dlib.org/docs/jabref_metadata_extraction_alpha.php", "url");
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }

        public void mouseReleased(MouseEvent e) {

        }

        public void mouseEntered(MouseEvent e) {
            component.setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        public void mouseExited(MouseEvent e) {
            component.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }

        public void mouseClicked(MouseEvent e) {
        }

}
