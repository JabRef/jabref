package org.jabref.logic.git.merge.util;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jabref.logic.git.conflicts.ThreeWayEntryConflict;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.Field;

public class ConflictRules {
    /**
     * Detect entry-level conflicts among base, local, and remote versions of an entry.
     * <p>
     *
     * @param base the entry in the common ancestor
     * @param local the entry in the local version
     * @param remote the entry in the remote version
     * @return optional conflict (if detected)
     */
    public static Optional<ThreeWayEntryConflict> detectEntryConflict(BibEntry base,
                                                                      BibEntry local,
                                                                      BibEntry remote) {
        // Case 1: Both local and remote added same citation key -> compare their fields
        if (base == null && local != null && remote != null) {
            if (hasConflictingFields(new BibEntry(), local, remote)) {
                return Optional.of(new ThreeWayEntryConflict(null, local, remote));
            } else {
                return Optional.empty();
            }
        }

        // Case 2: base exists, one side deleted, other modified -> conflict
        if (base != null) {
            boolean localDeleted = local == null;
            boolean remoteDeleted = remote == null;

            boolean localChanged = !localDeleted && !base.getFieldMap().equals(local.getFieldMap());
            boolean remoteChanged = !remoteDeleted && !base.getFieldMap().equals(remote.getFieldMap());

            if ((localChanged && remoteDeleted) || (remoteChanged && localDeleted)) {
                return Optional.of(new ThreeWayEntryConflict(base, local, remote));
            }
        }

        // Case 3: base exists, both sides modified the entry -> check field-level diff
        if (base != null && local != null && remote != null) {
            boolean localChanged = !base.getFieldMap().equals(local.getFieldMap());
            boolean remoteChanged = !base.getFieldMap().equals(remote.getFieldMap());

            if (localChanged && remoteChanged && hasConflictingFields(base, local, remote)) {
                return Optional.of(new ThreeWayEntryConflict(base, local, remote));
            }
        }

        return Optional.empty();
    }

    public static boolean hasConflictingFields(BibEntry base, BibEntry local, BibEntry remote) {
        if (entryTypeChangedDifferently(base, local, remote)) {
            return true;
        }

        Set<Field> allFields = Stream.of(base, local, remote)
                                     .flatMap(entry -> entry.getFields().stream())
                                     .collect(Collectors.toSet());

        for (Field field : allFields) {
            String baseVal = base.getField(field).orElse(null);
            String localVal = local.getField(field).orElse(null);
            String remoteVal = remote.getField(field).orElse(null);

            // Case 1: Both local and remote modified the same field from base, and the values differ
            if (modifiedOnBothSidesWithDisagreement(baseVal, localVal, remoteVal)) {
                return true;
            }

            // Case 2: One side deleted the field, the other side modified it
            if (oneSideDeletedOneSideModified(baseVal, localVal, remoteVal)) {
                return true;
            }

            // Case 3: Both sides added the field with different values
            if (addedOnBothSidesWithDisagreement(baseVal, localVal, remoteVal)) {
                return true;
            }
        }

        return false;
    }

    public static boolean entryTypeChangedDifferently(BibEntry base, BibEntry local, BibEntry remote) {
        if (base == null || local == null || remote == null) {
            return false;
        }

        boolean localChanged = !base.getType().equals(local.getType());
        boolean remoteChanged = !base.getType().equals(remote.getType());
        boolean changedToDifferentTypes = !local.getType().equals(remote.getType());

        return localChanged && remoteChanged && changedToDifferentTypes;
    }

    public static boolean modifiedOnBothSidesWithDisagreement(String baseVal, String localVal, String remoteVal) {
        return notEqual(baseVal, localVal) && notEqual(baseVal, remoteVal) && notEqual(localVal, remoteVal);
    }

    public static boolean oneSideDeletedOneSideModified(String baseVal, String localVal, String remoteVal) {
        if (localVal == null && remoteVal == null) {
            return false;
        }

        return (baseVal != null)
                && ((localVal == null && notEqual(baseVal, remoteVal))
                || (remoteVal == null && notEqual(baseVal, localVal)));
    }

    public static boolean addedOnBothSidesWithDisagreement(String baseVal, String localVal, String remoteVal) {
        return baseVal == null && localVal != null && remoteVal != null && notEqual(localVal, remoteVal);
    }

    public static boolean notEqual(String a, String b) {
        return !Objects.equals(a, b);
    }
}
