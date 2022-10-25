package org.jabref.gui.collab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jabref.gui.collab.entryadd.EntryAdd;
import org.jabref.gui.collab.entrychange.EntryChange;
import org.jabref.gui.collab.entrydelete.EntryDelete;
import org.jabref.gui.collab.groupchange.GroupChange;
import org.jabref.gui.collab.metedatachange.MetadataChange;
import org.jabref.gui.collab.preamblechange.PreambleChange;
import org.jabref.gui.collab.stringadd.BibTexStringAdd;
import org.jabref.gui.collab.stringchange.BibTexStringChange;
import org.jabref.gui.collab.stringdelete.BibTexStringDelete;
import org.jabref.gui.collab.stringrename.BibTexStringRename;
import org.jabref.logic.bibtex.comparator.BibDatabaseDiff;
import org.jabref.logic.bibtex.comparator.BibEntryDiff;
import org.jabref.logic.bibtex.comparator.BibStringDiff;
import org.jabref.model.database.BibDatabaseContext;

public class ExternalChangeList {
    private ExternalChangeList() {
    }

    public static List<ExternalChange> compareAndGetChanges(BibDatabaseContext originalDatabase, BibDatabaseContext otherDatabase, ExternalChangeResolverFactory externalChangeResolverFactory) {
        List<ExternalChange> changes = new ArrayList<>();

        BibDatabaseDiff differences = BibDatabaseDiff.compare(originalDatabase, otherDatabase);

        differences.getMetaDataDifferences().ifPresent(diff -> {
            changes.add(new MetadataChange(diff, originalDatabase, externalChangeResolverFactory));
            diff.getGroupDifferences().ifPresent(groupDiff -> changes.add(new GroupChange(
                    groupDiff, originalDatabase, externalChangeResolverFactory
            )));
        });
        differences.getPreambleDifferences().ifPresent(diff -> changes.add(new PreambleChange(diff, originalDatabase, externalChangeResolverFactory)));
        differences.getBibStringDifferences().forEach(diff -> changes.add(createBibStringDiff(originalDatabase, externalChangeResolverFactory, diff)));
        differences.getEntryDifferences().forEach(diff -> changes.add(createBibEntryDiff(originalDatabase, externalChangeResolverFactory, diff)));

        return Collections.unmodifiableList(changes);
    }

    private static ExternalChange createBibStringDiff(BibDatabaseContext originalDatabase, ExternalChangeResolverFactory externalChangeResolverFactory, BibStringDiff diff) {
        if (diff.getOriginalString() == null) {
            return new BibTexStringAdd(diff.getNewString(), originalDatabase, externalChangeResolverFactory);
        }

        if (diff.getNewString() == null) {
            return new BibTexStringDelete(diff.getOriginalString(), originalDatabase, externalChangeResolverFactory);
        }

        if (diff.getOriginalString().getName().equals(diff.getNewString().getName())) {
            return new BibTexStringChange(diff.getOriginalString(), diff.getNewString(), originalDatabase, externalChangeResolverFactory);
        }

        return new BibTexStringRename(diff.getOriginalString(), diff.getNewString(), originalDatabase, externalChangeResolverFactory);
    }

    private static ExternalChange createBibEntryDiff(BibDatabaseContext originalDatabase, ExternalChangeResolverFactory externalChangeResolverFactory, BibEntryDiff diff) {
        if (diff.getOriginalEntry() == null) {
            return new EntryAdd(diff.getNewEntry(), originalDatabase, externalChangeResolverFactory);
        }

        if (diff.getNewEntry() == null) {
            return new EntryDelete(diff.getOriginalEntry(), originalDatabase, externalChangeResolverFactory);
        }

        return new EntryChange(diff.getOriginalEntry(), diff.getNewEntry(), originalDatabase, externalChangeResolverFactory);
    }
}
