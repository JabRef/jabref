package org.jabref.gui.auximport;

import java.util.stream.Collectors;

import org.jabref.logic.auxparser.AuxParserResult;
import org.jabref.logic.l10n.Localization;

public class AuxParserResultViewModel {

    private AuxParserResult auxParserResult;

    public AuxParserResultViewModel(AuxParserResult auxParserResult) {
        this.auxParserResult = auxParserResult;
    }

    /**
     * Prints parsing statistics
     *
     * @param includeMissingEntries shows the missing entries as text (the GUI renderes them at another place)
     */
    public String getInformation(boolean includeMissingEntries) {
        String missingEntries = "";
        if (includeMissingEntries && (this.auxParserResult.getUnresolvedKeysCount() > 0)) {
            missingEntries = this.auxParserResult.getUnresolvedKeys().stream().collect(Collectors.joining(", ", " (", ")"));
        }

        StringBuilder result = new StringBuilder();
        result.append(Localization.lang("keys in library")).append(' ').append(this.auxParserResult.getMasterDatabase().getEntryCount()).append('\n')
              .append(Localization.lang("found in AUX file")).append(' ').append(this.auxParserResult.getFoundKeysInAux()).append('\n')
              .append(Localization.lang("resolved")).append(' ').append(this.auxParserResult.getResolvedKeysCount()).append('\n')
              .append(Localization.lang("not found")).append(' ').append(this.auxParserResult.getUnresolvedKeysCount()).append(missingEntries).append('\n')
              .append(Localization.lang("crossreferenced entries included")).append(' ').append(this.auxParserResult.getCrossRefEntriesCount()).append('\n')
              .append(Localization.lang("strings included")).append(' ').append(this.auxParserResult.getInsertedStrings()).append('\n');
        if (this.auxParserResult.getNestedAuxCount() > 0) {
            result.append(Localization.lang("nested AUX files")).append(' ').append(this.auxParserResult.getNestedAuxCount());
        }
        return result.toString();
    }
}
