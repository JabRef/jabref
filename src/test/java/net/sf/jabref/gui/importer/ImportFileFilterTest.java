package net.sf.jabref.gui.importer;


import java.util.Arrays;
import java.util.Collections;

import net.sf.jabref.logic.importer.fileformat.ImportFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class ImportFileFilterTest {

    @Test
    public void nameWithSingleExtensions() {
        ImportFormat importFormatSingleExtension = mock(ImportFormat.class);
        when(importFormatSingleExtension.getFormatName()).thenReturn("Single Extension");
        when(importFormatSingleExtension.getExtensions()).thenReturn(Collections.singletonList(".abc"));

        ImportFileFilter importFileFilter = new ImportFileFilter(importFormatSingleExtension);
        assertEquals("Single Extension (*.abc)", importFileFilter.getDescription());
    }

    @Test
    public void nameWithMultipleExtensions() {
        ImportFormat importFormatMultipleExtensions = mock(ImportFormat.class);
        when(importFormatMultipleExtensions.getFormatName()).thenReturn("Multiple Extensions");
        when(importFormatMultipleExtensions.getExtensions()).thenReturn(Arrays.asList(".abc", ".xyz"));

        ImportFileFilter importFileFilter = new ImportFileFilter(importFormatMultipleExtensions);
        assertEquals("Multiple Extensions (*.abc, *.xyz)", importFileFilter.getDescription());
    }

}
