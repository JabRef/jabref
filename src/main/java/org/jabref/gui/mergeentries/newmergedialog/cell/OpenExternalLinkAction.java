package org.jabref.gui.mergeentries.newmergedialog.cell;

import java.io.IOException;
import java.net.URI;

import org.jabref.logic.FilePreferences;
import org.jabref.gui.actions.SimpleCommand;
import org.jabref.gui.desktop.os.NativeDesktop;
import org.jabref.model.entry.identifier.DOI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A command for opening DOIs and URLs. This was created primarily for simplifying {@link FieldValueCell}.
 */
public class OpenExternalLinkAction extends SimpleCommand {
    private final Logger LOGGER = LoggerFactory.getLogger(OpenExternalLinkAction.class);

    private final FilePreferences filePreferences;

    private final String urlOrDoi;

    public OpenExternalLinkAction(String urlOrDoi, FilePreferences filePreferences) {
        this.filePreferences = filePreferences;
        this.urlOrDoi = urlOrDoi;
    }

    @Override
    public void execute() {
        try {
            if (DOI.isValid(urlOrDoi)) {
                NativeDesktop.openBrowser(
                        DOI.parse(urlOrDoi)
                           .flatMap(DOI::getExternalURI)
                           .map(URI::toString)
                           .orElse(""),
                        filePreferences
                );
            } else {
                NativeDesktop.openBrowser(urlOrDoi, filePreferences);
            }
        } catch (IOException e) {
            LOGGER.warn("Cannot open the given external link '{}'", urlOrDoi, e);
        }
    }
}
