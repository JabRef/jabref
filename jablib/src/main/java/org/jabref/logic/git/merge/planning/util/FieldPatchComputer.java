package org.jabref.logic.git.merge.planning.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;
import org.jabref.model.entry.types.StandardEntryType;

import static org.jabref.logic.git.merge.planning.util.MergeFieldUtil.isMetaField;

public final class FieldPatchComputer {
    private static final BibEntry EMPTY_ENTRY = new BibEntry(StandardEntryType.Article);

    /// Compares base and remote and constructs a patch at the field level. null == the field is deleted.
    ///
    /// - Apply remote change when local kept base value (including deletions: null);
    /// - If both sides changed to the same value, no patch needed;
    /// - Fallback: if a divergence is still observed, do not override local; skip this field,
    ///
    /// @param base base version
    /// @param local local version
    /// @param remote remote version
    /// @return A map from field to new value
    public static Map<Field, String> compute(BibEntry base, BibEntry local, BibEntry remote) {
        Map<Field, String> patch = new LinkedHashMap<>();

        if (remote == null) {
            return patch;
        }

        BibEntry baseSafe = (base == null) ? EMPTY_ENTRY : base;
        Stream.concat(baseSafe.getFields().stream(), remote.getFields().stream())
              .distinct()
              .filter(field -> !isMetaField(field))
              .forEach(field -> {
                  String baseValue = baseSafe.getField(field).orElse(null);
                  String remoteValue = remote.getField(field).orElse(null);
                  String localValue = local == null ? null : local.getField(field).orElse(null);

                  if (Objects.equals(baseValue, remoteValue)) {
                      return;
                  }
                  if (Objects.equals(localValue, baseValue)) {
                      patch.put(field, remoteValue);
                      return;
                  }
                  if (Objects.equals(localValue, remoteValue)) {
                      return;
                  }
              });
        return patch;
    }
}
