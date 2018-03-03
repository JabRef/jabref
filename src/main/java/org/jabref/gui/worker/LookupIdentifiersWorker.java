package org.jabref.gui.worker;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.IdFetcher;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.identifier.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LookupIdentifiersWorker<T extends Identifier> extends AbstractWorker {

    private static final Logger LOGGER = LoggerFactory.getLogger(LookupIdentifiersWorker.class);
    private final JabRefFrame frame;
    private final IdFetcher<T> fetcher;

    private String message;

    public LookupIdentifiersWorker(JabRefFrame frame, IdFetcher<T> fetcher) {
        this.frame = Objects.requireNonNull(frame);
        this.fetcher = Objects.requireNonNull(fetcher);
    }

    @Override
    public void run() {
        BasePanel basePanel = Objects.requireNonNull(frame.getCurrentBasePanel());
        List<BibEntry> bibEntries = basePanel.getSelectedEntries();
        if (!bibEntries.isEmpty()) {
            String totalCount = Integer.toString(bibEntries.size());
            NamedCompound namedCompound = new NamedCompound(Localization.lang("Look up %0", fetcher.getIdentifierName()));
            int count = 0;
            int foundCount = 0;
            for (BibEntry bibEntry : bibEntries) {
                count++;
                frame.output(Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                        fetcher.getIdentifierName(), Integer.toString(count), totalCount, Integer.toString(foundCount)));
                Optional<T> identifier = Optional.empty();
                try {
                    identifier = fetcher.findIdentifier(bibEntry);
                } catch (FetcherException e) {
                    LOGGER.error("Could not fetch " + fetcher.getIdentifierName(), e);
                }
                if (identifier.isPresent() && !bibEntry.hasField(identifier.get().getDefaultField())) {
                    Optional<FieldChange> fieldChange = bibEntry.setField(identifier.get().getDefaultField(), identifier.get().getNormalized());
                    if (fieldChange.isPresent()) {
                        namedCompound.addEdit(new UndoableFieldChange(fieldChange.get()));
                        foundCount++;
                        frame.output(Localization.lang("Looking up %0... - entry %1 out of %2 - found %3",
                                Integer.toString(count), totalCount, Integer.toString(foundCount)));
                    }
                }
            }
            namedCompound.end();
            if (foundCount > 0) {
                basePanel.getUndoManager().addEdit(namedCompound);
                basePanel.markBaseChanged();
            }
            message = Localization.lang("Determined %0 for %1 entries", fetcher.getIdentifierName(), Integer.toString(foundCount));
        }
    }

    @Override
    public void update() {
        frame.output(message);
    }
}
