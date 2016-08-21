package net.sf.jabref.gui.actions;

import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JTextField;

import net.sf.jabref.gui.FileDialog;
import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.logic.util.FileExtensions;

/**
 * Action used to produce a "Browse" button for one of the text fields.
 */
public final class BrowseAction extends AbstractAction {

    private final JFrame frame;
    private final JTextField comp;
    private final boolean dirsOnly;
    private final Set<FileExtensions> extensions;


    public static BrowseAction buildForDir(JFrame frame, JTextField tc) {
        return new BrowseAction(frame, tc, true, Collections.emptySet());
    }

    public static BrowseAction buildForDir(JTextField tc) {
        return new BrowseAction(null, tc, true, Collections.emptySet());
    }

    public static BrowseAction buildForFile(JTextField tc) {

        return new BrowseAction(null, tc, false, Collections.emptySet());
    }

    public static BrowseAction buildForFile(JTextField tc, FileExtensions extensions) {

        return new BrowseAction(null, tc, false, EnumSet.of(extensions));
    }

    public static BrowseAction buildForFile(JTextField tc, Set<FileExtensions> extensions) {
        return new BrowseAction(null, tc, false, extensions);
    }


    private BrowseAction(JFrame frame, JTextField tc, boolean dirsOnly, Set<FileExtensions> extensions) {
        super(Localization.lang("Browse"));
        this.frame = frame;
        this.dirsOnly = dirsOnly;
        this.comp = tc;
        this.extensions = extensions;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String chosen = askUser();

        if (chosen != null) {
            File newFile = new File(chosen);
            comp.setText(newFile.getPath());

        }
    }

    private String askUser() {
        if (dirsOnly) {
            Path path  = new FileDialog(frame, comp.getText()).dirsOnly().withExtensions(extensions)
                    .showDialogAndGetSelectedFile().orElse(Paths.get(""));
            String file = path.toString();

            return file;
        } else {
            Path path = new FileDialog(frame, comp.getText()).withExtensions(extensions)
                    .showDialogAndGetSelectedFile().orElse(Paths.get(""));
            String file = path.toString();

            return file;
        }
    }
}
