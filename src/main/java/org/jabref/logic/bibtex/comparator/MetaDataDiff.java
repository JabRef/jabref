package org.jabref.logic.bibtex.comparator;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class MetaDataDiff {
    public enum Difference {
        PROTECTED,
        GROUPS_ALTERED,
        ENCODING,
        SAVE_SORT_ORDER,
        KEY_PATTERNS,
        USER_FILE_DIRECTORY,
        LATEX_FILE_DIRECTORY,
        DEFAULT_KEY_PATTERN,
        SAVE_ACTIONS,
        MODE,
        GENERAL_FILE_DIRECTORY,
        CONTENT_SELECTOR
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
            return Optional.of(new MetaDataDiff(originalMetaData, newMetaData));
        }
    }

    /**
     * @implNote Should be kept in sync with {@link MetaData#equals(Object)}
     */
    public EnumSet<Difference> getDifferences(PreferencesService preferences) {
        EnumSet<Difference> changes = EnumSet.noneOf(Difference.class);

        if (originalMetaData.isProtected() != newMetaData.isProtected()) {
            changes.add(Difference.PROTECTED);
        }
        if (!Objects.equals(originalMetaData.getGroups(), newMetaData.getGroups())) {
            changes.add(Difference.GROUPS_ALTERED);
        }
        if (!Objects.equals(originalMetaData.getEncoding(), newMetaData.getEncoding())) {
            changes.add(Difference.ENCODING);
        }
        if (!Objects.equals(originalMetaData.getSaveOrderConfig(), newMetaData.getSaveOrderConfig())) {
            changes.add(Difference.SAVE_SORT_ORDER);
        }
        if (!Objects.equals(originalMetaData.getCiteKeyPattern(preferences.getGlobalCitationKeyPattern()), newMetaData.getCiteKeyPattern(preferences.getGlobalCitationKeyPattern()))) {
            changes.add(Difference.KEY_PATTERNS);
        }
        if (!Objects.equals(originalMetaData.getUserFileDirectories(), newMetaData.getUserFileDirectories())) {
            changes.add(Difference.USER_FILE_DIRECTORY);
        }
        if (!Objects.equals(originalMetaData.getLatexFileDirectories(), newMetaData.getLatexFileDirectories())) {
            changes.add(Difference.LATEX_FILE_DIRECTORY);
        }
        if (!Objects.equals(originalMetaData.getDefaultCiteKeyPattern(), newMetaData.getDefaultCiteKeyPattern())) {
            changes.add(Difference.DEFAULT_KEY_PATTERN);
        }
        if (!Objects.equals(originalMetaData.getSaveActions(), newMetaData.getSaveActions())) {
            changes.add(Difference.SAVE_ACTIONS);
        }
        if (originalMetaData.getMode() != newMetaData.getMode()) {
            changes.add(Difference.MODE);
        }
        if (!Objects.equals(originalMetaData.getDefaultFileDirectory(), newMetaData.getDefaultFileDirectory())) {
            changes.add(Difference.GENERAL_FILE_DIRECTORY);
        }
        if (!Objects.equals(originalMetaData.getContentSelectors(), newMetaData.getContentSelectors())) {
            changes.add(Difference.CONTENT_SELECTOR);
        }
        return changes;
    }

    public MetaData getNewMetaData() {
        return newMetaData;
    }

    public Optional<GroupDiff> getGroupDifferences() {
        return groupDiff;
    }
}
