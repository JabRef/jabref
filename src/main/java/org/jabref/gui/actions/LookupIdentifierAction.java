package org.jabref.gui.actions;

import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.JabRefIcon;
import org.jabref.gui.keyboard.KeyBinding;
import org.jabref.gui.worker.LookupIdentifiersWorker;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.model.entry.identifier.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupIdentifierAction<T extends Identifier> extends SimpleCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupIdentifierAction.class);

    private final JabRefFrame frame;

    private final IdFetcher<T> fetcher;

    public LookupIdentifierAction(JabRefFrame frame, IdFetcher<T> fetcher) {
        this.frame = frame;
        this.fetcher = fetcher;
    }

    @Override
    public void execute() {
        try {
            BasePanel.runWorker(new LookupIdentifiersWorker<>(frame, fetcher));
        } catch (Exception e) {
            LOGGER.error("Problem running ID Worker", e);
        }
    }

    public Action getAction() {
        return new Action() {

            @Override
            public Optional<JabRefIcon> getIcon() {
                return Optional.empty();
            }

            @Override
            public Optional<KeyBinding> getKeyBinding() {
                return Optional.empty();
            }

            @Override
            public String getText() {
                return fetcher.getIdentifierName();
            }

            @Override
            public String getDescription() {
                return "";
            }
        };
    }
}
