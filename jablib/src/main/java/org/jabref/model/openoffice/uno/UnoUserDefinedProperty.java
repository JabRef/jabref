package org.jabref.model.openoffice.uno;

import java.util.ArrayList;
import java.util.List;
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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Document level user-defined properties.
 * <p>
 * LibreOffice GUI: [File]/[Properties]/[Custom Properties]
 */
public class UnoUserDefinedProperty {

    private static final Logger LOGGER = LoggerFactory.getLogger(UnoUserDefinedProperty.class);

    private UnoUserDefinedProperty() {
    }

    public static Optional<XPropertyContainer> getPropertyContainer(XTextDocument doc) {
        return UnoTextDocument.getDocumentProperties(doc).map(XDocumentProperties::getUserDefinedProperties);
    }

    public static List<String> getListOfNames(XTextDocument doc) {
        return UnoUserDefinedProperty.getPropertyContainer(doc)
                                     .map(UnoProperties::getPropertyNames)
                                     .orElse(new ArrayList<>());
    }

    /**
     * @param property Name of a custom document property in the current document.
     * @return The value of the property or Optional.empty()
     * These properties are used to store extra data about individual citation. In particular, the `pageInfo` part.
     */
    public static Optional<String> getStringValue(XTextDocument doc, String property)
            throws
            WrappedTargetException {
        Optional<XPropertySet> propertySet = UnoUserDefinedProperty.getPropertyContainer(doc)
                                                                   .flatMap(UnoProperties::asPropertySet);
        if (propertySet.isEmpty()) {
            throw new java.lang.IllegalArgumentException("getting UserDefinedProperties as XPropertySet failed");
        }
        try {
            String value = propertySet.get().getPropertyValue(property).toString();
            return Optional.ofNullable(value);
        } catch (UnknownPropertyException _) {
            return Optional.empty();
        }
    }

    /**
     * @param property Name of a custom document property in the current document. Created if does not exist yet.
     * @param value    The value to be stored.
     */
    public static void setStringProperty(XTextDocument doc, @NonNull String property, @NonNull String value)
            throws
            IllegalTypeException,
            PropertyVetoException,
            WrappedTargetException {
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
            } catch (UnknownPropertyException _) {
                // fall through to addProperty
            }
        }

        try {
            container.get().addProperty(property, PropertyAttribute.REMOVEABLE, new Any(Type.STRING, value));
        } catch (PropertyExistException _) {
            throw new java.lang.IllegalStateException("Caught PropertyExistException for property assumed not to exist");
        }
    }

    /**
     * @param property Name of a custom document property in the current document.
     *                 <p>
     *                 Logs warning if does not exist.
     */
    public static void remove(XTextDocument doc, @NonNull String property)
            throws
            NotRemoveableException {
        Optional<XPropertyContainer> container = UnoUserDefinedProperty.getPropertyContainer(doc);

        if (container.isEmpty()) {
            throw new java.lang.IllegalArgumentException("getUserDefinedPropertiesAsXPropertyContainer failed");
        }

        try {
            container.get().removeProperty(property);
        } catch (UnknownPropertyException ex) {
            LOGGER.warn("UnoUserDefinedProperty.remove({}) This property was not there to remove", property, ex);
        }
    }

    /**
     * @param property Name of a custom document property in the current document.
     *                 <p>
     *                 Keep silent if property did not exist.
     */
    public static void removeIfExists(XTextDocument doc, @NonNull String property)
            throws
            NotRemoveableException {
        Optional<XPropertyContainer> container = UnoUserDefinedProperty.getPropertyContainer(doc);

        if (container.isEmpty()) {
            throw new java.lang.IllegalArgumentException("getUserDefinedPropertiesAsXPropertyContainer failed");
        }

        try {
            container.get().removeProperty(property);
        } catch (UnknownPropertyException _) {
            // did not exist
        }
    }
}
