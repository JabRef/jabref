package org.jabref.logic.bibtex.comparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.jabref.logic.l10n.Localization;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.PreferencesService;

public class MetaDataDiff {

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
    public List<String> getDifferences(PreferencesService preferences) {
        List<String> changes = new ArrayList<>();

        if (originalMetaData.isProtected() != newMetaData.isProtected()) {
            changes.add(Localization.lang("Library protection"));
        }
        if (!Objects.equals(originalMetaData.getGroups(), newMetaData.getGroups())) {
            changes.add(Localization.lang("Modified groups tree"));
        }
        if (!Objects.equals(originalMetaData.getEncoding(), newMetaData.getEncoding())) {
            changes.add(Localization.lang("Library encoding"));
        }
        if (!Objects.equals(originalMetaData.getSaveOrderConfig(), newMetaData.getSaveOrderConfig())) {
            changes.add(Localization.lang("Save sort order"));
        }
        if (!Objects.equals(originalMetaData.getCiteKeyPattern(preferences.getGlobalCitationKeyPattern()), newMetaData.getCiteKeyPattern(preferences.getGlobalCitationKeyPattern()))) {
            changes.add(Localization.lang("Key patterns"));
        }
        if (!Objects.equals(originalMetaData.getUserFileDirectories(), newMetaData.getUserFileDirectories())) {
            changes.add(Localization.lang("User-specific file directory"));
        }
        if (!Objects.equals(originalMetaData.getLatexFileDirectories(), newMetaData.getLatexFileDirectories())) {
            changes.add(Localization.lang("LaTeX file directory"));
        }
        if (!Objects.equals(originalMetaData.getDefaultCiteKeyPattern(), newMetaData.getDefaultCiteKeyPattern())) {
            changes.add(Localization.lang("Default pattern"));
        }
        if (!Objects.equals(originalMetaData.getSaveActions(), newMetaData.getSaveActions())) {
            changes.add(Localization.lang("Save actions"));
        }
        if (originalMetaData.getMode() != newMetaData.getMode()) {
            changes.add(Localization.lang("Library mode"));
        }
        if (!Objects.equals(originalMetaData.getDefaultFileDirectory(), newMetaData.getDefaultFileDirectory())) {
            changes.add(Localization.lang("General file directory"));
        }
        if (!Objects.equals(originalMetaData.getContentSelectors(), newMetaData.getContentSelectors())) {
            changes.add(Localization.lang("Content selectors"));
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
