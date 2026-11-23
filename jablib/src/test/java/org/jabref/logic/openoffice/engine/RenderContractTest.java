package org.jabref.logic.openoffice.engine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.jabref.logic.openoffice.style.BstCitationFormat;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.openoffice.ootext.OOText;
import org.jabref.model.openoffice.style.Citation;
import org.jabref.model.openoffice.style.CitationGroup;
import org.jabref.model.openoffice.style.CitationGroupId;
import org.jabref.model.openoffice.style.CitationType;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@NullMarked
class RenderContractTest {

    @Test
    void capabilitiesCopySupportedTypes() {
        Set<CitationType> types = EnumSet.of(CitationType.AUTHORYEAR_PAR);
        StyleCapabilities capabilities = new StyleCapabilities(types, true, false, false);
        types.add(CitationType.AUTHOR_ONLY);

        assertEquals(Set.of(CitationType.AUTHORYEAR_PAR), capabilities.supportedTypes());
    }

    @Test
    void capabilitiesExposeImmutableSupportedTypes() {
        StyleCapabilities capabilities = new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, false, false);

        assertThrows(UnsupportedOperationException.class, () -> capabilities.supportedTypes().add(CitationType.YEAR_ONLY));
    }

    @Test
    void requestCopiesContainersButPreservesExistingGroupInstances() {
        CitationGroup group = group();
        BibDatabase database = new BibDatabase();
        List<CitationGroup> groups = new ArrayList<>(List.of(group));
        List<BibDatabase> databases = new ArrayList<>(List.of(database));
        RenderRequest request = new RenderRequest(groups, databases, new RenderOptions(BstCitationFormat.NUMERIC));
        groups.clear();
        databases.clear();

        assertEquals(List.of(List.of(group), List.of(database)), List.of(request.groupsInDocumentOrder(), request.databases()));
    }

    @Test
    void requestExposesImmutableGroups() {
        RenderRequest request = new RenderRequest(List.of(group()), List.of(), new RenderOptions(BstCitationFormat.AUTHOR_YEAR));

        assertThrows(UnsupportedOperationException.class, () -> request.groupsInDocumentOrder().clear());
    }

    @Test
    void requestExposesImmutableDatabases() {
        RenderRequest request = new RenderRequest(List.of(), List.of(new BibDatabase()), new RenderOptions(BstCitationFormat.NUMERIC));

        assertThrows(UnsupportedOperationException.class, () -> request.databases().clear());
    }

    @Test
    void requestRetainsPerCitationPageInfoAndStoredType() {
        CitationGroup group = group();
        group.citationsInStorageOrder.getFirst().setPageInfo(Optional.of(OOText.fromString("p. 5")));
        RenderRequest request = new RenderRequest(List.of(group), List.of(), new RenderOptions(BstCitationFormat.STYLE_DEFINED));

        assertEquals(List.of(CitationType.AUTHOR_ONLY, Optional.of(OOText.fromString("p. 5"))),
                List.of(request.groupsInDocumentOrder().getFirst().citationType,
                        request.groupsInDocumentOrder().getFirst().citationsInStorageOrder.getFirst().getPageInfo()));
    }

    @Test
    void resultCopiesFormattedTextContainersWithoutFlatteningMarkup() {
        CitationGroupId groupId = new CitationGroupId("uid");
        Map<CitationGroupId, OOText> markers = new HashMap<>(Map.of(groupId, OOText.fromString("<sup>1</sup>")));
        List<OOText> bibliography = new ArrayList<>(List.of(OOText.fromString("<i>Title</i>")));
        List<String> unresolvedKeys = new ArrayList<>(List.of("missing"));
        RenderResult result = new RenderResult(markers, bibliography, unresolvedKeys);
        markers.clear();
        bibliography.clear();
        unresolvedKeys.clear();

        assertEquals(new RenderResult(Map.of(groupId, OOText.fromString("<sup>1</sup>")), List.of(OOText.fromString("<i>Title</i>")), List.of("missing")), result);
    }

    @Test
    void resultUsesOriginalGroupIdInstances() {
        CitationGroupId groupId = new CitationGroupId("uid");
        RenderResult result = new RenderResult(Map.of(groupId, OOText.fromString("text")), List.of(), List.of());

        assertSame(groupId, result.markers().keySet().iterator().next());
    }

    @Test
    void resultExposesImmutableMarkers() {
        RenderResult result = new RenderResult(Map.of(), List.of(), List.of());

        assertThrows(UnsupportedOperationException.class, () -> result.markers().put(new CitationGroupId("uid"), OOText.fromString("text")));
    }

    @Test
    void resultExposesImmutableBibliography() {
        RenderResult result = new RenderResult(Map.of(), List.of(OOText.fromString("text")), List.of());

        assertThrows(UnsupportedOperationException.class, () -> result.bibliography().clear());
    }

    @Test
    void resultExposesImmutableUnresolvedKeys() {
        RenderResult result = new RenderResult(Map.of(), List.of(), List.of("missing"));

        assertThrows(UnsupportedOperationException.class, () -> result.unresolvedKeys().clear());
    }

    @Test
    void checkedRenderFailurePreservesConversionCause() {
        IOException cause = new IOException("Conversion failed");
        CitationRenderer renderer = new CitationRenderer() {
            @Override
            public StyleCapabilities capabilities() {
                return new StyleCapabilities(Set.of(CitationType.AUTHORYEAR_PAR), true, false, false);
            }

            @Override
            public RenderResult render(RenderRequest request) throws RenderException {
                throw new RenderException("Cannot render bibliography", cause);
            }
        };

        RenderException error = assertThrows(RenderException.class,
                () -> renderer.render(new RenderRequest(List.of(), List.of(), new RenderOptions(BstCitationFormat.NUMERIC))));

        assertSame(cause, error.getCause());
    }

    private static CitationGroup group() {
        return new CitationGroup(new CitationGroupId("uid"), CitationType.AUTHOR_ONLY,
                List.of(new Citation("key")), Optional.empty());
    }
}
