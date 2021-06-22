package org.jabref.gui.externalfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;
import java.util.stream.Collectors;

public class FileFilterUtils {

    public static LocalDateTime getFileTime(Path path) {
        FileTime lastEditedTime = null;
        try {
            lastEditedTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            System.err.println("Could not retrieve file time");
        }
        LocalDateTime localDateTime = lastEditedTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return localDateTime;
    }

    public boolean isDuringLastDay(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        if (fileEditTime.isAfter(NOW.minusHours(24))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDuringLastWeek(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        if (fileEditTime.isAfter(NOW.minusDays(7))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDuringLastMonth(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        if (fileEditTime.isAfter(NOW.minusDays(30))) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isDuringLastYear(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        if (fileEditTime.isAfter(NOW.minusYears(1))) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean filterByDate(Path path, String filter) {
        FileFilterUtils fileFilter = new FileFilterUtils();
        LocalDateTime fileTime = FileFilterUtils.getFileTime(path);
        switch(filter) {
            case "Last day":
                return fileFilter.isDuringLastDay(fileTime);
            case "Last week":
                return fileFilter.isDuringLastWeek(fileTime);
            case "Last month":
                return fileFilter.isDuringLastMonth(fileTime);
            case "Last year":
                return fileFilter.isDuringLastYear(fileTime);
            case "All time":
                return true;
            default:
                return true;

        }
    }

    public List<Path> sortByDateAscending(List<Path> files) {
        return files.stream()
                .sorted(Comparator.comparingLong(x -> FileFilterUtils.getFileTime(x)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()))
                .collect(Collectors.toList());
    }

    public List<Path> sortByDateDescending(List<Path> files) {
        return files.stream()
                .sorted(Comparator.comparingLong(x -> - FileFilterUtils.getFileTime(x)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()))
                .collect(Collectors.toList());
    }

    public static List<Path> sortByDate(List<Path> files, String sortType) {
        FileFilterUtils fileFilter = new FileFilterUtils();
        switch(sortType) {
            case "Default":
                return files;
            case "Newest first":
                return fileFilter.sortByDateDescending(files);
            case "Oldest first":
                return fileFilter.sortByDateAscending(files);
            default:
                return files;
        }
    }
}
