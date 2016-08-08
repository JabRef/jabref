package net.sf.jabref.gui.maintable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;

import net.sf.jabref.external.ExternalFileType;
import net.sf.jabref.gui.FileListTableModel;
import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.specialfields.Printed;
import net.sf.jabref.specialfields.Priority;
import net.sf.jabref.specialfields.Quality;
import net.sf.jabref.specialfields.Rank;
import net.sf.jabref.specialfields.ReadStatus;
import net.sf.jabref.specialfields.Relevance;
import net.sf.jabref.specialfields.SpecialFieldsUtils;

public class SpecialMainTableColumns {

    public static final MainTableColumn NUMBER_COL = new MainTableColumn(FieldName.NUMBER_COL) {

        @Override
        public Object getColumnValue(BibEntry entry) {
            return "#";
        }

        @Override
        public String getDisplayName() {
            return "#";
        }
    };

    public static final MainTableColumn RANKING_COLUMN = new MainTableColumn(SpecialFieldsUtils.FIELDNAME_RANKING,
            Collections.singletonList(SpecialFieldsUtils.FIELDNAME_RANKING),
            new JLabel(SpecialFieldsUtils.FIELDNAME_RANKING)) {

        @Override
        public Object getColumnValue(BibEntry entry) {

            return entry.getFieldOptional(SpecialFieldsUtils.FIELDNAME_RANKING)
                    .flatMap(Rank.getInstance()::parse).map(rank -> rank.createLabel()).orElse(null);
        }
    };

    public static final MainTableColumn PRIORITY_COLUMN = new MainTableColumn(SpecialFieldsUtils.FIELDNAME_PRIORITY,
            Collections.singletonList(SpecialFieldsUtils.FIELDNAME_PRIORITY),
            new JLabel(Priority.getInstance().getRepresentingIcon())) {

        @Override
        public Object getColumnValue(BibEntry entry) {

            return entry.getFieldOptional(SpecialFieldsUtils.FIELDNAME_PRIORITY)
                    .flatMap(Priority.getInstance()::parse).map(prio -> prio.createLabel()).orElse(null);
        }
    };

    public static final MainTableColumn READ_STATUS_COLUMN = new MainTableColumn(SpecialFieldsUtils.FIELDNAME_READ,
            Collections.singletonList(SpecialFieldsUtils.FIELDNAME_READ),
            new JLabel(ReadStatus.getInstance().getRepresentingIcon())) {

        @Override
        public Object getColumnValue(BibEntry entry) {

            return entry.getFieldOptional(SpecialFieldsUtils.FIELDNAME_READ)
                    .flatMap(ReadStatus.getInstance()::parse).map(status -> status.createLabel()).orElse(null);
        }
    };

    public static final MainTableColumn RELEVANCE_COLUMN = createIconColumn(SpecialFieldsUtils.FIELDNAME_RELEVANCE,
            Collections.singletonList(SpecialFieldsUtils.FIELDNAME_RELEVANCE),
            new JLabel(Relevance.getInstance().getRepresentingIcon()));

    public static final MainTableColumn PRINTED_COLUMN = createIconColumn(SpecialFieldsUtils.FIELDNAME_PRINTED,
            Collections.singletonList(SpecialFieldsUtils.FIELDNAME_PRINTED),
            new JLabel(Printed.getInstance().getRepresentingIcon()));

    public static final MainTableColumn QUALITY_COLUMN = createIconColumn(SpecialFieldsUtils.FIELDNAME_QUALITY,
            Collections.singletonList(SpecialFieldsUtils.FIELDNAME_QUALITY),
            new JLabel(Quality.getInstance().getRepresentingIcon()));


    public static final MainTableColumn FILE_COLUMN = new MainTableColumn(FieldName.FILE,
            Collections.singletonList(FieldName.FILE), new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon())) {

        @Override
        public Object getColumnValue(BibEntry entry) {
            // We use a FileListTableModel to parse the field content:
            FileListTableModel fileList = new FileListTableModel();
            entry.getFieldOptional(FieldName.FILE).ifPresent(fileList::setContent);
            if (fileList.getRowCount() > 1) {
                return new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
            } else if (fileList.getRowCount() == 1) {
                Optional<ExternalFileType> type = fileList.getEntry(0).type;
                if (type.isPresent()) {
                    return type.get().getIconLabel();
                } else {
                    return new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon());
                }
            }

            return null;
        }
    };

    /**
     * Creates a MainTableColumn which shows an icon instead textual content
     *
     * @param columnName the name of the column
     * @param fields     the entry fields which should be shown
     * @return the crated MainTableColumn
     */
    public static MainTableColumn createIconColumn(String columnName, List<String> fields, JLabel iconLabel) {
        return new MainTableColumn(columnName, fields, iconLabel) {

            @Override
            public Object getColumnValue(BibEntry entry) {
                JLabel iconLabel = null;
                boolean iconFound = false;

                // check for each field whether content is available
                for (String field : fields) {
                    if (entry.hasField(field)) {
                        if (iconFound) {
                            return new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                        } else {
                            iconLabel = GUIGlobals.getTableIcon(field);
                            iconFound = true;
                        }

                    }
                }
                return iconLabel;
            }
        };
    }

    /**
     * create a MainTableColumn for specific file types.
     *
     * Shows the icon for the given type (or the FILE_MULTIPLE icon)
     *
     * @param externalFileTypeName the name of the externalFileType
     *
     * @return the created MainTableColumn
     */
    public static MainTableColumn createFileIconColumn(String externalFileTypeName) {



        return new MainTableColumn(externalFileTypeName, Collections.singletonList(FieldName.FILE), new JLabel()) {

            @Override
            public boolean isFileFilter() {
                return true;
            }

            @Override
            public String getDisplayName() {
                return externalFileTypeName;
            }

            @Override
            public Object getColumnValue(BibEntry entry) {

                boolean iconFound = false;
                JLabel iconLabel = null;
                FileListTableModel fileList = new FileListTableModel();
                entry.getFieldOptional(FieldName.FILE).ifPresent(fileList::setContent);
                for (int i = 0; i < fileList.getRowCount(); i++) {
                    if ((fileList.getEntry(i).type.isPresent())
                            && externalFileTypeName.equalsIgnoreCase(fileList.getEntry(i).type.get().getName())) {
                        if (iconFound) {
                            // already found another file of the desired type - show FILE_MULTIPLE Icon
                            return new JLabel(IconTheme.JabRefIcon.FILE_MULTIPLE.getSmallIcon());
                        } else {
                            iconLabel = fileList.getEntry(i).type.get().getIconLabel();
                            iconFound = true;
                        }
                    }
                }
                return iconLabel;
            }
        };
    }
}
