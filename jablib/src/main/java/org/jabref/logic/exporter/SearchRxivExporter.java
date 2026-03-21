package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import tools.jackson.databind.ObjectMapper;

/// Exports study search queries in the SearchRxiv / search-query JSON format.
/// One file is created per query and database combination.
///
/// @see <a href="https://github.com/CoLRev-Environment/search-query">search-query library</a>
/// @see <a href="https://www.cabidigitallibrary.org/journal/searchrxiv">SearchRxiv</a>
public class SearchRxivExporter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /// Exports the study's search queries as JSON files to the given directory.
    /// One file per query and database combination is created.
    ///
    /// @param study     The study containing queries, databases, and authors
    /// @param directory The target directory to write the JSON files into
    /// @throws IOException if a file cannot be written
    public void export(Study study, Path directory) throws IOException {
        int index = 0;
        for (StudyQuery studyQuery : study.getQueries()) {
            for (StudyDatabase database : study.getDatabases()) {
                Path file = directory.resolve(buildFileName(studyQuery.getQuery(), database.getName(), index));
                Files.writeString(file, buildJson(study, studyQuery.getQuery(), database.getName()));
                index++;
            }
        }
    }

    private String buildJson(Study study, String query, String platform) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("search_string", query);
        data.put("platform", platform.toLowerCase());
        data.put("authors", study.getAuthors().stream()
                                 .map(author -> Map.of("name", author))
                                 .toList());
        data.put("record_info", Map.of());
        data.put("date", Map.of());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    private String buildFileName(String query, String databaseName, int index) {
        String cleanDb = databaseName.replaceAll("[^A-Za-z0-9]", "_");
        String cleanQuery = query.replaceAll("[^A-Za-z0-9]", "_");
        if (cleanQuery.isEmpty()) {
            cleanQuery = "query";
        }
        if (cleanQuery.length() > 20) {
            cleanQuery = cleanQuery.substring(0, 20);
        }
        return cleanDb + "-" + cleanQuery + "-" + index + ".json";
    }
}
