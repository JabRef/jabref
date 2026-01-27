package org.jabref.logic.database;

import java.util.List;
import java.util.stream.Collectors;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.database.BibDatabaseModeDetection;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.BibEntryTypesManager;
import org.jabref.model.entry.BibtexString;
import org.jabref.model.groups.AllEntriesGroup;
import org.jabref.model.groups.ExplicitGroup;
import org.jabref.model.groups.GroupHierarchyType;
import org.jabref.model.metadata.ContentSelector;
import org.jabref.model.metadata.MetaData;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseMerger {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseMerger.class);
    private final char keywordDelimiter;

    public DatabaseMerger(char keywordDelimiter) {
        this.keywordDelimiter = keywordDelimiter;
    }

    /**
     * Merges all entries and strings of the other database into the target database.
     * Any duplicates are ignored.
     * In case a string has a different content, it is added with a new unique name.
     *
     * @param target the target database
     * @param other  the database that is merged into the target
     */
    public synchronized void merge(BibDatabase target, BibDatabase other) {
        mergeEntries(target, other);
        mergeStrings(target, other);
    }

    /**
     * Merges all entries, strings, and metadata of the other database context
     * into the target database context.
     *
     * @param target        the target database context
     * @param other         the database context to merge from
     * @param otherFileName the filename of the imported library
     */
    public synchronized void merge(BibDatabaseContext target, BibDatabaseContext other, String otherFileName) {
        mergeEntries(target.getDatabase(), other.getDatabase());
        mergeStrings(target.getDatabase(), other.getDatabase());
        mergeMetaData(target.getMetaData(), other.getMetaData(), otherFileName, other.getEntries());
    }

    private void mergeEntries(BibDatabase target, BibDatabase other) {
        DuplicateCheck duplicateCheck = new DuplicateCheck(new BibEntryTypesManager());
        List<BibEntry> newEntries = other.getEntries().stream()
                                         // Remove all entries that are already part of the database (duplicate)
                                         .filter(entry -> duplicateCheck.containsDuplicate(target, entry, BibDatabaseModeDetection.inferMode(target)).isEmpty()).collect(Collectors.toList());
        target.insertEntries(newEntries);
    }

    public void mergeStrings(BibDatabase target, BibDatabase other) {
        for (BibtexString bibtexString : other.getStringValues()) {
            String bibtexStringName = bibtexString.getName();
            if (target.hasStringByName(bibtexStringName)) {
                String importedContent = bibtexString.getContent();
                String existingContent = target.getStringByName(bibtexStringName).get().getContent();
                if (!importedContent.equals(existingContent)) {
                    LOGGER.info("String contents differ for {}: {} != {}", bibtexStringName, importedContent, existingContent);
                    int suffix = 1;
                    String newName = bibtexStringName + "_" + suffix;
                    while (target.hasStringByName(newName)) {
                        suffix++;
                        newName = bibtexStringName + "_" + suffix;
                    }
                    BibtexString newBibtexString = new BibtexString(newName, importedContent);
                    // TODO undo/redo
                    target.addString(newBibtexString);
                    LOGGER.info("New string added: {} = {}", newBibtexString.getName(), newBibtexString.getContent());
                }
            } else {
                // TODO undo/redo
                target.addString(bibtexString);
            }
        }
    }

    /**
     * Merges metadata from another library into the target metadata.
     *
     * @param target          the metadata merge target
     * @param other           the metadata to merge from
     * @param otherFilename   the filename of the imported library
     * @param allOtherEntries all entries from the imported library
     */
    public void mergeMetaData(@NonNull MetaData target, @NonNull MetaData other, @NonNull String otherFilename, @NonNull List<BibEntry> allOtherEntries) {
        mergeGroups(target, other, otherFilename, allOtherEntries);
        mergeContentSelectors(target, other);
    }

    /**
     * Merges groups from the imported metadata into the target metadata.
     *
     * @param target          the metadata merge target
     * @param other           the metadata to merge from
     * @param otherFilename   the filename of the imported library
     * @param allOtherEntries all entries from the imported library
     */
    private void mergeGroups(@NonNull MetaData target,
                             @NonNull MetaData other,
                             @NonNull String otherFilename,
                             @NonNull List<BibEntry> allOtherEntries) {

        // Adds the specified node as a child of the current root. The group contained in <b>newGroups</b> must not be of
        // type AllEntriesGroup, since every tree has exactly one AllEntriesGroup (its root). The <b>newGroups</b> are
        // inserted directly, i.e. they are not deepCopy()'d.
        other.getGroups().ifPresent(newGroups -> {
            // ensure that there is always only one AllEntriesGroup in the resulting database
            // "Rename" the AllEntriesGroup of the imported database to "Imported"
            if (newGroups.getGroup() instanceof AllEntriesGroup) {
                // create a dummy group
                try {
                    // This will cause a bug if the group already exists
                    // There will be group where the two groups are merged
                    ExplicitGroup group = new ExplicitGroup("Imported " + otherFilename, GroupHierarchyType.INDEPENDENT, keywordDelimiter);
                    newGroups.setGroup(group);
                    group.add(allOtherEntries);
                } catch (IllegalArgumentException e) {
                    LOGGER.error("Problem appending entries to group", e);
                }
            }
            target.getGroups().ifPresentOrElse(newGroups::moveTo,
                    // target does not contain any groups, so we can just use the new groups
                    () -> target.setGroups(newGroups));

            target.groupsBinding().invalidate();
        });
    }

    /**
     * Merges content selectors from the imported metadata into the target metadata.
     *
     * @param target the metadata merge target
     * @param other  the metadata to merge from
     */
    private void mergeContentSelectors(MetaData target, MetaData other) {
        for (ContentSelector selector : other.getContentSelectorsSorted()) {
            target.addContentSelector(selector);
        }
    }
}
