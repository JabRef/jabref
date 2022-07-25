package org.jabref.gui.mergeentries.newmergedialog.cell;

import java.io.IOException;
import java.net.URI;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.entry.identifier.DOI;
import org.jabref.model.strings.StringUtil;

/**
 * A command for opening DOIs and URLs. This was created primarily for simplifying {@link FieldValueCell}.
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
            String url;
            if (DOI.isValid(urlOrDoi)) {
                url = DOI.parse(urlOrDoi).flatMap(DOI::getExternalURI).map(URI::toString).orElse("");
            } else {
                url = urlOrDoi;
            }

            JabRefDesktop.openBrowser(url);
        } catch (
                IOException ex) {
            // TODO: Do something
        }
    }
}
