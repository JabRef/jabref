package org.jabref.logic.openoffice.engine;

import org.jabref.logic.openoffice.ZoteroReferenceMark;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class ZoteroCitationModifiersTest {
    @ParameterizedTest
    @ValueSource(strings = {"{}", "[]", "null", "not JSON", "{\"citationItems\":null}",
            "{\"citationItems\":{}}", "{\"citationItems\":true}", "{\"citationItems\":[null]}",
            "{\"citationItems\":[42]}", "{\"citationItems\":[{}],\"prefix\":{}}",
            "{\"citationItems\":[{\"suppress-author\":null}]}", "{\"citationItems\":[{\"author-only\":[]}]}"})
    void malformedNativeModifiersAreCheckedFailures(String json) {
        assertThrows(RenderException.class, () -> ZoteroCitationModifiers.requireSupportedModifiers(ZoteroReferenceMark.PREFIX + json));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"citationItems\":[]}",
            "{\"citationItems\":[{},{}],\"unknown\":{\"locator\":\"opaque metadata\"}}",
            "{\"citationItems\":[{\"suppress-author\":true},{\"suppress-author\":true}]}",
            "{\"citationItems\":[{\"suppress-author\":false},{}]}",
            "{\"citationItems\":[{\"author-only\":false,\"locator\":\"\",\"label\":null,\"prefix\":\"\",\"suffix\":null}]}",
            "{\"citationItems\":[{\"itemData\":{\"locator\":\"opaque metadata\"}}],\"properties\":{\"formattedCitation\":\"external update\",\"custom\":42}}"})
    void supportedModifiersAndOpaqueMetadataAreAccepted(String json) {
        assertDoesNotThrow(() -> ZoteroCitationModifiers.requireSupportedModifiers(ZoteroReferenceMark.PREFIX + json));
    }
}
