package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.LookupIdentifiersWorker;
import org.jabref.logic.importer.IdFetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupIdentifierAction extends MnemonicAwareAction {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupIdentifierAction.class);

    private final JabRefFrame frame;
    private final IdFetcher fetcher;

    public LookupIdentifierAction(JabRefFrame frame, IdFetcher fetcher) {
        super();
        this.frame = frame;
        this.fetcher = fetcher;

        putValue(Action.NAME, fetcher.getIdentifierName());
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        try {
            BasePanel.runWorker(new LookupIdentifiersWorker(frame, fetcher));
        } catch (Exception e) {
            LOGGER.error("Problem running ID Worker", e);
        }
    }
}
