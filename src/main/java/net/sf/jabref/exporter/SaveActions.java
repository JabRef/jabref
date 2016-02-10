package net.sf.jabref.exporter;

import net.sf.jabref.MetaData;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.model.entry.BibEntry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SaveActions {

    private final Map<String, Formatter> actions;

    private List<Formatter> availableFormatters;

    public static final String META_KEY = "saveActions";

    private boolean enabled;

    public SaveActions(MetaData metaData) {
        actions = new HashMap<>();
        setAvailableFormatters();

        List<String> formatters = metaData.getData(META_KEY);
        if (formatters == null) {
            // no save actions defined in the meta data
            return;
        } else {
            parseEnabledStatus(formatters);

            parseSaveActions(formatters);
        }

    }

    private void parseSaveActions(List<String> formatters) {
        //read concrete actions
        for (int i = 0; i < formatters.size(); i += 2) {
            try {
                String field = formatters.get(i);
                Formatter formatter = getFormatterFromString(formatters.get(i + 1));

                actions.put(field, formatter);
            } catch (IndexOutOfBoundsException e) {
                // the meta data string in the file is broken. -> Ignore the last item
                break;
            }
        }
    }

    private void parseEnabledStatus(List<String> formatters) {
        //read if save actions should be enabled
        String enableActions = formatters.get(0);
        if ("enabled".equals(enableActions)) {
            enabled = true;
        } else {
            enabled = false;
        }
    }

    private void setAvailableFormatters() {
        availableFormatters = new ArrayList<>();

        availableFormatters.addAll(BibtexFieldFormatters.ALL);
        availableFormatters.addAll(CaseChangers.ALL);
    }

    public BibEntry applySaveActions(BibEntry entry) {
        if (enabled) {
            applyAllActions(entry);
        }
        return entry;
    }

    private void applyAllActions(BibEntry entry) {
        for (String key : actions.keySet()) {
            applyActionForField(entry, key);
        }
    }

    private void applyActionForField(BibEntry entry, String key) {
        Formatter formatter = actions.get(key);

        String fieldContent = entry.getField(key);
        String formattedContent = formatter.format(fieldContent);
        entry.setField(key, formattedContent);
    }

    private Formatter getFormatterFromString(String formatterName) {
        for (Formatter formatter : availableFormatters) {
            if (formatterName.equals(formatter.getKey())) {
                return formatter;
            }
        }
        return new IdentityFormatter();
    }

}
