package org.jabref.gui.externalfiles;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class FindUnlinkedFilesViewModelTest {
    @Test
    public void disableImportButton() throws Exception {
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No target directory specified
        Assert.assertFalse(findUnlinkedFilesViewModel.isDirectorySpecifiedProperty().get());

        // No unlinked files available
        Assert.assertEquals(findUnlinkedFilesViewModel.totalUnlinkedFilesProperty().get(), 0);
    }

    @Test
    public void disableScanDirectory() throws Exception {
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No target directory specified
        Assert.assertFalse(findUnlinkedFilesViewModel.isDirectorySpecifiedProperty().get());
    }

    @Test
    public void disableSelectAll(){
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No unlinked files available
        Assert.assertEquals(findUnlinkedFilesViewModel.totalUnlinkedFilesProperty().get(), 0);

        // No unlinked files unselected
        Assert.assertEquals(findUnlinkedFilesViewModel.totalUnlinkedFilesProperty().get()
                - findUnlinkedFilesViewModel.totalUnlinkedFilesSelectedProperty().get(), 0);
    }

    @Test
    public void disableUnselectAll(){
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No unlinked files available
        Assert.assertEquals(findUnlinkedFilesViewModel.totalUnlinkedFilesProperty().get(), 0);

        // No unlinked files selected
        Assert.assertEquals(findUnlinkedFilesViewModel.totalUnlinkedFilesSelectedProperty().get(), 0);
    }

    @Test
    public void disableExpandAll(){
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No unlinked files available
        Assert.assertEquals(findUnlinkedFilesViewModel.numberOfUnlinkedFilesProperty().get(), 0);

        // No collapsed trees available
        Assert.assertEquales(findUnlinkedFilesViewModel.totalTreesCollapsedProperty().get(), 0);
    }

    @Test
    public void disableCollapseAll(){
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No unlinked files available
        Assert.assertEquals(findUnlinkedFilesViewModel.numberOfUnlinkedFilesProperty().get(), 0);

        // No expanded trees available
        Assert.assertEquales(findUnlinkedFilesViewModel.totalTreesProperty().get()
                - findUnlinkedFilesViewModel.totalTreesCollapsedProperty().get(), 0);
    }

    @Test
    public void disableExportSelectedEntries(){
        FindUnlinkedFilesViewModel findUnlinkedFilesViewModel = new FindUnlinkedFilesViewModel();

        // No unlinked files available
        Assert.assertEquals(findUnlinkedFilesViewModel.numberOfUnlinkedFilesProperty().get(), 0);

        // No unlinked files selected
        Assert.assertEquals(findUnlinkedFilesViewModel.totalUnlinkedFilesSelectedProperty().get(), 0);
    }
}
