package org.jabref.logic.ai.preferences;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.Property;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/// Guards [AiPreferences#copyFrom(AiPreferences)] against silently dropping values: the AI
/// preferences tab commits every edit to a dialog-scoped working copy and relies on `copyFrom`
/// to flush each one back to the live preferences on save. A newly added property that is missing
/// from `copyFrom` would look editable in the dialog but never persist — this test enumerates the
/// properties reflectively, so adding a field without extending `copyFrom` fails here.
class AiPreferencesCopyFromTest {

    /// Captures the state at JabRef startup and belongs to the instance's own session; `copyFrom`
    /// deliberately leaves it alone.
    private static final String NOT_COPIED = "aiFeaturesEnabledInitially";

    @Test
    void copyFromCopiesEveryProperty() throws IllegalAccessException {
        AiPreferences source = AiPreferences.getDefault();
        AiPreferences target = AiPreferences.getDefault();

        List<Field> propertyFields = propertyFields();
        assertFalse(propertyFields.isEmpty(), "reflection found no properties; the test no longer tests anything");

        for (Field field : propertyFields) {
            Property<Object> property = propertyOf(source, field);
            Object changed = differentValue(field.getName(), property.getValue());
            if (changed.equals(property.getValue())) {
                // A single-constant enum: no different value exists, so a copy is unobservable.
                continue;
            }
            property.setValue(changed);
        }

        target.copyFrom(source);

        for (Field field : propertyFields) {
            assertEquals(propertyOf(source, field).getValue(), propertyOf(target, field).getValue(),
                    "AiPreferences.copyFrom does not copy '%s'; extend copyFrom when adding a preference".formatted(field.getName()));
        }
    }

    @Test
    void copyFromLeavesInitialEnabledStateAlone() throws IllegalAccessException {
        AiPreferences source = AiPreferences.getDefault();
        AiPreferences target = AiPreferences.getDefault();

        Field field = fieldNamed(NOT_COPIED);
        propertyOf(source, field).setValue(true);

        target.copyFrom(source);

        assertFalse((Boolean) propertyOf(target, field).getValue(),
                "'%s' captures the session's startup state and must not be overwritten by copyFrom".formatted(NOT_COPIED));
    }

    /// All JavaFX property fields of [AiPreferences] except [#NOT_COPIED].
    private static List<Field> propertyFields() {
        List<Field> result = new ArrayList<>();
        for (Field field : AiPreferences.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())
                    || !Property.class.isAssignableFrom(field.getType())
                    || NOT_COPIED.equals(field.getName())) {
                continue;
            }
            result.add(field);
        }
        return result;
    }

    private static Field fieldNamed(String name) {
        for (Field field : AiPreferences.class.getDeclaredFields()) {
            if (field.getName().equals(name)) {
                return field;
            }
        }
        throw new AssertionError("AiPreferences no longer has a field '%s'; update this test".formatted(name));
    }

    @SuppressWarnings("unchecked")
    private static Property<Object> propertyOf(AiPreferences preferences, Field field) throws IllegalAccessException {
        field.setAccessible(true);
        return (Property<Object>) field.get(preferences);
    }

    /// A value of the property's type that differs from `current`, so that a copy is observable.
    private static Object differentValue(String fieldName, Object current) {
        return switch (current) {
            case Boolean value ->
                    !value;
            case String value ->
                    value + "-changed";
            case Integer value ->
                    value + 1;
            case Double value ->
                    value + 0.25;
            case Enum<?> value -> {
                Object[] constants = value.getDeclaringClass().getEnumConstants();
                yield constants[(value.ordinal() + 1) % constants.length];
            }
            case null ->
                    throw new AssertionError("property '%s' holds null by default; teach differentValue() its type".formatted(fieldName));
            default ->
                    throw new AssertionError("property '%s' holds a %s; teach differentValue() this type".formatted(fieldName, current.getClass()));
        };
    }
}
