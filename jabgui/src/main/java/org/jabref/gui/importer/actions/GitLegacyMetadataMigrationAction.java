package org.jabref.gui.importer.actions;

import java.util.Collections;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.metadata.MetaData;

/// Migrates the legacy 'gitEnabled' metadata key to the separate
/// gitAutoPull, gitAutoCommit, and gitAutoPush keys.
public class GitLegacyMetadataMigrationAction implements GUIPostOpenAction {

    @Override
    public boolean isActionNecessary(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        // Run migration only if the old key is present and true
        return parserResult.getMetaData()
                           .getUnknownMetaData()
                           .getOrDefault(MetaData.LEGACY_GIT_ENABLED, Collections.emptyList())
                           .contains("true");
    }

    @Override
    public void performAction(ParserResult parserResult, DialogService dialogService, CliPreferences preferences) {
        MetaData metaData = parserResult.getMetaData();

        // Enable the separate settings to match previous behavior
        metaData.setGitAutoPullEnabled(true);
        metaData.setGitAutoCommitEnabled(true);
        metaData.setGitAutoPushEnabled(true);

        // Erase the old legacy key so this migration never runs again
        metaData.removeUnknownMetaDataItem(MetaData.LEGACY_GIT_ENABLED);

        // Mark the database as changed so the user is prompted to save the new metadata
        parserResult.setChangedOnMigration(true);
    }
}
