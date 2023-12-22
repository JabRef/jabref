package org.jabref.gui.mergeentries.newmergedialog.fieldsmerger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FileMergerTest {
    FileMerger fileMerger = new FileMerger();

    /**
     * Test the following cases
     * nullvalues
     * emptyString for FileB
     * emptyString for FileA
     * FileA and FileB are valid strings and are separated by semicolon
     *
     * @param expect Expected value
     * @param fileA File string a
     * @param fileB File String b
     */
    @ParameterizedTest
    @CsvSource(textBlock = """
                ,,,
                FileA,FileA,
                FileA,FileA,''
                FileB, ,FileB
                FileB,'',FileB
                :A2012 -A.pdf:PDF;B2013 - B.pdf:PDF:,:A2012 -A.pdf:PDF,B2013 - B.pdf:PDF
                :A2012 -A.pdf:;B2013 - B.pdf:PDF:,A2012 -A.pdf,B2013 - B.pdf:PDF
                :A2012 -A.pdf:;:asdf:,A2012 -A.pdf,asdf
            """)
    void testMerge(String expect, String fileA, String fileB) {
        assertEquals(expect, fileMerger.merge(fileA, fileB));
    }
}
