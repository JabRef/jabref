package org.jabref.gui.libraryproperties.git;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitPropertiesViewModelTest {
    private BibDatabaseContext databaseContext;
    private MetaData metaData;
    private GitPropertiesViewModel viewModel;

    @BeforeEach
    void setUp() {
        databaseContext = new BibDatabaseContext();
        metaData = new MetaData();
        databaseContext.setMetaData(metaData);

        viewModel = new GitPropertiesViewModel(databaseContext);
    }

    @Test
    void initialStateIsFalse() {
        viewModel.setValues();
        assertFalse(viewModel.autoCommitProperty().get());
        assertFalse(viewModel.autoPullProperty().get());
        assertFalse(viewModel.autoPushProperty().get());
    }

    @Test
    void setValuesReadsLegacyKey() {
        metaData.putUnknownMetaDataItem(GitPropertiesViewModel.LEGACY_GIT_ENABLED, Collections.singletonList("true"));

        viewModel.setValues();

        assertTrue(viewModel.autoCommitProperty().get());
        assertTrue(viewModel.autoPullProperty().get());
        assertTrue(viewModel.autoPushProperty().get());
    }

    @Test
    void setValuesReadsGranularKeys() {
        metaData.putUnknownMetaDataItem(GitPropertiesViewModel.GIT_AUTO_COMMIT, Collections.singletonList("true"));

        viewModel.setValues();

        assertTrue(viewModel.autoCommitProperty().get());
        assertFalse(viewModel.autoPullProperty().get());
        assertFalse(viewModel.autoPushProperty().get());
    }

    @Test
    void storeSettingsWritesGranularKeys() {
        // checks "Commit" and "Push"
        viewModel.autoCommitProperty().set(true);
        viewModel.autoPullProperty().set(false);
        viewModel.autoPushProperty().set(true);

        viewModel.storeSettings();

        Map<String, List<String>> data = metaData.getUnknownMetaData();

        assertTrue(data.containsKey(GitPropertiesViewModel.GIT_AUTO_COMMIT));
        assertFalse(data.containsKey(GitPropertiesViewModel.GIT_AUTO_PULL));
        assertTrue(data.containsKey(GitPropertiesViewModel.GIT_AUTO_PUSH));
    }

    @Test
    void storeSettingsRemovesLegacyKey() {
        metaData.putUnknownMetaDataItem(GitPropertiesViewModel.LEGACY_GIT_ENABLED, Collections.singletonList("true"));

        viewModel.setValues();

        viewModel.autoCommitProperty().set(true);
        viewModel.autoPullProperty().set(true);
        viewModel.autoPushProperty().set(true);

        viewModel.storeSettings();

        Map<String, List<String>> data = metaData.getUnknownMetaData();

        assertFalse(data.containsKey(GitPropertiesViewModel.LEGACY_GIT_ENABLED));

        assertTrue(data.containsKey(GitPropertiesViewModel.GIT_AUTO_COMMIT));
        assertTrue(data.containsKey(GitPropertiesViewModel.GIT_AUTO_PULL));
        assertTrue(data.containsKey(GitPropertiesViewModel.GIT_AUTO_PUSH));
    }
}
