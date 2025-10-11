package org.jabref.logic.util;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.shared.exception.MscCodeLoadingException;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.HashBiMap;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MscCodeUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(MscCodeUtils.class);

    /**
     * Load MSC codes and descriptions from a JSON resource URL into a HashMap
     *
     * @param resourceUrl URL to the JSON resource containing MSC codes
     * @return Map with MSC codes as keys and descriptions as values
     * @throws MscCodeLoadingException If there is an issue loading or parsing the JSON
     */
    @NonNull
    public static Optional<HashBiMap<String, String>> loadMscCodesFromJson(URL resourceUrl) throws MscCodeLoadingException {

        ObjectMapper mapper = new ObjectMapper();
        try {
            Map<String, String> mapping =
                    mapper.readValue(resourceUrl, new TypeReference<>() {
                    });
            HashBiMap<String, String> result = HashBiMap.create(mapping);

            if (result.isEmpty()) {
                return Optional.empty();
            }

            return Optional.of(result);
        } catch (JsonParseException | JsonMappingException e) {
            LOGGER.error("Error parsing MSC codes from JSON", e);
            throw new MscCodeLoadingException("Failed to parse MSC codes from JSON", e);
        } catch (IOException e) {
            LOGGER.error("Error loading MSC codes from JSON URL", e);
            throw new MscCodeLoadingException("Failed to load MSC codes from JSON URL", e);
        }
    }
}

