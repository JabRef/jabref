package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.logic.groups.DefaultGroupsFactory;
import org.jabref.model.entry.field.Field;
import org.jabref.model.groups.GroupTreeNode;
import org.jabref.model.metadata.ContentSelectors;
import org.jabref.model.metadata.MetaData;

public class MetaDataDiff {
    public enum DifferenceType {
        CONTENT_SELECTOR,
        DEFAULT_KEY_PATTERN,
        ENCODING,
        LIBRARY_SPECIFIC_FILE_DIRECTORY,
        GROUPS,
        KEY_PATTERNS,
        LATEX_FILE_DIRECTORY,
        MODE,
        PROTECTED,
        SAVE_ACTIONS,
        SAVE_SORT_ORDER,
        USER_FILE_DIRECTORY
    }

    public record Difference(DifferenceType differenceType, Object originalObject, Object newObject) {
    }

    private final Optional<GroupDiff> groupDiff;
    private final MetaData originalMetaData;
    private final MetaData newMetaData;

    private MetaDataDiff(MetaData originalMetaData, MetaData newMetaData) {
        this.originalMetaData = originalMetaData;
        this.newMetaData = newMetaData;
        this.groupDiff = GroupDiff.compare(originalMetaData, newMetaData);
    }

    public static Optional<MetaDataDiff> compare(MetaData originalMetaData, MetaData newMetaData) {
        if (originalMetaData.equals(newMetaData)) {
            return Optional.empty();
        } else {
            MetaDataDiff diff = new MetaDataDiff(originalMetaData, newMetaData);
            List<Difference> differences = diff.getDifferences(new GlobalCitationKeyPatterns(CitationKeyPattern.NULL_CITATION_KEY_PATTERN));
            if (differences.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(diff);
        }
    }

    /**
     * Checks if given content selectors are empty or default
     */
    private static boolean isDefaultContentSelectors(ContentSelectors contentSelectors) {
        if (contentSelectors.getContentSelectors().isEmpty()) {
            return true;
        }
        Map<Field, List<String>> fieldKeywordsMap = ContentSelectors.getFieldKeywordsMap(contentSelectors.getContentSelectors());
        return ContentSelectors.isDefaultMap(fieldKeywordsMap);
    }

    private void addToListIfDiff(List<Difference> changes, DifferenceType differenceType, Object originalObject, Object newObject) {
        if (!Objects.equals(originalObject, newObject)) {
            if (differenceType == DifferenceType.CONTENT_SELECTOR) {
                ContentSelectors originalContentSelectors = (ContentSelectors) originalObject;
                ContentSelectors newContentSelectors = (ContentSelectors) newObject;
                if (isDefaultContentSelectors(originalContentSelectors) && isDefaultContentSelectors(newContentSelectors)) {
                    return;
                }
            } else if (differenceType == DifferenceType.GROUPS) {
                Optional<GroupTreeNode> originalGroups = (Optional<GroupTreeNode>) originalObject;
                Optional<GroupTreeNode> newGroups = (Optional<GroupTreeNode>) newObject;
                if (isDefaultGroup(originalGroups) && isDefaultGroup(newGroups)) {
                    return;
                }
            }
            changes.add(new Difference(differenceType, originalObject, newObject));
        }
    }

    private boolean isDefaultGroup(Optional<GroupTreeNode> groups) {
        if (groups.isEmpty()) {
            return true;
        }
        GroupTreeNode groupRoot = groups.get();
        if (!groupRoot.getChildren().isEmpty()) {
            return false;
        }
        return groupRoot.getGroup().equals(DefaultGroupsFactory.getAllEntriesGroup());
    }

    /**
     * Should be kept in sync with {@link MetaData#equals(Object)}
     */
    public List<Difference> getDifferences(GlobalCitationKeyPatterns globalCitationKeyPatterns) {
        List<Difference> changes = new ArrayList<>();
        addToListIfDiff(changes, DifferenceType.PROTECTED, originalMetaData.isProtected(), newMetaData.isProtected());
        addToListIfDiff(changes, DifferenceType.GROUPS, originalMetaData.getGroups(), newMetaData.getGroups());
        addToListIfDiff(changes, DifferenceType.ENCODING, originalMetaData.getEncoding(), newMetaData.getEncoding());
        addToListIfDiff(changes, DifferenceType.SAVE_SORT_ORDER, originalMetaData.getSaveOrder(), newMetaData.getSaveOrder());
        addToListIfDiff(changes, DifferenceType.KEY_PATTERNS,
                originalMetaData.getCiteKeyPatterns(globalCitationKeyPatterns),
                newMetaData.getCiteKeyPatterns(globalCitationKeyPatterns));
        addToListIfDiff(changes, DifferenceType.USER_FILE_DIRECTORY, originalMetaData.getUserFileDirectories(), newMetaData.getUserFileDirectories());
        addToListIfDiff(changes, DifferenceType.LATEX_FILE_DIRECTORY, originalMetaData.getLatexFileDirectories(), newMetaData.getLatexFileDirectories());
        addToListIfDiff(changes, DifferenceType.DEFAULT_KEY_PATTERN, originalMetaData.getDefaultCiteKeyPattern(), newMetaData.getDefaultCiteKeyPattern());
        addToListIfDiff(changes, DifferenceType.SAVE_ACTIONS, originalMetaData.getSaveActions(), newMetaData.getSaveActions());
        addToListIfDiff(changes, DifferenceType.MODE, originalMetaData.getMode(), newMetaData.getMode());
        addToListIfDiff(changes, DifferenceType.LIBRARY_SPECIFIC_FILE_DIRECTORY, originalMetaData.getLibrarySpecificFileDirectory(), newMetaData.getLibrarySpecificFileDirectory());
        addToListIfDiff(changes, DifferenceType.CONTENT_SELECTOR, originalMetaData.getContentSelectors(), newMetaData.getContentSelectors());
        return changes;
    }

    public MetaData getNewMetaData() {
        return newMetaData;
    }

    /**
     * Currently, the groups diff is contained here - and as entry in {@link #getDifferences(GlobalCitationKeyPatterns)}
     */
    public Optional<GroupDiff> getGroupDifferences() {
        return groupDiff;
    }

    @Override
    public String toString() {
        return "MetaDataDiff{" +
                "groupDiff=" + groupDiff +
                ", originalMetaData=" + originalMetaData +
                ", newMetaData=" + getNewMetaData() +
                ", getDifferences()=" + getDifferences(new GlobalCitationKeyPatterns(CitationKeyPattern.NULL_CITATION_KEY_PATTERN)) +
                '}';
    }
}
