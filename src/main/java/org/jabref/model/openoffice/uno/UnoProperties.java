package org.jabref.model.openoffice.uno;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.sun.star.beans.Property;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.lang.WrappedTargetException;

/**
 *  Utilities for properties.
 */
public class UnoProperties {

    private UnoProperties() { }

    /*
     * asPropertySet
     */

    public static Optional<XPropertySet> asPropertySet(XPropertyContainer propertyContainer) {
        return UnoCast.cast(XPropertySet.class, propertyContainer);
    }

    /*
     * getPropertySetInfo
     */

    public static Optional<XPropertySetInfo> getPropertySetInfo(XPropertySet propertySet) {
        return (Optional.ofNullable(propertySet)
                .flatMap(e -> Optional.ofNullable(e.getPropertySetInfo())));
    }

    public static Optional<XPropertySetInfo> getPropertySetInfo(XPropertyContainer propertyContainer) {
        return Optional.ofNullable(propertyContainer).flatMap(UnoProperties::getPropertySetInfo);
    }

    /*
     * getPropertyNames
     */

    public static List<String> getPropertyNames(Property[] properties) {
        Objects.requireNonNull(properties);
        return (Arrays.stream(properties)
                .map(p -> p.Name)
                .collect(Collectors.toList()));
    }

    public static List<String> getPropertyNames(XPropertySetInfo propertySetInfo) {
        return getPropertyNames(propertySetInfo.getProperties());
    }

    public static List<String> getPropertyNames(XPropertySet propertySet) {
        return getPropertyNames(propertySet.getPropertySetInfo());
    }

    public static List<String> getPropertyNames(XPropertyContainer propertyContainer) {
        return (asPropertySet(propertyContainer)
                .map(UnoProperties::getPropertyNames)
                .orElse(new ArrayList<>()));
    }

    /*
     * getPropertyValue
     */

    public static Optional<Object> getValueAsObject(XPropertySet propertySet, String property)
        throws
        WrappedTargetException {
        Objects.requireNonNull(propertySet);
        Objects.requireNonNull(property);
        try {
            return Optional.ofNullable(propertySet.getPropertyValue(property));
        } catch (UnknownPropertyException e) {
            return Optional.empty();
        }
    }

    public static Optional<Object> getValueAsObject(XPropertyContainer propertyContainer, String property)
        throws
        WrappedTargetException {
        Optional<XPropertySet> propertySet = asPropertySet(propertyContainer);
        if (propertySet.isEmpty()) {
            return Optional.empty();
        }
        return UnoProperties.getValueAsObject(propertySet.get(), property);
    }
}
