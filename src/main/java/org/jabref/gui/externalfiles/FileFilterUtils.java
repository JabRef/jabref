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
    
    public static LocalDateTime getFileTime(Path path) {
        FileTime lastEditedTime = null;
        try {
            lastEditedTime = Files.getLastModifiedTime(path);
        } catch (IOException e) {
            LOGGER.error("Could not retrieve file time", e);
        }
        LocalDateTime localDateTime = lastEditedTime
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        return localDateTime;
    }

    public boolean isDuringLastDay(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusHours(24));
    }

    public boolean isDuringLastWeek(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusDays(7));
    }

    public boolean isDuringLastMonth(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusMonths(1));
    }

    public boolean isDuringLastYear(LocalDateTime fileEditTime) {
        LocalDateTime NOW = LocalDateTime.now(ZoneId.systemDefault());
        return fileEditTime.isAfter(NOW.minusYears(1));
    }

    public static boolean filterByDate(Path path, String filter) {
        FileFilterUtils fileFilter = new FileFilterUtils();
        LocalDateTime fileTime = FileFilterUtils.getFileTime(path);
        switch (filter) {
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
                .sorted(Comparator.comparingLong(x -> -FileFilterUtils.getFileTime(x)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli()))
                .collect(Collectors.toList());
    }

    public static List<Path> sortByDate(List<Path> files, String sortType) {
        FileFilterUtils fileFilter = new FileFilterUtils();
        switch (sortType) {
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

