package net.sf.jabref.exporter;

import net.sf.jabref.MetaData;
import net.sf.jabref.logic.cleanup.FieldFormatterCleanup;
import net.sf.jabref.logic.formatter.BibtexFieldFormatters;
import net.sf.jabref.logic.formatter.CaseChangers;
import net.sf.jabref.logic.formatter.Formatter;
import net.sf.jabref.logic.formatter.IdentityFormatter;
import net.sf.jabref.model.entry.BibEntry;

import java.util.*;

public class SaveActions {

    private final List<FieldFormatterCleanup> actions;

    private List<Formatter> availableFormatters;

    public static final String META_KEY = "saveActions";

    private boolean enabled;

    public SaveActions(MetaData metaData) {
        Objects.requireNonNull(metaData);

        actions = new ArrayList<>();
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

    public boolean isEnabled() {
        return enabled;
    }

    public List<FieldFormatterCleanup> getConfiguredActions() {
        return Collections.unmodifiableList(actions);
    }

    public List<Formatter> getAvailableFormatters() {
        return Collections.unmodifiableList(availableFormatters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SaveActions that = (SaveActions) o;

        if (enabled != that.enabled) return false;
        return actions.equals(that.actions);

    }

    @Override
    public int hashCode() {
        int result = actions.hashCode();
        result = 31 * result + (enabled ? 1 : 0);
        return result;
    }

    private void parseSaveActions(List<String> formatters) {
        //read concrete actions
        for (int i = 1; i < formatters.size(); i += 2) {
            try {
                String field = formatters.get(i);
                Formatter formatter = getFormatterFromString(formatters.get(i + 1));

                actions.add(new FieldFormatterCleanup(field, formatter));
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
        for (FieldFormatterCleanup action : actions) {
            action.cleanup(entry);
        }
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
