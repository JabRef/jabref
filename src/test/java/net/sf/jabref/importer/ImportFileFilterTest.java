package net.sf.jabref.importer;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.sf.jabref.importer.fileformat.ImportFormat;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImportFileFilterTest {

    @Test
    public void nameWithSingleExtensions() {
        ImportFormat importFormatSingleExtension = new ImportFormat() {
            @Override
            protected boolean isRecognizedFormat(BufferedReader input) throws IOException {
                return false;
            }

            @Override
            protected ParserResult importDatabase(BufferedReader input) throws IOException {
                return null;
            }

            @Override
            public String getFormatName() {
                return "Single Extension";
            }

            @Override
            public List<String> getExtensions() {
                return Collections.singletonList(".abc");
            }

            @Override
            public String getDescription() {
                return null;
            }
        };

        ImportFileFilter importFileFilter = new ImportFileFilter(importFormatSingleExtension);
        assertEquals("Single Extension (*.abc)", importFileFilter.getDescription());
    }

    @Test
    public void nameWithMultipleExtensions() {
        ImportFormat importFormatMultipleExtensions = new ImportFormat() {
            @Override
            protected boolean isRecognizedFormat(BufferedReader input) throws IOException {
                return false;
            }

            @Override
            protected ParserResult importDatabase(BufferedReader input) throws IOException {
                return null;
            }

            @Override
            public String getFormatName() {
                return "Multiple Extensions";
            }

            @Override
            public List<String> getExtensions() {
                return Arrays.asList(".abc", ".xyz");
            }

            @Override
            public String getDescription() {
                return null;
            }
        };

        ImportFileFilter importFileFilter = new ImportFileFilter(importFormatMultipleExtensions);
        assertEquals("Multiple Extensions (*.abc, *.xyz)", importFileFilter.getDescription());
    }

}
