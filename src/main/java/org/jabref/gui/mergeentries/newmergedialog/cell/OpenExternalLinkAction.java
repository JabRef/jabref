package org.jabref.gui.mergeentries.newmergedialog.cell;

import java.io.IOException;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.strings.StringUtil;

/**
 * This action can open an Url and DOI
 * */
public class OpenExternalLinkAction extends SimpleCommand {
    private final String urlOrDoi;

    public OpenExternalLinkAction(String urlOrDoi) {
        this.urlOrDoi = urlOrDoi;
    }

    @Override
    public void execute() {
        if (StringUtil.isBlank(urlOrDoi)) {
            return;
        }

        try {
            JabRefDesktop.openBrowser(urlOrDoi);
        } catch (
                IOException ex) {
            // TODO: Do something
        }
    }
}
