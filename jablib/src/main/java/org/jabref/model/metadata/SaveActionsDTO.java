package org.jabref.model.metadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveActionsDTO {
    public boolean state = false;
    public Map<String, List<String>> actions = new HashMap<>();
}
