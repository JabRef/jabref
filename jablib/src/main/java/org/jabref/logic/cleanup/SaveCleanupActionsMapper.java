package org.jabref.logic.cleanup;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jabref.logic.util.strings.StringUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public class SaveCleanupActionsMapper {
    //    public static final EnumSet<CleanupPreferences.CleanupStep> SAVE_ACTIONS_CLEANUP_STEPS = EnumSet.of(
    //            CleanupPreferences.CleanupStep.ABBREVIATE_DEFAULT,
    //            CleanupPreferences.CleanupStep.ABBREVIATE_DOTLESS,
    //            CleanupPreferences.CleanupStep.ABBREVIATE_SHORTEST_UNIQUE,
    //            CleanupPreferences.CleanupStep.ABBREVIATE_LTWA,
    //            CleanupPreferences.CleanupStep.UNABBREVIATE,
    //            CleanupPreferences.CleanupStep.CLEAN_UP_DOI,
    //            CleanupPreferences.CleanupStep.CLEANUP_EPRINT,
    //            CleanupPreferences.CleanupStep.CLEAN_UP_URL,
    //            CleanupPreferences.CleanupStep.CONVERT_TO_BIBLATEX,
    //            CleanupPreferences.CleanupStep.CONVERT_TO_BIBTEX,
    //            CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE,
    //            CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE
    //    );
    private static final BiMap<Supplier<? extends CleanupJob>, String> SAVE_ACTIONS_CLEANUP_STEPS_MAP = HashBiMap.create();

    static {
        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(DoiCleanup::new, "CLEAN_UP_DOI");
        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(EprintCleanup::new, "CLEANUP_EPRINT");
        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(URLCleanup::new, "CLEAN_UP_URL");
        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(ConvertToBiblatexCleanup::new, "CONVERT_TO_BIBLATEX");
        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(ConvertToBibtexCleanup::new, "CONVERT_TO_BIBTEX");
        //        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(TimeStampToCreationDate::new, CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_CREATIONDATE);
        //        SAVE_ACTIONS_CLEANUP_STEPS_MAP.put(TimeStampToModificationDate::new, CleanupPreferences.CleanupStep.CONVERT_TIMESTAMP_TO_MODIFICATIONDATE);
    }

    private static final Pattern FIELD_FORMATTER_CLEANUP_PATTERN = Pattern.compile("([^\\[]+)\\[([^]]+)]");

    private SaveCleanupActionsMapper() {
    }

    /// This parses the key/list map of fields and clean up actions for the field.
    ///
    /// General format for one key/list map: `...[...]` - `field[formatter1,formatter2,...]`
    /// Multiple are written as `...[...]...[...]...[...]`
    /// `field1[formatter1,formatter2,...]field2[formatter3,formatter4,...]`
    ///
    /// The idea is that characters are field names until `[` is reached and that formatter lists are terminated by `]`
    ///
    /// Example: `pages[normalize_page_numbers]title[escapeAmpersands,escapeDollarSign,escapeUnderscores,latex_cleanup]`
    public static List<CleanupJob> parseActions(String saveActionsString) {
        //        JournalAbbreviationRepository
        if ((saveActionsString == null) || saveActionsString.isEmpty()) {
            // no save actions defined in the metadata
            return List.of();
        }

        List<CleanupJob> result = new ArrayList<>();

        // first remove all newlines for easier parsing
        String separator = ";";
        String formatterStringWithoutLineBreaks = StringUtil.unifyLineBreaks(saveActionsString, separator);
        String[] cleanupsSubStrings = formatterStringWithoutLineBreaks.split(";");
        Pattern pattern = Pattern.compile("\\((.*?)\\)");
        for (String subStr : cleanupsSubStrings) {
            Matcher matcher = pattern.matcher(subStr);
            if (matcher.find()) {
                result.add(FieldFormatterCleanupMapper.parseActions(matcher.group(1)).getFirst());
            } else {
                Supplier<? extends CleanupJob> supplier = SAVE_ACTIONS_CLEANUP_STEPS_MAP.inverse().get(subStr);
                if (supplier != null) {
                    result.add(supplier.get());
                }
            }
        }

        return result;
    }

    public static String serializeActions(List<CleanupJob> actionList, String newLineSeparator) {
        StringBuilder result = new StringBuilder();
        for (CleanupJob cleanupJob : actionList) {
            if (cleanupJob instanceof FieldFormatterCleanup) {
                result.append("FIELD_FORMATTER_CLEANUP(").append(FieldFormatterCleanupMapper.serializeActions(List.of((FieldFormatterCleanup) cleanupJob), newLineSeparator)).append(")");
            } else {
                String cleanupActionStr = SAVE_ACTIONS_CLEANUP_STEPS_MAP.get(cleanupJob.getClass());
                if (cleanupActionStr != null) {
                    result.append(cleanupActionStr);
                } else {
                    throw new UnsupportedOperationException(cleanupJob.toString());
                }
            }
            result.append(newLineSeparator);
        }

        return result.toString();
    }
}
