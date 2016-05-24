package net.sf.jabref.logic.auxparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.jabref.logic.l10n.Localization;
import net.sf.jabref.model.database.BibDatabase;

public class AuxParserResult {

    private final BibDatabase masterDatabase;
    private final Set<String> uniqueKeys = new HashSet<>();
    private final List<String> unresolvedKeys = new ArrayList<>();

    private final BibDatabase auxDatabase = new BibDatabase();
    private int nestedAuxCount;
    private int crossRefEntriesCount;

    public AuxParserResult(BibDatabase masterDatabase) {
        this.masterDatabase = masterDatabase;
    }

    public BibDatabase getGeneratedBibDatabase() {
        return auxDatabase;
    }

    public List<String> getUnresolvedKeys() {
        return unresolvedKeys;
    }

    public int getFoundKeysInAux() {
        return uniqueKeys.size();
    }

    public int getResolvedKeysCount() {
        return auxDatabase.getEntryCount() - crossRefEntriesCount;
    }

    public int getUnresolvedKeysCount() {
        return unresolvedKeys.size();
    }

    /**
     * Query the number of extra entries pulled in due to crossrefs from other entries.
     *
     * @return The number of additional entries pulled in due to crossref
     */
    public int getCrossRefEntriesCount() {
        return crossRefEntriesCount;
    }

    public void increaseCrossRefEntriesCounter() {
        crossRefEntriesCount++;
    }

    public void increaseNestedAuxFilesCounter() {
        nestedAuxCount++;
    }

    /**
     * Prints parsing statistics
     *
     * @param includeMissingEntries
     * @return
     */
    public String getInformation(boolean includeMissingEntries) {
        StringBuilder result = new StringBuilder();

        result.append(Localization.lang("keys_in_database")).append(' ').append(masterDatabase.getEntryCount()).append('\n')
                .append(Localization.lang("found_in_aux_file")).append(' ').append(getFoundKeysInAux()).append('\n')
                .append(Localization.lang("resolved")).append(' ').append(getResolvedKeysCount()).append('\n')
                .append(Localization.lang("not_found")).append(' ').append(getUnresolvedKeysCount()).append('\n')
                .append(Localization.lang("crossreferenced entries included")).append(' ')
                .append(crossRefEntriesCount).append('\n');

        if (includeMissingEntries && (getUnresolvedKeysCount() > 0)) {
            for (String entry : unresolvedKeys) {
                result.append(entry).append('\n');
            }
        }
        if (nestedAuxCount > 0) {
            result.append(Localization.lang("nested_aux_files")).append(' ').append(nestedAuxCount);
        }
        return result.toString();
    }

    public Set<String> getUniqueKeys() {
        return uniqueKeys;
    }
}
