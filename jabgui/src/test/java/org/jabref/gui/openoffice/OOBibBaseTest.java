package org.jabref.gui.openoffice;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.gui.DialogService;
import org.jabref.logic.citationstyle.CitationStyle;
import org.jabref.logic.openoffice.ZoteroDocumentPreferences;
import org.jabref.logic.openoffice.oocsltext.CSLCitationOOAdapter;
import org.jabref.logic.openoffice.oocsltext.CSLUpdateBibliography;
import org.jabref.model.database.BibDatabase;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.openoffice.rangesort.FunctionalTextViewCursor;
import org.jabref.model.openoffice.uno.NoDocumentException;
import org.jabref.model.openoffice.util.OOResult;
import org.jabref.model.openoffice.util.OOVoidResult;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyShort;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class OOBibBaseTest {
    private static final String IEEE_STYLE_ID = "http://www.zotero.org/styles/ieee";

    @Test
    void writeDocumentCslStylePersistsStyleIdInDocumentPreferences() throws Exception {
        TestDocument document = new TestDocument();
        CitationStyle citationStyle = citationStyle("ieee.csl", IEEE_STYLE_ID, "IEEE");

        OOVoidResult<OOError> result = OOBibBase.writeDocumentCslStyle(document.getTextDocument(), citationStyle);

        assertTrue(result.isOK());
        assertEquals(Optional.of(citationStyle), ZoteroDocumentPreferences.findCitationStyle(document.getTextDocument(), List.of(citationStyle)));
    }

    @Test
    void updateCSLBibliographyDoesNotLockControllersWhenNoEntriesAreCited() throws Exception {
        DialogService dialogService = mock(DialogService.class);
        XTextDocument doc = mock(XTextDocument.class);
        FunctionalTextViewCursor fcursor = mock(FunctionalTextViewCursor.class);
        CSLCitationOOAdapter cslCitationOOAdapter = mock(CSLCitationOOAdapter.class);
        CSLUpdateBibliography cslUpdateBibliography = mock(CSLUpdateBibliography.class);
        CitationStyle citationStyle = mock(CitationStyle.class);

        doNothing().when(fcursor).restore(doc);

        OOVoidResult<OOError> result = OOBibBase.updateCSLBibliography(
                dialogService,
                List.of(new BibDatabase()),
                citationStyle,
                doc,
                OOResult.ok(fcursor),
                "Error title",
                cslCitationOOAdapter,
                cslUpdateBibliography);

        assertTrue(result.isOK());
        verify(cslCitationOOAdapter).refreshCitationState();
        verify(doc, never()).lockControllers();
        verify(doc, never()).unlockControllers();
        verify(cslUpdateBibliography, never()).rebuildCSLBibliography(any(), any(), any(), any(), any());
        verify(fcursor).restore(doc);
    }

    @Test
    void updateCSLBibliographyReturnsErrorWhenRebuildThrowsNoDocumentException() throws Exception {
        DialogService dialogService = mock(DialogService.class);
        XTextDocument doc = mock(XTextDocument.class);
        FunctionalTextViewCursor fcursor = mock(FunctionalTextViewCursor.class);
        CSLCitationOOAdapter cslCitationOOAdapter = mock(CSLCitationOOAdapter.class);
        CSLUpdateBibliography cslUpdateBibliography = mock(CSLUpdateBibliography.class);
        CitationStyle citationStyle = mock(CitationStyle.class);
        BibEntry bibEntry = new BibEntry();

        doNothing().when(fcursor).restore(doc);
        when(cslCitationOOAdapter.isCitedEntry(bibEntry)).thenReturn(true);
        doThrow(new NoDocumentException()).when(cslUpdateBibliography).rebuildCSLBibliography(
                eq(doc),
                eq(cslCitationOOAdapter),
                any(),
                any(),
                eq(citationStyle));

        OOVoidResult<OOError> result = OOBibBase.updateCSLBibliography(
                dialogService,
                List.of(new BibDatabase(List.of(bibEntry))),
                citationStyle,
                doc,
                OOResult.ok(fcursor),
                "Error title",
                cslCitationOOAdapter,
                cslUpdateBibliography);

        assertTrue(result.isError());
        verify(cslCitationOOAdapter).refreshCitationState();
        assertEquals("Error title", result.getError().getTitle());
        verify(doc).lockControllers();
        verify(doc).unlockControllers();
        verify(fcursor).restore(doc);
    }

    private static CitationStyle citationStyle(String filePath, String styleId, String title) {
        return new CitationStyle(filePath, styleId, "in-text", title, title, false, true, false, false, title, true);
    }

    @NullMarked
    private static final class TestDocument {

        private final Map<String, String> properties = new LinkedHashMap<>();
        private final XTextDocument textDocument = mock(
                XTextDocument.class,
                withSettings().extraInterfaces(XDocumentPropertiesSupplier.class));
        private final XDocumentProperties documentProperties = mock(XDocumentProperties.class);
        private final XPropertyContainer userDefinedProperties = mock(
                XPropertyContainer.class,
                withSettings().extraInterfaces(XPropertySet.class));
        private final XPropertySet propertySet = (XPropertySet) userDefinedProperties;
        private final XPropertySetInfo propertySetInfo = mock(XPropertySetInfo.class);

        private TestDocument() throws Exception {
            XDocumentPropertiesSupplier documentPropertiesSupplier = (XDocumentPropertiesSupplier) textDocument;
            when(documentPropertiesSupplier.getDocumentProperties()).thenReturn(documentProperties);
            when(documentProperties.getUserDefinedProperties()).thenReturn(userDefinedProperties);
            when(propertySet.getPropertySetInfo()).thenReturn(propertySetInfo);
            when(propertySetInfo.hasPropertyByName(anyString())).thenAnswer(invocation -> {
                String propertyName = invocation.getArgument(0);
                return properties.containsKey(propertyName);
            });
            when(propertySetInfo.getProperties()).thenAnswer(_ -> getProperties());
            when(propertySet.getPropertyValue(anyString())).thenAnswer(invocation -> {
                String propertyName = invocation.getArgument(0);
                if (!properties.containsKey(propertyName)) {
                    throw new UnknownPropertyException(propertyName);
                }
                return properties.get(propertyName);
            });
            doAnswer(invocation -> {
                String propertyName = invocation.getArgument(0);
                Object value = invocation.getArgument(1);
                if (!properties.containsKey(propertyName)) {
                    throw new UnknownPropertyException(propertyName);
                }
                properties.put(propertyName, value.toString());
                return null;
            }).when(propertySet).setPropertyValue(anyString(), any());
            doAnswer(invocation -> {
                String propertyName = invocation.getArgument(0);
                Object value = invocation.getArgument(2);
                if (properties.containsKey(propertyName)) {
                    throw new PropertyExistException(propertyName);
                }
                properties.put(propertyName, getStringValue(value));
                return null;
            }).when(userDefinedProperties).addProperty(anyString(), anyShort(), any());
            doAnswer(invocation -> {
                String propertyName = invocation.getArgument(0);
                if (properties.remove(propertyName) == null) {
                    throw new UnknownPropertyException(propertyName);
                }
                return null;
            }).when(userDefinedProperties).removeProperty(anyString());
        }

        private XTextDocument getTextDocument() {
            return textDocument;
        }

        private Property[] getProperties() {
            return properties.keySet().stream()
                             .map(propertyName -> new Property(propertyName, 0, Type.STRING, PropertyAttribute.REMOVEABLE))
                             .toArray(Property[]::new);
        }

        private String getStringValue(Object value) {
            if (value instanceof Any anyValue) {
                return anyValue.getObject().toString();
            }
            return value.toString();
        }
    }
}
