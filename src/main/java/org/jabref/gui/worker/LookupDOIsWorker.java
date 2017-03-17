package org.jabref.gui.worker;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.gui.BasePanel;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.undo.NamedCompound;
import org.jabref.gui.undo.UndoableFieldChange;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.importer.WebFetchers;
import org.jabref.logic.l10n.Localization;
import org.jabref.model.FieldChange;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.FieldName;
import org.jabref.model.entry.identifier.DOI;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LookupDOIsWorker extends AbstractWorker {

    private final JabRefFrame frame;
    private String message;

    private static final Log LOGGER = LogFactory.getLog(LookupDOIsWorker.class);

    public LookupDOIsWorker(JabRefFrame frame) {
        this.frame = Objects.requireNonNull(frame);
    }

    @Override
    public void run() {
        BasePanel basePanel = Objects.requireNonNull(frame.getCurrentBasePanel());
        List<BibEntry> bibEntries = basePanel.getSelectedEntries();
        if (!bibEntries.isEmpty()) {
            String totalCount = Integer.toString(bibEntries.size());
            NamedCompound namedCompound = new NamedCompound(Localization.lang("Look up DOIs"));
            int count = 0;
            int foundCount = 0;
            for (BibEntry bibEntry : bibEntries) {
                count++;
                frame.output(Localization.lang("Looking up DOIs... - entry %0 out of %1 - found %2", Integer.toString(count), totalCount, Integer.toString(foundCount)));
                if (!bibEntry.hasField(FieldName.DOI)) {
                    Optional<DOI> doi = Optional.empty();
                    try {
                        doi = WebFetchers.getIdFetcherForIdentifier(DOI.class).findIdentifier(bibEntry);
                    } catch (FetcherException e) {
                        LOGGER.error("Could not fetch doi", e);
                    }
                    if (doi.isPresent()) {
                        Optional<FieldChange> fieldChange = bibEntry.setField(FieldName.DOI, doi.get().getDOI());
                        if (fieldChange.isPresent()) {
                            namedCompound.addEdit(new UndoableFieldChange(fieldChange.get()));
                            foundCount++;
                            frame.output(Localization.lang("Looking up DOIs... - entry %0 out of %1 - found %2", Integer.toString(count), totalCount, Integer.toString(foundCount)));
                        }
                    }
                }
            }
            namedCompound.end();
            if (foundCount > 0) {
                basePanel.getUndoManager().addEdit(namedCompound);
                basePanel.markBaseChanged();
            }
            message = Localization.lang("Determined_DOIs_for_%0_entries", Integer.toString(foundCount));
        }
    }

    @Override
    public void update() {
        frame.output(message);
    }

}
