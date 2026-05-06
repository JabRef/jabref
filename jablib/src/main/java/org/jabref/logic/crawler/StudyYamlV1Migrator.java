package org.jabref.logic.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jabref.model.study.Study;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

public class StudyYamlV1Migrator {

    public static String migrate(String v1YamlContent) throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        LinkedHashMap<String, Object> v1Mapping = mapper.readValue(v1YamlContent, new TypeReference<>() {
        });

        LinkedHashMap<String, Object> v2Mapping = new LinkedHashMap<>();
        v2Mapping.put("version", Study.CURRENT_SCHEMA_VERSION);
        v2Mapping.putAll(v1Mapping);

        if (v2Mapping.containsKey("databases")) {
            Object databasesValue = v2Mapping.remove("databases");
            if (databasesValue instanceof List<?> databasesList) {
                List<Map<String, Object>> catalogs = new ArrayList<>();
                for (Object entry : databasesList) {
                    if (entry instanceof Map<?, ?> entryMap) {
                        LinkedHashMap<String, Object> catalogEntry = new LinkedHashMap<>();
                        for (Map.Entry<?, ?> mapEntry : entryMap.entrySet()) {
                            catalogEntry.put((String) mapEntry.getKey(), mapEntry.getValue());
                        }
                        if (!catalogEntry.containsKey("reason")) {
                            catalogEntry.put("reason", "");
                        }
                        catalogs.add(catalogEntry);
                    }
                }
                v2Mapping.put("catalogs", catalogs);
            }
        }

        YAMLMapper writeMapper = new YAMLMapper(YAMLFactory.builder()
                                                           .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                                           .enable(YAMLWriteFeature.MINIMIZE_QUOTES).build());
        return writeMapper.writeValueAsString(v2Mapping);
    }
}

