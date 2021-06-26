package org.jabref.gui.externalfiles;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.io.IOException;
import java.text.ParseException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileFilterUtilsTest {

    private final FileFilterUtils fileFilterUtils = new FileFilterUtils();
    private final LocalDateTime time = LocalDateTime.now();

    private List<Path> files = new ArrayList<Path>();
    private List<String> expectedSortByDateAscending = new ArrayList<Path>();
    private List<String> expectedSortByDateDescending = new ArrayList<Path>();

    @BeforeAll
    private static void setUp() {
        FileFilterUtilsTest test = new FileFilterUtilsTest();
        try{
            String dirPath = "src/test/resources/unlinkedFiles";
            Path path = Paths.get(dirPath);
            Files.createDirectory(path);

            Path firstPath = Paths.get(dirPath.concat("/firstFile.pdf"));
            Path secondPath = Paths.get(dirPath.concat("/secondFile.pdf"));
            Path thirdPath = Paths.get(dirPath.concat("/thirdFile.pdf"));
            Path fourthPath = Paths.get(dirPath.concat("/fourthFile.pdf"));

            File firstFile = new File(firstPath);
            File secondFile = new File(secondPath);
            File thirdFile = new File(thirdPath);
            File fourthFile = new File(fourthPath);

            firstFile.createNewFile();
            secondFile.createNewFile();
            thirdFile.createNewFile();
            fourthFile.createNewFile();

            SimpleDateFormat dateFormat = new SimpleDateFormat("mm/dd/yyyy");

            Date firstDate = dateFormat.parse("10/1/2021");
            Date secondDate = dateFormat.parse("5/1/2021");
            Date thirdDate = dateFormat.parse("1/1/2021");
            Date fourthDate = dateFormat.parse("2/1/2021");

            firstFile.setLastModified(firstDate.getTime());
            secondFile.setLastModified(secondDate.getTime());
            thirdFile.setLastModified(thirdDate.getTime());
            fourthFile.setLastModified(fourthDate.getTime());

            test.files.add(firstPath);
            test.files.add(secondPath);
            test.files.add(thirdPath);
            test.files.add(fourthPath);

            test.expectedSortByDateAscending.add(firstPath.toString());
            test.expectedSortByDateAscending.add(secondPath.toString());
            test.expectedSortByDateAscending.add(thirdPath.toString());
            test.expectedSortByDateAscending.add(fourthPath.toString());

            test.expectedSortByDateDescending.add(Paths.concat("/firstFile.pdf")));
            test.expectedSortByDateDescending.add(Paths.concat("/secondFile.pdf")));
            test.expectedSortByDateDescending.add(Paths.concat("/thirdFile.pdf")));
            test.expectedSortByDateDescending.add(Paths.concat("/fourthFile.pdf")));
        } catch (Exception e) {}
    }

    @AfterAll
    private static void cleanUp() {
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
    public void testSortByDateAscending() {
        ArrayList<String> actuals = new ArrayList<String>();
        ArrayList<Path> sortedPaths = fileFilterUtils.sortByDateAscending(files);
        for (Path path : sortedPaths) {
            actuals.add(path.)
        }
        assertThat(files, is(expectedSortByDateAscending));
    }

    @Test
    public void testSortByDateDescending() {
        assertThat(fileFilterUtils.sortByDateDescending(new ArrayList<Path>(files)), is(expectedSortByDateDescending));
    }

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
}
