package org.jabref.gui.fieldeditors.msccodes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MscCodeUtils {
    
    /**
     * Load MSC codes and descriptions from a JSON file into a HashMap
     * 
     * @param jsonFilePath Path to the JSON file containing MSC codes
     * @return Map with MSC codes as keys and descriptions as values
     */
    public static Map<String, String> loadMscCodesFromJson(String jsonFilePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(new File(jsonFilePath), new TypeReference<Map<String, String>>() {});
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }
    
}