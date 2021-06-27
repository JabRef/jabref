package org.jabref.gui.externalfiles;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
            List<String> actualsList = new ArrayList<String>();
            List<Path> sortedPaths = fileFilterUtils.sortByDateAscending(files);
            String[] expecteds = new String[4];
            String[] actuals = new String[4];
            for (Path path : sortedPaths) {
                actualsList.add(path.toString());
            }
            actualsList.toArray(actuals);
            expectedSortByDateAscending.toArray(expecteds);
            assertArrayEquals(actuals, expecteds);
        }

        @Test
        public void sortByDateAscendingNegativeTest() {
            List<String> actualsList = new ArrayList<String>();
            List<Path> sortedPaths = fileFilterUtils.sortByDateAscending(files);
            String[] expecteds = new String[4];
            String[] actuals = new String[4];
            for (Path path : sortedPaths) {
                actualsList.add(path.toString());
            }
            actualsList.toArray(actuals);
            wrongOrder.toArray(expecteds);
            assertFalse(Arrays.equals(actuals, expecteds));
        }
    
        @Test
        public void sortByDateDescendingPositiveTest() {
            List<String> actualsList = new ArrayList<String>();
            List<Path> sortedPaths = fileFilterUtils.sortByDateDescending(files);
            String[] expecteds = new String[4];
            String[] actuals = new String[4];
            for (Path path : sortedPaths) {
                actualsList.add(path.toString());
            }
            actualsList.toArray(actuals);
            expectedSortByDateDescending.toArray(expecteds);
            assertArrayEquals(actuals, expecteds);
        }

        @Test
        public void testSortByDateDescendingNegativeTest() {
            List<String> actualsList = new ArrayList<String>();
            List<Path> sortedPaths = fileFilterUtils.sortByDateDescending(files);
            String[] expecteds = new String[4];
            String[] actuals = new String[4];
            for (Path path : sortedPaths) {
                actualsList.add(path.toString());
            }
            actualsList.toArray(actuals);
            wrongOrder.toArray(expecteds);
            assertFalse(Arrays.equals(actuals, expecteds));
        }
    }
}
