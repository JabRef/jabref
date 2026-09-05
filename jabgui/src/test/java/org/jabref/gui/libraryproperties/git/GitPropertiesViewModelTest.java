package org.jabref.gui.libraryproperties.git;

import org.jabref.model.database.BibDatabase;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.metadata.MetaData;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitPropertiesViewModelTest {

    private MetaData metaData;
    private GitPropertiesViewModel viewModel;

    @BeforeEach
    void setUp() {
        metaData = new MetaData();
        viewModel = new GitPropertiesViewModel(new BibDatabaseContext(new BibDatabase(), metaData));
    }

    @Test
    void setValuesReadsMetaData() {
        metaData.setGitAutoCommit(true);
        metaData.setGitAutoPull(true);

        viewModel.setValues();

        assertTrue(viewModel.autoCommitProperty().getValue());
        assertTrue(viewModel.autoPullProperty().getValue());
        assertFalse(viewModel.autoPushProperty().getValue());
    }

    @Test
    void storeSettingsWritesMetaData() {
        viewModel.autoCommitProperty().setValue(true);
        viewModel.autoPushProperty().setValue(true);

        viewModel.storeSettings();

        assertTrue(metaData.isGitAutoCommit());
        assertTrue(metaData.isGitAutoPush());
        assertFalse(metaData.isGitAutoPull());
    }
}
