package net.sf.jabref.gui.maintable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.swing.JLabel;

import net.sf.jabref.gui.GUIGlobals;
import net.sf.jabref.gui.IconTheme;
import net.sf.jabref.gui.externalfiletype.ExternalFileType;
import net.sf.jabref.gui.filelist.FileListTableModel;
import net.sf.jabref.gui.specialfields.SpecialFieldValueViewModel;
import net.sf.jabref.gui.specialfields.SpecialFieldViewModel;
import net.sf.jabref.model.entry.BibEntry;
import net.sf.jabref.model.entry.FieldName;
import net.sf.jabref.model.entry.specialfields.SpecialField;
import net.sf.jabref.model.entry.specialfields.SpecialFields;

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

    public static final MainTableColumn RANKING_COLUMN = new MainTableColumn(SpecialField.RANK.getFieldName(),
            Collections.singletonList(SpecialField.RANK.getFieldName()),
            new JLabel(SpecialField.RANK.getFieldName())) {

        @Override
        public Object getColumnValue(BibEntry entry) {

            return entry.getField(SpecialField.RANK.getFieldName())
                    .flatMap(SpecialField.RANK::parse).map(rank -> new SpecialFieldValueViewModel(rank).createSpecialFieldValueLabel()).orElse(null);
        }
    };

    public static final MainTableColumn PRIORITY_COLUMN = new MainTableColumn(SpecialField.PRIORITY.getFieldName(),
            Collections.singletonList(SpecialField.PRIORITY.getFieldName()),
            new JLabel(new SpecialFieldViewModel(SpecialField.PRIORITY).getRepresentingIcon())) {

        @Override
        public Object getColumnValue(BibEntry entry) {

            return entry.getField(SpecialField.PRIORITY.getFieldName())
                    .flatMap(SpecialField.PRIORITY::parse).map(prio -> new SpecialFieldValueViewModel(prio).createSpecialFieldValueLabel()).orElse(null);
        }
    };

    public static final MainTableColumn READ_STATUS_COLUMN = new MainTableColumn(SpecialField.READ_STATUS.getFieldName(),
            Collections.singletonList(SpecialField.READ_STATUS.getFieldName()),
            new JLabel(new SpecialFieldViewModel(SpecialField.READ_STATUS).getRepresentingIcon())) {

        @Override
        public Object getColumnValue(BibEntry entry) {

            return entry.getField(SpecialField.READ_STATUS.getFieldName())
                    .flatMap(SpecialField.READ_STATUS::parse).map(status -> new SpecialFieldValueViewModel(status).createSpecialFieldValueLabel()).orElse(null);
        }
    };

    public static final MainTableColumn RELEVANCE_COLUMN = createIconColumn(SpecialField.RELEVANCE.getFieldName(),
            Collections.singletonList(SpecialField.RELEVANCE.getFieldName()),
            new JLabel(new SpecialFieldViewModel(SpecialField.RELEVANCE).getRepresentingIcon()));

    public static final MainTableColumn PRINTED_COLUMN = createIconColumn(SpecialField.PRINTED.getFieldName(),
            Collections.singletonList(SpecialField.PRINTED.getFieldName()),
            new JLabel(new SpecialFieldViewModel(SpecialField.PRINTED).getRepresentingIcon()));

    public static final MainTableColumn QUALITY_COLUMN = createIconColumn(SpecialField.QUALITY.getFieldName(),
            Collections.singletonList(SpecialField.QUALITY.getFieldName()),
            new JLabel(new SpecialFieldViewModel(SpecialField.QUALITY).getRepresentingIcon()));


    public static final MainTableColumn FILE_COLUMN = new MainTableColumn(FieldName.FILE,
            Collections.singletonList(FieldName.FILE), new JLabel(IconTheme.JabRefIcon.FILE.getSmallIcon())) {

        @Override
        public Object getColumnValue(BibEntry entry) {
            // We use a FileListTableModel to parse the field content:
            FileListTableModel fileList = new FileListTableModel();
            entry.getField(FieldName.FILE).ifPresent(fileList::setContent);
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
                entry.getField(FieldName.FILE).ifPresent(fileList::setContent);
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
