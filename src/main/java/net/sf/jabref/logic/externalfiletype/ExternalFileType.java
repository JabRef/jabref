package net.sf.jabref.logic.externalfiletype;

import java.util.Objects;

/**
 * This class defines a type of external files that can be linked to from JabRef.
 * The class contains enough information to provide an icon, a standard extension
 * and a link to which application handles files of this type.
 */
public class ExternalFileType implements Comparable<ExternalFileType> {

    private String name;
    private String extension;
    private String openWith;
    private String mimeType;
    private String materialDesignIconCodePoint;

    public ExternalFileType(String name, String extension, String mimeType, String openWith,
            String materialDesignIconCodePoint) {
        this.name = name;
        this.extension = extension;
        this.mimeType = mimeType;
        this.openWith = openWith;
        this.materialDesignIconCodePoint = materialDesignIconCodePoint;
    }

    /**
     * Construct an ExternalFileType from a String array. This is used when
     * reading file type definitions from Preferences, where the available data types are
     * limited. We assume that the array contains the same values as the main constructor,
     * in the same order.
     *
     * @param val arguments.
     */
    public static ExternalFileType buildFromArgs(String[] val) {
        if ((val == null) || (val.length < 4) || (val.length > 5)) {
            throw new IllegalArgumentException("Cannot construct ExternalFileType without four elements in String[] argument.");
        }
        String name = val[0];
        String extension = val[1];
        String openWith;
        String mimeType;
        String materialDesignIconCodePoint;

        if (val.length == 4) {
            // Up to version 2.4b the mime type is not included:
            mimeType = "";
            openWith = val[2];
            materialDesignIconCodePoint = FileTypeIconMapping.FILE;
        } else {
            // When mime type is included, the array length should be 5:
            mimeType = val[2];
            openWith = val[3];
            materialDesignIconCodePoint = val[4]; // TODO: Try to distinguish between pre 3.7-format where this is not a code point, but the name of the icon
        }

        // check whether there is another icon defined for this file type
        for (ExternalFileType fileType : ExternalFileTypes.getDefaultExternalFileTypes()) {
            if (fileType.getName().equals(name)) {
                materialDesignIconCodePoint = fileType.getMaterialDesignIconCodePoint();
                break;
            }
        }

        return new ExternalFileType(name, extension, mimeType, openWith, materialDesignIconCodePoint);
    }

    /**
     * Return a String array representing this file type. This is used for storage into
     * Preferences, and the same array can be used to construct the file type later,
     * using the String[] constructor.
     *
     * @return A String[] containing all information about this file type.
     */
    public String[] getStringArrayRepresentation() {
        return new String[] {name, extension, mimeType, openWith, materialDesignIconCodePoint};
    }

    public String getName() {
        return name;
    }

    public void setName(String newName) {
        this.name = newName;
    }

    public String getExtension() {
        if (extension == null) {
            return "";
        }
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getMimeType() {
        if (mimeType == null) {
            return "";
        }
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * Get the bibtex field name used to extension to this file type.
     * Currently we assume that field name equals filename extension.
     *
     * @return The field name.
     */
    public String getFieldName() {
        return extension;
    }

    public String getOpenWithApplication() {
        if (openWith == null) {
            return "";
        }
        return openWith;
    }

    public void setOpenWith(String openWith) {
        this.openWith = openWith;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public int compareTo(ExternalFileType o) {
        return getName().compareTo(o.getName());
    }

    public ExternalFileType copy() {
        return new ExternalFileType(name, extension, mimeType, openWith, materialDesignIconCodePoint);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, extension, mimeType, openWith, materialDesignIconCodePoint);
    }

    /**
     * We define two file type objects as equal if their name, extension, openWith and
     * iconName are equal.
     *
     * @param object The file type to compare with.
     * @return true if the file types are equal.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object instanceof ExternalFileType) {
            ExternalFileType other = (ExternalFileType) object;
            return Objects.equals(name, other.name) && Objects.equals(extension, other.extension)
                    && Objects.equals(mimeType, other.mimeType) && Objects.equals(openWith, other.openWith)
                    && Objects.equals(materialDesignIconCodePoint, other.materialDesignIconCodePoint);
        }
        return false;
    }

    public String getMaterialDesignIconCodePoint() {
        return materialDesignIconCodePoint;
    }

    public void setMaterialDesignIconCodePoint(String materialDesignIconCodePoint) {
        this.materialDesignIconCodePoint = materialDesignIconCodePoint;
    }
}
