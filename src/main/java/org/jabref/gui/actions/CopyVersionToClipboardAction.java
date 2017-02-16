package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jabref.Globals;
import org.jabref.JabRefGUI;
import org.jabref.gui.ClipBoardManager;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.util.BuildInfo;

public class CopyVersionToClipboardAction extends AbstractAction {

    @Override
    public void actionPerformed(ActionEvent e) {
        String info = String.format("JabRef %s%n%s %s %s %nJava %s", Globals.BUILD_INFO.getVersion(), BuildInfo.OS,
                BuildInfo.OS_VERSION, BuildInfo.OS_ARCH, BuildInfo.JAVA_VERSION);
        new ClipBoardManager().setClipboardContents(info);
        JabRefGUI.getMainFrame().output(Localization.lang("Copied version to clipboard"));
    }

}
