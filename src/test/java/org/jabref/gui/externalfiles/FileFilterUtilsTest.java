package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileFilterUtilsTest {

    private final FileFilterUtils fileFilterUtils = new FileFilterUtils();
    private final LocalDateTime time = LocalDateTime.now();

    @Test
    public void isDuringLastDayNegativeTest() {
        assertFalse(fileFilterUtils.isDuringLastDay(time.minusHours(24)));
    }

    @Test
    public void isDuringLastDayPositiveTest() {
        assertTrue(fileFilterUtils.isDuringLastDay(time.minusHours(23)));
    }

    @Test
    public void isDuringLastWeekNegativeTest() {
        assertFalse(fileFilterUtils.isDuringLastWeek(time.minusDays(7)));
    }

    @Test
    public void isDuringLastWeekPositiveTest() {
        assertTrue(fileFilterUtils.isDuringLastWeek(time.minusDays(6).minusHours(23)));
    }

    @Test
    public void isDuringLastMonthNegativeTest() {
        assertFalse(fileFilterUtils.isDuringLastMonth(time.minusDays(30)));
    }

    @Test
    public void isDuringLastMonthPositiveTest() {
        assertTrue(fileFilterUtils.isDuringLastMonth(time.minusDays(29).minusHours(23)));
    }

    @Test
    public void isDuringLastYearNegativeTest() {
        assertFalse(fileFilterUtils.isDuringLastYear(time.minusDays(365)));
    }

    @Test
    public void isDuringLastYearPositiveTest() {
        assertTrue(fileFilterUtils.isDuringLastYear(time.minusDays(364).minusHours(23)));
    }

    @Nested
    class SortingTests {

        private final List<Path> files = new ArrayList<>();
        private final List<String> expectedSortByDateAscending = new ArrayList<>();
        private final List<String> expectedSortByDateDescending = new ArrayList<>();
        private final List<String> wrongOrder = new ArrayList<>();

        /* Initialize the directory and files used in the sorting tests, and change their last edited dates. */
        @BeforeEach
        public void setUp(@TempDir Path tempDir) throws Exception {

            Path firstPath = tempDir.resolve("firstFile.pdf");
            Path secondPath = tempDir.resolve("secondFile.pdf");
            Path thirdPath = tempDir.resolve("thirdFile.pdf");
            Path fourthPath = tempDir.resolve("fourthFile.pdf");

            Files.createFile(firstPath);
            Files.createFile(secondPath);
            Files.createFile(thirdPath);
            Files.createFile(fourthPath);

            // change the files last edited times.
            Files.setLastModifiedTime(firstPath, FileTime.fromMillis(10));
            Files.setLastModifiedTime(secondPath, FileTime.fromMillis(5));
            Files.setLastModifiedTime(thirdPath, FileTime.fromMillis(1));
            Files.setLastModifiedTime(fourthPath, FileTime.fromMillis(2));

            // fill the list to be sorted by the tests.
            files.add(firstPath);
            files.add(secondPath);
            files.add(thirdPath);
            files.add(fourthPath);

            // fill the expected values lists.
            expectedSortByDateAscending.add(thirdPath.toString());
            expectedSortByDateAscending.add(fourthPath.toString());
            expectedSortByDateAscending.add(secondPath.toString());
            expectedSortByDateAscending.add(firstPath.toString());

            expectedSortByDateDescending.add(firstPath.toString());
            expectedSortByDateDescending.add(secondPath.toString());
            expectedSortByDateDescending.add(fourthPath.toString());
            expectedSortByDateDescending.add(thirdPath.toString());

            wrongOrder.add(firstPath.toString());
            wrongOrder.add(secondPath.toString());
            wrongOrder.add(thirdPath.toString());
            wrongOrder.add(fourthPath.toString());
        }

        @Test
        public void sortByDateAscendingPositiveTest() {
            List<String> sortedPaths = fileFilterUtils
                .sortByDateAscending(files)
                .stream()
                .map(Path::toString)
                .collect(Collectors.toList());
            assertEquals(sortedPaths, expectedSortByDateAscending);
        }

        @Test
        public void sortByDateAscendingNegativeTest() {
            List<String> sortedPaths = fileFilterUtils
                .sortByDateAscending(files)
                .stream()
                .map(Path::toString)
                .collect(Collectors.toList());
            assertNotEquals(sortedPaths, wrongOrder);
        }

        @Test
        public void sortByDateDescendingPositiveTest() {
            List<String> sortedPaths = fileFilterUtils
                .sortByDateDescending(files)
                .stream()
                .map(Path::toString)
                .collect(Collectors.toList());
            assertEquals(sortedPaths, expectedSortByDateDescending);
        }

        @Test
        public void testSortByDateDescendingNegativeTest() {
            List<String> sortedPaths = fileFilterUtils
                .sortByDateDescending(files)
                .stream()
                .map(Path::toString)
                .collect(Collectors.toList());
            assertNotEquals(sortedPaths, wrongOrder);
        }
    }

    @Nested
    class filteringTests {
        private final List<Path> files = new ArrayList<>();
        private final List<Path> targetFiles = new ArrayList<>();
        private final Set<String> ignoreFileSet = new HashSet<>();

        @BeforeEach
        public void setUp(@TempDir Path tempDir) throws Exception {
            ignoreFileSet.add(".DS_Store");
            ignoreFileSet.add("Thumbs.db");

            Path firstPath = tempDir.resolve("firstFile.pdf");
            Path secondPath = tempDir.resolve("secondFile.pdf");
            Path thirdPath = tempDir.resolve("thirdFile.pdf");
            Path fourthPath = tempDir.resolve("fourthFile.pdf");
            Path fifthPath = tempDir.resolve(".DS_Store");
            Path sixthPath = tempDir.resolve("Thumbs.db");

            Files.createFile(firstPath);
            Files.createFile(secondPath);
            Files.createFile(thirdPath);
            Files.createFile(fourthPath);
            Files.createFile(fifthPath);
            Files.createFile(sixthPath);

            files.add(firstPath);
            files.add(secondPath);
            files.add(thirdPath);
            files.add(fourthPath);
            files.add(fifthPath);
            files.add(sixthPath);

            targetFiles.add(firstPath);
            targetFiles.add(secondPath);
            targetFiles.add(thirdPath);
            targetFiles.add(fourthPath);
        }
    }
}
