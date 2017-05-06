package org.jabref.gui.fieldeditors;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class YesNoEditorViewModel extends MapBasedEditorViewModel<String> {

    private BiMap<String, String> itemMap = HashBiMap.create(2);

    public YesNoEditorViewModel() {
        itemMap.put("yes", "Yes");
        itemMap.put("no", "No");
    }

    @Override
    protected BiMap<String, String> getItemMap() {
        return itemMap;
    }

    @Override
    public String convertToDisplayText(String object) {
        return object;
    }
}
