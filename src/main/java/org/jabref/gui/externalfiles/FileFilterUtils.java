package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileFilterUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileFilterUtils.class);
    
    /* Returns the last edited time of a file as LocalDateTime. */
    public static LocalDateTime getFileTime(Path path) {
        FileTime lastEditedTime = null;
        try {
            lastEditedTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            LOGGER.error("Could not retrieve file time", e);
            return LocalDateTime.now();
        }
        LocalDateTime localDateTime = lastEditedTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return localDateTime;
    }

    /* Returns true if a file with a specific path 
     * was edited during the last 24 hours. */
    public boolean isDuringLastDay(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusHours(24));
    }

    /* Returns true if a file with a specific path 
     * was edited during the last 7 days. */
    public boolean isDuringLastWeek(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusDays(7));
    }

    /* Returns true if a file with a specific path 
     * was edited during the last 30 days. */
    public boolean isDuringLastMonth(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusDays(30));
    }

    /* Returns true if a file with a specific path 
     * was edited during the last 365 days. */
    public boolean isDuringLastYear(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusDays(365));
    }

    /* Returns true if a file is edited in the time margin specified by the given filter. */
    public static boolean filterByDate(Path path, DateRange filter) {
        FileFilterUtils fileFilter = new FileFilterUtils();
        LocalDateTime fileTime = FileFilterUtils.getFileTime(path);
        boolean isInDateRange = switch (filter) {
            case DAY -> fileFilter.isDuringLastDay(fileTime);
            case WEEK -> fileFilter.isDuringLastWeek(fileTime);
            case MONTH -> fileFilter.isDuringLastMonth(fileTime);
            case YEAR -> fileFilter.isDuringLastYear(fileTime);
            case ALL_TIME -> true;
        };
        return isInDateRange;
    }

    /* Sorts a list of Path objects according to the last edited date 
     * of their corresponding files, from newest to oldest. */
    public List<Path> sortByDateAscending(List<Path> files) {
        return files.stream()
                .sorted(Comparator.comparingLong(file -> FileFilterUtils.getFileTime(file)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()))
                .collect(Collectors.toList());
    }

    /* Sorts a list of Path objects according to the last edited date 
     * of their corresponding files, from oldest to newest. */
    public List<Path> sortByDateDescending(List<Path> files) {
        return files.stream()
                .sorted(Comparator.comparingLong(file -> -FileFilterUtils.getFileTime(file)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()))
                .collect(Collectors.toList());
    }

    /* Sorts a list of Path objects according to the last edited date
     * the order depends on the specified sorter type. */
    public static List<Path> sortByDate(List<Path> files, ExternalFileSorter sortType) {
        FileFilterUtils fileFilter = new FileFilterUtils();
        List<Path> sortedFiles = switch (sortType) {
            case DEFAULT -> files;
            case DATE_ASCENDING -> fileFilter.sortByDateDescending(files);
            case DATE_DESCENDING -> fileFilter.sortByDateAscending(files);
        };
        return sortedFiles;
    }
}

