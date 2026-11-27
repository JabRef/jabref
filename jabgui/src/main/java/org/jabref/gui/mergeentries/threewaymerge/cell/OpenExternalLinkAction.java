package org.jabref.gui.mergeentries.threewaymerge.cell;

import java.io.IOException;
import java.net.URI;

import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// A command for opening DOIs and URLs. This was created primarily for simplifying [FieldValueCell].
public class OpenExternalLinkAction extends SimpleCommand {
    private final Logger LOGGER = LoggerFactory.getLogger(OpenExternalLinkAction.class);

    private final NativeDesktop nativeDesktop;

    private final String urlOrDoi;

    public OpenExternalLinkAction(String urlOrDoi, NativeDesktop nativeDesktop) {
        this.nativeDesktop = nativeDesktop;
        this.urlOrDoi = urlOrDoi;
    }

    @Override
    public void execute() {
        try {
            if (DOI.isValid(urlOrDoi)) {
                nativeDesktop.openBrowser(
                        DOI.parse(urlOrDoi)
                           .flatMap(DOI::getExternalURI)
                           .map(URI::toString)
                           .orElse("")
                );
            } else {
                nativeDesktop.openBrowser(urlOrDoi);
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot open the given external link '{}'", urlOrDoi, e);
        }
    }
}
