package org.jabref.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.Action;

import org.jabref.Logger;
import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.worker.LookupIdentifiersWorker;
import org.jabref.logic.importer.IdFetcher;

public class LookupIdentifierAction extends MnemonicAwareAction {


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
            Logger.error(this, "worker error", e);
        }
    }
}
