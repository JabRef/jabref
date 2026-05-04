package org.jabref.logic.exporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jabref.logic.util.io.FileNameCleaner;
import org.jabref.logic.util.io.FileUtil;
import org.jabref.model.study.Study;
import org.jabref.model.study.StudyDatabase;
import org.jabref.model.study.StudyQuery;

import tools.jackson.databind.ObjectMapper;

/// Exports study search queries in the SearchRxiv / search-query JSON format.
/// One file is created per query and database combination,
/// as the format uses a single platform string per record.
///
/// The JSON format follows the search-query library's SearchFile specification.
///
/// @see <a href="https://github.com/CoLRev-Environment/search-query">search-query library</a>
/// @see <a href="https://colrev-environment.github.io/search-query/">search-query format documentation</a>
/// @see <a href="https://www.cabidigitallibrary.org/journal/searchrxiv">SearchRxiv</a>
public class SearchRxivExporter {

    public static final String SEARCHRXIV_URL = "https://www.cabidigitallibrary.org/journal/searchrxiv";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

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

    /// Builds a JSON string following the search-query format.
    /// Format spec: [search-query](https://github.com/CoLRev-Environment/search-query)
    private String buildJson(Study study, String query, String platform) throws IOException {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("search_string", query);
        data.put("platform", platform.toLowerCase(Locale.ROOT));
        data.put("authors", study.getAuthors().stream()
                                 .map(author -> Map.of("name", author))
                                 .toList());
        // Placeholder fields — to be filled by the user on SearchRxiv
        data.put("record_info", Map.of());
        data.put("date", Map.of());
        data.put("database", List.of());
        return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(data);
    }

    private String buildFileName(String query, String databaseName, int index) {
        String queryPart = query.isBlank() ? "query" : query;
        String name = FileNameCleaner.cleanFileName(databaseName) + "-" + FileNameCleaner.cleanFileName(queryPart) + "-" + index + ".json";
        return FileUtil.getValidFileName(name);
    }
}
