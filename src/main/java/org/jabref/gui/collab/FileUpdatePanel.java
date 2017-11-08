package org.jabref.gui.collab;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.jabref.gui.BasePanel;
import org.jabref.gui.IconTheme;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.logic.l10n.Localization;

public class FileUpdatePanel extends SidePaneComponent implements ActionListener, ChangeScanner.DisplayResultCallback {

    private final SidePaneManager manager;

    private final ChangeScanner scanner;


    public FileUpdatePanel(BasePanel panel, SidePaneManager manager, File file, ChangeScanner scanner) {
        super(manager, IconTheme.JabRefIcon.SAVE.getIcon(), Localization.lang("File changed"));
        close.setEnabled(false);
        this.panel = panel;
        this.manager = manager;
        this.scanner = scanner;

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());

        JLabel message = new JLabel("<html><center>"
                + Localization.lang("The file<BR>'%0'<BR>has been modified<BR>externally!", file.getName())
                + "</center></html>", SwingConstants.CENTER);

        main.add(message, BorderLayout.CENTER);
        JButton test = new JButton(Localization.lang("Review changes"));
        main.add(test, BorderLayout.SOUTH);
        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        add(main, BorderLayout.CENTER);
        test.addActionListener(this);
    }

    /**
     * We include a getter for the BasePanel this component refers to, because this
     * component needs to be closed if the BasePanel is closed.
     * @return the base panel this component refers to.
     */
    public BasePanel getPanel() {
        return panel;
    }

    /**
     * Unregister when this component closes. We need that to avoid showing
     * two such external change warnings at the same time, only the latest one.
     */
    @Override
    public void componentClosing() {
        manager.unregisterComponent(FileUpdatePanel.class);
    }

    @Override
    public int getRescalingWeight() {
        return 0;
    }

    @Override
    public ToggleAction getToggleAction() {
        throw new UnsupportedOperationException();
    }

    /**
     * actionPerformed
     *
     * @param e
     *            ActionEvent
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        // ChangeScanner scanner = new ChangeScanner(frame, panel); //,
        // panel.database(), panel.metaData());
        // try {
        scanner.displayResult(this);
        // scanner.changeScan(panel.file());

        // } catch (IOException ex) {
        // ex.printStackTrace();
        // }
    }

    /**
     * Callback method for signalling that the change scanner has displayed the
     * scan results to the user.
     * @param resolved true if there were no changes, or if the user has resolved them.
     */
    @Override
    public void scanResultsResolved(boolean resolved) {
        if (resolved) {
            manager.hideComponent(this);
            panel.markExternalChangesAsResolved();
        }
    }
}
