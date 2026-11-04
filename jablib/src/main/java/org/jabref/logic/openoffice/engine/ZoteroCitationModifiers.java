package org.jabref.logic.openoffice.engine;

import java.util.List;
import java.util.Optional;

import org.jabref.logic.openoffice.ZoteroReferenceMark;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jspecify.annotations.NullMarked;

@NullMarked
final class ZoteroCitationModifiers {
    private ZoteroCitationModifiers() {
    }

    // [impl->req~openoffice.zotero-modifier-safe-update~1]
    static void requireSupportedModifiers(String name) throws RenderException {
        try {
            JsonObject citation = JsonParser.parseString(ZoteroReferenceMark.getCSLJson(name)).getAsJsonObject();
            rejectTextModifiers(citation);
            if (citation.has("properties") && citation.get("properties").isJsonObject()) {
                rejectTextModifiers(citation.getAsJsonObject("properties"));
            }
            if (!citation.has("citationItems") || !citation.get("citationItems").isJsonArray()) {
                throw new RenderException("Native Zotero citationItems must be an array");
            }
            Optional<Boolean> suppression = Optional.empty();
            for (JsonElement element : citation.getAsJsonArray("citationItems")) {
                JsonObject item = element.getAsJsonObject();
                rejectTextModifiers(item);
                if (item.has("author-only") && item.get("author-only").getAsBoolean()) {
                    throw unsupportedModifiers();
                }
                boolean suppress = item.has("suppress-author") && item.get("suppress-author").getAsBoolean();
                if (suppression.isPresent() && suppression.orElseThrow() != suppress) {
                    throw unsupportedModifiers();
                }
                suppression = Optional.of(suppress);
            }
        } catch (JsonParseException | IllegalStateException | UnsupportedOperationException exception) {
            throw new RenderException("Invalid native Zotero citation modifiers", exception);
        }
    }

    private static void rejectTextModifiers(JsonObject json) throws RenderException {
        for (String field : List.of("locator", "label", "prefix", "suffix")) {
            if (json.has(field) && !json.get(field).isJsonNull() && !json.get(field).getAsString().isEmpty()) {
                throw unsupportedModifiers();
            }
        }
    }

    private static UnsupportedModifiersException unsupportedModifiers() {
        return new UnsupportedModifiersException();
    }

    @NullMarked
    static final class UnsupportedModifiersException extends RenderException {
        private UnsupportedModifiersException() {
            super("Unsupported native Zotero citation modifiers");
        }
    }
}
