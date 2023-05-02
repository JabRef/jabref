package org.jabref.model.schema;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.type.AbstractField;
import org.apache.xmpbox.type.ArrayProperty;
import org.apache.xmpbox.type.DateType;
import org.apache.xmpbox.type.StructuredType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  A DublinCoreSchema extension Class.
 *  In case anyone intends to alter standard behaviour.
 */
@StructuredType(preferedPrefix = "dc", namespace = "http://purl.org/dc/elements/1.1/")
public class DublinCoreSchemaCustom extends DublinCoreSchema {

    private static final Logger LOGGER = LoggerFactory.getLogger(DublinCoreSchemaCustom.class);

    public DublinCoreSchemaCustom(XMPMetadata metadata) {
        super(metadata);
    }

    public static DublinCoreSchema copyDublinCoreSchema(DublinCoreSchema dcSchema) {
        if (Objects.isNull(dcSchema)) {
            return null;
        }

        try {
            DublinCoreSchemaCustom dublinCoreSchemaCustom = new DublinCoreSchemaCustom(dcSchema.getMetadata());
            FieldUtils.writeField(dublinCoreSchemaCustom, "container", dcSchema.getContainer(), true);
            FieldUtils.writeField(dublinCoreSchemaCustom, "attributes",
                    FieldUtils.readField(dcSchema, "attributes", true), true);
            return dublinCoreSchemaCustom;
        } catch (Exception e) {
            LOGGER.error("Error making custom DC Schema. Using the default", e);
            return dcSchema;
        }
    }

    /**
     *  Overloaded XMP Schema method
     *  Behaviour is same except when seqName is "Date". Will return raw value instead
     */
    @Override
    public List<String> getUnqualifiedSequenceValueList(String seqName) {
        AbstractField abstractProperty = getAbstractProperty(seqName);
        if (abstractProperty instanceof ArrayProperty) {
            if ("date".equals(seqName)) {
                return ((ArrayProperty) abstractProperty).getContainer()
                        .getAllProperties()
                        .stream()
                        .map(field -> (String) ((DateType) field).getRawValue())
                        .collect(Collectors.toList());
            }
            return ((ArrayProperty) abstractProperty).getElementsAsString();
        }
        return null;
    }
}
