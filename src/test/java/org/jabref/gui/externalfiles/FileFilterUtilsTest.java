package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class FileFilterUtilsTest {

    private final FileFilterUtils fileFilterUtils = new FileFilterUtils();
    private final LocalDateTime time = LocalDateTime.now();

    @Test
    public void isDuringLastDayNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastDay(time.minusHours(24)), false);
    }

    @Test
    public void isDuringLastDayPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastDay(time.minusHours(23)), true);
    }

    @Test
    public void isDuringLastWeekNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastWeek(time.minusDays(7)), false);
    }

    @Test
    public void isDuringLastWeekPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastWeek(time.minusDays(6).minusHours(23)), true);
    }

    @Test
    public void isDuringLastMonthNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastMonth(time.minusDays(30)), false);
    }

    @Test
    public void isDuringLastMonthPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastMonth(time.minusDays(29).minusHours(23)), true);
    }

    @Test
    public void isDuringLastYearNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastYear(time.minusDays(365)), false);
    }

    @Test
    public void isDuringLastYearPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastYear(time.minusDays(364).minusHours(23)), true);
    }

    @Nested
    class SortingTests {

        private List<Path> files = new ArrayList<Path>();
        private List<String> expectedSortByDateAscending = new ArrayList<String>();
        private List<String> expectedSortByDateDescending = new ArrayList<String>();
        private List<String> wrongOrder = new ArrayList<String>();

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
}
