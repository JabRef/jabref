package org.jabref.logic.openoffice;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jabref.logic.citationstyle.CitationStyle;

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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@NullMarked
class ZoteroDocumentPreferencesTest {
    private static final String APA_STYLE_ID = "http://www.zotero.org/styles/apa";
    private static final String IEEE_STYLE_ID = "http://www.zotero.org/styles/ieee";

    @Test
    void findCitationStyleReadsStyleIdFromZoteroPreferenceChunks() throws Exception {
        TestDocument document = new TestDocument();
        String zoteroPreference = """
                <data data-version="3" zotero-version="7.0">\
                <session id="session-id"/>\
                <style id="%s" hasBibliography="1" bibliographyStyleHasBeenSet="0"/>\
                <prefs><pref name="fieldType" value="ReferenceMark"/></prefs>\
                </data>""".formatted(APA_STYLE_ID);
        document.putZoteroPreferenceChunks(zoteroPreference.substring(0, 80), zoteroPreference.substring(80));

        CitationStyle ieeeStyle = citationStyle("ieee.csl", IEEE_STYLE_ID, "IEEE");
        CitationStyle apaStyle = citationStyle("apa.csl", APA_STYLE_ID, "American Psychological Association 7th edition");

        Optional<CitationStyle> citationStyle = ZoteroDocumentPreferences.findCitationStyle(
                document.getTextDocument(),
                List.of(ieeeStyle, apaStyle));

        assertEquals(Optional.of(apaStyle), citationStyle);
    }

    @Test
    void writeCitationStyleUpdatesOnlyTheStyleIdInExistingZoteroPreference() throws Exception {
        TestDocument document = new TestDocument();
        String zoteroPreference = """
                <data data-version="3" zotero-version="7.0">\
                <session id="session-id"/>\
                <style id="%s" hasBibliography="1" bibliographyStyleHasBeenSet="1"/>\
                <prefs>\
                <pref name="fieldType" value="ReferenceMark"/>\
                <pref name="noteType" value="0"/>\
                </prefs>\
                </data>""".formatted(APA_STYLE_ID);
        document.putZoteroPreferenceChunks(zoteroPreference);

        boolean written = ZoteroDocumentPreferences.writeCitationStyle(
                document.getTextDocument(),
                citationStyle("ieee.csl", IEEE_STYLE_ID, "IEEE"));

        String updatedPreference = document.getZoteroPreference();
        assertTrue(written);
        assertEquals(Optional.of(IEEE_STYLE_ID), ZoteroDocumentPreferences.findStyleId(updatedPreference));
        assertTrue(updatedPreference.contains("session-id"));
        assertTrue(updatedPreference.contains("fieldType"));
        assertTrue(updatedPreference.contains("noteType"));
    }

    @Test
    void writeCitationStyleCreatesMinimalZoteroPreferenceWhenDocumentHasNoZoteroPreference() throws Exception {
        TestDocument document = new TestDocument();

        boolean written = ZoteroDocumentPreferences.writeCitationStyle(
                document.getTextDocument(),
                citationStyle("ieee.csl", IEEE_STYLE_ID, "IEEE"));

        String updatedPreference = document.getZoteroPreference();
        assertTrue(written);
        assertEquals(Optional.of(IEEE_STYLE_ID), ZoteroDocumentPreferences.findStyleId(updatedPreference));
        assertTrue(updatedPreference.contains("fieldType"));
        assertTrue(updatedPreference.contains("ReferenceMark"));
    }

    private static CitationStyle citationStyle(String filePath, String styleId, String title) {
        return new CitationStyle(filePath, styleId, "in-text", title, title, false, true, false, title, true);
    }

    private static final class TestDocument {
        private static final String ZOTERO_PREF = "ZOTERO_PREF";

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

        private void putZoteroPreferenceChunks(String... chunks) {
            for (int chunkIndex = 0; chunkIndex < chunks.length; chunkIndex++) {
                properties.put(ZOTERO_PREF + "_" + (chunkIndex + 1), chunks[chunkIndex]);
            }
        }

        private String getZoteroPreference() {
            List<String> chunks = new ArrayList<>();
            int chunkIndex = 1;
            while (properties.containsKey(ZOTERO_PREF + "_" + chunkIndex)) {
                chunks.add(properties.get(ZOTERO_PREF + "_" + chunkIndex));
                chunkIndex++;
            }
            return String.join("", chunks);
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
