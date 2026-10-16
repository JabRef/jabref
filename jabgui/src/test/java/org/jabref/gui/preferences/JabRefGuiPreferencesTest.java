package org.jabref.gui.preferences;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jabref.gui.entryeditor.EntryEditorTabModel;
import org.jabref.gui.preview.PreviewPreferences;
import org.jabref.logic.preview.CustomizedPreviewStyle;
import org.jabref.logic.preview.TextBasedPreviewLayout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JabRefGuiPreferencesTest {

    private static final EntryEditorTabModel PREVIEW =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.PREVIEW, true);
    private static final EntryEditorTabModel MAIN =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.ALL_FIELDS, true);
    private static final EntryEditorTabModel SOURCE =
            new EntryEditorTabModel.BuiltInTab(EntryEditorTabModel.BuiltIn.SOURCE, true);

    @Test
    void storedTabOrderIsApplied() {
        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, SOURCE),
                List.of("SOURCE", "ALL_FIELDS"));

        assertEquals(List.of(PREVIEW, SOURCE, MAIN), ordered);
    }

    @Test
    void tabsUnknownToStoredOrderAreAppendedInDefaultOrder() {
        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, SOURCE),
                List.of("SOURCE"));

        assertEquals(List.of(PREVIEW, SOURCE, MAIN), ordered);
    }

    @Test
    void duplicateCustomTabNamesAreAllKept() {
        EntryEditorTabModel first = new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("keywords"));
        EntryEditorTabModel second = new EntryEditorTabModel.CustomizedFieldsTab("General", List.of("doi"));

        List<EntryEditorTabModel> ordered = JabRefGuiPreferences.applyStoredTabOrder(
                List.of(PREVIEW, MAIN, first, second),
                List.of("custom:General", "ALL_FIELDS"));

        // The stored-order entry consumes the first duplicate; the second survives at the end.
        assertEquals(List.of(PREVIEW, first, MAIN, second), ordered);
    }

    // [utest->req~entry-editor.custom-tabs.extract-field~1]
    @Test
    void parseCustomTabsReadsPlainStringsAndExtractObjects() {
        assertEquals(
                List.of(new EntryEditorTabModel.CustomizedFieldsTab(
                        "My tab", List.of("author", "doi"), Set.of("doi"))),
                JabRefGuiPreferences.parseCustomTabs(
                        """
                                {"My tab":["author",{"pattern":"doi","extract":true}]}
                                """));
    }

    @Test
    void storeCustomizedPreviewStylePersistsData() {
        List<CustomizedPreviewStyle> styles = List.of(
                new CustomizedPreviewStyle("id1", "name1", "text1"),
                new CustomizedPreviewStyle("id2", "name2", "text2"));
        JabRefGuiPreferences jabRefGuiPreferences = new InMemoryGuiPreferences();
        jabRefGuiPreferences.storeCustomizedPreviewStyle(styles);
        List<CustomizedPreviewStyle> customizedPreviewStyleList = jabRefGuiPreferences.getCustomizedPreviewStyle(List.of());
        assertEquals(styles, customizedPreviewStyleList);
    }

    @Test
    void storedDefaultStyleEqualsTheDefault() {
        List<CustomizedPreviewStyle> defaults = PreviewPreferences.getDefault().getCustomizedPreviewStyles();
        assertEquals(TextBasedPreviewLayout.NAME, defaults.getFirst().id());

        JabRefGuiPreferences jabRefGuiPreferences = new InMemoryGuiPreferences();
        jabRefGuiPreferences.storeCustomizedPreviewStyle(defaults);

        List<CustomizedPreviewStyle> customizedPreviewStyleList = jabRefGuiPreferences.getCustomizedPreviewStyle(defaults);
        assertEquals(TextBasedPreviewLayout.NAME, customizedPreviewStyleList.getFirst().id());
        assertEquals(defaults, customizedPreviewStyleList);
    }

    @Test
    void getCustomizedPreviewStyleMigratesPreviewStyle() {
        JabRefGuiPreferences preferences = new InMemoryGuiPreferences();
        preferences.put(JabRefGuiPreferences.PREVIEW_STYLE, "\\myText__NEWLINE__");

        List<CustomizedPreviewStyle> result = preferences.getCustomizedPreviewStyle(List.of());

        assertEquals(List.of(new CustomizedPreviewStyle(TextBasedPreviewLayout.NAME,
                TextBasedPreviewLayout.DEFAULT_DISPLAY_NAME, "\\myText\n")), result);
    }
}

class InMemoryGuiPreferences extends JabRefGuiPreferences {
    // Used so tests do not persist to local preferences
    private Map<String, String> store;

    private Map<String, String> store() {
        if (store == null) {
            store = new HashMap<>();
        }
        return store;
    }

    @Override
    public boolean hasKey(String key) {
        return store().containsKey(key);
    }

    @Override
    public String get(String key, String def) {
        return store().getOrDefault(key, def);
    }

    @Override
    public void put(String key, String value) {
        store().put(key, value);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return store().containsKey(key) ? Boolean.parseBoolean(store().get(key)) : def;
    }

    @Override
    public void putBoolean(String key, boolean value) {
        store().put(key, String.valueOf(value));
    }

    @Override
    protected void remove(String key) {
        store().remove(key);
    }
}
