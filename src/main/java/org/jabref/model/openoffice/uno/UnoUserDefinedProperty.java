package org.jabref.model.openoffice.uno;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.NotRemoveableException;
import com.sun.star.beans.PropertyAttribute;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Document level user-defined properties.
 *
 *  LibreOffice GUI: [File]/[Properties]/[Custom Properties]
 */
public class UnoUserDefinedProperty {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoUserDefinedProperty.class);

    private UnoUserDefinedProperty() { }

    public static Optional<XPropertyContainer> getPropertyContainer(XTextDocument doc) {
        return UnoTextDocument.getDocumentProperties(doc).map(XDocumentProperties::getUserDefinedProperties);
    }

    public static List<String> getListOfNames(XTextDocument doc) {
        return (UnoUserDefinedProperty.getPropertyContainer(doc)
                .map(UnoProperties::getPropertyNames)
                .orElse(new ArrayList<>()));
    }

    /**
     * @param property Name of a custom document property in the current document.
     *
     * @return The value of the property or Optional.empty()
     *
     * These properties are used to store extra data about individual citation.
     * In particular, the `pageInfo` part.
     *
     */
    public static Optional<String> getStringValue(XTextDocument doc, String property)
        throws
        WrappedTargetException {
        Optional<XPropertySet> propertySet = (UnoUserDefinedProperty.getPropertyContainer(doc)
                                              .flatMap(UnoProperties::asPropertySet));
        if (propertySet.isEmpty()) {
            throw new java.lang.IllegalArgumentException("getting UserDefinedProperties as XPropertySet failed");
        }
        try {
            String value = propertySet.get().getPropertyValue(property).toString();
            return Optional.ofNullable(value);
        } catch (UnknownPropertyException ex) {
            return Optional.empty();
        }
    }

    /**
     * @param property Name of a custom document property in the current document.
     *                 Created if does not exist yet.
     *
     * @param value The value to be stored.
     */
    public static void setStringProperty(XTextDocument doc, String property, String value)
        throws
        IllegalTypeException,
        PropertyVetoException,
        WrappedTargetException {

        Objects.requireNonNull(property);
        Objects.requireNonNull(value);

        Optional<XPropertyContainer> container = UnoUserDefinedProperty.getPropertyContainer(doc);

        if (container.isEmpty()) {
            throw new java.lang.IllegalArgumentException("UnoUserDefinedProperty.getPropertyContainer failed");
        }

        Optional<XPropertySet> propertySet = container.flatMap(UnoProperties::asPropertySet);
        if (propertySet.isEmpty()) {
            throw new java.lang.IllegalArgumentException("asPropertySet failed");
        }

        XPropertySetInfo propertySetInfo = propertySet.get().getPropertySetInfo();

        if (propertySetInfo.hasPropertyByName(property)) {
            try {
                propertySet.get().setPropertyValue(property, value);
                return;
            } catch (UnknownPropertyException ex) {
                // fall through to addProperty
            }
        }

        try {
            container.get().addProperty(property, PropertyAttribute.REMOVEABLE, new Any(Type.STRING, value));
        } catch (PropertyExistException ex) {
            throw new java.lang.IllegalStateException("Caught PropertyExistException for property assumed not to exist");
        }
    }

    /**
     * @param property Name of a custom document property in the current document.
     *
     *  Logs warning if does not exist.
     */
    public static void remove(XTextDocument doc, String property)
        throws
        NotRemoveableException {

        Objects.requireNonNull(property);

        Optional<XPropertyContainer> container = UnoUserDefinedProperty.getPropertyContainer(doc);

        if (container.isEmpty()) {
            throw new java.lang.IllegalArgumentException("getUserDefinedPropertiesAsXPropertyContainer failed");
        }

        try {
            container.get().removeProperty(property);
        } catch (UnknownPropertyException ex) {
            LOGGER.warn(String.format("UnoUserDefinedProperty.remove(%s) This property was not there to remove",
                                      property));
        }
    }

    /**
     * @param property Name of a custom document property in the current document.
     *
     * Keep silent if property did not exist.
     */
    public static void removeIfExists(XTextDocument doc, String property)
        throws
        NotRemoveableException {

        Objects.requireNonNull(property);

        Optional<XPropertyContainer> container = UnoUserDefinedProperty.getPropertyContainer(doc);

        if (container.isEmpty()) {
            throw new java.lang.IllegalArgumentException("getUserDefinedPropertiesAsXPropertyContainer failed");
        }

        try {
            container.get().removeProperty(property);
        } catch (UnknownPropertyException ex) {
            // did not exist
        }
    }
}
