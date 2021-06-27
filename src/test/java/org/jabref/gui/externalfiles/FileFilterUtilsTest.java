package org.jabref.gui.externalfiles;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileFilterUtilsTest {

    private final FileFilterUtils fileFilterUtils = new FileFilterUtils();
    private final LocalDateTime time = LocalDateTime.now();

    private List<Path> files = new ArrayList<Path>();
    private List<String> expectedSortByDateAscending = new ArrayList<String>();
    private List<String> expectedSortByDateDescending = new ArrayList<String>();

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
        assertEquals(fileFilterUtils.isDuringLastMonth(time.minusMonths(1)), false);
    }

    @Test
    public void isDuringLastMonthPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastMonth(time.minusDays(29).minusHours(23)), true);
    }

    @Test
    public void isDuringLastYearNegativeTest() {
        assertEquals(fileFilterUtils.isDuringLastYear(time.minusYears(1)), false);
    }

    @Test
    public void isDuringLastYearPositiveTest() {
        assertEquals(fileFilterUtils.isDuringLastYear(time.minusMonths(11).minusDays(29).minusHours(23)), true);
    }

    @Nested
    class SortingTests {

        @BeforeEach
        public void setUp() throws Exception {
            String dirPath = "src/test/resources/unlinkedFiles";

            Path path = Paths.get(dirPath);
            Files.createDirectory(path);
    
            Path firstPath = Paths.get(dirPath.concat("/firstFile.pdf"));
            Path secondPath = Paths.get(dirPath.concat("/secondFile.pdf"));
            Path thirdPath = Paths.get(dirPath.concat("/thirdFile.pdf"));
            Path fourthPath = Paths.get(dirPath.concat("/fourthFile.pdf"));
    
            File firstFile = new File(firstPath.toString());
            File secondFile = new File(secondPath.toString());
            File thirdFile = new File(thirdPath.toString());
            File fourthFile = new File(fourthPath.toString());
    
            firstFile.createNewFile();
            secondFile.createNewFile();
            thirdFile.createNewFile();
            fourthFile.createNewFile();
    
            ZoneId zoneId = ZoneId.systemDefault();
    
            LocalDateTime firstDate = LocalDateTime.of(2021, 1, 10, 0, 0, 0, 0);
            LocalDateTime secondDate = LocalDateTime.of(2021, 1, 5, 0, 0, 0, 0);
            LocalDateTime thirdDate = LocalDateTime.of(2021, 1, 1, 0, 0, 0, 0);
            LocalDateTime fourthDate = LocalDateTime.of(2021, 1, 2, 0, 0, 0, 0);
    
            firstFile.setLastModified(firstDate.atZone(zoneId).toEpochSecond());
            secondFile.setLastModified(secondDate.atZone(zoneId).toEpochSecond());
            thirdFile.setLastModified(thirdDate.atZone(zoneId).toEpochSecond());
            fourthFile.setLastModified(fourthDate.atZone(zoneId).toEpochSecond());
    
            files.add(firstPath);
            files.add(secondPath);
            files.add(thirdPath);
            files.add(fourthPath);
    
            expectedSortByDateAscending.add(thirdPath.toString());
            expectedSortByDateAscending.add(fourthPath.toString());
            expectedSortByDateAscending.add(secondPath.toString());
            expectedSortByDateAscending.add(firstPath.toString());
    
            expectedSortByDateDescending.add(firstPath.toString());
            expectedSortByDateDescending.add(secondPath.toString());
            expectedSortByDateDescending.add(fourthPath.toString());
            expectedSortByDateDescending.add(thirdPath.toString());
        }

        @AfterEach
        public void cleanUp() {
            String dirPath = "src/test/resources/unlinkedFiles";

            File firstFile = new File(dirPath.concat("/firstFile.pdf"));
            File secondFile = new File(dirPath.concat("/secondFile.pdf"));
            File thirdFile = new File(dirPath.concat("/thirdFile.pdf"));
            File fourthFile = new File(dirPath.concat("/fourthFile.pdf"));
            File directory = new File(dirPath);
    
            firstFile.delete();
            secondFile.delete();
            thirdFile.delete();
            fourthFile.delete();
            directory.delete();
        }

        @Test
        public void testSortByDateAscending() throws Exception {
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
        public void testSortByDateDescending() {
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
    }
}
