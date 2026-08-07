package org.jabref.logic.biblog;

import java.util.ArrayList;
import java.util.List;

import org.jabref.logic.integrity.IntegrityMessage;
import org.jabref.model.biblog.BibWarning;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.field.FieldFactory;
import org.jabref.model.entry.field.InternalField;

/// Converts {@link BibWarning}s into {@link IntegrityMessage}s for integration with the existing integrity check UI.
///
/// Notes:
/// - The current IntegrityMessage interface expects a {@link BibEntry} and a {@link Field},
///   but .blg warnings come from a different source and may not include a field.
/// - For now, we map missing fields to a placeholder (InternalField.KEY_FIELD) to make it compatible with the UI.
/// - This is a minimal MVP solution to reuse the Integrity tab without changing the UI structure.
///
/// Future direction:
/// - Consider defining a proper interface (e.g., IntegrityMessageWithField / WithoutField)
///   to support warnings without fields cleanly.
public class BibWarningToIntegrityMessageConverter {
    public static List<IntegrityMessage> convert(
            List<BibWarning> bibWarnings,
            BibDatabaseContext context) {
        if (bibWarnings.isEmpty()) {
            return List.of();
        }

        List<IntegrityMessage> messages = new ArrayList<>();
        for (BibWarning bibWarning : bibWarnings) {
            if (context.getDatabase().getEntryByCitationKey(bibWarning.entryKey()).isEmpty()) {
                continue;
            }

            BibEntry entry = context.getDatabase().getEntryByCitationKey(bibWarning.entryKey()).get();

            Field field = bibWarning.getFieldName()
                                    .map(FieldFactory::parseField)
                                    .orElse(InternalField.KEY_FIELD);

            IntegrityMessage message = new IntegrityMessage(
                    bibWarning.message(),
                    entry,
                    field);
            messages.add(message);
        }
        return messages;
    }
}
