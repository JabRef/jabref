package org.jabref.gui.mergeentries.newmergedialog.cell;

import java.io.IOException;
import java.net.URI;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.JabRefDesktop;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command for opening DOIs and URLs. This was created primarily for simplifying {@link FieldValueCell}.
 */
public class OpenExternalLinkAction extends SimpleCommand {
    private final String urlOrDoi;
    private final Logger LOGGER = LoggerFactory.getLogger(OpenExternalLinkAction.class);

    public OpenExternalLinkAction(String urlOrDoi) {
        this.urlOrDoi = urlOrDoi;
    }

    @Override
    public void execute() {
        try {
            if (DOI.isValid(urlOrDoi)) {
                JabRefDesktop.openBrowser(
                        DOI.parse(urlOrDoi)
                           .flatMap(DOI::getExternalURI)
                           .map(URI::toString)
                           .orElse("")
                );
            } else {
                JabRefDesktop.openBrowser(
                        urlOrDoi
                );
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot open the given external link '{}'", urlOrDoi, e);
        }
    }
}
