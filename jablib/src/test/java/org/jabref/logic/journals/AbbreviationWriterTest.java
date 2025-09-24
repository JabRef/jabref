package org.jabref.logic.journals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Execution(ExecutionMode.CONCURRENT)
class AbbreviationWriterTest {

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideAbbreviationWriteTestCases")
    void testAbbreviationWriting(String testName, Abbreviation abbreviation, List<String> expectedOutput, @TempDir Path tempDir) throws IOException {
        Path csvFile = tempDir.resolve("test.csv");
        AbbreviationWriter.writeOrCreate(csvFile, List.of(abbreviation));
        assertEquals(expectedOutput, Files.readAllLines(csvFile));
    }

    private static Stream<Arguments> provideAbbreviationWriteTestCases() {
        return Stream.of(
                // Write shortest unique abbreviation when it differs from regular abbreviation
                Arguments.of("shortestUniqueAbbreviationWrittenIfItDiffers",
                        new Abbreviation("Full", "Abbr", "A"),
                        List.of("Full,Abbr,A")),

                // Do not write shortest unique abbreviation when it's the same as regular abbreviation
                Arguments.of("doNotWriteShortestUniqueAbbreviationWrittenIfItDiffers",
                        new Abbreviation("Full", "Abbr"),
                        List.of("Full,Abbr"))
        );
    }
}
