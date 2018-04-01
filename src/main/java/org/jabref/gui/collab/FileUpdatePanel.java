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
import javax.swing.SwingUtilities;

import javafx.embed.swing.SwingNode;
import javafx.scene.Node;
import javafx.scene.layout.Priority;

import org.jabref.gui.BasePanel;
import org.jabref.gui.SidePaneComponent;
import org.jabref.gui.SidePaneManager;
import org.jabref.gui.SidePaneType;
import org.jabref.gui.actions.Action;
import org.jabref.gui.icon.IconTheme;
import org.jabref.logic.l10n.Localization;

public class FileUpdatePanel extends SidePaneComponent implements ActionListener, ChangeScanner.DisplayResultCallback {

    private final SidePaneManager manager;
    private ChangeScanner scanner;
    private File file;
    private BasePanel panel;

    public FileUpdatePanel(SidePaneManager manager) {
        super(manager, IconTheme.JabRefIcons.SAVE, Localization.lang("File changed"));

        this.manager = manager;
    }

    public void showForFile(BasePanel panel, File file, ChangeScanner scanner) {
        this.file = file;
        this.panel = panel;
        this.scanner = scanner;

        this.show();
    }

    /**
     * We include a getter for the BasePanel this component refers to, because this
     * component needs to be closed if the BasePanel is closed.
     * @return the base panel this component refers to.
     */
    public BasePanel getPanel() {
        return panel;
    }

    @Override
    public Priority getResizePolicy() {
        return Priority.NEVER;
    }

    @Override
    public ToggleCommand getToggleCommand() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Action getToggleAction() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected Node createContentPane() {
        JPanel main = new JPanel();
        main.setLayout(new BorderLayout());

        JLabel message = new JLabel("<html><center>"
                + Localization.lang("The file<BR>'%0'<BR>has been modified<BR>externally!", file.getName())
                + "</center></html>", SwingConstants.CENTER);

        main.add(message, BorderLayout.CENTER);
        JButton reviewChanges = new JButton(Localization.lang("Review changes"));
        reviewChanges.addActionListener(this);
        main.add(reviewChanges, BorderLayout.SOUTH);
        main.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

        SwingNode swingNode = new SwingNode();
        SwingUtilities.invokeLater(() -> swingNode.setContent(main));
        return swingNode;
    }

    @Override
    public SidePaneType getType() {
        return SidePaneType.FILE_UPDATE_NOTIFICATION;
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
            manager.hide(this.getType());
            panel.markExternalChangesAsResolved();
        }
    }
}
