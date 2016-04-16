package net.sf.jabref;

import javax.swing.SwingUtilities;

/**
 * JabRef MainClass
 */
public class JabRefMain {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JabRef().start(args));
    }
}
