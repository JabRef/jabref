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

public class SaveActions {

    private HashMap<String, Formatter> actions;

    private List<Formatter> availableFormatters;

    public static final String META_KEY = "saveActions";

    public SaveActions(MetaData metaData) {
        actions = new HashMap<>();
        List<String> formatters = metaData.getData(META_KEY);
        setAvailableFormatters();

        for (int i = 0; i < formatters.size(); i += 2) {
            try {
                String field = formatters.get(i);
                Formatter formatter = getFormatterFromString(formatters.get(i + 1));

                actions.put(field, formatter);
            } catch(IndexOutOfBoundsException e){
                // the meta data string in the file is broken. -> Ignore the last item
                break;
            }
        }

    }

    private void setAvailableFormatters() {
        availableFormatters = new ArrayList<>();

        availableFormatters.addAll(BibtexFieldFormatters.ALL);
        availableFormatters.addAll(CaseChangers.ALL);
    }

    public BibEntry applySaveActions(BibEntry entry) {
        for (String key : actions.keySet()) {
            Formatter formatter = actions.get(key);

            String fieldContent = entry.getField(key);
            String formattedContent = formatter.format(fieldContent);
            entry.setField(key, formattedContent);
        }

        return entry;
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
