package org.jabref.logic.crawler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

public class StudyYamlV1Migrator {

    public static String migrate(String v1YamlContent) throws IOException {
        var mapper = new ObjectMapper(new YAMLFactory());
        LinkedHashMap<String, Object> v1Map = mapper.readValue(v1YamlContent, new TypeReference<>() {
        });

        var v2Map = new LinkedHashMap<String, Object>();
        v2Map.put("version", "2.0");
        v2Map.putAll(v1Map);

        if (v2Map.containsKey("databases")) {
            var databasesValue = v2Map.remove("databases");
            if (databasesValue instanceof List<?> databasesList) {
                var catalogs = new ArrayList<Map<String, Object>>();
                for (var entry : databasesList) {
                    if (entry instanceof Map<?, ?> entryMap) {
                        var catalogEntry = new LinkedHashMap<String, Object>();
                        for (var e : entryMap.entrySet()) {
                            catalogEntry.put((String) e.getKey(), e.getValue());
                        }
                        if (!catalogEntry.containsKey("reason")) {
                            catalogEntry.put("reason", "");
                        }
                        catalogs.add(catalogEntry);
                    }
                }
                v2Map.put("catalogs", catalogs);
            }
        }

        var writeMapper = new YAMLMapper(YAMLFactory.builder()
                                                    .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                                                    .enable(YAMLWriteFeature.MINIMIZE_QUOTES).build());
        return writeMapper.writeValueAsString(v2Map);
    }
}

