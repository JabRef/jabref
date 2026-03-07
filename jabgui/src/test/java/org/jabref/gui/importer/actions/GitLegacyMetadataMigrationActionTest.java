package org.jabref.gui.importer.actions;

import java.util.Collections;

import org.jabref.gui.DialogService;
import org.jabref.logic.importer.ParserResult;
import org.jabref.logic.preferences.CliPreferences;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class GitLegacyMetadataMigrationActionTest {
    private GitLegacyMetadataMigrationAction migrationAction;
    private ParserResult parserResult;
    private MetaData metaData;

    @BeforeEach
    void setUp() {
        migrationAction = new GitLegacyMetadataMigrationAction();

        metaData = new MetaData();
        parserResult = new ParserResult(new BibDatabase());
        parserResult.getDatabaseContext().setMetaData(metaData);
    }

    @Test
    void actionIsNecessaryWhenLegacyKeyIsPresent() {
        // Give the metadata the old legacy key
        metaData.putUnknownMetaDataItem(MetaData.LEGACY_GIT_ENABLED, Collections.singletonList("true"));

        // Check that the migration flags itself as necessary
        assertTrue(migrationAction.isActionNecessary(parserResult, mock(DialogService.class), mock(CliPreferences.class)));
    }

    @Test
    void actionIsNotNecessaryWhenLegacyKeyIsAbsent() {
        // Metadata is empty, so migration should not be necessary
        assertFalse(migrationAction.isActionNecessary(parserResult, mock(DialogService.class), mock(CliPreferences.class)));
    }

    @Test
    void performActionMigratesKeysAndRemovesLegacyKey() {
        // Setup the old state
        metaData.putUnknownMetaDataItem(MetaData.LEGACY_GIT_ENABLED, Collections.singletonList("true"));

        // Run the migration
        migrationAction.performAction(parserResult, mock(DialogService.class), mock(CliPreferences.class));

        // Verify the old key was deleted
        assertFalse(metaData.getUnknownMetaData().containsKey(MetaData.LEGACY_GIT_ENABLED));

        // Verify the new separate keys were turned on
        assertTrue(metaData.isGitAutoPullEnabled());
        assertTrue(metaData.isGitAutoCommitEnabled());
        assertTrue(metaData.isGitAutoPushEnabled());
    }
}
