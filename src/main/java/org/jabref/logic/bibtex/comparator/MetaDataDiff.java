package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.citationkeypattern.CitationKeyPattern;
import org.jabref.logic.citationkeypattern.GlobalCitationKeyPatterns;
import org.jabref.model.entry.field.Field;
import org.jabref.model.metadata.ContentSelectors;
import org.jabref.model.metadata.MetaData;

public class MetaDataDiff {
    public enum DifferenceType {
        CONTENT_SELECTOR,
        DEFAULT_KEY_PATTERN,
        ENCODING,
        GENERAL_FILE_DIRECTORY,
        GROUPS_ALTERED,
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
            if (isDefaultContentSelectorDiff(diff)) {
                return Optional.empty();
            }
            return Optional.of(diff);
        }
    }

    private static boolean isDefaultContentSelectorDiff(MetaDataDiff diff) {
        GlobalCitationKeyPatterns globalCitationKeyPatterns = new GlobalCitationKeyPatterns(CitationKeyPattern.NULL_CITATION_KEY_PATTERN);
        List<Difference> differences = diff.getDifferences(globalCitationKeyPatterns);
        if (differences.size() != 1) {
            return false;
        }
        Difference onlyDifference = differences.getFirst();
        if (onlyDifference.differenceType() != DifferenceType.CONTENT_SELECTOR) {
            return false;
        }
        ContentSelectors originalContentSelectors = (ContentSelectors) onlyDifference.originalObject();
        ContentSelectors newContentSelectors = (ContentSelectors) onlyDifference.newObject();
        if (isDefaultContentSelectorDiff(originalContentSelectors) && isDefaultContentSelectorDiff(newContentSelectors)) {
            return true;
        }
        return false;
    }

    /**
     * Checks if given content selectors are empty or default
     */
    private static boolean isDefaultContentSelectorDiff(ContentSelectors contentSelectors) {
        if (contentSelectors.getContentSelectors().isEmpty()) {
            return true;
        }
        Map<Field, List<String>> fieldKeywordsMap = ContentSelectors.getFieldKeywordsMap(contentSelectors.getContentSelectors());
        return ContentSelectors.isDefaultMap(fieldKeywordsMap);
    }

    private void addToListIfDiff(List<Difference> changes, DifferenceType differenceType, Object originalObject, Object newObject) {
        if (!Objects.equals(originalObject, newObject)) {
            changes.add(new Difference(differenceType, originalObject, newObject));
        }
    }

    /**
     * Should be kept in sync with {@link MetaData#equals(Object)}
     */
    public List<Difference> getDifferences(GlobalCitationKeyPatterns globalCitationKeyPatterns) {
        List<Difference> changes = new ArrayList<>();
        addToListIfDiff(changes, DifferenceType.PROTECTED, originalMetaData.isProtected(), newMetaData.isProtected());
        addToListIfDiff(changes, DifferenceType.GROUPS_ALTERED, originalMetaData.getGroups(), newMetaData.getGroups());
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
        addToListIfDiff(changes, DifferenceType.GENERAL_FILE_DIRECTORY, originalMetaData.getDefaultFileDirectory(), newMetaData.getDefaultFileDirectory());
        addToListIfDiff(changes, DifferenceType.CONTENT_SELECTOR, originalMetaData.getContentSelectors(), newMetaData.getContentSelectors());
        return changes;
    }

    public MetaData getNewMetaData() {
        return newMetaData;
    }

    public Optional<GroupDiff> getGroupDifferences() {
        return groupDiff;
    }

    @Override
    public String toString() {
        return "MetaDataDiff{" +
                "groupDiff=" + groupDiff +
                ", originalMetaData=" + originalMetaData +
                ", newMetaData=" + getNewMetaData() +
                '}';
    }
}
