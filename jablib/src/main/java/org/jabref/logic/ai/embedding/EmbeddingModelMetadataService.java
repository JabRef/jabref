package org.jabref.logic.ai.embedding;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jabref.logic.ai.preferences.AiPreferences;
import org.jabref.logic.importer.FetcherException;
import org.jabref.logic.net.URLDownload;
import org.jabref.logic.util.strings.StringUtil;

import ai.djl.repository.Artifact;
import ai.djl.repository.MRL;
import ai.djl.repository.zoo.ModelLoader;
import ai.djl.repository.zoo.ModelZoo;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jspecify.annotations.NullMarked;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Service for discovering available AI embedding models and querying their download size
/// and maximum snippet (token / sequence) limit dynamically at runtime from DJL and Hugging Face.
// [impl->feat~ai.expert-settings.embedding-model-size~1]
// [impl->feat~ai.expert-settings.embedding-model-token-limit~1]
@NullMarked
public class EmbeddingModelMetadataService {
    public static final String DJL_PYTORCH_GROUP_ID = "ai.djl.huggingface.pytorch";

    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingModelMetadataService.class);

    private static final Set<String> CONFIG_AND_TOKENIZER_FILES = Set.of(
            "config.json",
            "tokenizer.json",
            "tokenizer_config.json",
            "vocab.txt",
            "special_tokens_map.json",
            "sentence_bert_config.json"
    );

    private final AiPreferences aiPreferences;
    private final Map<String, EmbeddingModelMetadata> metadataCache = new ConcurrentHashMap<>();

    public EmbeddingModelMetadataService(AiPreferences aiPreferences) {
        this.aiPreferences = aiPreferences;
    }

    public EmbeddingModelMetadataService() {
        this(AiPreferences.getDefault());
    }

    private boolean isAiEnabled() {
        return aiPreferences.getAiFeaturesEnabled() || aiPreferences.getAiFeaturesEnabledCurrently();
    }

    /// Returns the list of available embedding models discovered from the DJL HuggingFace Model Zoo.
    public List<String> getAvailableModels() {
        try {
            ModelZoo modelZoo = ModelZoo.getModelZoo(DJL_PYTORCH_GROUP_ID);
            if (modelZoo != null) {
                Collection<ModelLoader> loaders = modelZoo.getModelLoaders();
                if (loaders != null && !loaders.isEmpty()) {
                    List<String> models = loaders.stream()
                                                 .map(ModelLoader::getArtifactId)
                                                 .filter(StringUtil::isNotBlank)
                                                 .distinct()
                                                 .sorted(String.CASE_INSENSITIVE_ORDER)
                                                 .toList();
                    if (!models.isEmpty()) {
                        return models;
                    }
                }
            }
        } catch (RuntimeException e) {
            LOGGER.debug("Could not retrieve available embedding models from DJL ModelZoo", e);
        }

        return List.of();
    }

    /// Queries metadata (download size in bytes and maximum snippet length in tokens)
    /// for the given embedding model from DJL ModelZoo, falling back to the Hugging Face Hub REST API.
    public Optional<EmbeddingModelMetadata> getMetadata(String modelName) {
        if (!isAiEnabled() || StringUtil.isBlank(modelName)) {
            return Optional.empty();
        }

        EmbeddingModelMetadata cached = metadataCache.get(modelName);
        if (cached != null) {
            return Optional.of(cached);
        }

        OptionalLong downloadSize = OptionalLong.empty();
        OptionalInt maxTokens = OptionalInt.empty();

        // 1. Try resolving via DJL ModelZoo
        try {
            ModelZoo modelZoo = ModelZoo.getModelZoo(DJL_PYTORCH_GROUP_ID);
            if (modelZoo != null) {
                ModelLoader loader = modelZoo.getModelLoader(modelName);
                if (loader != null) {
                    MRL mrl = loader.getMrl();
                    if (mrl != null) {
                        Artifact artifact = mrl.getDefaultArtifact();
                        if (artifact != null) {
                            downloadSize = extractSizeFromArtifact(artifact);
                            maxTokens = extractMaxTokensFromArtifact(artifact);
                        }
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.debug("Failed resolving DJL artifact metadata for {}", modelName, e);
        }

        // 2. If size is not present, query Hugging Face Tree API
        if (downloadSize.isEmpty()) {
            downloadSize = fetchSizeFromHuggingFace(modelName);
        }

        // 3. If maxTokens is not present, query Hugging Face configs
        if (maxTokens.isEmpty()) {
            maxTokens = fetchMaxTokensFromHuggingFace(modelName);
        }

        EmbeddingModelMetadata metadata = new EmbeddingModelMetadata(modelName, downloadSize, maxTokens);
        if (downloadSize.isPresent() || maxTokens.isPresent()) {
            metadataCache.put(modelName, metadata);
        }
        return Optional.of(metadata);
    }

    private OptionalLong extractSizeFromArtifact(Artifact artifact) {
        if (artifact.getFiles() != null && !artifact.getFiles().isEmpty()) {
            long total = artifact.getFiles().values().stream()
                                 .mapToLong(Artifact.Item::getSize)
                                 .filter(s -> s > 0)
                                 .sum();
            if (total > 0) {
                return OptionalLong.of(total);
            }
        }
        return OptionalLong.empty();
    }

    private OptionalInt extractMaxTokensFromArtifact(Artifact artifact) {
        if (artifact.getArguments() != null && artifact.getArguments().containsKey("maxLength")) {
            Object val = artifact.getArguments().get("maxLength");
            if (val instanceof Number num) {
                return OptionalInt.of(num.intValue());
            } else if (val instanceof String str && !str.isBlank()) {
                try {
                    return OptionalInt.of(Integer.parseInt(str.trim()));
                } catch (NumberFormatException e) {
                    LOGGER.debug("Could not parse maxLength value '{}'", str, e);
                }
            }
        }
        return OptionalInt.empty();
    }

    private OptionalLong fetchSizeFromHuggingFace(String modelName) {
        String treeUrl = "https://huggingface.co/api/models/" + modelName + "/tree/main";
        try {
            URLDownload download = new URLDownload(new URI(treeUrl).toURL());
            String response = download.asString();
            JsonElement parsed = JsonParser.parseString(response);
            if (!parsed.isJsonArray()) {
                return OptionalLong.empty();
            }
            JsonArray filesArray = parsed.getAsJsonArray();

            long safetensorsBytes = 0;
            long binBytes = 0;
            long configBytes = 0;

            for (JsonElement element : filesArray) {
                if (element.isJsonObject()) {
                    JsonObject fileObj = element.getAsJsonObject();
                    String path = fileObj.has("path") && fileObj.get("path").isJsonPrimitive()
                            ? fileObj.get("path").getAsString()
                            : "";
                    long size = fileObj.has("size") && fileObj.get("size").isJsonPrimitive() && fileObj.get("size").getAsJsonPrimitive().isNumber()
                            ? fileObj.get("size").getAsLong()
                            : 0;

                    String lowerPath = path.toLowerCase(Locale.ROOT);
                    if (lowerPath.endsWith(".safetensors")) {
                        safetensorsBytes += size;
                    } else if (lowerPath.endsWith(".bin") && (lowerPath.contains("model") || lowerPath.contains("pytorch"))) {
                        binBytes += size;
                    } else if (isTokenizerOrConfigFile(path)) {
                        configBytes += size;
                    }
                }
            }

            if (safetensorsBytes > 0) {
                return OptionalLong.of(safetensorsBytes + configBytes);
            } else if (binBytes > 0) {
                return OptionalLong.of(binBytes + configBytes);
            }
        } catch (JsonParseException | NumberFormatException | FetcherException | IOException | URISyntaxException e) {
            LOGGER.debug("Could not fetch model size from Hugging Face tree API for {}", modelName, e);
        }
        return OptionalLong.empty();
    }

    private boolean isTokenizerOrConfigFile(String path) {
        return CONFIG_AND_TOKENIZER_FILES.contains(path.toLowerCase(Locale.ROOT));
    }

    private OptionalInt fetchMaxTokensFromHuggingFace(String modelName) {
        // Priority 1: sentence_bert_config.json (max_seq_length)
        String sbertUrl = "https://huggingface.co/" + modelName + "/raw/main/sentence_bert_config.json";
        OptionalInt sbertLimit = fetchIntFieldFromJson(sbertUrl, "max_seq_length");
        if (sbertLimit.orElse(0) > 0) {
            return sbertLimit;
        }

        // Priority 2: tokenizer_config.json (model_max_length)
        String tokenizerUrl = "https://huggingface.co/" + modelName + "/raw/main/tokenizer_config.json";
        OptionalInt tokLimit = fetchIntFieldFromJson(tokenizerUrl, "model_max_length");
        int tokLimitVal = tokLimit.orElse(0);
        if (tokLimitVal > 0 && tokLimitVal < 100_000) {
            return tokLimit;
        }

        // Priority 3: config.json (max_position_embeddings)
        String configUrl = "https://huggingface.co/" + modelName + "/raw/main/config.json";
        return fetchIntFieldFromJson(configUrl, "max_position_embeddings");
    }

    private OptionalInt fetchIntFieldFromJson(String url, String fieldName) {
        try {
            URLDownload download = new URLDownload(new URI(url).toURL());
            String response = download.asString();
            JsonElement parsed = JsonParser.parseString(response);
            if (!parsed.isJsonObject()) {
                return OptionalInt.empty();
            }
            JsonObject obj = parsed.getAsJsonObject();
            if (obj.has(fieldName) && obj.get(fieldName).isJsonPrimitive() && obj.get(fieldName).getAsJsonPrimitive().isNumber()) {
                return OptionalInt.of(obj.get(fieldName).getAsInt());
            }
        } catch (JsonParseException | NumberFormatException | FetcherException | IOException | URISyntaxException e) {
            LOGGER.debug("Could not fetch or parse {} from {}", fieldName, url, e);
        }
        return OptionalInt.empty();
    }
}
